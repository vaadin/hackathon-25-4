package com.vaadin.securitystatus.status;

import java.io.Serializable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.component.shared.DisableOnClickMode;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.dom.DomEventFilter;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsSnippets;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.securitystatus.base.ui.HelpDialog;
import com.vaadin.securitystatus.data.DataRefreshSignal;
import com.vaadin.securitystatus.data.DataUnreachableException;
import com.vaadin.securitystatus.data.SecurityDataService;
import com.vaadin.securitystatus.data.VersionNumber;
import com.vaadin.securitystatus.data.model.CasesIndex;

/**
 * The lookup: three tabs, by version, by advisory ids and by report, over the
 * session's {@link LookupState}. Check navigates to the route that answers;
 * clearing a field, with its clear control or Escape, withdraws its part of the
 * question. The slash key focuses it from anywhere, except while the reader is
 * typing in another field.
 *
 * <p>
 * Mounted once per window by the customer shell, so it is never rebuilt by a
 * navigation. Each navigation writes what its address asks into the state, and
 * the fields follow the state.
 */
public final class Lookup extends Composite<Div> {

    /**
     * The keyboard filters, declared so nothing is compiled from a string in the
     * browser. A filter runs in the browser for each event and only an event it
     * passes reaches the server.
     */
    @JsDefinition
    public interface KeysJs extends Serializable {

        /** The slash key, unless the reader is typing in a field, an editable region or a select. */
        @JsExpression("return event.key === '/' && !event.ctrlKey && !event.metaKey && !event.altKey && !event.composedPath().some(node => node instanceof HTMLElement && (node.isContentEditable || ['INPUT', 'TEXTAREA', 'SELECT'].includes(node.tagName)))")
        DomEventFilter slashOutsideAField();

        /** Escape on a field whose dropdown, if it has one, is closed. */
        @JsExpression("return event.key === 'Escape' && !element.opened")
        DomEventFilter escapeWithNothingOpen();

        /** A change that left the field empty: its clear control, Escape, or every character deleted. */
        @JsExpression("return element.value === ''")
        DomEventFilter emptied();
    }

    static final KeysJs KEYS = JsSnippets.of(KeysJs.class);

    /** What the version field is told when an advisory id leaves it. */
    @JsDefinition
    public interface VersionFieldJs extends Serializable {

        /** Empties the typed text, which is the input's and not the value's, and closes the dropdown. */
        @JsExpression("$0.clear(); $0.close();")
        PendingJavaScriptResult emptyAndClose(Element field);
    }

    private final LookupState state;
    private final SecurityDataService data;

    private final Tabs tabs = new Tabs();
    private final Map<LookupState.Tab, com.vaadin.flow.component.tabs.Tab> tabOf = new HashMap<>();
    private final ComboBox<String> version = new ComboBox<>();
    private final TextField ids = new TextField();
    private final TextField idsVersion = new TextField();
    private final Button check = new Button();

    /** The text in the version field while it is typed, before it is committed. */
    private final ValueSignal<String> typed = new ValueSignal<>("");
    /** What the last Check could not use, said in place. */
    private final ValueSignal<String> note = new ValueSignal<>("");

    Lookup(LookupState state, SecurityDataService data, DataRefreshSignal refresh) {
        this.state = state;
        this.data = data;
        addClassName("lookup");
        getElement().setAttribute("role", "search");

        addTab(LookupState.Tab.VERSION, "lookup.tab.version");
        addTab(LookupState.Tab.IDS, "lookup.tab.ids");
        addTab(LookupState.Tab.REPORT, "lookup.tab.report");
        tabs.addClassName("lookup-tabs");
        // Acts only when the tab differs from the state, so the effect below,
        // which selects the tab the state names, never writes back into it.
        tabs.addSelectedChangeListener(change -> tabOf.forEach((which, tab) -> {
            if (tab == change.getSelectedTab() && which != state.tab().peek()) {
                showTab(which);
            }
        }));
        Signal.effect(this, () -> tabs.setSelectedTab(tabOf.get(state.tab().get())));

        var fields = new Div(versionPanel(refresh), idsPanel(), reportPanel(), checkButton());
        fields.addClassName("lookup-fields");

        var said = new Span();
        said.addClassName("lookup-note");
        said.getElement().setAttribute("role", "status");
        said.bindText(note);
        said.bindVisible(note.map(text -> !text.isEmpty()));

        getContent().add(tabs, fields, said);

        whenAttached(ui -> {
            var slash = ui.getElement().addEventListener("keydown", key -> focusQuestion())
                    .setFilterExpression(KEYS.slashOutsideAField()).preventDefault();
            var navigation = ui.addAfterNavigationListener(this::reflect);
            return () -> {
                slash.remove();
                navigation.remove();
            };
        });
    }

    private void addTab(LookupState.Tab which, String key) {
        var tab = new com.vaadin.flow.component.tabs.Tab(getTranslation(key));
        tabOf.put(which, tab);
        tabs.add(tab);
    }

    private Component versionPanel(DataRefreshSignal refresh) {
        version.addClassName("lookup-version");
        version.getElement().appendChild(labelWithHelp("lookup.version.label", versionHelp()));
        version.setAriaLabel(getTranslation("lookup.version.label"));
        version.setClearButtonVisible(true);
        // Free text, so a version nobody measured can still be asked. That rules
        // out PartialMatchMode, which the component refuses beside custom values.
        version.setAllowCustomValue(true);
        version.bindValue(state.version().map(text -> text.isEmpty() ? null : text),
                value -> state.version().set(value == null ? "" : value));

        var generatedAt = refresh.generatedAt();
        Signal.effect(version, () -> {
            generatedAt.get();
            offerMeasuredVersions();
        });

        // The text being typed is not a value until it is committed, and the
        // component does not say what it is. Asked of the element, so the Check
        // button can follow it and a pasted advisory id can move to its tab.
        version.getElement().addPropertyChangeListener("filter", "filter-changed",
                change -> typedIntoVersion(Objects.toString(change.getValue(), "")));
        version.addCustomValueSetListener(custom -> {
            if (LookupInput.isAdvisory(custom.getDetail())) {
                moveToIds(custom.getDetail());
            } else {
                state.version().set(custom.getDetail());
            }
        });
        version.addValueChangeListener(change -> {
            if (change.isFromClient() && change.getValue() == null) {
                withdrawVersion();
            }
        });
        onEscape(version.getElement(), this::withdrawVersion);

        var panel = new Div(version);
        panel.addClassName("lookup-panel");
        panel.bindVisible(state.tab().map(LookupState.Tab.VERSION::equals));
        return panel;
    }

    private Component idsPanel() {
        ids.addClassName("lookup-ids");
        ids.getElement().appendChild(labelWithHelp("lookup.ids.label", idsHelp()));
        ids.setAriaLabel(getTranslation("lookup.ids.label"));
        ids.setClearButtonVisible(true);
        ids.setValueChangeMode(ValueChangeMode.EAGER);
        ids.bindValue(state.ids(), state.ids()::set);
        ids.addValueChangeListener(change -> {
            if (change.isFromClient()) {
                note.set("");
            }
        });
        onEmptied(ids.getElement(), this::withdrawIds);
        onEscape(ids.getElement(), this::withdrawIds);

        idsVersion.addClassName("lookup-ids-version");
        idsVersion.setLabel(getTranslation("lookup.idsVersion.label"));
        idsVersion.setClearButtonVisible(true);
        idsVersion.setValueChangeMode(ValueChangeMode.EAGER);
        idsVersion.bindValue(state.idsVersion(), state.idsVersion()::set);
        onEmptied(idsVersion.getElement(), this::withdrawIdsVersion);
        onEscape(idsVersion.getElement(), this::withdrawIdsVersion);

        var panel = new Div(ids, idsVersion);
        panel.addClassName("lookup-panel");
        panel.bindVisible(state.tab().map(LookupState.Tab.IDS::equals));
        return panel;
    }

    private Component reportPanel() {
        var upload = new Upload();
        upload.addClassName("lookup-report");
        upload.setMaxFiles(1);
        upload.setAcceptedFileTypes(".json", "application/json");
        upload.setDropLabel(new Span(getTranslation("lookup.report.drop")));
        upload.setUploadButton(new Button(getTranslation("lookup.report.choose")));
        // Reading the report is epic 06. Until then the drop zone is drawn and
        // takes nothing, rather than accepting a file it would ignore.
        upload.setEnabled(false);

        var formats = new Paragraph(getTranslation("lookup.report.formats"));
        formats.addClassName("lookup-report-formats");
        var card = new Div(upload, formats, reportHelp());
        card.addClassName("lookup-report-card");

        var panel = new Div(card);
        panel.addClassName("lookup-panel");
        panel.bindVisible(state.tab().map(LookupState.Tab.REPORT::equals));
        return panel;
    }

    private Component checkButton() {
        check.setText(getTranslation("lookup.check"));
        check.addClassName("lookup-check");
        check.addThemeVariants(ButtonVariant.PRIMARY);
        check.setDisableOnClick(DisableOnClickMode.UNTIL_RESPONSE);
        Signal<Boolean> asked = Signal.computed(() -> switch (state.tab().get()) {
            case VERSION -> !typed.get().isBlank() || !state.version().get().isBlank();
            case IDS -> !state.ids().get().isBlank();
            case REPORT -> false;
        });
        // Not bindEnabled: Button refuses a bound enabled state beside disable on
        // click, even in the UNTIL_RESPONSE mode where it hands the state back by
        // itself. specs/FEEDBACK-25.4.md, draft 13.
        Signal.effect(check, () -> check.setEnabled(asked.get()));
        check.bindVisible(state.tab().map(tab -> tab != LookupState.Tab.REPORT));
        check.addClickListener(click -> submit());
        return check;
    }

    private void offerMeasuredVersions() {
        List<String> measured;
        String newestShown;
        try {
            CasesIndex index = data.index();
            measured = index.byLine().values().stream().flatMap(List::stream).distinct()
                    .sorted(Comparator.reverseOrder()).map(VersionNumber::toString).toList();
            newestShown = HomeView.shownLines(index, false).stream().findFirst()
                    .map(line -> HomeView.spokenFor(line).toString()).orElse(null);
        } catch (DataUnreachableException unreachable) {
            measured = List.of();
            newestShown = null;
        }
        version.setItems(measured);
        // An example, never a value: "e.g." in front and the faintest ink on the page.
        version.setPlaceholder(newestShown == null ? null : getTranslation("lookup.version.example", newestShown));
    }

    /**
     * A pasted advisory id moves to its tab at once. One typed character at a
     * time it would move on "CVE-", in the middle of the word, so a typed id
     * moves when it is committed instead: Enter, leaving the field, or Check.
     */
    private void typedIntoVersion(String text) {
        int before = typed.peek().length();
        typed.set(text);
        if (LookupInput.isAdvisory(text) && text.length() - before > 1) {
            moveToIds(text);
        }
    }

    /** Somebody who pastes an advisory id into the version field means the other tab. */
    private void moveToIds(String text) {
        state.ids().set(text.strip());
        typed.set("");
        version.clear();
        // The text typed is the input's and not the value's, so emptying the
        // value leaves it standing in the field. Asked of the lookup itself,
        // because JavaScript for an invisible element waits until it is shown.
        getElement().executeJs(VersionFieldJs.class).emptyAndClose(version.getElement());
        showTab(LookupState.Tab.IDS);
        ids.focus();
    }

    private void showTab(LookupState.Tab which) {
        note.set("");
        // A dropdown left open on a hidden field covers the page and takes its
        // clicks once the field is shown again.
        version.setOpened(false);
        state.tab().set(which);
    }

    private void submit() {
        note.set("");
        switch (state.tab().peek()) {
            case VERSION -> submitVersion();
            case IDS -> submitIds();
            case REPORT -> {
                // The report tab answers by its upload, epic 06.
            }
        }
    }

    private void submitVersion() {
        String text = typed.peek().isBlank() ? state.version().peek() : typed.peek();
        if (LookupInput.isAdvisory(text)) {
            moveToIds(text);
            submitIds();
            return;
        }
        String cleaned = LookupInput.cleanVersion(text);
        if (cleaned.isEmpty()) {
            return;
        }
        state.version().set(cleaned);
        UI.getCurrent().navigate(VersionView.class, new RouteParameters(VersionView.VERSION, cleaned));
    }

    private void submitIds() {
        LookupInput.Ids parsed = LookupInput.splitIds(state.ids().peek());
        if (parsed.ids().isEmpty()) {
            note.set(getTranslation("lookup.ids.none"));
            return;
        }
        if (!parsed.unrecognised().isEmpty()) {
            note.set(getTranslation("lookup.ids.unrecognised", String.join(", ", parsed.unrecognised())));
        }
        var parameters = new HashMap<String, String>();
        parameters.put(CveView.IDS, String.join(",", parsed.ids()));
        String asked = LookupInput.cleanVersion(state.idsVersion().peek());
        if (!asked.isEmpty()) {
            parameters.put(CveView.VERSION, asked);
        }
        UI.getCurrent().navigate(CveView.class, new RouteParameters(parameters));
    }

    /** The version field emptied: the question is gone, so is its answer. */
    private void withdrawVersion() {
        typed.set("");
        state.version().set("");
        goHomeFrom(VersionView.class);
    }

    private void withdrawIds() {
        note.set("");
        state.ids().set("");
        goHomeFrom(CveView.class);
    }

    /** The version beside the ids emptied: the same ids, answered for no version. */
    private void withdrawIdsVersion() {
        state.idsVersion().set("");
        var ui = UI.getCurrent();
        var current = ui.getInternals().getActiveRouterTargetsChain();
        if (!current.isEmpty() && current.get(0) instanceof CveView) {
            ui.getInternals().getActiveViewLocation().getSegments().stream().skip(1).findFirst()
                    .ifPresent(asked -> ui.navigate(CveView.class, new RouteParameters(CveView.IDS, asked)));
        }
    }

    private void goHomeFrom(Class<? extends Component> answer) {
        var ui = UI.getCurrent();
        var current = ui.getInternals().getActiveRouterTargetsChain();
        if (!current.isEmpty() && answer.isInstance(current.get(0))) {
            ui.navigate(HomeView.class);
        }
    }

    /** The field of the tab on show, from anywhere on the page. */
    private void focusQuestion() {
        switch (state.tab().peek()) {
            case VERSION -> version.focus();
            case IDS -> ids.focus();
            case REPORT -> {
                // Nothing on the report tab takes text until epic 06.
            }
        }
    }

    /** Each navigation says which question is on screen, and the fields follow it. */
    private void reflect(AfterNavigationEvent navigation) {
        var chain = navigation.getActiveChain();
        if (chain.isEmpty()) {
            return;
        }
        var parameters = navigation.getRouteParameters();
        var target = chain.get(0);
        if (target instanceof VersionView) {
            parameters.get(VersionView.VERSION).ifPresent(asked -> {
                typed.set("");
                state.version().set(asked);
                state.tab().set(LookupState.Tab.VERSION);
            });
        } else if (target instanceof CveView) {
            parameters.get(CveView.IDS)
                    .ifPresent(asked -> state.ids().set(String.join(", ", LookupInput.splitIds(asked).ids())));
            state.idsVersion().set(parameters.get(CveView.VERSION).orElse(""));
            state.tab().set(LookupState.Tab.IDS);
        } else if (target instanceof ScanView) {
            parameters.get(ScanView.VERSION).ifPresent(state.version()::set);
            state.tab().set(LookupState.Tab.REPORT);
        }
    }

    private static void onEscape(Element field, Runnable action) {
        field.addEventListener("keydown", key -> action.run()).setFilterExpression(KEYS.escapeWithNothingOpen());
    }

    private static void onEmptied(Element field, Runnable action) {
        field.addEventListener("change", change -> action.run()).setFilterExpression(KEYS.emptied());
    }

    private Element labelWithHelp(String key, HelpDialog help) {
        var label = new Span(new Span(getTranslation(key)), help);
        label.addClassName("lookup-label");
        label.getElement().setAttribute("slot", "label");
        return label.getElement();
    }

    private HelpDialog versionHelp() {
        return new HelpDialog(getTranslation("lookup.help.version.title"),
                HelpDialog.section(getTranslation("lookup.help.version.what"),
                        getTranslation("lookup.help.version.what.text")),
                HelpDialog.section(getTranslation("lookup.help.version.unmeasured"),
                        getTranslation("lookup.help.version.unmeasured.text")));
    }

    private HelpDialog idsHelp() {
        return new HelpDialog(getTranslation("lookup.help.ids.title"),
                HelpDialog.section(getTranslation("lookup.help.ids.first"),
                        getTranslation("lookup.help.ids.first.text")),
                HelpDialog.section(getTranslation("lookup.help.ids.why"), getTranslation("lookup.help.ids.why.text")),
                HelpDialog.section(getTranslation("lookup.help.ids.ruled"),
                        getTranslation("lookup.help.ids.ruled.text")));
    }

    private HelpDialog reportHelp() {
        return new HelpDialog(getTranslation("lookup.help.report.title"),
                HelpDialog.section(getTranslation("lookup.help.report.assessed"),
                        getTranslation("lookup.help.report.assessed.text"),
                        getTranslation("lookup.help.report.rest.text")));
    }
}

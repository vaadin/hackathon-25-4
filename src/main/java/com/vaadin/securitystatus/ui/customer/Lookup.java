package com.vaadin.securitystatus.ui.customer;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.component.shared.DisableOnClickMode;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.UploadButton;
import com.vaadin.flow.component.upload.UploadDropZone;
import com.vaadin.flow.component.upload.UploadManager;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.dom.DomEventFilter;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsSnippets;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.server.streams.UploadEvent;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.securitystatus.customer.LookupInput;
import com.vaadin.securitystatus.customer.LookupState;
import com.vaadin.securitystatus.customer.ReportFormat;
import com.vaadin.securitystatus.customer.ReportParser;
import com.vaadin.securitystatus.customer.ScanState;
import com.vaadin.securitystatus.customer.ScannerReport;
import com.vaadin.securitystatus.customer.Triage;
import com.vaadin.securitystatus.data.DataRefreshSignal;
import com.vaadin.securitystatus.data.DataUnreachableException;
import com.vaadin.securitystatus.data.SecurityDataService;
import com.vaadin.securitystatus.data.VersionNumber;
import com.vaadin.securitystatus.data.model.CasesIndex;
import com.vaadin.securitystatus.ui.common.HelpDialog;

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

        @JsExpression("return event.key === 'Enter' && !event.isComposing")
        DomEventFilter enter();

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

    /** The largest report read, in bytes. A scanner report of a whole application is a few megabytes. */
    static final long MAX_REPORT_BYTES = 32L * 1024 * 1024;

    private final LookupState state;
    private final SecurityDataService data;
    private final ScanState scan;
    private final UploadManager reports;
    private final TextField reportVersion = new TextField();
    /** The version the scan route answers for, empty anywhere else. */
    private final ValueSignal<String> scanVersion = new ValueSignal<>("");
    /** A report is in and the file names no release: only then is the reader asked for one. */
    private final Signal<Boolean> reportAsks;

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
    /** Whether an answer, and not the home page, is under the lookup. */
    private boolean answering;

    Lookup(LookupState state, SecurityDataService data, DataRefreshSignal refresh, ScanState scan) {
        this.state = state;
        this.data = data;
        this.scan = scan;
        // Read in the request and never stored: the handler parses the request's own
        // stream, so no copy of the file exists anywhere once the request ends.
        this.reports = new UploadManager(this, this::receive);
        this.reportAsks = scan.held().map(held -> held.isPresent() && held.get().report().vaadin().isEmpty());
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
        // Items first: a combo box refuses a value, even a remembered one, before it has items.
        var generatedAt = refresh.generatedAt();
        Signal.effect(version, () -> {
            generatedAt.get();
            offerMeasuredVersions();
        });
        version.bindValue(state.version().map(text -> text.isEmpty() ? null : text),
                value -> state.version().set(value == null ? "" : value));

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
        onEnter(version.getElement());

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
        onEnter(ids.getElement());

        idsVersion.addClassName("lookup-ids-version");
        idsVersion.setLabel(getTranslation("lookup.idsVersion.label"));
        idsVersion.setClearButtonVisible(true);
        idsVersion.setValueChangeMode(ValueChangeMode.EAGER);
        idsVersion.bindValue(state.idsVersion(), state.idsVersion()::set);
        onEmptied(idsVersion.getElement(), this::withdrawIdsVersion);
        onEscape(idsVersion.getElement(), this::withdrawIdsVersion);
        onEnter(idsVersion.getElement());

        var panel = new Div(ids, idsVersion);
        panel.addClassName("lookup-panel");
        panel.bindVisible(state.tab().map(LookupState.Tab.IDS::equals));
        return panel;
    }

    private Component reportPanel() {
        reports.setMaxFiles(1);
        reports.setMaxFileSize(MAX_REPORT_BYTES);
        reports.addFileRejectedListener(rejected -> scan.refusal().set(switch (rejected.getReason()) {
            case FILE_TOO_LARGE -> getTranslation("lookup.report.refused.large", MAX_REPORT_BYTES / (1024 * 1024));
            case TOO_MANY_FILES -> getTranslation("lookup.report.refused.many");
            case INCORRECT_FILE_TYPE, UNKNOWN -> refusedFormat();
        }));

        var choose = new UploadButton(reports);
        choose.addClassName("lookup-report-choose");
        choose.bindText(scan.held().map(held -> getTranslation(
                held.isPresent() ? "lookup.report.replace" : "lookup.report.choose")));

        // The prototype's card: before a file its title asks for one and the line names the
        // formats read, and no others, rule 6.11.21; with one in, the title is the file's
        // name and the line says what is in it. The help mark and the button close the row.
        var title = new Span();
        title.addClassName("lookup-report-title");
        title.bindText(scan.held().map(held -> held.map(ScanState.Held::fileName)
                .orElseGet(() -> getTranslation("lookup.report.drop"))));
        var line = new Paragraph();
        line.addClassName("lookup-report-formats");
        line.bindText(scan.held().map(held -> held.map(this::fileLine).orElseGet(this::formatsLine)));
        var said = new Div(title, line);
        said.addClassName("lookup-report-said");
        choose.getElement().getThemeList().add("tertiary");
        // The prototype's mark at the start of the card: an arrow up, the file going in.
        var mark = VaadinIcon.ARROW_UP.create();
        mark.addClassName("lookup-report-mark");
        mark.getElement().setAttribute("aria-hidden", "true");
        var drop = new UploadDropZone(new Div(mark, said, reportHelp(), choose), reports);
        drop.addClassName("lookup-report");

        var refused = new Span();
        refused.addClassName("lookup-report-refused");
        refused.getElement().setAttribute("role", "alert");
        refused.bindText(scan.refusal());
        refused.bindVisible(scan.refusal().map(text -> !text.isEmpty()));

        reportVersion.addClassName("lookup-report-version");
        reportVersion.setLabel(getTranslation("lookup.report.version"));
        reportVersion.setClearButtonVisible(true);
        reportVersion.setValueChangeMode(ValueChangeMode.EAGER);
        reportVersion.bindValue(state.version(), state.version()::set);
        onEnter(reportVersion.getElement());
        reportVersion.bindVisible(reportAsks);

        // Under the card, as the prototype says it: why the file asks for a version, until
        // one is given.
        var asks = new Paragraph();
        asks.addClassName("lookup-report-asks");
        asks.bindText(scan.held().map(held -> held.map(this::asksSentence).orElse("")));
        asks.bindVisible(Signal.computed(() -> reportAsks.get() && scanVersion.get().isEmpty()));

        var card = new Div(drop, refused);
        card.addClassName("lookup-report-card");

        var panel = new Div(reportVersion, card, asks);
        panel.addClassNames("lookup-panel", "lookup-report-panel");
        panel.bindVisible(state.tab().map(LookupState.Tab.REPORT::equals));
        return panel;
    }

    /** A vulnerability report lists only what carries a finding; a bill of materials that names no release is said plainly. */
    private String asksSentence(ScanState.Held held) {
        return getTranslation(held.report().components().isEmpty() && !held.report().findings().isEmpty()
                ? "scan.noVersion.findings" : "scan.noVersion");
    }

    /** The four formats the parser reads, in the parser's order, so the line cannot name another. */
    String formatsLine() {
        var names = new ArrayList<String>();
        for (ReportFormat format : ReportFormat.values()) {
            names.add(getTranslation(format.nameKey()));
        }
        String last = names.removeLast();
        return getTranslation("lookup.report.formats", String.join(", ", names), last);
    }

    private String fileLine(ScanState.Held held) {
        var parts = new ArrayList<String>();
        held.report().vaadin().ifPresent(version -> parts.add(getTranslation("lookup.report.file.vaadin", version)));
        int size = held.report().size();
        parts.add(getTranslation(size == 1 ? "lookup.report.file.findings.one" : "lookup.report.file.findings.many",
                size));
        parts.add(getTranslation(held.report().format().nameKey()));
        return String.join(" \u00b7 ", parts);
    }

    private String refusedFormat() {
        var names = new ArrayList<String>();
        for (ReportFormat format : ReportFormat.values()) {
            names.add(getTranslation(format.nameKey()));
        }
        String last = names.removeLast();
        return getTranslation("lookup.report.refused.format", String.join(", ", names), last);
    }

    /**
     * Runs on the request that carries the file, outside the session lock: the
     * stream is parsed as it arrives and what the screen needs is handed to the
     * session. A file that is not a report is refused in the upload area and
     * nothing navigates.
     */
    private void receive(UploadEvent event) throws IOException {
        ScannerReport report;
        try (InputStream body = event.getInputStream()) {
            report = ReportParser.parse(body, MAX_REPORT_BYTES);
        } catch (ReportParser.RefusedException refused) {
            event.reject(refused.refusal().name());
            event.getUI().access(() -> {
                scan.refusal().set(switch (refused.refusal()) {
                    case NOT_JSON -> getTranslation("lookup.report.refused.json", refusedFormat());
                    case UNKNOWN_FORMAT -> refusedFormat();
                    case TOO_LARGE -> getTranslation("lookup.report.refused.large", MAX_REPORT_BYTES / (1024 * 1024));
                    case TOO_MANY_FINDINGS -> getTranslation("lookup.report.refused.findings",
                            ReportParser.MAX_FINDINGS);
                });
                reports.clearFileList();
            });
            return;
        }
        String fileName = event.getFileName();
        event.getUI().access(() -> took(new ScanState.Held(fileName, report)));
    }

    private void took(ScanState.Held held) {
        Optional<String> before = scan.held().peek().flatMap(previous -> previous.report().vaadin());
        scan.held().set(Optional.of(held));
        scan.refusal().set("");
        reports.clearFileList();
        // A version the reader named wins over the file's: it is their own statement.
        // One that came from the previous file is the file's, and the new file answers it.
        String asked = scanVersion.peek();
        boolean readersOwn = !asked.isEmpty() && !before.filter(asked::equals).isPresent();
        String version = readersOwn ? asked : held.report().vaadin().orElse("");
        state.tab().set(LookupState.Tab.REPORT);
        if (version.isEmpty()) {
            UI.getCurrent().navigate(ScanView.class);
        } else {
            state.version().set(version);
            UI.getCurrent().navigate(ScanView.class, new RouteParameters(ScanView.VERSION, version));
        }
    }

    private Component checkButton() {
        check.addClassName("lookup-check");
        check.addThemeVariants(ButtonVariant.PRIMARY);
        check.setDisableOnClick(DisableOnClickMode.UNTIL_RESPONSE);
        Signal<Boolean> asked = Signal.computed(() -> switch (state.tab().get()) {
            case VERSION -> !typed.get().isBlank() || !state.version().get().isBlank();
            case IDS -> !state.ids().get().isBlank();
            case REPORT -> reportAsks.get() && !state.version().get().isBlank();
        });
        // Not bindEnabled: Button refuses a bound enabled state beside disable on
        // click, even in the UNTIL_RESPONSE mode where it hands the state back by
        // itself. specs/FEEDBACK-25.4.md, draft 13.
        Signal.effect(check, () -> check.setEnabled(asked.get()));
        check.bindVisible(Signal.computed(() -> state.tab().get() != LookupState.Tab.REPORT || reportAsks.get()));
        check.bindText(state.tab().map(tab -> getTranslation(
                tab == LookupState.Tab.REPORT ? "lookup.report.answer" : "lookup.check")));
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
        // A new question starts from the home page, not under the answer to the last one.
        if (answering) {
            UI.getCurrent().navigate(HomeView.class);
        }
    }

    private void submit() {
        note.set("");
        switch (state.tab().peek()) {
            case VERSION -> submitVersion();
            case IDS -> submitIds();
            case REPORT -> submitReportVersion();
        }
    }

    /** The version a report that names none is answered for. */
    private void submitReportVersion() {
        String cleaned = LookupInput.cleanVersion(state.version().peek());
        if (cleaned.isEmpty()) {
            return;
        }
        state.version().set(cleaned);
        UI.getCurrent().navigate(ScanView.class, new RouteParameters(ScanView.VERSION, cleaned));
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
            // The refusal is the answer now: the last answer does not stay under it.
            if (answering) {
                UI.getCurrent().navigate(HomeView.class);
            }
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
                if (reportVersion.isVisible()) {
                    reportVersion.focus();
                }
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
        answering = !(target instanceof HomeView);
        scanVersion.set(target instanceof ScanView ? parameters.get(ScanView.VERSION).orElse("") : "");
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

    /**
     * Enter in a field asks the question, as Check does. The answer waits for
     * the end of the round trip, so a value the same Enter commits, such as the
     * combo box's typed version, is already in the state, D-240.
     */
    private void onEnter(Element field) {
        field.addEventListener("keydown", key -> getUI()
                .ifPresent(ui -> ui.beforeClientResponse(this, context -> submit())))
                .setFilterExpression(KEYS.enter());
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
        var counted = new Paragraph();
        counted.addClassName("lookup-help-scope");
        counted.bindText(scan.held().map(held -> held.map(this::scopeSentence).orElse("")));
        counted.bindVisible(scan.held().map(Optional::isPresent));
        var assessed = HelpDialog.section(getTranslation("lookup.help.report.assessed"),
                getTranslation("lookup.help.report.assessed.text"), getTranslation("lookup.help.report.rest.text"));
        assessed.add(counted);

        var sections = new ArrayList<Component>();
        sections.add(assessed);
        // One recipe per format the parser reads, in its order: a format accepted and
        // never explained is one the reader has to guess at.
        for (ReportFormat format : ReportFormat.values()) {
            var recipe = HelpDialog.section(getTranslation(format.nameKey()),
                    getTranslation("lookup.help.report." + format.name() + ".why"));
            recipe.addClassName("lookup-help-format");
            var command = new Pre(getTranslation("lookup.help.report." + format.name() + ".run"));
            command.addClassName("lookup-help-run");
            recipe.add(command, new Paragraph(getTranslation("lookup.help.report." + format.name() + ".leaves")));
            if (format == ReportFormat.CYCLONEDX) {
                // Its second half, the frontend's, is a second command and a second file.
                var frontend = new Pre(getTranslation("lookup.help.report.CYCLONEDX.frontend.run"));
                frontend.addClassName("lookup-help-run");
                recipe.add(frontend, new Paragraph(getTranslation("lookup.help.report.CYCLONEDX.frontend.leaves")));
            }
            sections.add(recipe);
        }
        sections.add(new Paragraph(getTranslation("lookup.help.report.json")));
        // The app's own promise, which replaces the prototype's "Read in this browser":
        // last, where it disturbs the prototype's order least.
        sections.add(HelpDialog.section(getTranslation("lookup.help.report.kept"),
                getTranslation("lookup.help.report.kept.text")));
        return new HelpDialog(getTranslation("lookup.help.report.title"), sections.toArray(Component[]::new));
    }

    /** The two numbers of rule 6.11.18, counted against the file in hand. */
    private String scopeSentence(ScanState.Held held) {
        Triage.Scope scope;
        try {
            scope = Triage.scope(held.report(), data.scanIndex());
        } catch (DataUnreachableException unreachable) {
            return "";
        }
        if (scope.listed() == 0) {
            return "";
        }
        if (scope.unknown() == 0) {
            return getTranslation("lookup.help.report.scope.all");
        }
        String key = (scope.inventory() ? "lookup.help.report.scope.inventory." : "lookup.help.report.scope.findings.")
                + (scope.unknown() == 1 ? "one" : "many");
        String said = getTranslation(key, scope.listed(), scope.known(), scope.unknown());
        if (!scope.unknownExamples().isEmpty()) {
            said += " " + getTranslation(scope.unknown() == 1 ? "lookup.help.report.scope.example.one"
                    : "lookup.help.report.scope.example.many", String.join(", ", scope.unknownExamples()));
        }
        return said;
    }
}

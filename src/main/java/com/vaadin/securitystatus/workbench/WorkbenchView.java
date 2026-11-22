package com.vaadin.securitystatus.workbench;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.clipboard.Clipboard;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.NativeForm;
import com.vaadin.flow.component.html.Nav;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.Table;
import com.vaadin.flow.component.html.TableRow;
import com.vaadin.flow.component.markdown.Markdown;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.securitystatus.base.nav.Address;
import com.vaadin.securitystatus.base.nav.QueryFlag;
import com.vaadin.securitystatus.base.security.Roles;
import com.vaadin.securitystatus.base.ui.DataOutageNotice;
import com.vaadin.securitystatus.base.ui.InternalLayout;
import com.vaadin.securitystatus.base.ui.ViewHeading;
import com.vaadin.securitystatus.data.DataRefreshSignal;
import com.vaadin.securitystatus.data.DataUnreachableException;
import com.vaadin.securitystatus.data.SecurityDataService;
import com.vaadin.securitystatus.data.model.VerdictQueue;
import com.vaadin.securitystatus.data.model.VerdictQueue.QueueEntry;
import com.vaadin.securitystatus.workbench.QueueRules.Reach;
import com.vaadin.securitystatus.workbench.QueueRules.Row;
import com.vaadin.securitystatus.workbench.QueueRules.Summary;
import com.vaadin.securitystatus.workbench.VerdictYaml.Handover;

import jakarta.annotation.security.RolesAllowed;

/**
 * The verdict workbench: the queue of {@code data/queue.json} ranked by what
 * one verdict would close, narrowed to one series through {@code ?series},
 * and the form that composes a verdict, with the YAML it produces beside it,
 * the drafts kept in H2 under it, and the handover into GitHub's new file
 * editor, which is where GitHub's sign in and review take over. This screen
 * writes nothing to cve-status itself.
 */
@Route(value = "findings/workbench", layout = InternalLayout.class)
@PageTitle("workbench.title")
@Menu(order = 2, title = "workbench.title")
@RolesAllowed(Roles.SECURITY)
public final class WorkbenchView extends Composite<Div> implements BeforeEnterObserver {

    static final String PATH = "findings/workbench";
    private static final DateTimeFormatter SAVED_AT = new java.time.format.DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_LOCAL_DATE).appendLiteral(' ').appendPattern("HH:mm").toFormatter()
            .withZone(ZoneOffset.UTC);

    private final SecurityDataService data;
    private final VerdictDrafts drafts;

    private final ValueSignal<Optional<String>> asked = new ValueSignal<>(Optional.empty());
    private final ValueSignal<String> yaml = new ValueSignal<>("");
    private final ValueSignal<Optional<QueueEntry>> context = new ValueSignal<>(Optional.empty());
    private final ValueSignal<Optional<Handover>> handover = new ValueSignal<>(Optional.empty());
    private final ValueSignal<List<String>> refused = new ValueSignal<>(List.of());
    private final ValueSignal<Optional<VerdictDraft>> saved = new ValueSignal<>(Optional.empty());
    private final ValueSignal<List<VerdictDraft>> stored = new ValueSignal<>(List.of());

    private final Paragraph lead = new Paragraph();
    private final Nav filter = new Nav();
    private final Div summary = new Div();
    private final Div outage = new Div();
    private final Grid<Row> queue = new Grid<>();
    private final Paragraph elsewhere = new Paragraph();
    private final H2 composing = new H2();
    private final VerdictForm form = new VerdictForm();
    private final Grid<VerdictDraft> draftList = new Grid<>();
    private final Input yamlSource = hidden("yaml-source");
    private final Input filename = hidden("filename");
    private final Input value = hidden("value");
    private final NativeButton open = new NativeButton();
    private final Paragraph reach = new Paragraph();
    private final Paragraph platform = new Paragraph();
    private final Paragraph path = new Paragraph();
    private final Paragraph refusal = new Paragraph();
    private final Paragraph tooLong = new Paragraph();
    private final Paragraph lastSaved = new Paragraph();

    /** What the queue said when last read, kept for the listeners, which may not read signals. */
    private Optional<VerdictQueue> loaded = Optional.empty();

    public WorkbenchView(SecurityDataService data, DataRefreshSignal refresh, VerdictDrafts drafts) {
        this.data = data;
        this.drafts = drafts;
        addClassName("workbench-view");
        lead.addClassName("workbench-lead");
        filter.addClassName("workbench-series");
        filter.getElement().setAttribute("aria-label", getTranslation("workbench.series.label"));
        summary.addClassName("workbench-summary");
        outage.addClassName("workbench-outage");
        configureQueue();

        getContent().add(new ViewHeading(), outage, lead, filter, summary, queue, compose(), draftsSection());

        var generatedAt = refresh.generatedAt();
        Signal<Optional<VerdictQueue>> read = Signal.computed(() -> {
            generatedAt.get();
            return load();
        });
        Signal.effect(this, () -> drawQueue(read.get(), asked.get()));
        Signal.effect(this, () -> drawOutput(yaml.get(), context.get(), handover.get(), refused.get(), saved.get()));
        Signal.effect(this, () -> draftList.setItems(stored.get()));

        form.binder().addValueChangeListener(change -> changed());
        stored.set(drafts.list());
        changed();
    }

    /** The verdict form, for whatever attaches to it later: the form AI controller of epic 11. */
    public VerdictForm form() {
        return form;
    }

    Grid<Row> queue() {
        return queue;
    }

    Grid<VerdictDraft> draftList() {
        return draftList;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        asked.set(Address.first(event.getLocation().getQueryParameters(), QueryFlag.SERIES));
    }

    private Optional<VerdictQueue> load() {
        try {
            return Optional.of(data.queue());
        } catch (DataUnreachableException unreachable) {
            return Optional.empty();
        }
    }

    // The queue

    private void configureQueue() {
        queue.addClassName("workbench-queue");
        queue.addThemeVariants(GridVariant.ROW_STRIPES);
        queue.setAllRowsVisible(false);
        queue.addColumn(Row::closes).setHeader(getTranslation("workbench.col.closes")).setKey("closes")
                .setAutoWidth(true).setFlexGrow(0);
        queue.addColumn(Row::name).setHeader(getTranslation("workbench.col.package")).setKey("package")
                .setFlexGrow(1).setTooltipGenerator(row -> row.entry().purl().orElse(row.name()));
        queue.addColumn(row -> row.here().size()).setHeader(getTranslation("workbench.col.advisories"))
                .setKey("advisories").setAutoWidth(true).setFlexGrow(0);
        queue.addColumn(row -> String.join(", ", row.entry().lines().stream().sorted(QueueRules.NEWEST_FIRST).toList()))
                .setHeader(getTranslation("workbench.col.series")).setKey("series").setFlexGrow(1);
        queue.addColumn(row -> row.entry().origin().filter("spring-boot"::equals)
                .map(origin -> getTranslation("workbench.origin.spring-boot")).orElse(""))
                .setHeader(getTranslation("workbench.col.owner")).setKey("owner").setAutoWidth(true).setFlexGrow(0);
        queue.asSingleSelect().addValueChangeListener(change -> {
            if (change.getValue() != null) {
                choose(change.getValue());
            }
        });
    }

    private void drawQueue(Optional<VerdictQueue> read, Optional<String> wanted) {
        loaded = read;
        outage.removeAll();
        boolean reachable = read.isPresent();
        lead.setVisible(reachable);
        filter.setVisible(reachable);
        summary.setVisible(reachable);
        queue.setVisible(reachable);
        if (!reachable) {
            outage.add(new DataOutageNotice());
            queue.setItems(List.of());
            return;
        }
        VerdictQueue all = read.get();
        Optional<String> line = QueueRules.series(all, wanted);
        List<Row> rows = QueueRules.rows(all, line);
        Summary counts = QueueRules.summary(rows, line);

        String scope = line.map(series -> getTranslation("workbench.scope.series", series))
                .orElseGet(() -> getTranslation("workbench.scope.all"));
        int open = rows.stream().mapToInt(Row::closes).sum();
        if (open > 0) {
            lead.setText(getTranslation("workbench.lead.open", open, scope,
                    rows.stream().filter(row -> row.closes() > 0).count(),
                    Math.round(100.0 * rows.getFirst().closes() / open)));
        } else {
            lead.setText(getTranslation("workbench.lead.none", scope, rows.size()));
        }

        filter.removeAll();
        filter.add(seriesLink(getTranslation("workbench.series.all"), Optional.empty(), line.isEmpty()));
        for (String series : QueueRules.series(all)) {
            int waiting = QueueRules.rows(all, Optional.of(series)).stream().mapToInt(Row::closes).sum();
            filter.add(seriesLink(getTranslation("workbench.series.one", series, waiting), Optional.of(series),
                    line.filter(series::equals).isPresent()));
        }

        summary.removeAll();
        summary.add(summaryLine(counts), table("severity", counts.bySeverity(), true), table("line", counts.byLine(),
                false));
        queue.setItems(rows);
        context.set(entry(all, Optional.of(form.draft().getPurl().trim())));
    }

    private Component seriesLink(String text, Optional<String> series, boolean active) {
        var link = new RouterLink(text, WorkbenchView.class);
        link.setQueryParameters(Address.of(PATH).with(QueryFlag.SERIES, series).query());
        link.addClassName("workbench-series-link");
        series.ifPresent(word -> link.getElement().setAttribute("data-series", word));
        if (active) {
            link.addClassName("active");
            link.getElement().setAttribute("aria-current", "true");
        }
        return link;
    }

    private Component summaryLine(Summary counts) {
        var line = new Paragraph(getTranslation("workbench.summary.total", counts.findings(), counts.packages(),
                counts.unjudged()));
        line.addClassName("workbench-summary-total");
        return line;
    }

    private Component table(String kind, Map<String, int[]> counts, boolean words) {
        var table = new Table();
        table.addClassNames("workbench-summary-table", "workbench-summary-" + kind, "answer-table",
                "vaadin-themed-html");
        table.addHeaderRow(getTranslation("workbench.summary." + kind), getTranslation("workbench.summary.findings"),
                getTranslation("workbench.summary.unjudged"));
        counts.forEach((key, numbers) -> {
            TableRow row = table.addRow();
            row.getElement().setAttribute("data-key", key);
            row.addDataCell(words ? getTranslation("severity." + key) : key);
            row.addDataCell(String.valueOf(numbers[0]));
            row.addDataCell(String.valueOf(numbers[1]));
        });
        return table;
    }

    /** A row chosen: the form opens on its package and advisories, or on the draft already kept for them. */
    private void choose(Row row) {
        Optional<String> line = loaded.flatMap(all -> QueueRules.series(all, asked.peek()));
        List<String> ids = QueueRules.prefill(row, line);
        String purl = row.entry().purl().orElse("");
        String pair = VerdictDraft.pairOf(purl, ids);
        Optional<VerdictDraft> kept = drafts.list().stream()
                .filter(draft -> VerdictDraft.pairOf(draft.getPurl(), draft.advisoryIds()).equals(pair)).findFirst();
        if (kept.isPresent()) {
            restore(kept.get());
            return;
        }
        var fresh = new VerdictDraft();
        fresh.setPurl(purl);
        fresh.setAdvisories(String.join(", ", ids));
        fresh.setVerdictId(QueueRules.proposedId(row.entry(), line, drafts.today()));
        form.edit(fresh);
        saved.set(Optional.empty());
        changed();
    }

    private void restore(VerdictDraft draft) {
        form.edit(draft);
        saved.set(Optional.of(draft));
        changed();
    }

    private static Optional<QueueEntry> entry(VerdictQueue queue, Optional<String> purl) {
        return purl.flatMap(wanted -> queue.packages().stream()
                .filter(entry -> entry.purl().filter(wanted::equals).isPresent()).findFirst());
    }

    // The form and its output

    private Component compose() {
        var section = new Section();
        section.addClassName("workbench-compose");
        composing.addClassName("workbench-composing");
        elsewhere.addClassName("workbench-elsewhere");

        var output = new Div();
        output.addClassName("workbench-output");
        Markdown preview = new Markdown(Signal.computed(() -> fenced(yaml.get())));
        preview.addClassName("workbench-yaml");

        reach.addClassName("workbench-reach");
        platform.addClassName("workbench-platform");
        path.addClassName("workbench-path");
        refusal.addClassName("workbench-refused");
        tooLong.addClassName("workbench-too-long");
        lastSaved.addClassName("workbench-saved");

        var save = new Button(getTranslation("workbench.save"), click -> save());
        save.addClassName("workbench-save");
        var export = new Button(getTranslation("workbench.export"), click -> export());
        export.addClassName("workbench-export");
        export.addThemeVariants(ButtonVariant.PRIMARY);
        var copy = new Button(getTranslation("workbench.copy"));
        copy.addClassName("workbench-copy");
        Clipboard.onClick(copy).writeText(yamlSource, copied -> {
            var done = Notification.show(getTranslation("workbench.copy.done"));
            done.addThemeVariants(NotificationVariant.SUCCESS);
        }, failure -> {
            var failed = Notification.show(getTranslation("workbench.copy.failed"));
            failed.addThemeVariants(NotificationVariant.ERROR);
        });

        open.setText(getTranslation("workbench.github"));
        open.setType(NativeButton.Type.SUBMIT);
        open.addClassName("workbench-github");
        var github = new NativeForm(filename, value, open);
        github.addClassName("workbench-handover");
        github.setMethod(NativeForm.Method.GET);
        github.setAction(VerdictYaml.EDITOR);
        github.setTarget(AnchorTarget.BLANK);
        github.getElement().setAttribute("rel", "noopener");

        var actions = new Div(save, export, copy, github);
        actions.addClassName("workbench-actions");
        output.add(preview, reach, platform, path, actions, refusal, tooLong, lastSaved, yamlSource);

        var columns = new Div(form, output);
        columns.addClassName("workbench-columns");
        section.add(composing, elsewhere, columns);
        return section;
    }

    /** Any edit: the YAML follows it, and an export already approved no longer says what is on screen. */
    private void changed() {
        VerdictDraft draft = form.draft();
        String text = VerdictYaml.of(draft, drafts.today());
        yaml.set(text);
        yamlSource.setValue(text);
        handover.set(Optional.empty());
        refused.set(List.of());
        context.set(loaded.flatMap(all -> entry(all, Optional.of(draft.getPurl().trim()))));
    }

    private void drawOutput(String text, Optional<QueueEntry> entry, Optional<Handover> approved, List<String> missing,
            Optional<VerdictDraft> last) {
        VerdictDraft draft = form.draft();
        List<String> ids = draft.advisoryIds();
        Optional<String> line = loaded.flatMap(all -> QueueRules.series(all, asked.peek()));

        composing.setText(entry.map(QueueEntry::packageName)
                .map(name -> getTranslation("workbench.composing", name))
                .orElseGet(() -> getTranslation("workbench.composing.none")));

        Optional<Row> row = entry.flatMap(found -> loaded.flatMap(all -> QueueRules.rows(all, line).stream()
                .filter(candidate -> candidate.entry() == found).findFirst()));
        int outside = row.map(Row::elsewhere).orElse(0);
        elsewhere.setVisible(line.isPresent() && outside > 0);
        line.ifPresent(series -> elsewhere.setText(getTranslation("workbench.elsewhere", series, outside)));

        Optional<Reach> reached = entry.map(found -> QueueRules.reach(found, ids)).filter(r -> r.advisories() > 0);
        reach.setVisible(reached.isPresent());
        reached.ifPresent(r -> {
            String said = getTranslation("workbench.reach", r.advisories(), r.lines().size(),
                    String.join(", ", r.lines()), r.findings(), r.unjudged());
            String applicability = draft.getApplicability();
            if (QueueRules.silencing(applicability)) {
                said += " " + getTranslation("workbench.reach.silences");
            } else if ("risk".equals(applicability)) {
                said += " " + getTranslation("workbench.reach.risk");
            }
            reach.setText(said);
        });

        List<String> red = entry.filter(found -> QueueRules.silencing(draft.getApplicability()))
                .map(QueueRules::stillRed).orElse(List.of());
        platform.setVisible(!red.isEmpty());
        platform.setText(getTranslation("workbench.platform", String.join(", ", red)));

        path.setVisible(!draft.getVerdictId().isBlank());
        path.setText(getTranslation("workbench.path", VerdictYaml.path(draft)));

        refusal.setVisible(!missing.isEmpty());
        refusal.setText(getTranslation("workbench.refused", String.join(", ", missing), missing.size()));

        boolean fits = approved.map(Handover::fits).orElse(false);
        filename.setValue(fits ? approved.get().path() : "");
        value.setValue(fits ? approved.get().yaml() : "");
        open.setEnabled(fits);
        tooLong.setVisible(approved.isPresent() && !fits);
        approved.ifPresent(found -> tooLong.setText(getTranslation("workbench.too-long", found.url().length(),
                VerdictYaml.URL_CAP, found.path())));

        lastSaved.setVisible(last.isPresent());
        last.ifPresent(kept -> lastSaved.setText(getTranslation("workbench.saved", kept.getSavedBy(),
                kept.getUpdatedAt() == null ? "" : SAVED_AT.format(kept.getUpdatedAt()))));
    }

    private void save() {
        if (!form.validForDraft()) {
            return;
        }
        VerdictDraft kept = drafts.save(form.draft());
        Optional<Handover> approved = handover.peek();
        form.edit(kept);
        stored.set(drafts.list());
        saved.set(Optional.of(kept));
        changed();
        // Saving is no edit: an export approved before it still says what is on screen.
        handover.set(approved);
        var done = Notification.show(getTranslation("workbench.save.done"));
        done.addThemeVariants(NotificationVariant.SUCCESS);
    }

    private void export() {
        List<String> missing = form.missingForReady();
        if (!missing.isEmpty()) {
            handover.set(Optional.empty());
            refused.set(missing);
            return;
        }
        refused.set(List.of());
        handover.set(Optional.of(drafts.handover(form.draft())));
    }

    private static String fenced(String text) {
        String fence = "```";
        while (text.contains(fence)) {
            fence += "`";
        }
        return fence + "yaml\n" + text + (text.endsWith("\n") ? "" : "\n") + fence;
    }

    // The drafts

    private Component draftsSection() {
        var section = new Section();
        section.addClassName("workbench-drafts");
        var title = new H2(getTranslation("workbench.drafts"));
        var none = new Paragraph(getTranslation("workbench.drafts.none"));
        none.addClassName("workbench-drafts-none");
        none.bindVisible(Signal.computed(() -> stored.get().isEmpty()));
        draftList.addClassName("workbench-draft-list");
        draftList.setAllRowsVisible(true);
        draftList.bindVisible(Signal.computed(() -> !stored.get().isEmpty()));
        draftList.addColumn(VerdictDraft::getVerdictId).setHeader(getTranslation("workbench.drafts.id"))
                .setKey("id").setFlexGrow(1);
        draftList.addColumn(draft -> String.join(", ", draft.advisoryIds()))
                .setHeader(getTranslation("workbench.drafts.advisories")).setKey("advisories").setFlexGrow(1);
        draftList.addColumn(VerdictDraft::getPurl).setHeader(getTranslation("workbench.drafts.purl")).setKey("purl")
                .setFlexGrow(1);
        draftList.addColumn(VerdictDraft::getSavedBy).setHeader(getTranslation("workbench.drafts.by")).setKey("by")
                .setAutoWidth(true).setFlexGrow(0);
        draftList.addColumn(draft -> draft.getUpdatedAt() == null ? "" : SAVED_AT.format(draft.getUpdatedAt()))
                .setHeader(getTranslation("workbench.drafts.when")).setKey("when").setAutoWidth(true).setFlexGrow(0);
        draftList.addItemClickListener(click -> restore(click.getItem()));
        section.add(title, none, draftList);
        return section;
    }

    private static Input hidden(String name) {
        var input = new Input();
        input.setType("hidden");
        input.getElement().setAttribute("name", name);
        input.addClassName("workbench-input-" + name);
        return input;
    }
}

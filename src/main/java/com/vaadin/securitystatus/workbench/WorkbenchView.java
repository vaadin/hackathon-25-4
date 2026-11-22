package com.vaadin.securitystatus.workbench;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.clipboard.Clipboard;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Code;
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
import com.vaadin.securitystatus.base.ai.AiSurfaces;
import com.vaadin.securitystatus.base.nav.Address;
import com.vaadin.securitystatus.base.nav.QueryFlag;
import com.vaadin.securitystatus.base.security.Roles;
import com.vaadin.securitystatus.base.ui.DataOutageNotice;
import com.vaadin.securitystatus.base.ui.InternalLayout;
import com.vaadin.securitystatus.data.DataRefreshSignal;
import com.vaadin.securitystatus.data.DataUnreachableException;
import com.vaadin.securitystatus.data.SecurityDataService;
import com.vaadin.securitystatus.data.model.Assessment;
import com.vaadin.securitystatus.data.model.VerdictQueue;
import com.vaadin.securitystatus.data.model.VerdictQueue.QueueEntry;
import com.vaadin.securitystatus.data.model.VerdictQueue.QueuedAdvisory;
import com.vaadin.securitystatus.explorer.ExplorerView;
import com.vaadin.securitystatus.workbench.QueueRules.Reach;
import com.vaadin.securitystatus.workbench.QueueRules.Row;
import com.vaadin.securitystatus.workbench.QueueRules.Summary;
import com.vaadin.securitystatus.workbench.VerdictYaml.Handover;

import jakarta.annotation.security.RolesAllowed;

/**
 * The verdict queue, in the prototype's distribution, D-220 to D-222: the
 * lead, the series as chips held in {@code ?series}, and one row per package
 * ranked by what one verdict would close. A row opened shows what the queue
 * knows about the package, the evidence a verdict is written from, and the
 * fold that writes one: the form, filled from a verdict the package already
 * carries or from the ticks of its advisories, with the YAML beside it, the
 * handover into GitHub's new file editor, and the drafts kept in H2. One row
 * is open at a time, because the form is one; with no row open it waits at the
 * foot of the page for a package typed by hand. This screen writes nothing to
 * cve-status itself.
 */
@Route(value = "findings/workbench", layout = InternalLayout.class)
@PageTitle("workbench.title")
@Menu(order = 2, title = "workbench.menu")
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
    private final Paragraph hint = new Paragraph();
    private final Nav filter = new Nav();
    private final Div outage = new Div();
    private final Div queue = new Div();
    private final Details summaryFold = new Details();
    private final Div summary = new Div();
    private final Paragraph footer = new Paragraph();

    private final Section composer = new Section();
    private final Details home = new Details();
    private final H2 composing = new H2();
    private final Div reuse = new Div();
    private final Paragraph filled = new Paragraph();
    private final Paragraph narrow = new Paragraph();
    private final Div picks = new Div();
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

    /** The paste box of the form, when the assistant is there to offer it. */
    private final ObjectProvider<AiSurfaces> ai;

    /** What the queue said when last read, kept for the listeners, which may not read signals. */
    private Optional<VerdictQueue> loaded = Optional.empty();
    /** The rows drawn, by package, in their order. */
    private final Map<String, Details> drawn = new LinkedHashMap<>();
    private final Map<String, Row> rowsByName = new LinkedHashMap<>();
    /** The row the form belongs to, when one is open. */
    private @Nullable String openRow;
    /** The ticks of the advisories of the row the form belongs to, by id. */
    private final Map<String, Checkbox> ticks = new LinkedHashMap<>();

    public WorkbenchView(SecurityDataService data, DataRefreshSignal refresh, VerdictDrafts drafts,
            ObjectProvider<AiSurfaces> ai) {
        this.data = data;
        this.drafts = drafts;
        this.ai = ai;
        addClassName("workbench-view");
        lead.addClassName("workbench-lead");
        hint.addClassNames("workbench-hint", "workbench-intro");
        hint.setText(getTranslation("workbench.hint"));
        filter.addClassName("workbench-series");
        filter.getElement().setAttribute("aria-label", getTranslation("workbench.series.label"));
        outage.addClassName("workbench-outage");
        queue.addClassName("workbench-queue");
        summary.addClassName("workbench-summary");
        summaryFold.setSummaryText(getTranslation("workbench.summary.title"));
        summaryFold.addClassName("workbench-summary-fold");
        summaryFold.add(summary);
        footer.addClassName("workbench-footer");
        footer.setText(getTranslation("workbench.footer"));

        buildComposer();
        home.addClassNames("workbench-home", "workbench-compose-fold");
        home.setSummary(foldSummary(getTranslation("workbench.home")));
        home.add(composer);

        getContent().add(backLink(), outage, lead, hint, filter, queue, home, summaryFold, footer, draftsSection());

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

    private static RouterLink backLink() {
        var back = new RouterLink(ExplorerView.class);
        back.add(com.vaadin.flow.component.icon.VaadinIcon.ANGLE_LEFT.create(),
                new Span(back.getTranslation("release.back")));
        back.addClassName("release-back");
        return back;
    }

    /** The verdict form, for whatever attaches to it later: the form AI controller of epic 11. */
    public VerdictForm form() {
        return form;
    }

    /** The rows shown, in their order, for the tests. */
    List<Row> rows() {
        return List.copyOf(rowsByName.values());
    }

    /** The row of a package as drawn, for the tests. */
    Details row(String name) {
        return drawn.get(name);
    }

    /** Opens the row of a package, as a click on it does: the form moves into it, prefilled. */
    void open(String name) {
        Details row = drawn.get(name);
        if (row != null) {
            row.setOpened(true);
            opened(name, true);
        }
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

    private Optional<String> line() {
        return loaded.flatMap(all -> QueueRules.series(all, asked.peek()));
    }

    // The queue

    private void drawQueue(Optional<VerdictQueue> read, Optional<String> wanted) {
        loaded = read;
        outage.removeAll();
        goHome();
        queue.removeAll();
        drawn.clear();
        rowsByName.clear();
        boolean reachable = read.isPresent();
        lead.setVisible(reachable);
        hint.setVisible(reachable);
        filter.setVisible(reachable);
        summaryFold.setVisible(reachable);
        if (!reachable) {
            outage.add(new DataOutageNotice());
            return;
        }
        VerdictQueue all = read.get();
        Optional<String> line = QueueRules.series(all, wanted);
        List<Row> rows = QueueRules.rows(all, line);
        Summary counts = QueueRules.summary(rows, line);

        String scope = line.map(series -> getTranslation("workbench.scope.series", series))
                .orElseGet(() -> getTranslation("workbench.scope.all"));
        int waiting = rows.stream().mapToInt(Row::closes).sum();
        if (waiting > 0) {
            lead.setText(getTranslation("workbench.lead.open", waiting, scope,
                    rows.stream().filter(row -> row.closes() > 0).count(),
                    Math.round(100.0 * rows.getFirst().closes() / waiting)));
        } else {
            lead.setText(getTranslation("workbench.lead.none", scope, rows.size()));
        }

        filter.removeAll();
        filter.add(seriesLink(getTranslation("workbench.series.all"), Optional.empty(), line.isEmpty(), null));
        for (String series : QueueRules.series(all)) {
            int open = QueueRules.rows(all, Optional.of(series)).stream().mapToInt(Row::closes).sum();
            filter.add(seriesLink(getTranslation("workbench.series.one", series, String.valueOf(open)),
                    Optional.of(series), line.filter(series::equals).isPresent(),
                    getTranslation("workbench.series.title", open, series)));
        }

        for (Row row : rows) {
            rowsByName.put(row.name(), row);
            Details details = packageRow(row);
            drawn.put(row.name(), details);
            queue.add(details);
        }

        summary.removeAll();
        summary.add(summaryLine(counts), table("severity", counts.bySeverity(), true),
                table("line", counts.byLine(), false));
        context.set(entry(all, Optional.of(form.draft().getPurl().trim())));
    }

    private Component seriesLink(String text, Optional<String> series, boolean active, @Nullable String title) {
        var link = new RouterLink(text, WorkbenchView.class);
        link.setQueryParameters(Address.of(PATH).with(QueryFlag.SERIES, series).query());
        link.addClassName("workbench-series-link");
        series.ifPresent(word -> link.getElement().setAttribute("data-series", word));
        if (title != null) {
            link.getElement().setAttribute("title", title);
        }
        if (active) {
            link.addClassName("active");
            link.getElement().setAttribute("aria-current", "true");
        }
        return link;
    }

    /** One package, as the prototype's row: what it would close, its name, its state and what it holds. */
    private Details packageRow(Row row) {
        QueueEntry entry = row.entry();
        var summaryLine = new Div();
        summaryLine.addClassName("workbench-row-summary");
        var closes = new Span(String.valueOf(row.closes()));
        closes.addClassName("workbench-row-closes");
        var name = new Span(row.name());
        name.addClassName("workbench-row-package");
        name.getElement().setAttribute("title", entry.purl().orElse(row.name()));
        boolean todo = row.closes() > 0;
        var state = new Span(getTranslation(todo ? "workbench.row.todo" : "workbench.row.done"));
        state.addClassNames("workbench-row-state", todo ? "todo" : "done");
        List<String> lines = entry.lines().stream().sorted(QueueRules.NEWEST_FIRST).toList();
        String says = getTranslation("workbench.row.says", entry.cves().size(), lines.size(), String.join(", ", lines))
                + (entry.origin().filter("spring-boot"::equals).isPresent()
                        ? getTranslation("workbench.row.spring")
                        : "");
        var holds = new Span(says);
        holds.addClassName("workbench-row-says");
        summaryLine.add(closes, name, state, holds);

        var details = new Details();
        details.setSummary(summaryLine);
        details.addClassNames("workbench-row", todo ? "state-todo" : "state-done");
        details.getElement().setAttribute("data-package", row.name());
        details.addOpenedChangeListener(change -> {
            if (change.isFromClient()) {
                opened(row.name(), change.isOpened());
            }
        });
        return details;
    }

    /** A row opened or shut: one at a time, the form following the row that is open. */
    private void opened(String name, boolean isOpen) {
        if (!isOpen) {
            if (name.equals(openRow)) {
                goHome();
            }
            return;
        }
        if (openRow != null && !openRow.equals(name)) {
            Details previous = drawn.get(openRow);
            if (previous != null) {
                previous.setOpened(false);
            }
        }
        goHome();
        Row row = rowsByName.get(name);
        Details details = drawn.get(name);
        if (row == null || details == null) {
            return;
        }
        openRow = name;
        details.add(rowContent(row));
        choose(row);
    }

    /** The form back at the foot of the page, and the row it was in emptied. */
    private void goHome() {
        if (openRow != null) {
            Details previous = drawn.get(openRow);
            if (previous != null) {
                previous.getContent().toList().forEach(previous::remove);
            }
        }
        openRow = null;
        ticks.clear();
        picks.removeAll();
        picks.setVisible(false);
        reuse.removeAll();
        reuse.setVisible(false);
        filled.setVisible(false);
        narrow.setVisible(false);
        home.setVisible(true);
        if (composer.getParent().filter(home::equals).isEmpty()) {
            home.add(composer);
        }
    }

    private Component rowContent(Row row) {
        var content = new Div();
        content.addClassName("workbench-row-content");
        acknowledged(row).ifPresent(content::add);
        Optional<String> line = line();
        if (line.isPresent() && row.elsewhere() > 0) {
            content.add(paragraph("workbench-elsewhere", getTranslation("workbench.elsewhere", line.get(),
                    row.elsewhere())));
        }
        content.add(evidence(row.entry()));
        var fold = new Details();
        fold.addClassName("workbench-compose-fold");
        fold.setSummary(foldSummary(getTranslation("workbench.compose")));
        fold.add(composer);
        content.add(fold);
        home.setVisible(false);
        return content;
    }

    private Component foldSummary(String label) {
        var title = new Span(label);
        title.addClassName("workbench-fold-label");
        var says = new Span(getTranslation("workbench.compose.says"));
        says.addClassName("workbench-fold-says");
        var summaryLine = new Span(title, says);
        summaryLine.addClassName("workbench-fold-summary");
        return summaryLine;
    }

    /**
     * What the platform branches already acknowledge for this package, and
     * which of them nobody could read, in the prototype's words.
     */
    private Optional<Component> acknowledged(Row row) {
        QueueEntry entry = row.entry();
        var listed = new ArrayList<String>();
        var unread = new ArrayList<String>();
        entry.acknowledged().entrySet().stream()
                .sorted(Map.Entry.comparingByKey(QueueRules.NEWEST_FIRST)).forEach(branch -> {
                    if (branch.getValue() == null) {
                        unread.add(branch.getKey());
                    } else if (!branch.getValue().isEmpty()) {
                        listed.add(getTranslation("workbench.acknowledged.branch", branch.getKey(),
                                branch.getValue().size(), entry.cves().size()));
                    }
                });
        if (listed.isEmpty() && unread.isEmpty()) {
            return Optional.empty();
        }
        var text = new StringBuilder();
        if (!listed.isEmpty()) {
            text.append(getTranslation("workbench.acknowledged", String.join(", ", listed)));
        }
        if (!unread.isEmpty()) {
            if (!text.isEmpty()) {
                text.append(' ');
            }
            text.append(getTranslation("workbench.unread", String.join(", ", unread), unread.size()));
        }
        return Optional.of(paragraph("workbench-acknowledged", text.toString()));
    }

    /**
     * The evidence a verdict is written from, D-221: what the package
     * resolves to on each series, the chains that bring it, the CPEs a scanner
     * matched, who reported it, what upstream did, and the frontend trap.
     */
    private Component evidence(QueueEntry entry) {
        var box = new Div();
        box.addClassName("workbench-evidence");

        var byVersion = new LinkedHashMap<String, List<String>>();
        entry.resolved().forEach((series, versions) -> byVersion
                .computeIfAbsent(String.join(", ", versions), key -> new ArrayList<>()).add(series));
        if (!byVersion.isEmpty()) {
            box.add(label("workbench.evidence.resolves"));
            byVersion.forEach((versions, series) -> box.add(pkgLine(versions, getTranslation("workbench.evidence.on",
                    String.join(", ", series.stream().sorted(QueueRules.NEWEST_FIRST).toList())))));
        }
        if (!entry.routes().isEmpty()) {
            box.add(label("workbench.evidence.routes"));
            for (VerdictQueue.Route route : entry.routes()) {
                var hops = new ArrayList<String>();
                route.entry().ifPresent(first -> {
                    if (route.chain().isEmpty() || !route.chain().getFirst().equals(first)) {
                        hops.add(first);
                    }
                });
                hops.addAll(route.chain());
                box.add(pkgLine(String.join(" → ", hops), route.declaredBy()
                        .map(by -> getTranslation("workbench.evidence.declared", by)).orElse(null)));
            }
        }
        if (!entry.cpes().isEmpty()) {
            box.add(label("workbench.evidence.cpes"));
            var cpes = new Div();
            cpes.addClassName("workbench-pkg");
            entry.cpes().forEach(cpe -> cpes.add(new Code(cpe)));
            box.add(cpes, paragraph("workbench-hint", getTranslation("workbench.evidence.cpes.hint")));
        }
        if (!entry.scanners().isEmpty()) {
            box.add(paragraph("workbench-hint", getTranslation("workbench.evidence.scanners",
                    String.join(", ", entry.scanners()))));
        }
        long fixed = entry.cves().stream().filter(advisory -> !advisory.fixedUpstream().isEmpty()).count();
        long absent = entry.cves().stream().filter(advisory -> !advisory.absentUpstream().isEmpty()).count();
        if (fixed > 0 || absent > 0) {
            var said = new ArrayList<String>();
            if (fixed > 0) {
                said.add(getTranslation("workbench.evidence.fixed", (int) fixed));
            }
            if (absent > 0) {
                said.add(getTranslation("workbench.evidence.absent", (int) absent));
            }
            box.add(paragraph("workbench-hint", getTranslation("workbench.evidence.batch", entry.cves().size(),
                    String.join(getTranslation("workbench.evidence.and"), said))));
        }
        if (entry.ecosystem().filter(word -> "npm".equals(word) || "javascript".equals(word)).isPresent()) {
            box.add(paragraph("workbench-hint", getTranslation("workbench.evidence.frontend")));
        }
        return box;
    }

    private Component label(String key) {
        var label = new Span(getTranslation(key));
        label.addClassName("workbench-field-label");
        return label;
    }

    private static Component pkgLine(String code, @Nullable String through) {
        var line = new Div();
        line.addClassName("workbench-pkg");
        line.add(new Code(code));
        if (through != null) {
            var span = new Span(through);
            span.addClassName("workbench-through");
            line.add(span);
        }
        return line;
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

    /**
     * A row chosen: the form opens on its package and advisories, or on the
     * draft already kept for them; with exactly one verdict carried by the
     * package and no draft, it arrives filled from that verdict, D-222.
     */
    private void choose(Row row) {
        Optional<String> line = line();
        List<String> ids = QueueRules.prefill(row, line);
        String purl = row.entry().purl().orElse("");
        String pair = VerdictDraft.pairOf(purl, ids);
        Optional<VerdictDraft> kept = drafts.list().stream()
                .filter(draft -> VerdictDraft.pairOf(draft.getPurl(), draft.advisoryIds()).equals(pair)).findFirst();
        drawReuse(row);
        drawPicks(row, line);
        if (kept.isPresent()) {
            restore(kept.get());
            syncTicks();
            return;
        }
        var fresh = new VerdictDraft();
        fresh.setPurl(purl);
        fresh.setAdvisories(String.join(", ", ids));
        fresh.setVerdictId(QueueRules.proposedId(row.entry(), line, drafts.today()));
        form.edit(fresh);
        saved.set(Optional.empty());
        changed();
        List<String> carried = carried(row);
        if (carried.size() == 1) {
            fillFrom(row, carried.getFirst());
        }
        syncTicks();
    }

    /** The verdicts this package already carries, as the queue publishes them, in the order first met. */
    private List<String> carried(Row row) {
        Map<String, Assessment> written = loaded.map(VerdictQueue::assessments).orElse(Map.of());
        var ids = new LinkedHashSet<String>();
        row.entry().cves().forEach(advisory -> advisory.assessments().stream().filter(written::containsKey)
                .forEach(ids::add));
        return List.copyOf(ids);
    }

    private void drawReuse(Row row) {
        reuse.removeAll();
        List<String> carried = carried(row);
        reuse.setVisible(!carried.isEmpty());
        if (carried.isEmpty()) {
            return;
        }
        Map<String, Assessment> written = loaded.map(VerdictQueue::assessments).orElse(Map.of());
        reuse.add(label("workbench.reuse"));
        var chips = new Div();
        chips.addClassName("workbench-reuse-chips");
        for (String id : carried) {
            var chip = new Button(getTranslation("workbench.reuse.fill", id), click -> fillFrom(row, id));
            chip.addThemeVariants(ButtonVariant.TERTIARY);
            chip.addClassName("workbench-reuse-chip");
            chip.getElement().setAttribute("data-verdict", id);
            Optional.ofNullable(written.get(id)).flatMap(Assessment::applicability).ifPresent(word -> {
                var applicability = new Span(word);
                applicability.addClassName("workbench-reuse-applicability");
                chip.setSuffixComponent(applicability);
            });
            chips.add(chip);
        }
        reuse.add(chips);
    }

    /**
     * Fills the form from a verdict the package already carries, D-222: its
     * words, its range and its promises, the depth back to read, not run,
     * {@code supersedes} pointing at it, and the advisories it answered ticked
     * beside the ones already ticked. The old file is never written over.
     */
    void fillFrom(Row row, String id) {
        Assessment old = loaded.map(VerdictQueue::assessments).map(all -> all.get(id)).orElse(null);
        if (old == null) {
            return;
        }
        VerdictDraft draft = form.draft().copy();
        old.applicability().filter(VerdictDraft.APPLICABILITY::contains).ifPresent(draft::setApplicability);
        draft.setDepth("cursory");
        draft.setAffects(old.affects().filter(range -> !"*".equals(range.trim())).orElse(""));
        old.assessedBy().ifPresent(draft::setAssessedBy);
        draft.setCommentary(old.commentary().orElse(""));
        draft.setImpact(old.impact().orElse(""));
        draft.setWorkaround(old.workaround().orElse(""));
        draft.setFixExpected(old.fixExpected().orElse(""));
        var promises = new ArrayList<String>();
        old.fixExpectedVersion().forEach((series, version) -> promises.add(series + ": " + version));
        draft.setFixExpectedVersion(String.join(", ", promises));
        draft.setFixPrs(String.join(" ", old.fixPrs()));
        draft.setSupersedes(id);
        var ids = new LinkedHashSet<>(draft.advisoryIds());
        Set<String> answered = Set.copyOf(old.cves());
        row.entry().cves().stream().map(QueuedAdvisory::cve).filter(answered::contains).forEach(ids::add);
        draft.setAdvisories(String.join(", ", ordered(row, ids)));
        form.edit(draft);
        saved.set(Optional.empty());
        changed();
        syncTicks();

        filled.setText(getTranslation("workbench.filled", id, old.cves().size()));
        filled.setVisible(true);
        Optional<String> line = line();
        boolean reaches = line.isEmpty() || row.entry().cves().stream()
                .anyMatch(advisory -> id.equals(advisory.saidByLine().get(line.get())));
        narrow.setVisible(!reaches);
        line.ifPresent(series -> narrow.setText(getTranslation("workbench.filled.narrow",
                old.affects().orElse("*"), series)));
    }

    /** Ids in the row's order first, then any typed by hand, in the order typed. */
    private static List<String> ordered(Row row, Set<String> ids) {
        var out = new ArrayList<String>();
        row.entry().cves().stream().map(QueuedAdvisory::cve).filter(ids::contains).forEach(out::add);
        ids.stream().filter(id -> !out.contains(id)).forEach(out::add);
        return out;
    }

    /**
     * One tick per advisory of the package, with its severity, its series and
     * what is known of it, D-221. The ticks write the advisories field; ids
     * typed there by hand are kept.
     */
    private void drawPicks(Row row, Optional<String> line) {
        picks.removeAll();
        ticks.clear();
        picks.setVisible(true);
        picks.add(label("workbench.picks"));
        for (QueuedAdvisory advisory : row.entry().cves()) {
            var tick = new Checkbox(advisory.cve());
            tick.addClassName("workbench-pick-box");
            tick.addValueChangeListener(change -> {
                if (change.isFromClient()) {
                    picked(row);
                }
            });
            ticks.put(advisory.cve(), tick);
            var pick = new Div(tick);
            pick.addClassName("workbench-pick");
            pick.getElement().setAttribute("data-cve", advisory.cve());
            advisory.severity().ifPresent(word -> {
                var severity = new Span(getTranslation("severity." + word));
                severity.addClassNames("explorer-severity", "severity-" + word);
                pick.add(severity);
            });
            var series = new Span(String.join(", ", advisory.lines().stream().sorted(QueueRules.NEWEST_FIRST).toList()));
            series.addClassName("workbench-through");
            pick.add(series);
            List<String> said = said(advisory, line);
            if (!said.isEmpty()) {
                var box = new Span();
                box.addClassName("workbench-said");
                said.forEach(text -> {
                    var one = new Span(text);
                    one.addClassName("workbench-said-one");
                    box.add(one);
                });
                pick.add(box);
            }
            picks.add(pick);
        }
    }

    /** What is said about one advisory beside its tick, in the prototype's words. */
    private List<String> said(QueuedAdvisory advisory, Optional<String> line) {
        var out = new ArrayList<String>();
        var byVersion = new java.util.TreeMap<String, List<String>>();
        advisory.fixedUpstream().forEach((series, version) -> byVersion.computeIfAbsent(version,
                key -> new ArrayList<>()).add(series));
        if (byVersion.size() == 1) {
            out.add(getTranslation("workbench.said.fixed", byVersion.firstKey()));
        } else if (byVersion.size() > 1) {
            var parts = new ArrayList<String>();
            byVersion.forEach((version, series) -> parts.add(getTranslation("workbench.said.fixed.on", version,
                    String.join(", ", series.stream().sorted(QueueRules.NEWEST_FIRST).toList()))));
            out.add(getTranslation("workbench.said.fixed", String.join("; ", parts)));
        }
        if (!advisory.absentUpstream().isEmpty()) {
            boolean everywhere = advisory.absentUpstream().size() == advisory.lines().size();
            String why = whyAbsent(advisory);
            out.add(everywhere ? getTranslation("workbench.said.absent.all", why)
                    : getTranslation("workbench.said.absent.some", String.join(", ", advisory.absentUpstream()), why));
        }
        if (Boolean.TRUE.equals(advisory.gitOnly())) {
            out.add(getTranslation("workbench.said.git"));
        }
        if (!QueueRules.unjudged(advisory, line)) {
            String answer = QueueRules.verdictOn(advisory, line).orElse("");
            Optional<String> where = line.map(advisory.saidByLine()::get)
                    .or(() -> advisory.assessments().stream().findFirst());
            out.add(where.map(found -> getTranslation("workbench.said.already.in", answer, found))
                    .orElseGet(() -> getTranslation("workbench.said.already", answer)));
        } else if (!advisory.assessments().isEmpty()) {
            out.add(getTranslation("workbench.said.elsewhere", String.join(", ", advisory.assessments())));
        }
        return out;
    }

    private String whyAbsent(QueuedAdvisory advisory) {
        String about = String.join(getTranslation("workbench.said.and"),
                advisory.absentAbout().stream().limit(2).toList());
        return switch (advisory.absentReason().orElse("")) {
            case "other_package" -> about.isEmpty() ? getTranslation("workbench.why.other_package")
                    : getTranslation("workbench.why.other_package.about", about);
            case "other_versions" -> getTranslation("workbench.why.other_versions");
            case "commit_range" -> getTranslation("workbench.why.commit_range");
            case "no_record" -> getTranslation("workbench.why.no_record");
            default -> getTranslation("workbench.why.unlisted");
        };
    }

    /** The ticks written into the advisories field: the row's in its order, then the ones typed by hand. */
    private void picked(Row row) {
        var ids = new LinkedHashSet<String>();
        ticks.forEach((id, tick) -> {
            if (tick.getValue()) {
                ids.add(id);
            }
        });
        form.draft().advisoryIds().stream().filter(id -> !ticks.containsKey(id)).forEach(ids::add);
        VerdictDraft draft = form.draft().copy();
        draft.setAdvisories(String.join(", ", ordered(row, ids)));
        form.edit(draft);
        changed();
    }

    /** The ticks following the advisories field. */
    private void syncTicks() {
        Set<String> ids = Set.copyOf(form.draft().advisoryIds());
        ticks.forEach((id, tick) -> {
            boolean on = ids.contains(id);
            if (tick.getValue() != on) {
                tick.setValue(on);
            }
        });
    }

    private void restore(VerdictDraft draft) {
        form.edit(draft);
        saved.set(Optional.of(draft));
        changed();
        syncTicks();
    }

    private static Optional<QueueEntry> entry(VerdictQueue queue, Optional<String> purl) {
        return purl.flatMap(wanted -> queue.packages().stream()
                .filter(entry -> entry.purl().filter(wanted::equals).isPresent()).findFirst());
    }

    // The form and its output

    private void buildComposer() {
        composer.addClassName("workbench-compose");
        composing.addClassName("workbench-composing");
        reuse.addClassName("workbench-reuse");
        filled.addClassName("workbench-filled");
        narrow.addClassName("workbench-narrow");
        picks.addClassName("workbench-picks");

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
        var check = paragraph("workbench-check", getTranslation("workbench.check"));
        output.add(preview, reach, platform, path, actions, refusal, tooLong, lastSaved, check, yamlSource);

        // The paste box sits above the form, in the form's column: it fills the
        // form and nothing else, and saving stays the Save button's alone.
        var formColumn = new Div();
        formColumn.addClassName("workbench-form-column");
        ai.ifAvailable(surfaces -> formColumn.add(surfaces.formFill(form.layout(), form.binder(), form.personOnly())));
        formColumn.add(picks, form);
        var columns = new Div(formColumn, output);
        columns.addClassName("workbench-columns");
        composer.add(composing, reuse, filled, narrow, columns);
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
        syncTicks();
    }

    private void drawOutput(String text, Optional<QueueEntry> entry, Optional<Handover> approved, List<String> missing,
            Optional<VerdictDraft> last) {
        VerdictDraft draft = form.draft();
        List<String> ids = draft.advisoryIds();

        composing.setText(entry.map(QueueEntry::packageName)
                .map(name -> getTranslation("workbench.composing", name))
                .orElseGet(() -> getTranslation("workbench.composing.none")));

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
        platform.setText(getTranslation("workbench.platform", String.join(", ", red), red.size()));

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
        draftList.addItemClickListener(click -> {
            // A draft restored belongs to no row; the form waits at the foot of the page.
            if (openRow != null) {
                Details row = drawn.get(openRow);
                if (row != null) {
                    row.setOpened(false);
                }
                goHome();
            }
            restore(click.getItem());
            home.setOpened(true);
        });
        section.add(title, none, draftList);
        return section;
    }

    private static Paragraph paragraph(String className, String text) {
        var paragraph = new Paragraph(text);
        paragraph.addClassName(className);
        return paragraph;
    }

    private static Input hidden(String name) {
        var input = new Input();
        input.setType("hidden");
        input.getElement().setAttribute("name", name);
        input.addClassName("workbench-input-" + name);
        return input;
    }
}

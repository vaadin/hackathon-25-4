package com.vaadin.securitystatus.status;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Switch;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.Table;
import com.vaadin.flow.component.html.TableRow;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouteParent;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.securitystatus.base.nav.Address;
import com.vaadin.securitystatus.base.nav.QueryFlag;
import com.vaadin.securitystatus.base.ui.DataOutageNotice;
import com.vaadin.securitystatus.base.ui.Downloads;
import com.vaadin.securitystatus.base.ui.PdfPreview;
import com.vaadin.securitystatus.base.ui.ViewHeading;
import com.vaadin.securitystatus.data.DataRefreshSignal;
import com.vaadin.securitystatus.data.DataUnreachableException;
import com.vaadin.securitystatus.data.SecurityDataService;
import com.vaadin.securitystatus.data.VersionNumber;
import com.vaadin.securitystatus.data.model.CasesIndex;
import com.vaadin.securitystatus.data.model.ScanIndex;
import com.vaadin.securitystatus.data.model.VersionAnswer;

/**
 * The scanner report, triaged: the headline, the three buckets, the open
 * findings and the two files an auditor takes away. The report is read by the
 * lookup's report tab and held for the session, so this screen answers from
 * it; the address carries only the version, which is all that survives the
 * session, and the screen says so when no report is in it.
 */
@Route("scan/:version?")
@RouteParent(HomeView.class)
@PageTitle("scan.title")
@AnonymousAllowed
public final class ScanView extends Composite<Div> implements BeforeEnterObserver {

    public static final String VERSION = "version";

    /** The rows the table draws; the files carry all of them. */
    static final int TABLE_CAP = 200;

    private static final Pattern FILE_NAME_SAFE = Pattern.compile("[^A-Za-z0-9._-]");

    private final SecurityDataService data;
    private final ScanState scan;

    private final ValueSignal<String> asked = new ValueSignal<>("");
    private final ValueSignal<Boolean> showSecure = new ValueSignal<>(false);
    private final Div body = new Div();

    public ScanView(SecurityDataService data, DataRefreshSignal refresh, ScanState scan) {
        this.data = data;
        this.scan = scan;
        addClassName("scan-view");
        body.addClassName("scan-body");
        getContent().add(ViewHeading.ofAnswer(), body);

        var generatedAt = refresh.generatedAt();
        Signal.effect(body, () -> {
            generatedAt.get();
            draw(scan.held().get(), asked.get(), showSecure.get());
        });
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        asked.set(event.getRouteParameters().get(VERSION).orElse(""));
        showSecure.set(Address.has(event.getLocation().getQueryParameters(), QueryFlag.SECURE));
    }

    /** What the screen is drawn from: the index, the scan index, and the version's document when measured. */
    private record Measurements(CasesIndex index, ScanIndex scan, Optional<VersionAnswer> version) {
    }

    private void draw(Optional<ScanState.Held> held, String version, boolean secureShown) {
        body.removeAll();
        if (held.isEmpty()) {
            body.add(lead("scan-none", version.isEmpty() ? getTranslation("scan.none")
                    : getTranslation("scan.none.version", version)));
            return;
        }
        Measurements measured;
        try {
            measured = new Measurements(data.index(), data.scanIndex(),
                    version.isEmpty() ? Optional.empty() : data.version(VersionNumber.of(version)));
        } catch (DataUnreachableException unreachable) {
            body.add(new DataOutageNotice());
            return;
        }
        ScannerReport report = held.get().report();
        Optional<String> named = version.isEmpty() ? Optional.empty() : Optional.of(version);
        Triage.Result result = Triage.of(report, measured.scan(), measured.version(), named);

        List<String> lead = new ArrayList<>();
        // A report with no version is asked for one in the lookup, under the card, as
        // the prototype asks; the rows below still say what can be said without it.
        if (!version.isEmpty() && measured.version().isEmpty()) {
            body.add(unmeasured(measured.index(), version, report.vaadin().filter(version::equals).isPresent(),
                    lead));
        } else if (!version.isEmpty()) {
            VersionAnswer answer = measured.version().get();
            body.add(headline(result, answer, report, lead));
            body.add(buckets(result, answer, lead));
        }
        if (!version.isEmpty()) {
            body.add(actions(sheet(result, measured, version, lead), version, measured.version()
                    .flatMap(VersionAnswer::upgrade).flatMap(VersionAnswer.Upgrade::to), measured.index()));
        }
        body.add(findings(result, measured.index(), version, secureShown));
        if (!report.skipped().isEmpty()) {
            body.add(skipped(report.skipped()));
        }
        body.add(lead("scan-kept", getTranslation("scan.kept")));
    }

    // The headline and the buckets

    private Component headline(Triage.Result result, VersionAnswer answer, ScannerReport report, List<String> lead) {
        int open = result.open().size();
        int clears = answer.upgrade().flatMap(VersionAnswer.Upgrade::to).isPresent()
                ? result.count(Triage.Bucket.FIX_AVAILABLE)
                : 0;
        String version = answer.version().toString();
        String title;
        if (result.rows().isEmpty()) {
            title = getTranslation(report.findings().isEmpty() && report.components().isEmpty() ? "scan.headline.empty"
                    : "scan.headline.nothingKnown");
        } else if (clears > 0) {
            title = getTranslation(open == 1 ? "scan.headline.clears.one" : "scan.headline.clears.many", clears, open);
        } else if (open > 0) {
            title = getTranslation(open == 1 ? "scan.headline.attention.one" : "scan.headline.attention.many", open);
        } else {
            title = getTranslation("scan.headline.none", version);
        }
        String sentence;
        if (clears > 0) {
            VersionNumber to = answer.upgrade().flatMap(VersionAnswer.Upgrade::to).orElseThrow();
            sentence = to.line().equals(answer.version().line())
                    ? getTranslation("scan.headline.upgrade.sameLine", to.toString())
                    : getTranslation("scan.headline.upgrade.otherLine", to.toString(), to.line());
            if (open > clears) {
                sentence += " " + getTranslation(open - clears == 1 ? "scan.headline.remaining.one"
                        : "scan.headline.remaining.many", open - clears);
            }
        } else {
            sentence = getTranslation("scan.headline.measured", version);
        }
        lead.add(title);
        lead.add(sentence);
        var heading = new H3(title);
        heading.addClassName("scan-headline");
        var box = new Section(heading, new Paragraph(sentence));
        box.addClassName("scan-answer");
        return box;
    }

    private Component buckets(Triage.Result result, VersionAnswer answer, List<String> lead) {
        var cards = new Div();
        cards.addClassName("scan-buckets");
        for (Triage.Bucket bucket : Triage.Bucket.values()) {
            int count = result.count(bucket);
            var number = new Span(Integer.toString(count));
            number.addClassName("scan-bucket-count");
            var word = new Span(getTranslation(bucketKey(bucket)));
            word.addClassName("scan-bucket-word");
            var card = new Div(number, word);
            card.addClassNames("scan-bucket", "state-" + bucket.name().toLowerCase(java.util.Locale.ROOT));
            Optional<String> detail = bucketDetail(bucket, result, answer);
            detail.ifPresent(text -> {
                var line = new Span(text);
                line.addClassName("scan-bucket-detail");
                card.add(line);
            });
            cards.add(card);
            lead.add(count + " " + getTranslation(bucketKey(bucket)) + detail.map(text -> ": " + text).orElse(""));
        }
        return cards;
    }

    /** The sentence under a count. Each unresolved row is counted under one reason, so the parts add up. */
    private Optional<String> bucketDetail(Triage.Bucket bucket, Triage.Result result, VersionAnswer answer) {
        String version = answer.version().toString();
        return switch (bucket) {
            case SECURE -> Optional.of(getTranslation("scan.bucket.detail.SECURE", version));
            case FIX_AVAILABLE -> answer.upgrade().flatMap(VersionAnswer.Upgrade::to)
                    .filter(to -> result.count(Triage.Bucket.FIX_AVAILABLE) > 0)
                    .map(to -> getTranslation("scan.bucket.detail.FIX_AVAILABLE", to.toString()));
            case UNRESOLVED -> {
                Map<String, Integer> reasons = new LinkedHashMap<>();
                for (Triage.Row row : result.rows()) {
                    if (row.bucket() == Triage.Bucket.UNRESOLVED) {
                        reasons.merge(reason(row), 1, Integer::sum);
                    }
                }
                var parts = new ArrayList<String>();
                reasons.forEach((reason, count) -> parts.add(getTranslation(reason, count)));
                yield parts.isEmpty() ? Optional.empty()
                        : Optional.of(getTranslation("scan.reasons", String.join(", ", parts)));
            }
        };
    }

    private static String reason(Triage.Row row) {
        if (row.pending().isPresent()) {
            return "scan.reason.notAssessed";
        }
        return switch (row.owner()) {
            case YOURS -> "scan.reason.yours";
            case SPRING_BOOT -> "scan.reason.springBoot";
            case LIBRARY -> "scan.reason.library";
            case VAADIN -> row.action() == Triage.Action.DISCONTINUED ? "scan.reason.library" : "scan.reason.vaadin";
        };
    }

    // A version nobody measured

    /**
     * @param fromFile true when the version is the one the report names, which the
     *                 prototype says as "Your report was taken from"
     */
    private Component unmeasured(CasesIndex index, String version, boolean fromFile, List<String> lead) {
        var notice = new Section();
        notice.addClassName("scan-unmeasured");
        var word = new Badge(getTranslation("scan.unmeasured.word"));
        word.addClassNames("status-chip", "state-unmeasured");
        String said = getTranslation(fromFile ? "scan.unmeasured.text.file" : "scan.unmeasured.text", version);
        // One paragraph, as the prototype says it: the notice, then the nearest measured release.
        var text = new Paragraph(new Span(said));
        text.addClassName("scan-unmeasured-text");
        notice.add(word, text);
        lead.add(said);
        VersionRules.nearestMeasured(index, VersionNumber.of(version)).ifPresent(nearest -> {
            String near = nearest.version().toString();
            var link = new RouterLink(near, ScanView.class, new RouteParameters(VERSION, near));
            link.addClassName("scan-nearest");
            text.add(new Text(" "), new Span(getTranslation("scan.unmeasured.nearest." + nearest.kind().name(),
                    nearest.version().line())), new Text(" "), link,
                    new Span(getTranslation("scan.unmeasured.notYours")));
        });
        return notice;
    }

    // The files

    private TriageFiles.Sheet sheet(Triage.Result result, Measurements measured, String version, List<String> lead) {
        // The prototype's columns in its order, then the app's verdict column, D-197.
        var columns = List.of(getTranslation("scan.column.cve"), getTranslation("scan.column.bucket"),
                getTranslation("scan.column.owner"), getTranslation("scan.column.component"),
                getTranslation("scan.column.severity"), getTranslation("scan.column.action"),
                getTranslation("scan.column.verdict"));
        var rows = new ArrayList<List<String>>();
        for (Triage.Row row : result.rows()) {
            rows.add(List.of(row.advisory(), getTranslation(bucketKey(row.bucket())),
                    getTranslation(ownerKey(row.owner())), row.coordinate(), row.severity().orElse(""), action(row),
                    verdictText(row, measured.index())));
        }
        String generatedAt = measured.index().generatedAt().toString();
        var notes = List.of(getTranslation("scan.file.heading"), getTranslation("scan.file.version", version),
                getTranslation("scan.file.measured", generatedAt));
        return new TriageFiles.Sheet(getTranslation("scan.file.title", version),
                getTranslation("scan.file.about", version, generatedAt), notes, lead, columns, rows);
    }

    private Component actions(TriageFiles.Sheet sheet, String version, Optional<VersionNumber> upgrade,
            CasesIndex index) {
        String safe = FILE_NAME_SAFE.matcher(version).replaceAll("_");
        String csvName = "vaadin-security-triage-" + safe + ".csv";
        String pdfName = "vaadin-security-audit-" + safe + ".pdf";
        Supplier<Optional<Downloads.Content>> csv = () -> content(TriageFiles.csv(sheet), TriageFiles.CSV);
        Supplier<Optional<Downloads.Content>> pdf = () -> content(TriageFiles.pdf(sheet), TriageFiles.PDF);

        var bar = new Div();
        bar.addClassName("scan-actions");
        var triage = new Button(getTranslation("scan.download.csv"));
        triage.addClassName("scan-download-csv");
        triage.addThemeVariants(ButtonVariant.PRIMARY);
        Downloads.onClick(triage, csvName, csv);
        bar.add(triage);
        // The prototype's two, the download and the release notes, then the app's audit report.
        upgrade.flatMap(to -> VersionRules.releaseNotes(index, to).map(href -> {
            var notes = new Anchor(href, getTranslation("scan.releaseNotes", to.toString()));
            notes.addClassName("scan-release-notes");
            return notes;
        })).ifPresent(bar::add);

        if (PdfPreview.available()) {
            var preview = new Button(getTranslation("scan.preview.pdf"));
            preview.addClassName("scan-preview-pdf");
            preview.addClickListener(click -> previewDialog(pdfName, pdf).open());
            bar.add(preview);
        } else {
            // The viewer is an experimental component behind its flag. Without it the
            // auditor still takes the same document, unseen.
            var download = new Button(getTranslation("scan.download.pdf"));
            download.addClassName("scan-download-pdf");
            Downloads.onClick(download, pdfName, pdf);
            bar.add(download);
        }
        return bar;
    }

    private Dialog previewDialog(String pdfName, Supplier<Optional<Downloads.Content>> pdf) {
        var dialog = new Dialog();
        dialog.addClassName("scan-pdf-dialog");
        dialog.setHeaderTitle(getTranslation("scan.preview.title"));
        dialog.setWidth("min(72rem, 95vw)");
        var viewer = PdfPreview.of(Downloads.shown(pdfName, pdf), pdfName);
        viewer.addClassName("scan-pdf-viewer");
        viewer.setWidthFull();
        viewer.setHeight("70vh");
        dialog.add(viewer);
        var download = new Button(getTranslation("scan.download.pdf"));
        download.addClassName("scan-download-pdf");
        download.addThemeVariants(ButtonVariant.PRIMARY);
        Downloads.onClick(download, pdfName, pdf);
        var close = new Button(getTranslation("help.close"), click -> dialog.close());
        dialog.getFooter().add(close, download);
        dialog.addOpenedChangeListener(change -> {
            if (!change.isOpened()) {
                dialog.removeFromParent();
            }
        });
        return dialog;
    }

    private static Optional<Downloads.Content> content(byte[] bytes, String contentType) {
        return Optional.of(new Downloads.Content(new ByteArrayInputStream(bytes), contentType, bytes.length));
    }

    // The table

    private Component findings(Triage.Result result, CasesIndex index, String version, boolean secureShown) {
        var section = new Section();
        section.addClassName("scan-findings");
        var head = new Div(new H3(getTranslation("scan.findings.title")));
        head.addClassName("scan-findings-head");
        int secure = result.count(Triage.Bucket.SECURE);
        if (secure > 0) {
            String label = secureShown ? (secure == 1 ? "scan.findings.secure.hide.one" : "scan.findings.secure.hide.many")
                    : (secure == 1 ? "scan.findings.secure.one" : "scan.findings.secure.many");
            var toggle = new Switch(getTranslation(label, secure));
            toggle.addClassName("scan-secure-switch");
            toggle.setValue(secureShown);
            toggle.addValueChangeListener(change -> {
                if (change.isFromClient() || !change.getValue().equals(secureShown)) {
                    showSecure(version, change.getValue());
                }
            });
            head.add(toggle);
        }
        section.add(head);

        List<Triage.Row> rows = secureShown ? result.rows() : result.open();
        if (rows.isEmpty()) {
            section.add(lead("scan-findings-empty", getTranslation("scan.findings.none")));
            return section;
        }
        var table = new Table();
        table.addClassNames("scan-table", "answer-table", "vaadin-themed-html");
        table.addHeaderRow(getTranslation("scan.column.cve"), getTranslation("scan.column.component"),
                getTranslation("scan.column.owner"), getTranslation("scan.column.verdict"),
                getTranslation("scan.column.action"));
        for (Triage.Row row : rows.subList(0, Math.min(rows.size(), TABLE_CAP))) {
            TableRow line = table.addRow();
            line.addClassName("scan-row");
            line.getElement().setAttribute("data-advisory", row.advisory());
            line.addDataCell(advisoryLink(row.advisory(), version));
            var component = new Span(row.component());
            component.setTitle(row.coordinate());
            line.addDataCell(component);
            line.addDataCell(getTranslation(ownerKey(row.owner())));
            line.addDataCell(verdictCell(row, index));
            line.addDataCell(action(row));
        }
        section.add(table);
        if (rows.size() > TABLE_CAP) {
            section.add(lead("scan-cap", getTranslation("scan.findings.cap", TABLE_CAP, rows.size(),
                    result.rows().size())));
        }
        return section;
    }

    private void showSecure(String version, boolean show) {
        Address.of(version.isEmpty() ? "scan" : "scan/" + version).bare(QueryFlag.SECURE, show)
                .go(UI.getCurrentOrThrow());
    }

    private static Component advisoryLink(String advisory, String version) {
        var parameters = new LinkedHashMap<String, String>();
        parameters.put(CveView.IDS, advisory);
        if (!version.isEmpty()) {
            parameters.put(CveView.VERSION, version);
        }
        var link = new RouterLink(advisory, CveView.class, new RouteParameters(parameters));
        link.addClassName("scan-advisory");
        return link;
    }

    private Component verdictCell(Triage.Row row, CasesIndex index) {
        var cell = new Span();
        cell.addClassName("scan-verdict");
        // The fourth word is no fourth state: the row stays counted under unresolved, rule 6.11.19.
        var chip = new Badge(row.pending().isPresent() ? getTranslation("state.notAssessed")
                : getTranslation(bucketKey(row.bucket())));
        chip.addClassNames("status-chip", "state-" + (row.pending().isPresent() ? "in_triage"
                : row.bucket().name().toLowerCase(java.util.Locale.ROOT)));
        cell.add(chip);
        settledVerdict(row, index).ifPresent(label -> {
            var said = new Span(label);
            said.addClassName("scan-verdict-label");
            cell.add(said);
        });
        return cell;
    }

    private String verdictText(Triage.Row row, CasesIndex index) {
        String word = row.pending().isPresent() ? getTranslation("state.notAssessed")
                : getTranslation(bucketKey(row.bucket()));
        return settledVerdict(row, index).map(label -> word + ": " + label).orElse(word);
    }

    /**
     * The verdict named, in the index's own words: for a secure row the verdict
     * that settled it, and for a version nobody measured whatever the catalogue
     * says of the advisory, which is all that can be said.
     */
    private static Optional<String> settledVerdict(Triage.Row row, CasesIndex index) {
        // A secure row names the verdict only when the verdict settled it. A row secure
        // because the version does not report it says so in its action, and the
        // catalogue's word for other releases beside it would contradict it, D-198.
        boolean named = row.action() == Triage.Action.RULED_OUT || row.action() == Triage.Action.FROM_BUILD
                || row.action() == Triage.Action.VERSION_UNMEASURED;
        return row.verdict().filter(verdict -> named).map(index.labels()::get);
    }

    private String action(Triage.Row row) {
        return getTranslation(row.action().key(), row.arguments().toArray());
    }

    private static String ownerKey(Triage.Owner owner) {
        return "scan.owner." + owner.name();
    }

    private static String bucketKey(Triage.Bucket bucket) {
        return "scan.bucket." + bucket.name();
    }

    private Component skipped(List<String> names) {
        List<String> shown = names.stream().filter(name -> !name.isBlank()).limit(4).toList();
        String key = names.size() == 1 ? "scan.skipped.one" : "scan.skipped.many";
        String said = shown.isEmpty() ? getTranslation(key, names.size())
                : getTranslation(key + ".named", names.size(), String.join(", ", shown));
        return lead("scan-skipped", said);
    }

    private static Paragraph lead(String className, String text) {
        var paragraph = new Paragraph(text);
        paragraph.addClassName(className);
        return paragraph;
    }
}

package com.vaadin.securitystatus.status;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.BrowserlessTestConfig;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Switch;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.Table;
import com.vaadin.flow.component.html.TableRow;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.UploadButton;
import com.vaadin.flow.component.upload.UploadDropZone;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.securitystatus.data.FixturePages;

/**
 * SCN-01, SCN-03, SCN-04 and SCN-09, and US-6.4: a report through the
 * lookup's upload, the browserless upload testers standing in for the browser,
 * answered from the committed fixtures. The osv-scanner fixture is one scan of
 * an application on 24.9.21, whose upgrade to 24.9.26 clears everything of
 * Vaadin's.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class ScanViewBrowserlessTest extends SpringBrowserlessTest {

    @Autowired
    FixturePages fixtures;

    @AfterEach
    void restore() {
        fixtures.restore();
    }

    // SCN-01, S1 to S3
    @Test
    void an_osv_scanner_report_is_detected_its_version_read_and_the_headline_counts_it() {
        upload("osv-scanner.json");

        assertThat(getCurrentView()).isInstanceOf(ScanView.class);
        assertThat(path()).isEqualTo("scan/24.9.21");
        assertThat(inLookup(Paragraph.class, "lookup-report-formats").getFirst().getText())
                .isEqualTo("osv-scanner.json · Vaadin 24.9.21 · 9 findings · osv-scanner");
        assertThat(find(H2.class).withClassName("scan-headline").single().getText())
                .isEqualTo("One upgrade clears 1 of your 6 open findings");
        assertThat(text(find(Section.class).withClassName("scan-answer").single()))
                .contains("Upgrade to 24.9.26, a maintenance release in the minor you already use.")
                .contains("The remaining 5 need a Spring Boot change, a fix of your own, or are waiting on a fix.");

        // S4: the design's three words and the counts.
        assertThat(find(Div.class).withClassName("scan-bucket").all()).extracting(this::text).containsExactly(
                "3 Already secure Not affected or already fixed in 24.9.21. Safe to whitelist.",
                "1 Fix available All fixed by upgrading to 24.9.26.",
                "5 Unresolved 2 awaiting a Spring Boot change, 1 awaiting the library, 1 not assessed yet,"
                        + " 1 not ours to answer");
        // The upgrade's release notes are offered beside the downloads.
        assertThat(find(com.vaadin.flow.component.html.Anchor.class).withClassName("scan-release-notes").single()
                .getHref()).isEqualTo("https://github.com/vaadin/platform/releases/tag/24.9.26");
    }

    // S5: the open findings table, owner and action per row, the secure ones on demand
    @Test
    void the_table_lists_the_open_findings_and_the_switch_brings_the_secure_ones() {
        upload("osv-scanner.json");

        assertThat(rows()).hasSize(6);
        assertThat(row("CVE-2026-89407")).containsExactly("CVE-2026-89407", "jackson-core", "Vaadin",
                "Fix available", "Upgrade to 24.9.26");
        assertThat(row("CVE-2026-45292")).containsExactly("CVE-2026-45292", "opentelemetry-api", "Spring Boot",
                "Unresolved", "Upgrade Spring Boot, or override its opentelemetry-api version");
        assertThat(row("CVE-2099-0002")).containsExactly("CVE-2099-0002", "flow-server", "Vaadin",
                "Not assessed yet", "Reported on a coordinate Vaadin brings, and on no release we measure");
        assertThat(row("CVE-2099-0001")).containsExactly("CVE-2099-0001", "billing-core", "Yours", "Unresolved",
                "Not on anything Vaadin ships: billing-core is yours to assess");
        RouterLink id = find(RouterLink.class).withClassName("scan-advisory").all().getFirst();
        assertThat(id.getHref()).startsWith("cve/").endsWith("/24.9.21");

        Switch secure = find(Switch.class).withClassName("scan-secure-switch").single();
        assertThat(secure.getLabel()).isEqualTo("Show the 3 secure findings");
        test(secure).click();

        // The switch writes the address, and the version stays in it, rule 6.11.13.
        assertThat(UI.getCurrent().getInternals().getActiveViewLocation().getPathWithQueryParameters())
                .startsWith("scan/24.9.21?secure");
        assertThat(rows()).hasSize(9);
        // AC2: the ruled out finding is already secure with the verdict named.
        assertThat(row("CVE-2021-33604")).containsExactly("CVE-2021-33604", "vaadin-swing-kit-flow", "Vaadin",
                "Already secure scanner false positive", "Does not reach a Vaadin application");
        assertThat(row("CVE-2026-2742").get(4)).isEqualTo("Not reported on 24.9.21");
    }

    // SCN-03, S2
    @Test
    void a_report_naming_no_version_asks_for_one_and_the_submit_reads_answer_for_this_version() {
        upload("bomber-no-version.json");

        assertThat(path()).isEqualTo("scan");
        TextField version = inLookup(TextField.class, "lookup-report-version").getFirst();
        assertThat(version.isVisible()).isTrue();
        Button check = find(Button.class).withClassName("lookup-check").single();
        assertThat(check.getText()).isEqualTo("Answer for this version");
        assertThat(text(find(Div.class).withClassName("scan-body").single()))
                .contains("Your report names no Vaadin release.");
        assertThat(rows()).allMatch(row -> !row.get(4).startsWith("Upgrade"));

        test(version).setValue("24.9.21");
        test(check).click();

        assertThat(path()).isEqualTo("scan/24.9.21");
        assertThat(find(H2.class).withClassName("scan-headline").single().getText())
                .isEqualTo("One upgrade clears 1 of your 6 open findings");
    }

    @Test
    void a_report_that_names_its_version_draws_no_version_field() {
        upload("dependency-check.json");

        assertThat(inLookup(TextField.class, "lookup-report-version").getFirst().isVisible()).isFalse();
        assertThat(find(Button.class).withClassName("lookup-check").exists()).isFalse();
    }

    // SCN-04
    @Test
    void a_json_that_is_no_report_is_refused_with_the_four_formats_named_and_nothing_navigates() {
        navigate(HomeView.class);
        uploadHere("not-a-report.json");

        assertThat(getCurrentView()).isInstanceOf(HomeView.class);
        assertThat(inLookup(Span.class, "lookup-report-refused").getFirst().getText()).isEqualTo(
                "That file is not a CycloneDX, OWASP Dependency-Check, osv-scanner or bomber report."
                        + " Those four are what this page reads.");

        uploadHere("not-json.txt");
        assertThat(getCurrentView()).isInstanceOf(HomeView.class);
        assertThat(inLookup(Span.class, "lookup-report-refused").getFirst().getText())
                .startsWith("That file is not JSON").contains("CycloneDX, OWASP Dependency-Check, osv-scanner or bomber");
    }

    // The 25.4 upload testers put a file through the browser's own gate: the size limit refuses
    // before anything is sent, and the refusal is the bundle's sentence
    @Test
    void a_file_over_the_size_limit_is_refused_before_it_is_read(@TempDir Path temporary) throws IOException {
        Path huge = temporary.resolve("huge.json");
        try (var file = new java.io.RandomAccessFile(huge.toFile(), "rw")) {
            file.setLength(Lookup.MAX_REPORT_BYTES + 1);
        }
        navigate(HomeView.class);
        showReportTab();
        var choose = test(inLookup(UploadButton.class, "lookup-report-choose").getFirst());
        choose.upload(huge.toFile());

        choose.ensureUploadFailed();
        assertThat(getCurrentView()).isInstanceOf(HomeView.class);
        assertThat(inLookup(Span.class, "lookup-report-refused").getFirst().getText()).isEqualTo(
                "That file is larger than 32 MB, which is more than any scanner report this page reads.");
    }

    // The drop zone takes one file as the button does: of two dropped at once the first is read and
    // the second refused at the gate. The refusal sentence does not stay, because the read of the first
    // clears it; recorded in the Still open notes of feature 06
    @Test
    void of_two_reports_dropped_at_once_the_first_is_read_and_the_second_refused() {
        navigate(HomeView.class);
        showReportTab();
        var zone = test(inLookup(UploadDropZone.class, "lookup-report").getFirst());

        zone.drop(fixtureFile("osv-scanner.json"), fixtureFile("bomber.json"));

        assertThat(zone.getLastUploadStatus()).extracting(status -> status.fileName() + " " + status.status())
                .containsExactly("osv-scanner.json UPLOADED", "bomber.json REJECTED");
        assertThat(getCurrentView()).isInstanceOf(ScanView.class);
        assertThat(find(Lookup.class).single().getElement().getTextRecursively())
                .contains("osv-scanner.json · Vaadin 24.9.21 · 9 findings · osv-scanner");
    }

    // SCN-09, S8
    @Test
    void entries_with_no_coordinate_are_counted_on_screen() {
        upload("dependency-check-skipped.json");

        // The file names no version: answered for one, so the whole screen is drawn.
        test(inLookup(TextField.class, "lookup-report-version").getFirst()).setValue("24.9.21");
        test(find(Button.class).withClassName("lookup-check").single()).click();

        assertThat(find(Paragraph.class).withClassName("scan-skipped").single().getText()).isEqualTo(
                "2 entries in your report carried findings with no coordinate, so they could not be matched."
                        + " Named in the file: legacy-tools.zip, bundled-js/app.min.js.");
    }

    // The edge case: zero findings
    @Test
    void a_report_with_no_findings_says_so_and_the_buckets_are_zero() {
        upload("osv-scanner-empty.json");
        test(inLookup(TextField.class, "lookup-report-version").getFirst()).setValue("24.9.21");
        test(find(Button.class).withClassName("lookup-check").single()).click();

        assertThat(find(H2.class).withClassName("scan-headline").single().getText())
                .isEqualTo("This report carries no findings");
        assertThat(find(Span.class).withClassName("scan-bucket-count").all()).extracting(Span::getText)
                .containsExactly("0", "0", "0");
        assertThat(find(Button.class).withClassName("scan-download-csv").exists()).isTrue();
    }

    // The edge case: a version we never measured
    @Test
    void a_report_from_a_version_nobody_measured_offers_the_nearest_measured_release_in_place() {
        upload("cyclonedx-unmeasured.json");

        assertThat(path()).isEqualTo("scan/24.9.99");
        Section notice = find(Section.class).withClassName("scan-unmeasured").single();
        assertThat(text(notice)).startsWith("Not measured We have no scan of Vaadin 24.9.99.");
        RouterLink nearest = find(RouterLink.class).withClassName("scan-nearest").single();
        assertThat(nearest.getText()).isEqualTo("24.9.26");
        assertThat(find(H2.class).withClassName("scan-headline").exists()).isFalse();
        // What the catalogue can say is said, and nothing is called secure.
        assertThat(rows()).isNotEmpty().allMatch(row -> !row.get(3).startsWith("Already secure"));

        // In place: the same report, answered for the offered release. The fixtures carry no
        // version document of 24.9.26, so only the address is asserted here.
        test(nearest).click();
        assertThat(path()).isEqualTo("scan/24.9.26");
        assertThat(inLookup(Paragraph.class, "lookup-report-formats").getFirst().getText())
                .startsWith("cyclonedx-unmeasured.json");
    }

    // The edge case: a huge report caps the table at 200 and counts the rest
    @Test
    void a_huge_report_is_capped_at_200_rows_with_the_remainder_counted(@TempDir Path temporary) throws IOException {
        var packages = new ArrayList<String>();
        for (int i = 0; i < 250; i++) {
            packages.add("""
                    {"package": {"name": "com.example:lib%d", "version": "1.0", "ecosystem": "Maven"},
                     "vulnerabilities": [{"id": "CVE-2099-%d"}]}""".formatted(i, 10000 + i));
        }
        Path huge = temporary.resolve("huge.json");
        Files.writeString(huge, "{\"results\": [{\"packages\": [" + String.join(",", packages) + "]}]}",
                StandardCharsets.UTF_8);
        navigate(HomeView.class);
        showReportTab();
        test(inLookup(UploadButton.class, "lookup-report-choose").getFirst()).upload(huge.toFile());
        test(inLookup(TextField.class, "lookup-report-version").getFirst()).setValue("24.9.21");
        test(find(Button.class).withClassName("lookup-check").single()).click();

        assertThat(rows()).hasSize(ScanView.TABLE_CAP);
        assertThat(find(Paragraph.class).withClassName("scan-cap").single().getText())
                .isEqualTo("Showing the first 200 of 250. The downloads carry all 250.");
    }

    // AC4 and the epic's Definition of Done: no upload leaves a file behind
    @Test
    void no_upload_leaves_a_file_behind(@TempDir Path source) throws IOException {
        String marker = "marker-" + java.util.UUID.randomUUID();
        Path report = source.resolve("report.json");
        Files.writeString(report, """
                {"results": [{"packages": [{"package": {"name": "com.example:%s", "version": "1.0",
                 "ecosystem": "Maven"}, "vulnerabilities": [{"id": "CVE-2099-0001"}]}]}]}""".formatted(marker));
        Path temporary = Path.of(System.getProperty("java.io.tmpdir"));
        java.util.Set<Path> before = listing(temporary);
        long started = System.currentTimeMillis();

        navigate(HomeView.class);
        showReportTab();
        test(inLookup(UploadButton.class, "lookup-report-choose").getFirst()).upload(report.toFile());
        test(inLookup(UploadButton.class, "lookup-report-choose").getFirst()).ensureUploaded();
        assertThat(getCurrentView()).isInstanceOf(ScanView.class);

        // Whatever appeared in the temporary directory during the upload, none of it holds the report.
        try (var walk = Files.walk(temporary, 3)) {
            List<Path> leftBehind = walk.filter(Files::isRegularFile).filter(path -> !path.startsWith(source))
                    .filter(path -> !before.contains(path) || modifiedSince(path, started))
                    .filter(path -> contains(path, marker)).toList();
            assertThat(leftBehind).isEmpty();
        }
    }

    private static java.util.Set<Path> listing(Path directory) throws IOException {
        try (var walk = Files.walk(directory, 3)) {
            return walk.filter(Files::isRegularFile).collect(java.util.stream.Collectors.toSet());
        } catch (java.io.UncheckedIOException unreadable) {
            return java.util.Set.of();
        }
    }

    private static boolean modifiedSince(Path path, long started) {
        try {
            return Files.getLastModifiedTime(path).toMillis() >= started;
        } catch (IOException gone) {
            return false;
        }
    }

    private static boolean contains(Path path, String marker) {
        try {
            return Files.size(path) < 10_000_000 && Files.readString(path, StandardCharsets.ISO_8859_1).contains(marker);
        } catch (IOException | RuntimeException unreadable) {
            return false;
        }
    }

    // The edge case: the same file twice is the same answer
    @Test
    void the_same_file_twice_is_the_same_answer_and_a_second_file_replaces_the_first() {
        upload("osv-scanner.json");
        List<List<String>> first = rows();

        uploadHere("osv-scanner.json");
        assertThat(rows()).isEqualTo(first);

        uploadHere("dependency-check-skipped.json");
        assertThat(inLookup(Paragraph.class, "lookup-report-formats").getFirst().getText())
                .startsWith("dependency-check-skipped.json");
    }

    // The session bound report: a fresh address with no report in the session
    @Test
    void a_scan_address_with_no_report_in_the_session_says_so_with_the_version_prefilled() {
        navigate("scan/24.9.21", ScanView.class);

        assertThat(find(Paragraph.class).withClassName("scan-none").single().getText()).isEqualTo(
                "No report is in this session. Drop your report in the box above and it is answered for Vaadin 24.9.21.");
        assertThat(find(Tabs.class).withClassName("lookup-tabs").single().getSelectedIndex()).isEqualTo(2);
        assertThat(inLookup(UploadDropZone.class, "lookup-report").getFirst().isVisible()).isTrue();
    }

    // US-6.4: the format list held to the parser
    @Test
    void the_format_line_and_the_help_list_exactly_the_formats_the_parser_reads() {
        navigate(HomeView.class);
        List<String> parsed = new ArrayList<>();
        for (ReportFormat format : ReportFormat.values()) {
            parsed.add(UI.getCurrent().getTranslation(format.nameKey()));
        }

        String line = inLookup(Paragraph.class, "lookup-report-formats").getFirst().getText();
        assertThat(line).isEqualTo("CycloneDX, OWASP Dependency-Check, osv-scanner or bomber exports.");
        parsed.forEach(name -> assertThat(line).contains(name));
        assertThat(line.split(",| or ")).hasSize(parsed.size());
        assertThat(line).doesNotContain("Sonatype");

        List<String> recipes = new ArrayList<>();
        collect(reportHelp(), Section.class, "lookup-help-format", recipes);
        assertThat(recipes).containsExactlyElementsOf(parsed);
    }

    // US-6.4, AC4: what is assessed, counted against the file in hand, and the parsed and discarded sentence
    @Test
    void the_help_counts_the_file_and_says_it_is_not_kept() {
        upload("osv-scanner.json");

        String help = text(reportHelp());
        assertThat(help).contains("The file you uploaded flags findings on 9 coordinates, and 8 of those are"
                + " coordinates we measure inside Vaadin releases. The remaining one is not, so its findings come"
                + " back as yours with no verdict of ours. That one is billing-core.");
        assertThat(help).contains("Your file is not kept").contains("the file itself is not stored");
        assertThat(find(Paragraph.class).withClassName("scan-kept").single().getText())
                .contains("the file was not stored");
    }

    // CHANGELOG-RISK rule 1: without the flag the screen offers the plain download
    @Test
    @BrowserlessTestConfig(featureFlags = "pdfViewerComponent=false")
    void without_the_pdf_viewer_flag_the_audit_report_is_a_plain_download() {
        upload("osv-scanner.json");

        assertThat(find(Button.class).withClassName("scan-preview-pdf").exists()).isFalse();
        assertThat(find(Button.class).withClassName("scan-download-pdf").single().getText())
                .isEqualTo("Download the audit report");
    }

    @Test
    void with_the_flag_the_audit_report_is_previewed_in_the_viewer_before_it_is_taken() {
        upload("osv-scanner.json");

        test(find(Button.class).withClassName("scan-preview-pdf").single()).click();

        var viewer = find(com.vaadin.flow.component.pdfviewer.PdfViewer.class).single();
        assertThat(viewer.getFileName()).isEqualTo("vaadin-security-audit-24.9.21.pdf");
        assertThat(find(Button.class).withClassName("scan-download-pdf").single().getText())
                .isEqualTo("Download the audit report");
    }

    // Helpers

    private void upload(String fixture) {
        navigate(HomeView.class);
        uploadHere(fixture);
    }

    private void uploadHere(String fixture) {
        showReportTab();
        test(inLookup(UploadButton.class, "lookup-report-choose").getFirst()).upload(fixtureFile(fixture));
    }

    private void showReportTab() {
        test(find(Tabs.class).withClassName("lookup-tabs").single()).select(2);
    }

    private static File fixtureFile(String name) {
        try {
            return new File(ScanViewBrowserlessTest.class.getResource("/scan/" + name).toURI());
        } catch (URISyntaxException wrong) {
            throw new IllegalStateException(wrong);
        }
    }

    private List<List<String>> rows() {
        return find(Table.class).withClassName("scan-table").all().stream()
                .flatMap(table -> table.getBodyRows().stream())
                .map(row -> row.getDataCells().stream().map(cell -> text(cell).strip()).toList()).toList();
    }

    private List<String> row(String advisory) {
        return find(TableRow.class).all().stream()
                .filter(row -> advisory.equals(row.getElement().getAttribute("data-advisory"))).findFirst()
                .map(row -> row.getDataCells().stream().map(cell -> text(cell).strip()).toList()).orElseThrow();
    }

    /** Opens the report tab's help from its question mark, and returns the dialog. */
    private Component reportHelp() {
        showReportTab();
        Button mark = inLookup(Button.class, "help-mark").stream()
                .filter(button -> button.getAriaLabel().orElse("").startsWith("What is assessed")).findFirst()
                .orElseThrow();
        test(mark).click();
        return find(Dialog.class).withClassName("help-dialog").single();
    }

    /** The text of every leaf, joined with spaces, as a reader sees separate boxes. */
    private String text(Component component) {
        return words(component.getElement()).replaceAll("\\s+", " ").replace(" ,", ",").strip();
    }

    private static String words(com.vaadin.flow.dom.Element element) {
        if (element.isTextNode()) {
            return element.getText();
        }
        return String.join(" ", element.getChildren().map(ScanViewBrowserlessTest::words).toList());
    }

    private static String path() {
        return UI.getCurrent().getInternals().getActiveViewLocation().getPath();
    }

    private <T extends Component> List<T> inLookup(Class<T> type, String className) {
        var found = new ArrayList<T>();
        collectStyled(find(Lookup.class).single(), type, className, found);
        return found;
    }

    private static <T extends Component> void collectStyled(Component from, Class<T> type, String className,
            List<T> found) {
        if (type.isInstance(from) && (className.isEmpty()
                || from instanceof HasStyle styled && styled.hasClassName(className))) {
            found.add(type.cast(from));
        }
        from.getChildren().forEach(child -> collectStyled(child, type, className, found));
    }

    /** The heading of each section of that class, in order. */
    private static void collect(Component from, Class<Section> type, String className, List<String> headings) {
        if (type.isInstance(from) && ((HasStyle) from).hasClassName(className)) {
            from.getChildren().filter(H4.class::isInstance).findFirst()
                    .ifPresent(heading -> headings.add(((H4) heading).getText()));
        }
        from.getChildren().forEach(child -> collect(child, type, className, headings));
    }
}

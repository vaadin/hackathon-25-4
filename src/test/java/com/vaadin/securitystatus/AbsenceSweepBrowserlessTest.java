package com.vaadin.securitystatus;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.markdown.Markdown;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.upload.UploadButton;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.securitystatus.data.FixturePages;
import com.vaadin.securitystatus.security.Roles;
import com.vaadin.securitystatus.ui.customer.AdvisoriesView;
import com.vaadin.securitystatus.ui.customer.CasesView;
import com.vaadin.securitystatus.ui.customer.CveView;
import com.vaadin.securitystatus.ui.customer.HomeView;
import com.vaadin.securitystatus.ui.customer.ScanView;
import com.vaadin.securitystatus.ui.customer.VersionView;
import com.vaadin.securitystatus.ui.team.diagnostics.DiagnosticsView;
import com.vaadin.securitystatus.ui.team.explorer.ExplorerView;
import com.vaadin.securitystatus.ui.team.explorer.ReleaseView;
import com.vaadin.securitystatus.ui.team.explorer.SpringView;
import com.vaadin.securitystatus.ui.team.workbench.WorkbenchView;
import com.vaadin.securitystatus.team.workbench.VerdictDraftRepository;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * POL-03, the rule 4 sweep: the page never invents a value. For each screen,
 * one fixture or constructed record that lacks fields is opened, and the whole
 * page is walked: no text holder is rendered empty, no grid cell holds an
 * element with nothing in it, and nothing prints a placeholder such as
 * {@code null}. Then the one omission the record was built for is asserted, and
 * where the same page shows the field for a record that carries it, that is
 * asserted too, so the omission cannot pass by the field never being drawn.
 *
 * <p>
 * A grid cell for an absent value holds no element at all: in a table the
 * column is there for every row, so the cell itself cannot be left out, but
 * nothing is drawn in it, D-159.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@WithMockUser(username = "team", roles = Roles.SECURITY)
class AbsenceSweepBrowserlessTest extends SpringBrowserlessTest {

    /** Elements whose whole purpose is to hold text, so empty means drawn empty. */
    private static final Set<String> TEXT_HOLDERS = Set.of("span", "p", "h1", "h2", "h3", "h4", "h5", "h6", "li",
            "td", "th", "a", "strong", "em", "dt", "dd", "label", "mark", "code", "small", "b", "summary", "button",
            "vaadin-button", "vaadin-badge", "vaadin-details-summary");

    /** What a value the data does not carry prints as when it leaks through. */
    private static final Pattern PLACEHOLDER = Pattern
            .compile("\\bnull\\b|\\bundefined\\b|\\bNaN\\b|Optional\\[|Optional\\.empty");

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    FixturePages fixtures;

    @Autowired
    VerdictDraftRepository drafts;

    @AfterEach
    void restore() {
        fixtures.restore();
    }

    // Home: a line with no scan date, no support tier, and no scan date on the release it speaks for
    @Test
    void home_a_line_without_a_scan_date_or_support_tier_draws_neither() {
        edit("cases/index.json", root -> {
            for (JsonNode line : root.get("lines")) {
                if ("24.9".equals(line.get("line").asString())) {
                    ((ObjectNode) line).remove(List.of("scanned_at", "support"));
                }
            }
            for (JsonNode summary : root.get("summary")) {
                if (summary.has("line") && "24.9".equals(summary.get("line").asString())) {
                    ((ObjectNode) summary).remove("scanned_at");
                }
            }
        });
        navigate(AdvisoriesView.class);
        navigate(HomeView.class);

        assertThat(sweep()).isEmpty();
        Component bare = card("24.9");
        assertThat(classesIn(bare)).doesNotContain("line-card-scanned", "line-card-tier");
        // The positive control: the line beside it still carries both.
        assertThat(classesIn(card("24.10"))).contains("line-card-scanned", "line-card-tier");
    }

    // Version: one verdict without a workaround, and in the table of every line, one line whose
    // record carries no open counts, no support label and no scan date
    @Test
    void version_absent_workaround_and_line_facts_draw_nothing_in_their_place() {
        edit("cases/index.json", root -> {
            for (JsonNode line : root.get("lines")) {
                if ("24.10".equals(line.get("line").asString())) {
                    ((ObjectNode) line).remove(List.of("scanned_at", "support"));
                }
            }
            for (JsonNode summary : root.get("summary")) {
                if (summary.has("line") && "24.10".equals(summary.get("line").asString())) {
                    ((ObjectNode) summary).remove(List.of("scanned_at", "risk", "triage"));
                }
            }
        });
        navigate(HomeView.class);
        navigate("version/24.4.28", VersionView.class);

        List<String> blanks = sweep();
        // Open, support and scanned are the fourth, fifth and sixth cells of a line's row.
        assertThat(blanks).containsExactlyInAnyOrder("td 24.10 3", "td 24.10 4", "td 24.10 5");
        // Two verdicts answer CVE-2025-31672 here and only the second carries a workaround.
        assertThat(count(pageText(), "Do this now")).isGreaterThanOrEqualTo(1);
        assertThat(find(Markdown.class).all()).allSatisfy(md -> assertThat(md.getContent()).isNotBlank());
    }

    // CVE: an advisory document with no summary, no first seen date and no fix owner, beside one with all three
    @Test
    void cve_an_advisory_without_summary_first_seen_or_owner_draws_none_of_them() {
        edit("cases/cve/CVE-2026-8763.json", root -> root.remove(List.of("summary", "first_seen", "fix_owner")));
        navigate("cve/CVE-2026-8763,CVE-2026-45292/24.9.21", CveView.class);

        assertThat(sweep()).isEmpty();
        assertThat(answerText("CVE-2026-8763")).contains("CVE-2026-8763")
                .doesNotContain("Bouncy Castle: Name Constraints bypass via trailing dot in rfc822Name and URI");
        assertThat(answerText("CVE-2026-45292"))
                .contains("OpenTelemetry Java SDK has Unbounded Memory Allocation in W3C Baggage Propagation");
    }

    // Scan: a report whose components carry no vulnerabilities at all
    @Test
    void scan_a_report_with_no_vulnerabilities_draws_no_rows() throws URISyntaxException {
        navigate(HomeView.class);
        test(find(Tabs.class).withClassName("lookup-tabs").single()).select(2);
        test(find(UploadButton.class).withClassName("lookup-report-choose").all().getFirst())
                .upload(new File(getClass().getResource("/scan/cyclonedx-components-only.json").toURI()));

        assertThat(getCurrentView()).isInstanceOf(ScanView.class);
        assertThat(sweep()).isEmpty();
        assertThat(find(com.vaadin.flow.component.html.Table.class).withClassName("scan-table").all())
                .allSatisfy(table -> assertThat(table.getBodyRows()).isEmpty());
    }

    // Advisories: one entry with no severity, no verdict and no fix owner; its cells hold no element
    @Test
    void advisories_an_entry_without_severity_verdict_or_owner_has_cells_with_no_element() {
        edit("cases/index.json", root -> ((ObjectNode) root.get("advisories").get("CVE-2016-1000027"))
                .remove(List.of("severity", "applicability", "fix_owner")));
        AdvisoriesView view = navigate(AdvisoriesView.class);

        List<String> blanks = sweep();
        var grid = test(view.grid());
        int bare = -1;
        int full = -1;
        for (int row = 0; row < grid.size(); row++) {
            String cve = grid.getRow(row).cve();
            bare = "CVE-2016-1000027".equals(cve) ? row : bare;
            full = "CVE-2026-45292".equals(cve) ? row : full;
        }
        for (String column : List.of("severity", "owner", "verdict")) {
            assertThat(blanks).contains("advisories-grid " + column + " " + bare)
                    .doesNotContain("advisories-grid " + column + " " + full);
        }
        // Every other blank is an owner or a verdict the index leaves unestablished, never an id or a package.
        assertThat(blanks).allSatisfy(blank -> assertThat(blank).matches("advisories-grid (severity|owner|verdict) \\d+"));
    }

    // Cases: a case with no screen and no count draws its title and link and nothing else
    @Test
    void cases_a_case_without_screen_or_count_draws_neither() {
        edit("cases/cases.json", root -> {
            for (JsonNode item : root.get("cases")) {
                if ("version.secure".equals(item.get("id").asString())) {
                    ((ObjectNode) item).remove(List.of("screen", "count"));
                }
            }
        });
        navigate(CasesView.class);

        assertThat(sweep()).isEmpty();
        Component row = find(Div.class).withClassName("case-row").all().stream()
                .filter(div -> "version.secure".equals(div.getElement().getAttribute("data-case"))).findFirst()
                .orElseThrow();
        assertThat(classesIn(row)).contains("case-title", "case-link").doesNotContain("case-screen", "case-count");
        Component other = find(Div.class).withClassName("case-row").all().stream()
                .filter(div -> "version.fix_available".equals(div.getElement().getAttribute("data-case")))
                .findFirst().orElseThrow();
        assertThat(classesIn(other)).contains("case-screen", "case-count");
    }

    // Explorer: a line with no scan date
    @Test
    void explorer_a_line_without_a_scan_date_says_nothing_about_one() {
        edit("data/front.json", root -> {
            for (JsonNode line : root.get("lines")) {
                if ("25.3".equals(line.get("line").asString())) {
                    ((ObjectNode) line).remove("scanned_at");
                }
            }
        });
        navigate(ExplorerView.class);

        assertThat(sweep()).isEmpty();
        assertThat(rowText("25.3")).doesNotContain("scanned");
        assertThat(rowText("24.9")).contains("scanned 2026-11-21");
    }

    // Release: a record with no scanner files flag, no scan date and no Spring Boot version
    @Test
    void release_a_record_without_scanner_files_or_scan_date_offers_json_and_csv_only() {
        edit("data/25.3.5.json", root -> root.remove(List.of("scanner_files", "scanned_at")));
        navigate("findings/release/25.3/25.3.5", ReleaseView.class);

        // A finding that arrives through no product draws no dots at all, rather than an empty element.
        assertThat(sweep()).isEmpty();
        assertThat(find(Anchor.class).withClassName("release-file").all()).extracting(Anchor::getText)
                .containsExactly("JSON", "CSV");
        assertThat(pageText()).doesNotContain("Scanned").doesNotContain("scanned");
    }

    // Spring Boot: one release with no supply entry, beside one that has it
    @Test
    void spring_a_release_without_a_supply_entry_draws_no_supply() {
        edit("data/front.json", root -> ((ObjectNode) root.get("spring_boot_supply")).remove("3.5.16"));
        navigate(SpringView.class);

        assertThat(sweep()).isEmpty();
        assertThat(classesIn(bootSection("3.5.16"))).noneMatch(name -> name.startsWith("spring-supply"));
        assertThat(classesIn(bootSection("3.4.13"))).anyMatch(name -> name.startsWith("spring-supply"));
    }

    // Workbench: a package not owned by Spring Boot says nothing about Spring Boot, and leaves no blank behind
    @Test
    void workbench_a_package_not_owned_by_spring_boot_says_nothing_about_it() {
        // Drafts other tests saved, half filled on purpose, are not this page's absences.
        drafts.deleteAll();
        navigate(WorkbenchView.class);

        assertThat(sweep()).isEmpty();
        assertThat(pageText()).contains("node-forge");
    }

    // Diagnostics: before the browser describes itself, nothing about it is drawn
    @Test
    void diagnostics_before_the_browser_answers_draws_no_browser_fact() {
        navigate(DiagnosticsView.class);

        assertThat(sweep()).isEmpty();
        assertThat(pageText()).contains("Waiting for your browser to describe itself.")
                .doesNotContain("Time zone");
    }

    // The sweep finds what it is for

    @Test
    void a_planted_empty_text_holder_and_a_leaked_placeholder_are_found() {
        var problems = new ArrayList<String>();
        var planted = new Div(new Span("Fixed in"), new Span(), new com.vaadin.flow.component.Text(" "));
        walk(planted.getElement(), problems);
        assertThat(problems).singleElement().asString().startsWith("empty <span");
        assertThat(PLACEHOLDER.matcher("Fixed in null").find()).isTrue();
        assertThat(PLACEHOLDER.matcher("Optional[24.9.26]").find()).isTrue();
    }

    // The sweep

    /**
     * Walks the page. Fails on an empty text holder, an empty element in a grid
     * cell, or a placeholder anywhere. Returns the cells left blank, grid cells
     * as {@code <grid class> <column key> <row>} and table cells as
     * {@code td <row header> <position>}, for the caller to account for.
     */
    private List<String> sweep() {
        var problems = new ArrayList<String>();
        var blanks = new ArrayList<String>();
        UI.getCurrent().getChildren().forEach(child -> walk(child.getElement(), problems, blanks));
        String text = pageText();
        var leak = PLACEHOLDER.matcher(text);
        if (leak.find()) {
            problems.add("placeholder " + leak.group());
        }

        for (Grid<?> grid : allGrids()) {
            gridCells(grid, problems, blanks);
        }
        assertThat(problems).isEmpty();
        return blanks;
    }

    private static void walk(Element element, List<String> problems) {
        walk(element, problems, new ArrayList<>());
    }

    /**
     * A table cell is the table's own, like a grid cell: one with nothing in it
     * is the omission of an absent value, so it goes to {@code blanks}, named by
     * the header cell of its row. Any other empty text holder is a problem.
     */
    private static void walk(Element element, List<String> problems, List<String> blanks) {
        if (element.isTextNode() || !element.isVisible() || "true".equals(element.getAttribute("aria-hidden"))) {
            return;
        }
        boolean holdsElements = element.getChildren().anyMatch(child -> !child.isTextNode());
        boolean empty = !holdsElements && element.getTextRecursively().isBlank();
        if (empty && "td".equals(element.getTag())) {
            Element rowHeader = element.getParent().getChild(0);
            blanks.add("td " + rowHeader.getTextRecursively().strip() + " " + element.getParent().indexOfChild(element));
        } else if (empty && TEXT_HOLDERS.contains(element.getTag()) && element.getAttribute("aria-label") == null
                && element.getAttribute("title") == null) {
            problems.add("empty <" + element.getTag() + " class=\"" + element.getAttribute("class") + "\"> in "
                    + element.getParent().getTextRecursively());
        }
        element.getChildren().forEach(child -> walk(child, problems, blanks));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void gridCells(Grid grid, List<String> problems, List<String> blanks) {
        if (!grid.isVisible()) {
            return;
        }
        var tester = test(grid);
        List<Grid.Column> columns = grid.getColumns();
        for (int row = 0; row < tester.size(); row++) {
            for (int position = 0; position < columns.size(); position++) {
                Grid.Column column = columns.get(position);
                String where = String.join(" ", grid.getClassNames()) + " "
                        + (column.getKey() == null ? "column" + position : column.getKey()) + " " + row;
                if (column.getRenderer() instanceof ComponentRenderer) {
                    Component cell = (Component) tester.getCellComponent(row, position);
                    if (cell == null || cell.getElement().isTextNode()) {
                        blanks.add(where);
                    } else if (cell.getElement().getTextRecursively().isBlank()
                            && cell.getElement().getChildCount() == 0) {
                        problems.add("empty <" + cell.getElement().getTag() + "> in " + where);
                    }
                } else {
                    String text = tester.getCellText(row, position);
                    if (text == null || text.isBlank()) {
                        blanks.add(where);
                    } else if (PLACEHOLDER.matcher(text).find()) {
                        problems.add("placeholder in " + where + ": " + text);
                    }
                }
            }
        }
    }

    // Helpers

    /**
     * Serves a changed copy of one document from now on. Everything cached is
     * forgotten first, since another test class in the shared context may have
     * read the committed document already; so one test makes one edit.
     */
    private void edit(String path, Consumer<ObjectNode> change) {
        fixtures.cold();
        var root = (ObjectNode) JSON.readTree(fixtures.transport().fixture(path));
        change.accept(root);
        fixtures.transport().replace(path, JSON.writeValueAsString(root));
        fixtures.revalidate();
    }

    private List<Grid<?>> allGrids() {
        var grids = new ArrayList<Grid<?>>();
        UI.getCurrent().getChildren().forEach(child -> collectGrids(child, grids));
        return grids;
    }

    private static void collectGrids(Component from, List<Grid<?>> grids) {
        if (from instanceof Grid<?> grid) {
            grids.add(grid);
        }
        from.getChildren().forEach(child -> collectGrids(child, grids));
    }

    private static String pageText() {
        return String.join(" ", UI.getCurrent().getChildren().map(child -> child.getElement().getTextRecursively())
                .toList());
    }

    private static Set<String> classesIn(com.vaadin.flow.component.HasElement from) {
        var names = new java.util.TreeSet<String>();
        collectClasses(from.getElement(), names);
        return names;
    }

    private static void collectClasses(Element element, Set<String> names) {
        if (element.isTextNode()) {
            return;
        }
        names.addAll(element.getClassList());
        element.getChildren().forEach(child -> collectClasses(child, names));
    }

    private Component card(String line) {
        return find(RouterLink.class).withClassName("line-card").all().stream()
                .filter(card -> find(Span.class, card).withClassName("line-card-minor").all().stream()
                        .anyMatch(minor -> line.equals(minor.getText())))
                .findFirst().orElseThrow();
    }

    private String rowText(String line) {
        return find(com.vaadin.flow.component.details.Details.class).withClassName("explorer-row").all().stream()
                .filter(button -> line.equals(button.getElement().getAttribute("data-line"))).findFirst()
                .orElseThrow().getElement().getTextRecursively();
    }

    private String answerText(String cve) {
        return find(com.vaadin.flow.component.html.Section.class).withClassName("cve-answer").all().stream()
                .filter(section -> cve.equals(section.getElement().getAttribute("data-advisory")))
                .findFirst().orElseThrow().getElement().getTextRecursively();
    }

    private Component bootSection(String release) {
        return find(com.vaadin.flow.component.details.Details.class).withClassName("spring-boot").all().stream()
                .filter(section -> release.equals(section.getElement().getAttribute("data-boot"))).findFirst()
                .orElseThrow();
    }

    private static int count(String text, String part) {
        return text.split(Pattern.quote(part), -1).length - 1;
    }
}

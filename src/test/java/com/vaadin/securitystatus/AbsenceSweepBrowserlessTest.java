package com.vaadin.securitystatus;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.Table;
import com.vaadin.flow.component.markdown.Markdown;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.upload.UploadButton;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.securitystatus.security.Roles;
import com.vaadin.securitystatus.ui.customer.AdvisoriesView;
import com.vaadin.securitystatus.ui.customer.CasesView;
import com.vaadin.securitystatus.ui.customer.CveView;
import com.vaadin.securitystatus.ui.customer.HomeView;
import com.vaadin.securitystatus.ui.customer.ScanView;
import com.vaadin.securitystatus.ui.customer.VersionView;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/** POL-03 on the customer's screens: see {@link AbsenceSweep}. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@WithMockUser(username = "team", roles = Roles.SECURITY)
class AbsenceSweepBrowserlessTest extends AbsenceSweep {

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
        assertThat(find(Table.class).withClassName("scan-table").all())
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

    // The sweep finds what it is for

    @Test
    void a_planted_empty_text_holder_and_a_leaked_placeholder_are_found() {
        var problems = new ArrayList<String>();
        var planted = new Div(new Span("Fixed in"), new Span(), new Text(" "));
        walk(planted.getElement(), problems);
        assertThat(problems).singleElement().asString().startsWith("empty <span");
        assertThat(PLACEHOLDER.matcher("Fixed in null").find()).isTrue();
        assertThat(PLACEHOLDER.matcher("Optional[24.9.26]").find()).isTrue();
    }

    // Helpers

    private Component card(String line) {
        return find(RouterLink.class).withClassName("line-card").all().stream()
                .filter(card -> find(Span.class, card).withClassName("line-card-minor").all().stream()
                        .anyMatch(minor -> line.equals(minor.getText())))
                .findFirst().orElseThrow();
    }

    private String answerText(String cve) {
        return find(Section.class).withClassName("cve-answer").all().stream()
                .filter(section -> cve.equals(section.getElement().getAttribute("data-advisory")))
                .findFirst().orElseThrow().getElement().getTextRecursively();
    }
}

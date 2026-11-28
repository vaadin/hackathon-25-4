package com.vaadin.securitystatus.ui.team;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.securitystatus.AbsenceSweep;
import com.vaadin.securitystatus.security.Roles;
import com.vaadin.securitystatus.team.workbench.VerdictDraftRepository;
import com.vaadin.securitystatus.ui.team.diagnostics.DiagnosticsView;
import com.vaadin.securitystatus.ui.team.explorer.ExplorerView;
import com.vaadin.securitystatus.ui.team.explorer.ReleaseView;
import com.vaadin.securitystatus.ui.team.explorer.SpringView;
import com.vaadin.securitystatus.ui.team.workbench.WorkbenchView;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/** POL-03 on the team's screens: see {@link AbsenceSweep}. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@WithMockUser(username = "team", roles = Roles.SECURITY)
class TeamAbsenceSweepBrowserlessTest extends AbsenceSweep {

    @Autowired
    VerdictDraftRepository drafts;

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

    // Helpers

    private String rowText(String line) {
        return find(Details.class).withClassName("explorer-row").all().stream()
                .filter(button -> line.equals(button.getElement().getAttribute("data-line"))).findFirst()
                .orElseThrow().getElement().getTextRecursively();
    }

    private Component bootSection(String release) {
        return find(Details.class).withClassName("spring-boot").all().stream()
                .filter(section -> release.equals(section.getElement().getAttribute("data-boot"))).findFirst()
                .orElseThrow();
    }
}

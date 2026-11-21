package com.vaadin.securitystatus.explorer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.securitystatus.base.security.Roles;
import com.vaadin.securitystatus.data.FixturePages;
import com.vaadin.securitystatus.data.model.ExplorerFront.Finding;

/**
 * EXP-01, EXP-02, EXP-05 and EXP-06: one row per line with its badges, the
 * address naming only what is off, the groups the reader opened coming back,
 * and a full version typed in the search landing on its release.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@WithMockUser(username = "team", roles = Roles.SECURITY)
class ExplorerBrowserlessTest extends SpringBrowserlessTest {

    @Autowired
    FixturePages fixtures;

    @AfterEach
    void restore() {
        fixtures.restore();
    }

    // EXP-01
    @Test
    void one_row_per_line_with_the_badges_the_fixture_gives() {
        navigate(ExplorerView.class);

        assertThat(rows()).extracting(row -> row.getElement().getAttribute("data-line")).containsExactly("25.4",
                "25.3", "24.10", "24.9", "24.7", "24.4", "23.6", "14.14", "8.32", "7.7");
        // 25.3.5: nothing to act on, the one Spring Boot finding a false positive.
        assertThat(badges("25.3")).containsExactly("Vaadin: not affected", "Spring Boot 4.1.2: not affected");
        // 24.9.26: CVE-2026-45292 is a risk of ours, and Spring Boot brings risks of its own.
        assertThat(badges("24.9")).containsExactly("Vaadin: vulnerable", "Spring Boot 3.5.16: vulnerable");
        // 8.32 was measured without Spring: no Spring Boot badge at all, rule 4.
        assertThat(badges("8.32")).containsExactly("Vaadin: not affected");
        assertThat(text(row("25.3"))).contains("25.3.5", "Scanned 2026-11-21",
                "15 findings: 0 to act on, 0 not assessed yet, 1 from Spring Boot, 0 only in the build, 14 answered.");
    }

    // EXP-01: the opened line is the detail of the layout, choosing another replaces it
    @Test
    void opening_a_line_fills_the_detail_and_another_replaces_it() {
        var view = navigate(ExplorerView.class);
        assertThat(test(view.layout()).getDetail()).isNull();

        open("25.3");
        assertThat(test(view.layout()).getDetail().getElement().getAttribute("data-line")).isEqualTo("25.3");
        open("24.9");
        assertThat(test(view.layout()).getDetail().getElement().getAttribute("data-line")).isEqualTo("24.9");
        assertThat($(Details.class).withClassName("explorer-group").all()).extracting(Details::getSummaryText)
                .containsExactly("To act on (2)", "From your Spring Boot 3.5.16 (41)", "Only in the build (1)",
                        "Answered (69)");
    }

    // AC2: the two groups that ask for work start open, the others closed
    @Test
    void the_groups_that_ask_for_work_start_open() {
        navigate(ExplorerView.class);
        open("24.9");

        assertThat(group("RISK").isOpened()).isTrue();
        assertThat(group("SPRING").isOpened()).isFalse();
        assertThat(group("BUILD").isOpened()).isFalse();
        assertThat(group("ANSWERED").isOpened()).isFalse();
        assertThat(group("BUILD").getElement().getTextRecursively())
                .contains("Only build tooling carries these. Nothing deployed contains them.");
    }

    // EXP-02
    @Test
    void two_chips_off_are_named_in_the_address_and_nothing_else() {
        navigate(ExplorerView.class);

        test(chip("flow-webpush")).click();
        test(chip("vaadin-testbench")).click();

        assertThat(location()).startsWith("findings?");
        assertThat(query().getParameters()).containsOnlyKeys("off");
        assertThat(query().getParameters().get("off")).containsExactly("flow-webpush,vaadin-testbench");
        assertThat(chip("flow-webpush").getElement().getAttribute("aria-checked")).isEqualTo("false");
        assertThat(chip("vaadin-spreadsheet-flow").getElement().getAttribute("aria-checked")).isEqualTo("true");

        // Turning both back on leaves no flag at all.
        test(chip("flow-webpush")).click();
        test(chip("vaadin-testbench")).click();
        assertThat(location()).isEqualTo("findings");
    }

    // AC1: off only addressing by registry name, unknown names dropped
    @Test
    void a_name_matching_no_product_is_dropped_from_the_address() {
        navigate("findings?off=nothing-like-this,flow-webpush,vaadin", ExplorerView.class);

        assertThat(query().getParameters().get("off")).containsExactly("flow-webpush");
        assertThat(chip("flow-webpush").getElement().getAttribute("aria-checked")).isEqualTo("false");
    }

    // AC1, 6.7.5: a remembered selection is written into the address, never applied behind it
    @Test
    void the_selection_comes_back_into_the_address_of_a_later_visit() {
        navigate("findings?off=flow-webpush", ExplorerView.class);
        navigate("findings/release/25.3/25.3.5", ReleaseView.class);

        navigate("findings", ExplorerView.class);

        assertThat(location()).isEqualTo("findings?off=flow-webpush");

        // Everything on again is what is remembered after the tri state control turns it on.
        test($(com.vaadin.flow.component.checkbox.Checkbox.class).withClassName("explorer-all").single()).click();
        assertThat(location()).isEqualTo("findings");
        navigate("findings/release/25.3/25.3.5", ReleaseView.class);
        navigate("findings", ExplorerView.class);
        assertThat(location()).isEqualTo("findings");
    }

    // EXP-05
    @Test
    void the_groups_opened_come_back_open_after_a_reload() {
        navigate(ExplorerView.class);
        open("25.3");
        test(group("ANSWERED")).openDetails();
        open("24.9");
        test(group("RISK")).closeDetails();

        // A reload: a new view of the same session.
        navigate("findings/release/25.3/25.3.5", ReleaseView.class);
        navigate(ExplorerView.class);
        open("25.3");

        assertThat(group("ANSWERED").isOpened()).isTrue();
        assertThat(group("SPRING").isOpened()).isFalse();
        // Merged, not replaced: what was decided on another line holds here too.
        open("24.9");
        assertThat(group("RISK").isOpened()).isFalse();
        assertThat(group("ANSWERED").isOpened()).isTrue();
    }

    // EXP-06
    @Test
    void a_full_version_typed_in_the_search_lands_on_the_release() {
        navigate(ExplorerView.class);

        test(search()).setValue("Vaadin 24.9.21");

        assertThat(location()).isEqualTo("findings/release/24.9/24.9.21");
        // The title takes the route parameters in the template's order: the version, not the line.
        assertThat(UI.getCurrent().getInternals().getTitle()).isEqualTo("Release 24.9.21");
        assertThat($(Paragraph.class).withClassName("release-older").single().getText())
                .isEqualTo("The 24.9 line is read at 24.9.26, its newest measured release.");
    }

    // AC3: ?q narrows the rows and survives navigation
    @Test
    void a_search_narrows_the_rows_and_survives_navigation() {
        navigate(ExplorerView.class);

        test(search()).setValue("jackson");

        assertThat(location()).isEqualTo("findings?q=jackson");
        assertThat(rows()).extracting(row -> row.getElement().getAttribute("data-line")).containsExactly("24.10",
                "24.9", "24.7", "24.4", "23.6", "14.14", "8.32", "7.7");
        assertThat($(Paragraph.class).withClassName("explorer-search-count").single().getText())
                .isEqualTo("8 of 10 lines match");
        // A chip keeps the search in the address it writes.
        assertThat(test(chip("flow-webpush")).getQueryParameters().getParameters()).containsKeys("q", "off");

        navigate("findings/release/25.3/25.3.5", ReleaseView.class);
        navigate("findings?q=jackson", ExplorerView.class);
        assertThat(search().getValue()).isEqualTo("jackson");
        assertThat(rows()).hasSize(8);
        open("24.9");
        assertThat(findings("ANSWERED")).isNotEmpty();
        assertThat(allFindings()).hasSize(11);
    }

    // AC4, T6: the six files of the opened release
    @Test
    void the_opened_line_offers_its_six_files() {
        navigate(ExplorerView.class);
        open("25.3");

        assertThat($(Anchor.class).withClassName("release-file").all()).extracting(Anchor::getText).containsExactly(
                "JSON record", "CSV", "VEX", "Dependency-Check suppressions", "osv-scanner config",
                "bomber ignore list");
    }

    // An opened finding reads the release's record, and leaves out what it does not carry
    @Test
    void an_opened_finding_shows_what_the_release_record_carries() {
        navigate(ExplorerView.class);
        open("25.3");
        test(group("ANSWERED")).openDetails();
        Finding swing = findings("ANSWERED").stream().filter(finding -> finding.cve().equals("CVE-2021-33604"))
                .findFirst().orElseThrow();

        // The grid opens a row's details on click in the browser; the tester's clickRow does not,
        // browserless-test#29, so the test opens them as the click would.
        grid("ANSWERED").setDetailsVisible(swing, true);

        String text = $(Div.class).withClassName("explorer-finding").single().getElement().getTextRecursively();
        assertThat(text).contains("Summary", "What it means", "None. The advisory is about flow-server",
                "com.vaadin:vaadin-swing-kit-flow@3.0.1", "Advisory record of CVE-2021-33604");
        // The record carries no workaround and no fix for it, so neither is shown.
        assertThat(text).doesNotContain("What you can do today", "Fixed in");
    }

    // A release whose record is not published says so, and nothing else
    @Test
    void a_finding_of_a_release_with_no_published_record_says_so() {
        navigate(ExplorerView.class);
        open("24.9");
        Finding risk = findings("RISK").getFirst();

        grid("RISK").setDetailsVisible(risk, true);

        assertThat($(Div.class).withClassName("explorer-finding").single().getElement().getTextRecursively())
                .startsWith("The record of 24.9.26 is not published");
    }

    // Helpers

    private List<NativeButton> rows() {
        return $(NativeButton.class).withClassName("explorer-row").all();
    }

    private NativeButton row(String line) {
        return rows().stream().filter(row -> line.equals(row.getElement().getAttribute("data-line"))).findFirst()
                .orElseThrow();
    }

    private void open(String line) {
        test(row(line)).click();
    }

    private List<String> badges(String line) {
        var texts = new ArrayList<String>();
        row(line).getChildren().forEach(child -> child.getChildren().filter(Badge.class::isInstance)
                .forEach(badge -> texts.add(badge.getElement().getTextRecursively())));
        return texts;
    }

    private RouterLink chip(String product) {
        return $(RouterLink.class).withClassName("explorer-chip").all().stream()
                .filter(link -> product.equals(link.getElement().getAttribute("data-product"))).findFirst()
                .orElseThrow();
    }

    private Details group(String key) {
        return $(Details.class).withClassName("explorer-group").all().stream()
                .filter(details -> key.equals(details.getElement().getAttribute("data-group"))).findFirst()
                .orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private Grid<Finding> grid(String key) {
        return (Grid<Finding>) group(key).getContent().filter(Grid.class::isInstance).findFirst().orElseThrow();
    }

    private List<Finding> findings(String key) {
        return grid(key).getGenericDataView().getItems().toList();
    }

    private List<Finding> allFindings() {
        var all = new ArrayList<Finding>();
        $(Details.class).withClassName("explorer-group").all()
                .forEach(details -> all.addAll(findings(details.getElement().getAttribute("data-group"))));
        return all;
    }

    private TextField search() {
        return $(TextField.class).withClassName("explorer-search").single();
    }

    private static String text(Component component) {
        return component.getElement().getTextRecursively();
    }

    static String location() {
        return UI.getCurrent().getInternals().getActiveViewLocation().getPathWithQueryParameters();
    }

    static QueryParameters query() {
        return UI.getCurrent().getInternals().getActiveViewLocation().getQueryParameters();
    }
}

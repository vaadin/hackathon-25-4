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
import com.vaadin.flow.component.html.H2;
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
 * and a full version typed in the search landing on its release. The halves,
 * groups and words are the prototype's, D-204 to D-212.
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
        // 25.3.5: nothing of ours is a risk, and the three Spring Boot brings or pins are answered.
        assertThat(badges("25.3")).containsExactly("Vaadin: Not affected", "Spring Boot: Not affected");
        // 24.9.26: its two risks of ours are on versions Spring Boot pins, so they move to the other badge, D-205.
        assertThat(badges("24.9")).containsExactly("Vaadin: Not affected", "Spring Boot: Vulnerable");
        assertThat(text(row("24.9"))).contains("26 findings arrive with your Spring Boot");
        // 24.7.19: CVE-2026-45292 is ours and arrives through TestBench, which is on.
        assertThat(badges("24.7")).containsExactly("Vaadin: Vulnerable", "Spring Boot: Vulnerable");
        assertThat(text(row("24.7"))).contains("1 finding here is a real risk. Each one says what to do.");
        // 8.32 carries only what the build has: no Spring Boot badge at all, rule 4.
        assertThat(badges("8.32")).containsExactly("Vaadin: Not affected");
        assertThat(text(row("25.3"))).contains("25.3.5", "scanned 2026-11-21");
        assertThat($(H2.class).withClassName("explorer-series-heading").single().getText())
                .isEqualTo("Series (10 maintained)");
    }

    // EXP-01: a series opens under its own row, one at a time, and the address names it, D-204
    @Test
    void opening_a_line_opens_it_in_place_and_another_replaces_it() {
        navigate(ExplorerView.class);
        assertThat(rows()).noneMatch(Details::isOpened);

        open("25.3");
        assertThat(location()).isEqualTo("findings?open=25.3");
        assertThat(row("25.3").isOpened()).isTrue();
        assertThat($(Div.class).withClassName("explorer-detail").single().getElement().getAttribute("data-line"))
                .isEqualTo("25.3");
        open("24.9");
        assertThat(location()).isEqualTo("findings?open=24.9");
        assertThat(rows()).filteredOn(Details::isOpened).extracting(row -> row.getElement().getAttribute("data-line"))
                .containsExactly("24.9");
        assertThat(groupLabels()).containsExactly("14 answered, nothing to do", "26 a real risk",
                "58 answered, nothing to do", "Only in the build (1)");
        assertThat($(Paragraph.class).withClassName("explorer-half-title").all()).extracting(Paragraph::getText)
                .containsExactly("From Vaadin", "With Spring Boot");
        assertThat(hints()).contains(
                "Nothing on this release is waiting for you: every finding a scanner reports carries a verdict and none of them is a risk.",
                "14 findings on this release arrive only through products you have switched off, so they are not listed here. They still count towards the state above, which answers for the release rather than for one application.");
    }

    // AC2: the two groups that ask for work start open, the others closed
    @Test
    void the_groups_that_ask_for_work_start_open() {
        navigate(ExplorerView.class);
        open("24.7");

        assertThat(group("RISK").isOpened()).isTrue();
        assertThat(group("ANSWERED").isOpened()).isFalse();
        assertThat(group("SPRING_RISK").isOpened()).isTrue();
        assertThat(group("SPRING_ANSWERED").isOpened()).isFalse();
        assertThat(group("BUILD").isOpened()).isFalse();
        assertThat(group("BUILD").getElement().getTextRecursively())
                .contains("Only build tooling carries these. Nothing deployed contains them.");
    }

    // EXP-02: the address names what is off; the default, the discontinued product off, is no flag at all
    @Test
    void two_chips_off_are_named_in_the_address_and_nothing_else() {
        navigate(ExplorerView.class);
        assertThat(chip("control-center-starter").getElement().getAttribute("aria-checked")).isEqualTo("false");

        test(chip("flow-webpush")).click();
        test(chip("vaadin-testbench")).click();

        assertThat(location()).startsWith("findings?");
        assertThat(query().getParameters()).containsOnlyKeys("off");
        assertThat(query().getParameters().get("off"))
                .containsExactly("flow-webpush,vaadin-testbench,control-center-starter");
        assertThat(chip("flow-webpush").getElement().getAttribute("aria-checked")).isEqualTo("false");
        assertThat(chip("vaadin-spreadsheet-flow").getElement().getAttribute("aria-checked")).isEqualTo("true");

        // Turning both back on is the default again, which leaves no flag at all.
        test(chip("flow-webpush")).click();
        test(chip("vaadin-testbench")).click();
        assertThat(location()).isEqualTo("findings");

        // And the discontinued product on is everything on: a bare flag.
        test(chip("control-center-starter")).click();
        assertThat(location()).isEqualTo("findings?off");
    }

    // AC1: off only addressing by registry name, unknown names dropped
    @Test
    void a_name_matching_no_product_is_dropped_from_the_address() {
        navigate("findings?off=nothing-like-this,flow-webpush,vaadin", ExplorerView.class);

        assertThat(query().getParameters().get("off")).containsExactly("flow-webpush");
        assertThat(chip("flow-webpush").getElement().getAttribute("aria-checked")).isEqualTo("false");
        // The address names everything off, so the discontinued product it does not name is on.
        assertThat(chip("control-center-starter").getElement().getAttribute("aria-checked")).isEqualTo("true");
    }

    // AC1, 6.7.5: a remembered selection is written into the address, never applied behind it
    @Test
    void the_selection_comes_back_into_the_address_of_a_later_visit() {
        navigate("findings?off=flow-webpush", ExplorerView.class);
        navigate("findings/release/25.3/25.3.5", ReleaseView.class);

        navigate("findings", ExplorerView.class);

        assertThat(location()).isEqualTo("findings?off=flow-webpush");

        // The box at the head of the list, with some products off, goes back to the default and forgets.
        assertThat(allBox().getElement().getAttribute("data-state")).isEqualTo("some");
        test(allBox()).click();
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
        open("24.7");
        test(group("RISK")).closeDetails();

        // A reload: a new view of the same session.
        navigate("findings/release/25.3/25.3.5", ReleaseView.class);
        navigate(ExplorerView.class);
        open("25.3");

        assertThat(group("ANSWERED").isOpened()).isTrue();
        assertThat(group("SPRING_ANSWERED").isOpened()).isFalse();
        // Merged, not replaced: what was decided on another line holds here too.
        open("24.7");
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
        assertThat($(Div.class).withClassName("release-card").single().getElement().getAttribute("data-version"))
                .isEqualTo("24.9.21");
    }

    // AC3: ?q narrows the rows and survives navigation, read as the prototype reads it, D-209
    @Test
    void a_search_narrows_the_rows_and_survives_navigation() {
        navigate(ExplorerView.class);

        test(search()).setValue("jackson");

        assertThat(location()).isEqualTo("findings?q=jackson");
        assertThat($(Paragraph.class).withClassName("explorer-lead").single().getText())
                .matches("\\d+ maintained series of 10 match jackson: .*\\.");
        assertThat(noteText()).startsWith("Showing what arrives through a coordinate matching jackson: ");
        assertThat($(H2.class).withClassName("explorer-series-heading").single().getText()).isEqualTo("Series");
        int shown = rows().size();
        assertThat(shown).isBetween(1, 9);
        // A chip keeps the search in the address it writes.
        assertThat(test(chip("flow-webpush")).getQueryParameters().getParameters()).containsKeys("q", "off");

        navigate("findings/release/25.3/25.3.5", ReleaseView.class);
        navigate("findings?q=jackson", ExplorerView.class);
        assertThat(search().getValue()).isEqualTo("jackson");
        assertThat(rows()).hasSize(shown);
    }

    // D-209: a series, an id, and a name nothing carries
    @Test
    void a_series_an_id_and_nothing_are_each_answered_in_the_note() {
        navigate("findings?q=24.9", ExplorerView.class);
        assertThat(rows()).extracting(row -> row.getElement().getAttribute("data-line")).containsExactly("24.9");
        assertThat(noteText()).isEqualTo("Showing the 24.9 series and nothing else.");

        navigate("findings?q=CVE-2026-45292", ExplorerView.class);
        assertThat(noteText()).isEqualTo("Showing 1 advisory matching CVE-2026-45292.");

        navigate("findings?q=zzz", ExplorerView.class);
        assertThat(rows()).isEmpty();
        assertThat($(Paragraph.class).withClassName("explorer-lead").single().getText())
                .isEqualTo("None of the 10 maintained series carries anything matching zzz.");
        assertThat(noteText()).startsWith("We report nothing under that on any series we follow.");

        navigate("findings?q=19.0", ExplorerView.class);
        assertThat($(Paragraph.class).withClassName("explorer-lead").single().getText())
                .isEqualTo("We follow 10 maintained series and 19.0 is not one of them.");
    }

    // AC4, T6: the six files of the opened release, in the prototype's words
    @Test
    void the_opened_line_offers_its_six_files() {
        navigate(ExplorerView.class);
        open("25.3");

        assertThat($(Anchor.class).withClassName("release-file").all()).extracting(Anchor::getText).containsExactly(
                "JSON", "CSV", "VEX", "dependency-check", "osv-scanner", "bomber");
        assertThat(text($(Div.class).withClassName("explorer-detail").single())).contains("For your own tooling:",
                "Other measured releases of 25.3:");
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
        assertThat(text).contains("What it means for your application", "None. The advisory is about flow-server",
                "com.vaadin:vaadin-swing-kit-flow@3.0.1", "Read the advisory");
        // The record carries no workaround and no fix for it, so neither is shown.
        assertThat(text).doesNotContain("What you can do today", "Fixed in");
    }

    // A release whose record is not published says so, and nothing else
    @Test
    void a_finding_of_a_release_with_no_published_record_says_so() {
        navigate(ExplorerView.class);
        open("24.9");
        Finding risk = findings("SPRING_RISK").getFirst();

        grid("SPRING_RISK").setDetailsVisible(risk, true);

        assertThat($(Div.class).withClassName("explorer-finding").single().getElement().getTextRecursively())
                .startsWith("The record of 24.9.26 is not published");
    }

    // D-211: the legend the "?" opens
    @Test
    void the_help_mark_opens_the_legend_in_the_prototypes_words() {
        navigate(ExplorerView.class);
        assertThat($(com.vaadin.flow.component.html.Aside.class).withClassName("explorer-legend").exists()).isFalse();

        test($(com.vaadin.flow.component.button.Button.class).withClassName("explorer-help").single()).click();

        var legend = $(com.vaadin.flow.component.html.Aside.class).withClassName("explorer-legend").single();
        assertThat($(com.vaadin.flow.component.button.Button.class).withClassName("explorer-help").exists()).isFalse();
        assertThat(text(legend)).contains("What the words on this page mean", "The badge on a series",
                "The groups inside a series", "How far a verdict was taken", "What this page is measuring",
                "Reproduced against a Vaadin application");
    }

    // Helpers

    private List<Details> rows() {
        return $(Details.class).withClassName("explorer-row").all();
    }

    private Details row(String line) {
        return rows().stream().filter(row -> line.equals(row.getElement().getAttribute("data-line"))).findFirst()
                .orElseThrow();
    }

    private void open(String line) {
        test(row(line)).openDetails();
    }

    private List<String> badges(String line) {
        var texts = new ArrayList<String>();
        row(line).getSummary().getChildren().filter(Badge.class::isInstance)
                .forEach(badge -> texts.add(badge.getElement().getTextRecursively()));
        return texts;
    }

    private RouterLink chip(String product) {
        return $(RouterLink.class).withClassName("explorer-chip").all().stream()
                .filter(link -> product.equals(link.getElement().getAttribute("data-product"))).findFirst()
                .orElseThrow();
    }

    private RouterLink allBox() {
        return $(RouterLink.class).withClassName("explorer-all").single();
    }

    private Details group(String key) {
        return $(Details.class).withClassName("explorer-group").all().stream()
                .filter(details -> key.equals(details.getElement().getAttribute("data-group"))).findFirst()
                .orElseThrow();
    }

    private List<String> groupLabels() {
        return $(Details.class).withClassName("explorer-group").all().stream()
                .map(details -> details.getSummary().getElement().getTextRecursively()).toList();
    }

    private List<String> hints() {
        return $(Paragraph.class).withClassName("explorer-hint").all().stream().map(Paragraph::getText).toList();
    }

    private String noteText() {
        return $(Div.class).withClassName("explorer-note").single().getChildren()
                .map(child -> child.getElement().getTextRecursively()).reduce((a, b) -> a + " " + b).orElse("");
    }

    @SuppressWarnings("unchecked")
    private Grid<Finding> grid(String key) {
        return (Grid<Finding>) group(key).getContent().filter(Grid.class::isInstance).findFirst().orElseThrow();
    }

    private List<Finding> findings(String key) {
        return grid(key).getGenericDataView().getItems().toList();
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

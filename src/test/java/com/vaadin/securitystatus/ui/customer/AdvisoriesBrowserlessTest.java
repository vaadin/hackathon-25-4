package com.vaadin.securitystatus.ui.customer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.securitystatus.customer.LookupState;
import com.vaadin.securitystatus.data.FixturePages;
import com.vaadin.securitystatus.data.SecurityDataService;
import com.vaadin.securitystatus.data.model.CasesIndex.AdvisoryCatalogEntry;

/**
 * ADV-01 to ADV-03: the whole catalogue worst first, the verdict pills and
 * their flag, and the context menu built for the row it opens on.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@WithMockUser(username = "guest")
class AdvisoriesBrowserlessTest extends AdvisoriesTesting {

    @Autowired
    FixturePages fixtures;

    @Autowired
    SecurityDataService data;

    @Autowired
    ObjectProvider<LookupState> lookups;

    @AfterEach
    void restore() {
        fixtures.restore();
    }

    // ADV-01
    @Test
    void the_list_is_the_whole_catalogue_in_fixture_order_worst_first() {
        var view = navigate(AdvisoriesView.class);

        List<AdvisoryCatalogEntry> rows = rows(view.grid());
        List<String> published = new ArrayList<>(data.index().advisories().keySet());
        assertThat(rows).extracting(AdvisoryCatalogEntry::cve).containsExactlyElementsOf(published);
        assertThat(rows).hasSize(535);
        assertThat(rows.get(0).cve()).isEqualTo("CVE-2016-1000027");
        assertThat(rows).extracting(row -> row.severity().orElse("")).startsWith("critical")
                .endsWith("low");
        // Severity is never better further up the list.
        var rank = List.of("critical", "high", "moderate", "low");
        for (int i = 1; i < rows.size(); i++) {
            assertThat(rank.indexOf(rows.get(i).severity().orElseThrow()))
                    .isGreaterThanOrEqualTo(rank.indexOf(rows.get(i - 1).severity().orElseThrow()));
        }
        assertThat($(Paragraph.class).withClassName("advisories-total").single().getText())
                .isEqualTo("535 advisories, worst first");
    }

    // ADV-01: the cells of a row, with the severity, the owner chip and the verdict in customer words
    @Test
    void a_row_shows_id_severity_owner_package_and_verdict() {
        var view = navigate(AdvisoriesView.class);
        var grid = test(view.grid());

        assertThat(text(grid.getCellComponent(0, "package"))).isEqualTo("org.springframework:spring-web");
        assertThat(text(grid.getCellComponent(0, "id"))).isEqualTo("CVE-2016-1000027");
        assertThat(text(grid.getCellComponent(0, "severity"))).isEqualTo("Critical");
        assertThat(text(grid.getCellComponent(0, "owner"))).isEqualTo("Vaadin issue");
        assertThat(text(grid.getCellComponent(0, "verdict"))).isEqualTo("affects you");
        // The risk verdict in the error badge, then the row's chevron, D-194.
        Badge risk = $(Badge.class, grid.getCellComponent(0, "verdict")).single();
        assertThat(risk.getElement().getThemeList()).contains("error");
        assertThat(grid.getCellComponent(0, "verdict").getElement().getChildren()
                .anyMatch(child -> child.getClassList().contains("advisory-chevron"))).isTrue();
        // Any other verdict in the plain badge.
        Badge other = $(Badge.class, grid.getCellComponent(1, "verdict")).single();
        assertThat(other.getElement().getThemeList()).doesNotContain("error");
    }

    // D-194: a row with several packages lists every one whole, one to a line, never joined and cut
    @Test
    void every_package_of_a_row_is_listed_whole() {
        var view = navigate(AdvisoriesView.class);
        var grid = test(view.grid());
        List<AdvisoryCatalogEntry> rows = rows(view.grid());

        int several = java.util.stream.IntStream.range(0, rows.size()).filter(i -> rows.get(i).packages().size() > 1)
                .findFirst().orElseThrow();
        Component cell = grid.getCellComponent(several, "package");
        assertThat(cell.getChildren().map(child -> child.getElement().getText()).toList())
                .containsExactlyElementsOf(rows.get(several).packages());
    }

    // AC1: owner chips follow the fix owner rule, D-51
    @Test
    void owner_chips_follow_the_fix_owner_rule() {
        var view = navigate(AdvisoriesView.class);
        var grid = test(view.grid());
        List<AdvisoryCatalogEntry> rows = rows(view.grid());

        for (int i = 0; i < 20; i++) {
            String owner = rows.get(i).fixOwner().orElse("");
            Component cell = grid.getCellComponent(i, "owner");
            switch (owner) {
                case "vaadin" -> assertThat(text(cell)).isEqualTo("Vaadin issue");
                case "upstream" -> assertThat(text(cell)).isEqualTo("Dependency issue");
                default -> assertThat(cell).isNotInstanceOf(Badge.class);
            }
        }
        // CVE-2019-10202 has the owner unknown: no chip, never "Vaadin issue".
        assertThat(rows.get(1).fixOwner()).contains("unknown");
        assertThat(text(grid.getCellComponent(1, "owner"))).isEmpty();
    }

    // ADV-02
    @Test
    void the_risk_pill_shows_only_risk_rows_and_writes_the_flag() {
        navigate(AdvisoriesView.class);
        RouterLink risk = pill("verdict-risk");
        assertThat(text(risk)).isEqualTo("affects you150");
        assertThat($(Span.class, risk).withClassName("pill-count").single().getText()).isEqualTo("150");
        assertThat(risk.getHref()).isEqualTo("advisories?verdict=risk");

        var view = navigate(risk.getHref(), AdvisoriesView.class);

        List<AdvisoryCatalogEntry> rows = rows(view.grid());
        assertThat(rows).hasSize(150).allSatisfy(row -> assertThat(row.applicability()).contains("risk"));
        assertThat(pill("verdict-risk").hasClassName("active")).isTrue();
        assertThat(pill("verdict-risk").getElement().getAttribute("aria-current")).isEqualTo("true");
        assertThat($(Paragraph.class).withClassName("advisories-total").single().getText())
                .isEqualTo("150 of 535 advisories shown: affects you");
        // The All pill takes the filter away.
        assertThat(pills().get(0).getHref()).isEqualTo("advisories");
        assertThat(text(pills().get(0))).isEqualTo("All535");
    }

    // AC1: one pill per verdict present, in the prototype's order, counts as each would show, D-192.
    // Every advisory in triage on the fixture is on no current release, so it is listed as such.
    @Test
    void one_pill_per_verdict_present_with_its_count() {
        navigate(AdvisoriesView.class);

        assertThat(pills()).extracting(pill -> pill.getText() + " " + $(Span.class, pill).single().getText())
                .containsExactly("All 535", "affects you 150", "used only in specific configurations 114",
                        "used, no risk 74", "not used by Vaadin 32", "scanner false positive 38",
                        "already fixed in a release 5", "not on a current release 122");
    }

    // Edge cases: an unknown verdict is ignored, and so is one no advisory carries
    @Test
    void an_unknown_or_absent_verdict_is_ignored() {
        var view = navigate("advisories?verdict=bogus", AdvisoriesView.class);
        assertThat(rows(view.grid())).hasSize(535);
        assertThat(pills().get(0).hasClassName("active")).isTrue();

        view = navigate("advisories?verdict=in_triage", AdvisoriesView.class);
        assertThat(rows(view.grid())).hasSize(535);
        assertThat(pills()).noneMatch(pill -> pill.hasClassName("verdict-in_triage"));
    }

    // A row navigates to its answer by CVE
    @Test
    void a_row_opens_the_answer_by_cve() {
        var view = navigate(AdvisoriesView.class);

        test(view.grid()).clickRow(0);

        assertThat(getCurrentView()).isInstanceOf(CveView.class);
        assertThat(currentLocation()).isEqualTo("cve/CVE-2016-1000027");
    }

    // ADV-03
    @Test
    void the_context_menu_names_the_rows_id_and_record_host() {
        var view = navigate(AdvisoriesView.class);
        var menu = openMenu(view, 0);

        assertThat(itemTexts(view)).containsExactly("Copy CVE-2016-1000027", "Open the record at www.cve.org",
                "Ask about CVE-2016-1000027");
        Anchor record = $(Anchor.class, view.menu()).single();
        assertThat(record.getHref()).isEqualTo("https://www.cve.org/CVERecord?id=CVE-2016-1000027");
        assertThat(record.getTarget()).contains("_blank");
        menu.close();

        // A GHSA row: the record lives on GitHub. The menu is built again for it.
        int ghsa = indexOf(view.grid(), "GHSA-gr4j-r575-g665");
        openMenu(view, ghsa);
        assertThat(itemTexts(view)).containsExactly("Copy GHSA-gr4j-r575-g665",
                "Open the record at github.com", "Ask about GHSA-gr4j-r575-g665");
        assertThat($(Anchor.class, view.menu()).single().getHref())
                .isEqualTo("https://github.com/advisories/GHSA-gr4j-r575-g665");
    }

    // AC2: the copy item is armed with the row's id, which it also carries as data-answer, as CveView's control does
    @Test
    void the_copy_item_is_armed_with_the_rows_id() {
        var view = navigate(AdvisoriesView.class);
        openMenu(view, 2);

        var copy = view.menu().getItems().get(0);
        assertThat(copy.hasClassName("menu-copy")).isTrue();
        assertThat(copy.getElement().getText()).isEqualTo("Copy CVE-2021-26291");
        assertThat(copy.getElement().getAttribute("data-answer")).isEqualTo("CVE-2021-26291");
    }

    // Ask about it, P-14: with no version given, the answer for the id across the lines we follow
    @Test
    void ask_opens_the_answer_for_the_id() {
        var view = navigate(AdvisoriesView.class);
        openMenu(view, 0).clickItem("Ask about CVE-2016-1000027");

        assertThat(getCurrentView()).isInstanceOf(CveView.class);
        assertThat(currentLocation()).isEqualTo("cve/CVE-2016-1000027");
    }

    // Ask about it on my version, P-14: the version the reader already gave goes with it
    @Test
    void ask_on_my_version_opens_the_answer_for_the_known_version() {
        lookups.getObject().version().set("24.9.21");
        var view = navigate(AdvisoriesView.class);
        openMenu(view, 0).clickItem("Ask about CVE-2016-1000027 on my version");

        assertThat(getCurrentView()).isInstanceOf(CveView.class);
        assertThat(currentLocation()).isEqualTo("cve/CVE-2016-1000027/24.9.21");
    }

    // Helpers

    private static int indexOf(Grid<AdvisoryCatalogEntry> grid, String id) {
        List<AdvisoryCatalogEntry> rows = rows(grid);
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).cve().equals(id)) {
                return i;
            }
        }
        throw new AssertionError(id);
    }

    /** The text of each item as a reader sees it: the record item is a link, whose text the tester's getItemTexts does not read. */
    private static List<String> itemTexts(AdvisoriesView view) {
        return view.menu().getItems().stream().map(item -> item.getElement().getTextRecursively()).toList();
    }

    private RouterLink pill(String className) {
        return $(RouterLink.class).withClassName(className).single();
    }

    private static String text(Component component) {
        return component.getElement().getTextRecursively();
    }

}

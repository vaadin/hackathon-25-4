package com.vaadin.securitystatus.ui.customer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.securitystatus.data.FixturePages;

/**
 * ADV-04: the case catalogue in the source page's three groups, each
 * renderable case with its count and a See one link that resolves, and a case
 * whose example no longer shows it moved to none today.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@WithMockUser(username = "guest")
class CasesBrowserlessTest extends SpringBrowserlessTest {

    private static final String CASES = "cases/cases.json";

    @Autowired
    FixturePages fixtures;

    @AfterEach
    void restore() {
        fixtures.restore();
    }

    // ADV-04
    @Test
    void three_groups_in_the_source_order_with_their_counts() {
        navigate(CasesView.class);

        // The prototype's heading is the answer's H2, the groups its H3s with their asides, D-195.
        assertThat($(H2.class).all()).extracting(H2::getText).containsExactly("Every case, with a real example");
        assertThat($(H3.class).all()).extracting(H3::getText).containsExactly(
                "What the design asks for and the measurements cannot give", "Cases that do not occur today",
                "Cases you can go and look at");
        assertThat($(Span.class).withClassName("section-aside").all()).extracting(Span::getText).containsExactly(
                "Measured on the payload of 2026-11-21", "5 of them", "The layout exists and nothing reaches it",
                "31 of them");
        assertThat(ids("cases-impossible")).containsExactly("fix_eta", "advisory_published", "api_changes",
                "ticket_answer", "notify");
        assertThat(ids("cases-none_today")).containsExactly("cve.no_workaround", "cve.fix_means.mitigated",
                "field.fixed_in_enterprise");
    }

    // ADV-04: every renderable case has its count and a See one link to its example's own address
    @Test
    void every_renderable_case_has_a_count_and_a_see_one_link() {
        navigate(CasesView.class);

        List<Div> rows = rows("cases-renderable");
        assertThat(rows).hasSize(31).allSatisfy(row -> {
            assertThat($(RouterLink.class, row).single().getText()).isEqualTo("See one");
            assertThat($(Span.class, row).withClassName("case-count").single().getText()).matches("\\d+");
        });
        // The most frequent first, as the prototype orders them, D-195.
        List<Integer> counts = rows.stream()
                .map(row -> Integer.valueOf($(Span.class, row).withClassName("case-count").single().getText())).toList();
        assertThat(counts).isSortedAccordingTo(Comparator.reverseOrder());
        // A case that cannot be drawn has no count and the cannot chip; none today counts 0.
        assertThat($(Span.class, row("fix_eta")).withClassName("case-count").exists()).isFalse();
        assertThat(row("fix_eta").getElement().getTextRecursively()).contains("cannot");
        assertThat($(Span.class, row("cve.no_workaround")).withClassName("case-count").single().getText())
                .isEqualTo("0");
        assertThat(link("version.secure").getHref()).isEqualTo("version/7.7.50");
        assertThat(link("version.spring_pinned").getHref()).isEqualTo("version/23.6.0?boot=1");
        assertThat(link("cve.applicability.risk").getHref()).isEqualTo("cve/CVE-2016-1000027/23.6.19");
        assertThat(link("cve.ghsa_only").getHref()).isEqualTo("cve/GHSA-gr4j-r575-g665/14.14.9");
        // Nothing renders an impossible or a none today case as a link.
        assertThat($(RouterLink.class, section("cases-impossible")).all()).isEmpty();
        assertThat($(RouterLink.class, section("cases-none_today")).all()).isEmpty();
    }

    // ADV-04: the links resolve, each to the screen it names
    @Test
    void every_see_one_link_resolves_to_its_screen() {
        navigate(CasesView.class);
        List<String> hrefs = $(RouterLink.class, section("cases-renderable")).all().stream()
                .map(RouterLink::getHref).toList();

        for (String href : hrefs) {
            Class<?> expected = href.startsWith("version/") ? VersionView.class : CveView.class;
            var view = navigate(href, expected.asSubclass(Component.class));
            assertThat(view).as(href).isInstanceOf(expected).isNotInstanceOf(NotFoundView.class);
            assertThat(location()).as(href).isEqualTo(href);
        }
    }

    // The impossible case says what it needs, in the catalogue's words
    @Test
    void an_impossible_case_says_what_it_needs() {
        navigate(CasesView.class);

        assertThat(row("fix_eta").getElement().getTextRecursively())
                .contains("Would need: fix_expected_version filled in when a verdict promises a release");
    }

    // Edge case: a case pointing at data that no longer exhibits it moves to none today
    @Test
    void a_case_whose_example_no_longer_shows_it_is_none_today() {
        fixtures.cold();
        String cases = fixtures.transport().fixture(CASES);
        fixtures.transport().replace(CASES, cases
                // 14.14.0 is unresolved, not secure.
                .replace("\"example\":\"#version/7.7.50\"", "\"example\":\"#version/14.14.0\"")
                // A release the index has never heard of.
                .replace("\"example\":\"#version/14.14.9\"", "\"example\":\"#version/1.2.3\"")
                // An advisory judged risk is not a no risk example.
                .replace("\"example\":\"#cve/CVE-2020-7660/23.6.19\"",
                        "\"example\":\"#cve/CVE-2016-1000027/23.6.19\""));

        navigate(CasesView.class);

        assertThat(ids("cases-none_today")).contains("version.secure", "version.stuck", "cve.applicability.no_risk");
        assertThat(ids("cases-renderable")).doesNotContain("version.secure", "version.stuck",
                "cve.applicability.no_risk");
    }

    // /cases/<id> goes to the example; an unknown or impossible case shows the catalogue
    @Test
    void the_case_address_opens_its_example() {
        navigate("cases/version.spring_pinned", VersionView.class);
        assertThat(location()).isEqualTo("version/23.6.0?boot=1");

        navigate("cases/cve.applicability.fixed", CveView.class);
        assertThat(location()).isEqualTo("cve/CVE-2026-116796/25.4.0-beta2");

        navigate("cases/notify", CasesView.class);
        assertThat(location()).isEqualTo("cases");

        navigate("cases/no.such.case", CasesView.class);
        assertThat(location()).isEqualTo("cases");
    }

    // Helpers

    private Section section(String className) {
        return $(Section.class).withClassName(className).single();
    }

    private List<Div> rows(String sectionClass) {
        return $(Div.class, section(sectionClass)).withClassName("case-row").all();
    }

    private List<String> ids(String sectionClass) {
        return rows(sectionClass).stream().map(row -> row.getElement().getAttribute("data-case")).toList();
    }

    private Div row(String id) {
        return $(Div.class).withClassName("case-row").withAttribute("data-case", id).single();
    }

    private RouterLink link(String id) {
        return $(RouterLink.class, row(id)).single();
    }

    private static String location() {
        return UI.getCurrent().getInternals().getActiveViewLocation().getPathWithQueryParameters();
    }
}

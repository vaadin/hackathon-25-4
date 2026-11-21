package com.vaadin.securitystatus.status;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.securitystatus.base.ui.DataOutageNotice;
import com.vaadin.securitystatus.data.FixturePages;

/**
 * HOM-01, HOM-02 and HOM-08: the line cards tell each line's truth from the
 * index fixture, prereleases come only with {@code ?pre}, and recently
 * reviewed lists eight advisories in the customer's words.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class HomeBrowserlessTest extends SpringBrowserlessTest {

    private static final String INDEX = "cases/index.json";

    @Autowired
    FixturePages fixtures;

    @AfterEach
    void restore() {
        fixtures.restore();
    }

    // HOM-01
    @Test
    void one_card_per_followed_line_newest_first_without_prereleases() {
        navigate(HomeView.class);

        assertThat(texts("line-card-minor")).containsExactly("25.3", "24.10", "24.9", "24.7", "24.4", "23.6", "14.14",
                "8.32", "7.7");
        assertThat(cards()).extracting(RouterLink::getHref).containsExactly("version/25.3.5", "version/24.10.14",
                "version/24.9.26", "version/24.7.19", "version/24.4.28", "version/23.6.19", "version/14.14.9",
                "version/8.32.3", "version/7.7.56");
    }

    // HOM-01, AC3: state word and icon match the index state for every line
    @Test
    void each_card_says_the_state_of_its_newest_release_with_a_word_and_an_icon() {
        navigate(HomeView.class);

        // The fixture's summary for each newest release: secure, or unresolved
        // with risk 1, 25, 13 and 13 and nothing in triage.
        assertThat(texts("line-card-state")).containsExactly("Secure", "Secure", "Secure", "1 finding open",
                "25 findings open", "13 findings open", "13 findings open", "Secure", "Secure");
        assertThat(cards()).extracting(card -> stateClass(card)).containsExactly("state-secure", "state-secure",
                "state-secure", "state-unresolved", "state-unresolved", "state-unresolved", "state-unresolved",
                "state-secure", "state-secure");
        assertThat(cards()).allSatisfy(card -> assertThat(icons(card)).hasSize(1));
        assertThat(icons(cards().get(0)).get(0).getElement().getAttribute("icon")).isEqualTo("vaadin:check-circle");
        assertThat(icons(cards().get(3)).get(0).getElement().getAttribute("icon")).isEqualTo("vaadin:warning");
    }

    // HOM-01: the action line and the scanned date
    @Test
    void each_card_says_what_to_do_and_when_it_was_scanned() {
        navigate(HomeView.class);

        // 24.9.24, .25 and .26 are secure and .23 is not, so 24.9 is secure from 24.9.24.
        assertThat(texts("line-card-action")).containsExactly("On 25.3.5 or later", "On 24.10.12 or later",
                "On 24.9.24 or later", "Newest is 24.7.19", "Newest is 24.4.28", "Newest is 23.6.19",
                "Newest is 14.14.9", "On 8.32.0 or later", "On 7.7.50 or later");
        assertThat(texts("line-card-scanned")).containsOnly("Scanned 2026-11-21");
    }

    // AC3: the action line names upgrade_to when the index has one
    @Test
    void the_action_line_names_the_upgrade_the_index_gives() {
        String index = fixtures.transport().fixture(INDEX);
        String moved = index.replace(
                "\"24.7.19\":{\"line\":\"24.7\",\"state\":\"unresolved\",\"scanned_at\":\"2026-11-21T03:25:53+00:00\",\"risk\":1,\"triage\":0,\"ruled_out\":102,\"theirs\":99,\"theirs_risk\":59,\"spring_pinned\":7,\"discontinued\":4,\"from_build\":1,\"upgrade_to\":null}",
                "\"24.7.19\":{\"line\":\"24.7\",\"state\":\"fix_available\",\"scanned_at\":\"2026-11-21T03:25:53+00:00\",\"risk\":1,\"triage\":0,\"upgrade_to\":\"24.7.20\"}");
        assertThat(moved).isNotEqualTo(index);
        replaceIndex(moved);

        reopenHome();

        assertThat(texts("line-card-action").get(3)).isEqualTo("Upgrade to 24.7.20");
        assertThat(texts("line-card-state").get(3)).isEqualTo("Fix available");
    }

    // AC3: Not assessed yet is said out loud
    @Test
    void a_line_whose_open_findings_are_all_in_triage_says_not_assessed_yet() {
        String index = fixtures.transport().fixture(INDEX);
        String moved = index.replace("\"24.7.19\":{\"line\":\"24.7\",\"state\":\"unresolved\",\"scanned_at\":\"2026-11-21T03:25:53+00:00\",\"risk\":1,\"triage\":0",
                "\"24.7.19\":{\"line\":\"24.7\",\"state\":\"unresolved\",\"scanned_at\":\"2026-11-21T03:25:53+00:00\",\"risk\":0,\"triage\":2");
        assertThat(moved).isNotEqualTo(index);
        replaceIndex(moved);

        reopenHome();

        assertThat(texts("line-card-state").get(3)).isEqualTo("Not assessed yet");
        assertThat(icons(cards().get(3)).get(0).getElement().getAttribute("icon")).isEqualTo("vaadin:clock");
    }

    @Test
    void the_extended_tier_is_marked_on_the_card_with_the_line_label() {
        navigate(HomeView.class);

        List<Badge> tiers = find(Badge.class).withClassName("line-card-tier").all();
        assertThat(tiers).hasSize(8);
        assertThat(tiers).allSatisfy(badge -> {
            assertThat(badge.getText()).isEqualTo("Extended");
            assertThat(badge.getElement().getAttribute("title")).isEqualTo("Extended maintenance");
        });
    }

    // HOM-02
    @Test
    void the_pre_flag_lists_the_prerelease_lines_too() {
        navigate("?pre", HomeView.class);

        assertThat(texts("line-card-minor")).containsExactly("25.4", "25.3", "24.10", "24.9", "24.7", "24.4", "23.6",
                "14.14", "8.32", "7.7");
        assertThat(cards().get(0).getHref()).isEqualTo("version/25.4.0-beta2");
    }

    // HOM-02, the reference's own spelling of the flag
    @Test
    void the_pre_flag_with_a_value_counts_too() {
        navigate("?pre=1", HomeView.class);

        assertThat(texts("line-card-minor")).first().isEqualTo("25.4");
    }

    // HOM-08
    @Test
    void recently_reviewed_lists_eight_advisories_in_the_customers_words() {
        navigate(HomeView.class);

        List<RouterLink> rows = find(RouterLink.class).withClassName("recent-row").all();
        assertThat(rows).hasSize(8);
        assertThat(texts("recent-id")).containsExactly("CVE-2026-45292", "CVE-2026-118259", "CVE-2026-18401",
                "CVE-2026-19032", "CVE-2025-48734", "CVE-2020-15250", "CVE-2023-42503", "CVE-2025-48924");
        assertThat(rows.get(0).getHref()).isEqualTo("cve/CVE-2026-45292");
        assertThat(texts("recent-says").get(0)).isEqualTo(
                "None unless a Vaadin 24 application declares observability-kit-starter and whoever can send a baggage header can reach it.");
        assertThat(texts("recent-says").get(2)).isEqualTo("None in a deployed application: it carries no Jackson.");
        assertThat(find(Badge.class).withClassName("verdict-chip").all()).extracting(Badge::getText).containsExactly(
                "affects you", "used only in specific configurations", "not used by Vaadin", "not used by Vaadin",
                "not used by Vaadin", "not used by Vaadin", "not used by Vaadin", "not used by Vaadin");
        assertThat(texts("recent-date").get(0)).isEqualTo("2026-11-17");
    }

    // HOM-08: one row per advisory, though a verdict is written per package
    @Test
    void an_advisory_judged_twice_is_listed_once() {
        String index = fixtures.transport().fixture(INDEX);
        String twice = index.replace("\"recent\":[", "\"recent\":[{\"id\":\"again\",\"cves\":[\"CVE-2026-45292\"],\"applicability\":\"risk\",\"impact\":\"Said twice.\"},");
        assertThat(twice).isNotEqualTo(index);
        replaceIndex(twice);

        reopenHome();

        assertThat(texts("recent-id")).hasSize(8).doesNotHaveDuplicates().first().isEqualTo("CVE-2026-45292");
        assertThat(texts("recent-says").get(0)).isEqualTo("Said twice.");
    }

    @Test
    void the_advisories_link_carries_the_count_and_the_pointer_is_the_designers() {
        navigate(HomeView.class);

        RouterLink all = find(RouterLink.class).withClassName("recent-all").single();
        assertThat(all.getText()).isEqualTo("All 535 advisories");
        assertThat(all.getHref()).isEqualTo("advisories");
        assertThat(find(Component.class).withClassName("home-pointer").single().getElement().getTextRecursively())
                .contains("Scanner flagged Tomcat, Spring or logback?").contains("managed by Spring Boot");
    }

    @Test
    void with_the_data_out_of_reach_home_says_so_and_invents_no_card() {
        fixtures.coldAndDown();

        reopenHome();

        assertThat(find(DataOutageNotice.class).exists()).isTrue();
        assertThat(find(RouterLink.class).withClassName("line-card").exists()).isFalse();
    }

    // Helpers

    /**
     * Another index from now on. Asked of the host at once, because the open
     * window reads the index again as soon as anything moves, and would cache
     * the committed one first.
     */
    private void replaceIndex(String body) {
        fixtures.transport().replace(INDEX, body);
        fixtures.revalidate();
    }

    /** The window opens on home, so the view is built again only after leaving it. */
    private void reopenHome() {
        navigate(AdvisoriesView.class);
        navigate(HomeView.class);
    }

    private List<RouterLink> cards() {
        return find(RouterLink.class).withClassName("line-card").all();
    }

    private List<String> texts(String className) {
        return find(Span.class).withClassName(className).all().stream().map(Span::getText).toList();
    }

    private static List<Icon> icons(Component card) {
        return card.getChildren().flatMap(Component::getChildren).filter(Icon.class::isInstance)
                .map(Icon.class::cast).toList();
    }

    private static String stateClass(RouterLink card) {
        return card.getClassNames().stream().filter(name -> name.startsWith("state-")).findFirst().orElse("");
    }
}

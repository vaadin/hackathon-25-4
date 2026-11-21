package com.vaadin.securitystatus.explorer;

import static com.vaadin.securitystatus.explorer.ExplorerBrowserlessTest.location;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.securitystatus.base.security.Roles;
import com.vaadin.securitystatus.base.ui.NotFoundView;
import com.vaadin.securitystatus.data.model.ExplorerFront.Finding;

/**
 * REL-01 and REL-02: a measured release read in full, card, advice,
 * downloads and lookup, from the committed record of 24.9.21; and a release
 * nobody measured answered in place, with the line's measured releases
 * listed.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@WithMockUser(username = "team", roles = Roles.SECURITY)
class ReleaseViewBrowserlessTest extends SpringBrowserlessTest {

    // REL-01
    @Test
    void a_measured_release_shows_its_card_and_advice_from_the_fixture() {
        navigate("findings/release/24.9/24.9.21", ReleaseView.class);

        // Rule 6.1.7: the heading names the version, and the document title follows.
        assertThat($(H1.class).single().getText()).isEqualTo("Release 24.9.21");
        assertThat(UI.getCurrent().getInternals().getTitle()).isEqualTo("Release 24.9.21");
        Div card = $(Div.class).withClassName("release-card").single();
        assertThat(card.getElement().getAttribute("data-version")).isEqualTo("24.9.21");
        assertThat($(Badge.class).withClassName("explorer-badge").all())
                .extracting(badge -> badge.getElement().getTextRecursively())
                .containsExactly("Vaadin: vulnerable", "Spring Boot 3.5.15: vulnerable");
        // The record's own counts: 2 risk, 15 unjudged, 41 from Spring Boot, 1 in the build, 76 ruled out.
        assertThat(paragraph("release-card-summary")).isEqualTo(
                "135 findings: 2 to act on, 15 not assessed yet, 41 from Spring Boot, 1 only in the build, 76 answered.");
        assertThat(paragraph("release-advice-update")).isEqualTo(
                "Updating to 24.9.26, a patch of the 24.9 line you are on, removes 22 of these: CVE-2026-116804, "
                        + "CVE-2026-8763, CVE-2026-102276, CVE-2026-102278, CVE-2026-102554, CVE-2026-112942 "
                        + "and 16 more. Read 24.9.26");
        assertThat(paragraph("release-advice-spring")).isEqualTo(
                "41 findings on this release come with Spring Boot rather than with Vaadin. They were measured "
                        + "against Spring Boot 3.5.15, which this release also declares. Spring Boot 3.5.15 can move "
                        + "to 3.5.16, the newest patch of that line. That is where fixes for these arrive, and no "
                        + "Vaadin release moves them. What Spring Boot brings");
        assertThat(link("release-spring-link").getHref()).isEqualTo("findings/spring");
        // Rule 6.3.14: 24.9.21 has no scanner files, only its record and its CSV.
        assertThat($(Anchor.class).withClassName("release-file").all()).extracting(Anchor::getText)
                .containsExactly("JSON record", "CSV");
        assertThat($(Details.class).withClassName("explorer-group").all())
                .extracting(details -> details.getElement().getAttribute("data-group"))
                .containsExactly("RISK", "UNJUDGED", "SPRING", "BUILD", "ANSWERED");
        assertThat(lookup().getValue()).isEqualTo("24.9.21");
        // Rule 6.1.6: one way back, above the content.
        assertThat(link("release-back").getHref()).isEqualTo("findings");
    }

    @Test
    void the_newest_release_says_so_and_offers_its_six_files() {
        navigate("findings/release/25.3/25.3.5", ReleaseView.class);

        assertThat(paragraph("release-advice-update")).isEqualTo("This is the newest measured release of 25.3.");
        assertThat($(Anchor.class).withClassName("release-file").all()).extracting(Anchor::getText).containsExactly(
                "JSON record", "CSV", "VEX", "Dependency-Check suppressions", "osv-scanner config",
                "bomber ignore list");
    }

    @Test
    void an_opened_finding_reads_the_release_record() {
        navigate("findings/release/24.9/24.9.21", ReleaseView.class);
        Grid<Finding> grid = grid("RISK");
        Finding risk = grid.getGenericDataView().getItems().filter(item -> item.cve().equals("CVE-2026-45292"))
                .findFirst().orElseThrow();

        // The tester's clickRow does not open item details, browserless-test#29.
        grid.setDetailsVisible(risk, true);

        assertThat($(Div.class).withClassName("explorer-finding").single().getElement().getTextRecursively())
                .contains("OpenTelemetry Java SDK has Unbounded Memory Allocation",
                        "Advisory record of CVE-2026-45292");
    }

    // REL-02
    @Test
    void an_unmeasured_version_is_answered_in_place_with_the_measured_releases() {
        navigate("findings/release/24.9/24.9.15", ReleaseView.class);

        assertThat(location()).isEqualTo("findings/release/24.9/24.9.15");
        assertThat($(Div.class).withClassName("release-card").exists()).isFalse();
        assertThat(paragraph("release-unmeasured")).isEqualTo("We hold no measurement of 24.9.15. The newest "
                + "measured release of 24.9 is 24.9.26, a patch of the line you are already on, and the one to "
                + "update to.");
        assertThat(link("release-go").getHref()).isEqualTo("findings/release/24.9/24.9.26");
        List<RouterLink> others = $(RouterLink.class).withClassName("release-other").all();
        assertThat(others).hasSize(12);
        assertThat(others.getFirst().getText()).isEqualTo("24.9.25");
        assertThat(others.getFirst().getHref()).isEqualTo("findings/release/24.9/24.9.25");
        assertThat(paragraph("release-others")).startsWith("Other measured releases of 24.9: 24.9.25 24.9.24")
                .endsWith("and 13 more");
    }

    @Test
    void a_version_under_another_line_is_answered_under_its_own_and_the_address_corrected() {
        navigate("findings/release/24.4/24.9.21", ReleaseView.class);

        assertThat(location()).isEqualTo("findings/release/24.9/24.9.21");
        assertThat($(Div.class).withClassName("release-card").single().getElement().getAttribute("data-version"))
                .isEqualTo("24.9.21");
    }

    @Test
    void the_unmeasured_answers_for_a_release_ahead_a_line_not_followed_and_a_major_not_followed() {
        navigate("findings/release/24.9/24.9.99", ReleaseView.class);
        assertThat(paragraph("release-unmeasured")).isEqualTo(
                "We hold no measurement of 24.9.99. The newest measured release of 24.9 is 24.9.26, which is older "
                        + "than it.");

        navigate("findings/release/24.8/24.8.3", ReleaseView.class);
        assertThat(paragraph("release-unmeasured")).isEqualTo("We hold no measurement of 24.8.3, and 24.8 is not a "
                + "line we follow. The line we follow on Vaadin 24 is 24.10, whose newest measured release is "
                + "24.10.14.");

        navigate("findings/release/22.0/22.0.1", ReleaseView.class);
        assertThat(paragraph("release-unmeasured")).startsWith("We hold no measurement of 22.0.1, and its line is "
                + "not one we follow.");
        assertThat($(RouterLink.class).withClassName("release-go").exists()).isFalse();
    }

    @Test
    void a_listed_release_with_no_published_record_says_so_and_keeps_its_files() {
        navigate("findings/release/24.9/24.9.26", ReleaseView.class);

        assertThat(paragraph("release-unpublished")).isEqualTo("24.9.26 is a measured release of 24.9, and its "
                + "record is not published, so there is nothing more to read here.");
        assertThat($(Anchor.class).withClassName("release-file").all()).hasSize(6);
    }

    @Test
    void the_lookup_opens_another_release_or_searches_the_explorer() {
        navigate("findings/release/24.9/24.9.21", ReleaseView.class);

        test(lookup()).setValue("25.3.5");
        assertThat(location()).isEqualTo("findings/release/25.3/25.3.5");

        test(lookup()).setValue("jackson");
        assertThat(location()).isEqualTo("findings?q=jackson");
    }

    @Test
    @WithMockUser(username = "guest", roles = {})
    void a_reader_without_the_role_is_refused() {
        navigate("findings/release/24.9/24.9.21", NotFoundView.class);

        assertThat($(Div.class).withClassName("release-card").exists()).isFalse();
    }

    private String paragraph(String className) {
        return $(Paragraph.class).withClassName(className).single().getElement().getTextRecursively();
    }

    private RouterLink link(String className) {
        return $(RouterLink.class).withClassName(className).single();
    }

    private TextField lookup() {
        return $(TextField.class).withClassName("release-lookup").single();
    }

    @SuppressWarnings("unchecked")
    private Grid<Finding> grid(String key) {
        Details group = $(Details.class).withClassName("explorer-group").all().stream()
                .filter(details -> key.equals(details.getElement().getAttribute("data-group"))).findFirst()
                .orElseThrow();
        return (Grid<Finding>) group.getContent().filter(Grid.class::isInstance).findFirst().orElseThrow();
    }
}

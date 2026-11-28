package com.vaadin.securitystatus.ui.team.explorer;

import static com.vaadin.securitystatus.ui.team.explorer.ExplorerBrowserlessTest.location;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.securitystatus.security.Roles;
import com.vaadin.securitystatus.ui.customer.HomeView;

/**
 * REL-01 and REL-02: a measured release read in full, card, advice,
 * downloads and lookup, from the committed record of 24.9.21; and a release
 * nobody measured answered in place, with the line's measured releases
 * listed.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@WithMockUser(username = "team", roles = Roles.SECURITY)
class ReleaseViewBrowserlessTest extends SpringBrowserlessTest {

    @Autowired
    ObjectProvider<ProductSelection> selections;

    // REL-01
    @Test
    void a_measured_release_shows_its_card_and_advice_from_the_fixture() {
        navigate("findings/release/24.9/24.9.21", ReleaseView.class);

        // Rule 6.1.7: the heading names the version, and the document title follows, in the prototype's words.
        assertThat($(H1.class).single().getText()).isEqualTo("Vaadin 24.9.21 security status");
        assertThat(UI.getCurrent().getInternals().getTitle()).isEqualTo("Vaadin 24.9.21 security status");
        Details card = $(Details.class).withClassName("release-card").single();
        assertThat(card.getElement().getAttribute("data-version")).isEqualTo("24.9.21");
        assertThat(card.isOpened()).isTrue();
        // The badges and the groups read the same halves, D-205, D-216.
        assertThat($(Badge.class).withClassName("explorer-badge").all())
                .extracting(badge -> badge.getElement().getTextRecursively())
                .containsExactly("Vaadin: Vulnerable", "Spring Boot: Vulnerable");
        assertThat(paragraph("explorer-detail-summary")).isEqualTo("117 findings: 1 to act on, 11 not assessed yet, 70 from Spring Boot, 1 only in the build, 34 answered.");
        assertThat(paragraph("release-advice-update")).isEqualTo("You are not on the newest of this series. Updating to 24.9.26 removes 18 of these: "
                + "CVE-2026-102276, CVE-2026-102277, CVE-2026-102278, CVE-2026-102554, CVE-2026-109344, "
                + "CVE-2026-109345 and 12 more. It is a patch of the line you are already on. Read 24.9.26");
        assertThat(paragraph("release-advice-spring")).isEqualTo("70 findings on this release come with Spring Boot rather than with Vaadin. Measured "
                + "against 3.5.15, which is also what this release declares. There is somewhere to upgrade to: Spring "
                + "Boot 3.5.16, the newest patch of that line. That is where fixes for those arrive, and no Vaadin "
                + "release moves them. See what Spring Boot brings.");
        assertThat(link("release-spring-link").getHref()).isEqualTo("findings/spring");
        // Rule 6.3.14: 24.9.21 has no scanner files, only its record and its CSV.
        assertThat($(Anchor.class).withClassName("release-file").all()).extracting(Anchor::getText)
                .containsExactly("JSON", "CSV");
        List<Details> groups = $(Details.class).withClassName("explorer-group").all();
        assertThat(groups).extracting(details -> details.getElement().getAttribute("data-group"))
                .containsExactly("RISK", "UNJUDGED", "ANSWERED", "SPRING_RISK", "SPRING_ANSWERED", "BUILD");
        // Every group shut on arrival, as the prototype's release page has them.
        assertThat(groups).noneMatch(Details::isOpened);
        assertThat(footer()).isEqualTo("Scanned 2026-11-15. Measured against an application shaped like the "
                + "starter of its own major.");
        // The left panel: the filter holding the version, and every product of the catalogue.
        assertThat(lookup().getValue()).isEqualTo("24.9.21");
        assertThat($(NativeButton.class).withClassName("explorer-chip").all()).hasSize(15);
        // Rule 6.1.6: one way back, above the content.
        assertThat(link("release-back").getHref()).isEqualTo("findings");
    }

    @Test
    void a_product_turned_off_on_the_release_narrows_it_and_the_explorer_shares_the_choice() {
        navigate("findings/release/24.9/24.9.21", ReleaseView.class);
        // 24.9.21 was measured both with and without Spring, so its starter is a control; vaadin never is.
        assertThat(chip("vaadin-spring-boot-starter").isEnabled()).isTrue();
        assertThat(chip("vaadin").isEnabled()).isFalse();
        assertThat(chip("control-center-starter").getElement().getAttribute("aria-checked")).isEqualTo("false");
        String before = paragraph("explorer-detail-summary");
        assertThat(hints()).anyMatch(hint -> hint.startsWith("18 findings on this release arrive only through products "
                + "you have switched off"));

        // Turning the discontinued product on lists what only it brings, on this page and on the explorer.
        test(chip("control-center-starter")).click();

        assertThat(chip("control-center-starter").getElement().getAttribute("aria-checked")).isEqualTo("true");
        assertThat(paragraph("explorer-detail-summary")).isNotEqualTo(before);
        assertThat(hints()).noneMatch(hint -> hint.contains("only through products you have switched off"));
        assertThat(selection().off().peek()).doesNotContain("control-center-starter");
        test(chip("control-center-starter")).click();
        assertThat(selection().off().peek()).contains("control-center-starter");
        assertThat(paragraph("explorer-detail-summary")).isEqualTo(before);
    }

    @Test
    void the_newest_release_says_so_and_offers_its_six_files() {
        navigate("findings/release/25.3/25.3.5", ReleaseView.class);

        assertThat(paragraph("release-advice-update")).isEqualTo("This is the newest measured release of 25.3.");
        assertThat($(Anchor.class).withClassName("release-file").all()).extracting(Anchor::getText).containsExactly(
                "JSON", "CSV", "VEX", "dependency-check", "osv-scanner", "bomber");
    }

    @Test
    void an_opened_finding_reads_the_release_record() {
        navigate("findings/release/24.9/24.9.21", ReleaseView.class);
        listOf("CVE-2026-45292").rowOf("CVE-2026-45292").orElseThrow().setOpened(true);

        assertThat($(Div.class).withClassName("explorer-finding").single().getElement().getTextRecursively())
                .contains("OpenTelemetry Java SDK has Unbounded Memory Allocation",
                        "What Vaadin says", "Read the advisory");
    }

    // REL-02
    @Test
    void an_unmeasured_version_is_answered_in_place_with_the_measured_releases() {
        navigate("findings/release/24.9/24.9.15", ReleaseView.class);

        assertThat(location()).isEqualTo("findings/release/24.9/24.9.15");
        assertThat($(Details.class).withClassName("release-card").exists()).isFalse();
        assertThat(paragraph("release-unmeasured")).isEqualTo("We hold no measurement of 24.9.15. The newest "
                + "maintenance release of 24.9 is 24.9.26, and that is the one to update to: it is a patch of the "
                + "line you are already on. It is the release we measure and answer for.");
        assertThat(link("release-go").getHref()).isEqualTo("findings/release/24.9/24.9.26");
        List<RouterLink> others = $(RouterLink.class).withClassName("release-other").all();
        assertThat(others).hasSize(12);
        assertThat(others.getFirst().getText()).isEqualTo("24.9.25");
        assertThat(others.getFirst().getHref()).isEqualTo("findings/release/24.9/24.9.25");
        assertThat(paragraph("release-others")).startsWith("Other releases of 24.9 we have measured: 24.9.25, 24.9.24")
                .endsWith("and 13 more");
    }

    @Test
    void a_version_under_another_line_is_answered_under_its_own_and_the_address_corrected() {
        navigate("findings/release/24.4/24.9.21", ReleaseView.class);

        assertThat(location()).isEqualTo("findings/release/24.9/24.9.21");
        assertThat($(Details.class).withClassName("release-card").single().getElement().getAttribute("data-version"))
                .isEqualTo("24.9.21");
    }

    @Test
    void the_unmeasured_answers_for_a_release_ahead_a_line_not_followed_and_a_major_not_followed() {
        navigate("findings/release/24.9/24.9.99", ReleaseView.class);
        assertThat(paragraph("release-unmeasured")).isEqualTo(
                "We hold no measurement of 24.9.99. The newest measured release of 24.9 is 24.9.26, which is older "
                        + "than it.");

        navigate("findings/release/24.8/24.8.3", ReleaseView.class);
        assertThat(paragraph("release-unmeasured")).isEqualTo("We hold no measurement of 24.8.3. 24.8 is out of "
                + "maintenance. The line we follow on Vaadin 24 is 24.10, whose newest release is 24.10.14, and that "
                + "is the one to update to. It is the release we measure and answer for.");

        navigate("findings/release/22.0/22.0.1", ReleaseView.class);
        assertThat(paragraph("release-unmeasured")).isEqualTo("We hold no measurement of 22.0.1, and we do not "
                + "follow that series. Vaadin measures the maintained lines, and the status page lists them: moving "
                + "to one of those is what gets you an answer.");
        assertThat(link("release-go").getText()).isEqualTo("See the maintained series");
        assertThat(link("release-go").getHref()).isEqualTo("findings");
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
        navigate("findings/release/24.9/24.9.21", HomeView.class);

        assertThat($(Details.class).withClassName("release-card").exists()).isFalse();
    }

    private String paragraph(String className) {
        return $(Paragraph.class).withClassName(className).single().getElement().getTextRecursively();
    }

    private List<String> hints() {
        return $(Paragraph.class).withClassName("explorer-hint").all().stream()
                .map(hint -> hint.getElement().getTextRecursively()).toList();
    }

    private String footer() {
        return paragraph("release-footer");
    }

    private NativeButton chip(String product) {
        return $(NativeButton.class).withClassName("explorer-chip").all().stream()
                .filter(chip -> product.equals(chip.getElement().getAttribute("data-product"))).findFirst()
                .orElseThrow();
    }

    private ProductSelection selection() {
        return selections.getObject();
    }

    private RouterLink link(String className) {
        return $(RouterLink.class).withClassName(className).single();
    }

    private TextField lookup() {
        return $(TextField.class).withClassName("release-lookup").single();
    }

    private FindingList listOf(String cve) {
        return $(Details.class).withClassName("explorer-group").all().stream()
                .flatMap(details -> details.getContent().filter(FindingList.class::isInstance))
                .map(FindingList.class::cast).filter(list -> list.rowOf(cve).isPresent()).findFirst().orElseThrow();
    }
}

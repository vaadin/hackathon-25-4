package com.vaadin.securitystatus.explorer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.TableRow;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.securitystatus.base.security.Roles;
import com.vaadin.securitystatus.base.ui.NotFoundView;

/**
 * SPR-01: what Spring Boot brings, one group per Spring Boot release the
 * measured lines sit on, each with its supply status read from
 * {@code spring_boot_supply} and a record link per advisory.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@WithMockUser(username = "team", roles = Roles.SECURITY)
class SpringViewBrowserlessTest extends SpringBrowserlessTest {

    // SPR-01
    @Test
    void one_group_per_spring_boot_release_newest_first_with_its_supply_wording() {
        navigate(SpringView.class);

        assertThat($(H1.class).single().getText()).isEqualTo("What Spring Boot brings");
        assertThat(UI.getCurrent().getInternals().getTitle()).isEqualTo("What Spring Boot brings");
        List<Details> boots = $(Details.class).withClassName("spring-boot").all();
        assertThat(boots).extracting(details -> details.getElement().getAttribute("data-boot"))
                .containsExactly("4.2.0-RC2", "4.1.2", "3.5.16", "3.4.13", "3.2.12", "2.7.18");
        // Each release is a row shut on arrival, the prototype's, D-219.
        assertThat(boots).noneMatch(Details::isOpened);

        Details open = boot("4.1.2");
        assertThat(badges(open)).containsExactly("Not affected");
        // Silent on the row while the line still gets free patches; said once inside.
        assertThat(span(open, "spring-supply-note")).isEmpty();
        assertThat(text(open, "spring-supply")).isEqualTo("Supply open: this line still gets free patches.");
        assertThat(supplyRows(open)).containsExactly("Open 4.1.3 measured on Vaadin 25.3.5: closes 0 of these 1");
        assertThat(text(open, "spring-advice-patch")).isEqualTo("Taking the open release above is the first thing "
                + "to try, because that is where fixes for these arrive.");
        assertThat(text(open, "spring-advice-minor")).startsWith("Spring Boot 4.2.0 is a later minor of the same "
                + "major, which moves more than a patch does");

        Details commercial = boot("3.5.16");
        assertThat(span(commercial, "spring-boot-lines")).contains("Vaadin 24.10, 24.9");
        assertThat(badges(commercial)).containsExactly("Vulnerable");
        assertThat(span(commercial, "spring-boot-summary"))
                .contains("41 findings arrive with this release, 24 of them a real risk");
        assertThat(span(commercial, "spring-supply-note")).contains("free patches ended \u00b7 3.5.18 is commercial");
        assertThat(supplyRows(commercial)).containsExactly("Open 3.5.16 what you have",
                "Commercial 3.5.18 Spring Enterprise, not measured");
        // spring_boot_latest holds an empty entry for 3.5.16: nothing newer is published openly.
        assertThat(text(commercial, "spring-advice-none")).isEqualTo("We know of nothing newer to take within this "
                + "major that anybody publishes, so until the next release a version property in the application "
                + "pom moves any one of these.");
    }

    // The edge case: a release whose supply ended says so and still lists its findings
    @Test
    void a_release_whose_supply_ended_says_so_and_still_lists_its_findings() {
        navigate(SpringView.class);

        Details ended = boot("3.2.12");
        assertThat(badges(ended)).containsExactly("Vulnerable");
        assertThat(span(ended, "spring-supply-note")).contains("free and commercial patches have both ended");
        assertThat(text(ended, "spring-supply"))
                .isEqualTo("Supply ended: free and commercial patches for this line have both ended.");
        assertThat(supplyRows(ended)).containsExactly("Open 3.2.12 what you have",
                "Commercial 3.2.19 the last there was, and nobody sells it now");
        List<Details> groups = $(Details.class, ended).withClassName("spring-group").all();
        assertThat(groups).extracting(Details::getSummaryText).containsExactly("60 we have judged a real risk",
                "10 we have answered, nothing to do");
        // The prototype's folds: the risks open, the answered shut.
        assertThat(groups).extracting(Details::isOpened).containsExactly(true, false);
        assertThat(records(ended)).hasSize(70);
        assertThat(records(ended).getFirst().getText()).isEqualTo("CVE-2025-24813");
        assertThat($(Span.class, ended).withClassName("spring-finding-verdict").all().getFirst().getText())
                .isEqualTo("we have judged this a real risk");
    }

    // AC2: the record links match the advisories, one per finding, to NVD
    @Test
    void every_advisory_links_to_its_own_nvd_record_once() {
        navigate(SpringView.class);

        for (Details boot : $(Details.class).withClassName("spring-boot").all()) {
            List<Anchor> records = records(boot);
            assertThat(span(boot, "spring-boot-summary")).first().asString().startsWith(records.size() + " finding");
            assertThat(records).extracting(Anchor::getText).doesNotHaveDuplicates();
            assertThat(records).allSatisfy(record -> assertThat(record.getHref())
                    .isEqualTo("https://nvd.nist.gov/vuln/detail/" + record.getText()));
        }
    }

    @Test
    void lines_that_resolve_different_sets_on_one_release_are_named() {
        navigate(SpringView.class);

        assertThat($(Paragraph.class).withClassName("spring-caveat").single().getText())
                .startsWith("On Spring Boot 2.7.18 the Vaadin series listed do not all resolve the same set");
        assertThat($(RouterLink.class).withClassName("release-back").single().getHref()).isEqualTo("findings");
    }

    @Test
    @WithMockUser(username = "guest", roles = {})
    void a_reader_without_the_role_is_refused() {
        navigate("findings/spring", NotFoundView.class);

        assertThat($(Details.class).withClassName("spring-boot").exists()).isFalse();
    }

    private Details boot(String release) {
        return $(Details.class).withClassName("spring-boot").all().stream()
                .filter(details -> release.equals(details.getElement().getAttribute("data-boot"))).findFirst()
                .orElseThrow();
    }

    private List<String> badges(Details boot) {
        return $(Badge.class, boot).all().stream().map(badge -> badge.getElement().getTextRecursively()).toList();
    }

    private List<String> span(Details boot, String className) {
        return $(Span.class, boot).withClassName(className).all().stream().map(Span::getText).toList();
    }

    private String text(Component in, String className) {
        return $(Paragraph.class, in).withClassName(className).single().getElement().getTextRecursively();
    }

    private List<String> supplyRows(Details boot) {
        return $(TableRow.class, boot).withClassName("spring-supply-row").all().stream()
                .map(row -> String.join(" ", row.getChildren()
                        .map(cell -> cell.getElement().getTextRecursively()).toList()))
                .toList();
    }

    private List<Anchor> records(Details boot) {
        return $(Anchor.class, boot).withClassName("spring-finding-record").all();
    }
}

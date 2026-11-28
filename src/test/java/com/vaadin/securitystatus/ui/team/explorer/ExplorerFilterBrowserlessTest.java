package com.vaadin.securitystatus.ui.team.explorer;

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
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.checkbox.Switch;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.securitystatus.data.FixturePages;
import com.vaadin.securitystatus.security.Roles;
import com.vaadin.securitystatus.team.explorer.ExplorerAnswer;
import com.vaadin.securitystatus.team.explorer.ExplorerRules;

/**
 * EXP-03, EXP-04 and EXP-08: the filter is the prototype's filter, D-206 and
 * D-207. A finding of ours is the reader's when any product it arrives through
 * is on, a finding with no product recorded never hides, what the reader's
 * Spring Boot brings is not narrowed at all, a chip counts the different
 * advisories that arrive through it as if it were on, and the Spring Boot
 * starter switches the page to its measurement without Spring.
 *
 * <p>
 * The fixture: on 24.7.19, CVE-2026-45292 is ours and arrives through
 * vaadin-testbench and observability-kit-starter; on 25.3.5, CVE-2022-31691 is
 * Spring Boot's and twelve of its fifteen findings are measured without
 * Spring.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@WithMockUser(username = "team", roles = Roles.SECURITY)
class ExplorerFilterBrowserlessTest extends SpringBrowserlessTest {

    private static final String TWO_PRODUCTS = "CVE-2026-45292";
    private static final String SPRING_BOOTS = "CVE-2022-31691";

    @Autowired
    FixturePages fixtures;

    @AfterEach
    void restore() {
        fixtures.restore();
    }

    // EXP-03
    @Test
    void a_finding_through_two_products_needs_both_off_to_hide() {
        navigate("findings?open=24.7&off=vaadin-testbench,control-center-starter", ExplorerView.class);
        assertThat(shown()).contains(TWO_PRODUCTS);
        assertThat(through(TWO_PRODUCTS)).isEqualTo("through 1 of 2 products");
        assertThat(badgeTexts("24.7")).first().isEqualTo("Vaadin: Vulnerable");

        navigate("findings?open=24.7&off=vaadin-testbench,observability-kit-starter,control-center-starter",
                ExplorerView.class);

        assertThat(shown()).doesNotContain(TWO_PRODUCTS);
        assertThat($(Paragraph.class).withClassName("explorer-hint").all()).extracting(Paragraph::getText)
                .anyMatch(text -> text.endsWith(
                        "only through products you have switched off, so they are not listed here. They still count towards the state above, which answers for the release rather than for one application."));
        // The colour answers for the reader, over the findings the list shows.
        assertThat(badgeTexts("24.7")).first().isEqualTo("Vaadin: Not affected");
    }

    // AC1: the no product finding never hides, and Spring Boot's half is not narrowed
    @Test
    void a_finding_with_no_product_recorded_never_hides() {
        navigate("findings?open=25.3", ExplorerView.class);
        assertThat(shown()).contains(SPRING_BOOTS, "CVE-2025-15056");

        var everything = new ArrayList<String>();
        var front = FixturePages.offline().explorerFront();
        ExplorerAnswer.catalogue(ExplorerRules.products(front), front).stream()
                .filter(ExplorerRules.Product::switchable).filter(product -> !product.switchesShape())
                .forEach(product -> everything.add(product.name()));
        navigate("findings?open=25.3&off=" + String.join(",", everything), ExplorerView.class);

        // The bundled finding has no product recorded; Spring Boot's is not the reader's to narrow.
        assertThat(shown()).contains(SPRING_BOOTS, "CVE-2025-15056");
        // The fixed product, vaadin, is drawn ticked and cannot be clicked.
        Span vaadin = $(Span.class).withClassName("explorer-chip").all().stream()
                .filter(chip -> "vaadin".equals(chip.getElement().getAttribute("data-product"))).findFirst()
                .orElseThrow();
        assertThat(vaadin.getElement().getAttribute("aria-checked")).isEqualTo("true");
        assertThat(vaadin.getElement().getAttribute("aria-disabled")).isEqualTo("true");
    }

    // EXP-04
    @Test
    void a_chip_counts_its_advisories_as_if_it_were_on() {
        navigate(ExplorerView.class);
        int webpush = count("flow-webpush");
        int testbench = count("vaadin-testbench");
        assertThat(webpush).isPositive();

        navigate("findings?off=flow-webpush,control-center-starter", ExplorerView.class);

        // Turning it off moves the rows below it, not its own number.
        assertThat(count("flow-webpush")).isEqualTo(webpush);
        assertThat(count("vaadin-testbench")).isEqualTo(testbench);

        // A search narrows the counts to what it names: one advisory, through TestBench.
        navigate("findings?q=" + TWO_PRODUCTS, ExplorerView.class);
        assertThat(count("vaadin-testbench")).isEqualTo(1);
        assertThat(count("flow-webpush")).isZero();
    }

    // EXP-08
    @Test
    void the_spring_starter_off_renders_the_measurement_without_spring() {
        navigate("findings?open=25.3", ExplorerView.class);
        assertThat(shown()).hasSize(15).contains(SPRING_BOOTS);

        // The starter's chip is the control, D-266: switched off, the address says so.
        navigate("findings?open=25.3&off=vaadin-spring-boot-starter,control-center-starter", ExplorerView.class);

        assertThat(ExplorerBrowserlessTest.query().getParameters().get("off"))
                .containsExactly("vaadin-spring-boot-starter,control-center-starter");
        // The fixture's without_spring list for 25.3: twelve findings, and Spring Boot's are not among them.
        assertThat(shown()).hasSize(12).doesNotContain(SPRING_BOOTS);
        // The shape without Spring has no Spring Boot half, so no second badge.
        assertThat(badgeTexts("25.3")).containsExactly("Vaadin: Not affected");
        assertThat($(Switch.class).withClassName("explorer-spring-switch").exists()).isFalse();
        // The products that need Spring cannot be had on that shape: drawn off, not clickable.
        Span hilla = $(Span.class).withClassName("explorer-chip").all().stream()
                .filter(chip -> "hilla-spring-boot-starter".equals(chip.getElement().getAttribute("data-product")))
                .findFirst().orElseThrow();
        assertThat(hilla.getElement().getClassList()).contains("impossible");
    }

    // The select all box reads the chips, and its two targets are the prototype's, D-206
    @Test
    void the_select_all_box_turns_everything_off_and_then_back_to_the_default() {
        navigate(ExplorerView.class);
        assertThat(allBox().getElement().getAttribute("data-state")).isEqualTo("ticked");
        assertThat(allBox().getElement().getAttribute("aria-label")).isEqualTo("Count only what comes with Vaadin");

        test(allBox()).click();

        assertThat(ExplorerBrowserlessTest.query().getParameters().get("off").get(0)).contains(
                "vaadin-spring-boot-starter", "flow-webpush", "vaadin-spreadsheet-flow", "control-center-starter");
        assertThat(allBox().getElement().getAttribute("data-state")).isEqualTo("none");
        assertThat(allBox().getElement().getAttribute("aria-label")).isEqualTo("Count every product");

        test(allBox()).click();
        assertThat(ExplorerBrowserlessTest.location()).isEqualTo("findings");
        assertThat(allBox().getElement().getAttribute("data-state")).isEqualTo("ticked");
    }

    // Helpers

    private List<String> shown() {
        var ids = new ArrayList<String>();
        for (Details group : $(Details.class).withClassName("explorer-group").all()) {
            group.getContent().filter(FindingList.class::isInstance).map(FindingList.class::cast)
                    .forEach(list -> list.findings().forEach(finding -> ids.add(finding.cve())));
        }
        return ids;
    }

    private String through(String cve) {
        for (Details group : $(Details.class).withClassName("explorer-group").all()) {
            for (var list : group.getContent().filter(FindingList.class::isInstance).map(FindingList.class::cast)
                    .toList()) {
                var row = list.rowOf(cve);
                if (row.isPresent()) {
                    return row.get().getSummary().getChildren().filter(Span.class::isInstance).map(Span.class::cast)
                            .filter(span -> span.hasClassName("explorer-through")).findFirst().orElseThrow()
                            .getText();
                }
            }
        }
        throw new AssertionError(cve + " is not shown");
    }

    private int count(String product) {
        var chip = $(Component.class).withClassName("explorer-chip").all().stream()
                .filter(candidate -> product.equals(candidate.getElement().getAttribute("data-product")))
                .findFirst().orElseThrow();
        return Integer.parseInt(chip.getChildren().filter(Span.class::isInstance).map(Span.class::cast)
                .filter(span -> span.hasClassName("chip-count")).findFirst().orElseThrow().getText());
    }

    private List<String> badgeTexts(String line) {
        var texts = new ArrayList<String>();
        $(Details.class).withClassName("explorer-row").all().stream()
                .filter(row -> line.equals(row.getElement().getAttribute("data-line"))).findFirst().orElseThrow()
                .getSummary().getChildren().filter(Badge.class::isInstance)
                .forEach(badge -> texts.add(badge.getElement().getTextRecursively()));
        return texts;
    }

    private RouterLink allBox() {
        return $(RouterLink.class).withClassName("explorer-all").single();
    }
}

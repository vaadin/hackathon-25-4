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
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.checkbox.Switch;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.securitystatus.base.security.Roles;
import com.vaadin.securitystatus.data.FixturePages;
import com.vaadin.securitystatus.data.model.ExplorerFront.Finding;

/**
 * EXP-03, EXP-04 and EXP-08: the filter is the source filter. A finding is
 * hidden only when every product it arrives through is off, a finding with no
 * product never hides, the counts are marginal, and the Spring Boot starter
 * switches the line to its measurement without Spring.
 *
 * <p>
 * The line is 25.3.5 of the fixture: CVE-2026-53914 arrives through
 * flow-webpush and hilla-spring-boot-starter, CVE-2022-31691 through no
 * recorded product, and twelve of its fifteen findings are measured without
 * Spring.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@WithMockUser(username = "team", roles = Roles.SECURITY)
class ExplorerFilterBrowserlessTest extends SpringBrowserlessTest {

    private static final String TWO_PRODUCTS = "CVE-2026-53914";
    private static final String NO_PRODUCT = "CVE-2022-31691";

    @Autowired
    FixturePages fixtures;

    @AfterEach
    void restore() {
        fixtures.restore();
    }

    // EXP-03
    @Test
    void a_finding_through_two_products_needs_both_off_to_hide() {
        navigate("findings?off=flow-webpush", ExplorerView.class);
        open("25.3");
        assertThat(shown()).contains(TWO_PRODUCTS);
        assertThat(through(TWO_PRODUCTS)).isEqualTo("through 1 of 2 products");

        navigate("findings?off=flow-webpush,hilla-spring-boot-starter", ExplorerView.class);

        assertThat(shown()).doesNotContain(TWO_PRODUCTS).hasSize(14);
        assertThat($(Paragraph.class).withClassName("explorer-hidden").single().getText())
                .isEqualTo("1 findings are hidden because every product they arrive through is off.");
        assertThat($(Paragraph.class).withClassName("explorer-detail-summary").single().getText()).startsWith(
                "14 of 15 findings shown:");
    }

    // AC1: the no product finding never hides
    @Test
    void a_finding_with_no_product_recorded_never_hides() {
        navigate(ExplorerView.class);
        open("25.3");
        // Turn every product a reader can turn off, off, through the chips' own addresses.
        var everything = new ArrayList<String>();
        ExplorerRules.products(FixturePages.offline().explorerFront()).stream().filter(ExplorerRules.Product::switchable)
                .filter(product -> !product.switchesShape()).forEach(product -> everything.add(product.name()));
        navigate("findings?off=" + String.join(",", everything), ExplorerView.class);

        assertThat(shown()).contains(NO_PRODUCT);
        // The fixed product, vaadin, has no chip and cannot be turned off.
        assertThat($(RouterLink.class).withClassName("explorer-chip").all())
                .noneMatch(chip -> "vaadin".equals(chip.getElement().getAttribute("data-product")));
    }

    // EXP-04
    @Test
    void a_chip_count_moves_when_another_chip_turns_off() {
        navigate(ExplorerView.class);
        open("25.3");
        // Alone, hilla-spring-boot-starter hides nothing on 25.3: CVE-2026-53914 comes through flow-webpush too.
        assertThat(chipNamed("hilla-spring-boot-starter")).isEmpty();
        assertThat(chipNamed("flow-webpush")).isEmpty();
        assertThat($(Paragraph.class).withClassName("explorer-silent").single().getText())
                .endsWith("only findings you also have through the ones shown.");

        navigate("findings?off=flow-webpush", ExplorerView.class);

        // Now turning it off would hide that finding: its count is the marginal one, 1.
        assertThat(count("hilla-spring-boot-starter")).isEqualTo(1);
        // And flow-webpush, off, says what turning it back on would show: nothing, as hilla still carries it.
        assertThat(count("flow-webpush")).isZero();
    }

    // EXP-08
    @Test
    void the_spring_starter_off_renders_the_measurement_without_spring() {
        navigate(ExplorerView.class);
        open("25.3");
        assertThat(shown()).hasSize(15).contains(NO_PRODUCT);

        test($(Switch.class).withClassName("explorer-spring-switch").single()).switchOn();

        assertThat(ExplorerBrowserlessTest.query().getParameters().get("off"))
                .containsExactly("vaadin-spring-boot-starter");
        // The fixture's without_spring list for 25.3: twelve findings, and the Spring Boot one is not among them.
        assertThat(shown()).hasSize(12).doesNotContain(NO_PRODUCT, TWO_PRODUCTS);
        assertThat(badgeTexts("25.3")).containsExactly("Vaadin: not affected", "Measured without Spring Boot");
        assertThat($(Switch.class).withClassName("explorer-spring-switch").single().getValue()).isTrue();
    }

    // Edge case: a line measured without Spring only explains itself
    @Test
    void a_line_measured_without_spring_only_says_so() {
        navigate(ExplorerView.class);
        open("8.32");

        Switch toggle = $(Switch.class).withClassName("explorer-spring-switch").single();
        assertThat(toggle.isEnabled()).isFalse();
        assertThat($(Paragraph.class).withClassName("explorer-spring-only").single().getText())
                .isEqualTo("This line was measured without Spring only.");
    }

    // The tri state control reads the chips
    @Test
    void the_all_control_is_checked_mixed_or_unchecked_and_its_second_state_turns_the_optional_ones_off() {
        navigate(ExplorerView.class);
        Checkbox all = all();
        assertThat(all.getValue()).isTrue();
        assertThat(all.isIndeterminate()).isFalse();

        test(all).click();

        // The profile's optional products are off, and the control is mixed.
        assertThat(ExplorerBrowserlessTest.query().getParameters().get("off").get(0)).contains(
                "vaadin-spreadsheet-flow", "kubernetes-kit-starter", "hilla-spring-boot-starter")
                .doesNotContain("flow-webpush");
        assertThat(all().isIndeterminate()).isTrue();

        test(all()).click();
        assertThat(ExplorerBrowserlessTest.location()).isEqualTo("findings");
        assertThat(all().getValue()).isTrue();
    }

    // Helpers

    private void open(String line) {
        test($(NativeButton.class).withClassName("explorer-row").all().stream()
                .filter(row -> line.equals(row.getElement().getAttribute("data-line"))).findFirst().orElseThrow())
                .click();
    }

    @SuppressWarnings("unchecked")
    private List<String> shown() {
        var ids = new ArrayList<String>();
        for (Details group : $(Details.class).withClassName("explorer-group").all()) {
            group.getContent().filter(Grid.class::isInstance).map(grid -> (Grid<Finding>) grid)
                    .forEach(grid -> grid.getGenericDataView().getItems().forEach(finding -> ids.add(finding.cve())));
        }
        return ids;
    }

    @SuppressWarnings("unchecked")
    private String through(String cve) {
        for (Details group : $(Details.class).withClassName("explorer-group").all()) {
            for (var component : group.getContent().filter(Grid.class::isInstance).toList()) {
                var grid = (Grid<Finding>) component;
                var found = grid.getGenericDataView().getItems().filter(finding -> finding.cve().equals(cve))
                        .findFirst();
                if (found.isPresent()) {
                    int row = grid.getGenericDataView().getItems().toList().indexOf(found.get());
                    return test(grid).getCellText(row, 2);
                }
            }
        }
        throw new AssertionError(cve + " is not shown");
    }

    private java.util.Optional<RouterLink> chipNamed(String product) {
        return $(RouterLink.class).withClassName("explorer-chip").all().stream()
                .filter(link -> product.equals(link.getElement().getAttribute("data-product"))).findFirst();
    }

    private int count(String product) {
        RouterLink chip = chipNamed(product).orElseThrow();
        return Integer.parseInt(chip.getChildren().filter(Span.class::isInstance).map(Span.class::cast)
                .filter(span -> span.hasClassName("chip-count")).findFirst().orElseThrow().getText());
    }

    private List<String> badgeTexts(String line) {
        var texts = new ArrayList<String>();
        $(NativeButton.class).withClassName("explorer-row").all().stream()
                .filter(row -> line.equals(row.getElement().getAttribute("data-line"))).findFirst().orElseThrow()
                .getChildren().forEach(child -> child.getChildren().filter(Badge.class::isInstance)
                        .forEach(badge -> texts.add(badge.getElement().getTextRecursively())));
        return texts;
    }

    private Checkbox all() {
        return $(Checkbox.class).withClassName("explorer-all").single();
    }
}

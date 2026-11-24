package com.vaadin.securitystatus.ui.team.workbench;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.securitystatus.security.Roles;
import com.vaadin.securitystatus.team.workbench.VerdictDrafts;

/**
 * AST-08 and AC5: a pasted GHSA paragraph fills only the fields it supports,
 * each marked as filled, and nothing is saved until a person presses Save.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@WithMockUser(username = "team", roles = Roles.SECURITY)
class WorkbenchAiBrowserlessTest extends SpringBrowserlessTest {

    static final String GHSA = """
            GHSA-h46c-h94j-95f3: jackson-core: ReDoS through quadratic backtracking in number parsing.
            Package: com.fasterxml.jackson.core:jackson-core (Maven). Affected: 2.21.0 to 2.21.6. Patched: 2.21.7.
            A crafted number string makes NumberInput.looksLikeValidNumber() backtrack quadratically, so parsing \
            untrusted JSON can use excessive CPU. Tracked as CVE-2026-89407.""";

    @Autowired
    VerdictDrafts drafts;

    @Test
    void the_paste_fills_only_the_supported_fields_with_markers_and_saves_nothing() {
        WorkbenchView view = navigate(WorkbenchView.class);
        int savedBefore = drafts.list().size();
        Map<String, HasValue<?, ?>> fields = view.form().fields();
        ((com.vaadin.flow.component.textfield.TextField) fields.get("assessedBy")).setValue("someone");
        String verdictIdBefore = (String) fields.get("verdictId").getValue();

        Button fill = $(Button.class).withClassName("verdict-fill-run").single();
        assertThat(fill.isEnabled()).as("nothing pasted yet").isFalse();
        test($(TextArea.class).withClassName("verdict-fill-paste").single()).setValue(GHSA);
        test(fill).click();
        // The end of a turn, markers included, is a UI.access task that the next component query runs.
        assertThat($(Paragraph.class).withClassName("verdict-fill-answer").single().getText())
                .isEqualTo("Filled the package, the advisories and the impact from the pasted advisory. "
                        + "Everything else is left for you.");

        assertThat(fields.get("purl").getValue()).isEqualTo("pkg:maven/com.fasterxml.jackson.core/jackson-core");
        assertThat(fields.get("advisories").getValue()).isEqualTo("CVE-2026-89407");
        assertThat((String) fields.get("impact").getValue()).startsWith("A crafted number string");
        // What the text does not state stays empty, and the person's own fields stay as they were.
        assertThat(fields.get("applicability").getValue()).isNull();
        assertThat(fields.get("workaround").getValue()).isEqualTo("");
        assertThat(fields.get("fixExpectedVersion").getValue()).isEqualTo("");
        assertThat(fields.get("assessedBy").getValue()).isEqualTo("someone");
        assertThat(fields.get("verdictId").getValue()).isEqualTo(verdictIdBefore);

        // Each written field carries the controller's marker, and only those.
        assertThat(marked(fields.get("purl"))).isTrue();
        assertThat(marked(fields.get("advisories"))).isTrue();
        assertThat(marked(fields.get("impact"))).isTrue();
        assertThat(marked(fields.get("workaround"))).isFalse();

        assertThat($(Paragraph.class).withClassName("verdict-fill-answer").single().getText())
                .isEqualTo("Filled the package, the advisories and the impact from the pasted advisory. "
                        + "Everything else is left for you.");
        // Filling is not saving.
        assertThat(drafts.list()).hasSize(savedBefore);
        assertThat(view.form().draft().getPurl()).isEqualTo("pkg:maven/com.fasterxml.jackson.core/jackson-core");
    }

    @Test
    void a_paste_that_supports_no_field_fills_nothing() {
        WorkbenchView view = navigate(WorkbenchView.class);
        Map<String, HasValue<?, ?>> fields = view.form().fields();
        Object purlBefore = fields.get("purl").getValue();

        test($(TextArea.class).withClassName("verdict-fill-paste").single()).setValue("Thanks for the update, talk soon.");
        test($(Button.class).withClassName("verdict-fill-run").single()).click();

        assertThat($(Paragraph.class).withClassName("verdict-fill-answer").single().getText())
                .isEqualTo("The text supports none of the fields, so nothing was filled.");
        assertThat(fields.get("purl").getValue()).isEqualTo(purlBefore);
        assertThat(marked(fields.get("purl"))).isFalse();
    }

    /** The marker the form AI controller puts on a field it changed, a {@code vaadin-ai-field-marker} child. */
    private static boolean marked(HasValue<?, ?> field) {
        return ((com.vaadin.flow.component.Component) field).getElement().getChildren()
                .anyMatch(child -> "vaadin-ai-field-marker".equals(child.getTag()));
    }
}

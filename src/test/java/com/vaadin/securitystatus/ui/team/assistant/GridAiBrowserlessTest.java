package com.vaadin.securitystatus.ui.team.assistant;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.securitystatus.data.SecurityDataService;
import com.vaadin.securitystatus.data.model.CasesIndex.AdvisoryCatalogEntry;
import com.vaadin.securitystatus.data.model.CasesIndex;
import com.vaadin.securitystatus.security.Roles;
import com.vaadin.securitystatus.team.assistant.AssistantTail;
import com.vaadin.securitystatus.ui.customer.AdvisoriesView;

/**
 * AST-05 and AC4: the plain language ask over the advisories list narrows the
 * same grid to the fixture's matching set through the Grid AI controller, an
 * ask outside the grid's vocabulary is refused by the query validation and
 * then in words, and an anonymous reader has no ask at all.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class GridAiBrowserlessTest extends SpringBrowserlessTest {

    @Autowired
    SecurityDataService data;

    @Autowired
    AssistantTail tail;

    @Test
    @WithMockUser(username = "team", roles = Roles.SECURITY)
    void the_slice_ask_narrows_the_grid_to_the_matching_rows() {
        var view = navigate(AdvisoriesView.class);
        assertThat(rows(view.grid())).hasSize(535);

        ask("show the critical ones still open on 24.9");
        // The end of a turn is a UI.access task, which the next component query runs.
        assertThat($(Paragraph.class).withClassName("advisories-ask-answer").single().getText()).isEqualTo(
                "The list now shows the critical advisories with no fix published on 24.9 that are a risk or still in triage.");

        // Worked out from the index without SQL: critical, a risk or in triage,
        // and no fix published on 24.9.
        CasesIndex index = data.index();
        Set<String> expected = index.advisories().values().stream()
                .filter(entry -> entry.severity().filter("critical"::equalsIgnoreCase).isPresent())
                .filter(entry -> entry.applicability().filter(List.of("risk", "in_triage")::contains).isPresent())
                .filter(entry -> !index.fixedIn().getOrDefault(entry.cve(), Map.of()).containsKey("24.9"))
                .map(AdvisoryCatalogEntry::cve).collect(Collectors.toSet());
        assertThat(expected).isNotEmpty();

        List<AdvisoryCatalogEntry> rows = rows(view.grid());
        assertThat(rows).extracting(AdvisoryCatalogEntry::cve).containsExactlyInAnyOrderElementsOf(expected);
        // Same grid, same order, worst first then by id: only narrowed.
        assertThat(rows).extracting(AdvisoryCatalogEntry::cve).isSorted();
        assertThat($(Paragraph.class).withClassName("advisories-total").single().getText())
                .isEqualTo(expected.size() + " of 535 advisories match the ask, worst first");

        test($(Button.class).withClassName("advisories-ask-all").single()).click();
        assertThat(rows(view.grid())).hasSize(535);
    }

    @Test
    @WithMockUser(username = "team", roles = Roles.SECURITY)
    void an_ask_outside_the_vocabulary_is_refused_in_words_and_narrows_nothing() {
        var view = navigate(AdvisoriesView.class);
        long before = tail.newest();

        ask("show the ones with a public exploit");

        assertThat($(Paragraph.class).withClassName("advisories-ask-answer").single().getText())
                .startsWith("The list cannot be narrowed by exploits");
        assertThat(rows(view.grid())).hasSize(535);
        assertThat($(Paragraph.class).withClassName("advisories-total").single().getText())
                .isEqualTo("535 advisories, worst first");
        assertThat(tail.entries().stream().filter(entry -> entry.number() > before))
                .extracting(AssistantTail.Entry::kind)
                .containsExactly(AssistantTail.Kind.PROMPT, AssistantTail.Kind.ANSWER);
    }

    @Nested
    class Anonymous {

        @Test
        @WithAnonymousUser
        void an_anonymous_reader_has_no_ask_and_so_no_model_call() {
            navigate(AdvisoriesView.class);

            assertThat($(MessageInput.class).all()).isEmpty();
            assertThat($(Paragraph.class).withClassName("advisories-ask-answer").all()).isEmpty();
        }
    }

    private void ask(String words) {
        test($(MessageInput.class).withClassName("advisories-ask-input").single()).send(words);
    }

    private static List<AdvisoryCatalogEntry> rows(Grid<AdvisoryCatalogEntry> grid) {
        return grid.getLazyDataView().getItems().toList();
    }
}

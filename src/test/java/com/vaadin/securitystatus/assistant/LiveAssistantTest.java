package com.vaadin.securitystatus.assistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.messages.MessageListItem;
import com.vaadin.securitystatus.base.observe.AssistantTail;
import com.vaadin.securitystatus.base.security.Roles;

/**
 * AST-07: with the {@code ai} profile and a key, the real provider wires up and
 * the canonical question lands end to end, grounded in a tool call. Evidence,
 * not proof: no assertion on the model's wording, {@code specs/06-ai.md}.
 *
 * <p>
 * Tagged {@code live-ai}, which the pom's {@code surefire.excludedGroups} keeps
 * out of every default run. Run it on purpose, with the key in the environment:
 * {@code ./mvnw test -Dtest=LiveAssistantTest -Dsurefire.excludedGroups=}. It
 * answers from the committed fixtures, so the model receives only this
 * application's prompts and public published data.
 */
@Tag("live-ai")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "securitystatus.assistant.live=true")
@WithMockUser(username = "team", roles = Roles.SECURITY)
class LiveAssistantTest extends SpringBrowserlessTest {

    @Autowired
    AssistantProviders providers;

    @Autowired
    AssistantTail tail;

    @Test
    void the_canonical_question_gets_an_answer_grounded_in_a_tool_call() {
        Assumptions.assumeTrue(providers.live(), "no model: run with the ai profile and a key");
        long before = tail.newest();
        UI.getCurrent().navigate("findings");

        test($(MessageInput.class).withClassName("assistant-input").single()).send("Is 24.9.21 safe?");

        await().pollInSameThread().atMost(Duration.ofSeconds(90)).pollInterval(Duration.ofMillis(500))
                // The query first: the end of the turn is a UI.access task that only a component query runs.
                .until(() -> !messages().isEmpty() && tail.entries().stream().anyMatch(entry -> entry.number() > before
                        && (entry.kind() == AssistantTail.Kind.ANSWER || entry.kind() == AssistantTail.Kind.FAILURE)));
        List<AssistantTail.Entry> turn = tail.entries().stream().filter(entry -> entry.number() > before).toList();
        turn.forEach(entry -> System.out.println("live tail: " + entry.kind() + " " + entry.text()));

        assertThat(turn).extracting(AssistantTail.Entry::kind).doesNotContain(AssistantTail.Kind.FAILURE);
        assertThat(turn).filteredOn(entry -> entry.kind() == AssistantTail.Kind.TOOL)
                .anyMatch(entry -> entry.text().startsWith("versionStatus "));
        assertThat(messages().getLast().getUserName()).isEqualTo("Assistant");
        assertThat(messages().getLast().getText()).isNotBlank();
    }

    private List<MessageListItem> messages() {
        return test($(MessageList.class).withClassName("assistant-messages").single()).getMessages();
    }
}

package com.vaadin.securitystatus.assistant;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.support.StaticApplicationContext;

import com.vaadin.browserless.BrowserlessExtension;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.ai.common.AIAttachment;
import com.vaadin.flow.component.ai.common.ChatMessage;
import com.vaadin.flow.component.ai.provider.LLMProvider;
import com.vaadin.flow.component.messages.MessageListItem;
import com.vaadin.securitystatus.base.observe.AssistantTail;
import com.vaadin.securitystatus.data.FixturePages;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * The chat panel on its own, with no Spring context: the JUnit 6
 * {@link BrowserlessExtension} gives the window, and a provider scripted here
 * stands in for the model, the way the 25.4 page on testing AI views shows,
 * and the conversation is read through the typed locators.
 * What the cassettes of {@link AssistantBrowserlessTest} cannot do is fail or
 * stream on cue, so those two are proven here.
 */
class AssistantPanelBrowserlessTest {

    @RegisterExtension
    BrowserlessExtension window = new BrowserlessExtension().withViewPackages(AssistantPanelBrowserlessTest.class);

    private final List<LLMProvider.LLMRequest> requests = new ArrayList<>();
    private final AssistantTail tail = new AssistantTail(Clock.fixed(FixturePages.NOW, ZoneOffset.UTC));
    private Flux<String> next = Flux.empty();

    @BeforeEach
    void openThePanel() {
        LLMProvider scripted = new LLMProvider() {
            @Override
            public Flux<String> stream(LLMRequest request) {
                requests.add(request);
                return next;
            }

            @Override
            public void setHistory(List<ChatMessage> history, Map<String, List<AIAttachment>> attachments) {
                // A new panel, an empty history: nothing to restore.
            }
        };
        var providers = new AssistantProviders(new StaticApplicationContext().getBeanProvider(ChatModel.class),
                false, false) {
            @Override
            LLMProvider create(String unrecorded) {
                return scripted;
            }
        };
        UI.getCurrent().add(new AssistantPanel(providers, new AssistantTools(FixturePages.offline(), tail), tail,
                new AssistantHistory(), "team"));
    }

    // A turn the provider fails ends in the bundle's sentence, not the orchestrator's English one
    @Test
    void a_failed_turn_says_so_in_the_bundles_words_and_is_in_the_tail() {
        next = Flux.error(new IOException("Connection refused"));

        window.findMessageInput().send("Is 24.9.21 safe?");

        List<MessageListItem> messages = window.findMessageList().getMessages();
        assertThat(messages).hasSize(2);
        assertThat(messages.getLast().getText()).isEqualTo(
                "The assistant could not answer this time. Try again in a moment.");
        assertThat(messages.getLast().hasClassName("assistant-failed")).isTrue();
        assertThat(tail.entries()).extracting(AssistantTail.Entry::kind).contains(AssistantTail.Kind.FAILURE);
        assertThat(requests).singleElement().extracting(LLMProvider.LLMRequest::userMessage)
                .isEqualTo("Is 24.9.21 safe?");
    }

    // A streamed answer grows in the list chunk by chunk, and the turn ends as an answer
    @Test
    void a_streamed_answer_grows_with_each_chunk() {
        Sinks.Many<String> answer = Sinks.many().unicast().onBackpressureBuffer();
        next = answer.asFlux();

        window.findMessageInput().send("Is 24.9.21 safe?");
        assertThat(window.findMessageList().size()).isEqualTo(1);

        answer.tryEmitNext("24.9.21 has a fix: ");
        assertThat(window.findMessageList().getMessage(1).getText()).isEqualTo("24.9.21 has a fix: ");

        answer.tryEmitNext("update to 24.9.26.");
        answer.tryEmitComplete();
        assertThat(window.findMessageList().getMessage(1).getText()).isEqualTo("24.9.21 has a fix: update to 24.9.26.");
        assertThat(tail.entries()).extracting(AssistantTail.Entry::kind).contains(AssistantTail.Kind.ANSWER)
                .doesNotContain(AssistantTail.Kind.FAILURE);
    }
}

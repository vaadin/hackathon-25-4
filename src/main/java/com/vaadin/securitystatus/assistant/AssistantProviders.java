package com.vaadin.securitystatus.assistant;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.vaadin.flow.component.ai.provider.LLMProvider;
import com.vaadin.flow.component.ai.provider.SpringAILLMProvider;

/**
 * Chooses the provider every assistant surface gets, a new one each, since a
 * provider may serve only one orchestrator. The live model when the {@code ai}
 * profile put a {@link ChatModel} on the classpath and the configuration
 * allows it, else the cassette provider, D-136.
 */
@Component
class AssistantProviders {

    private final ObjectProvider<ChatModel> model;
    private final boolean liveAllowed;

    AssistantProviders(ObjectProvider<ChatModel> model,
            @Value("${securitystatus.assistant.live:true}") boolean liveAllowed) {
        this.model = model;
        this.liveAllowed = liveAllowed;
    }

    /** Whether the surfaces talk to a real model. */
    boolean live() {
        return liveAllowed && model.getIfAvailable() != null;
    }

    /**
     * A new provider.
     *
     * @param unrecorded
     *            what the cassette provider says to a question it has no
     *            recording of
     */
    LLMProvider create(String unrecorded) {
        ChatModel chat = liveAllowed ? model.getIfAvailable() : null;
        if (chat == null) {
            return new CassetteLLMProvider(unrecorded);
        }
        // Streaming, the default: the answer arrives through push, which the
        // application already has, and the request thread is never held.
        return new SpringAILLMProvider(chat);
    }
}

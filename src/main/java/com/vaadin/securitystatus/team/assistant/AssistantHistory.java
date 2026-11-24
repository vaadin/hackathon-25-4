package com.vaadin.securitystatus.team.assistant;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.vaadin.flow.component.ai.common.ChatMessage;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;

/**
 * The conversation of one session, so that a panel built again, after the
 * reader moved between the shells or opened another tab, shows the exchange
 * so far and the model has it as context. Text only: attachments are not
 * replayed.
 */
@Component
@VaadinSessionScope
public class AssistantHistory {

    private final List<ChatMessage> messages = new ArrayList<>();

    public synchronized List<ChatMessage> messages() {
        return List.copyOf(messages);
    }

    public synchronized void keep(List<ChatMessage> conversation) {
        messages.clear();
        messages.addAll(conversation);
    }
}

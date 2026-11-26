package com.vaadin.securitystatus.team.assistant;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.springframework.stereotype.Component;

import com.vaadin.flow.component.ai.common.ChatMessage;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;

/**
 * The conversation of one session, so that a panel built again, after the
 * reader moved between the shells or opened another tab, shows the exchange
 * so far and the model has it as context. Text only: attachments are not
 * replayed. Only the last {@link #KEPT} messages are kept, since each turn
 * sends them all again, and a session asks at most {@link #PROMPTS} prompts
 * within {@link #WINDOW} across the three surfaces, D-260.
 */
@Component
@VaadinSessionScope
public class AssistantHistory {

    static final int KEPT = 40;
    static final int PROMPTS = 30;
    static final Duration WINDOW = Duration.ofMinutes(10);

    private final List<ChatMessage> messages = new ArrayList<>();
    private final Deque<Instant> asked = new ArrayDeque<>();
    private final Clock clock;

    public AssistantHistory() {
        this(Clock.systemUTC());
    }

    AssistantHistory(Clock clock) {
        this.clock = clock;
    }

    public synchronized List<ChatMessage> messages() {
        return List.copyOf(messages);
    }

    public synchronized void keep(List<ChatMessage> conversation) {
        messages.clear();
        messages.addAll(conversation.subList(Math.max(0, conversation.size() - KEPT), conversation.size()));
    }

    /** Whether one more prompt may go to the model now, counting it when it may. */
    public synchronized boolean admit() {
        Instant now = clock.instant();
        while (!asked.isEmpty() && asked.peekFirst().isBefore(now.minus(WINDOW))) {
            asked.removeFirst();
        }
        if (asked.size() >= PROMPTS) {
            return false;
        }
        asked.addLast(now);
        return true;
    }
}

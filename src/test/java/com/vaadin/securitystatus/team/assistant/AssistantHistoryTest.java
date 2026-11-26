package com.vaadin.securitystatus.team.assistant;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.ai.common.ChatMessage;

class AssistantHistoryTest {

    @Test
    void a_session_asks_a_bounded_number_of_prompts_in_a_window_and_then_again() {
        var now = new Instant[] { Instant.parse("2026-11-24T10:00:00Z") };
        var history = new AssistantHistory(new Clock() {
            @Override
            public Instant instant() {
                return now[0];
            }

            @Override
            public java.time.ZoneId getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(java.time.ZoneId zone) {
                return this;
            }
        });

        for (int i = 0; i < AssistantHistory.PROMPTS; i++) {
            assertThat(history.admit()).isTrue();
        }
        assertThat(history.admit()).isFalse();

        now[0] = now[0].plus(AssistantHistory.WINDOW).plus(Duration.ofSeconds(1));
        assertThat(history.admit()).isTrue();
    }

    @Test
    void only_the_last_messages_are_kept() {
        var history = new AssistantHistory();
        List<ChatMessage> conversation = new ArrayList<>();
        for (int i = 0; i < AssistantHistory.KEPT + 10; i++) {
            conversation.add(new ChatMessage(ChatMessage.Role.USER, "m" + i, null, null));
        }

        history.keep(conversation);

        assertThat(history.messages()).hasSize(AssistantHistory.KEPT);
        assertThat(history.messages().getLast().content()).isEqualTo("m" + (AssistantHistory.KEPT + 9));
    }
}

package com.vaadin.securitystatus.data;

import java.time.Instant;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.shared.SharedValueSignal;

/**
 * The newest {@code generated_at} the application has read, shared by every
 * session. Null until a measurement has been read.
 */
@Component
public class DataRefreshSignal {

    private final SharedValueSignal<@Nullable Instant> generatedAt = new SharedValueSignal<>(Instant.class);

    public Signal<@Nullable Instant> generatedAt() {
        return generatedAt.asReadonly();
    }

    public void publish(Instant newest) {
        generatedAt.set(newest);
    }

    /** Back to knowing nothing, for a test that needs a cold start. */
    void forget() {
        generatedAt.set(null);
    }
}

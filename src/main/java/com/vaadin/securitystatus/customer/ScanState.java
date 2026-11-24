package com.vaadin.securitystatus.customer;

import java.io.Serializable;
import java.util.Optional;

import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;

/**
 * The scanner report of this session, read. The file itself was never kept:
 * this is what it said, held in memory until the session ends or the next
 * file replaces it, because the address carries only the version half of the
 * question.
 */
@SpringComponent
@VaadinSessionScope
public class ScanState {

    /** One read report and the name of the file it came from. */
    public record Held(String fileName, ScannerReport report) implements Serializable {
    }

    private final ValueSignal<Optional<Held>> held = new ValueSignal<>(Optional.empty());
    private final ValueSignal<String> refusal = new ValueSignal<>("");

    public ValueSignal<Optional<Held>> held() {
        return held;
    }

    /** What was said about the last file that could not be read, empty when the last one was. */
    public ValueSignal<String> refusal() {
        return refusal;
    }
}

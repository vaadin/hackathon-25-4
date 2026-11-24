package com.vaadin.securitystatus.customer;

import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;

/**
 * The question as the reader has typed it so far, for the whole session:
 * switching tabs, navigating to an answer and back, or opening a second tab of
 * the same session never loses it. Clearing a field withdraws its part.
 */
@SpringComponent
@VaadinSessionScope
public class LookupState {

    /** The three ways of asking. */
    public enum Tab {
        VERSION, IDS, REPORT
    }

    private final ValueSignal<Tab> tab = new ValueSignal<>(Tab.VERSION);
    private final ValueSignal<String> version = new ValueSignal<>("");
    private final ValueSignal<String> ids = new ValueSignal<>("");
    private final ValueSignal<String> idsVersion = new ValueSignal<>("");

    public ValueSignal<Tab> tab() {
        return tab;
    }

    /** The version field of the version tab, and of the report tab when it asks. */
    public ValueSignal<String> version() {
        return version;
    }

    /** The advisory ids as pasted. */
    public ValueSignal<String> ids() {
        return ids;
    }

    /** The optional version beside the ids. */
    public ValueSignal<String> idsVersion() {
        return idsVersion;
    }
}

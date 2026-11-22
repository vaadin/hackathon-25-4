package com.vaadin.securitystatus.diagnostics;

import com.vaadin.flow.router.NavigationEndedEvent;

/**
 * How a navigation ended, one constant per record of the sealed
 * {@link NavigationEndedEvent.Outcome}. The switch in {@link #of} has no
 * default branch, so a fifth outcome in a later release stops the compilation
 * here instead of being counted under a wrong name.
 */
public enum Outcome {
    COMPLETED, FAILED, NOT_SHOWN, POSTPONED;

    static Outcome of(NavigationEndedEvent.Outcome outcome) {
        return switch (outcome) {
        case NavigationEndedEvent.Completed completed -> COMPLETED;
        case NavigationEndedEvent.Failed failed -> FAILED;
        case NavigationEndedEvent.NotShown notShown -> NOT_SHOWN;
        case NavigationEndedEvent.Postponed postponed -> POSTPONED;
        };
    }
}

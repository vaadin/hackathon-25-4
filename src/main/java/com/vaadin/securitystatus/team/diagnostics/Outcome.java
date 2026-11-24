package com.vaadin.securitystatus.team.diagnostics;

import com.vaadin.flow.router.AccessDeniedException;
import com.vaadin.flow.router.NavigationEndedEvent;
import com.vaadin.flow.router.NotFoundException;

/**
 * How a navigation ended, one constant per record of the sealed
 * {@link NavigationEndedEvent.Outcome}, except that a failure is split in two:
 * refused, when the router answered "not found" or "not allowed", and broke,
 * for anything else, which is the one somebody has to look at. The switch in
 * {@link #of} has no default branch, so a fifth outcome in a later release
 * stops the compilation here instead of being counted under a wrong name.
 */
public enum Outcome {
    COMPLETED, REFUSED, BROKE, NOT_SHOWN, POSTPONED;

    public static Outcome of(NavigationEndedEvent.Outcome outcome) {
        return switch (outcome) {
        case NavigationEndedEvent.Completed completed -> COMPLETED;
        case NavigationEndedEvent.Failed failed -> refused(failed.error()) ? REFUSED : BROKE;
        case NavigationEndedEvent.NotShown notShown -> NOT_SHOWN;
        case NavigationEndedEvent.Postponed postponed -> POSTPONED;
        };
    }

    private static boolean refused(Throwable error) {
        return error instanceof NotFoundException || error instanceof AccessDeniedException
                || error instanceof org.springframework.security.access.AccessDeniedException;
    }
}

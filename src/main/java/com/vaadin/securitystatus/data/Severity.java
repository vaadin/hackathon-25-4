package com.vaadin.securitystatus.data;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** The four severities the published data names, worst first, and how anything else sorts. */
public final class Severity {

    public static final List<String> WORST_FIRST = List.of("critical", "high", "moderate", "low");

    private Severity() {
    }

    /** Where a severity sorts, worst first; one the data does not name, "unknown" included, sorts last. */
    public static int rank(Optional<String> word) {
        int rank = word.map(found -> WORST_FIRST.indexOf(found.toLowerCase(Locale.ROOT))).orElse(-1);
        return rank < 0 ? WORST_FIRST.size() : rank;
    }

    /** A severity the data names, lowercased, or nothing for "unknown" and any other word. */
    public static Optional<String> known(String word) {
        return Optional.of(word.toLowerCase(Locale.ROOT)).filter(WORST_FIRST::contains);
    }
}

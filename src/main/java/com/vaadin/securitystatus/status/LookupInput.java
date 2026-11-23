package com.vaadin.securitystatus.status;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * What a reader types into the lookup, in the spelling the measurements use.
 * Pure functions, no state.
 */
final class LookupInput {

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern VAADIN_PREFIX = Pattern.compile("^v(?:aadin)?(?=\\d)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ADVISORY_START = Pattern.compile("^(?:CVE|GHSA)[-\\s]", Pattern.CASE_INSENSITIVE);
    private static final Pattern SEPARATORS = Pattern.compile("[\\s,;]+");
    private static final Pattern CVE = Pattern.compile("CVE-\\d{4}-\\d{4,}");
    private static final Pattern GHSA = Pattern.compile("GHSA(?:-[23456789cfghjmpqrvwx]{4}){3}");
    private static final Pattern ANY_ID = Pattern.compile("(?i)CVE-\\d{4}-\\d{4,}|GHSA(?:-[a-z0-9]{4}){3}");

    /** The ids that parse, in the order given and once each, and the rest. */
    record Ids(List<String> ids, List<String> unrecognised) {
    }

    private LookupInput() {
    }

    /**
     * {@code V24.9.20}, {@code Vaadin 24.9.20} and {@code 24.9.20 } are the same
     * question. Only a spelling that starts like a version loses its prefix:
     * anything else passes through, because inventing a version out of it would
     * answer a question nobody asked.
     */
    static String cleanVersion(String typed) {
        String compact = WHITESPACE.matcher(typed).replaceAll("");
        return VAADIN_PREFIX.matcher(compact).replaceFirst("");
    }

    /** Whether the text is an advisory id rather than a version, asked of the raw text. */
    static boolean isAdvisory(String typed) {
        return ADVISORY_START.matcher(typed.strip()).find();
    }

    /**
     * One or more ids, split on spaces, commas, semicolons or new lines. A CVE id
     * is upper case throughout; a GHSA keeps its identifying part in lower case,
     * because an upper cased GHSA matches nothing.
     */
    static Ids splitIds(String text) {
        var ids = new LinkedHashSet<String>();
        var unrecognised = new ArrayList<String>();
        for (String token : SEPARATORS.split(text.strip())) {
            if (token.isEmpty()) {
                continue;
            }
            String id = normalizeId(token);
            if (CVE.matcher(id).matches() || GHSA.matcher(id).matches()) {
                ids.add(id);
                continue;
            }
            // An id inside an address or a path, as copied from a scanner or this page's own URL.
            var inside = ANY_ID.matcher(token);
            boolean found = false;
            while (inside.find()) {
                String candidate = normalizeId(inside.group());
                if (CVE.matcher(candidate).matches() || GHSA.matcher(candidate).matches()) {
                    ids.add(candidate);
                    found = true;
                }
            }
            if (!found) {
                unrecognised.add(token);
            }
        }
        return new Ids(List.copyOf(ids), List.copyOf(unrecognised));
    }

    static String normalizeId(String token) {
        if (token.regionMatches(true, 0, "GHSA-", 0, 5)) {
            return "GHSA-" + token.substring(5).toLowerCase(Locale.ROOT);
        }
        return token.toUpperCase(Locale.ROOT);
    }
}

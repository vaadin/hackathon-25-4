package com.vaadin.securitystatus.status;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.vaadin.securitystatus.data.model.CasesIndex;
import com.vaadin.securitystatus.data.model.CasesIndex.AdvisoryCatalogEntry;

/**
 * The advisories list without components: the order, the filter and the
 * counts the pills carry.
 */
final class AdvisoryRules {

    /**
     * The verdicts worst first, the source's order for picking one headline:
     * not assessed yet ranks above a real risk because nobody has looked.
     */
    static final List<String> VERDICTS = List.of("in_triage", "risk", "configuration", "no_risk", "not_affected",
            "false_positive", "fixed", "not_current");

    private static final List<String> SEVERITIES = List.of("critical", "high", "moderate", "low");

    private AdvisoryRules() {
    }

    /**
     * Every advisory, worst severity first, then by id, rule 6.4.22. The rule
     * puts the oldest first between the two, and the index carries no date an
     * advisory was published, so the id is the whole tie break, D-81.
     */
    static List<AdvisoryCatalogEntry> ordered(CasesIndex index) {
        var rows = new ArrayList<>(index.advisories().values());
        rows.sort(Comparator.comparingInt(AdvisoryRules::severityRank).thenComparing(AdvisoryCatalogEntry::cve));
        return rows;
    }

    private static int severityRank(AdvisoryCatalogEntry entry) {
        return entry.severity().map(word -> word.toLowerCase(java.util.Locale.ROOT)).map(SEVERITIES::indexOf)
                .filter(rank -> rank >= 0).orElse(SEVERITIES.size());
    }

    /** A verdict the address may name: one of the closed vocabulary, else nothing, so everything is shown. */
    static Optional<String> verdict(Optional<String> asked) {
        return asked.map(String::strip).filter(VERDICTS::contains);
    }

    /** The rows the filter leaves, in the order given. */
    static List<AdvisoryCatalogEntry> filtered(List<AdvisoryCatalogEntry> rows, Optional<String> verdict) {
        if (verdict.isEmpty()) {
            return rows;
        }
        return rows.stream().filter(row -> row.applicability().equals(verdict)).toList();
    }

    /** How many rows each verdict would show, worst first, only the verdicts present. */
    static Map<String, Integer> counts(List<AdvisoryCatalogEntry> rows) {
        var counts = new LinkedHashMap<String, Integer>();
        for (String verdict : VERDICTS) {
            int count = (int) rows.stream().filter(row -> row.applicability().filter(verdict::equals).isPresent())
                    .count();
            if (count > 0) {
                counts.put(verdict, count);
            }
        }
        return counts;
    }

    /** Where the advisory's own record lives, and the host a reader recognises it by. */
    static String recordHost(String id) {
        return java.net.URI.create(CveRules.recordUrl(id)).getHost();
    }
}

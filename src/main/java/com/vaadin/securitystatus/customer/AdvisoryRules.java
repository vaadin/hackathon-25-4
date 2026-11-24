package com.vaadin.securitystatus.customer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.vaadin.securitystatus.data.model.CasesIndex.AdvisoryCatalogEntry;
import com.vaadin.securitystatus.data.model.CasesIndex;

/**
 * The advisories list without components: the order, the filter and the
 * counts the pills carry.
 */
public final class AdvisoryRules {

    /**
     * The verdicts in the order the prototype offers its filters, what needs
     * attention first and what is settled last, D-192.
     */
    public static final List<String> VERDICTS = List.of("risk", "in_triage", "configuration", "no_risk", "not_affected",
            "false_positive", "fixed", "not_current");

    private static final List<String> SEVERITIES = List.of("critical", "high", "moderate", "low");

    private AdvisoryRules() {
    }

    /**
     * Every advisory, worst severity first, then by id, rule 6.4.22. The rule
     * puts the oldest first between the two, and the index carries no date an
     * advisory was published, so the id is the whole tie break, D-81.
     */
    public static List<AdvisoryCatalogEntry> ordered(CasesIndex index) {
        var rows = new ArrayList<>(index.advisories().values());
        rows.sort(Comparator.comparingInt(AdvisoryRules::severityRank).thenComparing(AdvisoryCatalogEntry::cve));
        return rows;
    }

    private static int severityRank(AdvisoryCatalogEntry entry) {
        return entry.severity().map(word -> word.toLowerCase(java.util.Locale.ROOT)).map(SEVERITIES::indexOf)
                .filter(rank -> rank >= 0).orElse(SEVERITIES.size());
    }

    /** A verdict the address may name: one of the closed vocabulary, else nothing, so everything is shown. */
    public static Optional<String> verdict(Optional<String> asked) {
        return asked.map(String::strip).filter(VERDICTS::contains);
    }

    /**
     * The verdict a row is listed under: its applicability, except that one
     * still in triage which no current release reports is "not on a current
     * release", as the CVE screen and the prototype read it, D-192.
     */
    public static Optional<String> listed(AdvisoryCatalogEntry row) {
        return row.applicability().map(said -> "in_triage".equals(said) && Boolean.FALSE.equals(row.current())
                ? "not_current" : said);
    }

    /** The rows the filter leaves, in the order given. */
    public static List<AdvisoryCatalogEntry> filtered(List<AdvisoryCatalogEntry> rows, Optional<String> verdict) {
        if (verdict.isEmpty()) {
            return rows;
        }
        return rows.stream().filter(row -> listed(row).equals(verdict)).toList();
    }

    /** How many rows each verdict would show, in the filters' order, only the verdicts present. */
    public static Map<String, Integer> counts(List<AdvisoryCatalogEntry> rows) {
        var counts = new LinkedHashMap<String, Integer>();
        for (String verdict : VERDICTS) {
            int count = (int) rows.stream().filter(row -> listed(row).filter(verdict::equals).isPresent())
                    .count();
            if (count > 0) {
                counts.put(verdict, count);
            }
        }
        return counts;
    }

    /**
     * The catalogue's label for a verdict as published, lower case. The index
     * carries none for `not_current`, so that one is the prototype's own words
     * from the bundle, D-188, D-192.
     */
    public static Optional<String> label(CasesIndex index, String word, java.util.function.Function<String, String> bundle) {
        return Optional.ofNullable(index.labels().get(word))
                .or(() -> "not_current".equals(word) ? Optional.of(bundle.apply("verdict.label.not_current"))
                        : Optional.empty());
    }

    /** Where the advisory's own record lives, and the host a reader recognises it by. */
    public static String recordHost(String id) {
        return java.net.URI.create(CveRules.recordUrl(id)).getHost();
    }
}

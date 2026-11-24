package com.vaadin.securitystatus.team.explorer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.vaadin.securitystatus.data.VersionNumber;
import com.vaadin.securitystatus.data.model.ExplorerFront.Finding;
import com.vaadin.securitystatus.data.model.ExplorerFront.Line;
import com.vaadin.securitystatus.data.model.ExplorerFront;

/**
 * The Spring Boot page's questions answered without components: which
 * Spring Boot releases the measured lines sit on, the findings each brings,
 * whether every line on it resolves the same set, and the three groups.
 * Reads {@code data/front.json} only.
 */
public final class SpringRules {

    /** The groups of one Spring Boot release: what it asks of the reader first. */
    public enum Kind {
        RISK(true), UNJUDGED(false), ANSWERED(false);

        private final boolean startsOpen;

        Kind(boolean startsOpen) {
            this.startsOpen = startsOpen;
        }

        /** The prototype's folds, D-219: the risks open, the rest shut. */
        public boolean startsOpen() {
            return startsOpen;
        }
    }

    /**
     * One Spring Boot release: the lines whose measurement pinned it, every
     * finding it brings to any of them once, and whether those lines resolve
     * different sets.
     */
    public record Boot(VersionNumber release, List<String> lines, List<Finding> findings, boolean differs) {
    }

    private SpringRules() {
    }

    /**
     * One entry per Spring Boot release a measured line sits on, newest
     * first. An advisory two lines carry is one advisory, read at its worst
     * verdict, rule 6.4.23.
     */
    public static List<Boot> byBoot(ExplorerFront front) {
        var lines = new LinkedHashMap<VersionNumber, List<Line>>();
        for (Line line : front.lines()) {
            line.springBoot().ifPresent(boot -> lines.computeIfAbsent(boot, key -> new ArrayList<>()).add(line));
        }
        var boots = new ArrayList<Boot>();
        lines.forEach((release, on) -> {
            var seen = new LinkedHashMap<String, Finding>();
            Set<Set<String>> sets = new java.util.HashSet<>();
            for (Line line : on) {
                Set<String> here = new TreeSet<>();
                for (Finding finding : line.findings()) {
                    if (!"spring-boot".equals(finding.owner().orElse(""))) {
                        continue;
                    }
                    here.add(finding.cve());
                    seen.merge(finding.cve(), finding, SpringRules::worse);
                }
                sets.add(here);
            }
            var findings = new ArrayList<>(seen.values());
            findings.sort(Comparator.<Finding>comparingInt(finding -> ExplorerRules.severityRank(finding.severity()))
                    .thenComparing(Finding::cve));
            boots.add(new Boot(release, on.stream().map(Line::line).toList(), findings, sets.size() > 1));
        });
        boots.sort(Comparator.comparing(Boot::release).reversed());
        return boots;
    }

    private static Finding worse(Finding one, Finding other) {
        return kind(other).ordinal() < kind(one).ordinal() ? other : one;
    }

    /**
     * A real risk, not judged yet, or answered, by the explorer's rule of
     * D-205: a verdict outside the five ruled out verdicts and not {@code
     * risk} is not judged, as the prototype reads it, D-219.
     */
    static Kind kind(Finding finding) {
        return switch (ExplorerRules.stateOf(finding)) {
            case VULNERABLE -> Kind.RISK;
            case UNJUDGED -> Kind.UNJUDGED;
            case NOT_AFFECTED -> Kind.ANSWERED;
        };
    }

    /** The groups present, in their order, each worst first. */
    public static Map<Kind, List<Finding>> grouped(List<Finding> findings) {
        var groups = new LinkedHashMap<Kind, List<Finding>>();
        for (Kind kind : Kind.values()) {
            List<Finding> members = findings.stream().filter(finding -> kind(finding) == kind).toList();
            if (!members.isEmpty()) {
                groups.put(kind, members);
            }
        }
        return groups;
    }

    /** The address of an advisory's record: NVD for a CVE, GitHub's database for a GHSA, which NVD does not hold. */
    public static String recordUrl(String id) {
        return id.startsWith("GHSA-") ? "https://github.com/advisories/" + id
                : "https://nvd.nist.gov/vuln/detail/" + id;
    }
}

package com.vaadin.securitystatus.explorer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.vaadin.securitystatus.data.VersionNumber;
import com.vaadin.securitystatus.data.model.ExplorerFront;
import com.vaadin.securitystatus.data.model.ExplorerFront.Finding;
import com.vaadin.securitystatus.data.model.ExplorerFront.Line;

/**
 * The Spring Boot page's questions answered without components: which
 * Spring Boot releases the measured lines sit on, the findings each brings,
 * whether every line on it resolves the same set, and the three groups.
 * Reads {@code data/front.json} only.
 */
final class SpringRules {

    /** The groups of one Spring Boot release: what it asks of the reader first. */
    enum Kind {
        RISK(true), UNJUDGED(true), ANSWERED(false);

        private final boolean startsOpen;

        Kind(boolean startsOpen) {
            this.startsOpen = startsOpen;
        }

        /** The explorer's fold rule, 6.6.2: the two that ask for work start open. */
        boolean startsOpen() {
            return startsOpen;
        }
    }

    /**
     * One Spring Boot release: the lines whose measurement pinned it, every
     * finding it brings to any of them once, and whether those lines resolve
     * different sets.
     */
    record Boot(VersionNumber release, List<String> lines, List<Finding> findings, boolean differs) {
    }

    private SpringRules() {
    }

    /**
     * One entry per Spring Boot release a measured line sits on, newest
     * first. An advisory two lines carry is one advisory, read at its worst
     * verdict, rule 6.4.23.
     */
    static List<Boot> byBoot(ExplorerFront front) {
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

    /** A real risk, not judged yet, or answered: {@code in_triage} is the absence of a verdict. */
    static Kind kind(Finding finding) {
        var verdict = finding.verdict().filter(word -> !"in_triage".equals(word));
        if (verdict.isEmpty()) {
            return Kind.UNJUDGED;
        }
        return "risk".equals(verdict.get()) ? Kind.RISK : Kind.ANSWERED;
    }

    /** The groups present, in their order, each worst first. */
    static Map<Kind, List<Finding>> grouped(List<Finding> findings) {
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
    static String recordUrl(String id) {
        return id.startsWith("GHSA-") ? "https://github.com/advisories/" + id
                : "https://nvd.nist.gov/vuln/detail/" + id;
    }
}

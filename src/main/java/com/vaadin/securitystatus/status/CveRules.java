package com.vaadin.securitystatus.status;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.vaadin.securitystatus.data.VersionNumber;
import com.vaadin.securitystatus.data.model.Assessment;
import com.vaadin.securitystatus.data.model.CasesIndex;
import com.vaadin.securitystatus.data.model.CveAnswer;
import com.vaadin.securitystatus.data.model.VersionAnswer;

/**
 * The questions the CVE screen asks of one advisory and, when the reader
 * named one, of their measured version. Answered once and without a component
 * in sight, so the card, the line cards and the copied answer agree.
 */
final class CveRules {

    private CveRules() {
    }

    /** The settled answers a direct lookup can give, feature 05. */
    enum State {
        /** The reader's version is affected and the upgrade of its line clears it. */
        FIX,
        /** A risk on the reader's version with no release to move to. */
        AFFECTS,
        /** Reported on the reader's version and nobody has judged it. */
        TRIAGE,
        /** A risk the reader's Spring Boot brings or pins. */
        BOOT,
        /** A risk only a discontinued product brings. */
        DISCONTINUED,
        /** Measured on the reader's version and judged not to reach a Vaadin application. */
        NOT_AFFECTED,
        /** Not reported on the reader's version, and reported on earlier releases of the line. */
        FIXED,
        /** Not reported on the reader's version, and never on its line. */
        NOT_REPORTED,
        /** No measured version to answer for: what the catalogue knows. */
        MEASURED,
        /** An id nobody measured. */
        UNKNOWN
    }

    /** The release that clears the advisory for the reader, and what moving there leaves. */
    record Fix(VersionNumber to, boolean sameLine, boolean secure, @Nullable Integer remains) {
    }

    /** One verdict that answers the advisory, with the coordinates it answers for. */
    record Said(String id, Assessment assessment, List<String> packages) {
    }

    /**
     * The upgrade the reader's version document offers for this advisory: only
     * when the advisory is a risk there and the upgrade clears it, rule 6.11.11.
     */
    static Optional<Fix> fixFor(@Nullable VersionAnswer version, CasesIndex index, String id) {
        if (version == null || !version.risk().contains(id) || !version.fixable().contains(id)) {
            return Optional.empty();
        }
        return VersionRules.upgradeTo(version).map(to -> new Fix(to,
                to.line().equals(version.line().orElse(version.version().line())),
                index.summaryOf(to).flatMap(CasesIndex.Summary::state).filter("secure"::equals).isPresent(),
                version.upgrade().map(VersionAnswer.Upgrade::remains).orElse(null)));
    }

    /** Which answer the card gives. The reader's version outranks the catalogue. */
    static State state(@Nullable CveAnswer advisory, @Nullable VersionAnswer version, Optional<Fix> fix) {
        if (advisory == null) {
            return State.UNKNOWN;
        }
        if (version == null) {
            return State.MEASURED;
        }
        String id = advisory.cve();
        if (fix.isPresent()) {
            return State.FIX;
        }
        if (version.risk().contains(id)) {
            return State.AFFECTS;
        }
        if (version.triage().contains(id)) {
            return State.TRIAGE;
        }
        if (version.springPinned().contains(id)) {
            return State.BOOT;
        }
        if (version.discontinued().contains(id)) {
            return State.DISCONTINUED;
        }
        // Every route of a finding in `theirs`, `theirs_risk` included, is the reader's own Spring Boot:
        // it does not reach them through Vaadin, as the prototype answers, D-187.
        if (version.ruledOut().contains(id) || version.theirs().contains(id) || version.theirsRisk().contains(id)
                || version.fromBuild().contains(id)) {
            return State.NOT_AFFECTED;
        }
        boolean reportedOnTheLine = Optional.ofNullable(advisory.coverage().get(lineOf(version)))
                .map(CveAnswer.Coverage::affectedCount).filter(count -> count > 0).isPresent();
        return reportedOnTheLine ? State.FIXED : State.NOT_REPORTED;
    }

    static String lineOf(VersionAnswer version) {
        return version.line().orElse(version.version().line());
    }

    /**
     * Every verdict answering the advisory: on the reader's version the ones
     * its document lists, rule 6.4.8, and with no version every verdict the
     * advisory carries, each with the coordinates it spoke for on any line.
     */
    static List<Said> verdicts(CveAnswer advisory, @Nullable VersionAnswer version) {
        if (version != null) {
            return VersionRules.verdictsFor(version, advisory.cve()).stream()
                    .map(one -> new Said(one.id(), one.assessment(), one.packages())).toList();
        }
        Map<String, List<String>> packages = new LinkedHashMap<>();
        advisory.onVersions().values().forEach(verdict -> verdict.saidByPackage().forEach((name, id) -> {
            List<String> names = packages.computeIfAbsent(id, key -> new ArrayList<>());
            if (!names.contains(name)) {
                names.add(name);
            }
        }));
        List<Said> said = new ArrayList<>();
        advisory.assessments().forEach((id, assessment) -> {
            List<String> names = new ArrayList<>(packages.getOrDefault(id, List.of()));
            names.sort(Comparator.naturalOrder());
            said.add(new Said(id, assessment, List.copyOf(names)));
        });
        return said;
    }

    /**
     * The verdict word for the advisory: the reader's version's own where it has
     * one, the triage word where their version reports it unjudged, and with no
     * version the catalogue's, which for an advisory no release we follow
     * reports any more is not "not assessed yet", rule 6.11.22.
     */
    static Optional<String> applicability(CveAnswer advisory, @Nullable VersionAnswer version) {
        if (version != null) {
            var verdict = version.verdicts().get(advisory.cve());
            if (verdict != null && verdict.state().isPresent()) {
                return verdict.state();
            }
            if (version.triage().contains(advisory.cve())) {
                return Optional.of("in_triage");
            }
        }
        return advisory.applicability().map(said -> "in_triage".equals(said)
                && Boolean.FALSE.equals(advisory.current()) ? "not_current" : said);
    }

    /**
     * The part of each coordinate that differs from the others, rule 6.4.9: the
     * longest common start and end, both cut back to a separator so a word is
     * never split, and what lies between. Empty for a single coordinate or when
     * they are all the same.
     */
    record Marked(String before, String differs, String after) {
    }

    static List<Marked> markDifference(List<String> coordinates) {
        if (coordinates.size() < 2 || coordinates.stream().distinct().count() < 2) {
            return coordinates.stream().map(text -> new Marked(text, "", "")).toList();
        }
        int shortest = coordinates.stream().mapToInt(String::length).min().orElse(0);
        String first = coordinates.get(0);
        int start = 0;
        while (start < shortest && sameAt(coordinates, start, false)) {
            start++;
        }
        while (start > 0 && !isSeparator(first.charAt(start - 1))) {
            start--;
        }
        int end = 0;
        while (end < shortest - start && sameAt(coordinates, end, true)) {
            end++;
        }
        while (end > 0 && !isSeparator(first.charAt(first.length() - end))) {
            end--;
        }
        int keepEnd = end;
        int keepStart = start;
        return coordinates.stream().map(text -> new Marked(text.substring(0, keepStart),
                text.substring(keepStart, text.length() - keepEnd), text.substring(text.length() - keepEnd))).toList();
    }

    private static boolean sameAt(List<String> texts, int offset, boolean fromEnd) {
        String first = texts.get(0);
        char wanted = fromEnd ? first.charAt(first.length() - 1 - offset) : first.charAt(offset);
        return texts.stream().allMatch(text -> (fromEnd ? text.charAt(text.length() - 1 - offset) : text.charAt(offset))
                == wanted);
    }

    private static boolean isSeparator(char c) {
        return c == ':' || c == '/' || c == '.' || c == '-' || c == '@';
    }

    /** Where the advisory's own record lives. */
    static String recordUrl(String id) {
        return id.startsWith("GHSA-") ? "https://github.com/advisories/" + id
                : "https://www.cve.org/CVERecord?id=" + id;
    }
}

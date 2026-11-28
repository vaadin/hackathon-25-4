package com.vaadin.securitystatus.customer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Pattern;

import com.vaadin.securitystatus.data.VersionNumber;
import com.vaadin.securitystatus.data.model.Assessment;
import com.vaadin.securitystatus.data.model.CasesIndex;
import com.vaadin.securitystatus.data.model.Verdict;
import com.vaadin.securitystatus.data.model.VersionAnswer;

/**
 * The questions the version screen asks of one measurement, answered once
 * and without a component in sight, so the card, the sections and the table
 * cannot answer them differently.
 */
public final class VersionRules {

    /** The fixes list shows this many rows and counts the rest. */
    public static final int FIXES_SHOWN = 25;

    /** The not assessed notice names this many ids and counts the rest. */
    public static final int TRIAGE_NAMED = 12;

    /** The Spring Boot table shows this many rows and counts the rest. */
    public static final int BOOT_ROWS = 12;

    private static final List<String> SEVERITIES = List.of("critical", "high", "moderate", "low", "unknown");

    private static final int RECENT_ROWS = 8;
    private static final Pattern SENTENCE_END = Pattern.compile("(?<=\\.)\\s+");

    private VersionRules() {
    }

    /** A release the reader can move to that is measured secure, and whether it is on their own line. */
    public record Move(VersionNumber to, boolean sameLine) {
    }

    /** One verdict that answers an advisory on this version, and the packages it speaks for. */
    public record Said(String id, Assessment assessment, List<String> packages) {
    }

    /** The findings a verdict calls a real risk and no release of the line fixes. */
    public static List<String> noFixYet(VersionAnswer answer) {
        return answer.stuck().stream().filter(answer.risk()::contains).toList();
    }

    /** How many findings are open on the version: real risks and the ones nobody has judged yet. */
    public static int openCount(VersionAnswer answer) {
        return answer.risk().size() + answer.triage().size();
    }

    /**
     * Where the upgrade goes, unless it goes nowhere: an upgrade that names the
     * asked version itself has nothing to say about it.
     */
    public static Optional<VersionNumber> upgradeTo(VersionAnswer answer) {
        return answer.upgrade().flatMap(VersionAnswer.Upgrade::to).filter(to -> !to.equals(answer.version()));
    }

    /** The first number of a version, the major. */
    public static String major(VersionNumber version) {
        String text = version.toString();
        int dot = text.indexOf('.');
        return dot < 0 ? text : text.substring(0, dot);
    }

    /**
     * The secure release offered on a red card, rules 6.11.8 and 6.11.23 of the
     * source page: the newest secure release on the reader's own line, else the
     * newest of their major, never older than the version asked and never a
     * prerelease.
     */
    public static Optional<Move> secureMove(CasesIndex index, VersionNumber version, String line) {
        String major = major(version);
        List<VersionNumber> candidates = new ArrayList<>();
        index.summary().forEach((name, row) -> {
            VersionNumber candidate = VersionNumber.of(name);
            if (row.state().filter("secure"::equals).isPresent() && !candidate.isPrerelease()
                    && candidate.compareTo(version) > 0 && major(candidate).equals(major)) {
                candidates.add(candidate);
            }
        });
        Optional<VersionNumber> mine = candidates.stream().filter(candidate -> line.equals(lineOf(index, candidate)))
                .max(Comparator.naturalOrder());
        if (mine.isPresent()) {
            return Optional.of(new Move(mine.get(), true));
        }
        return candidates.stream().max(Comparator.naturalOrder()).map(other -> new Move(other, false));
    }

    private static String lineOf(CasesIndex index, VersionNumber version) {
        return index.summaryOf(version).flatMap(CasesIndex.Summary::line).orElse(version.line());
    }

    /**
     * Where a version's release notes live: the repository the index names for
     * its major, or its default. Empty when the index names neither.
     */
    public static Optional<String> releaseNotes(CasesIndex index, VersionNumber version) {
        Map<String, String> repositories = index.releaseRepos();
        String repository = repositories.getOrDefault(major(version), repositories.get("default"));
        return Optional.ofNullable(repository)
                .map(owner -> "https://github.com/" + owner + "/releases/tag/" + version);
    }

    /**
     * Every verdict that speaks for one advisory on this version, in the order
     * the version lists them, each with the packages it answers for.
     */
    public static List<Said> verdictsFor(VersionAnswer answer, String advisory) {
        Verdict verdict = answer.verdicts().get(advisory);
        if (verdict == null) {
            return List.of();
        }
        Map<String, List<String>> packages = new LinkedHashMap<>();
        verdict.saidByPackage().forEach((name, id) -> packages.computeIfAbsent(id, key -> new ArrayList<>()).add(name));
        List<Said> said = new ArrayList<>();
        for (String id : verdict.assessments()) {
            Assessment assessment = answer.assessments().get(id);
            if (assessment != null) {
                List<String> names = new ArrayList<>(packages.getOrDefault(id, List.of()));
                names.sort(Comparator.naturalOrder());
                said.add(new Said(id, assessment, List.copyOf(names)));
            }
        }
        return said;
    }

    /** Whether any verdict on this advisory offers a workaround. */
    public static boolean hasWorkaround(List<Said> said) {
        return said.stream().anyMatch(one -> one.assessment().workaround().isPresent());
    }

    /**
     * The one line of prose a row shows: the first verdict's impact, sentence by
     * sentence until it has said something.
     */
    public static Optional<String> oneLine(List<Said> said) {
        for (Said one : said) {
            Optional<String> impact = one.assessment().impact().map(String::strip);
            if (impact.isPresent()) {
                List<String> sentences = new ArrayList<>(List.of(SENTENCE_END.split(impact.get())));
                StringBuilder line = new StringBuilder(sentences.remove(0));
                while (line.length() < 40 && !sentences.isEmpty()) {
                    line.append(' ').append(sentences.remove(0));
                }
                return Optional.of(line.toString());
            }
        }
        return Optional.empty();
    }

    /** A coordinate as a reader reads it, the artifact without its group. */
    public static String shortName(String coordinate) {
        int colon = coordinate.lastIndexOf(':');
        return colon < 0 ? coordinate : coordinate.substring(colon + 1);
    }

    /** Advisory ids worst first by the catalogue's severity, then by id, so a cut list keeps what matters. */
    public static List<String> worstFirst(List<String> ids, CasesIndex index) {
        return ids.stream().distinct().sorted(Comparator.comparingInt((String id) -> severityRank(index, id))
                .thenComparing(Comparator.naturalOrder())).toList();
    }

    /** One of the four severity words, lower case, or empty for anything else, unknown included. */
    public static Optional<String> severity(String word) {
        String known = word.toLowerCase(java.util.Locale.ROOT);
        return SEVERITIES.indexOf(known) >= 0 && !"unknown".equals(known) ? Optional.of(known) : Optional.empty();
    }

    private static int severityRank(CasesIndex index, String id) {
        return Optional.ofNullable(index.advisories().get(id)).flatMap(CasesIndex.AdvisoryCatalogEntry::severity)
                .map(SEVERITIES::indexOf).filter(rank -> rank >= 0).orElse(SEVERITIES.size());
    }

    /** The release a measured line's fixed_in names for this advisory on the reader's line. */
    public static Optional<VersionNumber> fixedIn(CasesIndex index, String advisory, String line) {
        return Optional.ofNullable(index.fixedIn().get(advisory)).map(lines -> lines.get(line))
                .flatMap(CasesIndex.FixedIn::fixedIn);
    }

    /** The Spring Boot rows: the pinned ones first in rank, then the ones judged a risk, or all of them. */
    public static List<String> bootRows(VersionAnswer answer, CasesIndex index) {
        List<String> rows = new ArrayList<>(answer.springPinned());
        rows.addAll(answer.theirsRisk().isEmpty() ? answer.theirs() : answer.theirsRisk());
        return worstFirst(rows, index);
    }

    /** How many findings the Spring Boot section stands for. */
    public static int bootTotal(VersionAnswer answer) {
        return answer.theirs().size() + answer.springPinned().size();
    }

    /** What the reader asked is not measured: the release the answer offers instead, if there is one. */
    public record Nearest(VersionNumber version, Kind kind) {

        public enum Kind {
            /** The newest measured release of the reader's own line. */
            LINE,
            /** The newest measured release of the reader's major, on a line nobody follows. */
            MAJOR,
            /** The oldest release we measure, for a version below everything we follow. */
            OLDEST
        }
    }

    public static Optional<Nearest> nearestMeasured(CasesIndex index, VersionNumber asked) {
        Optional<VersionNumber> head = index.byLine().getOrDefault(asked.line(), List.of()).stream()
                .max(Comparator.naturalOrder());
        if (head.isPresent()) {
            return Optional.of(new Nearest(head.get(), Nearest.Kind.LINE));
        }
        TreeMap<VersionNumber, String> measured = new TreeMap<>();
        index.summary().keySet().stream().map(VersionNumber::of).filter(version -> !version.isPrerelease())
                .forEach(version -> measured.put(version, version.toString()));
        String major = major(asked);
        Optional<VersionNumber> ofMajor = measured.keySet().stream().filter(version -> major(version).equals(major))
                .max(Comparator.naturalOrder());
        if (ofMajor.isPresent()) {
            return Optional.of(new Nearest(ofMajor.get(), Nearest.Kind.MAJOR));
        }
        if (!measured.isEmpty() && asked.compareTo(measured.firstKey()) < 0) {
            return Optional.of(new Nearest(measured.firstKey(), Nearest.Kind.OLDEST));
        }
        return Optional.empty();
    }

    /** The lines the versions table lists: the followed ones, a prerelease line when asked or when it is the reader's. */
    public static List<CasesIndex.Line> tableLines(CasesIndex index, String readersLine, boolean withPrereleases) {
        return shownLines(index, true).stream()
                .filter(line -> withPrereleases || !line.isPrerelease() || line.line().equals(readersLine))
                .filter(Objects::nonNull).toList();
    }

    /** The lines a customer can be on, newest first: the followed ones, prereleases only when asked. */
    public static List<CasesIndex.Line> shownLines(CasesIndex index, boolean withPrereleases) {
        var followed = index.lineRule().map(rule -> new HashSet<>(rule.followed())).orElse(null);
        return index.lines().stream()
                .filter(line -> followed == null || followed.isEmpty() || followed.contains(line.line()))
                .filter(line -> withPrereleases || !line.isPrerelease())
                .sorted(Comparator.comparing(CasesIndex.Line::version).reversed()).toList();
    }

    /** The release a line's card speaks for: the newest one measured, which is not always the head. */
    public static VersionNumber spokenFor(CasesIndex.Line line) {
        return line.measured().orElse(line.version());
    }

    /**
     * The oldest release of the line from which every newer one is secure, or
     * empty when the newest is not. "On X or later" is only true when everything
     * above X is secure too.
     */
    public static Optional<VersionNumber> secureFrom(CasesIndex index, String line) {
        VersionNumber answer = null;
        var versions = new ArrayList<>(index.byLine().getOrDefault(line, List.of()));
        versions.sort(Comparator.reverseOrder());
        for (VersionNumber version : versions) {
            boolean secure = index.summaryOf(version).flatMap(CasesIndex.Summary::state).filter("secure"::equals)
                    .isPresent();
            if (!secure) {
                break;
            }
            answer = version;
        }
        return Optional.ofNullable(answer);
    }

    /**
     * The newest verdicts, one row per advisory. A verdict is written per package
     * and per range, so one advisory can arrive two or three times on the day it
     * is judged.
     */
    public static List<Assessment> recent(CasesIndex index) {
        var seen = new HashSet<String>();
        var rows = new ArrayList<Assessment>();
        for (Assessment verdict : index.recent()) {
            String key = verdict.cves().stream().findFirst().or(verdict::id).orElse(null);
            if (key == null || !seen.add(key)) {
                continue;
            }
            rows.add(verdict);
            if (rows.size() == RECENT_ROWS) {
                break;
            }
        }
        return rows;
    }

    /** The first sentence of what the verdict says it means, impact first. */
    public static Optional<String> firstSentence(Assessment verdict) {
        return verdict.impact().or(verdict::commentary).map(String::strip)
                .map(text -> SENTENCE_END.split(text, 2)[0]);
    }

    /** Whether no release of its line is newer than this one. */
    public static boolean isNewestOfItsLine(CasesIndex index, VersionAnswer measured) {
        String line = measured.line().orElse(measured.version().line());
        return index.byLine().getOrDefault(line, List.of()).stream().noneMatch(other -> other.compareTo(measured.version()) > 0);
    }
}

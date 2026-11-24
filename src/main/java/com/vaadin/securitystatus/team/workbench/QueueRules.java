package com.vaadin.securitystatus.team.workbench;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import org.jspecify.annotations.Nullable;

import com.vaadin.securitystatus.data.model.VerdictQueue.QueueEntry;
import com.vaadin.securitystatus.data.model.VerdictQueue.QueuedAdvisory;
import com.vaadin.securitystatus.data.model.VerdictQueue;

/**
 * The questions the workbench asks of {@code data/queue.json}, answered
 * without components: which series exist, what a row is worth on one of
 * them, what a verdict on a set of advisories would close, and which platform
 * branches would still go red.
 *
 * <p>
 * A verdict is keyed by a package with no version and answers for every
 * series at once, so narrowing to one series narrows what is shown and what a
 * row is worth there, never what a verdict covers, D-130.
 */
public final class QueueRules {

    /** The severities in the order every list here uses, worst first. */
    public static final List<String> SEVERITIES = List.of("critical", "high", "moderate", "low");

    private static final String IN_TRIAGE = "in_triage";

    private QueueRules() {
    }

    /**
     * One package as the queue shows it on a series, or on all of them: the
     * advisories that reach the series, how many of them carry no verdict
     * there, and how many reach other series only.
     */
    public record Row(QueueEntry entry, List<QueuedAdvisory> here, int closes, int elsewhere) {

        /** The package's name. */
        public String name() {
            return entry.packageName();
        }
    }

    /** What a verdict on some advisories of a package reaches, from the queue's own numbers. */
    public record Reach(int advisories, List<String> lines, int findings, int unjudged) {
    }

    /** Every series a package of the queue reaches, newest first. */
    public static List<String> series(VerdictQueue queue) {
        var seen = new TreeSet<String>(NEWEST_FIRST);
        queue.packages().forEach(entry -> seen.addAll(entry.lines()));
        return List.copyOf(seen);
    }

    /** The asked series when the queue knows it, else none: an unknown one narrows nothing. */
    public static Optional<String> series(VerdictQueue queue, Optional<String> asked) {
        return asked.filter(series(queue)::contains);
    }

    /**
     * The rows, ranked by what one verdict would close. With no series the
     * ranking is the queue's own, its {@code closes} and its order. On a series
     * the rows that do not reach it are dropped and the rest are ranked by what
     * would close there, the queue's order kept between equals.
     */
    public static List<Row> rows(VerdictQueue queue, Optional<String> line) {
        var rows = new ArrayList<Row>();
        for (QueueEntry entry : queue.packages()) {
            if (line.isEmpty()) {
                int closes = entry.closes() == null ? 0 : entry.closes();
                rows.add(new Row(entry, entry.cves(), closes, 0));
                continue;
            }
            List<QueuedAdvisory> here = entry.cves().stream().filter(advisory -> advisory.lines().contains(line.get()))
                    .toList();
            if (here.isEmpty()) {
                continue;
            }
            int closes = (int) here.stream().filter(advisory -> unjudged(advisory, line)).count();
            rows.add(new Row(entry, here, closes, entry.cves().size() - here.size()));
        }
        if (line.isPresent()) {
            rows.sort(Comparator.comparingInt(Row::closes).reversed());
        }
        return rows;
    }

    /** What is said about an advisory on a series, or over the row when none is named. */
    public static Optional<String> verdictOn(QueuedAdvisory advisory, Optional<String> line) {
        return line.map(advisory.byLine()::get).or(advisory::verdict);
    }

    /** True when nobody has judged the advisory, on the series or anywhere. */
    public static boolean unjudged(QueuedAdvisory advisory, Optional<String> line) {
        return verdictOn(advisory, line).map(IN_TRIAGE::equals).orElse(true);
    }

    /**
     * The advisories a verdict written from this row starts with: the ones
     * nobody has judged, which is what a new verdict is for, and every one of
     * them when all are answered, which makes the new verdict a supersession
     * of what answers them now, D-124.
     */
    public static List<String> prefill(Row row, Optional<String> line) {
        List<String> unjudged = row.entry().cves().stream().filter(advisory -> unjudged(advisory, line))
                .map(QueuedAdvisory::cve).toList();
        return unjudged.isEmpty() ? row.entry().cves().stream().map(QueuedAdvisory::cve).toList() : unjudged;
    }

    /**
     * The verdict's proposed name, {@code <ecosystem>-<package>[-<line>]-<date>}
     * from the package's purl, as the domain model asks; the person edits it.
     * With no purl there is nothing to name it after.
     */
    public static String proposedId(QueueEntry entry, Optional<String> line, LocalDate day) {
        Optional<String> purl = entry.purl();
        if (purl.isEmpty()) {
            return "";
        }
        var id = new StringBuilder(slug(purl.get()));
        line.ifPresent(series -> id.append('-').append(slug(series)));
        return id.append('-').append(day).toString();
    }

    static String slug(String text) {
        String bare = text.startsWith("pkg:") ? text.substring(4) : text;
        return bare.replaceAll("[^A-Za-z0-9]+", "-").replaceAll("^-+|-+$", "").toLowerCase(Locale.ROOT);
    }

    /**
     * What a verdict on these advisories of the package reaches: how many of
     * them the queue lists here, on which series, how many findings that is,
     * one per advisory and series, and how many of those carry no verdict now.
     */
    public static Reach reach(QueueEntry entry, List<String> ids) {
        Set<String> asked = new LinkedHashSet<>(ids);
        var lines = new TreeSet<String>(NEWEST_FIRST);
        int advisories = 0, findings = 0, unjudged = 0;
        for (QueuedAdvisory advisory : entry.cves()) {
            if (!asked.contains(advisory.cve())) {
                continue;
            }
            advisories++;
            for (String line : advisory.lines()) {
                lines.add(line);
                findings++;
                if (unjudged(advisory, Optional.of(line))) {
                    unjudged++;
                }
            }
        }
        return new Reach(advisories, List.copyOf(lines), findings, unjudged);
    }

    /** True for the verdicts that take a finding off a build's red list; a risk takes nothing off. */
    public static boolean silencing(String applicability) {
        return !applicability.isBlank() && !"risk".equals(applicability);
    }

    /**
     * The platform branches whose check list does not yet name every advisory
     * of the package, so their build stays red whatever this verdict says. A
     * branch whose list could not be read is no answer and is left out.
     */
    public static List<String> stillRed(QueueEntry entry) {
        var red = new TreeSet<String>(NEWEST_FIRST);
        for (Map.Entry<String, @Nullable List<String>> branch : entry.acknowledged().entrySet()) {
            List<String> listed = branch.getValue();
            if (listed != null && entry.cves().stream().anyMatch(advisory -> !listed.contains(advisory.cve()))) {
                red.add(branch.getKey());
            }
        }
        return List.copyOf(red);
    }

    /** Findings and how many carry no verdict, by severity worst first and by series newest first. */
    public record Summary(int packages, int findings, int unjudged, Map<String, int[]> bySeverity,
            Map<String, int[]> byLine) {
    }

    /**
     * The counts over the rows shown: a finding is one advisory of one package,
     * counted once per series it reaches in the line table. A severity the data
     * does not carry is counted in the total and in no severity row.
     */
    public static Summary summary(List<Row> rows, Optional<String> line) {
        var bySeverity = new LinkedHashMap<String, int[]>();
        var byLine = new java.util.TreeMap<String, int[]>(NEWEST_FIRST);
        int findings = 0, unjudged = 0;
        for (Row row : rows) {
            for (QueuedAdvisory advisory : row.here()) {
                boolean open = unjudged(advisory, line);
                findings++;
                if (open) {
                    unjudged++;
                }
                advisory.severity().map(word -> word.toLowerCase(Locale.ROOT)).filter(SEVERITIES::contains)
                        .ifPresent(word -> count(bySeverity.computeIfAbsent(word, key -> new int[2]), open));
                for (String series : advisory.lines()) {
                    if (line.isEmpty() || line.get().equals(series)) {
                        count(byLine.computeIfAbsent(series, key -> new int[2]),
                                unjudged(advisory, Optional.of(series)));
                    }
                }
            }
        }
        var ordered = new LinkedHashMap<String, int[]>();
        SEVERITIES.stream().filter(bySeverity::containsKey).forEach(word -> ordered.put(word, bySeverity.get(word)));
        return new Summary(rows.size(), findings, unjudged, ordered, new LinkedHashMap<>(byLine));
    }

    private static void count(int[] counts, boolean open) {
        counts[0]++;
        if (open) {
            counts[1]++;
        }
    }

    /** Series as {@code 25.3}, compared by their numbers, newest first. */
    public static final Comparator<String> NEWEST_FIRST = Comparator.<String, Integer>comparing(series -> part(series, 0))
            .thenComparing(series -> part(series, 1)).reversed().thenComparing(Comparator.naturalOrder());

    private static int part(String series, int index) {
        String[] parts = series.split("[.-]");
        try {
            return parts.length > index ? Integer.parseInt(parts[index]) : 0;
        } catch (NumberFormatException notANumber) {
            return 0;
        }
    }
}

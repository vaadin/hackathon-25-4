package com.vaadin.securitystatus.customer;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.vaadin.securitystatus.data.model.CasesIndex;
import com.vaadin.securitystatus.data.model.ScanIndex;
import com.vaadin.securitystatus.data.model.VersionAnswer;

/**
 * A scanner report against the measurements, in the design's three buckets
 * with an owner and an action per row, {@code 01-domain-model.md}. Nothing
 * here is computed that the published data does not carry: the bucket is read
 * from the version document's own lists, and an advisory or a coordinate the
 * measurements do not cover is never called secure.
 */
public final class Triage {

    /** The design's three words, and no fourth. */
    public enum Bucket {
        SECURE, FIX_AVAILABLE, UNRESOLVED
    }

    /** Who can change the vulnerable version, rule 6.11.14. */
    public enum Owner {
        VAADIN, SPRING_BOOT, LIBRARY, YOURS
    }

    /** Why a row is waiting on Vaadin's judgement, which the Verdict cell says as "Not assessed yet". */
    public enum Pending {
        /** An advisory on the reader's version that nobody has judged. */
        REVIEW,
        /** An advisory we have no measurement of, on a coordinate Vaadin brings. */
        UNMEASURED
    }

    /** What the reader does about a row, named for who moves, rule 6.11.20. */
    public enum Action {
        /** Argument: the release. */
        UPGRADE,
        UNDER_REVIEW,
        /** Arguments: the component, the reader's version. */
        NEWER_LIBRARY,
        AWAITING_VAADIN,
        /** Argument: the component. */
        SPRING_BOOT,
        DISCONTINUED,
        RULED_OUT,
        FROM_BUILD,
        /** Argument: the reader's version. */
        NOT_REPORTED,
        UNMEASURED_ADVISORY,
        /** Argument: the component. */
        NOT_OURS,
        NAME_VERSION,
        /** Argument: the version nobody measured. */
        VERSION_UNMEASURED;

        public String key() {
            return "scan.action." + name();
        }
    }

    /**
     * One reported advisory on one coordinate, answered.
     *
     * @param verdict the applicability word the measurements give it, on this
     *                coordinate when they speak for it, else for the advisory
     */
    public record Row(String advisory, String coordinate, Optional<String> severity, Bucket bucket, Owner owner,
            Optional<Pending> pending, Optional<String> verdict, Action action, List<String> arguments) {

        public Row {
            arguments = List.copyOf(arguments);
        }

        public String component() {
            return ReportParser.componentName(coordinate);
        }
    }

    /** The whole triage: every row, the counts per bucket. */
    public record Result(List<Row> rows, Map<Bucket, Integer> counts) {

        public Result {
            rows = List.copyOf(rows);
            counts = Map.copyOf(counts);
        }

        public int count(Bucket bucket) {
            return counts.getOrDefault(bucket, 0);
        }

        /** What the reader may have to act on: everything not already secure. */
        public List<Row> open() {
            return rows.stream().filter(row -> row.bucket() != Bucket.SECURE).toList();
        }
    }

    private Triage() {
    }

    /**
     * @param version the reader's version document, empty when no version is
     *                named or nobody measured it
     * @param asked   the version the reader named, measured or not
     */
    public static Result of(ScannerReport report, ScanIndex scan, Optional<VersionAnswer> version,
            Optional<String> asked) {
        var rows = new ArrayList<Row>();
        Set<String> seen = new HashSet<>();
        for (ScannerReport.Finding finding : report.findings()) {
            String id = finding.ids().stream().filter(scan.advisories()::containsKey).findFirst()
                    .orElse(finding.ids().getFirst());
            place(rows, seen, scan, version, asked, id, finding.coordinate(), finding.severity());
        }
        // A bill of materials carries no findings: the measurements supply them,
        // every advisory seen on a coordinate the reader ships.
        for (String component : report.components()) {
            Map<String, String> known = scan.coordinates().get(ReportParser.bare(component));
            if (known != null) {
                known.keySet().forEach(id -> place(rows, seen, scan, version, asked, id, component,
                        Optional.ofNullable(scan.advisories().get(id)).flatMap(ScanIndex.Advisory::severity)));
            }
        }
        var counts = new EnumMap<Bucket, Integer>(Bucket.class);
        for (Bucket bucket : Bucket.values()) {
            counts.put(bucket, (int) rows.stream().filter(row -> row.bucket() == bucket).count());
        }
        return new Result(rows, counts);
    }

    private static void place(List<Row> rows, Set<String> seen, ScanIndex scan, Optional<VersionAnswer> version,
            Optional<String> asked, String id, String coordinate, Optional<String> severity) {
        if (!seen.add(id + '\u0000' + coordinate)) {
            return;
        }
        ScanIndex.Advisory known = scan.advisories().get(id);
        Map<String, String> onCoordinate = scan.coordinates().get(ReportParser.bare(coordinate));
        String component = ReportParser.componentName(coordinate);
        if (known == null) {
            boolean ours = onCoordinate != null;
            rows.add(new Row(id, coordinate, severity, Bucket.UNRESOLVED, ours ? Owner.VAADIN : Owner.YOURS,
                    ours ? Optional.of(Pending.UNMEASURED) : Optional.empty(), Optional.empty(),
                    ours ? Action.UNMEASURED_ADVISORY : Action.NOT_OURS, ours ? List.of() : List.of(component)));
            return;
        }
        Optional<String> verdict = Optional.ofNullable(onCoordinate).map(verdicts -> verdicts.get(id))
                .or(known::applicability);
        if (version.isEmpty()) {
            Owner owner = owner(id, known, null);
            rows.add(new Row(id, coordinate, severity, Bucket.UNRESOLVED, owner, Optional.empty(), verdict,
                    asked.isPresent() ? Action.VERSION_UNMEASURED : Action.NAME_VERSION,
                    asked.map(List::of).orElse(List.of())));
            return;
        }
        VersionAnswer answer = version.get();
        Owner owner = owner(id, known, answer);
        String on = answer.version().toString();
        if (answer.risk().contains(id) || answer.triage().contains(id)) {
            boolean fixable = answer.fixable().contains(id);
            Optional<String> to = answer.upgrade().flatMap(VersionAnswer.Upgrade::to).map(Object::toString);
            if (fixable && to.isPresent()) {
                rows.add(new Row(id, coordinate, severity, Bucket.FIX_AVAILABLE, owner, Optional.empty(), verdict,
                        Action.UPGRADE, List.of(to.get())));
            } else if (answer.triage().contains(id)) {
                rows.add(new Row(id, coordinate, severity, Bucket.UNRESOLVED, owner, Optional.of(Pending.REVIEW),
                        verdict, Action.UNDER_REVIEW, List.of()));
            } else if (owner == Owner.SPRING_BOOT) {
                rows.add(new Row(id, coordinate, severity, Bucket.UNRESOLVED, owner, Optional.empty(), verdict,
                        Action.SPRING_BOOT, List.of(component)));
            } else if (owner == Owner.LIBRARY) {
                rows.add(new Row(id, coordinate, severity, Bucket.UNRESOLVED, owner, Optional.empty(), verdict,
                        Action.NEWER_LIBRARY, List.of(component, on)));
            } else {
                rows.add(new Row(id, coordinate, severity, Bucket.UNRESOLVED, owner, Optional.empty(), verdict,
                        Action.AWAITING_VAADIN, List.of()));
            }
            return;
        }
        if (answer.theirs().contains(id) || answer.springPinned().contains(id)) {
            rows.add(new Row(id, coordinate, severity, Bucket.UNRESOLVED, owner, Optional.empty(), verdict,
                    Action.SPRING_BOOT, List.of(component)));
            return;
        }
        if (answer.discontinued().contains(id)) {
            // Real on an application that declares the product, so never secure, and
            // no Vaadin release moves it.
            rows.add(new Row(id, coordinate, severity, Bucket.UNRESOLVED, owner, Optional.empty(), verdict,
                    Action.DISCONTINUED, List.of()));
            return;
        }
        Action secure = answer.ruledOut().contains(id) ? Action.RULED_OUT
                : answer.fromBuild().contains(id) ? Action.FROM_BUILD : Action.NOT_REPORTED;
        rows.add(new Row(id, coordinate, severity, Bucket.SECURE, owner, Optional.empty(), verdict, secure,
                secure == Action.NOT_REPORTED ? List.of(on) : List.of()));
    }

    /**
     * Whose release carries the fix. Spring Boot only where Spring Boot can fix
     * it: what the measurements put in {@code theirs} or {@code spring_pinned}
     * for the reader's version, or, before a version is named, an advisory whose
     * only origin is Spring Boot. Everything else fixed upstream is the library
     * that carries it.
     */
    static Owner owner(String id, ScanIndex.Advisory known, VersionAnswer answer) {
        boolean spring = answer != null ? answer.theirs().contains(id) || answer.springPinned().contains(id)
                : known.origins().equals(List.of("spring-boot"));
        if (spring) {
            return Owner.SPRING_BOOT;
        }
        return known.fixOwner().filter("upstream"::equals).isPresent() ? Owner.LIBRARY : Owner.VAADIN;
    }

    /**
     * How much of the file the measurements can answer for, counted by
     * coordinate without its version: what the file names, and how many of
     * those are coordinates an advisory has been seen on in a measured release.
     *
     * @param inventory true for a bill of materials, which lists the whole tree;
     *                  the other formats list only what carries a finding
     */
    public record Scope(boolean inventory, int listed, int known, List<String> unknownExamples) {

        public int unknown() {
            return listed - known;
        }
    }

    public static Scope scope(ScannerReport report, ScanIndex scan) {
        var listed = new java.util.LinkedHashSet<String>();
        report.components().forEach(component -> listed.add(ReportParser.bare(component)));
        report.findings().forEach(finding -> listed.add(ReportParser.bare(finding.coordinate())));
        List<String> unknown = listed.stream().filter(bare -> !scan.coordinates().containsKey(bare)).toList();
        return new Scope(!report.components().isEmpty(), listed.size(), listed.size() - unknown.size(),
                unknown.stream().limit(3).map(ReportParser::componentName).toList());
    }

    /** The verdict a row names beside its action, only where the verdict is what settled it. */
    public static Optional<String> settledVerdict(Row row, CasesIndex index) {
        // A secure row names the verdict only when the verdict settled it. A row secure
        // because the version does not report it says so in its action, and the
        // catalogue's word for other releases beside it would contradict it, D-198.
        boolean named = row.action() == Action.RULED_OUT || row.action() == Action.FROM_BUILD
                || row.action() == Action.VERSION_UNMEASURED;
        return row.verdict().filter(verdict -> named).map(index.labels()::get);
    }
}

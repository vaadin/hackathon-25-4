package com.vaadin.securitystatus.team.explorer;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.vaadin.securitystatus.data.ExportKind;
import com.vaadin.securitystatus.data.VersionNumber;
import com.vaadin.securitystatus.data.model.ExplorerFront.Line;
import com.vaadin.securitystatus.data.model.ExplorerFront;
import com.vaadin.securitystatus.data.model.ExplorerVersion;
import com.vaadin.securitystatus.data.model.SpringBootLatest;

/**
 * The release page's questions answered without components: which line a
 * release belongs to, its findings in the explorer's shape, what updating
 * within the line removes, which files exist, and where its Spring Boot can
 * move to. Reads {@code data/front.json} and the release's own record.
 */
public final class ReleaseRules {

    /** How many removed findings are named before the rest is counted, rule 6.3.9. */
    public static final int NAMED = 6;

    /** How many other measured releases the not measured answer links before counting the rest. */
    public static final int OTHERS = 12;

    /** Where a release's Spring Boot can go within its major: the newest patch and a later minor. */
    public record SpringTargets(VersionNumber from, Optional<VersionNumber> patch, Optional<VersionNumber> minor) {
    }

    private ReleaseRules() {
    }

    /** The line a release belongs to, by its minor, when the front door follows it. */
    public static Optional<Line> lineOf(ExplorerFront front, VersionNumber version) {
        return front.lines().stream().filter(line -> line.line().equals(version.line())).findFirst();
    }

    /**
     * The followed line of the same major, for a release on a line nobody
     * follows any more. The front door lists the lines newest first.
     */
    public static Optional<Line> sameMajor(ExplorerFront front, VersionNumber version) {
        String major = major(version.line());
        return front.lines().stream().filter(line -> major(line.line()).equals(major)).findFirst();
    }

    /** The major of a line, {@code 24} for {@code 24.9}. */
    public static String major(String line) {
        int dot = line.indexOf('.');
        return dot < 0 ? line : line.substring(0, dot);
    }

    /** The measured releases of a line other than the given one, in the order the front door lists them. */
    public static List<VersionNumber> others(Line line, VersionNumber except) {
        return line.releases().stream().filter(release -> !release.equals(except)).toList();
    }

    /**
     * A record's findings in the shape the explorer groups and counts, so the
     * release card and the explorer's row cannot disagree. Products are named
     * in the record and numbered in the front door's registry.
     */
    static List<ExplorerFront.Finding> findings(ExplorerVersion record, List<String> moduleNames) {
        var findings = new ArrayList<ExplorerFront.Finding>();
        for (ExplorerVersion.Finding finding : record.findings()) {
            List<Integer> modules = finding.modules().stream().map(moduleNames::indexOf).filter(index -> index >= 0)
                    .toList();
            findings.add(new ExplorerFront.Finding(finding.cve(), ecosystem(finding), modules, finding.owner(),
                    finding.severity(), finding.verdict(), finding.bundledIn(), finding.springPins()));
        }
        return findings;
    }

    /**
     * The release read as a series, so the explorer's rules answer it, D-216:
     * its own findings, its two shapes, its Spring Boot and its line's
     * releases. The products that need Spring are the front door's, not the
     * record's, so they are not read here: the prototype's release page reads
     * the record alone and rings a Spring borne finding through every product
     * on its route, D-216.
     */
    public static Line asLine(ExplorerFront front, ExplorerVersion record) {
        String name = record.line().orElse(record.version().line());
        Optional<Line> own = front.lines().stream().filter(line -> line.line().equals(name)).findFirst();
        return new Line(name, record.version(), Optional.empty(), own.map(Line::releases).orElse(List.of()),
                findings(record, front.moduleNames()), record.scannedAt(), record.scannerFiles(), record.springBoot(),
                record.springBootDeclared(), List.of(), record.withoutSpring(),
                record.noSpring(), Optional.empty());
    }

    /** Whether the release was measured both with and without Spring, so the starter is a control on it. */
    public static boolean measuredWithoutSpring(ExplorerVersion record) {
        return !record.withoutSpring().isEmpty() && !Boolean.TRUE.equals(record.noSpring());
    }

    /**
     * The ecosystem of a finding of a record, which names none: a frontend
     * one when any package it arrives in is an npm or javascript coordinate,
     * or a bare name with no group, the prototype's test for the frontend
     * bundle; otherwise none is said.
     */
    static Optional<String> ecosystem(ExplorerVersion.Finding finding) {
        boolean frontend = finding.packages().stream().anyMatch(found -> {
            String name = found.packageName() == null ? "" : found.packageName();
            return name.startsWith("pkg:npm") || name.startsWith("pkg:javascript") || !name.contains(":");
        });
        return frontend ? Optional.of("npm") : Optional.empty();
    }

    /**
     * What updating to the line's newest release removes, D-218: the reader's
     * own findings, Vaadin's and routed to a product that is on, whose {@code
     * fixed_in} lies after this release and no later than the newest, in the
     * record's order. Empty on the newest itself.
     */
    public static List<String> removedBy(ExplorerVersion record, Line line, ExplorerAnswer.Shape shape) {
        Optional<VersionNumber> newest = record.newest().filter(to -> to.compareTo(record.version()) > 0);
        if (newest.isEmpty()) {
            return List.of();
        }
        var fixed = new java.util.HashMap<String, VersionNumber>();
        record.findings().forEach(finding -> finding.fixedIn().ifPresent(at -> fixed.putIfAbsent(finding.cve(), at)));
        var removed = new ArrayList<String>();
        for (ExplorerFront.Finding finding : ExplorerAnswer.findingsOf(line, shape, ExplorerAnswer.Asked.nothing())) {
            VersionNumber at = fixed.get(finding.cve());
            if (at != null && at.compareTo(record.version()) > 0 && at.compareTo(newest.get()) <= 0
                    && ExplorerAnswer.mine(finding, shape, line) && !removed.contains(finding.cve())) {
                removed.add(finding.cve());
            }
        }
        return removed;
    }

    /**
     * The files a measured release has: its record and its CSV always, the
     * VEX and the three scanner files only where the record says they were
     * produced, rule 6.3.14.
     */
    public static Set<ExportKind> files(@org.jspecify.annotations.Nullable Boolean scannerFiles) {
        return Boolean.TRUE.equals(scannerFiles) ? EnumSet.allOf(ExportKind.class)
                : EnumSet.of(ExportKind.JSON, ExportKind.CSV);
    }

    /**
     * Where the Spring Boot a release declares can move, read from {@code
     * spring_boot_latest}: asked of what the release declares, or of what the
     * measurement pinned when it declares nothing. Empty when the front door
     * holds no entry for it, which means nobody could look.
     */
    public static Optional<SpringTargets> springTargets(ExplorerFront front, ExplorerVersion record) {
        Optional<VersionNumber> from = record.springBootDeclared().or(record::springBoot);
        if (from.isEmpty()) {
            return Optional.empty();
        }
        SpringBootLatest latest = front.springBootLatest().get(from.get().toString());
        if (latest == null) {
            return Optional.empty();
        }
        return Optional.of(new SpringTargets(from.get(), latest.patch(), latest.minor()));
    }
}

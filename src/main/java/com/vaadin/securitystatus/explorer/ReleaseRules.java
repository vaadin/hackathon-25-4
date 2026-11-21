package com.vaadin.securitystatus.explorer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.vaadin.securitystatus.data.ExportKind;
import com.vaadin.securitystatus.data.VersionNumber;
import com.vaadin.securitystatus.data.model.ExplorerFront;
import com.vaadin.securitystatus.data.model.ExplorerFront.Line;
import com.vaadin.securitystatus.data.model.ExplorerVersion;
import com.vaadin.securitystatus.data.model.SpringBootLatest;

/**
 * The release page's questions answered without components: which line a
 * release belongs to, its findings in the explorer's shape, what updating
 * within the line removes, which files exist, and where its Spring Boot can
 * move to. Reads {@code data/front.json} and the release's own record.
 */
final class ReleaseRules {

    /** How many removed findings are named before the rest is counted, rule 6.3.9. */
    static final int NAMED = 6;

    /** How many other measured releases the not measured answer links before counting the rest. */
    static final int OTHERS = 12;

    private static final Comparator<ExplorerVersion.Finding> WORST_FIRST = Comparator
            .<ExplorerVersion.Finding>comparingInt(finding -> ExplorerRules.severityRank(finding.severity()))
            .thenComparing(ExplorerVersion.Finding::cve);

    /** Where a release's Spring Boot can go within its major: the newest patch and a later minor. */
    record SpringTargets(VersionNumber from, Optional<VersionNumber> patch, Optional<VersionNumber> minor) {
    }

    private ReleaseRules() {
    }

    /** The line a release belongs to, by its minor, when the front door follows it. */
    static Optional<Line> lineOf(ExplorerFront front, VersionNumber version) {
        return front.lines().stream().filter(line -> line.line().equals(version.line())).findFirst();
    }

    /**
     * The followed line of the same major, for a release on a line nobody
     * follows any more. The front door lists the lines newest first.
     */
    static Optional<Line> sameMajor(ExplorerFront front, VersionNumber version) {
        String major = major(version.line());
        return front.lines().stream().filter(line -> major(line.line()).equals(major)).findFirst();
    }

    /** The major of a line, {@code 24} for {@code 24.9}. */
    static String major(String line) {
        int dot = line.indexOf('.');
        return dot < 0 ? line : line.substring(0, dot);
    }

    /** The measured releases of a line other than the given one, in the order the front door lists them. */
    static List<VersionNumber> others(Line line, VersionNumber except) {
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
            findings.add(new ExplorerFront.Finding(finding.cve(), Optional.empty(), modules, finding.owner(),
                    finding.severity(), finding.verdict(), finding.bundledIn(), finding.springPins()));
        }
        return findings;
    }

    /**
     * What updating to the line's newest release removes: the findings a
     * Vaadin release can move whose {@code fixed_in} lies after this release
     * and no later than the newest, worst first. Empty on the newest itself.
     */
    static List<ExplorerVersion.Finding> removedBy(ExplorerVersion record) {
        Optional<VersionNumber> newest = record.newest().filter(to -> to.compareTo(record.version()) > 0);
        if (newest.isEmpty()) {
            return List.of();
        }
        var removed = new ArrayList<ExplorerVersion.Finding>();
        for (ExplorerVersion.Finding finding : record.findings()) {
            if (finding.owner().filter("spring-boot"::equals).isPresent()) {
                continue;
            }
            finding.fixedIn().filter(fixed -> fixed.compareTo(record.version()) > 0)
                    .filter(fixed -> fixed.compareTo(newest.get()) <= 0).ifPresent(fixed -> removed.add(finding));
        }
        removed.sort(WORST_FIRST);
        return removed;
    }

    /**
     * The files a measured release has: its record and its CSV always, the
     * VEX and the three scanner files only where the record says they were
     * produced, rule 6.3.14.
     */
    static Set<ExportKind> files(@org.jspecify.annotations.Nullable Boolean scannerFiles) {
        return Boolean.TRUE.equals(scannerFiles) ? EnumSet.allOf(ExportKind.class)
                : EnumSet.of(ExportKind.JSON, ExportKind.CSV);
    }

    /**
     * Where the Spring Boot a release declares can move, read from {@code
     * spring_boot_latest}: asked of what the release declares, or of what the
     * measurement pinned when it declares nothing. Empty when the front door
     * holds no entry for it, which means nobody could look.
     */
    static Optional<SpringTargets> springTargets(ExplorerFront front, ExplorerVersion record) {
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

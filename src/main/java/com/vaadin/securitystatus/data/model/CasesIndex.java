package com.vaadin.securitystatus.data.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.vaadin.securitystatus.data.VersionNumber;

/** {@code cases/index.json}: the lines, the catalogue and the freshness fact. */
public record CasesIndex(Instant generatedAt, Map<String, String> labels, Map<String, String> originLabels,
        Optional<Counts> counts, Optional<LineRule> lineRule, List<Line> lines, Map<String, String> releaseRepos,
        Map<String, SpringBootLatest> springBootLatest, Map<String, SpringBootSupply> springBootSupply,
        Map<String, List<VersionNumber>> byLine, Map<String, Summary> summary,
        Map<String, AdvisoryCatalogEntry> advisories, Map<String, Map<String, FixedIn>> fixedIn,
        List<Assessment> recent) {

    public CasesIndex {
        labels = Wire.map(labels);
        originLabels = Wire.map(originLabels);
        counts = Wire.value(counts);
        lineRule = Wire.value(lineRule);
        lines = Wire.list(lines);
        releaseRepos = Wire.map(releaseRepos);
        springBootLatest = Wire.map(springBootLatest);
        springBootSupply = Wire.map(springBootSupply);
        byLine = Wire.map(byLine);
        summary = Wire.map(summary);
        advisories = Wire.map(advisories);
        fixedIn = Wire.map(fixedIn);
        recent = Wire.list(recent);
    }

    /** The summary entry of one release, when the index has one. */
    public Optional<Summary> summaryOf(VersionNumber version) {
        return Optional.ofNullable(summary.get(version.toString()));
    }

    public record Counts(@Nullable Integer cves, Map<String, Integer> byApplicability,
            Map<String, Integer> bySeverity, Map<String, Integer> byEcosystem, @Nullable Integer current,
            @Nullable Integer historical, @Nullable Integer appearedLater, @Nullable Integer unknown,
            @Nullable Integer linesScanned, @Nullable Integer linesTotal) {

        public Counts {
            byApplicability = Wire.map(byApplicability);
            bySeverity = Wire.map(bySeverity);
            byEcosystem = Wire.map(byEcosystem);
        }
    }

    public record LineRule(Optional<String> rule, @Nullable Integer windowDays, Optional<LocalDate> cutoff,
            List<String> followed, List<NotFollowed> notFollowed) {

        public LineRule {
            rule = Wire.text(rule);
            cutoff = Wire.value(cutoff);
            followed = Wire.list(followed);
            notFollowed = Wire.list(notFollowed);
        }

        public record NotFollowed(String line, Optional<VersionNumber> version, @Nullable Integer ageDays,
                Optional<String> reason) {

            public NotFollowed {
                version = Wire.value(version);
                reason = Wire.text(reason);
            }
        }
    }

    /** One maintained minor and its newest measurement. */
    public record Line(String line, VersionNumber version, Optional<String> kind, Optional<String> status,
            Optional<String> reason, @Nullable Integer findings, Optional<String> source,
            Optional<OffsetDateTime> scannedAt, Map<String, String> scannerVersions,
            Map<String, Object> skippedScanners, Map<String, Integer> bySeverity, Map<String, Integer> byEcosystem,
            Optional<VersionNumber> measured, Optional<OffsetDateTime> measuredAt, Optional<Support> support) {

        public Line {
            kind = Wire.text(kind);
            status = Wire.text(status);
            reason = Wire.text(reason);
            source = Wire.text(source);
            scannedAt = Wire.value(scannedAt);
            scannerVersions = Wire.map(scannerVersions);
            skippedScanners = Wire.map(skippedScanners);
            bySeverity = Wire.map(bySeverity);
            byEcosystem = Wire.map(byEcosystem);
            measured = Wire.value(measured);
            measuredAt = Wire.value(measuredAt);
            support = Wire.value(support);
        }

        public boolean isPrerelease() {
            return kind.filter("prerelease"::equals).isPresent();
        }
    }

    /** The support tier of a line, and the sentence quoting where that came from. */
    public record Support(Optional<String> major, Optional<String> tier, Optional<String> label,
            Optional<String> freeUntil, Optional<String> freeEnded, Optional<VersionNumber> subscriptionFrom,
            Optional<String> extendedUntil, Optional<String> reason, Optional<LocalDate> decidedAt) {

        public Support {
            major = Wire.text(major);
            tier = Wire.text(tier);
            label = Wire.text(label);
            freeUntil = Wire.text(freeUntil);
            freeEnded = Wire.text(freeEnded);
            subscriptionFrom = Wire.value(subscriptionFrom);
            extendedUntil = Wire.text(extendedUntil);
            reason = Wire.text(reason);
            decidedAt = Wire.value(decidedAt);
        }
    }

    /** One release in short: its state, its bucket counts and what to move to. */
    public record Summary(Optional<String> line, Optional<String> state, Optional<OffsetDateTime> scannedAt,
            @Nullable Integer risk, @Nullable Integer triage, @Nullable Integer ruledOut, @Nullable Integer theirs,
            @Nullable Integer theirsRisk, @Nullable Integer springPinned, @Nullable Integer discontinued,
            @Nullable Integer fromBuild, Optional<VersionNumber> upgradeTo) {

        public Summary {
            line = Wire.text(line);
            state = Wire.text(state);
            scannedAt = Wire.value(scannedAt);
            upgradeTo = Wire.value(upgradeTo);
        }
    }

    /** An advisory in the short form the lists use. */
    public record AdvisoryCatalogEntry(String cve, Optional<String> severity, Optional<String> applicability,
            Optional<String> fixOwner, @Nullable Boolean vaadinAdvisory, @Nullable Boolean current,
            List<String> modules, List<String> packages, List<String> origins) {

        public AdvisoryCatalogEntry {
            severity = Wire.text(severity);
            applicability = Wire.text(applicability);
            fixOwner = Wire.text(fixOwner);
            modules = Wire.list(modules);
            packages = Wire.list(packages);
            origins = Wire.list(origins);
        }
    }

    public record FixedIn(Optional<VersionNumber> fixedIn, Optional<VersionNumber> fixedInPrerelease,
            Optional<VersionNumber> fixedInEnterprise) {

        public FixedIn {
            fixedIn = Wire.value(fixedIn);
            fixedInPrerelease = Wire.value(fixedInPrerelease);
            fixedInEnterprise = Wire.value(fixedInEnterprise);
        }
    }
}

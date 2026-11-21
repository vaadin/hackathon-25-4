package com.vaadin.securitystatus.data.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vaadin.securitystatus.data.VersionNumber;

/** {@code data/queue.json}: every package with advisories, and how much one verdict on it would close. */
public record VerdictQueue(Optional<Instant> generatedAt, Map<String, VersionNumber> heads,
        Map<String, Assessment> assessments, List<QueueEntry> packages) {

    public VerdictQueue {
        generatedAt = Wire.value(generatedAt);
        heads = Wire.map(heads);
        assessments = Wire.map(assessments);
        packages = Wire.list(packages);
    }

    public record QueueEntry(@JsonProperty("package") String packageName, Optional<String> purl,
            Optional<String> ecosystem, List<QueuedAdvisory> cves, List<String> cpes, List<String> lines,
            @Nullable Integer closes, List<Route> routes, Map<String, List<String>> resolved,
            Optional<String> origin, List<String> scanners, List<String> sources,
            Map<String, @Nullable List<String>> acknowledged) {

        public QueueEntry {
            purl = Wire.text(purl);
            ecosystem = Wire.text(ecosystem);
            cves = Wire.list(cves);
            cpes = Wire.list(cpes);
            lines = Wire.list(lines);
            routes = Wire.list(routes);
            resolved = Wire.map(resolved);
            origin = Wire.text(origin);
            scanners = Wire.list(scanners);
            sources = Wire.list(sources);
            acknowledged = Wire.map(acknowledged);
        }
    }

    public record QueuedAdvisory(String cve, Optional<String> severity, Optional<String> summary,
            Optional<String> verdict, List<String> lines, Map<String, String> byLine,
            Map<String, String> saidByLine, Map<String, String> fixedUpstream, List<String> assessments,
            List<String> absentAbout, Optional<String> absentReason, List<String> absentUpstream,
            @Nullable Boolean gitOnly) {

        public QueuedAdvisory {
            severity = Wire.text(severity);
            summary = Wire.text(summary);
            verdict = Wire.text(verdict);
            lines = Wire.list(lines);
            byLine = Wire.map(byLine);
            saidByLine = Wire.map(saidByLine);
            fixedUpstream = Wire.map(fixedUpstream);
            assessments = Wire.list(assessments);
            absentAbout = Wire.list(absentAbout);
            absentReason = Wire.text(absentReason);
            absentUpstream = Wire.list(absentUpstream);
        }
    }

    public record Route(Optional<String> entry, Optional<String> declaredBy, List<String> chain,
            @Nullable Integer hops) {

        public Route {
            entry = Wire.text(entry);
            declaredBy = Wire.text(declaredBy);
            chain = Wire.list(chain);
        }
    }
}

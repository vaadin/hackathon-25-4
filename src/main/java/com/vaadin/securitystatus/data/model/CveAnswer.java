package com.vaadin.securitystatus.data.model;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.vaadin.securitystatus.data.VersionNumber;

/** {@code cases/cve/<id>.json}: everything the by CVE screen needs for one advisory. */
public record CveAnswer(String cve, Optional<String> severity, Optional<String> summary, List<String> aliases,
        Optional<String> applicability, Optional<String> fixOwner, @Nullable Boolean vaadinAdvisory,
        List<String> modules, List<String> packages, List<String> origins, Optional<OffsetDateTime> firstSeen,
        @Nullable Boolean current, Map<String, Coverage> coverage, Map<String, List<Arrival>> arrives,
        Map<String, Assessment> assessments, Map<String, Verdict> onVersions) {

    public CveAnswer {
        severity = Wire.text(severity);
        summary = Wire.text(summary);
        aliases = Wire.list(aliases);
        applicability = Wire.text(applicability);
        fixOwner = Wire.text(fixOwner);
        modules = Wire.list(modules);
        packages = Wire.list(packages);
        origins = Wire.list(origins);
        firstSeen = Wire.value(firstSeen);
        coverage = Wire.map(coverage);
        arrives = Wire.map(arrives);
        assessments = Wire.map(assessments);
        onVersions = Wire.map(onVersions);
    }

    /** How one maintained line stands against this advisory. */
    public record Coverage(Optional<VersionNumber> version, Optional<String> status,
            Optional<VersionNumber> fixedIn, Optional<VersionNumber> fixedInPrerelease,
            Optional<VersionNumber> fixedInEnterprise, Optional<VersionNumber> affectedFrom,
            Optional<VersionNumber> affectedTo, @Nullable Integer affectedCount, Optional<String> reason,
            Optional<FixMeans> fixMeans) {

        public Coverage {
            version = Wire.value(version);
            status = Wire.text(status);
            fixedIn = Wire.value(fixedIn);
            fixedInPrerelease = Wire.value(fixedInPrerelease);
            fixedInEnterprise = Wire.value(fixedInEnterprise);
            affectedFrom = Wire.value(affectedFrom);
            affectedTo = Wire.value(affectedTo);
            reason = Wire.text(reason);
            fixMeans = Wire.value(fixMeans);
        }
    }

    /** What the fix actually was. */
    public record FixMeans(Optional<String> kind, List<Change> changes) {

        public FixMeans {
            kind = Wire.text(kind);
            changes = Wire.list(changes);
        }

        public record Change(@com.fasterxml.jackson.annotation.JsonProperty("package") Optional<String> packageName,
                List<String> from, List<String> to) {

            public Change {
                packageName = Wire.text(packageName);
                from = Wire.list(from);
                to = Wire.list(to);
            }
        }
    }

    /** How the vulnerable package reaches a line. */
    public record Arrival(@com.fasterxml.jackson.annotation.JsonProperty("package") Optional<String> packageName,
            Optional<String> version, Optional<String> bundledIn, Optional<String> fixedUpstream) {

        public Arrival {
            packageName = Wire.text(packageName);
            version = Wire.text(version);
            bundledIn = Wire.text(bundledIn);
            fixedUpstream = Wire.text(fixedUpstream);
        }
    }
}

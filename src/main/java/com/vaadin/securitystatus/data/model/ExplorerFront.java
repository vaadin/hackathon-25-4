package com.vaadin.securitystatus.data.model;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.vaadin.securitystatus.data.VersionNumber;

/** {@code data/front.json}: the explorer's front door, one entry per maintained line. */
public record ExplorerFront(Optional<Instant> generatedAt, List<Line> lines, List<String> moduleNames,
        Optional<Profile> profile, Map<String, List<String>> packages, List<History> history,
        Map<String, SpringBootLatest> springBootLatest, Map<String, SpringBootSupply> springBootSupply,
        Map<String, SpringBootTried> springBootTried) {

    public ExplorerFront {
        generatedAt = Wire.value(generatedAt);
        lines = Wire.list(lines);
        moduleNames = Wire.list(moduleNames);
        profile = Wire.value(profile);
        packages = Wire.map(packages);
        history = Wire.list(history);
        springBootLatest = Wire.map(springBootLatest);
        springBootSupply = Wire.map(springBootSupply);
        springBootTried = Wire.map(springBootTried);
    }

    public record Line(String line, VersionNumber version, Optional<String> kind, List<VersionNumber> releases,
            List<Finding> findings, Optional<OffsetDateTime> scannedAt, @Nullable Boolean scannerFiles,
            Optional<VersionNumber> springBoot, Optional<VersionNumber> springBootDeclared,
            List<String> needsSpring, List<String> withoutSpring, @Nullable Boolean noSpring,
            Optional<String> reason) {

        public Line {
            kind = Wire.text(kind);
            releases = Wire.list(releases);
            findings = Wire.list(findings);
            scannedAt = Wire.value(scannedAt);
            springBoot = Wire.value(springBoot);
            springBootDeclared = Wire.value(springBootDeclared);
            needsSpring = Wire.list(needsSpring);
            withoutSpring = Wire.list(withoutSpring);
            reason = Wire.text(reason);
        }
    }

    /** One finding of a line. {@code modules} are positions in {@code module_names}. */
    public record Finding(String cve, Optional<String> ecosystem, List<Integer> modules, Optional<String> owner,
            Optional<String> severity, Optional<String> verdict, Optional<String> bundledIn,
            @Nullable Boolean springPins) {

        public Finding {
            ecosystem = Wire.text(ecosystem);
            modules = Wire.list(modules);
            owner = Wire.text(owner);
            severity = Wire.text(severity);
            verdict = Wire.text(verdict);
            bundledIn = Wire.text(bundledIn);
        }
    }

    /** Which products are optional, discontinued, always there, or carried inside another. */
    public record Profile(List<String> on, List<String> off, List<String> discontinued, List<String> fixed,
            Map<String, String> inside, Optional<String> switchesShape) {

        public Profile {
            on = Wire.list(on);
            off = Wire.list(off);
            discontinued = Wire.list(discontinued);
            fixed = Wire.list(fixed);
            inside = Wire.map(inside);
            switchesShape = Wire.text(switchesShape);
        }
    }

    public record History(String cve, Optional<String> severity, Map<String, Span> lines) {

        public History {
            severity = Wire.text(severity);
            lines = Wire.map(lines);
        }

        public record Span(Optional<VersionNumber> fixedIn, Optional<VersionNumber> until) {

            public Span {
                fixedIn = Wire.value(fixedIn);
                until = Wire.value(until);
            }
        }
    }

    public record SpringBootTried(Optional<VersionNumber> to, List<String> closes, List<VersionNumber> measuredOn) {

        public SpringBootTried {
            to = Wire.value(to);
            closes = Wire.list(closes);
            measuredOn = Wire.list(measuredOn);
        }
    }
}

package com.vaadin.securitystatus.data.model;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vaadin.securitystatus.data.VersionNumber;

/** {@code data/<version>.json}: one measured release in full, for the explorer. */
public record ExplorerVersion(VersionNumber version, Optional<String> line, Optional<String> state,
        Optional<VersionNumber> newest, Optional<OffsetDateTime> scannedAt, Optional<Instant> generatedAt,
        Map<String, Integer> counts, List<Finding> findings, Optional<FromSpringBoot> fromSpringBoot,
        List<String> withoutSpring, @Nullable Boolean noSpring, Optional<VersionNumber> springBoot,
        Optional<VersionNumber> springBootDeclared, @Nullable Boolean scannerFiles, @Nullable Boolean workaround) {

    public ExplorerVersion {
        line = Wire.text(line);
        state = Wire.text(state);
        newest = Wire.value(newest);
        scannedAt = Wire.value(scannedAt);
        generatedAt = Wire.value(generatedAt);
        counts = Wire.map(counts);
        findings = Wire.list(findings);
        fromSpringBoot = Wire.value(fromSpringBoot);
        withoutSpring = Wire.list(withoutSpring);
        springBoot = Wire.value(springBoot);
        springBootDeclared = Wire.value(springBootDeclared);
    }

    public record FromSpringBoot(@Nullable Integer count, List<String> risk, Optional<String> state) {

        public FromSpringBoot {
            risk = Wire.list(risk);
            state = Wire.text(state);
        }
    }

    public record Finding(String cve, Optional<String> severity, Optional<String> summary,
            Optional<String> verdict, Optional<String> owner, Optional<String> fixOwner,
            Optional<VersionNumber> fixedIn, Optional<String> fixExpected, Map<String, Object> fixExpectedVersion,
            Optional<String> impact, Optional<String> workaround, Optional<String> commentary,
            Optional<String> bundledIn, List<String> modules, @Nullable Boolean springPins,
            List<PackageFinding> packages) {

        public Finding {
            severity = Wire.text(severity);
            summary = Wire.text(summary);
            verdict = Wire.text(verdict);
            owner = Wire.text(owner);
            fixOwner = Wire.text(fixOwner);
            fixedIn = Wire.value(fixedIn);
            fixExpected = Wire.text(fixExpected);
            fixExpectedVersion = Wire.map(fixExpectedVersion);
            impact = Wire.text(impact);
            workaround = Wire.text(workaround);
            commentary = Wire.text(commentary);
            bundledIn = Wire.text(bundledIn);
            modules = Wire.list(modules);
            packages = Wire.list(packages);
        }
    }

    /** One package that carries the advisory into this release, and the routes it takes. */
    public record PackageFinding(@JsonProperty("package") String packageName, Optional<String> origin,
            Optional<String> verdict, List<String> versions, List<String> versionsWithoutSpring,
            List<String> declared, List<String> declaredWithoutSpring, List<String> modules, List<Route> routes,
            List<Route> routesWithoutSpring, Optional<String> published, Optional<String> impact,
            Optional<String> workaround, Optional<String> commentary, Optional<String> fixExpected,
            Map<String, Object> fixExpectedVersion) {

        public PackageFinding {
            origin = Wire.text(origin);
            verdict = Wire.text(verdict);
            versions = Wire.list(versions);
            versionsWithoutSpring = Wire.list(versionsWithoutSpring);
            declared = Wire.list(declared);
            declaredWithoutSpring = Wire.list(declaredWithoutSpring);
            modules = Wire.list(modules);
            routes = Wire.list(routes);
            routesWithoutSpring = Wire.list(routesWithoutSpring);
            published = Wire.text(published);
            impact = Wire.text(impact);
            workaround = Wire.text(workaround);
            commentary = Wire.text(commentary);
            fixExpected = Wire.text(fixExpected);
            fixExpectedVersion = Wire.map(fixExpectedVersion);
        }
    }

    public record Route(Optional<String> entry, List<String> steps) {

        public Route {
            entry = Wire.text(entry);
            steps = Wire.list(steps);
        }
    }
}

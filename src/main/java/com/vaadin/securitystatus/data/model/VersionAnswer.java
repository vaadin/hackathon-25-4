package com.vaadin.securitystatus.data.model;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.vaadin.securitystatus.data.VersionNumber;

/** {@code cases/v/<version>.json}: everything the by version screen needs for one release. */
public record VersionAnswer(VersionNumber version, Optional<String> line, Optional<String> state,
        Optional<String> rawState, Optional<OffsetDateTime> scannedAt, @Nullable Integer findings,
        List<String> risk, List<String> triage, List<String> ruledOut, List<String> theirs,
        List<String> theirsRisk, List<String> springPinned, Optional<VersionNumber> springBoot,
        Optional<VersionNumber> springBootDeclared, List<String> discontinued, List<String> discontinuedThrough,
        List<String> fromBuild, List<String> fixable, List<String> stuck, Optional<Upgrade> upgrade,
        Map<String, Verdict> verdicts, Map<String, Assessment> assessments) {

    public VersionAnswer {
        line = Wire.text(line);
        state = Wire.text(state);
        rawState = Wire.text(rawState);
        scannedAt = Wire.value(scannedAt);
        risk = Wire.list(risk);
        triage = Wire.list(triage);
        ruledOut = Wire.list(ruledOut);
        theirs = Wire.list(theirs);
        theirsRisk = Wire.list(theirsRisk);
        springPinned = Wire.list(springPinned);
        springBoot = Wire.value(springBoot);
        springBootDeclared = Wire.value(springBootDeclared);
        discontinued = Wire.list(discontinued);
        discontinuedThrough = Wire.list(discontinuedThrough);
        fromBuild = Wire.list(fromBuild);
        fixable = Wire.list(fixable);
        stuck = Wire.list(stuck);
        upgrade = Wire.value(upgrade);
        verdicts = Wire.map(verdicts);
        assessments = Wire.map(assessments);
    }

    /** The line the version belongs to: the one the document names, or the one its number says. */
    public String lineName() {
        return line.orElse(version.line());
    }

    /** The one upgrade sentence: where to, how much it clears, how much stays. */
    public record Upgrade(Optional<VersionNumber> to, @Nullable Integer clears, @Nullable Integer remains,
            @Nullable Boolean measured) {

        public Upgrade {
            to = Wire.value(to);
        }
    }
}

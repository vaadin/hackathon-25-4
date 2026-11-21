package com.vaadin.securitystatus.data.model;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * How one advisory stands on one release: the state, the assessments that
 * answer it, and which assessment spoke for which package.
 */
public record Verdict(Optional<String> state, List<String> assessments, Map<String, String> saidByPackage) {

    public Verdict {
        state = Wire.text(state);
        assessments = Wire.list(assessments);
        saidByPackage = Wire.map(saidByPackage);
    }
}

package com.vaadin.securitystatus.data.model;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * {@code cases/scan-index.json}: per coordinate, a purl without version, the
 * advisories it is known for and the verdict of each, and per advisory what a
 * triage needs.
 */
public record ScanIndex(Map<String, Map<String, String>> coordinates, Map<String, Advisory> advisories) {

    public ScanIndex {
        coordinates = Wire.map(coordinates);
        advisories = Wire.map(advisories);
    }

    public record Advisory(Optional<String> applicability, Optional<String> severity, Optional<String> fixOwner,
            List<String> origins) {

        public Advisory {
            applicability = Wire.text(applicability);
            severity = Wire.text(severity);
            fixOwner = Wire.text(fixOwner);
            origins = Wire.list(origins);
        }
    }
}

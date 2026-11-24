package com.vaadin.securitystatus.customer;

import java.util.List;
import java.util.Optional;

/**
 * One scanner report, read. Nothing of the file itself is kept: only what the
 * triage needs.
 *
 * @param format     the shape it was recognised by
 * @param vaadin     the Vaadin release the file names, when exactly one is named
 * @param findings   the advisories the scanner reported, each on its coordinate
 * @param components the coordinates of a bill of materials, for which the
 *                   measurements supply the advisories; empty for the
 *                   vulnerability reports
 * @param skipped    the entries that carried findings and no coordinate, by
 *                   the name the file gave them, never silently dropped
 */
public record ScannerReport(ReportFormat format, Optional<String> vaadin, List<Finding> findings,
        List<String> components, List<String> skipped) {

    public ScannerReport {
        findings = List.copyOf(findings);
        components = List.copyOf(components);
        skipped = List.copyOf(skipped);
    }

    /**
     * One reported advisory on one coordinate.
     *
     * @param ids        every id the scanner gave it, a CVE first when there is one
     * @param coordinate the canonical purl with its version
     * @param severity   the scanner's own severity word, lower case, when it gave one
     */
    public record Finding(List<String> ids, String coordinate, Optional<String> severity) {

        public Finding {
            ids = List.copyOf(ids);
        }
    }

    /** How many findings the file carries, or for a bill of materials how many components. */
    public int size() {
        return findings.isEmpty() ? components.size() : findings.size();
    }
}

package com.vaadin.securitystatus.status;

/**
 * The scanner reports this application reads, and no others. The help card
 * lists exactly these, rule 6.11.21, and a test holds the two together.
 */
public enum ReportFormat {

    CYCLONEDX, DEPENDENCY_CHECK, OSV_SCANNER, BOMBER;

    /** The bundle key of the format's name as its makers spell it. */
    public String nameKey() {
        return "scan.format." + name();
    }
}

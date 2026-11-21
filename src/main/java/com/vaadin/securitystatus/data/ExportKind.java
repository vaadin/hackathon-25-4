package com.vaadin.securitystatus.data;

/** The machine files the pipeline publishes per release, by the suffix it gives them. */
public enum ExportKind {

    CSV("csv"),
    VEX("vex.json"),
    DEPENDENCY_CHECK_SUPPRESSIONS("dependency-check-suppressions.xml"),
    OSV_SCANNER("osv-scanner.toml"),
    BOMBER_IGNORE("bomber-ignore.txt");

    private final String suffix;

    ExportKind(String suffix) {
        this.suffix = suffix;
    }

    /** The name the file has upstream and keeps when downloaded. */
    public String fileName(VersionNumber version) {
        return version + "." + suffix;
    }
}

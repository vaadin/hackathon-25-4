package com.vaadin.securitystatus.data;

/**
 * The machine files the pipeline publishes per release, by the folder and the
 * suffix it gives them. The JSON is the release's own record, {@code
 * data/<version>.json}, rule 7.1 of the source project; the others are the
 * scanner files under {@code exports/}.
 */
public enum ExportKind {

    JSON("data/", "json"),
    CSV("exports/", "csv"),
    VEX("exports/", "vex.json"),
    DEPENDENCY_CHECK_SUPPRESSIONS("exports/", "dependency-check-suppressions.xml"),
    OSV_SCANNER("exports/", "osv-scanner.toml"),
    BOMBER_IGNORE("exports/", "bomber-ignore.txt");

    private final String folder;
    private final String suffix;

    ExportKind(String folder, String suffix) {
        this.folder = folder;
        this.suffix = suffix;
    }

    /** Where the publisher has the file, relative to its root. */
    public String path(VersionNumber version) {
        return folder + fileName(version);
    }

    /** The name the file has upstream and keeps when downloaded. */
    public String fileName(VersionNumber version) {
        return version + "." + suffix;
    }
}

package com.vaadin.securitystatus.customer;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads a scanner report into a {@link ScannerReport}, recognising its format
 * by its shape and never by its file name. Four shapes: three vulnerability
 * reports, osv-scanner, OWASP Dependency-Check and bomber, which carry an
 * advisory beside a coordinate, and a CycloneDX bill of materials, which
 * carries coordinates and may carry findings of its own.
 *
 * <p>
 * The stream is read once and nothing of it is kept.
 */
public final class ReportParser {

    /** Why a file was refused. */
    public enum Refusal {
        /** Not JSON at all. */
        NOT_JSON,
        /** JSON, and none of the four shapes. */
        UNKNOWN_FORMAT,
        /** Larger than the bytes a report may have. */
        TOO_LARGE,
        /** More findings than one triage reads. */
        TOO_MANY_FINDINGS
    }

    /** The most findings one report is read for; a whole application's report has a few hundred. */
    public static final int MAX_FINDINGS = 10_000;

    /**
     * The longest coordinate or severity kept. A real purl is well under this;
     * a longer one is not a coordinate, and is listed as skipped instead.
     */
    static final int MAX_TEXT = 300;

    /** A file this parser cannot read, with the reason a reader can act on. */
    public static final class RefusedException extends Exception {

        private final Refusal refusal;

        RefusedException(Refusal refusal, Throwable cause) {
            super(refusal.name(), cause);
            this.refusal = refusal;
        }

        public Refusal refusal() {
            return refusal;
        }
    }

    /**
     * The coordinates that name the Vaadin release an application is on. In an
     * application tree only the umbrella artifacts carry the platform version
     * exactly; the bill of materials of the platform itself is written about the
     * other two, and Vaadin 7 and 8 have no umbrella, where the server artifact
     * carries it.
     */
    static final Set<String> PLATFORM = Set.of("pkg:maven/com.vaadin/vaadin", "pkg:maven/com.vaadin/vaadin-core",
            "pkg:maven/com.vaadin/vaadin-bom", "pkg:maven/com.vaadin/vaadin-platform-sbom",
            "pkg:maven/com.vaadin/vaadin-server");

    /** The osv-scanner ecosystem names, as purl types. */
    private static final Map<String, String> ECOSYSTEMS = Map.of("maven", "maven", "npm", "npm", "crates.io",
            "cargo", "go", "golang", "pypi", "pypi", "packagist", "composer", "rubygems", "gem", "nuget", "nuget");

    private static final Pattern ADVISORY = Pattern.compile("(?i)^(CVE|GHSA)-.+");

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private ReportParser() {
    }

    public static ScannerReport parse(InputStream body) throws RefusedException {
        return parse(body, Long.MAX_VALUE);
    }

    /** Reads at most {@code maxBytes}; a longer stream is refused as too large, whatever the browser said. */
    public static ScannerReport parse(InputStream body, long maxBytes) throws RefusedException {
        JsonNode root;
        var limited = new Limited(body, maxBytes);
        try {
            root = JSON.readTree(limited);
        } catch (JacksonException notJson) {
            if (limited.exceeded) {
                throw new RefusedException(Refusal.TOO_LARGE, notJson);
            }
            throw new RefusedException(Refusal.NOT_JSON, notJson);
        }
        if (root == null || !root.isObject()) {
            throw new RefusedException(Refusal.NOT_JSON, null);
        }
        ReportFormat format = detect(root)
                .orElseThrow(() -> new RefusedException(Refusal.UNKNOWN_FORMAT, null));
        var read = new Read();
        try {
            switch (format) {
                case OSV_SCANNER -> readOsv(root, read);
                case DEPENDENCY_CHECK -> readDependencyCheck(root, read);
                case BOMBER -> readBomber(root, read);
                case CYCLONEDX -> readCycloneDx(root, read);
            }
        } catch (TooMany stop) {
            throw new RefusedException(Refusal.TOO_MANY_FINDINGS, null);
        }
        return new ScannerReport(format, platformVersion(read.named), read.findings, List.copyOf(read.components),
                read.skipped);
    }

    /** The format by the shape of the document, or empty when it is none of the four. */
    static Optional<ReportFormat> detect(JsonNode root) {
        if (root.path("bomFormat").asString("").equals("CycloneDX")) {
            return Optional.of(ReportFormat.CYCLONEDX);
        }
        if (anyElement(root.path("results"), result -> result.path("packages").isArray())) {
            return Optional.of(ReportFormat.OSV_SCANNER);
        }
        if (root.path("dependencies").isArray() && root.path("scanInfo").isObject()) {
            return Optional.of(ReportFormat.DEPENDENCY_CHECK);
        }
        if (root.path("packages").isArray() && (root.path("meta").path("generator").asString("").equals("bomber")
                || anyElement(root.path("packages"), entry -> entry.has("coordinates")))) {
            return Optional.of(ReportFormat.BOMBER);
        }
        return Optional.empty();
    }

    /** What has been read so far. */
    private static final class Read {
        final List<ScannerReport.Finding> findings = new ArrayList<>();
        final Set<String> components = new LinkedHashSet<>();
        final List<String> skipped = new ArrayList<>();
        /** Every coordinate the file names, only ever asked which Vaadin it was. */
        final List<String> named = new ArrayList<>();

        void finding(List<String> ids, String coordinate, String severity) {
            if (ids.isEmpty()) {
                return;
            }
            if (coordinate.length() > MAX_TEXT) {
                skip(coordinate);
                return;
            }
            if (findings.size() >= MAX_FINDINGS) {
                throw new TooMany();
            }
            findings.add(new ScannerReport.Finding(ids, coordinate,
                    Optional.of(severity.toLowerCase(Locale.ROOT)).filter(word -> !word.isBlank())
                            .filter(word -> word.length() <= MAX_TEXT)));
        }

        /** A name the reader is told was left out, cut to a length a screen can show. */
        void skip(String name) {
            if (skipped.size() >= MAX_FINDINGS) {
                throw new TooMany();
            }
            skipped.add(name.length() > MAX_TEXT ? name.substring(0, MAX_TEXT) + "\u2026" : name);
        }
    }

    /** Stops the reading of a report with more findings than {@link #MAX_FINDINGS}. */
    private static final class TooMany extends RuntimeException {

        TooMany() {
            super(null, null, false, false);
        }
    }

    /** The upload's stream, refusing to give more than the bytes a report may have. */
    private static final class Limited extends FilterInputStream {

        private long left;
        boolean exceeded;

        Limited(InputStream in, long max) {
            super(in);
            this.left = max;
        }

        @Override
        public int read() throws IOException {
            int b = super.read();
            if (b >= 0) {
                count(1);
            }
            return b;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            int n = super.read(buffer, offset, length);
            if (n > 0) {
                count(n);
            }
            return n;
        }

        private void count(long n) throws IOException {
            left -= n;
            if (left < 0) {
                exceeded = true;
                throw new IOException("larger than allowed");
            }
        }
    }

    private static void readOsv(JsonNode root, Read read) {
        for (JsonNode result : root.path("results")) {
            for (JsonNode entry : result.path("packages")) {
                JsonNode pkg = entry.path("package");
                String name = pkg.path("name").asString("");
                String version = pkg.path("version").asString("");
                JsonNode vulnerabilities = entry.path("vulnerabilities");
                if (name.isBlank()) {
                    if (!vulnerabilities.isEmpty()) {
                        read.skip(firstNonBlank(result.path("source").path("path").asString(""),
                                pkg.path("purl").asString("")));
                    }
                    continue;
                }
                String coordinate = osvCoordinate(pkg.path("ecosystem").asString(""), name, version);
                read.named.add(coordinate);
                for (JsonNode vulnerability : vulnerabilities) {
                    var aliases = new ArrayList<String>();
                    vulnerability.path("aliases").forEach(alias -> aliases.add(alias.asString("")));
                    read.finding(ids(vulnerability.path("id").asString(""), aliases), coordinate,
                            vulnerability.path("database_specific").path("severity").asString(""));
                }
            }
        }
    }

    private static void readDependencyCheck(JsonNode root, Read read) {
        for (JsonNode dependency : root.path("dependencies")) {
            var coordinates = new ArrayList<String>();
            for (JsonNode pkg : dependency.path("packages")) {
                String id = canonical(pkg.path("id").asString(""));
                if (id.startsWith("pkg:")) {
                    coordinates.add(id);
                }
            }
            read.named.addAll(coordinates);
            JsonNode vulnerabilities = dependency.path("vulnerabilities");
            if (vulnerabilities.isEmpty()) {
                continue;
            }
            if (coordinates.isEmpty()) {
                // Findings with nothing to match them on. Named, because left out
                // they would read as a clean dependency.
                read.skip(firstNonBlank(dependency.path("fileName").asString(""),
                        dependency.path("filePath").asString("")));
                continue;
            }
            for (JsonNode vulnerability : vulnerabilities) {
                List<String> ids = ids(vulnerability.path("name").asString(""), List.of());
                for (String coordinate : coordinates) {
                    read.finding(ids, coordinate, vulnerability.path("severity").asString(""));
                }
            }
        }
    }

    private static void readBomber(JsonNode root, Read read) {
        for (JsonNode entry : root.path("packages")) {
            String coordinate = canonical(entry.path("coordinates").asString(""));
            JsonNode vulnerabilities = entry.path("vulnerabilities");
            if (!coordinate.startsWith("pkg:")) {
                if (!vulnerabilities.isEmpty()) {
                    read.skip(firstNonBlank(entry.path("name").asString(""), coordinate));
                }
                continue;
            }
            read.named.add(coordinate);
            for (JsonNode vulnerability : vulnerabilities) {
                // Several ids in one comma separated field, and "NOT PROVIDED" where there is none.
                var aliases = new ArrayList<String>();
                for (String alias : vulnerability.path("cve").asString("").split(",")) {
                    aliases.add(alias.strip());
                }
                read.finding(ids(vulnerability.path("id").asString(""), aliases), coordinate,
                        vulnerability.path("severity").asString(""));
            }
        }
    }

    private static void readCycloneDx(JsonNode root, Read read) {
        var byReference = new HashMap<String, String>();
        Consumer<JsonNode> take = component -> {
            String coordinate = canonical(component.path("purl").asString(""));
            if (coordinate.startsWith("pkg:")) {
                read.components.add(coordinate);
                read.named.add(coordinate);
                String reference = component.path("bom-ref").asString("");
                byReference.put(reference.isBlank() ? coordinate : reference, coordinate);
            }
        };
        walk(root.path("components"), take);
        JsonNode own = root.path("metadata").path("component");
        if (own.isObject()) {
            take.accept(own);
        }
        for (JsonNode vulnerability : root.path("vulnerabilities")) {
            var aliases = new ArrayList<String>();
            vulnerability.path("references").forEach(reference -> aliases.add(reference.path("id").asString("")));
            List<String> ids = ids(vulnerability.path("id").asString(""), aliases);
            String severity = vulnerability.path("ratings").path(0).path("severity").asString("");
            var affected = new ArrayList<String>();
            for (JsonNode affects : vulnerability.path("affects")) {
                String reference = affects.path("ref").asString("");
                String coordinate = byReference.getOrDefault(reference, canonical(reference));
                if (coordinate.startsWith("pkg:")) {
                    affected.add(coordinate);
                }
            }
            if (affected.isEmpty()) {
                if (!ids.isEmpty()) {
                    read.skip(ids.getFirst());
                }
                continue;
            }
            affected.forEach(coordinate -> read.finding(ids, coordinate, severity));
        }
    }

    /** Every component, nested ones too: npm components are written under the parent that pins them. */
    private static void walk(JsonNode components, Consumer<JsonNode> take) {
        for (JsonNode component : components) {
            take.accept(component);
            walk(component.path("components"), take);
        }
    }

    /** The release when the file names exactly one through the platform coordinates; two disagreeing is a question. */
    static Optional<String> platformVersion(List<String> coordinates) {
        var found = new LinkedHashSet<String>();
        for (String coordinate : coordinates) {
            int at = versionAt(coordinate);
            if (at > 0 && PLATFORM.contains(coordinate.substring(0, at))) {
                found.add(coordinate.substring(at + 1));
            }
        }
        return found.size() == 1 ? Optional.of(found.getFirst()) : Optional.empty();
    }

    /** The ids of one advisory, a CVE first when there is one, because tickets are written against it. */
    static List<String> ids(String primary, List<String> aliases) {
        var all = new LinkedHashSet<String>();
        for (String id : prepend(primary, aliases)) {
            String normal = normalId(id);
            if (ADVISORY.matcher(normal).matches()) {
                all.add(normal);
            }
        }
        var ordered = new ArrayList<String>();
        all.stream().filter(id -> id.startsWith("CVE-")).findFirst().ifPresent(ordered::add);
        all.stream().filter(id -> !ordered.contains(id)).forEach(ordered::add);
        return ordered;
    }

    static String normalId(String id) {
        String trimmed = id.strip();
        if (trimmed.regionMatches(true, 0, "GHSA-", 0, 5)) {
            return "GHSA-" + trimmed.substring(5).toLowerCase(Locale.ROOT);
        }
        return trimmed.toUpperCase(Locale.ROOT);
    }

    /**
     * A purl as the measurements key it: no qualifiers, no subpath, and an npm
     * scope decoded, because it arrives as {@code %40scope/name},
     * {@code %40scope%2Fname} or {@code @scope/name} depending on who wrote the file.
     */
    static String canonical(String purl) {
        String out = purl.strip();
        int hash = out.indexOf('#');
        if (hash >= 0) {
            out = out.substring(0, hash);
        }
        int query = out.indexOf('?');
        if (query >= 0) {
            out = out.substring(0, query);
        }
        try {
            return URLDecoder.decode(out.replace("+", "%2B"), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException malformed) {
            return out;
        }
    }

    /** The coordinate without its version, by the rule that survives a scoped npm name. */
    static String bare(String coordinate) {
        int at = versionAt(coordinate);
        return at > 0 ? coordinate.substring(0, at) : coordinate;
    }

    /** The artifact as a reader reads it: {@code jackson-core}, not the whole purl. */
    static String componentName(String coordinate) {
        String withoutVersion = bare(coordinate);
        String last = withoutVersion.substring(withoutVersion.lastIndexOf('/') + 1);
        return last.substring(last.lastIndexOf(':') + 1);
    }

    private static int versionAt(String coordinate) {
        int at = coordinate.lastIndexOf('@');
        return at > coordinate.lastIndexOf('/') ? at : -1;
    }

    private static String osvCoordinate(String ecosystem, String name, String version) {
        String lower = ecosystem.toLowerCase(Locale.ROOT);
        String type = ECOSYSTEMS.getOrDefault(lower, lower);
        String path = "maven".equals(type) ? name.replaceFirst(":", "/") : name;
        return canonical("pkg:" + type + "/" + path + (version.isBlank() ? "" : "@" + version));
    }

    private static List<String> prepend(String first, List<String> rest) {
        var all = new ArrayList<String>();
        all.add(first);
        all.addAll(rest);
        return all;
    }

    private static boolean anyElement(JsonNode array, Predicate<JsonNode> test) {
        if (!array.isArray()) {
            return false;
        }
        for (JsonNode element : array) {
            if (test.test(element)) {
                return true;
            }
        }
        return false;
    }

    private static String firstNonBlank(String first, String second) {
        return first.isBlank() ? second : first;
    }
}

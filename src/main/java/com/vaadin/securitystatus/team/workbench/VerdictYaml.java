package com.vaadin.securitystatus.team.workbench;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * The verdict as the file vaadin/cve-status keeps under
 * {@code data/assessments/}, and the address that opens GitHub's new file
 * editor with it.
 *
 * <p>
 * The keys come in the order the repository's own files use, one long line
 * per prose field. A field the draft leaves empty produces no key at all:
 * the repository's loader gives every optional field its default, so absent
 * is a legal answer, and an empty value would claim something nobody said,
 * D-122.
 */
public final class VerdictYaml {

    /** GitHub's new file editor on the default branch of the verdict store. */
    public static final String EDITOR = "https://github.com/vaadin/cve-status/new/main";

    /** Where the verdict files live in that repository. */
    public static final String FOLDER = "data/assessments/";

    /**
     * The longest address the handover carries. Browsers refuse a GET somewhere
     * past it, and the JavaScript workbench refuses at the same length.
     */
    public static final int URL_CAP = 7000;

    private VerdictYaml() {
    }

    /** The handover of one verdict: where it goes, what it says, and whether it fits a GET. */
    public record Handover(String path, String yaml, String url) {

        /** True when the address is short enough for a browser to send. */
        public boolean fits() {
            return url.length() <= URL_CAP;
        }
    }

    /** The YAML of the draft, composed on the given day. */
    public static String of(VerdictDraft draft, LocalDate composedOn) {
        return yaml().dump(fields(draft, composedOn));
    }

    /** The keys and values the YAML carries, in their order; what is empty is left out. */
    static Map<String, Object> fields(VerdictDraft draft, LocalDate composedOn) {
        var out = new LinkedHashMap<String, Object>();
        put(out, "id", draft.getVerdictId());
        put(out, "purl", draft.getPurl());
        if (!draft.advisoryIds().isEmpty()) {
            out.put("cves", draft.advisoryIds());
        }
        put(out, "applicability", draft.getApplicability());
        justification(draft.getApplicability()).ifPresent(word -> out.put("justification", word));
        put(out, "depth", draft.getDepth());
        put(out, "affects", draft.getAffects());
        put(out, "commentary", draft.getCommentary());
        put(out, "impact", draft.getImpact());
        put(out, "workaround", draft.getWorkaround());
        put(out, "fix_expected", draft.getFixExpected());
        Map<String, String> promised = promised(draft.getFixExpectedVersion());
        if (!promised.isEmpty()) {
            out.put("fix_expected_version", promised);
        }
        if (!draft.pullRequests().isEmpty()) {
            out.put("fix_prs", draft.pullRequests());
        }
        put(out, "evidence", draft.getEvidence());
        put(out, "assessed_by", draft.getAssessedBy());
        out.put("assessed_at", composedOn.toString());
        put(out, "supersedes", draft.getSupersedes());
        return out;
    }

    /**
     * The CSAF justification the cve-status loader pairs with each verdict
     * that claims something about code, D-121.
     */
    static Optional<String> justification(String applicability) {
        return Optional.ofNullable(switch (applicability) {
        case "not_affected" -> "code_not_present";
        case "configuration" -> "requires_configuration";
        case "no_risk" -> "code_not_reachable";
        default -> null;
        });
    }

    /** {@code 25.3: 25.3.4, 24.9: 24.9.30} as a mapping of line to release, in the order written. */
    public static Map<String, String> promised(String text) {
        var out = new LinkedHashMap<String, String>();
        for (String entry : text.split(",")) {
            int colon = entry.indexOf(':');
            if (colon > 0) {
                String line = entry.substring(0, colon).trim();
                String version = entry.substring(colon + 1).trim();
                if (!line.isEmpty() && !version.isEmpty()) {
                    out.put(line, version);
                }
            }
        }
        return out;
    }

    /** The path of the verdict's file in the repository. */
    public static String path(VerdictDraft draft) {
        return FOLDER + draft.getVerdictId().trim() + ".yaml";
    }

    /**
     * The handover: the file's path, its text, and the address a GET form with
     * {@code filename} and {@code value} sends. The address is written as a
     * browser writes it, form encoded with every line break sent as CR LF,
     * so its length is the one the browser would send, D-129.
     */
    public static Handover handover(VerdictDraft draft, LocalDate composedOn) {
        String path = path(draft);
        String yaml = of(draft, composedOn);
        String url = EDITOR + "?filename=" + encode(path) + "&value=" + encode(yaml);
        return new Handover(path, yaml, url);
    }

    private static String encode(String value) {
        String crlf = value.replace("\r\n", "\n").replace("\n", "\r\n");
        return URLEncoder.encode(crlf, StandardCharsets.UTF_8);
    }

    private static void put(Map<String, Object> out, String key, String value) {
        if (!value.isBlank()) {
            out.put(key, value.trim());
        }
    }

    private static Yaml yaml() {
        var options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setSplitLines(false);
        options.setWidth(Integer.MAX_VALUE);
        options.setIndent(2);
        options.setLineBreak(DumperOptions.LineBreak.UNIX);
        return new Yaml(options);
    }
}

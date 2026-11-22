package com.vaadin.securitystatus.workbench;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

/**
 * WRK-04 and WRK-05: the YAML is the contract. It round trips through a YAML
 * parser to the draft's fields, an absent field produces no key, and the
 * GitHub address carries the path and the YAML.
 */
class VerdictYamlTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 21);

    /** A ready draft with every field the form has, prose with quotes, colons and a hash. */
    static VerdictDraft full() {
        var draft = new VerdictDraft();
        draft.setVerdictId("maven-com-fasterxml-jackson-core-jackson-core-24-9-2026-11-21");
        draft.setPurl("pkg:maven/com.fasterxml.jackson.core/jackson-core");
        draft.setAdvisories("CVE-2026-89407, CVE-2026-89425 GHSA-588m-9qg5-35pq");
        draft.setApplicability("configuration");
        draft.setDepth("reproduced");
        draft.setAffects(">=24.9,<25.3");
        draft.setImpact("None unless the application parses: untrusted \"JSON\" # with async.");
        draft.setWorkaround("Do not feed the async parser input you did not write.");
        draft.setCommentary("It's reachable only through the async parser, which Vaadin never builds.");
        draft.setFixExpected("Spring Boot 3.5.17 moves it.");
        draft.setFixExpectedVersion("24.9: 24.9.30, 25.3: 25.3.6");
        draft.setFixPrs("https://github.com/vaadin/flow/pull/123 https://github.com/vaadin/platform/pull/9");
        draft.setEvidence("Measured on 2026-11-20 against 24.9.21.");
        draft.setSupersedes("maven-com-fasterxml-jackson-core-jackson-core-24-9");
        draft.setAssessedBy("manolo");
        return draft;
    }

    /** The fields a draft needs to be exported, and nothing else. */
    static VerdictDraft minimal() {
        var draft = new VerdictDraft();
        draft.setVerdictId("npm-highcharts-2026-11-21");
        draft.setPurl("pkg:npm/highcharts");
        draft.setAdvisories("CVE-2026-116796");
        draft.setApplicability("fixed");
        return draft;
    }

    // WRK-04
    @Test
    void a_ready_draft_round_trips_through_a_yaml_parser_to_its_fields() {
        VerdictDraft draft = full();

        Map<String, Object> parsed = new Yaml().load(VerdictYaml.of(draft, DAY));

        var expected = new LinkedHashMap<String, Object>();
        expected.put("id", draft.getVerdictId());
        expected.put("purl", draft.getPurl());
        expected.put("cves", List.of("CVE-2026-89407", "CVE-2026-89425", "GHSA-588m-9qg5-35pq"));
        expected.put("applicability", "configuration");
        expected.put("justification", "requires_configuration");
        expected.put("depth", "reproduced");
        expected.put("affects", ">=24.9,<25.3");
        expected.put("commentary", draft.getCommentary());
        expected.put("impact", draft.getImpact());
        expected.put("workaround", draft.getWorkaround());
        expected.put("fix_expected", draft.getFixExpected());
        expected.put("fix_expected_version", Map.of("24.9", "24.9.30", "25.3", "25.3.6"));
        expected.put("fix_prs",
                List.of("https://github.com/vaadin/flow/pull/123", "https://github.com/vaadin/platform/pull/9"));
        expected.put("evidence", draft.getEvidence());
        expected.put("assessed_by", "manolo");
        expected.put("assessed_at", "2026-11-21");
        expected.put("supersedes", draft.getSupersedes());
        assertThat(parsed).containsExactlyEntriesOf(expected);
    }

    // WRK-04, the store's own shape: one line per field, the dates and ranges quoted
    @Test
    void the_yaml_is_written_the_way_the_store_writes_it() {
        String yaml = VerdictYaml.of(full(), DAY);

        assertThat(yaml).startsWith("id: maven-com-fasterxml-jackson-core-jackson-core-24-9-2026-11-21\n"
                + "purl: pkg:maven/com.fasterxml.jackson.core/jackson-core\ncves:\n- CVE-2026-89407\n");
        assertThat(yaml).contains("\naffects: '>=24.9,<25.3'\n").contains("\nassessed_at: '2026-11-21'\n");
        assertThat(yaml.lines().filter(line -> line.startsWith("commentary:"))).singleElement().asString()
                .endsWith("Vaadin never builds.");
    }

    // WRK-04 and AC3: absent optional fields produce no YAML keys
    @Test
    void absent_fields_produce_no_keys() {
        Map<String, Object> parsed = new Yaml().load(VerdictYaml.of(minimal(), DAY));

        assertThat(parsed).isEqualTo(Map.of("id", "npm-highcharts-2026-11-21", "purl",
                "pkg:npm/highcharts", "cves", List.of("CVE-2026-116796"), "applicability", "fixed", "assessed_at",
                "2026-11-21"));
        assertThat(VerdictYaml.of(minimal(), DAY)).doesNotContain("null").doesNotContain("{}")
                .doesNotContain("''");
    }

    @Test
    void a_blank_field_is_absent_too() {
        VerdictDraft draft = minimal();
        draft.setImpact("   ");
        draft.setFixPrs(" , ");
        draft.setFixExpectedVersion(" ");

        Map<String, Object> parsed = new Yaml().load(VerdictYaml.of(draft, DAY));

        assertThat(parsed).doesNotContainKeys("impact", "fix_prs", "fix_expected_version");
    }

    @Test
    void the_justification_follows_the_applicability_as_the_loader_pairs_them() {
        assertThat(VerdictYaml.justification("not_affected")).contains("code_not_present");
        assertThat(VerdictYaml.justification("configuration")).contains("requires_configuration");
        assertThat(VerdictYaml.justification("no_risk")).contains("code_not_reachable");
        assertThat(VerdictYaml.justification("risk")).isEmpty();
        assertThat(VerdictYaml.justification("false_positive")).isEmpty();
        assertThat(VerdictYaml.justification("fixed")).isEmpty();
    }

    // WRK-05
    @Test
    void the_github_address_carries_the_path_and_the_yaml_when_decoded() {
        VerdictDraft draft = full();

        var handover = VerdictYaml.handover(draft, DAY);
        URI address = URI.create(handover.url());
        Map<String, String> query = query(address.getRawQuery());

        assertThat(address.getScheme() + "://" + address.getHost() + address.getPath())
                .isEqualTo("https://github.com/vaadin/cve-status/new/main");
        assertThat(query).containsOnlyKeys("filename", "value");
        assertThat(query.get("filename"))
                .isEqualTo("data/assessments/maven-com-fasterxml-jackson-core-jackson-core-24-9-2026-11-21.yaml");
        // A form sends its line breaks as CR LF, so the decoded value is the YAML with those.
        assertThat(query.get("value")).isEqualTo(VerdictYaml.of(draft, DAY).replace("\n", "\r\n"));
        assertThat(query.get("value").replace("\r\n", "\n")).isEqualTo(handover.yaml());
        assertThat(handover.fits()).isTrue();
    }

    // AC4: above the cap the address does not fit
    @Test
    void an_address_above_the_cap_does_not_fit() {
        VerdictDraft draft = full();
        draft.setEvidence("x".repeat(VerdictYaml.URL_CAP));

        var handover = VerdictYaml.handover(draft, DAY);

        assertThat(handover.url().length()).isGreaterThan(VerdictYaml.URL_CAP);
        assertThat(handover.fits()).isFalse();
        assertThat(handover.yaml()).contains(draft.getEvidence());
    }

    private static Map<String, String> query(String raw) {
        var out = new LinkedHashMap<String, String>();
        for (String pair : raw.split("&")) {
            int equals = pair.indexOf('=');
            out.put(URLDecoder.decode(pair.substring(0, equals), StandardCharsets.UTF_8),
                    URLDecoder.decode(pair.substring(equals + 1), StandardCharsets.UTF_8));
        }
        return out;
    }
}

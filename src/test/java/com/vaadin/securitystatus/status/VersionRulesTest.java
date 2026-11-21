package com.vaadin.securitystatus.status;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.vaadin.securitystatus.data.VersionNumber;
import com.vaadin.securitystatus.data.model.CasesIndex;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

/**
 * The rules of the version screen that are easiest to get wrong in one
 * direction: the secure move of rules 6.11.8 and 6.11.23, the nearest measured
 * release of an unmeasured version, and the worst first order.
 */
class VersionRulesTest {

    private static final JsonMapper MAPPER = JsonMapper.builder()
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();

    /** Two lines of 24, one of 23, one of 25 with a prerelease above it. */
    private static final CasesIndex INDEX = MAPPER.readValue("""
            {
              "generated_at": "2026-11-21T10:13:39Z",
              "release_repos": {"8": "vaadin/framework", "default": "vaadin/platform"},
              "by_line": {"24.7": ["24.7.18", "24.7.19"], "24.9": ["24.9.25", "24.9.26"], "23.6": ["23.6.19"],
                          "25.3": ["25.3.5"]},
              "summary": {
                "24.7.18": {"line": "24.7", "state": "secure"},
                "24.7.19": {"line": "24.7", "state": "unresolved"},
                "24.9.25": {"line": "24.9", "state": "secure"},
                "24.9.26": {"line": "24.9", "state": "secure"},
                "23.6.19": {"line": "23.6", "state": "secure"},
                "25.3.5": {"line": "25.3", "state": "secure"},
                "25.4.0-beta2": {"line": "25.4", "state": "secure"}
              },
              "advisories": {
                "CVE-1": {"cve": "CVE-1", "severity": "low"},
                "CVE-2": {"cve": "CVE-2", "severity": "critical"},
                "CVE-3": {"cve": "CVE-3", "severity": "high"}
              }
            }
            """, CasesIndex.class);

    @Test
    void the_move_prefers_the_readers_line_and_is_never_older() {
        // 24.7.18 is secure and on the line, but older than 24.7.19: never a downgrade.
        var move = VersionRules.secureMove(INDEX, VersionNumber.of("24.7.19"), "24.7").orElseThrow();
        assertThat(move.to()).hasToString("24.9.26");
        assertThat(move.sameLine()).isFalse();

        var onLine = VersionRules.secureMove(INDEX, VersionNumber.of("24.9.20"), "24.9").orElseThrow();
        assertThat(onLine.to()).hasToString("24.9.26");
        assertThat(onLine.sameLine()).isTrue();
    }

    @Test
    void the_move_never_leaves_the_major_and_never_offers_a_prerelease() {
        // 23.6.19 is secure but there is nothing newer in 23: no offer, not 24 or 25.
        assertThat(VersionRules.secureMove(INDEX, VersionNumber.of("23.6.19"), "23.6")).isEmpty();
        // On 25 only the prerelease is newer than 25.3.5.
        assertThat(VersionRules.secureMove(INDEX, VersionNumber.of("25.3.5"), "25.3")).isEmpty();
    }

    @Test
    void the_nearest_measured_release_is_the_line_then_the_major_then_the_oldest() {
        assertThat(VersionRules.nearestMeasured(INDEX, VersionNumber.of("24.9.99")).orElseThrow())
                .isEqualTo(new VersionRules.Nearest(VersionNumber.of("24.9.26"), VersionRules.Nearest.Kind.LINE));
        assertThat(VersionRules.nearestMeasured(INDEX, VersionNumber.of("24.8.1")).orElseThrow())
                .isEqualTo(new VersionRules.Nearest(VersionNumber.of("24.9.26"), VersionRules.Nearest.Kind.MAJOR));
        assertThat(VersionRules.nearestMeasured(INDEX, VersionNumber.of("6.0.1")).orElseThrow())
                .isEqualTo(new VersionRules.Nearest(VersionNumber.of("23.6.19"), VersionRules.Nearest.Kind.OLDEST));
        // A major nobody measured above the oldest one gets nothing: no line, no major, not below everything.
        assertThat(VersionRules.nearestMeasured(INDEX, VersionNumber.of("26.0.1"))).isEmpty();
    }

    @Test
    void release_notes_come_from_the_repository_of_the_major() {
        assertThat(VersionRules.releaseNotes(INDEX, VersionNumber.of("8.14.3")))
                .contains("https://github.com/vaadin/framework/releases/tag/8.14.3");
        assertThat(VersionRules.releaseNotes(INDEX, VersionNumber.of("24.9.26")))
                .contains("https://github.com/vaadin/platform/releases/tag/24.9.26");
    }

    @Test
    void worst_first_orders_by_severity_then_id_with_the_unrated_last() {
        assertThat(VersionRules.worstFirst(java.util.List.of("CVE-9", "CVE-1", "CVE-3", "CVE-2"), INDEX))
                .containsExactly("CVE-2", "CVE-3", "CVE-1", "CVE-9");
    }
}

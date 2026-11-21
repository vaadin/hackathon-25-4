package com.vaadin.securitystatus.explorer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.vaadin.securitystatus.data.ExportKind;
import com.vaadin.securitystatus.data.FixturePages;
import com.vaadin.securitystatus.data.SecurityDataService;
import com.vaadin.securitystatus.data.VersionNumber;
import com.vaadin.securitystatus.data.model.ExplorerFront;
import com.vaadin.securitystatus.data.model.ExplorerFront.Finding;
import com.vaadin.securitystatus.data.model.ExplorerVersion;

/** The release and Spring Boot pages' rules without components, against the committed fixtures. */
class ReleaseRulesTest {

    private final SecurityDataService data = FixturePages.offline();
    private final ExplorerFront front = data.explorerFront();
    private final ExplorerVersion record = data.explorerVersion(VersionNumber.of("24.9.21")).orElseThrow();

    @Test
    void an_update_removes_only_what_a_vaadin_release_moves_after_this_release() {
        List<ExplorerVersion.Finding> removed = ReleaseRules.removedBy(record);

        assertThat(removed).hasSize(22);
        assertThat(removed).allSatisfy(finding -> {
            assertThat(finding.owner()).isNotEqualTo(Optional.of("spring-boot"));
            assertThat(finding.fixedIn().orElseThrow()).isGreaterThan(VersionNumber.of("24.9.21"))
                    .isLessThanOrEqualTo(VersionNumber.of("24.9.26"));
        });
        // The build only finding carries a fixed_in before the release itself, which no update removes.
        assertThat(removed).noneMatch(finding -> finding.owner().equals(Optional.of("build")));
        assertThat(ReleaseRules.removedBy(data.explorerVersion(VersionNumber.of("25.3.5")).orElseThrow())).isEmpty();
    }

    @Test
    void the_record_reads_into_the_explorer_groups_with_its_own_counts() {
        var groups = ExplorerRules.grouped(ReleaseRules.findings(record, front.moduleNames()));

        assertThat(groups.get(ExplorerRules.Group.RISK)).hasSize(record.counts().get("risk"));
        assertThat(groups.get(ExplorerRules.Group.UNJUDGED)).hasSize(record.counts().get("unjudged"));
        assertThat(groups.get(ExplorerRules.Group.SPRING)).hasSize(record.counts().get("from_spring_boot"));
        assertThat(groups.get(ExplorerRules.Group.BUILD)).hasSize(record.counts().get("in_the_build"));
        assertThat(groups.get(ExplorerRules.Group.ANSWERED)).hasSize(record.counts().get("ruled_out"));
    }

    @Test
    void scanner_files_only_where_the_record_says_they_exist() {
        assertThat(ReleaseRules.files(false)).containsExactly(ExportKind.JSON, ExportKind.CSV);
        assertThat(ReleaseRules.files(null)).containsExactly(ExportKind.JSON, ExportKind.CSV);
        assertThat(ReleaseRules.files(true)).containsExactly(ExportKind.values());
    }

    @Test
    void spring_targets_are_asked_of_what_the_release_declares() {
        var targets = ReleaseRules.springTargets(front, record).orElseThrow();

        assertThat(targets.from()).isEqualTo(VersionNumber.of("3.5.15"));
        assertThat(targets.patch()).contains(VersionNumber.of("3.5.16"));
        assertThat(targets.minor()).isEmpty();
    }

    @Test
    void one_advisory_on_two_lines_of_one_spring_boot_is_one_finding_at_its_worst_verdict() {
        var line = new ExplorerFront.Line("1.0", VersionNumber.of("1.0.1"), Optional.empty(), List.of(),
                List.of(spring("CVE-1", "false_positive"), spring("CVE-2", null)), Optional.empty(), null,
                Optional.of(VersionNumber.of("9.9.9")), Optional.empty(), List.of(), List.of(), null, Optional.empty());
        var other = new ExplorerFront.Line("1.1", VersionNumber.of("1.1.1"), Optional.empty(), List.of(),
                List.of(spring("CVE-1", "risk")), Optional.empty(), null, Optional.of(VersionNumber.of("9.9.9")),
                Optional.empty(), List.of(), List.of(), null, Optional.empty());
        var front = new ExplorerFront(Optional.empty(), List.of(line, other), List.of(), Optional.empty(), null, null,
                null, null, null);

        List<SpringRules.Boot> boots = SpringRules.byBoot(front);

        assertThat(boots).hasSize(1);
        assertThat(boots.getFirst().lines()).containsExactly("1.0", "1.1");
        assertThat(boots.getFirst().differs()).isTrue();
        assertThat(boots.getFirst().findings()).extracting(Finding::cve, finding -> SpringRules.kind(finding))
                .containsExactly(org.assertj.core.groups.Tuple.tuple("CVE-1", SpringRules.Kind.RISK),
                        org.assertj.core.groups.Tuple.tuple("CVE-2", SpringRules.Kind.UNJUDGED));
    }

    @Test
    void a_ghsa_links_to_github_because_nvd_holds_no_ghsa() {
        assertThat(SpringRules.recordUrl("CVE-2025-24813")).isEqualTo("https://nvd.nist.gov/vuln/detail/CVE-2025-24813");
        assertThat(SpringRules.recordUrl("GHSA-abcd-efgh-ijkl"))
                .isEqualTo("https://github.com/advisories/GHSA-abcd-efgh-ijkl");
    }

    private static Finding spring(String cve, String verdict) {
        return new Finding(cve, Optional.of("maven"), List.of(), Optional.of("spring-boot"), Optional.of("high"),
                Optional.ofNullable(verdict), Optional.empty(), null);
    }
}

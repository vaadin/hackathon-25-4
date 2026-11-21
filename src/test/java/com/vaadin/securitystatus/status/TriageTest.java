package com.vaadin.securitystatus.status;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.vaadin.securitystatus.data.FixturePages;
import com.vaadin.securitystatus.data.SecurityDataService;
import com.vaadin.securitystatus.data.VersionNumber;
import com.vaadin.securitystatus.data.model.ScanIndex;
import com.vaadin.securitystatus.data.model.VersionAnswer;

/**
 * SCN-05 and AC2: the triage of the osv-scanner fixture against 24.9.21, whose
 * upgrade to 24.9.26 clears everything of Vaadin's. Owners follow rule 6.11.14,
 * Spring Boot only where Spring Boot can fix it, and every action names who
 * moves.
 */
class TriageTest {

    private final SecurityDataService data = FixturePages.offline();
    private final ScanIndex scan = data.scanIndex();
    private final Optional<VersionAnswer> on24921 = data.version(VersionNumber.of("24.9.21"));

    private Triage.Result triage(String fixture) {
        return Triage.of(ReportParserTest.read(fixture), scan, on24921, Optional.of("24.9.21"));
    }

    private static Triage.Row row(Triage.Result result, String advisory) {
        return result.rows().stream().filter(row -> row.advisory().equals(advisory)).findFirst().orElseThrow();
    }

    // SCN-05
    @Test
    void owners_follow_the_rule_and_actions_name_who_moves() {
        Triage.Result result = triage("osv-scanner.json");

        Triage.Row fixable = row(result, "CVE-2026-89407");
        assertThat(fixable.bucket()).isEqualTo(Triage.Bucket.FIX_AVAILABLE);
        assertThat(fixable.owner()).isEqualTo(Triage.Owner.VAADIN);
        assertThat(fixable.action()).isEqualTo(Triage.Action.UPGRADE);
        assertThat(fixable.arguments()).containsExactly("24.9.26");

        // Pinned by Spring Boot on this version: Spring Boot's to fix, though Vaadin ships the package.
        Triage.Row pinned = row(result, "CVE-2026-45292");
        assertThat(pinned.owner()).isEqualTo(Triage.Owner.SPRING_BOOT);
        assertThat(pinned.bucket()).isEqualTo(Triage.Bucket.UNRESOLVED);
        assertThat(pinned.action()).isEqualTo(Triage.Action.SPRING_BOOT);
        assertThat(pinned.arguments()).containsExactly("opentelemetry-api");

        // In theirs on this version.
        assertThat(row(result, "CVE-2026-47886").owner()).isEqualTo(Triage.Owner.SPRING_BOOT);

        // Fixed upstream and brought only by a discontinued product: the library's, and never secure.
        Triage.Row discontinued = row(result, "CVE-2026-8763");
        assertThat(discontinued.owner()).isEqualTo(Triage.Owner.LIBRARY);
        assertThat(discontinued.bucket()).isEqualTo(Triage.Bucket.UNRESOLVED);
        assertThat(discontinued.action()).isEqualTo(Triage.Action.DISCONTINUED);
    }

    @Test
    void before_a_version_is_named_spring_boot_is_only_an_advisory_whose_one_origin_is_spring_boot() {
        Triage.Result result = Triage.of(ReportParserTest.read("osv-scanner.json"), scan, Optional.empty(),
                Optional.empty());

        // Origins spring-boot and vaadin: not Spring Boot's alone.
        assertThat(row(result, "CVE-2026-47886").owner()).isEqualTo(Triage.Owner.VAADIN);
        // Origins vaadin, fix owner upstream: the library's, and not Spring Boot's.
        assertThat(row(result, "CVE-2026-45292").owner()).isEqualTo(Triage.Owner.LIBRARY);
        assertThat(result.rows()).allMatch(row -> row.bucket() == Triage.Bucket.UNRESOLVED)
                .filteredOn(row -> row.owner() != Triage.Owner.YOURS && row.pending().isEmpty())
                .allMatch(row -> row.action() == Triage.Action.NAME_VERSION);
        assertThat(Triage.owner("CVE-2022-31691", scan.advisories().get("CVE-2022-31691"), null))
                .as("an advisory whose only origin is Spring Boot").isEqualTo(Triage.Owner.SPRING_BOOT);
    }

    // AC2: a measured, ruled out finding lands in already secure with the verdict named
    @Test
    void a_ruled_out_finding_is_already_secure_with_its_verdict_named() {
        Triage.Row ruledOut = row(triage("osv-scanner.json"), "CVE-2021-33604");

        assertThat(ruledOut.bucket()).isEqualTo(Triage.Bucket.SECURE);
        assertThat(ruledOut.action()).isEqualTo(Triage.Action.RULED_OUT);
        assertThat(ruledOut.verdict()).contains("false_positive");

        Triage.Row fromBuild = row(triage("osv-scanner.json"), "CVE-2025-67030");
        assertThat(fromBuild.bucket()).isEqualTo(Triage.Bucket.SECURE);
        assertThat(fromBuild.action()).isEqualTo(Triage.Action.FROM_BUILD);
    }

    // AC2: an unmeasured coordinate lands with owner yours and is never called secure
    @Test
    void a_coordinate_nobody_measured_is_yours_and_never_secure() {
        Triage.Row yours = row(triage("osv-scanner.json"), "CVE-2099-0001");

        assertThat(yours.owner()).isEqualTo(Triage.Owner.YOURS);
        assertThat(yours.bucket()).isEqualTo(Triage.Bucket.UNRESOLVED);
        assertThat(yours.pending()).isEmpty();
        assertThat(yours.verdict()).isEmpty();
        assertThat(yours.action()).isEqualTo(Triage.Action.NOT_OURS);

        // An advisory we never measured, on a coordinate Vaadin brings: ours to answer, not answered yet.
        Triage.Row unmeasured = row(triage("osv-scanner.json"), "CVE-2099-0002");
        assertThat(unmeasured.owner()).isEqualTo(Triage.Owner.VAADIN);
        assertThat(unmeasured.pending()).contains(Triage.Pending.UNMEASURED);
        assertThat(unmeasured.bucket()).isEqualTo(Triage.Bucket.UNRESOLVED);
    }

    @Test
    void an_advisory_under_review_on_a_line_with_nothing_to_move_to_is_pending_review() {
        // 24.7.19 has no upgrade; CVE-2026-45292 is a real risk there that nothing clears.
        var on24719 = data.version(VersionNumber.of("24.7.19"));
        Triage.Result result = Triage.of(ReportParserTest.read("osv-scanner.json"), scan, on24719,
                Optional.of("24.7.19"));

        assertThat(result.count(Triage.Bucket.FIX_AVAILABLE)).isZero();
        Triage.Row stuck = row(result, "CVE-2026-45292");
        assertThat(stuck.bucket()).isEqualTo(Triage.Bucket.UNRESOLVED);
        assertThat(stuck.owner()).isEqualTo(Triage.Owner.LIBRARY);
        assertThat(stuck.action()).isEqualTo(Triage.Action.NEWER_LIBRARY);
        assertThat(stuck.arguments()).containsExactly("opentelemetry-api", "24.7.19");
    }

    @Test
    void a_bill_of_materials_is_answered_with_every_advisory_measured_on_its_coordinates() {
        Triage.Result result = Triage.of(ReportParserTest.read("cyclonedx-components-only.json"), scan, on24921,
                Optional.of("24.9.21"));

        assertThat(result.rows()).extracting(Triage.Row::advisory).contains("CVE-2021-33604", "CVE-2022-4244",
                "CVE-2025-67030", "CVE-2022-4245", "CVE-2026-2742", "CVE-2026-2741");
        assertThat(result.rows()).noneMatch(row -> row.coordinate().contains("shop-core"));
    }

    @Test
    void the_same_report_twice_is_the_same_answer() {
        assertThat(triage("osv-scanner.json")).isEqualTo(triage("osv-scanner.json"));
    }

    // 6.11.18: what is assessed, counted against the file in hand
    @Test
    void the_scope_counts_the_coordinates_of_the_file_against_the_measured_ones() {
        Triage.Scope findings = Triage.scope(ReportParserTest.read("osv-scanner.json"), scan);
        assertThat(findings.inventory()).isFalse();
        assertThat(findings.listed()).isEqualTo(9);
        assertThat(findings.known()).isEqualTo(8);
        assertThat(findings.unknownExamples()).containsExactly("billing-core");

        Triage.Scope bom = Triage.scope(ReportParserTest.read("cyclonedx-components-only.json"), scan);
        assertThat(bom.inventory()).isTrue();
        assertThat(bom.listed()).isEqualTo(6);
        assertThat(bom.known()).isEqualTo(3);
        assertThat(bom.unknown()).isEqualTo(3);
        assertThat(bom.unknownExamples()).isEqualTo(List.of("router", "shop-core", "shop"));
    }
}

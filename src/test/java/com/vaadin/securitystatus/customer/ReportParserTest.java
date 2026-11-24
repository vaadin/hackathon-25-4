package com.vaadin.securitystatus.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.vaadin.securitystatus.data.FixturePages;
import com.vaadin.securitystatus.data.SecurityDataService;
import com.vaadin.securitystatus.data.VersionNumber;

/**
 * SCN-02 and AC1: the four formats, recognised by their shape, read into the
 * same findings where they encode the same scan, and refused with a reason
 * when they are none of them. The fixtures under {@code src/test/resources/scan}
 * are written by hand, one per format, all encoding one scan of an application
 * on 24.9.21.
 */
class ReportParserTest {

    /** The scan every format encodes: advisory to coordinate without its version. */
    static final Map<String, String> THE_SCAN = Map.of(
            "CVE-2026-89407", "pkg:maven/com.fasterxml.jackson.core/jackson-core",
            "CVE-2021-33604", "pkg:maven/com.vaadin/vaadin-swing-kit-flow",
            "CVE-2026-45292", "pkg:maven/io.opentelemetry/opentelemetry-api",
            "CVE-2026-8763", "pkg:maven/org.bouncycastle/bcprov-jdk18on",
            "CVE-2025-67030", "pkg:maven/org.codehaus.plexus/plexus-utils",
            "CVE-2026-47886", "pkg:maven/org.springframework/spring-web",
            "CVE-2099-0002", "pkg:maven/com.vaadin/flow-server",
            "CVE-2099-0001", "pkg:maven/com.example/billing-core",
            "CVE-2026-2742", "pkg:maven/com.vaadin/vaadin");

    static final Map<ReportFormat, String> FIXTURES = Map.of(ReportFormat.OSV_SCANNER, "osv-scanner.json",
            ReportFormat.DEPENDENCY_CHECK, "dependency-check.json", ReportFormat.BOMBER, "bomber.json",
            ReportFormat.CYCLONEDX, "cyclonedx.json");

    private final SecurityDataService data = FixturePages.offline();

    static ScannerReport read(String fixture) {
        try (InputStream in = ReportParserTest.class.getResourceAsStream("/scan/" + fixture)) {
            return ReportParser.parse(in);
        } catch (IOException failed) {
            throw new UncheckedIOException(failed);
        } catch (ReportParser.RefusedException refused) {
            throw new AssertionError(fixture + " was refused: " + refused.refusal(), refused);
        }
    }

    // SCN-02, AC1
    @ParameterizedTest
    @EnumSource(ReportFormat.class)
    void each_format_is_detected_by_its_shape_and_reads_the_same_findings(ReportFormat format) {
        ScannerReport report = read(FIXTURES.get(format));

        assertThat(report.format()).isEqualTo(format);
        assertThat(report.findings()).hasSize(THE_SCAN.size());
        Map<String, String> found = report.findings().stream().collect(Collectors
                .toMap(finding -> finding.ids().getFirst(), finding -> ReportParser.bare(finding.coordinate())));
        assertThat(found).isEqualTo(THE_SCAN);
        assertThat(report.skipped()).isEmpty();
    }

    // SCN-02
    @Test
    void the_three_vulnerability_reports_triage_into_the_same_buckets_and_cyclonedx_adds_to_them() {
        var version = data.version(VersionNumber.of("24.9.21"));
        Map<ReportFormat, Triage.Result> results = new java.util.EnumMap<>(ReportFormat.class);
        FIXTURES.forEach((format, fixture) -> results.put(format,
                Triage.of(read(fixture), data.scanIndex(), version, Optional.of("24.9.21"))));

        Triage.Result osv = results.get(ReportFormat.OSV_SCANNER);
        assertThat(osv.count(Triage.Bucket.SECURE)).isEqualTo(3);
        assertThat(osv.count(Triage.Bucket.FIX_AVAILABLE)).isEqualTo(1);
        assertThat(osv.count(Triage.Bucket.UNRESOLVED)).isEqualTo(5);
        for (ReportFormat format : List.of(ReportFormat.DEPENDENCY_CHECK, ReportFormat.BOMBER)) {
            assertThat(answers(results.get(format))).as(format.name()).isEqualTo(answers(osv));
        }
        Triage.Result cyclone = results.get(ReportFormat.CYCLONEDX);
        assertThat(answers(cyclone)).containsAll(answers(osv));
        assertThat(cyclone.rows()).hasSizeGreaterThan(osv.rows().size());
    }

    /** Each row reduced to what the buckets, owners and actions say about it. */
    private static Set<String> answers(Triage.Result result) {
        return result.rows().stream().map(row -> row.advisory() + " " + ReportParser.bare(row.coordinate()) + " "
                + row.bucket() + " " + row.owner() + " " + row.action()).collect(Collectors.toSet());
    }

    @Test
    void the_version_is_read_from_the_coordinates_that_carry_it_and_only_from_those() {
        assertThat(read("dependency-check.json").vaadin()).contains("24.9.21");
        assertThat(read("cyclonedx.json").vaadin()).contains("24.9.21");
        assertThat(read("cyclonedx-components-only.json").vaadin()).contains("24.9.21");
        assertThat(read("osv-scanner.json").vaadin()).contains("24.9.21");
        // flow-server and the swing kit carry 24.9.21 too, and are not the platform's version.
        assertThat(read("bomber-no-version.json").vaadin()).isEmpty();
        assertThat(ReportParser.platformVersion(
                List.of("pkg:maven/com.vaadin/vaadin@24.9.21", "pkg:maven/com.vaadin/vaadin-core@24.9.20")))
                .as("two releases named is a question, not an answer").isEmpty();
        assertThat(ReportParser.platformVersion(List.of("pkg:maven/com.vaadin/vaadin-server@8.32.3")))
                .contains("8.32.3");
    }

    // The edge case: nested components flattened, coordinates found
    @Test
    void a_cyclonedx_bill_of_materials_is_flattened_nested_components_and_all() {
        ScannerReport report = read("cyclonedx-components-only.json");

        assertThat(report.findings()).isEmpty();
        assertThat(report.components()).containsExactlyInAnyOrder("pkg:maven/com.vaadin/vaadin-core@24.9.21",
                "pkg:maven/com.vaadin/vaadin-swing-kit-flow@24.9.21",
                "pkg:maven/org.codehaus.plexus/plexus-utils@3.5.1", "pkg:npm/@vaadin/router@1.2.0",
                "pkg:maven/com.example/shop-core@2.0.0", "pkg:maven/com.example/shop@2.0.0");
        assertThat(report.size()).isEqualTo(6);
    }

    @Test
    void ids_lead_with_the_cve_and_coordinates_lose_their_qualifiers() {
        assertThat(ReportParser.ids("GHSA-H46C-H94J-95F3", List.of("cve-2026-89407", "NOT PROVIDED")))
                .containsExactly("CVE-2026-89407", "GHSA-h46c-h94j-95f3");
        assertThat(ReportParser.canonical("pkg:npm/%40vaadin%2Frouter@1.2.0?foo=bar#sub"))
                .isEqualTo("pkg:npm/@vaadin/router@1.2.0");
        assertThat(ReportParser.bare("pkg:npm/@vaadin/router@1.2.0")).isEqualTo("pkg:npm/@vaadin/router");
        assertThat(ReportParser.bare("pkg:npm/@vaadin/router")).isEqualTo("pkg:npm/@vaadin/router");
        assertThat(ReportParser.componentName("pkg:maven/com.fasterxml.jackson.core/jackson-core@2.18.2"))
                .isEqualTo("jackson-core");
    }

    // S8: skipped entries are accounted for
    @Test
    void entries_with_findings_and_no_coordinate_are_named_as_skipped() {
        ScannerReport report = read("dependency-check-skipped.json");

        assertThat(report.findings()).hasSize(1);
        assertThat(report.skipped()).containsExactly("legacy-tools.zip", "bundled-js/app.min.js");
    }

    @Test
    void a_report_with_no_findings_is_still_a_report() {
        ScannerReport report = read("osv-scanner-empty.json");

        assertThat(report.format()).isEqualTo(ReportFormat.OSV_SCANNER);
        assertThat(report.findings()).isEmpty();
        assertThat(report.size()).isZero();
    }

    // SCN-04, the refusal half
    @Test
    void json_of_another_kind_and_text_that_is_not_json_are_refused_with_their_reason() {
        assertThatThrownBy(() -> parse("/scan/not-a-report.json")).isInstanceOf(ReportParser.RefusedException.class)
                .extracting(refused -> ((ReportParser.RefusedException) refused).refusal())
                .isEqualTo(ReportParser.Refusal.UNKNOWN_FORMAT);
        assertThatThrownBy(() -> parse("/scan/not-json.txt")).isInstanceOf(ReportParser.RefusedException.class)
                .extracting(refused -> ((ReportParser.RefusedException) refused).refusal())
                .isEqualTo(ReportParser.Refusal.NOT_JSON);
        assertThatThrownBy(() -> ReportParser.parse(new ByteArrayInputStream("[1, 2]".getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(ReportParser.RefusedException.class);
        assertThatThrownBy(() -> ReportParser.parse(new ByteArrayInputStream(new byte[0])))
                .isInstanceOf(ReportParser.RefusedException.class);
    }

    @Test
    void a_stream_longer_than_allowed_is_refused_as_too_large_whatever_it_holds() {
        byte[] body = ("{\"results\":[" + " ".repeat(4096) + "]}").getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> ReportParser.parse(new ByteArrayInputStream(body), 1024))
                .isInstanceOf(ReportParser.RefusedException.class)
                .extracting(refused -> ((ReportParser.RefusedException) refused).refusal())
                .isEqualTo(ReportParser.Refusal.TOO_LARGE);
    }

    @Test
    void a_report_that_multiplies_into_more_findings_than_one_triage_reads_is_refused() {
        var packages = new StringBuilder();
        var vulnerabilities = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            packages.append(i == 0 ? "" : ",").append("{\"id\":\"pkg:maven/g/a").append(i).append("@1\"}");
            vulnerabilities.append(i == 0 ? "" : ",").append("{\"name\":\"CVE-2026-").append(i)
                    .append("\",\"severity\":\"HIGH\"}");
        }
        String report = "{\"scanInfo\":{},\"dependencies\":[{\"packages\":[" + packages
                + "],\"vulnerabilities\":[" + vulnerabilities + "]}]}";

        assertThatThrownBy(() -> ReportParser.parse(new ByteArrayInputStream(report.getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(ReportParser.RefusedException.class)
                .extracting(refused -> ((ReportParser.RefusedException) refused).refusal())
                .isEqualTo(ReportParser.Refusal.TOO_MANY_FINDINGS);
    }

    @Test
    void a_coordinate_too_long_to_be_one_is_listed_as_skipped_and_a_long_severity_is_dropped() throws Exception {
        String long_ = "pkg:maven/g/" + "a".repeat(ReportParser.MAX_TEXT) + "@1";
        String report = "{\"scanInfo\":{},\"dependencies\":["
                + "{\"packages\":[{\"id\":\"" + long_ + "\"}],\"vulnerabilities\":[{\"name\":\"CVE-2026-1\"}]},"
                + "{\"packages\":[{\"id\":\"pkg:maven/g/b@1\"}],\"vulnerabilities\":[{\"name\":\"CVE-2026-2\","
                + "\"severity\":\"" + "=".repeat(ReportParser.MAX_TEXT + 1) + "\"}]}]}";

        ScannerReport read = ReportParser.parse(new ByteArrayInputStream(report.getBytes(StandardCharsets.UTF_8)));

        assertThat(read.findings()).singleElement().satisfies(finding -> {
            assertThat(finding.coordinate()).isEqualTo("pkg:maven/g/b@1");
            assertThat(finding.severity()).isEmpty();
        });
        assertThat(read.skipped()).singleElement().satisfies(name -> assertThat(name).hasSize(ReportParser.MAX_TEXT + 1));
    }

    private static ScannerReport parse(String resource) throws Exception {
        try (InputStream in = ReportParserTest.class.getResourceAsStream(resource)) {
            return ReportParser.parse(in);
        }
    }
}

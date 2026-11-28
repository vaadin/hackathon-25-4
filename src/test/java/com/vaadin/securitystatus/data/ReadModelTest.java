package com.vaadin.securitystatus.data;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.vaadin.securitystatus.data.model.Assessment;
import com.vaadin.securitystatus.data.model.CasesIndex;
import com.vaadin.securitystatus.data.model.CveAnswer;
import com.vaadin.securitystatus.data.model.ExplorerFront;
import com.vaadin.securitystatus.data.model.ExplorerVersion;
import com.vaadin.securitystatus.data.model.VerdictQueue;
import com.vaadin.securitystatus.data.model.VersionAnswer;

/**
 * The read models against the committed fixtures, whose index is pinned at
 * {@code generated_at} 2026-11-21T10:13:39Z, and against documents with fields
 * taken away.
 */
class ReadModelTest {

    private final FixtureTransport host = new FixtureTransport();
    private final SecurityDataService service = service();

    private SecurityDataService service() {
        var client = new PagesClient(FixtureTransport.BASE_URL, host);
        return new SecurityDataService(new PagesCache(client, Clock.systemUTC()), client,
                new DataRefreshSignal(), Runnable::run);
    }

    // DAT-06
    @Test
    void an_assessment_with_no_workaround_reports_it_absent_and_not_empty() {
        VersionAnswer answer = service.version(VersionNumber.of("24.9.21")).orElseThrow();
        // The control first: the fixture writes the field as an empty text, and another assessment has one.
        assertThat(host.fixture("cases/v/24.9.21.json")).contains("\"workaround\":\"\"");
        Assessment with = answer.assessments().get("maven-com-fasterxml-jackson-core-jackson-core-24-9-number-token");
        assertThat(with.workaround()).hasValueSatisfying(text -> assertThat(text).startsWith("Set <jackson-bom"));

        Assessment without = answer.assessments().get("npm-opentelemetry-core");

        assertThat(without.workaround()).isEmpty();
        assertThat(without.fixExpected()).isEmpty();
        assertThat(without.handover()).isEmpty();
        assertThat(without.impact()).isPresent();
    }

    // DAT-06
    @Test
    void a_field_that_disappears_is_absent_and_nothing_throws() {
        host.replace("cases/v/1.2.3.json", """
                {"version": "1.2.3", "assessments": {"bare": {}, "nulls": {"workaround": null, "cves": null}}}
                """);

        VersionAnswer answer = service.version(VersionNumber.of("1.2.3")).orElseThrow();

        assertThat(answer.version()).hasToString("1.2.3");
        assertThat(answer.state()).isEmpty();
        assertThat(answer.scannedAt()).isEmpty();
        assertThat(answer.findings()).isNull();
        assertThat(answer.risk()).isEmpty();
        assertThat(answer.upgrade()).isEmpty();
        assertThat(answer.springBoot()).isEmpty();
        assertThat(answer.verdicts()).isEmpty();
        for (Assessment assessment : answer.assessments().values()) {
            assertThat(assessment.workaround()).isEmpty();
            assertThat(assessment.impact()).isEmpty();
            assertThat(assessment.assessedAt()).isEmpty();
            assertThat(assessment.cves()).isEmpty();
            assertThat(assessment.fixPrs()).isEmpty();
        }
    }

    @Test
    void a_field_nobody_knows_yet_is_ignored() {
        host.replace("cases/v/1.2.3.json", """
                {"version": "1.2.3", "state": "secure", "added_next_month": {"deep": [1, 2]}, "upgrade": {"to": "1.2.4", "new_too": true}}
                """);

        VersionAnswer answer = service.version(VersionNumber.of("1.2.3")).orElseThrow();

        assertThat(answer.state()).contains("secure");
        assertThat(answer.upgrade().orElseThrow().to()).contains(VersionNumber.of("1.2.4"));
    }

    @Test
    void the_index_maps_as_published() {
        CasesIndex index = service.index();

        assertThat(index.generatedAt()).isEqualTo(FixturePages.PINNED_GENERATED_AT);
        assertThat(index.labels()).containsEntry("risk", "affects you").containsEntry("in_triage",
                "not assessed yet");
        assertThat(index.originLabels()).containsEntry("spring-boot", "comes from your Spring Boot");
        assertThat(index.lines()).extracting(CasesIndex.Line::line).containsExactly("25.4", "25.3", "24.10", "24.9",
                "24.7", "24.4", "23.6", "14.14", "8.32", "7.7");
        assertThat(index.counts().orElseThrow().cves()).isEqualTo(535);
        assertThat(index.advisories()).hasSize(535);
        assertThat(index.recent()).hasSize(12);
        assertThat(index.releaseRepos()).containsEntry("default", "vaadin/platform");

        CasesIndex.Line prerelease = index.lines().getFirst();
        assertThat(prerelease.version()).isEqualTo(VersionNumber.of("25.4.0-beta2"));
        assertThat(prerelease.isPrerelease()).isTrue();
        assertThat(prerelease.findings()).isEqualTo(13);
        assertThat(prerelease.scannedAt()).contains(OffsetDateTime.parse("2026-11-21T03:24:43+00:00"));
        assertThat(prerelease.support().orElseThrow().label()).contains("Free maintenance");
        assertThat(index.lines().get(1).isPrerelease()).isFalse();

        CasesIndex.Summary summary = index.summaryOf(VersionNumber.of("24.9.21")).orElseThrow();
        assertThat(summary.state()).contains("fix_available");
        assertThat(summary.upgradeTo()).contains(VersionNumber.of("24.9.26"));
        assertThat(summary.risk()).isEqualTo(1);
        // Present and null in the document, so absent here.
        assertThat(index.summaryOf(VersionNumber.of("25.3.5")).orElseThrow().upgradeTo()).isEmpty();
        assertThat(index.summaryOf(VersionNumber.of("0.0.0"))).isEmpty();

        Assessment newest = index.recent().getFirst();
        assertThat(newest.id()).isPresent();
        assertThat(newest.assessedAt()).contains(LocalDate.parse("2026-11-17"));
        assertThat(newest.cves()).isNotEmpty();
    }

    @Test
    void a_version_answer_maps_as_published() {
        VersionAnswer answer = service.version(VersionNumber.of("24.9.21")).orElseThrow();

        assertThat(answer.line()).contains("24.9");
        assertThat(answer.state()).contains("fix_available");
        assertThat(answer.rawState()).contains("vulnerable");
        assertThat(answer.findings()).isEqualTo(135);
        assertThat(answer.risk()).hasSize(1);
        assertThat(answer.triage()).hasSize(11);
        assertThat(answer.ruledOut()).hasSize(76);
        assertThat(answer.fixable()).hasSize(12);
        assertThat(answer.stuck()).isEmpty();
        assertThat(answer.springBoot()).contains(VersionNumber.of("3.5.15"));
        VersionAnswer.Upgrade upgrade = answer.upgrade().orElseThrow();
        assertThat(upgrade.to()).contains(VersionNumber.of("24.9.26"));
        assertThat(upgrade.clears()).isEqualTo(12);
        assertThat(upgrade.remains()).isZero();
        assertThat(answer.verdicts()).isNotEmpty().allSatisfy((id, verdict) -> assertThat(verdict.state()).isPresent());

        assertThat(service.version(VersionNumber.of("25.3.5")).orElseThrow().upgrade()).isEmpty();
        assertThat(service.version(VersionNumber.of("24.4.28")).orElseThrow().state()).contains("unresolved");
    }

    @Test
    void an_advisory_maps_as_published_and_keeps_its_case() {
        CveAnswer advisory = service.cve("CVE-2019-25028").orElseThrow();

        assertThat(advisory.cve()).isEqualTo("CVE-2019-25028");
        assertThat(advisory.severity()).contains("moderate");
        assertThat(advisory.summary()).isPresent();
        assertThat(advisory.coverage()).containsKeys("25.4", "7.7");
        CveAnswer.Coverage clean = advisory.coverage().get("25.4");
        assertThat(clean.status()).contains("clean");
        assertThat(clean.fixedIn()).isEmpty();
        assertThat(clean.fixMeans()).isEmpty();
        CveAnswer.Coverage fixed = advisory.coverage().get("7.7");
        assertThat(fixed.fixedIn()).contains(VersionNumber.of("7.7.23"));
        assertThat(fixed.fixMeans().orElseThrow().changes()).isNotEmpty()
                .allSatisfy(change -> assertThat(change.packageName()).isPresent());

        CveAnswer spring = service.cve("CVE-2016-1000027").orElseThrow();
        assertThat(spring.aliases()).contains("GHSA-4wrc-f8pq-fpqp");
        assertThat(spring.assessments()).isNotEmpty();
        assertThat(spring.onVersions()).isNotEmpty();
        // The host is case sensitive and so is an id.
        assertThat(service.cve("cve-2016-1000027")).isEmpty();
    }

    @Test
    void the_scan_index_and_the_case_catalogue_map_as_published() {
        var scan = service.scanIndex();
        assertThat(scan.coordinates()).hasSize(101);
        assertThat(scan.coordinates().get("pkg:maven/org.springframework/spring-web")).containsEntry(
                "CVE-2016-1000027", "risk");
        assertThat(scan.advisories().get("CVE-2016-1000027").severity()).contains("critical");

        var catalog = service.cases();
        assertThat(catalog.generatedAt()).contains(FixturePages.PINNED_GENERATED_AT);
        assertThat(catalog.cases()).hasSize(39);
        assertThat(catalog.cases().getFirst().id()).isEqualTo("version.secure");
        assertThat(catalog.cases().getFirst().example()).contains("#version/7.7.50");
    }

    @Test
    void the_explorer_documents_map_as_published() {
        ExplorerFront front = service.explorerFront();
        assertThat(front.lines()).hasSize(10);
        assertThat(front.moduleNames()).hasSize(35).startsWith("vaadin-server");
        assertThat(front.profile().orElseThrow().discontinued()).containsExactly("control-center-starter");
        ExplorerFront.Line newest = front.lines().getFirst();
        assertThat(newest.scannerFiles()).isTrue();
        assertThat(newest.noSpring()).isNull();
        assertThat(newest.findings()).hasSize(13);
        assertThat(newest.findings().getFirst().modules()).containsExactly(16);
        assertThat(front.lines().getLast().noSpring()).isTrue();
        assertThat(front.lines().getLast().springBoot()).isEmpty();
        assertThat(front.springBootTried().get("4.1.1").to()).contains(VersionNumber.of("4.1.2"));

        ExplorerVersion release = service.explorerVersion(VersionNumber.of("25.3.5")).orElseThrow();
        assertThat(release.counts()).containsEntry("total", 15).containsEntry("ruled_out", 14);
        assertThat(release.findings()).hasSize(15);
        ExplorerVersion.Finding finding = release.findings().getFirst();
        assertThat(finding.cve()).isEqualTo("CVE-2021-33604");
        assertThat(finding.fixExpected()).isEmpty();
        assertThat(finding.packages().getFirst().packageName()).isEqualTo("com.vaadin:vaadin-swing-kit-flow");
        assertThat(finding.packages().getFirst().workaround()).isEmpty();
        assertThat(release.fromSpringBoot().orElseThrow().state()).contains("safe");
        assertThat(service.explorerVersion(VersionNumber.of("0.0.0"))).isEmpty();
    }

    // A release record whose packages name who declared which version, as data/24.9.26.json does on
    // 2026-11-21T10:13:39Z: objects, not texts. The 25.3.5 fixture carries none, so it is given here.
    @Test
    void a_declared_version_is_read_as_who_asked_for_what() {
        host.replace("data/9.9.9.json", """
                {"version": "9.9.9", "findings": [{"cve": "CVE-2026-1", "packages": [{"package": "io.vertx:vertx-core",
                 "declared": [{"by": "kubernetes-httpclient-vertx@7.3.2", "version": "4.5.25", "won": false}],
                 "declared_without_spring": null}]}]}
                """);

        ExplorerVersion release = service.explorerVersion(VersionNumber.of("9.9.9")).orElseThrow();

        ExplorerVersion.PackageFinding coordinate = release.findings().getFirst().packages().getFirst();
        assertThat(coordinate.declared()).singleElement().satisfies(declared -> {
            assertThat(declared.by()).contains("kubernetes-httpclient-vertx@7.3.2");
            assertThat(declared.version()).contains("4.5.25");
            assertThat(declared.won()).isFalse();
        });
        assertThat(coordinate.declaredWithoutSpring()).isEmpty();
    }

    @Test
    void the_queue_maps_as_published() {
        VerdictQueue queue = service.queue();

        assertThat(queue.packages()).hasSize(101);
        assertThat(queue.assessments()).hasSize(213);
        assertThat(queue.heads()).containsEntry("24.9", VersionNumber.of("24.9.26"));
        VerdictQueue.QueueEntry first = queue.packages().getFirst();
        assertThat(first.packageName()).isEqualTo("com.fasterxml.jackson.core:jackson-core");
        assertThat(first.closes()).isZero();
        assertThat(first.cves()).isNotEmpty();
        assertThat(first.cves().getFirst().byLine()).containsEntry("14.14", "risk");
        // A line the package was never measured on carries null, and stays null.
        assertThat(first.acknowledged()).containsEntry("7.7", null).containsEntry("24.9", List.of());
    }
}

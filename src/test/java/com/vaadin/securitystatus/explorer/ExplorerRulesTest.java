package com.vaadin.securitystatus.explorer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.vaadin.securitystatus.data.FixturePages;
import com.vaadin.securitystatus.data.VersionNumber;
import com.vaadin.securitystatus.data.model.ExplorerFront;
import com.vaadin.securitystatus.data.model.ExplorerFront.Finding;

/** The explorer's rules without components: groups, the cap, the search and the registry. */
class ExplorerRulesTest {

    private final ExplorerFront front = FixturePages.offline().explorerFront();

    private static Finding finding(String cve, String owner, String verdict, Integer... modules) {
        return new Finding(cve, Optional.of("maven"), List.of(modules), Optional.of(owner), Optional.of("high"),
                Optional.ofNullable(verdict), Optional.empty(), null);
    }

    // AC2: the 150 cap counts its remainder
    @Test
    void a_group_is_capped_at_150_with_the_rest_counted() {
        var findings = new ArrayList<Finding>();
        for (int i = 0; i < 163; i++) {
            findings.add(finding("CVE-2026-%05d".formatted(i), "vaadin", "risk", 0));
        }

        ExplorerRules.Capped capped = ExplorerRules.capped(findings);

        assertThat(capped.shown()).hasSize(150);
        assertThat(capped.more()).isEqualTo(13);
        assertThat(ExplorerRules.capped(findings.subList(0, 150)).more()).isZero();
    }

    @Test
    void owner_comes_before_the_verdict_and_only_risk_is_to_act_on() {
        assertThat(ExplorerRules.group(finding("A", "build", "risk"))).isEqualTo(ExplorerRules.Group.BUILD);
        assertThat(ExplorerRules.group(finding("A", "spring-boot", null))).isEqualTo(ExplorerRules.Group.SPRING);
        assertThat(ExplorerRules.group(finding("A", "vaadin", null))).isEqualTo(ExplorerRules.Group.UNJUDGED);
        assertThat(ExplorerRules.group(finding("A", "vaadin", "in_triage"))).isEqualTo(ExplorerRules.Group.UNJUDGED);
        assertThat(ExplorerRules.group(finding("A", "vaadin", "risk"))).isEqualTo(ExplorerRules.Group.RISK);
        assertThat(ExplorerRules.group(finding("A", "vaadin", "configuration")))
                .isEqualTo(ExplorerRules.Group.ANSWERED);
    }

    @Test
    void the_groups_match_the_counts_the_release_record_publishes() {
        var line = front.lines().stream().filter(candidate -> candidate.line().equals("25.3")).findFirst()
                .orElseThrow();
        var record = FixturePages.offline().explorerVersion(VersionNumber.of("25.3.5")).orElseThrow();

        var groups = ExplorerRules.grouped(line.findings());

        assertThat(groups.getOrDefault(ExplorerRules.Group.ANSWERED, List.of())).hasSize(record.counts().get("ruled_out"));
        assertThat(groups.getOrDefault(ExplorerRules.Group.SPRING, List.of()))
                .hasSize(record.counts().get("from_spring_boot"));
        assertThat(groups).doesNotContainKey(ExplorerRules.Group.RISK);
    }

    @Test
    void a_full_version_is_recognised_in_the_spellings_people_use() {
        assertThat(ExplorerRules.fullVersion("24.9.21")).contains(VersionNumber.of("24.9.21"));
        assertThat(ExplorerRules.fullVersion(" V24.9.21 ")).contains(VersionNumber.of("24.9.21"));
        assertThat(ExplorerRules.fullVersion("Vaadin 25.4.0-beta2")).contains(VersionNumber.of("25.4.0-beta2"));
        assertThat(ExplorerRules.fullVersion("24.9")).isEmpty();
        assertThat(ExplorerRules.fullVersion("jackson")).isEmpty();
    }

    @Test
    void the_registry_keeps_usage_order_and_the_catalogue_the_profiles_order() {
        var products = ExplorerRules.products(front);
        var catalogue = ExplorerAnswer.catalogue(products, front);

        assertThat(products.get(0).name()).isEqualTo("vaadin-server");
        // D-206: what comes with Vaadin first, then the choices, then what Vaadin no longer develops.
        assertThat(catalogue.get(0).name()).isEqualTo("vaadin");
        assertThat(catalogue.getLast().name()).isEqualTo("control-center-starter");
        assertThat(catalogue).extracting(ExplorerRules.Product::name).doesNotContain("@vaadin/router",
                "vaadin-server");
        assertThat(ExplorerAnswer.defaultOff(catalogue)).containsExactly("control-center-starter");
        assertThat(ExplorerAnswer.known(List.of("vaadin-testbench", "nope", "vaadin", "flow-webpush"), catalogue))
                .containsExactly("flow-webpush", "vaadin-testbench");
    }

    // D-205: the half a Vaadin release moves and the half Spring Boot brings or pins
    @Test
    void a_version_spring_boot_pins_is_on_the_other_badge() {
        var pinned = new Finding("A", Optional.of("maven"), List.of(0), Optional.of("vaadin"), Optional.of("high"),
                Optional.of("risk"), Optional.empty(), true);
        var ours = finding("B", "vaadin", "false_positive", 0);
        var spring = finding("C", "spring-boot", "risk", 0);

        assertThat(ExplorerRules.vaadinBadge(List.of(pinned, ours, spring))).isEqualTo(ExplorerRules.Badge.NOT_AFFECTED);
        assertThat(ExplorerRules.springBadge(List.of(pinned, ours, spring))).isEqualTo(ExplorerRules.Badge.VULNERABLE);
        assertThat(ExplorerRules.vaadinBadge(List.of(finding("D", "vaadin", "in_triage", 0))))
                .isEqualTo(ExplorerRules.Badge.UNJUDGED);
    }

    // D-209: a number is a version first, then an id, then a coordinate
    @Test
    void a_search_is_read_as_the_prototype_reads_it() {
        assertThat(ExplorerAnswer.asked("24.9", front).kind()).isEqualTo(ExplorerAnswer.Kind.VERSION);
        assertThat(ExplorerAnswer.asked("24.9", front).series()).containsExactly("24.9");
        assertThat(ExplorerAnswer.asked("CVE-2026-45292", front).kind()).isEqualTo(ExplorerAnswer.Kind.ID);
        assertThat(ExplorerAnswer.asked("jackson", front).kind()).isEqualTo(ExplorerAnswer.Kind.PACKAGE);
        assertThat(ExplorerAnswer.asked("zzz", front).found()).isFalse();
        assertThat(ExplorerAnswer.asked("19.0", front).found()).isFalse();
        assertThat(ExplorerAnswer.asked("", front).kind()).isEqualTo(ExplorerAnswer.Kind.NONE);
    }
}

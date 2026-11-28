package com.vaadin.securitystatus.team.explorer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.vaadin.securitystatus.data.FixturePages;
import com.vaadin.securitystatus.data.VersionNumber;
import com.vaadin.securitystatus.data.model.ExplorerFront.Finding;
import com.vaadin.securitystatus.data.model.ExplorerFront;

/** The explorer's rules without components: the cap, the halves, the search and the registry. */
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
        var catalogue = ExplorerAnswer.catalogue(front);

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

        List<Finding> all = List.of(pinned, ours, spring);

        assertThat(all.stream().filter(finding -> ExplorerRules.ours(finding, false))).containsExactly(ours);
        assertThat(all.stream().filter(ExplorerRules::theirs)).containsExactly(pinned, spring);
        // Without Spring nothing pins anything, so the pinned package is Vaadin's again.
        assertThat(all.stream().filter(finding -> ExplorerRules.ours(finding, true))).containsExactly(pinned, ours);
        assertThat(ExplorerRules.worst(all.stream().filter(finding -> ExplorerRules.ours(finding, false)).toList()))
                .isEqualTo(ExplorerRules.LineState.NOT_AFFECTED);
        assertThat(ExplorerRules.springBadge(all)).isEqualTo(ExplorerRules.LineState.VULNERABLE);
        assertThat(ExplorerRules.worst(List.of(finding("D", "vaadin", "in_triage", 0))))
                .isEqualTo(ExplorerRules.LineState.UNJUDGED);
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

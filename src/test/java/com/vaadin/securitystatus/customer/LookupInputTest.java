package com.vaadin.securitystatus.customer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** The spellings a reader types, AC2. */
class LookupInputTest {

    @Test
    void the_spellings_of_one_version_are_one_question() {
        assertThat(LookupInput.cleanVersion("24.9.20")).isEqualTo("24.9.20");
        assertThat(LookupInput.cleanVersion("V24.9.20")).isEqualTo("24.9.20");
        assertThat(LookupInput.cleanVersion("v24.9.20")).isEqualTo("24.9.20");
        assertThat(LookupInput.cleanVersion("Vaadin 24.9.20")).isEqualTo("24.9.20");
        assertThat(LookupInput.cleanVersion(" vaadin 24.9.20 ")).isEqualTo("24.9.20");
        assertThat(LookupInput.cleanVersion("25.4.0-beta2")).isEqualTo("25.4.0-beta2");
    }

    @Test
    void what_does_not_start_like_a_version_passes_through() {
        assertThat(LookupInput.cleanVersion("Vaadin latest")).isEqualTo("Vaadinlatest");
        assertThat(LookupInput.cleanVersion("vintage")).isEqualTo("vintage");
    }

    @Test
    void an_advisory_id_is_told_from_a_version() {
        assertThat(LookupInput.isAdvisory("CVE-2026-45292")).isTrue();
        assertThat(LookupInput.isAdvisory(" cve-2026-45292")).isTrue();
        assertThat(LookupInput.isAdvisory("GHSA-ccrj-gw8r-854m")).isTrue();
        assertThat(LookupInput.isAdvisory("24.9.20")).isFalse();
        assertThat(LookupInput.isAdvisory("CVE")).isFalse();
    }

    @Test
    void ids_split_on_spaces_commas_semicolons_and_new_lines_once_each() {
        var parsed = LookupInput.splitIds("cve-2026-45292, CVE-2026-45292;GHSA-CCRJ-GW8R-854M\n  foo\tCVE-12");

        assertThat(parsed.ids()).containsExactly("CVE-2026-45292", "GHSA-ccrj-gw8r-854m");
        assertThat(parsed.unrecognised()).containsExactly("foo", "CVE-12");
    }

    @Test
    void an_empty_field_has_no_ids_and_nothing_unrecognised() {
        var parsed = LookupInput.splitIds("  ");

        assertThat(parsed.ids()).isEmpty();
        assertThat(parsed.unrecognised()).isEmpty();
    }
}

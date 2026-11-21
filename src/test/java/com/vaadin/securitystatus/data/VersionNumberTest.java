package com.vaadin.securitystatus.data;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class VersionNumberTest {

    // DAT-07
    @Test
    void versions_sort_numerically_and_not_as_text() {
        List<String> sorted = Stream.of("24.2", "24.10", "24.9.21").map(VersionNumber::of).sorted()
                .map(VersionNumber::toString).toList();

        assertThat(sorted).containsExactly("24.2", "24.9.21", "24.10");
        // The control: as text the order is the wrong one, which is what the type exists for.
        assertThat(Stream.of("24.2", "24.10", "24.9.21").sorted().toList()).containsExactly("24.10", "24.2",
                "24.9.21");
    }

    @Test
    void a_longer_number_wins_over_a_shorter_one_in_every_segment() {
        assertThat(VersionNumber.of("24.9.100")).isGreaterThan(VersionNumber.of("24.9.26"));
        assertThat(VersionNumber.of("100.0")).isGreaterThan(VersionNumber.of("25.4"));
        assertThat(VersionNumber.of("8.32.3")).isLessThan(VersionNumber.of("14.14.9"));
    }

    @Test
    void a_prerelease_is_below_its_release_and_prereleases_order_among_themselves() {
        List<String> sorted = Stream
                .of("25.4.0", "25.4.0-rc1", "25.4.0-beta2", "25.4.0-beta10", "25.4.0-alpha7", "25.3.5", "4.2.0-RC2")
                .map(VersionNumber::of).sorted().map(VersionNumber::toString).toList();

        assertThat(sorted).containsExactly("4.2.0-RC2", "25.3.5", "25.4.0-alpha7", "25.4.0-beta2", "25.4.0-beta10",
                "25.4.0-rc1", "25.4.0");
        assertThat(VersionNumber.of("25.4.0-beta2").isPrerelease()).isTrue();
        assertThat(VersionNumber.of("25.4.0").isPrerelease()).isFalse();
        assertThat(VersionNumber.of("24.0.0.beta1")).isLessThan(VersionNumber.of("24.0.0"));
    }

    @Test
    void a_missing_segment_counts_as_zero_and_the_text_stays_as_received() {
        assertThat(VersionNumber.of("24.9")).isEqualByComparingTo(VersionNumber.of("24.9.0"));
        assertThat(VersionNumber.of("24.9")).isNotEqualTo(VersionNumber.of("24.9.0"));
        assertThat(VersionNumber.of(" 24.9.21 ")).hasToString("24.9.21").isEqualTo(VersionNumber.of("24.9.21"));
    }

    @Test
    void the_line_is_the_minor() {
        assertThat(VersionNumber.of("24.9.21").line()).isEqualTo("24.9");
        assertThat(VersionNumber.of("25.4.0-beta2").line()).isEqualTo("25.4");
        assertThat(VersionNumber.of("24.10").line()).isEqualTo("24.10");
    }

    @Test
    void anything_parses_and_what_is_not_a_version_sorts_below_every_version() {
        assertThat(VersionNumber.of("not a version")).isLessThan(VersionNumber.of("0.0.1"));
        assertThat(VersionNumber.of("")).isLessThan(VersionNumber.of("0.0.1"));
        assertThat(VersionNumber.of("99999999999999999999.1")).isGreaterThan(VersionNumber.of("25.4"));
    }
}

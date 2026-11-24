package com.vaadin.securitystatus.customer;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TriageFilesTest {

    @ParameterizedTest
    @ValueSource(strings = { "=1+1", "+1", "-1", "@SUM(A1)", "\tx", "\rx" })
    void a_cell_that_starts_like_a_formula_is_written_as_text(String cell) {
        String csv = new String(TriageFiles.csv(sheet(List.of(row(cell)))), StandardCharsets.UTF_8);

        assertThat(csv).contains("\"'" + cell.replace("\"", "\"\"") + "\"");
    }

    @Test
    void an_ordinary_cell_is_written_as_it_is() {
        assertThat(TriageFiles.inert("CVE-2026-1")).isEqualTo("CVE-2026-1");
        assertThat(TriageFiles.inert("")).isEmpty();
    }

    @Test
    void a_note_cannot_start_a_line_of_its_own() {
        var sheet = new TriageFiles.Sheet("t", "a", List.of("one\rtwo\nthree"), List.of(), columns(), List.of());

        String csv = new String(TriageFiles.csv(sheet), StandardCharsets.UTF_8);

        assertThat(csv).startsWith("# one two three\r\n");
    }

    @Test
    void a_very_long_word_is_laid_out_in_a_moment() {
        var sheet = sheet(List.of(row("pkg:maven/" + "a".repeat(200_000))));

        long started = System.nanoTime();
        byte[] pdf = TriageFiles.pdf(sheet);

        assertThat(pdf).isNotEmpty();
        assertThat(Duration.ofNanos(System.nanoTime() - started)).isLessThan(Duration.ofSeconds(20));
    }

    private static TriageFiles.Sheet sheet(List<List<String>> rows) {
        return new TriageFiles.Sheet("Title", "About", List.of("note"), List.of("lead"), columns(), rows);
    }

    private static List<String> columns() {
        return List.of("cve", "bucket", "owner", "component", "severity", "action", "verdict");
    }

    private static List<String> row(String component) {
        return List.of("CVE-2026-1", "fix", "vaadin", component, "high", "upgrade", "risk");
    }
}

package com.vaadin.securitystatus.workbench;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.vaadin.securitystatus.data.FixturePages;
import com.vaadin.securitystatus.data.model.VerdictQueue;
import com.vaadin.securitystatus.data.model.VerdictQueue.QueueEntry;
import com.vaadin.securitystatus.data.model.VerdictQueue.QueuedAdvisory;
import com.vaadin.securitystatus.workbench.QueueRules.Reach;
import com.vaadin.securitystatus.workbench.QueueRules.Row;

/**
 * The queue's questions without components. The fixture has every advisory
 * answered, so the ranking by what would close is exercised on a queue built
 * here, with advisories nobody judged.
 */
class QueueRulesTest {

    private static final VerdictQueue FIXTURE = FixturePages.offline().queue();

    @Test
    void the_series_are_every_line_of_the_queue_newest_first() {
        assertThat(QueueRules.series(FIXTURE)).containsExactly("25.4", "25.3", "24.10", "24.9", "24.7", "24.4",
                "23.6", "14.14", "8.32", "7.7");
        assertThat(QueueRules.series(FIXTURE, Optional.of("24.9"))).contains("24.9");
        assertThat(QueueRules.series(FIXTURE, Optional.of("24.8"))).isEmpty();
    }

    @Test
    void on_a_series_the_rows_are_ranked_by_what_would_close_there() {
        var queue = new VerdictQueue(Optional.empty(), Map.of(), Map.of(), List.of(
                entry("a", "pkg:npm/a", advisory("CVE-1", Map.of("24.9", "risk"), "24.9")),
                entry("b", "pkg:npm/b", advisory("CVE-2", Map.of(), "24.9"), advisory("CVE-3", Map.of(), "24.9"),
                        advisory("CVE-4", Map.of(), "25.3")),
                entry("c", "pkg:npm/c", advisory("CVE-5", Map.of("24.9", "in_triage"), "24.9", "25.3")),
                entry("d", "pkg:npm/d", advisory("CVE-6", Map.of(), "25.3"))));

        List<Row> rows = QueueRules.rows(queue, Optional.of("24.9"));

        assertThat(rows).extracting(Row::name).containsExactly("b", "c", "a");
        assertThat(rows).extracting(Row::closes).containsExactly(2, 1, 0);
        assertThat(rows.getFirst().elsewhere()).isEqualTo(1);
        // With no series the queue's own order and numbers stand.
        assertThat(QueueRules.rows(queue, Optional.empty())).extracting(Row::name).containsExactly("a", "b", "c", "d");
    }

    @Test
    void the_prefill_is_the_unjudged_and_otherwise_everything() {
        var open = entry("b", "pkg:npm/b", advisory("CVE-2", Map.of(), "24.9"),
                advisory("CVE-3", Map.of("24.9", "risk"), "24.9"));
        var answered = entry("a", "pkg:npm/a", advisory("CVE-1", Map.of("24.9", "risk"), "24.9"),
                advisory("CVE-7", Map.of("24.9", "fixed"), "24.9"));

        assertThat(QueueRules.prefill(new Row(open, open.cves(), 1, 0), Optional.of("24.9"))).containsExactly("CVE-2");
        assertThat(QueueRules.prefill(new Row(answered, answered.cves(), 0, 0), Optional.of("24.9")))
                .containsExactly("CVE-1", "CVE-7");
    }

    @Test
    void the_proposed_id_is_the_purl_slug_the_series_and_the_day() {
        QueueEntry jackson = FIXTURE.packages().getFirst();
        LocalDate day = LocalDate.of(2026, 11, 21);

        assertThat(QueueRules.proposedId(jackson, Optional.of("24.9"), day))
                .isEqualTo("maven-com-fasterxml-jackson-core-jackson-core-24-9-2026-11-21");
        assertThat(QueueRules.proposedId(jackson, Optional.empty(), day))
                .isEqualTo("maven-com-fasterxml-jackson-core-jackson-core-2026-11-21");
    }

    @Test
    void the_reach_counts_one_finding_per_advisory_and_series() {
        QueueEntry jackson = FIXTURE.packages().getFirst();

        Reach reach = QueueRules.reach(jackson, List.of("CVE-2026-89407", "CVE-2026-89425", "CVE-0000-0000"));

        assertThat(reach.advisories()).isEqualTo(2);
        assertThat(reach.lines()).containsExactly("24.10", "24.9", "24.7", "24.4", "23.6", "14.14", "8.32", "7.7");
        assertThat(reach.findings()).isEqualTo(13);
        assertThat(reach.unjudged()).isZero();
    }

    @Test
    void the_branches_still_red_leave_out_the_unread_ones() {
        var acknowledged = new HashMap<String, List<String>>();
        acknowledged.put("25.3", List.of("CVE-1", "CVE-2"));
        acknowledged.put("24.9", List.of("CVE-1"));
        acknowledged.put("7.7", null);
        var entry = new QueueEntry("x", Optional.of("pkg:npm/x"), Optional.empty(),
                List.of(advisory("CVE-1", Map.of(), "24.9"), advisory("CVE-2", Map.of(), "25.3")), List.of(),
                List.of("24.9", "25.3"), 0, List.of(), Map.of(), Optional.empty(), List.of(), List.of(), acknowledged);

        assertThat(QueueRules.stillRed(entry)).containsExactly("24.9");
        assertThat(QueueRules.silencing("risk")).isFalse();
        assertThat(QueueRules.silencing("")).isFalse();
        assertThat(QueueRules.silencing("false_positive")).isTrue();
    }

    private static QueueEntry entry(String name, String purl, QueuedAdvisory... advisories) {
        List<String> lines = Arrays.stream(advisories).flatMap(advisory -> advisory.lines().stream()).distinct()
                .toList();
        return new QueueEntry(name, Optional.of(purl), Optional.empty(), List.of(advisories), List.of(), lines, 0,
                List.of(), Map.of(), Optional.empty(), List.of(), List.of(), Map.of());
    }

    private static QueuedAdvisory advisory(String id, Map<String, String> byLine, String... lines) {
        return new QueuedAdvisory(id, Optional.of("high"), Optional.empty(), Optional.empty(), List.of(lines), byLine,
                Map.of(), Map.of(), List.of(), List.of(), Optional.empty(), List.of(), false);
    }
}

package com.vaadin.securitystatus.ui.team.explorer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** D-255: a field shows its first sentences and the rest is one click away, cut as the prototype cuts it. */
class FindingDetailTest {

    @Test
    void a_short_text_is_kept_whole() {
        assertThat(FindingDetail.brief("  One sentence.  ")).isEqualTo(new FindingDetail.Brief("One sentence.", false));
    }

    @Test
    void a_long_text_is_cut_where_its_last_sentence_inside_the_limit_ends() {
        String first = "a".repeat(100) + ".";
        String second = "b".repeat(100) + ".";
        String third = "c".repeat(100) + ".";

        var brief = FindingDetail.brief(first + " " + second + " " + third);

        assertThat(brief).isEqualTo(new FindingDetail.Brief(first + " " + second, true));
    }

    @Test
    void a_long_text_with_no_sentence_ending_late_enough_is_cut_at_a_word() {
        String text = "Short. " + "word ".repeat(80);

        var brief = FindingDetail.brief(text);

        assertThat(brief.cut()).isTrue();
        assertThat(brief.text()).hasSizeLessThanOrEqualTo(FindingDetail.BRIEF).endsWith("word");
    }
}

package com.vaadin.securitystatus.team.explorer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** D-264: a field shows its first sentences and the rest is one click away, cut as the prototype cuts it. */
class BriefTest {

    @Test
    void a_short_text_is_kept_whole() {
        assertThat(ExplorerRules.brief("  One sentence.  ")).isEqualTo(new ExplorerRules.Brief("One sentence.", false));
    }

    @Test
    void a_long_text_is_cut_where_its_last_sentence_inside_the_limit_ends() {
        String first = "a".repeat(100) + ".";
        String second = "b".repeat(100) + ".";
        String third = "c".repeat(100) + ".";

        var brief = ExplorerRules.brief(first + " " + second + " " + third);

        assertThat(brief).isEqualTo(new ExplorerRules.Brief(first + " " + second, true));
    }

    @Test
    void a_long_text_with_no_sentence_ending_late_enough_is_cut_at_a_word() {
        String text = "Short. " + "word ".repeat(80);

        var brief = ExplorerRules.brief(text);

        assertThat(brief.cut()).isTrue();
        assertThat(brief.text()).hasSizeLessThanOrEqualTo(ExplorerRules.BRIEF).endsWith("word");
    }
}

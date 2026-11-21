package com.vaadin.securitystatus.base.nav;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.router.QueryParameters;

/**
 * The one writer of addresses keeps every flag it was given and writes them in
 * the declared order, whatever order they were set in, draft 14.
 */
class AddressTest {

    @Test
    void flags_come_out_in_the_declared_order_whatever_order_they_were_set_in() {
        var address = Address.of("version/24.9.21").with(QueryFlag.OPEN, "CVE-2026-1").flag(QueryFlag.DISC, true)
                .flag(QueryFlag.BOOT, true).flag(QueryFlag.RULED, true);

        assertThat(address.href()).isEqualTo("version/24.9.21?boot=1&ruled=1&disc=1&open=CVE-2026-1");
    }

    @Test
    void changing_one_flag_keeps_the_others() {
        var address = Address.of("findings").with(QueryFlag.Q, "jackson").list(QueryFlag.OFF,
                List.of("vaadin-spring-boot-starter", "flow-webpush"));

        assertThat(address.with(QueryFlag.Q, Optional.empty()).href())
                .isEqualTo("findings?off=vaadin-spring-boot-starter,flow-webpush");
        assertThat(address.list(QueryFlag.OFF, List.of()).href()).isEqualTo("findings?q=jackson");
    }

    @Test
    void a_bare_flag_has_no_value_and_a_value_is_encoded() {
        assertThat(Address.of("scan/24.9.21").bare(QueryFlag.SECURE, true).href()).isEqualTo("scan/24.9.21?secure");
        assertThat(Address.of("findings").with(QueryFlag.Q, "a b&c").href()).isEqualTo("findings?q=a%20b%26c");
    }

    @Test
    void a_flag_nobody_declared_is_kept_after_the_declared_ones() {
        var address = Address.parse("version/24.9.21", "zeta=1&open=x&boot=1");

        assertThat(address.href()).isEqualTo("version/24.9.21?boot=1&open=x&zeta=1");
    }

    @Test
    void reading_a_list_drops_blanks_and_repeats_and_keeps_the_written_order() {
        var query = QueryParameters.fromString("off=b,,a,%20b%20,c");

        assertThat(Address.list(query, QueryFlag.OFF)).containsExactly("b", "a", "c");
        assertThat(Address.has(query, QueryFlag.Q)).isFalse();
    }

    @Test
    void zero_is_off_and_anything_else_is_on() {
        var query = QueryParameters.fromString("ruled=0&disc=1&pre");

        assertThat(Address.on(query, QueryFlag.RULED)).isFalse();
        assertThat(Address.on(query, QueryFlag.DISC)).isTrue();
        assertThat(Address.has(query, QueryFlag.PRE)).isTrue();
    }
}

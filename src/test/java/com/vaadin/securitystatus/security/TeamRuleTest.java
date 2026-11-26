package com.vaadin.securitystatus.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class TeamRuleTest {

    private final TeamRule rule = new TeamRule("vaadin.com");

    @Test
    void a_verified_email_on_the_domain_is_the_team_and_any_other_is_not() {
        assertThat(rule.isTeam(Optional.of("Someone@Vaadin.com"), true)).isTrue();
        assertThat(rule.isTeam(Optional.of("someone@vaadin.com"), false)).isFalse();
        assertThat(rule.isTeam(Optional.of("someone@example.com"), true)).isFalse();
        assertThat(rule.isTeam(Optional.of("someone@notvaadin.com"), true)).isFalse();
        assertThat(rule.isTeam(Optional.of("someone@vaadin.com.example.com"), true)).isFalse();
        assertThat(rule.isTeam(Optional.of("@vaadin.com"), true)).isFalse();
        assertThat(rule.isTeam(Optional.empty(), true)).isFalse();
    }

    @Test
    void the_attributes_cas_sends_decide_and_an_unverified_address_does_not_count() {
        assertThat(rule.authorities(Map.of("email", "someone@vaadin.com"))).hasSize(1);
        assertThat(rule.authorities(Map.of("emailAddress", List.of("someone@vaadin.com"),
                TeamRule.VERIFIED_ATTRIBUTE, List.of("true")))).hasSize(1);
        assertThat(rule.authorities(Map.of("mail", "someone@vaadin.com", TeamRule.VERIFIED_ATTRIBUTE, "false")))
                .isEmpty();
        assertThat(rule.authorities(Map.of("email", "someone@example.com"))).isEmpty();
        assertThat(rule.authorities(Map.of())).isEmpty();
    }
}

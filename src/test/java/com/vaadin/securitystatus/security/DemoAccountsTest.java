package com.vaadin.securitystatus.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.ActiveProfiles;

/** A deployment names its own demonstration accounts, and the defaults give way to them. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
        "securitystatus.demo.team.name=alice", "securitystatus.demo.team.password=secret",
        "securitystatus.demo.guest.name=bob", "securitystatus.demo.guest.password=hidden" })
@ActiveProfiles("demo")
class DemoAccountsTest {

    @Autowired
    UserDetailsService users;

    @Test
    void the_named_accounts_sign_in_with_their_roles() {
        var team = users.loadUserByUsername("alice");
        assertThat(team.getPassword()).isEqualTo("{noop}secret");
        assertThat(team.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_" + Roles.SECURITY);
        var guest = users.loadUserByUsername("bob");
        assertThat(guest.getPassword()).isEqualTo("{noop}hidden");
        assertThat(guest.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .doesNotContain("ROLE_" + Roles.SECURITY);
    }

    @Test
    void the_default_accounts_are_gone() {
        assertThatThrownBy(() -> users.loadUserByUsername("team")).isInstanceOf(UsernameNotFoundException.class);
        assertThatThrownBy(() -> users.loadUserByUsername("guest")).isInstanceOf(UsernameNotFoundException.class);
    }
}

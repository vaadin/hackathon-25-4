package com.vaadin.securitystatus.base.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import com.vaadin.flow.spring.security.VaadinExpiredSessionStrategy;
import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;

/**
 * The public screens stay open and the internal ones need the
 * {@link Roles#SECURITY} role. A signed in user goes back to the address they
 * asked for, which is what the configurer's saved request handling does when no
 * success URL overrides it.
 */
@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Vaadin permits the stylesheets the application shell names, not the ones those import.
        http.authorizeHttpRequests(
                requests -> requests.requestMatchers("/styles/**", CspPolicy.REPORT_PATH).permitAll());
        http.csrf(csrf -> csrf.ignoringRequestMatchers(CspPolicy.REPORT_PATH));
        http.headers(headers -> headers.contentSecurityPolicy(
                policy -> policy.policyDirectives(CspPolicy.enforced().toHeaderValue())));
        // An address that matches no route reaches Flow and gets its not found view instead of
        // the login page. The views themselves are guarded by navigation access control.
        return http.with(VaadinSecurityConfigurer.vaadin(), configurer -> configurer.loginView(LoginView.class)
                .anyRequest(rule -> rule.permitAll())
                .expiredSessionStrategy(new VaadinExpiredSessionStrategy())).build();
    }

    @Bean
    UserDetailsService users() {
        return new InMemoryUserDetailsManager(
                User.withUsername("team").password("{noop}team").roles(Roles.SECURITY).build(),
                User.withUsername("guest").password("{noop}guest").roles().build());
    }
}

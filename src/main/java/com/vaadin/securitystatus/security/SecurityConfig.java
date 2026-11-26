package com.vaadin.securitystatus.security;

import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.cas.ServiceProperties;
import org.springframework.security.cas.authentication.CasAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.vaadin.flow.spring.security.RequestUtil;
import com.vaadin.flow.spring.security.VaadinExpiredSessionStrategy;
import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import com.vaadin.securitystatus.ui.common.LoginView;

/**
 * Every page needs a signed in user: the customer pages any one, the team's
 * the {@link Roles#SECURITY} role, D-258. Who signs in depends on the profile:
 * {@code cas} sends the visitor to vaadin.com's single sign on, {@code demo}
 * shows the login view with the demonstration accounts. A signed in user goes
 * back to the address they asked for, which is what the saved request handling
 * does when no success URL overrides it.
 */
@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, RequestUtil requestUtil,
            @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping controllers,
            ObjectProvider<CasSecurity> cas, ObjectProvider<ServiceProperties> casService,
            ObjectProvider<CasAuthenticationProvider> casProvider, SignInFailures failures) throws Exception {
        // Vaadin permits the stylesheets the application shell names, not the ones those import.
        http.authorizeHttpRequests(requests -> requests
                .requestMatchers("/styles/**", "/icons/**", CspPolicy.REPORT_PATH).permitAll()
                .requestMatchers(request -> notFound(request, requestUtil, controllers)).permitAll());
        http.csrf(csrf -> csrf.ignoringRequestMatchers(CspPolicy.REPORT_PATH));
        http.headers(headers -> headers
                .contentSecurityPolicy(policy -> policy.policyDirectives(CspPolicy.enforced().toHeaderValue()))
                .referrerPolicy(referrer -> referrer
                        .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)));
        Optional<CasSecurity> sso = Optional.ofNullable(cas.getIfAvailable());
        if (sso.isPresent()) {
            ServiceProperties service = casService.getObject();
            http.exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(sso.get().entryPoint(service)));
            http.addFilterBefore(sso.get().filter(service, casProvider.getObject()),
                    UsernamePasswordAuthenticationFilter.class);
        }
        // Everything else needs a signed in user, the configurer's default, D-01 as revised by D-235.
        return http.with(VaadinSecurityConfigurer.vaadin(), configurer -> {
            configurer.expiredSessionStrategy(new VaadinExpiredSessionStrategy());
            if (sso.isPresent()) {
                configurer.logoutSuccessHandler(sso.get().logoutSuccess());
            } else {
                configurer.loginView(LoginView.class).failureHandler(failures);
            }
        }).build();
    }

    /**
     * A page request that nothing in the application answers: no secured route
     * and no controller. It reaches Flow and gets the not found view instead of
     * the login page, while a controller added later stays closed until it is
     * opened on purpose.
     */
    static boolean notFound(HttpServletRequest request, RequestUtil requestUtil,
            RequestMappingHandlerMapping controllers) {
        if (!HttpMethod.GET.matches(request.getMethod()) || requestUtil.isSecuredFlowRoute(request)
                || requestUtil.isFrameworkInternalRequest(request)) {
            return false;
        }
        try {
            return controllers.getHandler(request) == null;
        } catch (Exception unmatched) {
            return false;
        }
    }

    /**
     * The demonstration accounts, only under the profile {@code demo}, which
     * development mode turns on by itself, D-258. Their emails go through the
     * same rule as a person signed in through CAS.
     */
    @Bean
    @Profile("demo")
    UserDetailsService demoUsers(TeamRule team, SignInFailures failures) {
        return failures.guard(new InMemoryUserDetailsManager(
                User.withUsername("team").password("{noop}team")
                        .authorities(team.authorities(Optional.of("team@vaadin.com"), true)).build(),
                User.withUsername("guest").password("{noop}guest")
                        .authorities(team.authorities(Optional.of("guest@example.com"), true)).build()));
    }
}

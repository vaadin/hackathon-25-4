package com.vaadin.securitystatus.security;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.vaadin.flow.spring.security.RequestUtil;
import com.vaadin.flow.spring.security.VaadinExpiredSessionStrategy;
import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import com.vaadin.securitystatus.ui.common.LoginView;

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
    SecurityFilterChain securityFilterChain(HttpSecurity http, RequestUtil requestUtil,
            @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping controllers) throws Exception {
        // Vaadin permits the stylesheets the application shell names, not the ones those import.
        http.authorizeHttpRequests(requests -> requests
                .requestMatchers("/styles/**", "/icons/**", CspPolicy.REPORT_PATH).permitAll()
                .requestMatchers(request -> notFound(request, requestUtil, controllers)).permitAll());
        http.csrf(csrf -> csrf.ignoringRequestMatchers(CspPolicy.REPORT_PATH));
        http.headers(headers -> headers.contentSecurityPolicy(
                policy -> policy.policyDirectives(CspPolicy.enforced().toHeaderValue())));
        // Everything else needs a signed in user, the configurer's default, D-01 as revised by D-235.
        return http.with(VaadinSecurityConfigurer.vaadin(), configurer -> configurer.loginView(LoginView.class)
                .expiredSessionStrategy(new VaadinExpiredSessionStrategy())).build();
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

    @Bean
    UserDetailsService users() {
        return new InMemoryUserDetailsManager(
                User.withUsername("team").password("{noop}team").roles(Roles.SECURITY).build(),
                User.withUsername("guest").password("{noop}guest").roles().build());
    }
}

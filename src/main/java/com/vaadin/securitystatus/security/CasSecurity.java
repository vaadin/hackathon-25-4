package com.vaadin.securitystatus.security;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.apereo.cas.client.validation.Cas30ServiceTicketValidator;
import org.apereo.cas.client.validation.TicketValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.cas.ServiceProperties;
import org.springframework.security.cas.authentication.CasAuthenticationProvider;
import org.springframework.security.cas.web.CasAuthenticationEntryPoint;
import org.springframework.security.cas.web.CasAuthenticationFilter;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.util.UriUtils;

/**
 * Signing in through vaadin.com's single sign on, the profile {@code cas}: a
 * visitor without a session goes to the CAS server, comes back with a ticket,
 * and the ticket is validated against the server, D-258. The same server
 * vaadin.com's own applications sign in with, so a person signed in there is
 * signed in here.
 */
@Configuration
@Profile("cas")
class CasSecurity {

    /** Where CAS sends the browser back with its ticket. */
    static final String CALLBACK = "/login/cas";

    private final String server;
    private final String service;

    CasSecurity(@Value("${securitystatus.cas.server:https://sso.vaadin.com}") String server,
            @Value("${securitystatus.cas.service}") String service) {
        this.server = server.endsWith("/") ? server.substring(0, server.length() - 1) : server;
        this.service = service;
    }

    @Bean
    ServiceProperties casService() {
        var properties = new ServiceProperties();
        properties.setService(service);
        properties.setSendRenew(false);
        return properties;
    }

    @Bean
    TicketValidator casTicketValidator() {
        return new Cas30ServiceTicketValidator(server);
    }

    @Bean
    CasAuthenticationProvider casAuthenticationProvider(ServiceProperties casService, TicketValidator validator,
            TeamRule team) {
        var provider = new CasAuthenticationProvider();
        provider.setServiceProperties(casService);
        provider.setTicketValidator(validator);
        provider.setKey(UUID.randomUUID().toString());
        provider.setAuthenticationUserDetailsService(token -> User.withUsername(token.getName()).password("")
                .authorities(team.authorities(token.getAssertion().getPrincipal().getAttributes())).build());
        return provider;
    }

    CasAuthenticationEntryPoint entryPoint(ServiceProperties casService) {
        var entryPoint = new CasAuthenticationEntryPoint();
        entryPoint.setLoginUrl(server + "/login");
        entryPoint.setServiceProperties(casService);
        return entryPoint;
    }

    CasAuthenticationFilter filter(ServiceProperties casService, CasAuthenticationProvider provider) {
        var filter = new CasAuthenticationFilter();
        filter.setServiceProperties(casService);
        filter.setAuthenticationManager(new ProviderManager(provider));
        filter.setFilterProcessesUrl(CALLBACK);
        filter.setSecurityContextRepository(new HttpSessionSecurityContextRepository());
        return filter;
    }

    /** Signing out here signs out of CAS too, and CAS sends the browser back to this application. */
    LogoutSuccessHandler logoutSuccess() {
        String home = service.endsWith(CALLBACK) ? service.substring(0, service.length() - CALLBACK.length()) + "/"
                : service;
        String target = server + "/logout?service=" + UriUtils.encodeQueryParam(home, StandardCharsets.UTF_8);
        return (request, response, authentication) -> response.sendRedirect(target);
    }
}

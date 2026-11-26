package com.vaadin.securitystatus.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.Map;

import org.apereo.cas.client.authentication.AttributePrincipalImpl;
import org.apereo.cas.client.validation.AssertionImpl;
import org.apereo.cas.client.validation.TicketValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** D-258: under the cas profile a visitor signs in at vaadin.com's single sign on. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "securitystatus.cas.service=https://vaadin.example/security-status/login/cas")
@ActiveProfiles("cas")
class CasSignInTest {

    @Autowired
    WebApplicationContext context;

    @MockitoBean
    TicketValidator validator;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void a_ticket_for_a_vaadin_address_signs_in_the_team() throws Exception {
        assertThat(roles(signIn("ST-1", "someone@vaadin.com"))).contains("ROLE_" + Roles.SECURITY);
    }

    @Test
    void a_ticket_for_any_other_address_signs_in_a_reader_without_the_team_role() throws Exception {
        assertThat(roles(signIn("ST-2", "someone@example.com"))).isNotEmpty().doesNotContain("ROLE_" + Roles.SECURITY);
    }

    @Test
    void a_ticket_the_server_does_not_vouch_for_signs_nobody_in() throws Exception {
        when(validator.validate(anyString(), anyString()))
                .thenThrow(new org.apereo.cas.client.validation.TicketValidationException("no"));

        var session = new MockHttpSession();
        mvc.perform(get(CasSecurity.CALLBACK).param("ticket", "ST-3").session(session));

        assertThat(session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY)).isNull();
    }

    private MockHttpSession signIn(String ticket, String email) throws Exception {
        when(validator.validate(eq(ticket), anyString()))
                .thenReturn(new AssertionImpl(new AttributePrincipalImpl("uuid-" + ticket, Map.of("email", email))));
        var session = new MockHttpSession();
        var answer = mvc.perform(get(CasSecurity.CALLBACK).param("ticket", ticket).session(session)).andReturn()
                .getResponse();
        assertThat(answer.getStatus()).isEqualTo(302);
        return session;
    }

    private static java.util.List<String> roles(MockHttpSession session) {
        var security = (SecurityContext) session
                .getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertThat(security).isNotNull();
        return security.getAuthentication().getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
    }
}

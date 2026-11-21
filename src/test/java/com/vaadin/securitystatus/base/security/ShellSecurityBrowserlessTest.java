package com.vaadin.securitystatus.base.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import jakarta.servlet.http.HttpSession;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WrappedHttpSession;
import com.vaadin.securitystatus.base.ui.NotFoundView;
import com.vaadin.securitystatus.explorer.ExplorerView;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class ShellSecurityBrowserlessTest extends SpringBrowserlessTest {

    @Autowired
    WebApplicationContext context;

    // FND-01
    @Test
    @WithAnonymousUser
    void anonymous_navigation_to_findings_lands_on_login() {
        navigate("findings", LoginView.class);

        assertThat($(Button.class).withText("Sign out").exists()).isFalse();
    }

    // FND-01
    @Test
    @WithAnonymousUser
    void signing_in_as_team_after_the_refusal_goes_back_to_findings() throws Exception {
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

        var direct = mvc.perform(formLogin("/login").user("team").password("team")).andReturn().getResponse();
        assertThat(direct.getRedirectedUrl()).doesNotContain("findings");

        navigate("findings", LoginView.class);
        // The refused navigation remembered its target in this session, and the sign in reads it there.
        HttpSession refused = ((WrappedHttpSession) VaadinSession.getCurrent().getSession()).getHttpSession();
        var back = mvc.perform(post("/login").param("username", "team").param("password", "team").with(csrf())
                .with(request -> {
                    request.setSession(refused);
                    return request;
                })).andReturn().getResponse();

        assertThat(back.getStatus()).isEqualTo(302);
        assertThat(back.getRedirectedUrl()).matches(".*/findings(\\?continue)?");
    }

    @Test
    void wrong_password_stays_on_login_with_the_error_flag() throws Exception {
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

        var failed = mvc.perform(formLogin("/login").user("team").password("wrong")).andReturn().getResponse();

        assertThat(failed.getRedirectedUrl()).endsWith("/login?error");
    }

    // FND-01
    @Test
    @WithMockUser(username = "team", roles = Roles.SECURITY)
    void team_reaches_findings_inside_the_internal_shell() {
        navigate(ExplorerView.class);

        assertThat($(H1.class).single().getText()).isEqualTo("Findings");
        assertThat($(SideNavItem.class).all()).extracting(SideNavItem::getLabel).containsExactly("Findings",
                "What Spring Boot brings");
        assertThat($(Button.class).withText("Sign out").exists()).isTrue();
    }

    // FND-02
    @Test
    @WithMockUser(username = "guest", roles = {})
    void guest_is_refused_on_findings() {
        navigate("findings", NotFoundView.class);

        assertThat($(SideNavItem.class).exists()).isFalse();
        assertThat($(Button.class).withText("Sign out").exists()).isTrue();
    }
}

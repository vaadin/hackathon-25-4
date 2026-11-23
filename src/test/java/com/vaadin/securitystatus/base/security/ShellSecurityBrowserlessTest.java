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
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WrappedHttpSession;
import com.vaadin.securitystatus.base.ui.NotFoundView;
import com.vaadin.securitystatus.explorer.ExplorerView;
import com.vaadin.securitystatus.status.HomeView;
import com.vaadin.flow.component.sidenav.SideNavItem;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class ShellSecurityBrowserlessTest extends SpringBrowserlessTest {

    @Autowired
    WebApplicationContext context;

    // The sign in page is no answer: no heading, purpose or lookup above the form
    @Test
    @WithAnonymousUser
    void the_sign_in_page_stands_alone_and_the_answers_keep_the_lookup() {
        navigate(LoginView.class);
        assertThat($(H1.class).withClassName("view-heading").exists()).isFalse();
        assertThat($(Div.class).withClassName("lookup-mount").exists()).isFalse();

        navigate(HomeView.class);
        assertThat($(H1.class).withClassName("view-heading").single().getText()).isEqualTo("Security status");
        assertThat($(Div.class).withClassName("lookup-mount").exists()).isTrue();
    }

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

        // D-203: the shell draws the H1 in its bar, the explorer's title the prototype's, and the four
        // destinations are links in the bar beside it, so the left panel holds only the filter.
        assertThat($(H1.class).single().getText()).isEqualTo("Vaadin security status");
        // D-237, D-245: the left panel's side navigation, in the order the team works.
        assertThat($(SideNavItem.class).withClassName("internal-nav-link").all()).extracting(SideNavItem::getLabel)
                .containsExactly("Customer view", "Findings", "Spring Boot", "Verdicts", "Advisories", "Diagnostics");
        assertThat($(Button.class).withText("Sign out").exists()).isTrue();
    }

    // FND-02
    @Test
    @WithMockUser(username = "guest", roles = {})
    void guest_is_refused_on_findings() {
        navigate("findings", NotFoundView.class);

        assertThat($(SideNavItem.class).withClassName("internal-nav-link").exists()).isFalse();
        assertThat($(Button.class).withText("Sign out").exists()).isTrue();
    }
}

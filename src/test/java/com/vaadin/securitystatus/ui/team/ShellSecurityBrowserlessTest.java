package com.vaadin.securitystatus.ui.team;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WrappedHttpSession;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.securitystatus.security.Roles;
import com.vaadin.securitystatus.ui.common.LoginView;
import com.vaadin.securitystatus.ui.common.NotFoundView;
import com.vaadin.securitystatus.ui.customer.HomeView;
import com.vaadin.securitystatus.ui.team.explorer.ExplorerView;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class ShellSecurityBrowserlessTest extends SpringBrowserlessTest {

    @Autowired
    WebApplicationContext context;

    // The sign in page is no answer: no heading, purpose or lookup above the form
    @Test
    @WithMockUser(username = "guest")
    void the_sign_in_page_stands_alone_and_the_answers_keep_the_lookup() {
        navigate(LoginView.class);
        assertThat($(H1.class).withClassName("view-heading").exists()).isFalse();
        assertThat($(Div.class).withClassName("lookup-mount").exists()).isFalse();

        navigate(HomeView.class);
        assertThat($(H1.class).withClassName("view-heading").single().getText()).isEqualTo("Security status");
        assertThat($(Div.class).withClassName("lookup-mount").exists()).isTrue();
    }

    // D-258: no page answers a visitor who has not signed in
    @Test
    @WithAnonymousUser
    void a_visitor_who_has_not_signed_in_is_sent_to_sign_in() {
        assertThatThrownBy(() -> navigate(HomeView.class)).hasMessageContaining(LoginView.class.getName());
    }

    // D-246: on the customer side the team has a way back to its own pages, a reader does not
    @Test
    @WithMockUser(username = "team", roles = Roles.SECURITY)
    void the_team_finds_its_way_back_from_the_customer_side() {
        navigate(HomeView.class);

        Anchor back = $(Anchor.class).withClassName("team-pages").single();
        assertThat(back.getHref()).isEqualTo("findings");
        assertThat(back.getElement().getAttribute("aria-label")).isEqualTo("Team pages");
    }

    @Test
    @WithMockUser(username = "guest")
    void a_reader_has_no_link_to_the_team_pages() {
        navigate(HomeView.class);

        assertThat($(Anchor.class).withClassName("team-pages").exists()).isFalse();
        assertThat($(SideNav.class).exists()).isFalse();
    }

    // D-253: a team member reads the customer pages with the team's navigation beside them
    @Test
    @WithMockUser(username = "team", roles = Roles.SECURITY)
    void the_team_reads_the_customer_pages_beside_its_navigation() {
        navigate(HomeView.class);

        assertThat($(SideNavItem.class).withClassName("internal-nav-link").all()).extracting(SideNavItem::getLabel)
                .containsExactly("Findings", "Spring Boot", "Customer view", "Verdicts", "Advisories", "Diagnostics");
        assertThat($(H1.class).single().getText()).isEqualTo("Security status");
    }

    // FND-01
    @Test
    @WithAnonymousUser
    void anonymous_navigation_to_findings_lands_on_login() {
        navigate("findings", LoginView.class);

        assertThat($(Button.class).withClassName("sign-out").exists()).isFalse();
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
                .containsExactly("Findings", "Spring Boot", "Customer view", "Verdicts", "Advisories", "Diagnostics");
        assertThat($(Button.class).withClassName("sign-out").exists()).isTrue();
    }

    // FND-02
    @Test
    @WithMockUser(username = "guest", roles = {})
    void guest_is_refused_on_findings() {
        navigate("findings", NotFoundView.class);

        assertThat($(SideNavItem.class).withClassName("internal-nav-link").exists()).isFalse();
        assertThat($(Button.class).withClassName("sign-out").exists()).isTrue();
    }
}

package com.vaadin.securitystatus.data;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.securitystatus.base.security.Roles;
import com.vaadin.securitystatus.base.ui.DataOutageNotice;
import com.vaadin.securitystatus.explorer.ExplorerView;
import com.vaadin.securitystatus.status.HomeView;

/** DAT-04. A cold cache and a dead upstream is one sentence in the shell, never an exception page. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class DataOutageBrowserlessTest extends SpringBrowserlessTest {

    private static final String UNREACHABLE = "The security data cannot be reached right now, so there is nothing to show here yet.";

    @Autowired
    FixturePages fixtures;

    @AfterEach
    void restore() {
        fixtures.restore();
    }

    // DAT-04
    @Test
    void home_says_the_one_sentence_inside_the_shell() {
        // The control: with the host up the same view shows the lines and no notice.
        navigate(HomeView.class);
        assertThat($(DataOutageNotice.class).exists()).isFalse();
        assertThat($(RouterLink.class).withClassName("line-card").all()).isNotEmpty();

        fixtures.coldAndDown();
        navigate("nothing-here", com.vaadin.flow.router.RouteNotFoundError.class);
        HomeView home = navigate(HomeView.class);

        assertThat(home).isNotNull();
        assertThat($(DataOutageNotice.class).single().getText()).isEqualTo(UNREACHABLE);
        assertThat($(RouterLink.class).withClassName("line-card").exists()).isFalse();
        assertThat($(H1.class).single().getText()).isEqualTo("Security status");
        // Nothing was measured as far as this application knows, so no date is shown.
        assertThat($(Badge.class).withClassName("measured-chip").exists()).isFalse();
        assertThat(fixtures.transport().requestsFor(SecurityDataService.INDEX)).isNotEmpty()
                .allMatch(request -> request.status() == 0);
    }

    // DAT-04
    @Test
    @WithMockUser(username = "team", roles = Roles.SECURITY)
    void the_internal_front_door_says_it_too() {
        // The control: with the host up, five files for each of the ten lines.
        navigate(ExplorerView.class);
        assertThat($(Anchor.class).withClassName("release-file").all()).hasSize(50);
        navigate("nothing-here", com.vaadin.flow.router.RouteNotFoundError.class);

        fixtures.coldAndDown();

        navigate(ExplorerView.class);

        assertThat($(DataOutageNotice.class).single().getText()).isEqualTo(UNREACHABLE);
        assertThat($(Anchor.class).withClassName("release-file").exists()).isFalse();
    }

    @Test
    void the_answer_comes_back_with_the_host_without_a_restart() {
        fixtures.coldAndDown();
        navigate(HomeView.class);
        assertThat($(DataOutageNotice.class).exists()).isTrue();

        fixtures.transport().comeBack();
        fixtures.revalidate();
        runPendingSignalsTasks();

        assertThat($(DataOutageNotice.class).exists()).isFalse();
        assertThat($(RouterLink.class).withClassName("line-card").all()).hasSize(9);
        assertThat($(Badge.class).withClassName("measured-chip").single().getText()).isEqualTo("Measured 2026-11-21");
    }

    @Test
    void a_dead_host_and_a_warm_cache_keeps_the_answer_and_its_date() {
        fixtures.transport().goDown();
        fixtures.revalidate();

        navigate(HomeView.class);

        assertThat($(DataOutageNotice.class).exists()).isFalse();
        assertThat($(RouterLink.class).withClassName("line-card").all()).hasSize(9);
        assertThat($(Badge.class).withClassName("measured-chip").single().getText()).isEqualTo("Measured 2026-11-21");
    }
}

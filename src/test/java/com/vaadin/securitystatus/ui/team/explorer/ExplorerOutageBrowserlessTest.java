package com.vaadin.securitystatus.ui.team.explorer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.router.RouteNotFoundError;
import com.vaadin.securitystatus.data.FixturePages;
import com.vaadin.securitystatus.security.Roles;
import com.vaadin.securitystatus.ui.common.DataOutageNotice;

/** DAT-04 on the team's front door: a cold cache and a dead upstream is the same one sentence. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@WithMockUser(username = "team", roles = Roles.SECURITY)
class ExplorerOutageBrowserlessTest extends SpringBrowserlessTest {

    private static final String UNREACHABLE = "The security data cannot be reached right now, so there is nothing to show here yet.";

    @Autowired
    FixturePages fixtures;

    @AfterEach
    void restore() {
        fixtures.restore();
    }

    // DAT-04
    @Test
    void the_internal_front_door_says_it_too() {
        // The control: with the host up, one row for each of the ten lines.
        navigate(ExplorerView.class);
        assertThat($(Details.class).withClassName("explorer-row").all()).hasSize(10);
        navigate("nothing-here", RouteNotFoundError.class);

        fixtures.coldAndDown();

        navigate(ExplorerView.class);

        assertThat($(DataOutageNotice.class).single().getText()).isEqualTo(UNREACHABLE);
        assertThat($(Details.class).withClassName("explorer-row").exists()).isFalse();
        assertThat($(Anchor.class).withClassName("release-file").exists()).isFalse();
    }
}

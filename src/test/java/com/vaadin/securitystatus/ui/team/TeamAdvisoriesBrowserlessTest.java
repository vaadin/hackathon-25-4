package com.vaadin.securitystatus.ui.team;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import com.vaadin.flow.router.RouterLink;
import com.vaadin.securitystatus.security.Roles;
import com.vaadin.securitystatus.ui.customer.AdvisoriesTesting;
import com.vaadin.securitystatus.ui.customer.AdvisoriesView;
import com.vaadin.securitystatus.ui.customer.CveView;

/** The advisories list inside the team's shell, at the address the team registers for it. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@WithMockUser(username = "team", roles = Roles.SECURITY)
class TeamAdvisoriesBrowserlessTest extends AdvisoriesTesting {

    // D-237: inside the internal shell the pills keep the internal address, so the side panel stays
    @Test
    void the_pills_keep_the_internal_address_inside_the_internal_shell() {
        navigate("findings/advisories", AdvisoriesView.class);

        assertThat(pills()).extracting(RouterLink::getHref).first().isEqualTo("findings/advisories");
        assertThat(pills()).extracting(RouterLink::getHref).contains("findings/advisories?verdict=risk");
    }

    // D-246: from the team's list, an answer opens inside the internal shell too
    @Test
    void ask_from_the_internal_list_opens_the_internal_answer() {
        var view = navigate("findings/advisories", AdvisoriesView.class);
        openMenu(view, 0).clickItem("Ask about CVE-2016-1000027");

        assertThat(getCurrentView()).isInstanceOf(CveView.class);
        assertThat(currentLocation()).isEqualTo("findings/cve/CVE-2016-1000027");
    }
}

package com.vaadin.securitystatus.workbench;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;

import com.vaadin.securitystatus.base.security.Roles;

/**
 * The two actions of {@code 04-security.md}: saving a draft and opening the
 * GitHub handover are {@code SECURITY} only, and the service itself refuses,
 * whatever calls it.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class WorkbenchSecurityTest {

    @Autowired
    VerdictDrafts drafts;

    @Test
    @WithMockUser(username = "guest", roles = {})
    void saving_a_draft_without_the_role_is_refused_by_the_service() {
        assertThatThrownBy(() -> drafts.save(VerdictYamlTest.minimal())).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(username = "guest", roles = {})
    void the_handover_without_the_role_is_refused_by_the_service() {
        assertThatThrownBy(() -> drafts.handover(VerdictYamlTest.minimal()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(username = "guest", roles = {})
    void the_drafts_are_not_listed_without_the_role() {
        assertThatThrownBy(() -> drafts.list()).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithAnonymousUser
    void an_anonymous_caller_is_refused_too() {
        assertThatThrownBy(() -> drafts.save(VerdictYamlTest.minimal())).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> drafts.handover(VerdictYamlTest.minimal()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void a_caller_with_no_authentication_at_all_is_refused() {
        assertThatThrownBy(() -> drafts.save(VerdictYamlTest.minimal())).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(username = "team", roles = Roles.SECURITY)
    void the_team_saves_and_hands_over() {
        VerdictDraft saved = drafts.save(VerdictYamlTest.minimal());

        assertThat(saved.getSavedBy()).isEqualTo("team");
        assertThat(drafts.handover(saved).path()).isEqualTo("data/assessments/npm-highcharts-2026-11-21.yaml");
    }
}

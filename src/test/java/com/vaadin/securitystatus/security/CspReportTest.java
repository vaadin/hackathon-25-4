package com.vaadin.securitystatus.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class CspReportTest {

    @Autowired
    WebApplicationContext context;

    @Autowired
    CspViolations violations;

    @Test
    void a_report_from_an_anonymous_browser_is_kept_and_answered_without_content() throws Exception {
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        assertThat(violations.newestFirst()).noneMatch(kept -> kept.report().contains("not-ours.js"));

        var answer = mvc.perform(post(CspPolicy.REPORT_PATH).contentType("application/csp-report").content("""
                {"csp-report":{"blocked-uri":"https://example.invalid/not-ours.js"}}""")).andReturn().getResponse();

        assertThat(answer.getStatus()).isEqualTo(204);
        assertThat(violations.newestFirst().getFirst().report()).contains("not-ours.js");
    }

    @Test
    void the_kept_reports_are_bounded_in_count_and_in_length() {
        var kept = new CspViolations();

        for (int i = 0; i < CspViolations.KEPT + 5; i++) {
            kept.record("x".repeat(CspViolations.MAX_REPORT_LENGTH + 50));
        }

        assertThat(kept.newestFirst()).hasSize(CspViolations.KEPT)
                .allMatch(violation -> violation.report().length() == CspViolations.MAX_REPORT_LENGTH);
    }

    @Test
    void the_policy_loosens_only_what_the_framework_forces() {
        var policy = CspPolicy.enforced();

        assertThat(policy.getSources("script-src")).containsExactly("'self'", "'unsafe-inline'", "'unsafe-eval'");
        assertThat(policy.getSources("style-src")).containsExactly("'self'", "'unsafe-inline'");
        assertThat(policy.getSources("default-src")).containsExactly("'self'");
        assertThat(policy.getSources("object-src")).containsExactly("'none'");
        assertThat(policy.getSources("frame-ancestors")).containsExactly("'none'");
    }
}

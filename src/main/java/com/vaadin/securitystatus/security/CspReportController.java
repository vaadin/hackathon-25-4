package com.vaadin.securitystatus.security;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Where the policy's {@code report-uri} points. Browsers post here on their
 * own, with no session and no CSRF token.
 */
@RestController
class CspReportController {

    private final CspViolations violations;

    CspReportController(CspViolations violations) {
        this.violations = violations;
    }

    @PostMapping(CspPolicy.REPORT_PATH)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void report(@RequestBody(required = false) String report) {
        if (report != null && !report.isBlank()) {
            violations.record(report);
        }
    }
}

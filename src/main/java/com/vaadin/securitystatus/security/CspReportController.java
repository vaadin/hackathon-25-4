package com.vaadin.securitystatus.security;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Where the policy's {@code report-uri} points. Browsers post here on their
 * own, with no session and no CSRF token.
 */
@RestController
class CspReportController {

    /** The most of a report read. A browser's report is about a kilobyte; the rest is never read. */
    static final int MAX_BYTES = 8 * 1024;

    private final CspViolations violations;

    CspReportController(CspViolations violations) {
        this.violations = violations;
    }

    @PostMapping(CspPolicy.REPORT_PATH)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void report(HttpServletRequest request) throws IOException {
        String report;
        try (InputStream body = request.getInputStream()) {
            report = new String(body.readNBytes(MAX_BYTES), StandardCharsets.UTF_8);
        }
        if (!report.isBlank()) {
            violations.record(report);
        }
    }
}

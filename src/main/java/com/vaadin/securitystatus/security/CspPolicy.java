package com.vaadin.securitystatus.security;

import com.vaadin.flow.server.ContentSecurityPolicy;

/**
 * The enforced Content Security Policy, sent on every response. Each source
 * looser than {@code 'self'} is here because something refused to work without
 * it, and has its row in {@code specs/FEEDBACK-25.4.md}.
 */
final class CspPolicy {

    static final String REPORT_PATH = "/csp-report";

    private static final String SELF = "'self'";
    private static final String NONE = "'none'";
    static final String GITHUB = "https://github.com";

    private CspPolicy() {
    }

    static ContentSecurityPolicy enforced() {
        return new ContentSecurityPolicy()
                .setDirective("default-src", SELF)
                // The bootstrap page starts the client engine inline, and the engine builds
                // functions from the strings the server sends.
                .setDirective("script-src", SELF, "'unsafe-inline'", "'unsafe-eval'")
                // Components write style attributes and inject style elements.
                .setDirective("style-src", SELF, "'unsafe-inline'")
                .setDirective("img-src", SELF, "data:")
                .setDirective("font-src", SELF, "data:")
                .setDirective("connect-src", SELF)
                .setDirective("object-src", NONE)
                .setDirective("base-uri", SELF)
                // The workbench hands a verdict over to GitHub's new file editor through a
                // GET form, and nothing else posts anywhere but here.
                .setDirective("form-action", SELF, GITHUB)
                .setDirective("frame-ancestors", NONE)
                .setDirective("report-uri", REPORT_PATH);
    }
}

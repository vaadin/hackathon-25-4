package com.vaadin.securitystatus.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * FND-05. The policy is on every response and enforcing, and the application
 * boots, signs in and navigates under it without one violation.
 */
@ExtendWith(FixturePagesServer.class)
class CspIT {

    private static final String PROBE = """
            window.__violations = [];
            document.addEventListener('securitypolicyviolation',
                event => window.__violations.push(event.effectiveDirective + ' ' + event.blockedURI));
            """;

    private ChromeDriver driver;
    private String base;

    @BeforeEach
    void openBrowser() {
        var options = new ChromeOptions();
        if (Boolean.parseBoolean(System.getProperty("it.headless", "true"))) {
            options.addArguments("--headless=new");
        }
        options.addArguments("--window-size=1280,900");
        // After a sign in Chrome offers to save the password, and the first click on the
        // page closes that bubble instead of reaching the page.
        options.setExperimentalOption("prefs",
                Map.of("credentials_enable_service", false, "profile.password_manager_enabled", false,
                        "profile.password_manager_leak_detection", false));
        driver = new ChromeDriver(options);
        driver.executeCdpCommand("Page.addScriptToEvaluateOnNewDocument", Map.of("source", PROBE));
        base = "http://localhost:" + System.getProperty("it.port", "8080");
    }

    @AfterEach
    void closeBrowser() {
        driver.quit();
    }

    @Test
    void every_response_carries_the_enforcing_header() {
        driver.get(base + "/");
        await("vaadin-select.theme-switch");

        for (String path : List.of("/", "/login", "/styles.css", "/nothing-here")) {
            assertThat(header(path, "content-security-policy")).as(path).contains("default-src 'self'")
                    .contains("frame-ancestors 'none'").contains("report-uri /csp-report");
            assertThat(header(path, "content-security-policy-report-only")).as(path).isNull();
        }
    }

    /**
     * A plain page request, as a browser makes for the document itself. The bootstrap page leaves
     * without the headers Spring Security writes, so it is asked for on its own.
     */
    @Test
    void the_bootstrap_page_itself_carries_the_enforcing_header() throws Exception {
        try (var client = HttpClient.newHttpClient()) {
            for (String path : List.of("/", "/login", "/nothing-here")) {
                var response = client.send(HttpRequest.newBuilder(URI.create(base + path)).build(),
                        HttpResponse.BodyHandlers.discarding());

                assertThat(response.headers().allValues("content-security-policy")).as(path).singleElement()
                        .asString().contains("default-src 'self'").contains("frame-ancestors 'none'");
            }
        }
    }

    @Test
    void the_application_boots_signs_in_and_navigates_without_a_violation() {
        driver.get(base + "/");
        await("vaadin-select.theme-switch");
        assertThat(violations()).isEmpty();

        driver.get(base + "/findings");
        await("vaadin-login-form input[name=password]");
        assertThat(violations()).isEmpty();
        driver.executeScript("""
                document.querySelector('vaadin-login-form vaadin-text-field').value = 'team';
                document.querySelector('vaadin-login-form vaadin-password-field').value = 'team';
                document.querySelector('vaadin-login-form vaadin-button[slot=submit]').click();
                """);

        // The destinations are links in the internal bar since D-203.
        await("a.internal-nav-link");
        assertThat(driver.getCurrentUrl()).contains("/findings");
        assertThat(violations()).isEmpty();

        driver.findElement(By.cssSelector(".sign-out")).click();
        new WebDriverWait(driver, Duration.ofSeconds(20)).until(d -> !d.getCurrentUrl().contains("/findings"));
        await("vaadin-select.theme-switch");
        assertThat(violations()).isEmpty();
    }

    @Test
    void a_script_from_another_origin_is_blocked() {
        driver.get(base + "/");
        await("vaadin-select.theme-switch");
        assertThat(violations()).isEmpty();

        driver.executeScript("""
                const foreign = document.createElement('script');
                foreign.src = 'https://example.invalid/not-ours.js';
                document.head.appendChild(foreign);
                """);

        new WebDriverWait(driver, Duration.ofSeconds(10)).until(d -> !violations().isEmpty());
        assertThat(violations()).anyMatch(violation -> violation.startsWith("script-src"));
    }

    /**
     * The workbench hands a verdict to GitHub's new file editor through a GET form, so
     * {@code form-action} admits github.com beside the application, D-128, and nothing else.
     * The admitted target is checked on the header only: submitting to github.com is a
     * person's act, never a test's.
     */
    @Test
    void forms_go_to_the_application_and_github_only() {
        driver.get(base + "/");
        await("vaadin-select.theme-switch");

        assertThat(header("/", "content-security-policy")).contains("form-action 'self' https://github.com;");

        driver.executeScript("""
                const form = document.createElement('form');
                form.method = 'get';
                form.action = 'https://example.invalid/elsewhere';
                document.body.appendChild(form);
                form.submit();
                """);

        new WebDriverWait(driver, Duration.ofSeconds(10)).until(d -> !violations().isEmpty());
        assertThat(violations()).anyMatch(violation -> violation.startsWith("form-action"));
    }

    private void await(String selector) {
        new WebDriverWait(driver, Duration.ofSeconds(30))
                .until(d -> !d.findElements(By.cssSelector(selector)).isEmpty());
    }

    private String header(String path, String name) {
        return (String) driver.executeAsyncScript("""
                const done = arguments[arguments.length - 1];
                fetch(arguments[0], { redirect: 'manual' })
                    .then(response => done(response.headers.get(arguments[1])));
                """, path, name);
    }

    @SuppressWarnings("unchecked")
    private List<String> violations() {
        return (List<String>) driver.executeScript("return window.__violations");
    }
}

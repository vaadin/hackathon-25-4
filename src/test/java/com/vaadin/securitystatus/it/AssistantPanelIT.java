package com.vaadin.securitystatus.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * AST-06: one exchange with the assistant in a real browser against the
 * cassette provider, typed and sent as a person does, the answer rendered as
 * Markdown, the camera offered on the picture button, and not one violation of
 * the enforced Content Security Policy on the way.
 */
@ExtendWith(FixturePagesServer.class)
class AssistantPanelIT {

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
        options.addArguments("--window-size=1400,1000");
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
    void one_exchange_in_the_panel_message_in_answer_rendered() {
        driver.get(base + "/findings");
        await("vaadin-login-form input[name=password]");
        driver.findElement(By.cssSelector("vaadin-login-form input[name=username]")).sendKeys("team");
        WebElement password = driver.findElement(By.cssSelector("vaadin-login-form input[name=password]"));
        password.sendKeys("team");
        password.sendKeys(Keys.ENTER);
        // The panel is on the right of the internal shell, shut until the Assistant button opens it, D-203.
        await("vaadin-button.assistant-toggle");
        driver.findElement(By.cssSelector("vaadin-button.assistant-toggle")).click();
        await(".assistant-panel vaadin-message-input textarea");

        assertThat(driver.findElement(By.cssSelector(".assistant-mock")).getText())
                .startsWith("Recorded answers: no model is configured here");
        assertThat(driver.executeScript(
                "return document.querySelector('.assistant-panel vaadin-upload-button').capture"))
                .isEqualTo("environment");

        WebElement field = driver.findElement(By.cssSelector(".assistant-panel vaadin-message-input textarea"));
        field.click();
        field.sendKeys("Is 24.9.21 safe?");
        field.sendKeys(Keys.ENTER);

        new WebDriverWait(driver, Duration.ofSeconds(30)).until(d -> messages().size() >= 2
                && messages().getLast().contains("Source:"));
        List<String> messages = messages();
        assertThat(messages.getFirst()).contains("Is 24.9.21 safe?");
        assertThat(messages.getLast()).contains("The measurements of 24.9.21 say fix_available")
                .contains("scanned 2026-11-15T03:57:22Z").contains("cases/v/24.9.21.json");
        // Markdown rendered, not shown as asterisks.
        assertThat(messages.getLast()).doesNotContain("**");
        assertThat((Long) driver.executeScript(
                "return document.querySelectorAll('.assistant-panel vaadin-message strong').length")).isPositive();

        assertThat(violations()).isEmpty();
    }

    @SuppressWarnings("unchecked")
    private List<String> messages() {
        return (List<String>) driver.executeScript("""
                return [...document.querySelectorAll('.assistant-panel vaadin-message')]
                    .map(message => message.innerText);
                """);
    }

    private void await(String selector) {
        new WebDriverWait(driver, Duration.ofSeconds(30))
                .until(d -> !d.findElements(By.cssSelector(selector)).isEmpty());
    }

    @SuppressWarnings("unchecked")
    private List<String> violations() {
        return (List<String>) driver.executeScript("return window.__violations");
    }
}

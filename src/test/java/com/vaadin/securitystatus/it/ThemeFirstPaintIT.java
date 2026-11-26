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
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.vaadin.securitystatus.ui.common.ThemeMemory;

/**
 * FND-04. A probe installed before the document exists samples the text color
 * the root element resolves to on every rendered frame, so a single light frame
 * ahead of the dark one is caught.
 */
@ExtendWith(FixturePagesServer.class)
class ThemeFirstPaintIT {

    private static final String PROBE = """
            window.__frames = [];
            (function sample() {
              if (document.documentElement) {
                window.__frames.push(getComputedStyle(document.documentElement).color);
              }
              if (window.__frames.length < 600) {
                requestAnimationFrame(sample);
              }
            })();
            """;

    private ChromeDriver driver;
    private String home;

    @BeforeEach
    void openBrowserWithADarkSystem() {
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
        driver.executeCdpCommand("Emulation.setEmulatedMedia",
                Map.of("features", List.of(Map.of("name", "prefers-color-scheme", "value", "dark"))));
        driver.executeCdpCommand("Page.addScriptToEvaluateOnNewDocument", Map.of("source", PROBE));
        home = "http://localhost:" + System.getProperty("it.port", "8080") + "/";
        // Signing in navigates twice, and the probe starts a fresh window.__frames on every new
        // document, so the frames each test asserts on belong to the page it opened itself.
        SignIn.as(driver, home, "guest");
    }

    @AfterEach
    void closeBrowser() {
        driver.quit();
    }

    @Test
    void a_dark_system_preference_paints_dark_from_the_first_frame() {
        driver.get(home);
        awaitSwitch();

        assertThat(frames()).isNotEmpty().allMatch(this::isLightText);
        assertThat(chosen()).isEqualTo("system");
    }

    @Test
    void a_choice_overrides_the_system_survives_a_reload_and_system_forgets_it() {
        driver.get(home);
        awaitSwitch();
        assertThat(frames()).allMatch(this::isLightText);

        choose("Light");
        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(d -> "light"
                        .equals(driver.executeScript("return localStorage.getItem(arguments[0])", ThemeMemory.KEY)));
        assertThat(isLightText(currentColor())).isFalse();

        driver.navigate().refresh();
        awaitSwitch();

        assertThat(frames()).isNotEmpty().noneMatch(this::isLightText);
        assertThat(chosen()).isEqualTo("light");

        choose("System");
        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(d -> driver.executeScript("return localStorage.getItem(arguments[0])", ThemeMemory.KEY) == null);
        assertThat(isLightText(currentColor())).isTrue();
    }

    /** Clicks the scheme button until it is on the scheme the label names, at most once round. */
    private void choose(String label) {
        String wanted = label.toLowerCase(java.util.Locale.ROOT);
        for (int i = 0; i < 3 && !wanted.equals(chosen()); i++) {
            String before = chosen();
            driver.findElement(By.cssSelector("vaadin-button.theme-switch")).click();
            new WebDriverWait(driver, Duration.ofSeconds(10)).until(d -> !before.equals(chosen()));
        }
    }

    private void awaitSwitch() {
        new WebDriverWait(driver, Duration.ofSeconds(30))
                .until(d -> !d.findElements(By.cssSelector("vaadin-button.theme-switch")).isEmpty());
        // The menu learns the remembered choice from the browser one round trip after it is attached.
        new WebDriverWait(driver, Duration.ofSeconds(10)).until(d -> !chosen().isEmpty());
    }

    private String chosen() {
        Object scheme = driver.executeScript(
                "return document.querySelector('vaadin-button.theme-switch')?.getAttribute('data-scheme') ?? ''");
        return String.valueOf(scheme);
    }

    private String currentColor() {
        return (String) driver.executeScript("return getComputedStyle(document.documentElement).color");
    }

    @SuppressWarnings("unchecked")
    private List<String> frames() {
        return (List<String>) driver.executeScript("return window.__frames");
    }

    /** Light text means a dark page. Works for rgb(), rgba() and oklch() alike. */
    private boolean isLightText(String cssColor) {
        Object lightness = driver.executeScript("""
                const context = document.createElement('canvas').getContext('2d', { willReadFrequently: true });
                context.fillStyle = arguments[0];
                context.fillRect(0, 0, 1, 1);
                const [red, green, blue] = context.getImageData(0, 0, 1, 1).data;
                return (red + green + blue) / 3;
                """, cssColor);
        return ((Number) lightness).doubleValue() > 128;
    }
}

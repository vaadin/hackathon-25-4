package com.vaadin.securitystatus.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.vaadin.securitystatus.base.ui.ThemeMemory;

/**
 * FND-04. A probe installed before the document exists samples the text color
 * the root element resolves to on every rendered frame, so a single light frame
 * ahead of the dark one is caught.
 */
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
        driver = new ChromeDriver(options);
        driver.executeCdpCommand("Emulation.setEmulatedMedia",
                Map.of("features", List.of(Map.of("name", "prefers-color-scheme", "value", "dark"))));
        driver.executeCdpCommand("Page.addScriptToEvaluateOnNewDocument", Map.of("source", PROBE));
        home = "http://localhost:" + System.getProperty("it.port", "8080") + "/";
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
        assertThat(switchedOn()).isTrue();
    }

    @Test
    void the_switch_overrides_the_system_and_survives_a_reload() {
        driver.get(home);
        awaitSwitch();
        assertThat(frames()).allMatch(this::isLightText);

        driver.findElement(By.cssSelector("vaadin-switch input")).click();
        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(d -> "light"
                        .equals(driver.executeScript("return localStorage.getItem(arguments[0])", ThemeMemory.KEY)));
        assertThat(isLightText(currentColor())).isFalse();

        driver.navigate().refresh();
        awaitSwitch();

        assertThat(frames()).isNotEmpty().noneMatch(this::isLightText);
        assertThat(switchedOn()).isFalse();
    }

    private void awaitSwitch() {
        new WebDriverWait(driver, Duration.ofSeconds(30))
                .until(d -> !d.findElements(By.cssSelector("vaadin-switch input")).isEmpty());
        // The switch learns its position from the browser one round trip after it is attached.
        new WebDriverWait(driver, Duration.ofSeconds(10)).until(d -> isLightText(currentColor()) == switchedOn());
    }

    private boolean switchedOn() {
        return (Boolean) driver.executeScript("return document.querySelector('vaadin-switch').checked");
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

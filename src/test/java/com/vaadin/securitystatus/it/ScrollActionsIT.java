package com.vaadin.securitystatus.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * VER-10, and the viewport half of AC3: What it fixes brings the fixes list to
 * the top of the viewport, an address naming a finding brings it into view and
 * marks it, a section toggle leaves the reader where they were, and a new
 * version starts at the top. Every one of these is a fact about the viewport,
 * which only a browser has. Clicks are real clicks.
 */
@ExtendWith(FixturePagesServer.class)
class ScrollActionsIT {

    private ChromeDriver driver;
    private WebDriverWait wait;
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
        wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        base = "http://localhost:" + System.getProperty("it.port", "8080") + "/";
        SignIn.as(driver, base, "guest");
    }

    @AfterEach
    void closeBrowser() {
        driver.quit();
    }

    @Test
    void what_it_fixes_brings_the_fixes_list_into_view() {
        driver.get(base + "version/24.9.21");
        WebElement button = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".what-it-fixes")));
        assertThat(scrollY()).isZero();
        double before = top(".version-fixes");
        assertThat(before).isGreaterThan(100);

        button.click();

        wait.until(d -> Math.abs(top(".version-fixes")) < 2);
        assertThat(scrollY()).isGreaterThan(100);
        assertThat(visible(".version-fixes .fix-row")).isTrue();
    }

    @Test
    void an_address_naming_a_finding_brings_it_into_view_and_marks_it() {
        driver.get(base + "version/24.4.28?open=CVE-2026-59314");
        String card = ".finding-card[data-advisory='CVE-2026-59314']";
        wait.until(d -> !d.findElements(By.cssSelector(card + ".revealed")).isEmpty());
        wait.until(d -> inViewport(card));

        assertThat(scrollY()).isGreaterThan(1000);
        assertThat(visible(card)).isTrue();
        assertThat(driver.executeScript("return document.querySelector(arguments[0] + \" vaadin-details[data-layer='means']\").opened",
                card)).isEqualTo(true);
        // The mark is an animation that runs once and fades, about two seconds.
        assertThat(driver.executeScript("return getComputedStyle(document.querySelector(arguments[0])).animationName",
                card)).isEqualTo("revealed");
        assertThat(driver.executeScript("return getComputedStyle(document.querySelector(arguments[0])).animationDuration",
                card)).isEqualTo("2.4s");
    }

    @Test
    void a_section_toggle_writes_the_address_and_leaves_the_reader_where_they_were() {
        driver.get(base + "version/24.9.21");
        WebElement show = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".boot-note a")));
        driver.executeScript("arguments[0].scrollIntoView({block: 'center'})", show);
        wait.until(d -> scrollY() > 200);
        long before = scrollY();

        show.click();

        wait.until(ExpectedConditions.urlContains("boot=1"));
        wait.until(d -> !d.findElements(By.cssSelector(".version-boot table")).isEmpty());
        assertThat(Math.abs(scrollY() - before)).isLessThanOrEqualTo(2);

        driver.navigate().back();
        wait.until(d -> !d.findElements(By.cssSelector(".boot-note")).isEmpty());
        assertThat(driver.getCurrentUrl()).doesNotContain("boot=");
    }

    @Test
    void a_new_version_starts_at_the_top() {
        driver.get(base + "version/24.9.21");
        WebElement other = wait.until(ExpectedConditions
                .presenceOfElementLocated(By.cssSelector(".lines-table a[href='version/24.4.28']")));
        driver.executeScript("arguments[0].scrollIntoView({block: 'center'})", other);
        wait.until(d -> scrollY() > 500);

        other.click();

        wait.until(ExpectedConditions.urlContains("version/24.4.28"));
        wait.until(d -> !d.findElements(By.cssSelector(".version-nofix .finding-card")).isEmpty());
        wait.until(d -> scrollY() == 0);
    }

    private long scrollY() {
        return ((Number) driver.executeScript("return Math.round(window.scrollY)")).longValue();
    }

    private double top(String selector) {
        return ((Number) driver.executeScript("return document.querySelector(arguments[0]).getBoundingClientRect().top",
                selector)).doubleValue();
    }

    private boolean inViewport(String selector) {
        return Boolean.TRUE.equals(driver.executeScript("const r = document.querySelector(arguments[0]).getBoundingClientRect();"
                + " return r.top >= -1 && r.top < window.innerHeight", selector));
    }

    private boolean visible(String selector) {
        return Boolean.TRUE.equals(driver.executeScript("return document.querySelector(arguments[0]).checkVisibility()",
                selector));
    }
}

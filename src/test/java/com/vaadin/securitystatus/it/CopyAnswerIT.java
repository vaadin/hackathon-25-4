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
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * CVE-06: Copy answer for your ticket puts the card's own words on the
 * clipboard and one notice says so. Only a real browser has a clipboard, and
 * it grants the write only inside a real click, so the click is a real one.
 * The permission a person would grant is granted through the DevTools
 * protocol, and a refused write is the negative control.
 */
@ExtendWith(FixturePagesServer.class)
class CopyAnswerIT {

    private static final String EXPECTED = "Fix available: CVE-2026-89407 is fixed in 24.9.26\n"
            + "You are on 24.9.21, which is affected. Upgrade to 24.9.26, a maintenance release in the minor you"
            + " already use, and you are secure.\nScanned 2026-11-15";

    private static final String REFUSE = """
            for (const name of ['write', 'writeText']) {
              navigator.clipboard[name] = () => Promise.reject(new DOMException('Refused', 'NotAllowedError'));
            }
            """;

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
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        base = "http://localhost:" + System.getProperty("it.port", "8080") + "/";
    }

    @AfterEach
    void closeBrowser() {
        driver.quit();
    }

    @Test
    void the_clipboard_receives_the_cards_words_and_one_notice_confirms() {
        permit("granted");
        driver.get(base + "cve/CVE-2026-89407/24.9.21");
        WebElement copy = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".copy-answer")));
        assertThat(copy.getAttribute("data-answer")).isEqualTo(EXPECTED);

        copy.click();

        WebElement notice = wait.until(d -> d.findElements(By.cssSelector("vaadin-notification-card")).stream()
                .filter(WebElement::isDisplayed).findFirst().orElse(null));
        assertThat(notice.getText()).isEqualTo("The answer for CVE-2026-89407 is on your clipboard");
        assertThat(driver.findElements(By.cssSelector("vaadin-notification-card"))).hasSize(1);
        assertThat(clipboard()).isEqualTo(EXPECTED);
    }

    @Test
    void a_refused_clipboard_is_said_and_nothing_is_claimed_copied() {
        // Chrome grants a write inside a click whatever the permission says, so the refusal a browser
        // gives an unfocused or locked down page is played by the page's own clipboard.
        driver.executeCdpCommand("Page.addScriptToEvaluateOnNewDocument", Map.of("source", REFUSE));
        driver.get(base + "cve/CVE-2026-89407/24.9.21");
        WebElement copy = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".copy-answer")));

        copy.click();

        WebElement notice = wait.until(d -> d.findElements(By.cssSelector("vaadin-notification-card")).stream()
                .filter(WebElement::isDisplayed).findFirst().orElse(null));
        assertThat(notice.getText()).isEqualTo("Your browser did not let this page write to the clipboard");
    }

    private void permit(String setting) {
        String origin = base.substring(0, base.length() - 1);
        for (String name : List.of("clipboard-write", "clipboard-read")) {
            driver.executeCdpCommand("Browser.setPermission",
                    Map.of("permission", Map.of("name", name, "allowWithoutSanitization", true), "setting", setting,
                            "origin", origin));
        }
    }

    private String clipboard() {
        permit("granted");
        return (String) driver.executeAsyncScript(
                "const done = arguments[arguments.length - 1];"
                        + " navigator.clipboard.readText().then(done, error => done('refused: ' + error.name));");
    }
}

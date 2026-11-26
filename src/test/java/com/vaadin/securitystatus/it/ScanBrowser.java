package com.vaadin.securitystatus.it;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * A real Chrome on the scan screen, shared by {@link ScanUploadIT} and
 * {@link TriageDownloadIT}: a report sent through the upload button's own file
 * input, every policy violation recorded from the first byte of each page,
 * and the address of a download read from its button, so it is fetched
 * rather than clicked through to the disk.
 */
final class ScanBrowser implements AutoCloseable {

    /** Installed before any script of the page runs. */
    private static final String PROBE = """
            window.__violations = [];
            document.addEventListener('securitypolicyviolation',
                event => window.__violations.push(event.effectiveDirective + ' ' + event.blockedURI));
            """;

    final ChromeDriver driver;
    final String base;

    ScanBrowser() {
        var options = new ChromeOptions();
        if (Boolean.parseBoolean(System.getProperty("it.headless", "true"))) {
            options.addArguments("--headless=new");
        }
        options.addArguments("--window-size=1280,900");
        options.setExperimentalOption("prefs", Map.of("credentials_enable_service", false,
                "profile.password_manager_enabled", false, "profile.password_manager_leak_detection", false));
        driver = new ChromeDriver(options);
        driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(30));
        driver.executeCdpCommand("Page.addScriptToEvaluateOnNewDocument", Map.of("source", PROBE));
        base = "http://localhost:" + System.getProperty("it.port", "8080");
        SignIn.as(driver, base, "guest");
    }

    /** Home, the report tab, and the fixture through the upload button's file input. */
    void upload(String fixture) {
        driver.get(base + "/");
        await("vaadin-tab");
        driver.findElements(By.cssSelector("vaadin-tab")).get(2).click();
        await(".lookup-report-choose");
        uploadHere(fixture);
    }

    void uploadHere(String fixture) {
        WebElement button = driver.findElement(By.cssSelector(".lookup-report-choose"));
        WebElement input = button.getShadowRoot().findElement(By.cssSelector("input[type=file]"));
        input.sendKeys(fixturePath(fixture).toString());
    }

    static Path fixturePath(String fixture) {
        try {
            return Path.of(ScanBrowser.class.getResource("/scan/" + fixture).toURI());
        } catch (URISyntaxException wrong) {
            throw new IllegalStateException(wrong);
        }
    }

    /**
     * The address a {@code Download.onClick} button downloads from, read without
     * clicking it. The platform writes it into an attribute of the button named
     * {@code data-flow-download-} and an id; that name is not documented, and it
     * is the only place the address is, draft 16.
     */
    String downloadAddressOf(String selector) {
        return (String) driver.executeScript("""
                const button = document.querySelector(arguments[0]);
                const name = button.getAttributeNames().find(name => name.startsWith('data-flow-download-'));
                return name ? button.getAttribute(name) : null;
                """, selector);
    }

    @SuppressWarnings("unchecked")
    List<String> violations() {
        return (List<String>) driver.executeScript("return window.__violations");
    }

    /** The response to a GET of the address, from the page, with the reader's session. */
    @SuppressWarnings("unchecked")
    Map<String, Object> fetch(String href) {
        return (Map<String, Object>) driver.executeAsyncScript("""
                const done = arguments[arguments.length - 1];
                fetch(arguments[0], { redirect: 'manual' }).then(async response => {
                    const body = new Uint8Array(await response.arrayBuffer());
                    done({
                        status: response.status,
                        type: response.headers.get('content-type'),
                        disposition: response.headers.get('content-disposition'),
                        bytes: body.byteLength,
                        head: String.fromCharCode(...body.slice(0, 5)),
                        text: new TextDecoder().decode(body)
                    });
                }).catch(error => done({ status: -1, text: String(error) }));
                """, href);
    }

    void await(String selector) {
        new WebDriverWait(driver, Duration.ofSeconds(30))
                .until(d -> !d.findElements(By.cssSelector(selector)).isEmpty());
    }

    void awaitText(String selector, String text) {
        new WebDriverWait(driver, Duration.ofSeconds(30)).until(d -> d.findElements(By.cssSelector(selector))
                .stream().anyMatch(element -> element.getText().contains(text)));
    }

    @Override
    public void close() {
        driver.quit();
    }
}

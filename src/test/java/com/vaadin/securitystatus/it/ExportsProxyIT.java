package com.vaadin.securitystatus.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * DAT-08. A per release file asked of this application arrives as the
 * publisher has it: the same bytes, the publisher's content type, and a
 * disposition that names the file. The publisher here is
 * {@link FixturePagesServer}, so the bytes cross two real HTTP connections.
 *
 * <p>
 * The download is fetched from the page, with the session of the signed in
 * reader, and never clicked.
 */
@ExtendWith(FixturePagesServer.class)
class ExportsProxyIT {

    private static final String RELEASE = "25.3.5";

    /** The suffix of each file, and the content type the live host served it with on the day of the fixtures. */
    private static final Map<String, String> PUBLISHED = Map.of("csv", "text/csv; charset=utf-8", "vex.json",
            "application/json; charset=utf-8", "dependency-check-suppressions.xml", "application/xml",
            "osv-scanner.toml", "application/toml", "bomber-ignore.txt", "text/plain; charset=utf-8");

    private ChromeDriver driver;
    private String base;

    @BeforeEach
    void openBrowserAndSignIn() {
        var options = new ChromeOptions();
        if (Boolean.parseBoolean(System.getProperty("it.headless", "true"))) {
            options.addArguments("--headless=new");
        }
        options.addArguments("--window-size=1280,900");
        options.setExperimentalOption("prefs", Map.of("credentials_enable_service", false,
                "profile.password_manager_enabled", false, "profile.password_manager_leak_detection", false));
        driver = new ChromeDriver(options);
        driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(30));
        base = "http://localhost:" + System.getProperty("it.port", "8080");

        driver.get(base + "/findings");
        await("vaadin-login-form input[name=password]");
        driver.executeScript("""
                document.querySelector('vaadin-login-form vaadin-text-field').value = 'team';
                document.querySelector('vaadin-login-form vaadin-password-field').value = 'team';
                document.querySelector('vaadin-login-form vaadin-button[slot=submit]').click();
                """);
        await("a.release-file");
    }

    @AfterEach
    void closeBrowser() {
        driver.quit();
    }

    // DAT-08
    @Test
    void each_file_of_a_release_streams_through_with_its_bytes_type_and_name() {
        assertThat(linksOf(RELEASE)).hasSize(PUBLISHED.size());

        PUBLISHED.forEach((suffix, contentType) -> {
            String fileName = RELEASE + "." + suffix;
            byte[] published = FixturePagesServer.fixture("exports/" + fileName);

            Map<String, Object> download = fetch(hrefEndingIn(fileName));

            assertThat(download.get("status")).as(fileName).isEqualTo(200L);
            // The servlet container prints the header in its own spelling, without the space
            // before the charset. The type and the charset are what has to arrive.
            assertThat(sameSpelling((String) download.get("type"))).as(fileName)
                    .isEqualTo(sameSpelling(contentType));
            assertThat((String) download.get("disposition")).as(fileName).startsWith("attachment")
                    .contains(fileName);
            assertThat(download.get("bytes")).as(fileName).isEqualTo((long) published.length);
            assertThat((String) download.get("text")).as(fileName)
                    .isEqualTo(new String(published, StandardCharsets.UTF_8));
        });
    }

    @Test
    void a_file_the_publisher_does_not_have_is_a_404_and_not_an_empty_download() {
        // Every line offers its files, and the fixtures hold those of one release only.
        String missing = "24.9.26.csv";

        Map<String, Object> download = fetch(hrefEndingIn(missing));

        assertThat(download.get("status")).isEqualTo(404L);
    }

    @Test
    void a_reader_who_is_not_signed_in_gets_no_file() {
        String href = hrefEndingIn(RELEASE + ".csv");
        driver.manage().deleteAllCookies();

        Map<String, Object> download = fetch(href);

        assertThat(download.get("status")).isNotEqualTo(200L);
        assertThat((String) download.get("text")).doesNotContain("CVE-");
    }

    private static String sameSpelling(String contentType) {
        return contentType.replaceAll("\\s*;\\s*", ";").toLowerCase(Locale.ROOT);
    }

    private List<WebElement> linksOf(String release) {
        return driver.findElements(By.cssSelector("a.release-file[href*='" + release + ".']"));
    }

    private String hrefEndingIn(String fileName) {
        return driver.findElements(By.cssSelector("a.release-file")).stream().map(link -> link.getAttribute("href"))
                .filter(href -> href != null && href.endsWith("/" + fileName)).findFirst().orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetch(String href) {
        return (Map<String, Object>) driver.executeAsyncScript("""
                const done = arguments[arguments.length - 1];
                fetch(arguments[0], { redirect: 'manual' }).then(async response => {
                    const body = await response.arrayBuffer();
                    done({
                        status: response.status,
                        type: response.headers.get('content-type'),
                        disposition: response.headers.get('content-disposition'),
                        bytes: body.byteLength,
                        text: new TextDecoder().decode(body)
                    });
                }).catch(error => done({ status: -1, text: String(error) }));
                """, href);
    }

    private void await(String selector) {
        new WebDriverWait(driver, Duration.ofSeconds(30))
                .until(d -> !d.findElements(By.cssSelector(selector)).isEmpty());
    }
}

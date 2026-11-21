package com.vaadin.securitystatus.it;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;

/**
 * SCN-06. A real file through the real upload: the osv-scanner fixture, one
 * scan of an application on 24.9.21, read on the server, triaged, and drawn
 * with its headline, its buckets and its table, under the enforced policy.
 */
@ExtendWith(FixturePagesServer.class)
class ScanUploadIT {

    private ScanBrowser browser;

    @BeforeEach
    void openBrowser() {
        browser = new ScanBrowser();
    }

    @AfterEach
    void closeBrowser() {
        browser.close();
    }

    // SCN-06
    @Test
    void a_report_uploaded_in_a_browser_is_answered_with_headline_buckets_and_table() {
        browser.upload("osv-scanner.json");

        browser.awaitText(".scan-headline", "One upgrade clears");
        var driver = browser.driver;
        assertThat(driver.getCurrentUrl()).endsWith("/scan/24.9.21");
        assertThat(driver.findElement(By.cssSelector(".scan-headline")).getText())
                .isEqualTo("One upgrade clears 1 of your 6 open findings");
        assertThat(driver.findElements(By.cssSelector(".scan-bucket-count"))).extracting(element -> element.getText())
                .containsExactly("3", "1", "5");
        assertThat(driver.findElements(By.cssSelector(".scan-row"))).hasSize(6);
        assertThat(driver.findElement(By.cssSelector(".lookup-report-formats")).getText())
                .contains("osv-scanner.json").contains("Vaadin 24.9.21").contains("9 findings");
        assertThat(browser.violations()).isEmpty();
    }

    @Test
    void a_file_that_is_no_report_is_refused_in_the_upload_area_and_nothing_navigates() {
        browser.upload("not-a-report.json");

        browser.awaitText(".lookup-report-refused", "is not a");
        assertThat(browser.driver.findElement(By.cssSelector(".lookup-report-refused")).getText())
                .contains("CycloneDX, OWASP Dependency-Check, osv-scanner or bomber");
        assertThat(browser.driver.getCurrentUrl()).doesNotContain("/scan");
        assertThat(browser.violations()).isEmpty();
    }
}

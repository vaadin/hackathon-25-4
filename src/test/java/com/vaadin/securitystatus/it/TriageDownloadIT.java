package com.vaadin.securitystatus.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * SCN-07 and SCN-08. The triage leaves as a CSV and as a PDF with the right
 * type, name and content. Each download's address is read from its button,
 * never clicked, and fetched from the page with the reader's session.
 */
@ExtendWith(FixturePagesServer.class)
class TriageDownloadIT {

    private ScanBrowser browser;

    @BeforeEach
    void uploadTheReport() {
        browser = new ScanBrowser();
        browser.upload("osv-scanner.json");
        browser.awaitText(".scan-headline", "One upgrade clears");
    }

    @AfterEach
    void closeBrowser() {
        browser.close();
    }

    // SCN-07, AC3
    @Test
    void the_csv_carries_every_finding_in_seven_columns_named_for_the_version() {
        String href = browser.downloadAddressOf(".scan-download-csv");

        Map<String, Object> csv = browser.fetch(href);

        assertThat(csv.get("status")).isEqualTo(200L);
        assertThat(((String) csv.get("type")).replace(" ", "")).isEqualTo("text/csv;charset=utf-8");
        assertThat((String) csv.get("disposition")).startsWith("attachment")
                .contains("vaadin-security-triage-24.9.21.csv");
        assertThat(href).endsWith("/vaadin-security-triage-24.9.21.csv");
        String[] lines = ((String) csv.get("text")).split("\r\n");
        // The prototype's three comment lines, then its columns in its order and the verdict, D-197.
        assertThat(lines[0]).isEqualTo("# Vaadin security status, triage of an uploaded report");
        assertThat(lines[1]).isEqualTo("# Vaadin version: 24.9.21");
        assertThat(lines[2]).isEqualTo("# Measurements of: 2026-11-21T10:13:39Z");
        assertThat(lines[3]).isEqualTo(
                "\"CVE\",\"Bucket\",\"Owner\",\"Component\",\"Severity\",\"Action\",\"Verdict\"");
        // The whole triage, the secure findings the table hides included.
        assertThat(lines).hasSize(4 + 9);
        assertThat(lines[5]).isEqualTo("\"CVE-2026-89407\",\"Fix available\",\"Vaadin\","
                + "\"pkg:maven/com.fasterxml.jackson.core/jackson-core@2.18.2\",\"high\",\"Upgrade to 24.9.26\","
                + "\"Fix available\"");
        assertThat(browser.violations()).isEmpty();
    }

    // SCN-08, AC3
    @Test
    void the_pdf_is_previewed_in_the_viewer_and_downloads_as_a_pdf() {
        var driver = browser.driver;
        driver.findElement(By.cssSelector(".scan-preview-pdf")).click();
        browser.await("vaadin-pdf-viewer");

        // The viewer has the document and has drawn its first page.
        new WebDriverWait(driver, Duration.ofSeconds(30)).until(d -> Boolean.TRUE.equals(driver.executeScript("""
                const viewer = document.querySelector('vaadin-pdf-viewer');
                if (!viewer || viewer.pageCount < 1) return false;
                const drawn = root => [...root.querySelectorAll('*')].some(node =>
                    (node.localName === 'canvas' && node.width > 0 && node.height > 0)
                    || (node.shadowRoot && drawn(node.shadowRoot)));
                return drawn(viewer.shadowRoot || viewer) || drawn(viewer);
                """)));
        assertThat(driver.executeScript("return document.querySelector('vaadin-pdf-viewer').pageCount"))
                .isEqualTo(1L);
        String shown = (String) driver.executeScript("return document.querySelector('vaadin-pdf-viewer').src");
        Map<String, Object> previewed = browser.fetch(shown);
        assertThat(previewed.get("status")).isEqualTo(200L);
        assertThat(previewed.get("type")).isEqualTo("application/pdf");
        assertThat(previewed.get("head")).isEqualTo("%PDF-");

        String href = browser.downloadAddressOf(".scan-download-pdf");
        Map<String, Object> pdf = browser.fetch(href);

        assertThat(pdf.get("status")).isEqualTo(200L);
        assertThat(pdf.get("type")).isEqualTo("application/pdf");
        assertThat((String) pdf.get("disposition")).startsWith("attachment")
                .contains("vaadin-security-audit-24.9.21.pdf");
        assertThat(pdf.get("head")).isEqualTo("%PDF-");
        assertThat(browser.violations()).isEmpty();
    }
}

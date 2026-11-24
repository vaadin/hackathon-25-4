package com.vaadin.securitystatus.data;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import com.vaadin.browserless.BrowserlessApplicationContext;
import com.vaadin.browserless.BrowserlessUIContext;
import com.vaadin.browserless.SpringBrowserlessApplicationContext;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.securitystatus.Application;
import com.vaadin.securitystatus.ui.customer.HomeView;

/**
 * DAT-05. The nightly publish reaches every open session: the measured chip
 * and the home cards move without a reload. The index fixture is pinned at
 * {@code generated_at} 2026-11-21T10:13:39Z.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class RefreshBroadcastBrowserlessTest {

    @Autowired
    ApplicationContext spring;

    @Autowired
    FixturePages fixtures;

    private BrowserlessApplicationContext app;

    @BeforeEach
    void open() {
        app = SpringBrowserlessApplicationContext.createSecured(spring, Application.class);
    }

    @AfterEach
    void close() {
        app.close();
        fixtures.restore();
    }

    // DAT-05
    @Test
    void a_moved_generated_at_updates_the_measured_chip_of_two_sessions() {
        BrowserlessUIContext first = app.newUser().newWindow();
        BrowserlessUIContext second = app.newUser().newWindow();
        first.navigate(HomeView.class);
        second.navigate(HomeView.class);
        assertThat(first.getUI().getSession()).isNotSameAs(second.getUI().getSession());
        assertThat(chip(first)).isEqualTo("Measured 2026-11-21");
        assertThat(chip(second)).isEqualTo("Measured 2026-11-21");

        fixtures.moveGeneratedAt(Instant.parse("2026-11-22T03:17:42Z"));
        fixtures.revalidate();
        first.runPendingSignalsTasks();
        second.runPendingSignalsTasks();

        assertThat(chip(first)).isEqualTo("Measured 2026-11-22");
        assertThat(chip(second)).isEqualTo("Measured 2026-11-22");
    }

    // DAT-05
    @Test
    void the_home_cards_of_two_sessions_show_the_new_measurement() {
        BrowserlessUIContext first = app.newUser().newWindow();
        BrowserlessUIContext second = app.newUser().newWindow();
        first.navigate(HomeView.class);
        second.navigate(HomeView.class);
        assertThat(newestReleases(first)).contains("24.9.26").doesNotContain("24.9.27");

        // The next night's publish measured a newer release of 24.9.
        String moved = fixtures.transport().fixture(SecurityDataService.INDEX)
                .replace("\"generated_at\":\"2026-11-21T10:13:39Z\"", "\"generated_at\":\"2026-11-22T03:17:42Z\"")
                .replace("\"line\":\"24.9\",\"version\":\"24.9.26\"", "\"line\":\"24.9\",\"version\":\"24.9.27\"")
                .replace("\"measured\":\"24.9.26\"", "\"measured\":\"24.9.27\"");
        assertThat(moved).contains("\"line\":\"24.9\",\"version\":\"24.9.27\"").contains("\"measured\":\"24.9.27\"");
        fixtures.transport().replace(SecurityDataService.INDEX, moved);
        fixtures.revalidate();
        first.runPendingSignalsTasks();
        second.runPendingSignalsTasks();

        assertThat(newestReleases(first)).contains("24.9.27").doesNotContain("24.9.26");
        assertThat(newestReleases(second)).contains("24.9.27").doesNotContain("24.9.26");
    }

    @Test
    void an_unmoved_index_changes_nothing() {
        BrowserlessUIContext window = app.newUser().newWindow();
        window.navigate(HomeView.class);

        fixtures.revalidate();
        window.runPendingSignalsTasks();

        assertThat(chip(window)).isEqualTo("Measured 2026-11-21");
        assertThat(newestReleases(window)).hasSize(9);
    }

    private static String chip(BrowserlessUIContext window) {
        return window.find(Badge.class).withClassName("measured-chip").single().getText();
    }

    private static List<String> newestReleases(BrowserlessUIContext window) {
        // Each card links to the newest measured release of its line.
        return window.find(RouterLink.class).withClassName("line-card").all().stream()
                .map(card -> card.getHref().substring(card.getHref().lastIndexOf('/') + 1)).toList();
    }
}

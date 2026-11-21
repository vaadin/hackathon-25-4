package com.vaadin.securitystatus.data;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;

/**
 * The test configuration that points the data layer at the classpath. It sits
 * in the application's package, so the component scan of every
 * {@code @SpringBootTest} finds it: the application then reads the committed
 * fixtures through the same client, cache and mapper it uses against the live
 * site, and no test touches the network.
 *
 * <p>
 * A test that changes what the host answers injects this and calls
 * {@link #restore()} when it is done, because the Spring context, and with it
 * the cache and the refresh signal, is shared by every test class.
 */
@Configuration
public class FixturePages {

    /** The {@code generated_at} of the committed index fixture. */
    public static final Instant PINNED_GENERATED_AT = Instant.parse("2026-11-21T10:13:39Z");

    /** The moment every test runs at: the day of the pinned measurement. */
    public static final Instant NOW = Instant.parse("2026-11-21T12:00:00Z");

    private final FixtureTransport transport = new FixtureTransport();
    private final SecurityDataService service;
    private final DataRefreshSignal refresh;

    FixturePages(@Lazy SecurityDataService service, DataRefreshSignal refresh) {
        this.service = service;
        this.refresh = refresh;
    }

    /**
     * A data service over the committed fixtures with no Spring context, for a
     * unit test outside this package.
     */
    public static SecurityDataService offline() {
        var client = new PagesClient(FixtureTransport.BASE_URL, new FixtureTransport());
        return new SecurityDataService(new PagesCache(client, Clock.fixed(NOW, ZoneOffset.UTC)), client,
                new DataRefreshSignal(), Runnable::run);
    }

    @Bean(DataConfiguration.TRANSPORT)
    FixtureTransport pagesTransport() {
        return transport;
    }

    @Bean
    @Primary
    Clock fixedClock() {
        return Clock.fixed(NOW, ZoneOffset.UTC);
    }

    public FixtureTransport transport() {
        return transport;
    }

    /** Nothing cached, nothing known, and the host refusing connections. */
    public void coldAndDown() {
        transport.reset();
        transport.goDown();
        service.forget();
        refresh.forget();
    }

    /** Nothing cached and nothing known, with the host up: the next read fetches. */
    public void cold() {
        transport.reset();
        service.forget();
        refresh.forget();
    }

    /** Replaces the index with a copy that carries another {@code generated_at}. */
    public void moveGeneratedAt(Instant to) {
        transport.replace(SecurityDataService.INDEX, transport.fixture(SecurityDataService.INDEX)
                .replace("\"generated_at\":\"" + PINNED_GENERATED_AT + "\"", "\"generated_at\":\"" + to + "\""));
    }

    /** What the scheduled task does every fifteen minutes. */
    public void revalidate() {
        service.revalidate();
    }

    /** Back to what a fresh context has: the host up and the pinned index read. */
    public void restore() {
        cold();
        service.revalidate();
        transport.reset();
    }
}

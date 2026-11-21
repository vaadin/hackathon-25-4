package com.vaadin.securitystatus.data;

import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;

import org.springframework.beans.factory.ObjectProvider;

import com.vaadin.flow.server.VaadinSession;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Wires the data layer: where the published site is, how it is reached, and
 * when the index is asked whether it moved.
 */
@Configuration
@EnableScheduling
class DataConfiguration {

    /** The name a transport bean takes to stand in for the network. */
    static final String TRANSPORT = "pagesTransport";

    private final ObjectProvider<SecurityDataService> service;

    DataConfiguration(ObjectProvider<SecurityDataService> service) {
        this.service = service;
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    PagesClient pagesClient(@Value("${securitystatus.pages.base-url}") String baseUrl,
            @Qualifier(TRANSPORT) ObjectProvider<ClientHttpRequestFactory> transport) {
        return new PagesClient(baseUrl, transport.getIfAvailable(DataConfiguration::overTheNetwork));
    }

    @Bean
    PagesCache pagesCache(PagesClient client, Clock clock) {
        return new PagesCache(client, clock);
    }

    @Bean
    SecurityDataService securityDataService(PagesCache cache, PagesClient client, DataRefreshSignal refresh) {
        return new SecurityDataService(cache, client, refresh, DataConfiguration::afterTheCurrentRequest);
    }

    /**
     * Runs a task once the request that is being handled is over, or on
     * another thread when there is none.
     *
     * <p>
     * A reader who finds a moved index is in the middle of building a view.
     * The signal cannot be set there and then, because the view's effect
     * depends on it. And it cannot be set from another thread meanwhile: a
     * shared signal changed by another thread while a component is built and
     * not yet attached never reaches that component's bindings, which is the
     * workaround recorded in specs/FEEDBACK-25.4.md.
     */
    private static void afterTheCurrentRequest(Runnable task) {
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            session.access(task::run);
        } else {
            Thread.startVirtualThread(task);
        }
    }

    /** Warms the cache, so the first reader does not wait for the index. */
    @Bean
    ApplicationListener<ApplicationReadyEvent> warmTheCache() {
        return ready -> service.getObject().revalidate();
    }

    @Scheduled(initialDelayString = "${securitystatus.pages.revalidate-every}", fixedDelayString = "${securitystatus.pages.revalidate-every}")
    void revalidate() {
        service.getObject().revalidate();
    }

    private static ClientHttpRequestFactory overTheNetwork() {
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NORMAL).build());
        factory.setReadTimeout(Duration.ofSeconds(20));
        return factory;
    }
}

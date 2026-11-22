package com.vaadin.securitystatus.diagnostics;

import java.time.Duration;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.vaadin.flow.router.NavigationEndedEvent;
import com.vaadin.flow.router.NavigationStartedEvent;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.server.RequestEndedEvent;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceEventBus;
import com.vaadin.flow.server.VaadinServiceInitListener;

/**
 * Registers the diagnostics listeners once, when the service starts: the
 * navigations with their outcomes and the requests with their durations on the
 * service event bus, and the sessions through its lifecycle listeners. The
 * policy violations and the assistant tail are kept in {@code base} and read by
 * the view through their own signals, D-151.
 *
 * <p>
 * A listener runs on the request thread around the work it reports, so each
 * one only moves a counter.
 */
@Component
class DiagnosticsListeners implements VaadinServiceInitListener {

    /** The handler kind of a request no handler answered, a push message for one. */
    static final String NO_HANDLER = "";

    /** How long a requested path may be when no route matches it. */
    static final int PATH_LIMIT = 100;

    private final DiagnosticsCounters counters;
    private final ThreadLocal<Long> navigationStarted = new ThreadLocal<>();

    DiagnosticsListeners(DiagnosticsCounters counters) {
        this.counters = counters;
    }

    @Override
    public void serviceInit(ServiceInitEvent event) {
        VaadinService service = event.getSource();
        VaadinServiceEventBus bus = service.getEventBus();
        bus.addListener(NavigationStartedEvent.class, started -> navigationStarted.set(System.nanoTime()));
        bus.addListener(NavigationEndedEvent.class, this::navigationEnded);
        bus.addListener(RequestEndedEvent.class, this::requestEnded);
        service.addSessionInitListener(init -> counters.sessionStarted());
        service.addSessionDestroyListener(destroy -> counters.sessionEnded());
    }

    private void navigationEnded(NavigationEndedEvent ended) {
        Long started = navigationStarted.get();
        navigationStarted.remove();
        Duration duration = started == null ? Duration.ZERO : Duration.ofNanos(System.nanoTime() - started);
        counters.navigationEnded(route(ended), Outcome.of(ended.getOutcome()), duration);
    }

    /**
     * The route template of the view shown, else of the route the requested path
     * matches, else the bare requested path. Never the query string.
     */
    static String route(NavigationEndedEvent ended) {
        RouteConfiguration routes = RouteConfiguration
                .forRegistry(ended.getUI().getInternals().getRouter().getRegistry());
        String path = ended.getLocation().getPath();
        Optional<String> template = ended.getOutcome() instanceof NavigationEndedEvent.Completed completed
                ? routes.getTemplate(completed.navigationTarget())
                : routes.getRoute(path).flatMap(routes::getTemplate);
        String route = "/" + template.orElse(path);
        return route.length() > PATH_LIMIT ? route.substring(0, PATH_LIMIT) : route;
    }

    private void requestEnded(RequestEndedEvent ended) {
        String handler = ended.getHandler().<Object>map(found -> found)
                .or(() -> ended.getSessionlessHandler().map(found -> found))
                .map(DiagnosticsListeners::kind).orElse(NO_HANDLER);
        counters.requestEnded(handler, ended.getFailure().isPresent(), ended.getDuration());
    }

    private static String kind(Object handler) {
        String simple = handler.getClass().getSimpleName();
        return simple.isEmpty() ? handler.getClass().getName() : simple;
    }
}

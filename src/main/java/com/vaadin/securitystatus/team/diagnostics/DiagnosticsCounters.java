package com.vaadin.securitystatus.team.diagnostics;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.shared.SharedValueSignal;

/**
 * The counters the diagnostics view shows, one shared signal each, so every
 * open view moves when any session moves them. Kept in memory only: a restart
 * starts again from zero, and {@link #since()} says when that was.
 *
 * <p>
 * What is kept is decided here and nowhere else, D-152 and D-153: a navigation
 * by its route template and outcome, a request by the minute it ended in and
 * the kind of handler that answered it. No address beyond the route, no query,
 * no session id, no user, no header and no body is ever recorded.
 */
@Component
public class DiagnosticsCounters {

    /** How many distinct routes are counted before the rest go to {@link #OTHER_ROUTE}. */
    static final int ROUTES_KEPT = 100;

    /** The key of the navigations past {@link #ROUTES_KEPT} distinct routes. */
    public static final String OTHER_ROUTE = "*";

    /** How many minutes of request timings are kept. */
    static final int MINUTES_KEPT = 60;

    /** One route's navigations, counted per outcome, with their durations. */
    public record RouteCount(String route, long completed, long refused, long broke, long notShown, long postponed,
            long totalMillis, long longestMillis) {

        public long total() {
            return completed + refused + broke + notShown + postponed;
        }

        public long count(Outcome outcome) {
            return switch (outcome) {
            case COMPLETED -> completed;
            case REFUSED -> refused;
            case BROKE -> broke;
            case NOT_SHOWN -> notShown;
            case POSTPONED -> postponed;
            };
        }

        RouteCount plus(Outcome outcome, long millis) {
            return new RouteCount(route, completed + (outcome == Outcome.COMPLETED ? 1 : 0),
                    refused + (outcome == Outcome.REFUSED ? 1 : 0), broke + (outcome == Outcome.BROKE ? 1 : 0),
                    notShown + (outcome == Outcome.NOT_SHOWN ? 1 : 0),
                    postponed + (outcome == Outcome.POSTPONED ? 1 : 0), totalMillis + millis,
                    Math.max(longestMillis, millis));
        }
    }

    /** Every route's navigations, ordered by route. */
    public record Navigations(List<RouteCount> routes) {

        public long total(Outcome outcome) {
            return routes.stream().mapToLong(route -> route.count(outcome)).sum();
        }

        public long total() {
            return routes.stream().mapToLong(RouteCount::total).sum();
        }

        /** The time of every navigation, for a mean weighted by how often each route was taken. */
        public long totalMillis() {
            return routes.stream().mapToLong(RouteCount::totalMillis).sum();
        }

        public long longestMillis() {
            return routes.stream().mapToLong(RouteCount::longestMillis).max().orElse(0);
        }
    }

    /** The requests that ended in one minute. */
    public record Minute(Instant start, long count, long failed, long totalMicros, long longestMicros) {

        Minute plus(boolean failure, long micros) {
            return new Minute(start, count + 1, failed + (failure ? 1 : 0), totalMicros + micros,
                    Math.max(longestMicros, micros));
        }
    }

    /** How many requests one kind of handler answered. */
    public record HandlerCount(String handler, long count) {
    }

    /** The request timing summary: totals since the start, the last minutes, and the handler kinds. */
    public record Requests(long count, long failed, long totalMicros, long longestMicros, List<Minute> minutes,
            List<HandlerCount> kinds) {
    }

    private final Clock clock;
    private final Instant since;
    private final SharedValueSignal<Navigations> navigations = new SharedValueSignal<>(Navigations.class);
    private final SharedValueSignal<Requests> requests = new SharedValueSignal<>(Requests.class);
    private final SharedValueSignal<Long> sessions = new SharedValueSignal<>(Long.class);

    public DiagnosticsCounters(Clock clock) {
        this.clock = clock;
        this.since = clock.instant();
        navigations.set(new Navigations(List.of()));
        requests.set(new Requests(0, 0, 0, 0, List.of(), List.of()));
        sessions.set(0L);
    }

    /** When counting started, which is when the application started. */
    public Instant since() {
        return since;
    }

    public Signal<Navigations> navigations() {
        return navigations.asReadonly();
    }

    public Signal<Requests> requests() {
        return requests.asReadonly();
    }

    public Signal<Long> sessions() {
        return sessions.asReadonly();
    }

    void navigationEnded(String route, Outcome outcome, Duration duration) {
        long millis = duration.toMillis();
        navigations.update(current -> {
            List<RouteCount> routes = new ArrayList<>(current.routes());
            String key = route;
            if (routes.stream().noneMatch(kept -> kept.route().equals(route)) && routes.size() >= ROUTES_KEPT) {
                key = OTHER_ROUTE;
            }
            String counted = key;
            int at = indexOf(routes, counted);
            if (at < 0) {
                routes.add(new RouteCount(counted, 0, 0, 0, 0, 0, 0, 0).plus(outcome, millis));
            } else {
                routes.set(at, routes.get(at).plus(outcome, millis));
            }
            routes.sort(Comparator.comparing(RouteCount::route));
            return new Navigations(List.copyOf(routes));
        });
    }

    public void requestEnded(String handler, boolean failure, Duration duration) {
        long micros = duration.toNanos() / 1000;
        Instant minute = clock.instant().truncatedTo(ChronoUnit.MINUTES);
        requests.update(current -> {
            List<Minute> minutes = new ArrayList<>(current.minutes());
            if (minutes.isEmpty() || !minutes.getLast().start().equals(minute)) {
                minutes.add(new Minute(minute, 0, 0, 0, 0));
            }
            minutes.set(minutes.size() - 1, minutes.getLast().plus(failure, micros));
            while (minutes.size() > MINUTES_KEPT) {
                minutes.removeFirst();
            }
            List<HandlerCount> kinds = new ArrayList<>(current.kinds());
            int at = -1;
            for (int i = 0; i < kinds.size(); i++) {
                if (kinds.get(i).handler().equals(handler)) {
                    at = i;
                }
            }
            if (at < 0) {
                kinds.add(new HandlerCount(handler, 1));
            } else {
                kinds.set(at, new HandlerCount(handler, kinds.get(at).count() + 1));
            }
            kinds.sort(Comparator.comparingLong(HandlerCount::count).reversed().thenComparing(HandlerCount::handler));
            return new Requests(current.count() + 1, current.failed() + (failure ? 1 : 0),
                    current.totalMicros() + micros, Math.max(current.longestMicros(), micros),
                    List.copyOf(minutes), List.copyOf(kinds));
        });
    }

    void sessionStarted() {
        sessions.update(count -> count + 1);
    }

    void sessionEnded() {
        sessions.update(count -> Math.max(0, count - 1));
    }

    private static int indexOf(List<RouteCount> routes, String route) {
        for (int i = 0; i < routes.size(); i++) {
            if (routes.get(i).route().equals(route)) {
                return i;
            }
        }
        return -1;
    }
}

package com.vaadin.securitystatus.data;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

/**
 * The cache in front of {@link PagesClient}: in memory, keyed by path, time
 * based only.
 *
 * <ul>
 * <li>An entry inside its time to live answers without a fetch.</li>
 * <li>An older one is revalidated with its {@code ETag}, and a 304 renews it
 * without parsing anything.</li>
 * <li>Concurrent requests for one path share one fetch.</li>
 * <li>When the fetch fails, the entry that exists keeps answering. Only a path
 * with no entry at all fails, with {@link DataUnreachableException}.</li>
 * <li>A 404 is an answer, "no such document", and is never stored.</li>
 * </ul>
 */
final class PagesCache {

    private static final Logger LOG = LoggerFactory.getLogger(PagesCache.class);

    /** How long a stale entry answers before the dead host is tried again. */
    static final Duration RETRY_AFTER_FAILURE = Duration.ofMinutes(1);

    private record Entry(Object value, @Nullable String etag, Instant freshUntil) {
    }

    private final PagesClient client;
    private final Clock clock;
    private final JsonMapper mapper = JsonMapper.builder().propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES).build();
    private final ConcurrentHashMap<String, Entry> entries = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CompletableFuture<Optional<Entry>>> inFlight = new ConcurrentHashMap<>();
    private final AtomicBoolean failing = new AtomicBoolean();

    PagesCache(PagesClient client, Clock clock) {
        this.client = client;
        this.clock = clock;
    }

    /** The document at a path, from the cache while it is fresh. Empty when the host has none. */
    <T> Optional<T> read(String path, Class<T> type, Duration timeToLive) {
        Entry cached = entries.get(path);
        if (cached != null && clock.instant().isBefore(cached.freshUntil())) {
            return Optional.of(type.cast(cached.value()));
        }
        return load(path, type, timeToLive, false);
    }

    /** The document at a path, asked of the host whatever the age of the cached one. */
    <T> Optional<T> revalidate(String path, Class<T> type, Duration timeToLive) {
        return load(path, type, timeToLive, true);
    }

    /**
     * Makes every entry but one due for revalidation. Nothing is dropped, so
     * each keeps answering if the host cannot be reached.
     */
    void expireAllExcept(String path) {
        entries.replaceAll((key, entry) -> key.equals(path) ? entry
                : new Entry(entry.value(), entry.etag(), Instant.MIN));
    }

    void clear() {
        entries.clear();
        failing.set(false);
    }

    private <T> Optional<T> load(String path, Class<T> type, Duration timeToLive, boolean force) {
        var mine = new CompletableFuture<Optional<Entry>>();
        var running = inFlight.putIfAbsent(path, mine);
        if (running != null) {
            return spectate(running).map(entry -> type.cast(entry.value()));
        }
        try {
            Entry cached = entries.get(path);
            Optional<Entry> loaded = !force && cached != null && clock.instant().isBefore(cached.freshUntil())
                    ? Optional.of(cached)
                    : fetch(path, type, timeToLive, cached);
            mine.complete(loaded);
            return loaded.map(entry -> type.cast(entry.value()));
        } catch (RuntimeException failure) {
            mine.completeExceptionally(failure);
            throw failure;
        } finally {
            inFlight.remove(path, mine);
        }
    }

    private static Optional<Entry> spectate(CompletableFuture<Optional<Entry>> running) {
        try {
            return running.join();
        } catch (CompletionException failure) {
            if (failure.getCause() instanceof RuntimeException cause) {
                throw cause;
            }
            throw failure;
        }
    }

    private Optional<Entry> fetch(String path, Class<?> type, Duration timeToLive, @Nullable Entry cached) {
        try {
            PagesClient.Answer answer = client.fetch(path, cached == null ? null : cached.etag());
            Optional<Entry> loaded = switch (answer) {
            case PagesClient.Answer.Body body -> Optional
                    .of(new Entry(mapper.readValue(body.bytes(), type), body.etag(), freshFor(timeToLive)));
            case PagesClient.Answer.NotModified unchanged -> {
                if (cached == null) {
                    throw new PagesClient.UnreachableException(path + ": 304 for a request with no ETag", null);
                }
                yield Optional.of(new Entry(cached.value(), cached.etag(), freshFor(timeToLive)));
            }
            case PagesClient.Answer.NotFound missing -> Optional.empty();
            };
            loaded.ifPresentOrElse(entry -> entries.put(path, entry), () -> entries.remove(path));
            if (failing.compareAndSet(true, false)) {
                LOG.info("The data source answers again");
            }
            return loaded;
        } catch (PagesClient.UnreachableException | JacksonException failure) {
            if (failing.compareAndSet(false, true)) {
                LOG.warn("The data source cannot be read, cached answers are served until it can: {}",
                        failure.getMessage());
            }
            if (cached == null) {
                throw new DataUnreachableException(path, failure);
            }
            Entry stale = new Entry(cached.value(), cached.etag(), clock.instant().plus(RETRY_AFTER_FAILURE));
            entries.put(path, stale);
            return Optional.of(stale);
        }
    }

    private Instant freshFor(Duration timeToLive) {
        return clock.instant().plus(timeToLive);
    }
}

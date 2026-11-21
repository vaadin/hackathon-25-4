package com.vaadin.securitystatus.data;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.securitystatus.data.model.CaseCatalog;
import com.vaadin.securitystatus.data.model.CasesIndex;
import com.vaadin.securitystatus.data.model.CveAnswer;
import com.vaadin.securitystatus.data.model.ExplorerFront;
import com.vaadin.securitystatus.data.model.ExplorerVersion;
import com.vaadin.securitystatus.data.model.ScanIndex;
import com.vaadin.securitystatus.data.model.VerdictQueue;
import com.vaadin.securitystatus.data.model.VersionAnswer;

/**
 * The only door views and tools have to the published measurements. Every
 * answer comes from the cache.
 *
 * <p>
 * A document every release of the site has, the index for one, is returned as
 * itself. A document per version or per advisory is an {@code Optional}, empty
 * when nobody measured that version or advisory. Any of them throws
 * {@link DataUnreachableException} when there is no cached answer and the site
 * cannot be read.
 */
public class SecurityDataService {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityDataService.class);

    static final Duration INDEX_FRESH_FOR = Duration.ofMinutes(15);
    static final Duration DOCUMENT_FRESH_FOR = Duration.ofHours(6);

    static final String INDEX = "cases/index.json";

    /** What a version or an advisory id may be made of, so neither can walk out of its directory. */
    private static final Pattern PATH_SEGMENT = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,99}");

    private final PagesCache cache;
    private final PagesClient client;
    private final DataRefreshSignal refresh;
    private final Executor publisher;
    private final AtomicReference<Instant> newest = new AtomicReference<>();

    SecurityDataService(PagesCache cache, PagesClient client, DataRefreshSignal refresh, Executor publisher) {
        this.cache = cache;
        this.client = client;
        this.refresh = refresh;
        this.publisher = publisher;
    }

    public CasesIndex index() {
        CasesIndex index = required(INDEX, CasesIndex.class, INDEX_FRESH_FOR);
        // Told from another thread: a view reads this inside the effect that
        // depends on the refresh signal, and an effect must not change a
        // signal it depends on.
        if (noteGeneratedAt(index)) {
            publisher.execute(() -> refresh.publish(index.generatedAt()));
        }
        return index;
    }

    public Optional<VersionAnswer> version(VersionNumber version) {
        return perId("cases/v/", version.toString(), VersionAnswer.class);
    }

    public Optional<CveAnswer> cve(String id) {
        return perId("cases/cve/", id, CveAnswer.class);
    }

    public ScanIndex scanIndex() {
        return required("cases/scan-index.json", ScanIndex.class, DOCUMENT_FRESH_FOR);
    }

    public CaseCatalog cases() {
        return required("cases/cases.json", CaseCatalog.class, DOCUMENT_FRESH_FOR);
    }

    public ExplorerFront explorerFront() {
        return required("data/front.json", ExplorerFront.class, DOCUMENT_FRESH_FOR);
    }

    public Optional<ExplorerVersion> explorerVersion(VersionNumber version) {
        return perId("data/", version.toString(), ExplorerVersion.class);
    }

    public VerdictQueue queue() {
        return required("data/queue.json", VerdictQueue.class, DOCUMENT_FRESH_FOR);
    }

    /**
     * One per release file, streamed from the publisher and never cached or
     * parsed. Empty when the publisher has no such file.
     */
    public Optional<ExportFile> export(VersionNumber version, ExportKind kind) {
        if (!PATH_SEGMENT.matcher(version.toString()).matches()) {
            return Optional.empty();
        }
        String fileName = kind.fileName(version);
        try {
            return client.open("exports/" + fileName).map(stream -> new ExportFile(stream.body(), fileName,
                    stream.contentType() == null ? "application/octet-stream" : stream.contentType(),
                    stream.length()));
        } catch (PagesClient.UnreachableException failure) {
            throw new DataUnreachableException("exports/" + fileName, failure);
        }
    }

    /**
     * Asks the publisher whether the index moved, whatever the age of the
     * cached one. When {@code generated_at} is another one, every other cached
     * document becomes due for revalidation and every session is told.
     *
     * <p>
     * Runs when the application starts and then on a schedule. It never
     * throws: with the publisher unreachable the cache keeps answering.
     */
    public void revalidate() {
        try {
            CasesIndex index = cache.revalidate(INDEX, CasesIndex.class, INDEX_FRESH_FOR)
                    .orElseThrow(() -> new DataUnreachableException(INDEX, null));
            if (noteGeneratedAt(index)) {
                refresh.publish(index.generatedAt());
            }
        } catch (DataUnreachableException failure) {
            LOG.debug("Nothing to revalidate against", failure);
        }
    }

    /** True when this index carries another {@code generated_at} than the last one seen. */
    private boolean noteGeneratedAt(CasesIndex index) {
        // Trusted as it comes, backwards too: the pipeline owns the clock.
        Instant before = newest.getAndSet(index.generatedAt());
        if (index.generatedAt().equals(before)) {
            return false;
        }
        if (before != null) {
            cache.expireAllExcept(INDEX);
        }
        return true;
    }

    /** Forgets what was read, for a test that needs a cold start. */
    void forget() {
        cache.clear();
        newest.set(null);
    }

    private <T> T required(String path, Class<T> type, Duration timeToLive) {
        return cache.read(path, type, timeToLive).orElseThrow(() -> new DataUnreachableException(path, null));
    }

    private <T> Optional<T> perId(String directory, String id, Class<T> type) {
        if (!PATH_SEGMENT.matcher(id).matches()) {
            return Optional.empty();
        }
        return cache.read(directory + id + ".json", type, DOCUMENT_FRESH_FOR);
    }
}

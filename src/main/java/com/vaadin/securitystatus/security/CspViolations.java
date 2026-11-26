package com.vaadin.securitystatus.security;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.shared.SharedValueSignal;

/**
 * What browsers reported against the policy: logged as it arrives and kept, the
 * newest first and bounded, for the diagnostics view to read.
 */
@Component
public class CspViolations {

    public record Violation(Instant receivedAt, String report) {
    }

    static final int KEPT = 100;
    static final int MAX_REPORT_LENGTH = 4000;
    /** How much of a report the log line carries; the whole of it is kept for the view. */
    static final int LOGGED_LENGTH = 200;

    private static final Logger LOG = LoggerFactory.getLogger(CspViolations.class);

    private final Deque<Violation> newestFirst = new ArrayDeque<>();
    private final SharedValueSignal<Long> received = new SharedValueSignal<>(Long.class);
    private long count;

    public CspViolations() {
        received.set(0L);
    }

    public synchronized void record(String report) {
        String bounded = report.length() > MAX_REPORT_LENGTH ? report.substring(0, MAX_REPORT_LENGTH) : report;
        String oneLine = bounded.replaceAll("\\s+", " ");
        // The log says one arrived and enough of it to recognise, not the whole body: anybody's
        // browser can post here, the text is theirs rather than ours, and four kilobytes of it
        // per report is a way to drown the log in somebody else's words. What the log leaves out
        // is not lost, the diagnostics view reads the whole bounded report from the tail below.
        LOG.warn("Content Security Policy violation reported, {} characters: {}", oneLine.length(),
                oneLine.length() > LOGGED_LENGTH ? oneLine.substring(0, LOGGED_LENGTH) + "..." : oneLine);
        newestFirst.addFirst(new Violation(Instant.now(), oneLine));
        while (newestFirst.size() > KEPT) {
            newestFirst.removeLast();
        }
        received.set(++count);
    }

    /**
     * How many reports arrived since the start, as a signal every session may
     * read, so a view showing them redraws when one arrives.
     */
    public Signal<Long> received() {
        return received.asReadonly();
    }

    public synchronized List<Violation> newestFirst() {
        return List.copyOf(newestFirst);
    }
}

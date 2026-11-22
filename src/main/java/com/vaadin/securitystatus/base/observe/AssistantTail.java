package com.vaadin.securitystatus.base.observe;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import org.springframework.stereotype.Component;

import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.shared.SharedValueSignal;

/**
 * The last exchanges of every assistant surface, for the diagnostics view: the
 * prompts as they went out, each tool call with its arguments and outcome, and
 * how each turn ended. Kept in memory, the newest {@value #KEEP} entries.
 *
 * <p>
 * Attachments are recorded by name and size only, never by content.
 */
@Component
public class AssistantTail {

    /** How many entries are kept. */
    public static final int KEEP = 200;

    /** How much of one text is kept. */
    static final int TEXT_LIMIT = 2000;

    /** What an entry records. */
    public enum Kind {
        /** A prompt, as the interceptor saw it go out. */
        PROMPT,
        /** A tool call with its arguments, answered. */
        TOOL,
        /** A tool call the tool refused, with the sentence the model was given. */
        REFUSAL,
        /** A turn that ended with an answer. */
        ANSWER,
        /** A turn that ended with an error. */
        FAILURE
    }

    /** One line of the tail, numbered in the order recorded. {@code surface} is chat, grid or form. */
    public record Entry(long number, Instant at, String surface, String user, Kind kind, String text) {
    }

    private final Clock clock;
    private final Deque<Entry> entries = new ArrayDeque<>();
    private final SharedValueSignal<Long> newestSignal = new SharedValueSignal<>(Long.class);
    private long recorded;

    public AssistantTail(Clock clock) {
        this.clock = clock;
        newestSignal.set(0L);
    }

    public void record(String surface, String user, Kind kind, String text) {
        String kept = text.length() > TEXT_LIMIT ? text.substring(0, TEXT_LIMIT) + "…" : text;
        long number;
        synchronized (entries) {
            number = ++recorded;
            entries.addLast(new Entry(number, clock.instant(), surface, user, kind, kept));
            while (entries.size() > KEEP) {
                entries.removeFirst();
            }
        }
        newestSignal.update(current -> Math.max(current, number));
    }

    /**
     * The number of the newest entry as a signal every session may read, so a
     * view showing the tail redraws when an entry arrives.
     */
    public Signal<Long> newestSignal() {
        return newestSignal.asReadonly();
    }

    /** The number of the newest entry, 0 before the first. */
    public long newest() {
        synchronized (entries) {
            return recorded;
        }
    }

    /** Every kept entry, oldest first. */
    public List<Entry> entries() {
        synchronized (entries) {
            return List.copyOf(entries);
        }
    }
}

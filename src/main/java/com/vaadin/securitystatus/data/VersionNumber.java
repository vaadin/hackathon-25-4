package com.vaadin.securitystatus.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * A version as the measurements name it: parsed once, compared numerically,
 * printed as received.
 *
 * <p>
 * The order is the numeric one, segment by segment, so {@code 24.10} is above
 * {@code 24.2}. A missing segment counts as zero. A prerelease is below the
 * release it leads to, and prereleases order by their word and then their
 * number: {@code alpha3}, {@code beta1}, {@code beta2}, {@code rc1}.
 *
 * <p>
 * Equality is on the text as received, so {@code 24.9} and {@code 24.9.0}
 * compare as zero and are not equal. Anything parses: a text with no leading
 * number has no segments and orders below every version that has one.
 */
public final class VersionNumber implements Comparable<VersionNumber> {

    private static final Pattern SHAPE = Pattern.compile("^(\\d+(?:\\.\\d+)*)(?:[.\\-_]?(.*))?$");
    private static final Pattern QUALIFIER = Pattern.compile("^([^\\d.\\-_]*)[.\\-_]?(\\d*)(.*)$");

    private final String text;
    private final List<Long> segments;
    private final String qualifier;

    private VersionNumber(String text) {
        this.text = text;
        Matcher shape = SHAPE.matcher(text);
        var numbers = new ArrayList<Long>();
        if (shape.matches()) {
            for (String segment : shape.group(1).split("\\.")) {
                numbers.add(segment.length() > 18 ? Long.MAX_VALUE : Long.parseLong(segment));
            }
            qualifier = shape.group(2) == null ? "" : shape.group(2).toLowerCase(Locale.ROOT);
        } else {
            qualifier = text.toLowerCase(Locale.ROOT);
        }
        segments = List.copyOf(numbers);
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static VersionNumber of(String text) {
        return new VersionNumber(text.strip());
    }

    /** The minor this version belongs to, {@code 24.9} for {@code 24.9.21}. */
    public String line() {
        return segments.size() < 2 ? text : segments.get(0) + "." + segments.get(1);
    }

    public boolean isPrerelease() {
        return !qualifier.isEmpty();
    }

    @Override
    public int compareTo(VersionNumber other) {
        int length = Math.max(segments.size(), other.segments.size());
        if (segments.isEmpty() != other.segments.isEmpty()) {
            return segments.isEmpty() ? -1 : 1;
        }
        for (int i = 0; i < length; i++) {
            int bySegment = Long.compare(segment(i), other.segment(i));
            if (bySegment != 0) {
                return bySegment;
            }
        }
        if (qualifier.isEmpty() || other.qualifier.isEmpty()) {
            return Boolean.compare(qualifier.isEmpty(), other.qualifier.isEmpty());
        }
        Matcher mine = QUALIFIER.matcher(qualifier);
        Matcher theirs = QUALIFIER.matcher(other.qualifier);
        if (!mine.matches() || !theirs.matches()) {
            return qualifier.compareTo(other.qualifier);
        }
        int byWord = mine.group(1).compareTo(theirs.group(1));
        if (byWord != 0) {
            return byWord;
        }
        int byNumber = Long.compare(number(mine.group(2)), number(theirs.group(2)));
        return byNumber != 0 ? byNumber : mine.group(3).compareTo(theirs.group(3));
    }

    private long segment(int index) {
        return index < segments.size() ? segments.get(index) : 0;
    }

    private static long number(String digits) {
        return digits.isEmpty() ? 0 : digits.length() > 18 ? Long.MAX_VALUE : Long.parseLong(digits);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof VersionNumber version && text.equals(version.text);
    }

    @Override
    public int hashCode() {
        return text.hashCode();
    }

    @JsonValue
    @Override
    public String toString() {
        return text;
    }
}

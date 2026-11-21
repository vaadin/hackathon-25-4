package com.vaadin.securitystatus.data.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

/**
 * What every record does with what arrived: absent stays absent, and an empty
 * text is absent too, because the pipeline writes {@code ""} where it has
 * nothing to say.
 */
final class Wire {

    private Wire() {
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    static Optional<String> text(@Nullable Optional<String> received) {
        return received == null ? Optional.empty() : received.filter(text -> !text.isBlank());
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    static <T> Optional<T> value(@Nullable Optional<T> received) {
        return received == null ? Optional.empty() : received;
    }

    static <T> List<T> list(@Nullable List<T> received) {
        return received == null ? List.of() : Collections.unmodifiableList(received);
    }

    /** Keeps the order the document had, which is often the order to show. */
    static <K, V> Map<K, V> map(@Nullable Map<K, V> received) {
        return received == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(received));
    }
}

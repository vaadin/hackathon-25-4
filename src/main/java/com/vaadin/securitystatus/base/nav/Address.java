package com.vaadin.securitystatus.base.nav;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.QueryParameters;

/**
 * One address: a route path and its query flags. Immutable; every change
 * answers a new address. Every screen writes its flags through this class, so
 * a control that changes one flag keeps all the others, rule 6.11.13 of the
 * source page, and the flags always come out in the order {@link QueryFlag}
 * declares, whatever order they were set in.
 *
 * <p>
 * The order holds for {@link #href()}, which is what a link carries. A
 * navigation goes through {@link QueryParameters}, which writes the browser's
 * address in hash order, draft 14, so {@link #go(UI)} cannot keep it.
 */
public final class Address {

    private static final Comparator<String> ORDER = Comparator.comparingInt(Address::rank);

    private final String path;
    private final Map<String, String> flags;

    private Address(String path, Map<String, String> flags) {
        this.path = path.startsWith("/") ? path.substring(1) : path;
        this.flags = flags;
    }

    /** The address of a route path, such as {@code version/24.9.21}, with no flags. */
    public static Address of(String path) {
        return new Address(path, Map.of());
    }

    /** The route path, without a leading slash and without the query. */
    public String path() {
        return path;
    }

    /** This address with the flag set to the value, or without it when the value is empty. */
    public Address with(QueryFlag flag, Optional<String> value) {
        return with(flag.key(), value);
    }

    /** This address with the flag set to the value. */
    public Address with(QueryFlag flag, String value) {
        return with(flag.key(), Optional.of(value));
    }

    /**
     * This address with a flag this class does not declare, such as one read
     * from a link written elsewhere. It comes after the declared ones, in the
     * order it was added.
     */
    public Address with(String key, Optional<String> value) {
        var next = new LinkedHashMap<>(flags);
        next.remove(key);
        value.ifPresent(present -> next.put(key, present));
        return new Address(path, next);
    }

    /** This address with the flag written as {@code 1}, or without it. */
    public Address flag(QueryFlag flag, boolean on) {
        return with(flag, on ? Optional.of("1") : Optional.empty());
    }

    /** This address with the flag written bare, {@code ?secure}, or without it. */
    public Address bare(QueryFlag flag, boolean on) {
        return with(flag, on ? Optional.of("") : Optional.empty());
    }

    /** This address with the values as one comma separated flag, or without it when there are none. */
    public Address list(QueryFlag flag, Collection<String> values) {
        return with(flag, values.isEmpty() ? Optional.empty() : Optional.of(String.join(",", values)));
    }

    /** The value of a flag on this address. */
    public Optional<String> get(QueryFlag flag) {
        return Optional.ofNullable(flags.get(flag.key()));
    }

    /** The query part in the declared order, without the question mark, empty when nothing is set. */
    public String queryString() {
        var keys = new ArrayList<>(flags.keySet());
        keys.sort(ORDER);
        return keys.stream().map(key -> {
            String value = flags.get(key);
            return value.isEmpty() ? encode(key) : encode(key) + "=" + encode(value);
        }).collect(Collectors.joining("&"));
    }

    /** The address as a link target, relative to the application root, flags in the declared order. */
    public String href() {
        String query = queryString();
        return query.isEmpty() ? path : path + "?" + query;
    }

    /** The flags for a router link or a navigation. Their order is lost there, draft 14. */
    public QueryParameters query() {
        var parameters = new LinkedHashMap<String, List<String>>();
        flags.forEach((key, value) -> parameters.put(key, List.of(value)));
        return new QueryParameters(parameters);
    }

    /** Goes to this address in the given UI. A view navigated to again is reused, not rebuilt. */
    public void go(UI ui) {
        ui.navigate(path, query());
    }

    @Override
    public String toString() {
        return href();
    }

    // Reading

    /** Whether the query carries the flag, with any value or none. */
    public static boolean has(QueryParameters query, QueryFlag flag) {
        return query.getParameters().containsKey(flag.key());
    }

    /** Whether the query carries the flag with a value other than {@code 0}. */
    public static boolean on(QueryParameters query, QueryFlag flag) {
        return first(query, flag).filter(value -> !"0".equals(value)).isPresent();
    }

    /** The first value of the flag, as written. */
    public static Optional<String> first(QueryParameters query, QueryFlag flag) {
        List<String> values = query.getParameters().getOrDefault(flag.key(), List.of());
        return values.isEmpty() ? Optional.empty() : Optional.ofNullable(values.get(0));
    }

    /**
     * The values of a comma separated flag, trimmed, without blanks and
     * repeats, in the order written. Every occurrence of the flag counts.
     */
    public static List<String> list(QueryParameters query, QueryFlag flag) {
        var values = new LinkedHashSet<String>();
        for (String written : query.getParameters().getOrDefault(flag.key(), List.of())) {
            for (String value : written.split(",")) {
                if (!value.isBlank()) {
                    values.add(value.trim());
                }
            }
        }
        return List.copyOf(values);
    }

    /** Reads every flag of a query into an address of the given path, declared or not. */
    public static Address of(String path, QueryParameters query) {
        var flags = new LinkedHashMap<String, String>();
        query.getParameters().forEach((key, values) -> flags.put(key, values.isEmpty() ? "" : values.get(0)));
        return new Address(path, flags);
    }

    /**
     * Reads a query string written elsewhere, {@code boot=1&ruled=1}, into an
     * address of the given path, keeping every flag, declared or not.
     */
    public static Address parse(String path, String queryString) {
        var flags = new LinkedHashMap<String, String>();
        for (String pair : queryString.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int equals = pair.indexOf('=');
            String key = decode(equals < 0 ? pair : pair.substring(0, equals));
            String value = equals < 0 ? "" : decode(pair.substring(equals + 1));
            flags.putIfAbsent(key, value);
        }
        return new Address(path, flags);
    }

    private static String decode(String text) {
        return URLDecoder.decode(text, StandardCharsets.UTF_8);
    }

    private static int rank(String key) {
        for (QueryFlag flag : QueryFlag.values()) {
            if (flag.key().equals(key)) {
                return flag.ordinal();
            }
        }
        return QueryFlag.values().length;
    }

    private static String encode(String text) {
        // A comma separates the values of one flag and stays readable in the address.
        return URLEncoder.encode(text, StandardCharsets.UTF_8).replace("%2C", ",").replace("+", "%20");
    }
}

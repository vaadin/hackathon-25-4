package com.vaadin.securitystatus.status;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.RouteParameters;

/**
 * The whole answer of the version screen as an address: the version and every
 * section the reader opened. Every control that opens or closes a section
 * writes its flag through {@link #go(UI)}, the one place that writes this
 * address, so a link a support engineer copies opens what they see, rule
 * 6.11.13 of the source page.
 *
 * @param version the version asked, as typed
 * @param boot which shape the Spring Boot section takes: the table when true,
 *            the note when false, the state's own choice when absent
 * @param ruled the findings we ruled out are listed
 * @param disc the findings only a discontinued product brings are listed
 * @param open the advisory whose details the reader opened, or the link names
 * @param prereleases the versions table lists the prerelease lines too
 */
public record VersionAddress(String version, @Nullable Boolean boot, boolean ruled, boolean disc,
        @Nullable String open, boolean prereleases) {

    static final String BOOT = "boot";
    static final String RULED = "ruled";
    static final String DISC = "disc";
    static final String OPEN = "open";

    /** The address for a version with nothing opened. */
    public static VersionAddress of(String version) {
        return new VersionAddress(version, null, false, false, null, false);
    }

    /** Reads the flags of an address the reader arrived at, written by hand or by us. */
    static VersionAddress of(String version, Location location) {
        Map<String, List<String>> query = location.getQueryParameters().getParameters();
        Boolean boot = first(query, BOOT).map(value -> !"0".equals(value)).orElse(null);
        return new VersionAddress(version, boot, flag(query, RULED), flag(query, DISC),
                first(query, OPEN).filter(value -> !value.isBlank()).orElse(null),
                query.containsKey(HomeView.PRERELEASES));
    }

    VersionAddress withBoot(boolean shown) {
        return new VersionAddress(version, shown, ruled, disc, open, prereleases);
    }

    VersionAddress withRuled(boolean shown) {
        return new VersionAddress(version, boot, shown, disc, open, prereleases);
    }

    VersionAddress withDisc(boolean shown) {
        return new VersionAddress(version, boot, ruled, shown, open, prereleases);
    }

    VersionAddress withOpen(@Nullable String advisory) {
        return new VersionAddress(version, boot, ruled, disc, advisory, prereleases);
    }

    /** The query part, in one fixed order, naming only what is not the default. */
    QueryParameters query() {
        var parameters = new LinkedHashMap<String, List<String>>();
        if (boot != null) {
            parameters.put(BOOT, List.of(boot ? "1" : "0"));
        }
        if (ruled) {
            parameters.put(RULED, List.of("1"));
        }
        if (disc) {
            parameters.put(DISC, List.of("1"));
        }
        if (open != null) {
            parameters.put(OPEN, List.of(open));
        }
        if (prereleases) {
            parameters.put(HomeView.PRERELEASES, List.of("1"));
        }
        return new QueryParameters(parameters);
    }

    /** The address as a link target, so every control that writes it is also a real link. */
    String href() {
        String query = query().getQueryString();
        return "version/" + version + (query.isEmpty() ? "" : "?" + query);
    }

    /** Goes to this address. The view decides from what changed whether the reader moves. */
    void go(UI ui) {
        ui.navigate(VersionView.class, new RouteParameters(VersionView.VERSION, version), query());
    }

    private static boolean flag(Map<String, List<String>> query, String name) {
        return first(query, name).filter(value -> !"0".equals(value)).isPresent();
    }

    private static Optional<String> first(Map<String, List<String>> query, String name) {
        List<String> values = query.getOrDefault(name, new ArrayList<>());
        return values.isEmpty() ? Optional.empty() : Optional.ofNullable(values.get(0));
    }
}

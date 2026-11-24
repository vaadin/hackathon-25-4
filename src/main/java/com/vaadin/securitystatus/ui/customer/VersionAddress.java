package com.vaadin.securitystatus.ui.customer;

import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.securitystatus.ui.common.nav.Address;
import com.vaadin.securitystatus.ui.common.nav.QueryFlag;

/**
 * The whole answer of the version screen as an address: the version and every
 * section the reader opened. Every control that opens or closes a section
 * writes its flag through {@link #go(UI)}, and the flags are written by
 * {@link Address}, the one place that writes addresses, so a link a support engineer copies opens what they see, rule
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

    /** The address for a version with nothing opened. */
    public static VersionAddress of(String version) {
        return new VersionAddress(version, null, false, false, null, false);
    }

    /** Reads the flags of an address the reader arrived at, written by hand or by us. */
    static VersionAddress of(String version, Location location) {
        QueryParameters query = location.getQueryParameters();
        Boolean boot = Address.first(query, QueryFlag.BOOT).map(value -> !"0".equals(value)).orElse(null);
        return new VersionAddress(version, boot, Address.on(query, QueryFlag.RULED),
                Address.on(query, QueryFlag.DISC),
                Address.first(query, QueryFlag.OPEN).filter(value -> !value.isBlank()).orElse(null),
                Address.has(query, QueryFlag.PRE));
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

    /** The whole address, naming only what is not the default. The order is {@link QueryFlag}'s. */
    Address address() {
        return Address.of("version/" + version)
                .with(QueryFlag.BOOT, Optional.ofNullable(boot).map(shown -> shown ? "1" : "0"))
                .flag(QueryFlag.RULED, ruled).flag(QueryFlag.DISC, disc)
                .with(QueryFlag.OPEN, Optional.ofNullable(open)).flag(QueryFlag.PRE, prereleases);
    }

    /** The query part, for a navigation. */
    QueryParameters query() {
        return address().query();
    }

    /** The address as a link target, so every control that writes it is also a real link. */
    String href() {
        return address().href();
    }

    /** Goes to this address. The view decides from what changed whether the reader moves. */
    void go(UI ui) {
        ui.navigate(VersionView.class, new RouteParameters(VersionView.VERSION, version), query());
    }
}

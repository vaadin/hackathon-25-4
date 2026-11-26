package com.vaadin.securitystatus.security;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

/**
 * Who is the security team: a signed in user whose verified email is on the
 * company's domain, D-258. Everyone else signed in reads the customer pages.
 * The one place the rule lives, so a role read from vaadin.com's own user
 * store later replaces this class and nothing else.
 */
@Component
public class TeamRule {

    /** The attributes CAS carries the email in, as vaadin.com's own applications read them. */
    static final List<String> EMAIL_ATTRIBUTES = List.of("email", "emailAddress", "mail");
    static final String VERIFIED_ATTRIBUTE = "emailAddressVerified";

    private final String domain;

    public TeamRule(@Value("${securitystatus.signin.team-domain:vaadin.com}") String domain) {
        this.domain = domain.toLowerCase(Locale.ROOT);
    }

    public boolean isTeam(Optional<String> email, boolean verified) {
        return verified && email.map(address -> address.strip().toLowerCase(Locale.ROOT))
                .filter(address -> address.lastIndexOf('@') > 0)
                .map(address -> address.substring(address.lastIndexOf('@') + 1))
                .filter(domain::equals).isPresent();
    }

    public List<GrantedAuthority> authorities(Optional<String> email, boolean verified) {
        return isTeam(email, verified) ? List.of(new SimpleGrantedAuthority("ROLE_" + Roles.SECURITY)) : List.of();
    }

    /** The authorities of a user CAS vouched for, from the attributes it released. */
    public List<GrantedAuthority> authorities(Map<String, Object> attributes) {
        Optional<String> email = EMAIL_ATTRIBUTES.stream().map(name -> first(attributes.get(name)))
                .flatMap(Optional::stream).filter(value -> !value.isBlank()).findFirst();
        // An attribute CAS does not send cannot be held against the user; one it sends must say true.
        boolean verified = !attributes.containsKey(VERIFIED_ATTRIBUTE)
                || first(attributes.get(VERIFIED_ATTRIBUTE)).map(Boolean::parseBoolean).orElse(false);
        return authorities(email, verified);
    }

    private static Optional<String> first(Object value) {
        if (value instanceof Collection<?> values) {
            return values.stream().findFirst().map(String::valueOf);
        }
        return Optional.ofNullable(value).map(String::valueOf);
    }
}

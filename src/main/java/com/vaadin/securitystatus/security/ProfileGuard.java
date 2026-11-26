package com.vaadin.securitystatus.security;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import tools.jackson.databind.json.JsonMapper;

/**
 * Refuses to start a production build that names neither {@code cas} nor
 * {@code demo}, D-258: the default profile is {@code demo}, whose accounts are
 * in the source, and a deployment has to say whether it signs in through
 * {@code cas} or shows the demonstration on purpose.
 */
@Component
class ProfileGuard {

    static final String BUILD_INFO = "META-INF/VAADIN/config/flow-build-info.json";

    ProfileGuard(Environment environment) {
        var active = java.util.Arrays.asList(environment.getActiveProfiles());
        if (!active.contains("cas") && !active.contains("demo") && productionBuild()) {
            // A production build has to name how people sign in: cas, or demo for the demonstration accounts.
            throw new IllegalStateException("spring.profiles.active=cas|demo");
        }
    }

    /** Whether this is a production build, as the Vaadin build wrote it into the application. */
    static boolean productionBuild() {
        try (InputStream info = ProfileGuard.class.getClassLoader().getResourceAsStream(BUILD_INFO)) {
            return info != null && JsonMapper.builder().build().readTree(info).path("productionMode").asBoolean(false);
        } catch (IOException | RuntimeException unreadable) {
            return false;
        }
    }
}

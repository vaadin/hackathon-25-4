package com.vaadin.securitystatus.data.model;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

/** {@code cases/cases.json}: every state a screen can be in, with a release or advisory that shows it today. */
public record CaseCatalog(Optional<Instant> generatedAt, List<Case> cases) {

    public CaseCatalog {
        generatedAt = Wire.value(generatedAt);
        cases = Wire.list(cases);
    }

    public record Case(String id, Optional<String> title, Optional<String> screen, Optional<String> status,
            @Nullable Integer count, Optional<String> example, Optional<String> needs) {

        public Case {
            title = Wire.text(title);
            needs = Wire.text(needs);
            screen = Wire.text(screen);
            status = Wire.text(status);
            example = Wire.text(example);
        }
    }
}

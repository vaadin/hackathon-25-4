package com.vaadin.securitystatus.data.model;

import java.util.Optional;

import com.vaadin.securitystatus.data.VersionNumber;

/** Where a Spring Boot release can be had: openly, or commercially only. */
public record SpringBootSupply(Optional<VersionNumber> open, Optional<String> state,
        Optional<VersionNumber> commercial) {

    public SpringBootSupply {
        open = Wire.value(open);
        state = Wire.text(state);
        commercial = Wire.value(commercial);
    }
}

package com.vaadin.securitystatus.data.model;

import java.util.Optional;

import com.vaadin.securitystatus.data.VersionNumber;

/** The newest Spring Boot a release can move to, within its patch line and its minor. */
public record SpringBootLatest(Optional<VersionNumber> patch, Optional<VersionNumber> minor) {

    public SpringBootLatest {
        patch = Wire.value(patch);
        minor = Wire.value(minor);
    }
}

package com.vaadin.securitystatus.data;

import org.jspecify.annotations.Nullable;

/**
 * The one real failure of the data layer: nothing cached and the published
 * data cannot be read. A screen answers it with the unreachable sentence. The
 * message is the path that could not be answered.
 */
public final class DataUnreachableException extends RuntimeException {

    DataUnreachableException(String path, @Nullable Throwable cause) {
        super(path, cause);
    }
}

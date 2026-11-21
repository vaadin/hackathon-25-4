package com.vaadin.securitystatus.data;

import java.io.InputStream;

/**
 * One per release file, open and unread. Whoever takes it reads the stream and
 * closes it. The content type is the one the publisher sent, and the length is
 * -1 when the publisher did not say.
 */
public record ExportFile(InputStream body, String fileName, String contentType, long length) {
}

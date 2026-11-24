package com.vaadin.securitystatus.ui.common;

import java.io.InputStream;
import java.util.Optional;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.download.Download;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;

/**
 * The one place a download is put together, so the file name, the content type
 * and the disposition are decided once for everything that leaves the
 * application.
 */
public final class Downloads {

    private static final Logger LOG = LoggerFactory.getLogger(Downloads.class);

    /**
     * What is sent: a stream that is read once and closed by the framework,
     * the content type as it should reach the browser, and the length in
     * bytes, -1 when it is not known.
     */
    public record Content(InputStream body, String contentType, long length) {
    }

    private Downloads() {
    }

    /**
     * A link that downloads a file under the given name. The content is asked
     * for when the browser requests it, never before, and is streamed. An
     * empty answer is a 404, and a source that fails is a 502, because what
     * failed is somewhere behind this application.
     */
    public static Anchor link(String text, String fileName, Supplier<Optional<Content>> source) {
        return new Anchor(handler(fileName, source), text);
    }

    /**
     * The same download, started by a click on any component, which keeps its
     * own look: the browser asks for the file inside the click, without a round
     * trip first.
     */
    public static Registration onClick(Component component, String fileName, Supplier<Optional<Content>> source) {
        return Download.onClick(component, handler(fileName, source));
    }

    /** A handler for a component that shows the content itself rather than saving it, such as a viewer. */
    public static DownloadHandler shown(String fileName, Supplier<Optional<Content>> source) {
        return handler(fileName, source);
    }

    static DownloadHandler handler(String fileName, Supplier<Optional<Content>> source) {
        return DownloadHandler.fromInputStream(event -> {
            try {
                return source.get()
                        .map(content -> new DownloadResponse(content.body(), fileName, content.contentType(),
                                content.length()))
                        .orElseGet(() -> DownloadResponse.error(404));
            } catch (RuntimeException failure) {
                LOG.warn("Download of {} failed: {}", fileName, failure.getMessage());
                return DownloadResponse.error(502);
            }
        }, fileName);
    }
}

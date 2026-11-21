package com.vaadin.securitystatus.it;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.util.Properties;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * Stands in for GitHub Pages during the browser tests. The application under
 * test is a separate process, started by Maven with its data address pointed
 * at {@code localhost:<it.pages.port>/cve-status/}, and this serves the
 * committed fixtures there over real HTTP, with the content type the live host
 * gave each. One server for the whole run, stopped when the run ends.
 */
public final class FixturePagesServer implements BeforeAllCallback {

    private static final String SITE = "/cve-status/";
    private static final String ROOT = "pages/";

    @Override
    public void beforeAll(ExtensionContext context) {
        context.getRoot().getStore(ExtensionContext.Namespace.GLOBAL).getOrComputeIfAbsent(FixturePagesServer.class,
                key -> new Running(), Running.class);
    }

    /** The committed fixture, to compare a download against. */
    static byte[] fixture(String path) {
        try (InputStream body = FixturePagesServer.class.getClassLoader().getResourceAsStream(ROOT + path)) {
            if (body == null) {
                throw new IllegalArgumentException(path);
            }
            return body.readAllBytes();
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    private static final class Running implements AutoCloseable {

        private final Properties contentTypes = new Properties();
        private final HttpServer server;

        Running() {
            try (InputStream types = FixturePagesServer.class.getClassLoader()
                    .getResourceAsStream(ROOT + "content-types.properties")) {
                contentTypes.load(types);
                int port = Integer.parseInt(System.getProperty("it.pages.port", "8082"));
                server = HttpServer.create(new InetSocketAddress("localhost", port), 0);
                server.createContext(SITE, this::answer);
                server.start();
            } catch (IOException failure) {
                throw new UncheckedIOException(failure);
            }
        }

        private void answer(HttpExchange exchange) throws IOException {
            try (exchange) {
                String path = exchange.getRequestURI().getPath().substring(SITE.length());
                // Only what the refresh script wrote, spelled as it wrote it.
                if (!contentTypes.containsKey(path)) {
                    exchange.sendResponseHeaders(404, -1);
                    return;
                }
                byte[] body = fixture(path);
                exchange.getResponseHeaders().set("Content-Type", contentTypes.getProperty(path));
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            }
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }
}

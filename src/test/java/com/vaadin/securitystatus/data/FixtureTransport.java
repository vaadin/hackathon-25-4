package com.vaadin.securitystatus.data;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.zip.CRC32;

import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;

/**
 * Stands in for the network: answers the paths under the site's address from
 * the committed fixtures in {@code src/test/resources/pages/}, with the content
 * type the live host served each with, an {@code ETag}, and a 404 for a path
 * that has no fixture. A test can replace one answer, or take the host down.
 */
public final class FixtureTransport implements ClientHttpRequestFactory {

    /** An address for a client built by hand. Any address ending in the site's name is answered. */
    public static final String BASE_URL = "https://fixtures.invalid/cve-status/";

    private static final String SITE = "/cve-status/";

    private static final String ROOT = "pages/";

    /** One request the transport answered: the path asked for and how it ended. */
    public record Request(String path, int status, @Nullable String ifNoneMatch) {
    }

    private final Properties contentTypes = new Properties();
    private final Map<String, byte[]> replaced = new ConcurrentHashMap<>();
    private final List<Request> requests = new CopyOnWriteArrayList<>();
    private volatile boolean down;
    private volatile int downWithStatus;

    public FixtureTransport() {
        try (InputStream types = resource("content-types.properties")) {
            if (types != null) {
                contentTypes.load(types);
            }
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    @Override
    public ClientHttpRequest createRequest(URI uri, HttpMethod method) {
        return new MockClientHttpRequest(method, uri) {
            @Override
            protected ClientHttpResponse executeInternal() throws IOException {
                return answer(uri, getHeaders());
            }
        };
    }

    /** Every connection is refused from now on. */
    public void goDown() {
        down = true;
        downWithStatus = 0;
    }

    /** Every request is answered with this status from now on. */
    public void answerEverythingWith(int status) {
        down = true;
        downWithStatus = status;
    }

    public void comeBack() {
        down = false;
    }

    /** Answers one path with another body from now on. The fixture files are never touched. */
    public void replace(String path, String body) {
        replaced.put(path, body.getBytes(StandardCharsets.UTF_8));
    }

    /** The committed fixture, as text. */
    public String fixture(String path) {
        return new String(fixtureBytes(path), StandardCharsets.UTF_8);
    }

    public byte[] fixtureBytes(String path) {
        try (InputStream body = resource(path)) {
            if (body == null) {
                throw new IllegalArgumentException("no fixture for " + path);
            }
            return body.readAllBytes();
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    public List<Request> requests() {
        return new ArrayList<>(requests);
    }

    public List<Request> requestsFor(String path) {
        return requests.stream().filter(request -> request.path().equals(path)).toList();
    }

    /** As it was built: the host up, no answer replaced, no request remembered. */
    public void reset() {
        down = false;
        replaced.clear();
        requests.clear();
    }

    private ClientHttpResponse answer(URI uri, HttpHeaders asked) throws IOException {
        String address = uri.toString();
        String path = address.substring(address.indexOf(SITE) + SITE.length());
        String ifNoneMatch = asked.getFirst(HttpHeaders.IF_NONE_MATCH);
        if (down && downWithStatus == 0) {
            requests.add(new Request(path, 0, ifNoneMatch));
            throw new IOException("connection refused, the fixture host is down");
        }
        if (down) {
            return answered(path, ifNoneMatch, downWithStatus, new byte[0], null);
        }
        byte[] body = replaced.get(path);
        if (body == null) {
            // Only the paths the refresh script wrote, spelled as it wrote them: the live host is
            // case sensitive, and the file system this runs on may not be.
            try (InputStream fixture = contentTypes.containsKey(path) ? resource(path) : null) {
                body = fixture == null ? null : fixture.readAllBytes();
            }
        }
        if (body == null) {
            return answered(path, ifNoneMatch, 404, new byte[0], null);
        }
        String etag = etag(body);
        if (etag.equals(ifNoneMatch)) {
            return answered(path, ifNoneMatch, 304, new byte[0], etag);
        }
        return answered(path, ifNoneMatch, 200, body, etag);
    }

    private ClientHttpResponse answered(String path, @Nullable String ifNoneMatch, int status, byte[] body,
            @Nullable String etag) {
        requests.add(new Request(path, status, ifNoneMatch));
        var response = new MockClientHttpResponse(body, HttpStatus.valueOf(status));
        if (etag != null) {
            response.getHeaders().set(HttpHeaders.ETAG, etag);
        }
        if (status == 200) {
            response.getHeaders().set(HttpHeaders.CONTENT_TYPE,
                    contentTypes.getProperty(path, "application/octet-stream"));
            response.getHeaders().setContentLength(body.length);
        }
        return response;
    }

    private static String etag(byte[] body) {
        var sum = new CRC32();
        sum.update(body);
        return "\"" + HexFormat.of().toHexDigits(sum.getValue(), 8) + "-" + Integer.toHexString(body.length) + "\"";
    }

    private static @Nullable InputStream resource(String path) {
        return FixtureTransport.class.getClassLoader().getResourceAsStream(ROOT + path);
    }
}

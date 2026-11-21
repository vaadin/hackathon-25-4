package com.vaadin.securitystatus.data;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * The one class that talks HTTP. It knows the published site and nothing about
 * what the documents mean: a path goes in, and what the host said comes out.
 */
final class PagesClient {

    /** What the host said about one path. */
    sealed interface Answer {

        record Body(byte[] bytes, @Nullable String etag) implements Answer {
        }

        /** The copy the given {@code ETag} names is still the published one. */
        record NotModified() implements Answer {
        }

        record NotFound() implements Answer {
        }
    }

    /** An open response, read by whoever asked and closed with its stream. */
    record Stream(InputStream body, @Nullable String contentType, long length) {
    }

    /** The host did not answer, or answered with something that is not an answer. */
    static final class UnreachableException extends Exception {

        UnreachableException(String message, @Nullable Throwable cause) {
            super(message, cause);
        }
    }

    private final String baseUrl;
    private final RestClient http;

    PagesClient(String baseUrl, ClientHttpRequestFactory transport) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        this.http = RestClient.builder().requestFactory(transport).build();
    }

    /** Fetches a document, conditionally when an {@code ETag} of it is known. */
    Answer fetch(String path, @Nullable String etag) throws UnreachableException {
        try {
            Answer answer = http.get().uri(uri(path)).headers(headers -> {
                if (etag != null) {
                    headers.set(HttpHeaders.IF_NONE_MATCH, etag);
                }
            }).exchange((request, response) -> {
                int status = response.getStatusCode().value();
                return switch (status) {
                case 200 -> new Answer.Body(response.getBody().readAllBytes(), response.getHeaders().getETag());
                case 304 -> new Answer.NotModified();
                case 404 -> new Answer.NotFound();
                default -> throw new IOException("status " + status);
                };
            });
            if (answer == null) {
                throw new UnreachableException(path + ": no answer", null);
            }
            return answer;
        } catch (RestClientException | IllegalStateException failure) {
            throw new UnreachableException(path + ": " + failure.getMessage(), failure);
        }
    }

    /**
     * Opens a file for streaming, without reading it. Empty when the host has
     * no such file.
     */
    Optional<Stream> open(String path) throws UnreachableException {
        try {
            Optional<Stream> stream = http.get().uri(uri(path)).exchange((request, response) -> {
                int status = response.getStatusCode().value();
                if (status == 404) {
                    response.close();
                    return Optional.<Stream>empty();
                }
                if (status != 200) {
                    response.close();
                    throw new IOException("status " + status);
                }
                HttpHeaders headers = response.getHeaders();
                // The header as sent, not parsed and printed again: the type is passed on, not interpreted.
                return Optional.of(new Stream(closing(response), headers.getFirst(HttpHeaders.CONTENT_TYPE),
                        headers.getContentLength()));
            }, false);
            return stream == null ? Optional.empty() : stream;
        } catch (RestClientException | IllegalStateException failure) {
            throw new UnreachableException(path + ": " + failure.getMessage(), failure);
        }
    }

    private URI uri(String path) {
        return URI.create(baseUrl + path);
    }

    private static InputStream closing(ClientHttpResponse response) throws IOException {
        return new FilterInputStream(response.getBody()) {
            @Override
            public void close() throws IOException {
                try {
                    super.close();
                } finally {
                    response.close();
                }
            }
        };
    }
}

package com.vaadin.securitystatus.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * D-235. Deny by default, measured on the running application with no
 * session: an unknown page address reaches the not found view, every secured
 * route and any other method go to sign in.
 */
@ExtendWith(FixturePagesServer.class)
class DenyByDefaultIT {

    private final HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
    private final String base = "http://localhost:" + System.getProperty("it.port", "8080");

    @Test
    void only_an_unknown_page_address_is_let_through_without_signing_in() throws Exception {
        assertThat(status("GET", "/")).isEqualTo(200);
        assertThat(status("GET", "/no-such-route")).isEqualTo(200);
        assertThat(status("GET", "/icons/favicon.svg")).isEqualTo(200);

        assertThat(location("GET", "/findings")).endsWith("/login");
        assertThat(location("GET", "/diagnostics")).endsWith("/login");
        assertThat(location("POST", "/no-such-route")).contains("/login");
    }

    private int status(String method, String path) throws Exception {
        return send(method, path).statusCode();
    }

    private String location(String method, String path) throws Exception {
        HttpResponse<Void> response = send(method, path);
        assertThat(response.statusCode()).isEqualTo(302);
        return response.headers().firstValue("Location").orElse("");
    }

    private HttpResponse<Void> send(String method, String path) throws Exception {
        var request = HttpRequest.newBuilder(URI.create(base + path))
                .method(method, HttpRequest.BodyPublishers.noBody()).build();
        return client.send(request, HttpResponse.BodyHandlers.discarding());
    }
}

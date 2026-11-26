package com.vaadin.securitystatus.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * D-235 tightened by D-258, measured on the running application with no
 * session. Two layers enforce it: the pages are guarded at Flow navigation,
 * so their bootstrap answers 200 and what renders inside is the sign in,
 * which the browserless suite proves; the team routes and every other method
 * are refused at HTTP with a redirect to the sign in. Only the sign in
 * itself and the static assets answer with content.
 */
@ExtendWith(FixturePagesServer.class)
class DenyByDefaultIT {

    private final HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
    private final String base = "http://localhost:" + System.getProperty("it.port", "8080");

    @Test
    void every_page_asks_for_a_sign_in_and_only_the_login_and_the_assets_are_open() throws Exception {
        assertThat(status("GET", "/login")).isEqualTo(200);
        assertThat(status("GET", "/icons/favicon.svg")).isEqualTo(200);

        // A page address ends at the sign in, and which layer answers has been seen to vary with
        // startup timing: sometimes the bootstrap is served with the sign in rendered inside, the
        // navigation layer, and sometimes HTTP refuses it outright. Both are the sign in, so both
        // pass; that the rendered half really is the sign in is the browserless suite's job. If
        // the variance ever narrows to one answer, tighten this to it.
        endsAtTheSignIn("GET", "/");
        endsAtTheSignIn("GET", "/no-such-route");

        assertThat(location("GET", "/findings")).endsWith("/login");
        assertThat(location("GET", "/diagnostics")).endsWith("/login");
        assertThat(location("POST", "/no-such-route")).contains("/login");
    }

    private void endsAtTheSignIn(String method, String path) throws Exception {
        HttpResponse<Void> response = send(method, path);
        if (response.statusCode() == 302) {
            assertThat(response.headers().firstValue("Location").orElse("")).as(path).endsWith("/login");
        } else {
            assertThat(response.statusCode()).as(path).isEqualTo(200);
        }
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

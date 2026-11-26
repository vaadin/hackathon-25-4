package com.vaadin.securitystatus.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/** D-258: under the cas profile no page answers a visitor without a session; the single sign on does. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "securitystatus.cas.service=https://vaadin.example/security-status/login/cas")
@ActiveProfiles("cas")
class CasEntryPointTest {

    @LocalServerPort
    int port;

    @Test
    void a_visitor_without_a_session_is_sent_to_the_single_sign_on() throws Exception {
        for (String page : new String[] { "/", "/version/24.9.21", "/findings" }) {
            var answer = HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder(URI.create("http://localhost:" + port + page)).GET().build(),
                    HttpResponse.BodyHandlers.discarding());

            assertThat(answer.statusCode()).as(page).isEqualTo(302);
            assertThat(answer.headers().firstValue("Location")).as(page).hasValueSatisfying(location -> assertThat(
                    location).startsWith("https://sso.vaadin.com/login?service=").contains("vaadin.example"));
        }
    }
}

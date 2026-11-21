package com.vaadin.securitystatus.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequestFactory;

import com.vaadin.securitystatus.data.FixtureTransport.Request;
import com.vaadin.securitystatus.data.model.CasesIndex;
import com.vaadin.securitystatus.data.model.VersionAnswer;

/**
 * The cache policy, with the real client, cache, mapper and service over the
 * committed fixtures, and a clock the test moves.
 */
class PagesCacheTest {

    private static final String INDEX = SecurityDataService.INDEX;
    private static final String VERSION = "cases/v/24.9.21.json";
    private static final VersionNumber MEASURED = VersionNumber.of("24.9.21");

    private static final class MovableClock extends Clock {

        private Instant now = FixturePages.NOW;

        void advance(Duration by) {
            now = now.plus(by);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }

    private final FixtureTransport host = new FixtureTransport();
    private final MovableClock clock = new MovableClock();
    private final DataRefreshSignal refresh = new DataRefreshSignal();
    private SecurityDataService service;

    @BeforeEach
    void wire() {
        service = serviceOver(host);
    }

    private SecurityDataService serviceOver(ClientHttpRequestFactory transport) {
        var client = new PagesClient(FixtureTransport.BASE_URL, transport);
        return new SecurityDataService(new PagesCache(client, clock), client, refresh, Runnable::run);
    }

    // DAT-01
    @Test
    void a_second_read_inside_the_time_to_live_makes_no_request() {
        CasesIndex first = service.index();
        assertThat(host.requestsFor(INDEX)).hasSize(1);

        clock.advance(SecurityDataService.INDEX_FRESH_FOR.minusSeconds(1));
        CasesIndex second = service.index();

        assertThat(host.requestsFor(INDEX)).hasSize(1);
        assertThat(second).isSameAs(first);
    }

    // DAT-02
    @Test
    void a_304_keeps_the_cached_document_and_renews_its_time_to_live() {
        CasesIndex first = service.index();
        clock.advance(SecurityDataService.INDEX_FRESH_FOR.plusSeconds(1));

        CasesIndex revalidated = service.index();

        List<Request> asked = host.requestsFor(INDEX);
        assertThat(asked).extracting(Request::status).containsExactly(200, 304);
        assertThat(asked.get(1).ifNoneMatch()).isNotBlank();
        // The same object, so nothing was parsed again.
        assertThat(revalidated).isSameAs(first);

        clock.advance(SecurityDataService.INDEX_FRESH_FOR.minusSeconds(1));
        service.index();
        assertThat(host.requestsFor(INDEX)).hasSize(2);
    }

    @Test
    void a_document_that_changed_upstream_is_read_again() {
        CasesIndex first = service.index();
        host.replace(INDEX, host.fixture(INDEX).replace("\"risk\":\"affects you\"", "\"risk\":\"reworded\""));
        clock.advance(SecurityDataService.INDEX_FRESH_FOR.plusSeconds(1));

        CasesIndex second = service.index();

        assertThat(host.requestsFor(INDEX)).extracting(Request::status).containsExactly(200, 200);
        assertThat(first.labels()).containsEntry("risk", "affects you");
        assertThat(second.labels()).containsEntry("risk", "reworded");
    }

    // DAT-03
    @Test
    void a_dead_upstream_leaves_the_stale_answers_and_the_measured_date_in_place() {
        service.revalidate();
        CasesIndex index = service.index();
        VersionAnswer answer = service.version(MEASURED).orElseThrow();
        assertThat(refresh.generatedAt().peek()).isEqualTo(FixturePages.PINNED_GENERATED_AT);

        host.goDown();
        clock.advance(SecurityDataService.DOCUMENT_FRESH_FOR.plusSeconds(1));

        assertThat(service.index()).isSameAs(index);
        assertThat(service.version(MEASURED)).containsSame(answer);
        service.revalidate();
        assertThat(refresh.generatedAt().peek()).isEqualTo(FixturePages.PINNED_GENERATED_AT);
        // The host was really asked, and really refused: the answers above are the stale ones.
        assertThat(host.requestsFor(VERSION)).extracting(Request::status).containsExactly(200, 0);
    }

    @Test
    void a_500_is_a_dead_upstream_too() {
        VersionAnswer answer = service.version(MEASURED).orElseThrow();
        host.answerEverythingWith(500);
        clock.advance(SecurityDataService.DOCUMENT_FRESH_FOR.plusSeconds(1));

        assertThat(service.version(MEASURED)).containsSame(answer);
        assertThat(host.requestsFor(VERSION)).extracting(Request::status).containsExactly(200, 500);
    }

    @Test
    void a_dead_upstream_is_not_asked_again_on_every_read_and_is_asked_again_later() {
        service.version(MEASURED);
        host.goDown();
        clock.advance(SecurityDataService.DOCUMENT_FRESH_FOR.plusSeconds(1));

        service.version(MEASURED);
        service.version(MEASURED);
        assertThat(host.requestsFor(VERSION)).hasSize(2);

        host.comeBack();
        clock.advance(PagesCache.RETRY_AFTER_FAILURE.plusSeconds(1));
        service.version(MEASURED);
        assertThat(host.requestsFor(VERSION)).extracting(Request::status).containsExactly(200, 0, 304);
    }

    @Test
    void a_cold_cache_and_a_dead_upstream_is_the_one_failure() {
        host.goDown();

        assertThatThrownBy(service::index).isInstanceOf(DataUnreachableException.class);
        assertThatThrownBy(() -> service.version(MEASURED)).isInstanceOf(DataUnreachableException.class);

        // Nothing was stored as an error: the next read asks again and is answered.
        host.comeBack();
        assertThat(service.version(MEASURED)).isPresent();
    }

    @Test
    void a_404_is_an_empty_answer_and_is_not_cached() {
        var nobodyMeasured = VersionNumber.of("0.0.0");

        assertThat(service.version(nobodyMeasured)).isEmpty();
        assertThat(service.version(nobodyMeasured)).isEmpty();
        assertThat(service.cve("CVE-1999-0001")).isEmpty();

        assertThat(host.requestsFor("cases/v/0.0.0.json")).extracting(Request::status).containsExactly(404, 404);
    }

    @Test
    void an_id_that_is_not_an_id_is_answered_empty_without_asking_anybody() {
        assertThat(service.cve("../index")).isEmpty();
        assertThat(service.cve("CVE-2016-1000027/../../index")).isEmpty();
        assertThat(service.version(VersionNumber.of("24.9.21?x=1"))).isEmpty();
        assertThat(service.export(VersionNumber.of("../cases/index"), ExportKind.CSV)).isEmpty();

        assertThat(host.requests()).isEmpty();
    }

    @Test
    void twenty_readers_of_a_cold_index_cause_one_fetch() throws Exception {
        var firstRequestArrived = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        ClientHttpRequestFactory slow = (URI uri, HttpMethod method) -> {
            firstRequestArrived.countDown();
            try {
                if (!release.await(30, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("never released");
                }
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(interrupted);
            }
            return host.createRequest(uri, method);
        };
        SecurityDataService shared = serviceOver(slow);

        try (var readers = Executors.newFixedThreadPool(20)) {
            var answers = new ArrayList<Future<CasesIndex>>();
            for (int i = 0; i < 20; i++) {
                answers.add(readers.submit(shared::index));
            }
            assertThat(firstRequestArrived.await(30, TimeUnit.SECONDS)).isTrue();
            release.countDown();

            CasesIndex one = answers.getFirst().get(30, TimeUnit.SECONDS);
            for (Future<CasesIndex> answer : answers) {
                assertThat(answer.get(30, TimeUnit.SECONDS)).isSameAs(one);
            }
        }
        assertThat(host.requestsFor(INDEX)).hasSize(1);
    }

    @Test
    void a_moved_generated_at_reaches_the_signal_and_makes_the_other_documents_due() {
        service.revalidate();
        VersionAnswer answer = service.version(MEASURED).orElseThrow();
        assertThat(host.requestsFor(VERSION)).hasSize(1);

        Instant nextNight = Instant.parse("2026-11-21T03:17:42Z");
        host.replace(INDEX, host.fixture(INDEX).replace(FixturePages.PINNED_GENERATED_AT.toString(),
                nextNight.toString()));
        service.revalidate();

        assertThat(refresh.generatedAt().peek()).isEqualTo(nextNight);
        // Due, not dropped: it is asked about again, and the 304 keeps the parsed copy.
        assertThat(service.version(MEASURED)).containsSame(answer);
        assertThat(host.requestsFor(VERSION)).extracting(Request::status).containsExactly(200, 304);
    }

    @Test
    void an_unmoved_generated_at_leaves_the_other_documents_alone() {
        service.revalidate();
        service.version(MEASURED);

        service.revalidate();
        service.version(MEASURED);

        assertThat(host.requestsFor(INDEX)).extracting(Request::status).containsExactly(200, 304);
        assertThat(host.requestsFor(VERSION)).hasSize(1);
    }

    @Test
    void a_generated_at_that_moves_backwards_is_trusted() {
        service.revalidate();
        Instant earlier = Instant.parse("2026-11-19T03:17:42Z");
        host.replace(INDEX,
                host.fixture(INDEX).replace(FixturePages.PINNED_GENERATED_AT.toString(), earlier.toString()));

        service.revalidate();

        assertThat(refresh.generatedAt().peek()).isEqualTo(earlier);
    }

    @Test
    void a_read_that_finds_a_moved_index_tells_the_signal_too() {
        service.index();

        assertThat(refresh.generatedAt().peek()).isEqualTo(FixturePages.PINNED_GENERATED_AT);
    }

    @Test
    void an_export_is_handed_over_open_with_the_type_and_the_name_the_publisher_gave_it() throws Exception {
        var release = VersionNumber.of("25.3.5");

        ExportFile file = service.export(release, ExportKind.OSV_SCANNER).orElseThrow();

        try (var body = file.body()) {
            assertThat(body.readAllBytes()).isEqualTo(host.fixtureBytes("exports/25.3.5.osv-scanner.toml"));
        }
        assertThat(file.fileName()).isEqualTo("25.3.5.osv-scanner.toml");
        assertThat(file.contentType()).isEqualTo("application/toml");
        assertThat(file.length()).isEqualTo(host.fixtureBytes("exports/25.3.5.osv-scanner.toml").length);
        assertThat(service.export(VersionNumber.of("0.0.0"), ExportKind.CSV)).isEmpty();
    }
}

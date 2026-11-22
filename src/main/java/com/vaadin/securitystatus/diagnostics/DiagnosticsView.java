package com.vaadin.securitystatus.diagnostics;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.html.Table;
import com.vaadin.flow.component.html.TableRow;
import com.vaadin.flow.component.page.ExtendedClientDetails;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.securitystatus.base.observe.AssistantTail;
import com.vaadin.securitystatus.base.security.CspViolations;
import com.vaadin.securitystatus.base.security.Roles;
import com.vaadin.securitystatus.base.ui.InternalLayout;
import com.vaadin.securitystatus.data.DataRefreshSignal;
import com.vaadin.securitystatus.diagnostics.DiagnosticsCounters.Kind;
import com.vaadin.securitystatus.diagnostics.DiagnosticsCounters.Minute;
import com.vaadin.securitystatus.diagnostics.DiagnosticsCounters.Navigations;
import com.vaadin.securitystatus.diagnostics.DiagnosticsCounters.Requests;
import com.vaadin.securitystatus.diagnostics.DiagnosticsCounters.RouteCount;

import jakarta.annotation.security.RolesAllowed;

/**
 * What the application has been doing since it started, read live: the
 * navigations per route and outcome, the request timings, the sessions open
 * now, the policy violations browsers reported and the assistant's exchanges,
 * and the reader's own browser as the extended client details describe it.
 * Every panel is bound to a shared signal, so another session's navigation
 * moves the numbers here without a reload. There is no refresh button.
 */
@Route(value = "diagnostics", layout = InternalLayout.class)
@PageTitle("diagnostics.title")
@Menu(order = 3, title = "diagnostics.title")
@RolesAllowed(Roles.SECURITY)
public final class DiagnosticsView extends Composite<Div> {

    /** How many minutes of request timings the table shows, newest first. */
    static final int MINUTES_SHOWN = 10;

    /** How many entries of each tail are shown, newest first. */
    static final int CSP_SHOWN = 20;
    static final int ASSISTANT_SHOWN = 50;

    private static final DateTimeFormatter AT = new DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_LOCAL_DATE).appendLiteral(' ').appendPattern("HH:mm:ss").toFormatter()
            .withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter MINUTE = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneOffset.UTC);

    private final ValueSignal<@Nullable ExtendedClientDetails> client = new ValueSignal<>(null);
    private final ValueSignal<int[]> viewport = new ValueSignal<>(new int[0]);

    public DiagnosticsView(DiagnosticsCounters counters, CspViolations violations, AssistantTail tail,
            DataRefreshSignal refresh) {
        addClassName("diagnostics-view");

        var since = new Paragraph(getTranslation("diagnostics.since", AT.format(counters.since())));
        since.addClassName("diagnostics-since");

        getContent().add(since, navigationsPanel(counters.navigations()),
                requestsPanel(counters.requests()), sessionsPanel(counters.sessions(), refresh.generatedAt()),
                clientPanel(), tailsPanel(violations, tail));

        UI.getCurrent().getPage().retrieveExtendedClientDetails(client::set);
        whenAttached(ui -> ui.getPage().addBrowserWindowResizeListener(
                resized -> viewport.set(new int[] { resized.getWidth(), resized.getHeight() })));
    }

    private Section panel(String name, String heading) {
        var section = new Section(new H2(getTranslation(heading)));
        section.addClassNames("diagnostics-panel", "diagnostics-" + name);
        return section;
    }

    private Section navigationsPanel(Signal<Navigations> navigations) {
        Section section = panel("navigations", "diagnostics.navigations.title");
        var body = new Div();
        section.add(body);
        Signal.effect(section, () -> {
            Navigations counted = navigations.get();
            body.removeAll();
            if (counted.routes().isEmpty()) {
                body.add(note("diagnostics.navigations.none"));
                return;
            }
            var table = new Table();
            table.addClassName("diagnostics-navigations-table");
            table.addHeaderRow(getTranslation("diagnostics.navigations.route"),
                    getTranslation("diagnostics.outcome.completed"), getTranslation("diagnostics.outcome.failed"),
                    getTranslation("diagnostics.outcome.notShown"), getTranslation("diagnostics.outcome.postponed"),
                    getTranslation("diagnostics.navigations.mean"), getTranslation("diagnostics.navigations.longest"));
            for (RouteCount route : counted.routes()) {
                TableRow row = table.addRow();
                row.getElement().setAttribute("data-route", route.route());
                row.addDataCell(DiagnosticsCounters.OTHER_ROUTE.equals(route.route())
                        ? getTranslation("diagnostics.navigations.other")
                        : route.route());
                row.addDataCell(String.valueOf(route.completed()));
                row.addDataCell(String.valueOf(route.failed()));
                row.addDataCell(String.valueOf(route.notShown()));
                row.addDataCell(String.valueOf(route.postponed()));
                row.addDataCell(millis(route.totalMillis() / (double) route.total()));
                row.addDataCell(millis(route.longestMillis()));
            }
            TableRow total = table.addRow();
            total.addClassName("diagnostics-total");
            total.addRowHeaderCell(getTranslation("diagnostics.navigations.total"));
            for (Outcome outcome : Outcome.values()) {
                total.addDataCell(String.valueOf(counted.total(outcome)));
            }
            body.add(table);
        });
        return section;
    }

    private Section requestsPanel(Signal<Requests> requests) {
        Section section = panel("requests", "diagnostics.requests.title");
        var body = new Div();
        section.add(body, note("diagnostics.requests.kept"));
        Signal.effect(section, () -> {
            Requests counted = requests.get();
            body.removeAll();
            if (counted.count() == 0) {
                body.add(note("diagnostics.requests.none"));
                return;
            }
            var summary = new Paragraph(getTranslation("diagnostics.requests.summary", counted.count(),
                    counted.failed(), millis(counted.totalMicros() / 1000.0 / counted.count()),
                    millis(counted.longestMicros() / 1000.0)));
            summary.addClassName("diagnostics-requests-summary");

            var minutes = new Table();
            minutes.addClassName("diagnostics-minutes");
            minutes.addHeaderRow(getTranslation("diagnostics.requests.minute"),
                    getTranslation("diagnostics.requests.count"), getTranslation("diagnostics.requests.failed"),
                    getTranslation("diagnostics.requests.mean"), getTranslation("diagnostics.requests.longest"));
            List<Minute> newestFirst = counted.minutes().reversed();
            for (Minute minute : newestFirst.subList(0, Math.min(MINUTES_SHOWN, newestFirst.size()))) {
                TableRow row = minutes.addRow();
                row.addDataCell(MINUTE.format(minute.start()));
                row.addDataCell(String.valueOf(minute.count()));
                row.addDataCell(String.valueOf(minute.failed()));
                row.addDataCell(millis(minute.totalMicros() / 1000.0 / minute.count()));
                row.addDataCell(millis(minute.longestMicros() / 1000.0));
            }

            var kinds = new Table();
            kinds.addClassName("diagnostics-kinds");
            kinds.addHeaderRow(getTranslation("diagnostics.requests.handler"),
                    getTranslation("diagnostics.requests.count"));
            for (Kind kind : counted.kinds()) {
                TableRow row = kinds.addRow();
                row.addDataCell(kind.handler().isEmpty() ? getTranslation("diagnostics.requests.noHandler")
                        : kind.handler());
                row.addDataCell(String.valueOf(kind.count()));
            }
            var tables = new Div(minutes, kinds);
            tables.addClassName("diagnostics-request-tables");
            body.add(summary, tables);
        });
        return section;
    }

    private Section sessionsPanel(Signal<Long> sessions, Signal<@Nullable Instant> generatedAt) {
        Section section = panel("sessions", "diagnostics.sessions.title");
        var now = new Paragraph();
        now.addClassName("diagnostics-sessions-now");
        now.bindText(sessions.map(count -> getTranslation(
                count == 1 ? "diagnostics.sessions.one" : "diagnostics.sessions.many", count)));
        var data = new Paragraph();
        data.addClassName("diagnostics-data");
        data.bindText(generatedAt.map(at -> at == null ? getTranslation("diagnostics.data.unread")
                : getTranslation("diagnostics.data.measured", AT.format(at))));
        section.add(now, data);
        return section;
    }

    private Section tailsPanel(CspViolations violations, AssistantTail tail) {
        Section section = panel("tails", "diagnostics.tails.title");

        var csp = new Div();
        csp.addClassName("diagnostics-csp");
        var cspReceived = violations.received();
        Signal.effect(csp, () -> {
            long received = cspReceived.get();
            csp.removeAll();
            csp.add(new H3(getTranslation("diagnostics.csp.title", received)));
            List<CspViolations.Violation> kept = violations.newestFirst();
            if (kept.isEmpty()) {
                csp.add(note("diagnostics.csp.none"));
                return;
            }
            var table = new Table();
            table.addHeaderRow(getTranslation("diagnostics.tail.at"), getTranslation("diagnostics.csp.report"));
            for (CspViolations.Violation violation : kept.subList(0, Math.min(CSP_SHOWN, kept.size()))) {
                TableRow row = table.addRow();
                row.addDataCell(AT.format(violation.receivedAt()));
                row.addDataCell(violation.report()).addClassName("diagnostics-text");
            }
            csp.add(table);
        });

        var assistant = new Div();
        assistant.addClassName("diagnostics-assistant");
        var newest = tail.newestSignal();
        Signal.effect(assistant, () -> {
            newest.get();
            assistant.removeAll();
            assistant.add(new H3(getTranslation("diagnostics.assistant.title")));
            List<AssistantTail.Entry> entries = tail.entries().reversed();
            if (entries.isEmpty()) {
                assistant.add(note("diagnostics.assistant.none"));
                return;
            }
            var table = new Table();
            table.addHeaderRow(getTranslation("diagnostics.assistant.number"), getTranslation("diagnostics.tail.at"),
                    getTranslation("diagnostics.assistant.surface"), getTranslation("diagnostics.assistant.user"),
                    getTranslation("diagnostics.assistant.kind"), getTranslation("diagnostics.assistant.text"));
            for (AssistantTail.Entry entry : entries.subList(0, Math.min(ASSISTANT_SHOWN, entries.size()))) {
                TableRow row = table.addRow();
                row.getElement().setAttribute("data-kind", entry.kind().name().toLowerCase(Locale.ROOT));
                row.addDataCell(String.valueOf(entry.number()));
                row.addDataCell(AT.format(entry.at()));
                row.addDataCell(getTranslation("diagnostics.assistant.surface." + entry.surface()));
                row.addDataCell(entry.user());
                row.addDataCell(getTranslation("diagnostics.assistant.kind."
                        + entry.kind().name().toLowerCase(Locale.ROOT)));
                row.addDataCell(entry.text()).addClassName("diagnostics-text");
            }
            assistant.add(table);
        });

        section.add(csp, assistant);
        return section;
    }

    private Section clientPanel() {
        Section section = panel("client", "diagnostics.client.title");
        var body = new Div();
        section.add(body);
        Signal.effect(section, () -> {
            ExtendedClientDetails details = client.get();
            int[] resized = viewport.get();
            body.removeAll();
            if (details == null) {
                body.add(note("diagnostics.client.waiting"));
                return;
            }
            int width = resized.length == 2 ? resized[0] : details.getWindowInnerWidth();
            int height = resized.length == 2 ? resized[1] : details.getWindowInnerHeight();
            var table = new Table();
            table.addClassName("diagnostics-client-table");
            fact(table, "diagnostics.client.zone", details.getZoneId().getId());
            fact(table, "diagnostics.client.browserTime",
                    details.getBrowserDateTime().truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            fact(table, "diagnostics.client.pixelRatio", String.valueOf(details.getDevicePixelRatio()));
            fact(table, "diagnostics.client.viewport", getTranslation("diagnostics.client.size", String.valueOf(width), String.valueOf(height)));
            fact(table, "diagnostics.client.screen", getTranslation("diagnostics.client.size",
                    String.valueOf(details.getScreenWidth()), String.valueOf(details.getScreenHeight())));
            fact(table, "diagnostics.client.colorScheme", details.getColorScheme().getValue());
            fact(table, "diagnostics.client.touch",
                    getTranslation(details.isTouchDevice() ? "diagnostics.client.yes" : "diagnostics.client.no"));
            body.add(table);
        });
        return section;
    }

    private void fact(Table table, String key, String value) {
        TableRow row = table.addRow();
        row.getElement().setAttribute("data-fact", key.substring(key.lastIndexOf('.') + 1));
        row.addRowHeaderCell(getTranslation(key));
        row.addDataCell(value);
    }

    private Paragraph note(String key) {
        var paragraph = new Paragraph(getTranslation(key));
        paragraph.addClassName("diagnostics-note");
        return paragraph;
    }

    private String millis(double millis) {
        return getTranslation("diagnostics.millis", String.format(getLocale(), "%.1f", millis));
    }
}

package com.vaadin.securitystatus.ui.team.diagnostics;

import static com.vaadin.securitystatus.ui.common.Parts.css;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.dashboard.Dashboard;
import com.vaadin.flow.component.dashboard.DashboardWidget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.html.Table;
import com.vaadin.flow.component.html.TableRow;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.page.ExtendedClientDetails;
import com.vaadin.flow.component.page.WebStorage;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.securitystatus.data.DataRefreshSignal;
import com.vaadin.securitystatus.security.CspViolations;
import com.vaadin.securitystatus.security.Roles;
import com.vaadin.securitystatus.team.assistant.AssistantTail;
import com.vaadin.securitystatus.team.diagnostics.DiagnosticsCounters.HandlerCount;
import com.vaadin.securitystatus.team.diagnostics.DiagnosticsCounters.Minute;
import com.vaadin.securitystatus.team.diagnostics.DiagnosticsCounters.Navigations;
import com.vaadin.securitystatus.team.diagnostics.DiagnosticsCounters.Requests;
import com.vaadin.securitystatus.team.diagnostics.DiagnosticsCounters.RouteCount;
import com.vaadin.securitystatus.team.diagnostics.DiagnosticsCounters;
import com.vaadin.securitystatus.team.diagnostics.Outcome;
import com.vaadin.securitystatus.ui.team.InternalLayout;

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

    /** Where this browser keeps the order, the sizes and the choice of cards. */
    static final String LAYOUT_KEY = "security-status.diagnostics.cards";

    /** The cards of the board, each one panel, with the span it starts with. */
    enum Card {
        NAVIGATIONS("diagnostics.navigations.title", 2), REQUESTS("diagnostics.requests.title", 2),
        SESSIONS("diagnostics.sessions.title", 1), CLIENT("diagnostics.client.title", 1),
        POLICY("diagnostics.csp.name", 2), ASSISTANT("diagnostics.assistant.title", 2);

        static final List<Placed> DEFAULT = Arrays.stream(values()).map(card -> new Placed(card, card.colspan, 1))
                .toList();

        /** The card's name, its title too except where the title carries a count. */
        final String title;
        final int colspan;

        Card(String title, int colspan) {
            this.title = title;
            this.colspan = colspan;
        }
    }

    /** A card on the board, with its size, as the browser remembers it: {@code NAVIGATIONS:2:1;...}. */
    record Placed(Card card, int colspan, int rowspan) {

        static List<Placed> parse(@Nullable String saved) {
            var placed = new ArrayList<Placed>();
            if (saved == null || saved.isBlank()) {
                return placed;
            }
            for (String part : saved.split(";")) {
                String[] fields = part.split(":");
                try {
                    Card card = Card.valueOf(fields[0]);
                    if (placed.stream().noneMatch(known -> known.card() == card)) {
                        placed.add(new Placed(card, Math.max(1, Integer.parseInt(fields[1])),
                                Math.max(1, Integer.parseInt(fields[2]))));
                    }
                } catch (RuntimeException unreadable) {
                    // A card this release no longer has, or a value somebody edited: left out.
                }
            }
            return placed;
        }

        static String format(List<Placed> placed) {
            return placed.stream().map(one -> one.card().name() + ":" + one.colspan() + ":" + one.rowspan())
                    .collect(Collectors.joining(";"));
        }
    }

    /** A widget that knows which card it is. */
    static final class CardWidget extends DashboardWidget {
        final Card card;

        CardWidget(Card card, String title) {
            super(title);
            this.card = card;
            addClassName("diagnostics-card-" + card.name().toLowerCase(Locale.ROOT));
        }
    }

    private final Dashboard board = new Dashboard();
    private final Map<Card, Function<CardWidget, Section>> builders = new EnumMap<>(Card.class);
    private final ValueSignal<Boolean> editing = new ValueSignal<>(false);
    private final ValueSignal<List<Card>> absent = new ValueSignal<>(List.of());
    private final ValueSignal<@Nullable ExtendedClientDetails> client = new ValueSignal<>(null);
    private final ValueSignal<int[]> viewport = new ValueSignal<>(new int[0]);

    public DiagnosticsView(DiagnosticsCounters counters, CspViolations violations, AssistantTail tail,
            DataRefreshSignal refresh) {
        addClassName("diagnostics-view");

        var since = css(new Paragraph(), "diagnostics-since");
        // Read again whenever a request ends, so "minutes ago" moves with the page in use and needs no timer.
        var requests = counters.requests();
        Signal.effect(since, () -> {
            requests.get();
            since.setText(getTranslation("diagnostics.since", ago(counters.since(), Instant.now()),
                    AT.format(counters.since())));
        });

        builders.put(Card.NAVIGATIONS, widget -> navigationsPanel(counters.navigations()));
        builders.put(Card.REQUESTS, widget -> requestsPanel(counters.requests()));
        builders.put(Card.SESSIONS, widget -> sessionsPanel(counters.sessions(), refresh.generatedAt()));
        builders.put(Card.CLIENT, widget -> clientPanel());
        builders.put(Card.POLICY, widget -> policyPanel(violations, widget));
        builders.put(Card.ASSISTANT, widget -> assistantPanel(tail));

        board.addClassName("diagnostics-board");
        board.setMinimumColumnWidth("28rem");
        board.setMaximumColumnCount(4);
        board.setPadding("0");
        board.setI18n(boardI18n());
        board.addItemMovedListener(moved -> save());
        board.addItemResizedListener(resized -> save());
        board.addItemRemovedListener(removed -> save());
        show(Card.DEFAULT);

        var info = css(new Div(since, toolbar()), "diagnostics-info", "aura-surface");
        getContent().add(info, board);
        whenAttached(ui -> {
            WebStorage.getItem(ui, WebStorage.Storage.LOCAL_STORAGE, LAYOUT_KEY).thenAccept(saved -> {
                List<Placed> placed = Placed.parse(saved);
                if (!placed.isEmpty()) {
                    show(placed);
                }
            });
            return null;
        });

        UI.getCurrent().getPage().retrieveExtendedClientDetails(client::set);
        whenAttached(ui -> ui.getPage().addBrowserWindowResizeListener(
                resized -> viewport.set(new int[] { resized.getWidth(), resized.getHeight() })));
    }

    // The board

    private Component toolbar() {
        var edit = css(new Button(), "diagnostics-edit");
        edit.addThemeVariants(ButtonVariant.TERTIARY);
        Signal.effect(edit, () -> {
            String label = getTranslation(editing.get() ? "diagnostics.board.done" : "diagnostics.board.edit");
            edit.setIcon((editing.get() ? VaadinIcon.CHECK : VaadinIcon.EDIT).create());
            edit.setAriaLabel(label);
            edit.setTooltipText(label);
        });
        edit.addClickListener(click -> {
            editing.set(!editing.peek());
            board.setEditable(editing.peek());
        });

        var add = css(new MenuBar(), "diagnostics-add");
        add.addThemeVariants(MenuBarVariant.TERTIARY);
        MenuItem cards = add.addItem(VaadinIcon.PLUS.create());
        cards.setAriaLabel(getTranslation("diagnostics.board.add"));
        add.setTooltipText(cards, getTranslation("diagnostics.board.add"));
        add.bindVisible(absent.map(missing -> !missing.isEmpty()));
        Signal.effect(add, () -> {
            cards.getSubMenu().removeAll();
            for (Card card : absent.get()) {
                cards.getSubMenu().addItem(getTranslation(card.title), click -> {
                    board.add(widget(new Placed(card, card.colspan, 1)));
                    save();
                });
            }
        });

        var restore = new Button(VaadinIcon.ROTATE_LEFT.create(), click -> {
            show(Card.DEFAULT);
            save();
        });
        restore.addClassName("diagnostics-restore");
        restore.addThemeVariants(ButtonVariant.TERTIARY);
        restore.setAriaLabel(getTranslation("diagnostics.board.restore"));
        restore.setTooltipText(getTranslation("diagnostics.board.restore"));
        restore.bindVisible(editing);

        return css(new Div(edit, add, restore), "diagnostics-toolbar");
    }

    /** How long ago, in words: the largest whole unit, down to "less than a minute". */
    private String ago(Instant then, Instant now) {
        long minutes = Math.max(0, Duration.between(then, now).toMinutes());
        if (minutes < 1) {
            return getTranslation("diagnostics.ago.now");
        }
        if (minutes < 60) {
            return getTranslation("diagnostics.ago.minutes", minutes);
        }
        long hours = minutes / 60;
        return hours < 48 ? getTranslation("diagnostics.ago.hours", hours)
                : getTranslation("diagnostics.ago.days", hours / 24);
    }

    private Dashboard.DashboardI18n boardI18n() {
        return new Dashboard.DashboardI18n().setSelectWidget(getTranslation("diagnostics.board.select"))
                .setSelectSection(getTranslation("diagnostics.board.select"))
                .setRemove(getTranslation("diagnostics.board.remove"))
                .setResize(getTranslation("diagnostics.board.resize"))
                .setResizeApply(getTranslation("diagnostics.board.apply"))
                .setResizeShrinkWidth(getTranslation("diagnostics.board.narrower"))
                .setResizeGrowWidth(getTranslation("diagnostics.board.wider"))
                .setResizeShrinkHeight(getTranslation("diagnostics.board.shorter"))
                .setResizeGrowHeight(getTranslation("diagnostics.board.taller"))
                .setMove(getTranslation("diagnostics.board.move"))
                .setMoveApply(getTranslation("diagnostics.board.apply"))
                .setMoveForward(getTranslation("diagnostics.board.forward"))
                .setMoveBackward(getTranslation("diagnostics.board.backward"));
    }

    private void show(List<Placed> placed) {
        board.removeAll();
        placed.forEach(one -> board.add(widget(one)));
        refreshAbsent();
    }

    private CardWidget widget(Placed placed) {
        var widget = new CardWidget(placed.card(), getTranslation(placed.card().title));
        widget.setContent(builders.get(placed.card()).apply(widget));
        widget.setColspan(placed.colspan());
        widget.setRowspan(placed.rowspan());
        return widget;
    }

    private void save() {
        List<Placed> placed = board.getWidgets().stream().map(CardWidget.class::cast)
                .map(widget -> new Placed(widget.card, widget.getColspan(), widget.getRowspan())).toList();
        WebStorage.setItem(WebStorage.Storage.LOCAL_STORAGE, LAYOUT_KEY, Placed.format(placed));
        refreshAbsent();
    }

    private void refreshAbsent() {
        List<Card> shown = board.getWidgets().stream().map(widget -> ((CardWidget) widget).card).toList();
        absent.set(Arrays.stream(Card.values()).filter(card -> !shown.contains(card)).toList());
    }

    private Section panel(String name) {
        return css(new Section(), "diagnostics-panel", "diagnostics-" + name);
    }

    private Section navigationsPanel(Signal<Navigations> navigations) {
        Section section = panel("navigations");
        var body = new Div();
        section.add(body);
        Signal.effect(section, () -> {
            Navigations counted = navigations.get();
            body.removeAll();
            if (counted.routes().isEmpty()) {
                body.add(note("diagnostics.navigations.none"));
                return;
            }
            var table = css(new Table(), "diagnostics-navigations-table");
            table.addHeaderRow(getTranslation("diagnostics.navigations.route"),
                    getTranslation("diagnostics.outcome.completed"), getTranslation("diagnostics.outcome.refused"),
                    getTranslation("diagnostics.outcome.broke"),
                    getTranslation("diagnostics.outcome.notShown"), getTranslation("diagnostics.outcome.postponed"),
                    getTranslation("diagnostics.navigations.mean"), getTranslation("diagnostics.navigations.longest"));
            for (RouteCount route : counted.routes()) {
                row(table, DiagnosticsCounters.OTHER_ROUTE.equals(route.route())
                        ? getTranslation("diagnostics.navigations.other") : route.route(),
                        route.completed(), route.refused(), route.broke(), route.notShown(), route.postponed(),
                        millis(route.totalMillis() / (double) route.total()), millis(route.longestMillis()))
                        .getElement().setAttribute("data-route", route.route());
            }
            TableRow total = table.addRow();
            total.addClassName("diagnostics-total");
            total.addRowHeaderCell(getTranslation("diagnostics.navigations.total"));
            for (Outcome outcome : Outcome.values()) {
                total.addDataCell(String.valueOf(counted.total(outcome)));
            }
            total.addDataCell(millis(counted.totalMillis() / (double) counted.total()));
            total.addDataCell(millis(counted.longestMillis()));
            body.add(table);
        });
        return section;
    }

    private Section requestsPanel(Signal<Requests> requests) {
        Section section = panel("requests");
        var body = new Div();
        section.add(body, note("diagnostics.requests.kept"));
        Signal.effect(section, () -> {
            Requests counted = requests.get();
            body.removeAll();
            if (counted.count() == 0) {
                body.add(note("diagnostics.requests.none"));
                return;
            }
            var summary = css(new Paragraph(getTranslation("diagnostics.requests.summary", counted.count(),
                    counted.failed(), millis(counted.totalMicros() / 1000.0 / counted.count()),
                    millis(counted.longestMicros() / 1000.0))), "diagnostics-requests-summary");

            var minutes = css(new Table(), "diagnostics-minutes");
            minutes.addHeaderRow(getTranslation("diagnostics.requests.minute"),
                    getTranslation("diagnostics.requests.count"), getTranslation("diagnostics.requests.failed"),
                    getTranslation("diagnostics.requests.mean"), getTranslation("diagnostics.requests.longest"));
            List<Minute> newestFirst = counted.minutes().reversed();
            for (Minute minute : newestFirst.subList(0, Math.min(MINUTES_SHOWN, newestFirst.size()))) {
                row(minutes, MINUTE.format(minute.start()), minute.count(), minute.failed(),
                        millis(minute.totalMicros() / 1000.0 / minute.count()), millis(minute.longestMicros() / 1000.0));
            }

            var kinds = css(new Table(), "diagnostics-kinds");
            kinds.addHeaderRow(getTranslation("diagnostics.requests.handler"),
                    getTranslation("diagnostics.requests.count"));
            for (HandlerCount kind : counted.kinds()) {
                TableRow row = kinds.addRow();
                row.addDataCell(kind.handler().isEmpty() ? getTranslation("diagnostics.requests.noHandler")
                        : kind.handler());
                row.addDataCell(String.valueOf(kind.count()));
            }
            var tables = css(new Div(minutes, kinds), "diagnostics-request-tables");
            body.add(summary, tables);
        });
        return section;
    }

    private Section sessionsPanel(Signal<Long> sessions, Signal<@Nullable Instant> generatedAt) {
        Section section = panel("sessions");
        var now = css(new Paragraph(), "diagnostics-sessions-now");
        now.bindText(sessions.map(count -> getTranslation(
                count == 1 ? "diagnostics.sessions.one" : "diagnostics.sessions.many", count)));
        var data = css(new Paragraph(), "diagnostics-data");
        data.bindText(generatedAt.map(at -> at == null ? getTranslation("diagnostics.data.unread")
                : getTranslation("diagnostics.data.measured", AT.format(at))));
        section.add(now, note("diagnostics.sessions.signIn"), data);
        return section;
    }

    private Section policyPanel(CspViolations violations, CardWidget widget) {
        Section section = panel("csp");
        var cspReceived = violations.received();
        Signal.effect(section, () -> {
            long received = cspReceived.get();
            section.removeAll();
            widget.setTitle(getTranslation("diagnostics.csp.title", received));
            List<CspViolations.Violation> kept = violations.newestFirst();
            if (kept.isEmpty()) {
                section.add(note("diagnostics.csp.none"));
                return;
            }
            var table = new Table();
            table.addHeaderRow(getTranslation("diagnostics.tail.at"), getTranslation("diagnostics.csp.report"));
            for (CspViolations.Violation violation : kept.subList(0, Math.min(CSP_SHOWN, kept.size()))) {
                TableRow row = table.addRow();
                row.addDataCell(AT.format(violation.receivedAt()));
                row.addDataCell(violation.report()).addClassName("diagnostics-text");
            }
            section.add(table);
        });
        return section;
    }

    private Section assistantPanel(AssistantTail tail) {
        Section section = panel("assistant");
        var newest = tail.newestSignal();
        Signal.effect(section, () -> {
            newest.get();
            section.removeAll();
            List<AssistantTail.Entry> entries = tail.entries().reversed();
            if (entries.isEmpty()) {
                section.add(note("diagnostics.assistant.none"));
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
            section.add(table);
        });
        return section;
    }

    private Section clientPanel() {
        Section section = panel("client");
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
            var table = css(new Table(), "diagnostics-client-table");
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
        return css(new Paragraph(getTranslation(key)), "diagnostics-note");
    }

    private String millis(double millis) {
        return getTranslation("diagnostics.millis", String.format(getLocale(), "%.1f", millis));
    }

    /** A row of plain cells, each value written as text. */
    private static TableRow row(Table table, Object... cells) {
        TableRow row = table.addRow();
        for (Object cell : cells) {
            row.addDataCell(String.valueOf(cell));
        }
        return row;
    }
}

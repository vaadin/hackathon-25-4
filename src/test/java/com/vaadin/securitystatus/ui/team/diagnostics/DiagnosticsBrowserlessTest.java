package com.vaadin.securitystatus.ui.team.diagnostics;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.test.context.support.WithMockUser;

import com.vaadin.browserless.BrowserlessApplicationContext;
import com.vaadin.browserless.BrowserlessUIContext;
import com.vaadin.browserless.SecuredBrowserlessApplicationContext;
import com.vaadin.browserless.SpringBrowserlessApplicationContext;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dashboard.Dashboard;
import com.vaadin.flow.component.dashboard.DashboardWidget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.html.TableRow;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.router.AccessDeniedException;
import com.vaadin.flow.router.NavigationEndedEvent;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.securitystatus.Application;
import com.vaadin.securitystatus.security.CspViolations;
import com.vaadin.securitystatus.security.Roles;
import com.vaadin.securitystatus.team.assistant.AssistantTail;
import com.vaadin.securitystatus.team.diagnostics.DiagnosticsCounters.Navigations;
import com.vaadin.securitystatus.team.diagnostics.DiagnosticsCounters.RouteCount;
import com.vaadin.securitystatus.team.diagnostics.DiagnosticsCounters;
import com.vaadin.securitystatus.team.diagnostics.DiagnosticsListeners;
import com.vaadin.securitystatus.team.diagnostics.Outcome;
import com.vaadin.securitystatus.ui.customer.HomeView;
import com.vaadin.securitystatus.ui.customer.NotFoundView;
import com.vaadin.securitystatus.ui.team.explorer.SpringView;

/**
 * DIA-01 to DIA-03: the navigations counted per route and outcome from the
 * 25.4 navigation events, the numbers of one session moved by another without
 * a reload, and the assistant's exchange in the tail.
 *
 * <p>
 * The counters belong to the application and the Spring context is shared by
 * the test classes, so every assertion is on the difference a test makes, not
 * on absolute numbers.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@WithMockUser(username = "team", roles = Roles.SECURITY)
class DiagnosticsBrowserlessTest extends SpringBrowserlessTest {

    @Autowired
    DiagnosticsCounters counters;

    @Autowired
    AssistantTail tail;

    @Autowired
    CspViolations violations;

    // DIA-01 and AC1

    @Test
    void three_navigations_one_failing_are_counted_per_route_and_outcome() {
        RouteCount diagnosticsBefore = count("/diagnostics");
        RouteCount springBefore = count("/findings/spring");
        RouteCount missingBefore = count("/no-such-route");

        navigate(DiagnosticsView.class);
        navigate(SpringView.class);
        navigate("no-such-route", NotFoundView.class);

        RouteCount diagnostics = count("/diagnostics");
        RouteCount spring = count("/findings/spring");
        RouteCount missing = count("/no-such-route");
        assertThat(diagnostics.completed() - diagnosticsBefore.completed()).isOne();
        assertThat(spring.completed() - springBefore.completed()).isOne();
        assertThat(missing.refused() - missingBefore.refused()).isOne();
        assertThat(missing.broke()).isEqualTo(missingBefore.broke());
        assertThat(missing.completed()).isEqualTo(missingBefore.completed());
        assertThat(diagnostics.refused()).isEqualTo(diagnosticsBefore.refused());

        // The view shows the same counts in the row of each route.
        navigate(DiagnosticsView.class);
        assertThat(cells("/no-such-route")).startsWith("/no-such-route", "0",
                String.valueOf(missing.refused()), "0", "0", "0");
        assertThat(cells("/diagnostics").get(1)).isEqualTo(String.valueOf(diagnostics.completed() + 1));

        // The total row has its mean, weighted by navigations, and its longest, like every route.
        Navigations all = counters.navigations().peek();
        List<String> total = $(TableRow.class).withClassName("diagnostics-total").single().getElement().getChildren()
                .map(cell -> cell.getTextRecursively()).toList();
        assertThat(total).hasSize(8);
        assertThat(total.get(7)).isEqualTo(all.longestMillis() + ".0 ms");
        assertThat(all.longestMillis()).isEqualTo(all.routes().stream().mapToLong(RouteCount::longestMillis).max()
                .orElseThrow());
    }

    @Test
    void a_route_with_parameters_is_counted_under_its_template_not_the_address_asked() {
        long before = count("/findings/release/:line/:version").completed();

        UI.getCurrent().navigate("findings/release/24.9/24.9.21");
        UI.getCurrent().navigate("findings/release/25.3/25.3.5?q=hidden");

        assertThat(count("/findings/release/:line/:version").completed() - before).isEqualTo(2);
        assertThat(counters.navigations().peek().routes()).extracting(RouteCount::route)
                .noneMatch(route -> route.contains("24.9.21") || route.contains("hidden"));
    }

    @Test
    void every_outcome_of_the_enum_has_its_own_column() {
        // The switch in Outcome.of names every record of the sealed interface; this
        // is the runtime half of that check, which the compiler does for new ones.
        assertThat(NavigationEndedEvent.Outcome.class.isSealed()).isTrue();
        // Failed is counted in two columns, Refused and Broke, P-19.
        assertThat(NavigationEndedEvent.Outcome.class.getPermittedSubclasses()).hasSize(Outcome.values().length - 1);
        assertThat(Outcome.of(new NavigationEndedEvent.Failed(new NotFoundException()))).isEqualTo(Outcome.REFUSED);
        assertThat(Outcome.of(new NavigationEndedEvent.Failed(new AccessDeniedException()))).isEqualTo(Outcome.REFUSED);
        assertThat(Outcome.of(new NavigationEndedEvent.Failed(new IllegalStateException()))).isEqualTo(Outcome.BROKE);
        assertThat(Outcome.of(new NavigationEndedEvent.Postponed())).isEqualTo(Outcome.POSTPONED);
        assertThat(Outcome.of(new NavigationEndedEvent.NotShown())).isEqualTo(Outcome.NOT_SHOWN);

        navigate(DiagnosticsView.class);
        TableRow header = $(TableRow.class, panel("navigations")).first();
        assertThat(header.getElement().getTextRecursively()).contains("Completed", "Refused", "Broke", "Not shown",
                "Postponed");
    }

    // The edge case: a restart starts from zero, and the view says since when

    @Test
    void the_view_says_counting_since_the_start_and_that_nothing_is_stored() {
        navigate(DiagnosticsView.class);

        assertThat($(H1.class).single().getText()).isEqualTo("Diagnostics");
        assertThat(UI.getCurrent().getInternals().getTitle()).isEqualTo("Diagnostics");
        assertThat($(Paragraph.class).withClassName("diagnostics-since").single().getText())
                .startsWith("Counting started ").contains(" ago, at ").contains(" UTC, when the application started.")
                .endsWith("a restart sets every number back to zero.");
        assertThat($(DashboardWidget.class).all()).extracting(DashboardWidget::getTitle).containsExactly("Navigations",
                "Requests", "Sessions", "Your browser", "Policy violations, "
                        + violations.received().peek() + " reported since the start",
                "Assistant exchanges");
    }

    @Test
    void a_request_is_counted_by_minute_and_handler_kind_only() {
        long before = counters.requests().peek().count();
        counters.requestEnded("UidlRequestHandler", false, java.time.Duration.ofMillis(3));
        counters.requestEnded(DiagnosticsListeners.NO_HANDLER, true, Duration.ofMillis(9));

        var requests = counters.requests().peek();
        assertThat(requests.count() - before).isEqualTo(2);
        assertThat(requests.longestMicros()).isGreaterThanOrEqualTo(9000);
        assertThat(requests.minutes().getLast().failed()).isPositive();
        // A record holds a minute, numbers and a handler kind, nothing a request carries.
        assertThat(DiagnosticsCounters.Minute.class.getRecordComponents()).extracting(c -> c.getName())
                .containsExactly("start", "count", "failed", "totalMicros", "longestMicros");

        navigate(DiagnosticsView.class);
        assertThat($(Paragraph.class).withClassName("diagnostics-requests-summary").single().getText())
                .matches("\\d+ requests, \\d+ failed\\. Mean .+ ms, longest .+ ms\\.");
        assertThat($(TableRow.class, $(Div.class).withClassName("diagnostics-request-tables").single()).all())
                .extracting(row -> row.getElement().getTextRecursively())
                .anyMatch(text -> text.startsWith("No handler, such as a push message"));
    }

    @Test
    void the_policy_tail_shows_a_new_report_without_a_reload() {
        navigate(DiagnosticsView.class);

        violations.record("{\"csp-report\":{\"blocked-uri\":\"https://diagnostics.example\"}}");
        runPendingSignalsTasks();

        Section csp = panel("csp");
        // The card's title carries the count, P-19.
        assertThat(((DashboardWidget) csp.getParent().orElseThrow()).getTitle()).startsWith("Policy violations, ");
        assertThat($(TableRow.class, csp).all().get(1).getElement().getTextRecursively())
                .contains("https://diagnostics.example");
    }

    @Test
    void the_reader_sees_their_own_browser_through_the_typed_accessors() {
        navigate(DiagnosticsView.class);
        Section client = panel("client");

        List<String> facts = $(TableRow.class, client).all().stream()
                .map(row -> row.getElement().getAttribute("data-fact")).toList();
        if (facts.isEmpty()) {
            // The browserless window has not described itself; the view waits and says so.
            assertThat($(Paragraph.class, client).single().getText())
                    .isEqualTo("Waiting for your browser to describe itself.");
        } else {
            assertThat(facts).containsExactly("zone", "browserTime", "pixelRatio", "viewport", "screen",
                    "colorScheme", "touch");
        }
    }

    // DIA-03

    @Test
    void the_assistant_used_once_is_in_the_tail_prompt_tool_call_and_answer() {
        navigate(DiagnosticsView.class);
        long started = tail.newest();
        // The panel is on the right of the internal shell, shut until the Assistant button opens it, D-203.
        test($(Button.class).withClassName("assistant-toggle").single()).click();

        test($(MessageInput.class).withClassName("assistant-input").single()).send("Is 24.9.21 safe?");

        Section assistant = panel("assistant");
        List<String> rows = $(TableRow.class, assistant).all().stream().skip(1)
                .filter(row -> Long.parseLong(firstCell(row)) > started)
                .map(row -> row.getElement().getAttribute("data-kind") + " "
                        + row.getElement().getTextRecursively())
                .toList();
        assertThat(rows).hasSize(3);
        // Newest first: the answer, the tool call, the prompt.
        assertThat(rows.get(0)).startsWith("answer ").contains("Chat", "team", "Answer",
                "The measurements of 24.9.21 say **fix_available**");
        assertThat(rows.get(1)).startsWith("tool ").contains("Tool call", "versionStatus {\"version\":\"24.9.21\"}");
        assertThat(rows.get(2)).startsWith("prompt ").contains("Prompt", "Is 24.9.21 safe?");
    }

    // The refusal: diagnostics is for the team role only

    @Test
    @WithMockUser(username = "guest", roles = {})
    void a_guest_is_refused_and_the_refusal_is_counted_as_refused() {
        long before = count("/diagnostics").refused();

        navigate("diagnostics", HomeView.class);

        assertThat($(Section.class).withClassName("diagnostics-panel").exists()).isFalse();
        assertThat(count("/diagnostics").refused() - before).isOne();
    }

    // P-19: every panel is a card on a board, in the default order until the reader arranges it

    @Test
    void every_panel_is_a_card_on_the_board_in_the_default_order() {
        navigate(DiagnosticsView.class);

        Dashboard board = $(Dashboard.class).withClassName("diagnostics-board").single();
        assertThat(board.getWidgets()).extracting(widget -> ((DiagnosticsView.CardWidget) widget).card)
                .containsExactly(DiagnosticsView.Card.values());
        assertThat(board.getWidgets()).extracting(DashboardWidget::getTitle).startsWith("Navigations", "Requests",
                "Sessions", "Your browser");
        assertThat(board.isEditable()).isFalse();

        test($(Button.class).withClassName("diagnostics-edit").single()).click();
        assertThat(board.isEditable()).isTrue();
        assertThat($(Button.class).withClassName("diagnostics-edit").single().getAriaLabel()).contains("Done");
        // Every card is on the board, so there is nothing to add.
        assertThat($(MenuBar.class).withClassName("diagnostics-add").exists()).isFalse();
    }

    @Test
    void a_remembered_layout_is_read_back_and_anything_unreadable_is_left_out() {
        var placed = DiagnosticsView.Placed.parse("SESSIONS:1:2;GONE:1:1;NAVIGATIONS:x:1;CLIENT:3:1;SESSIONS:2:2");

        assertThat(placed).containsExactly(new DiagnosticsView.Placed(DiagnosticsView.Card.SESSIONS, 1, 2),
                new DiagnosticsView.Placed(DiagnosticsView.Card.CLIENT, 3, 1));
        assertThat(DiagnosticsView.Placed.format(placed)).isEqualTo("SESSIONS:1:2;CLIENT:3:1");
        assertThat(DiagnosticsView.Placed.parse(null)).isEmpty();
    }

    private RouteCount count(String route) {
        return counters.navigations().peek().routes().stream().filter(count -> count.route().equals(route))
                .findFirst().orElse(new RouteCount(route, 0, 0, 0, 0, 0, 0, 0));
    }

    private Section panel(String name) {
        return $(Section.class).withClassName("diagnostics-" + name).single();
    }

    private List<String> cells(String route) {
        Optional<TableRow> row = $(TableRow.class, panel("navigations")).all().stream()
                .filter(found -> route.equals(found.getElement().getAttribute("data-route"))).findFirst();
        assertThat(row).as("the row of %s", route).isPresent();
        return row.get().getElement().getChildren().map(cell -> cell.getTextRecursively()).toList();
    }

    private static String firstCell(Component row) {
        return row.getElement().getChild(0).getTextRecursively();
    }

    /**
     * DIA-02 and AC2: two signed in sessions, each in its own window, one
     * watching the diagnostics view while the other navigates.
     */
    @Nested
    class TwoSessions {

        @Autowired
        ApplicationContext spring;

        private BrowserlessApplicationContext app;

        @BeforeEach
        void open() {
            app = SpringBrowserlessApplicationContext.createSecured(spring, Application.class);
        }

        @AfterEach
        void close() {
            app.close();
        }

        @Test
        void a_navigation_in_session_two_moves_the_numbers_in_session_one_without_a_reload() {
            var secured = (SecuredBrowserlessApplicationContext<?>) app;
            BrowserlessUIContext one = secured.newUser("team", Roles.SECURITY).newWindow();
            BrowserlessUIContext two = secured.newUser("team", Roles.SECURITY).newWindow();
            assertThat(one.getUI().getSession()).isNotSameAs(two.getUI().getSession());

            DiagnosticsView watched = one.navigate(DiagnosticsView.class);
            String before = springCompleted(one);

            two.navigate(SpringView.class);
            one.runPendingSignalsTasks();

            assertThat(one.getCurrentView()).isSameAs(watched);
            assertThat(Long.parseLong(springCompleted(one))).isEqualTo(Long.parseLong(before) + 1);
        }

        private String springCompleted(BrowserlessUIContext window) {
            Section navigations = window.find(Section.class).withClassName("diagnostics-navigations").single();
            return window.find(TableRow.class, navigations).all().stream()
                    .filter(row -> "/findings/spring".equals(row.getElement().getAttribute("data-route")))
                    .findFirst().map(row -> row.getElement().getChild(1).getTextRecursively()).orElse("0");
        }
    }
}

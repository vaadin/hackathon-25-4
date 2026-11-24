package com.vaadin.securitystatus.ui.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.breadcrumbs.BreadcrumbsItem;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.router.RouteNotFoundError;
import com.vaadin.flow.router.RouteParent;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.securitystatus.data.DataRefreshSignal;
import com.vaadin.securitystatus.data.FixturePages;
import com.vaadin.securitystatus.ui.customer.CustomerLayout;
import com.vaadin.securitystatus.ui.customer.HomeView;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class ShellBrowserlessTest extends SpringBrowserlessTest {

    @Route(value = "shell-test-leaf", layout = CustomerLayout.class)
    @RouteParent(HomeView.class)
    @PageTitle("shell.breadcrumbs.more")
    @AnonymousAllowed
    public static class LeafView extends Div {
        public LeafView() {
            add(ViewHeading.ofAnswer());
        }
    }

    @Autowired
    DataRefreshSignal dataRefresh;

    @Autowired
    FixturePages fixtures;

    // FND-03
    @Test
    void home_breadcrumbs_name_the_leaf_and_the_document_title_matches() {
        navigate(HomeView.class);

        List<BreadcrumbsItem> trail = $(BreadcrumbsItem.class).all();
        assertThat(trail).extracting(BreadcrumbsItem::getText).containsExactly("Vaadin", "My account",
                "Security status");
        assertThat(trail.get(0).getPath()).isEqualTo(AppBar.VAADIN_HOME);
        assertThat(trail.get(1).getPath()).isNotNull();
        assertThat(trail.get(2).getPath()).isNull();
        assertThat($(H1.class).single().getText()).isEqualTo("Security status");
        assertThat($(Paragraph.class).withClassName("customer-purpose").single().getText())
                .startsWith("Find out whether your Vaadin version is safe");
        assertThat(Tooltip.forComponent($(Badge.class).withClassName("measured-chip").single()).getText())
                .isEqualTo("These are the real measurements of that date, not sample data");
        assertThat(documentTitle()).isEqualTo("Security status");
    }

    // FND-03
    @Test
    void nested_route_links_its_ancestors_and_names_the_leaf() {
        RouteConfiguration.forSessionScope().setAnnotatedRoute(LeafView.class);

        navigate(LeafView.class);

        List<BreadcrumbsItem> trail = $(BreadcrumbsItem.class).all();
        assertThat(trail).extracting(BreadcrumbsItem::getText).containsExactly("Vaadin", "My account",
                "Security status", "More");
        assertThat(trail.get(2).getPath()).isNotNull();
        assertThat(trail.get(3).getPath()).isNull();
        // The shell draws the page's one H1 on every customer route; the view's title is its answer's H2.
        assertThat($(H1.class).single().getText()).isEqualTo("Security status");
        assertThat($(H2.class).withClassName("answer-heading").single().getText()).isEqualTo("More");
        assertThat(documentTitle()).isEqualTo("More");
    }

    // The prototype's distribution: one column that opens every customer page with the H1, the purpose and the lookup.
    @Test
    void every_customer_route_opens_with_the_heading_the_purpose_and_the_lookup_above_its_content() {
        RouteConfiguration.forSessionScope().setAnnotatedRoute(LeafView.class);

        for (Class<? extends com.vaadin.flow.component.Component> route : List.of(HomeView.class, LeafView.class)) {
            navigate(route);
            Main column = $(Main.class).withClassName("customer-column").single();
            assertThat(column.getChildren().map(child -> child.getElement().getAttribute("class")))
                    .containsExactly("view-heading", "customer-purpose", "lookup-mount", "customer-content");
            assertThat($(CustomerLayout.class).single().getLookupMount().getChildren().count()).isEqualTo(1);
        }
    }

    @Test
    void unknown_route_keeps_the_app_bar_and_leads_home() {
        navigate("nothing-here", RouteNotFoundError.class);

        List<BreadcrumbsItem> trail = $(BreadcrumbsItem.class).all();
        assertThat(trail).extracting(BreadcrumbsItem::getText).containsExactly("Vaadin", "My account",
                "Security status");
        assertThat(trail.get(1).getPath()).isNotNull();
    }

    @Test
    void measured_chip_is_omitted_until_a_measurement_is_known() {
        fixtures.coldAndDown();
        try {
            navigate(HomeView.class);
            assertThat($(Badge.class).withClassName("measured-chip").exists()).isFalse();

            dataRefresh.publish(Instant.parse("2026-11-19T03:15:00Z"));
            runPendingSignalsTasks();

            assertThat($(Badge.class).withClassName("measured-chip").single().getText()).isEqualTo("Measured 2026-11-19");
        } finally {
            fixtures.restore();
        }
    }

    @Test
    void measured_chip_shows_the_generated_at_of_the_index_that_was_read() {
        navigate(HomeView.class);

        assertThat($(Badge.class).withClassName("measured-chip").single().getText()).isEqualTo("Measured 2026-11-21");
    }

    private static String documentTitle() {
        return UI.getCurrent().getInternals().getTitle();
    }
}

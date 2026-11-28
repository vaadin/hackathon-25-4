package com.vaadin.securitystatus.ui.customer;

import static com.vaadin.securitystatus.ui.common.Parts.css;

import java.util.Optional;

import org.springframework.beans.factory.ObjectProvider;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.html.Aside;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.router.RouterLayout;
import jakarta.annotation.security.PermitAll;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.securitystatus.data.DataRefreshSignal;
import com.vaadin.securitystatus.ui.common.AppBar;
import com.vaadin.securitystatus.ui.common.TeamEntrance;

/**
 * The shell of the public routes: the app bar, then one centered column that
 * opens every page the same way, the H1, the purpose and the lookup, above
 * the route's own content. No navigation menu. The sign in page is not
 * drawn in it, D-276.
 */
@Layout
@PermitAll
public final class CustomerLayout extends Composite<Div> implements RouterLayout {

    private final Div lookupMount = new Div();
    private final Div content = new Div();

    CustomerLayout(DataRefreshSignal dataRefresh, AuthenticationContext authentication,
            ObjectProvider<LookupFactory> lookup, ObjectProvider<TeamEntrance> team) {
        addClassName("customer-shell");

        var heading = css(new H1(getTranslation("shell.customer.title")), "view-heading");
        var purpose = css(new Paragraph(getTranslation("shell.customer.purpose")), "customer-purpose");
        lookupMount.addClassName("lookup-mount");
        lookup.ifAvailable(provider -> lookupMount.add(provider.create()));
        content.addClassName("customer-content");

        var column = css(new Main(heading, purpose, lookupMount, content), "customer-column");
        // A team member reads the customer pages with the team's navigation beside them, D-253,
        // when the team's part of the application is there to give it, D-254.
        Optional<TeamEntrance> entrance = team.stream().filter(TeamEntrance::admits).findFirst();
        var bar = AppBar.customer(dataRefresh, authentication, entrance.map(TeamEntrance::barLink));
        if (entrance.isPresent()) {
            var side = css(new Aside(entrance.get().navigation()), "customer-team-nav");
            var body = css(new Div(side, column), "customer-team-body");
            getContent().add(bar, body);
        } else {
            getContent().add(bar, column);
        }
    }

    /** Where the lookup is mounted, once per window. */
    public Div getLookupMount() {
        return lookupMount;
    }

    @Override
    public void showRouterLayoutContent(HasElement routeContent) {
        content.getElement().removeAllChildren();
        if (routeContent != null) {
            content.getElement().appendChild(routeContent.getElement());
        }
    }

    @Override
    public void removeRouterLayoutContent(HasElement oldContent) {
        content.getElement().removeAllChildren();
    }
}

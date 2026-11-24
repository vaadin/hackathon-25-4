package com.vaadin.securitystatus.ui.customer;

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
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.securitystatus.data.DataRefreshSignal;
import com.vaadin.securitystatus.ui.common.AppBar;
import com.vaadin.securitystatus.ui.common.ShellLookup;
import com.vaadin.securitystatus.ui.common.StandsAlone;
import com.vaadin.securitystatus.ui.common.TeamEntrance;

/**
 * The shell of the public routes: the app bar, then one centered column that
 * opens every page the same way, the H1, the purpose and the lookup, above
 * the route's own content, except a route that {@link StandsAlone}. No
 * navigation menu.
 */
@Layout
@AnonymousAllowed
public final class CustomerLayout extends Composite<Div> implements RouterLayout {

    private final Div lookupMount = new Div();
    private final Div content = new Div();
    private final ValueSignal<Boolean> asking = new ValueSignal<>(true);

    CustomerLayout(DataRefreshSignal dataRefresh, AuthenticationContext authentication,
            ObjectProvider<ShellLookup> lookup, ObjectProvider<TeamEntrance> team) {
        addClassName("customer-shell");

        var heading = new H1(getTranslation("shell.customer.title"));
        heading.addClassName("view-heading");
        var purpose = new Paragraph(getTranslation("shell.customer.purpose"));
        purpose.addClassName("customer-purpose");
        lookupMount.addClassName("lookup-mount");
        lookup.ifAvailable(provider -> lookupMount.add(provider.create()));
        content.addClassName("customer-content");
        heading.bindVisible(asking);
        purpose.bindVisible(asking);
        lookupMount.bindVisible(asking);

        var column = new Main(heading, purpose, lookupMount, content);
        column.addClassName("customer-column");
        // A team member reads the customer pages with the team's navigation beside them, D-253,
        // when the team's part of the application is there to give it, D-254.
        Optional<TeamEntrance> entrance = team.stream().filter(TeamEntrance::admits).findFirst();
        var bar = AppBar.customer(dataRefresh, authentication, entrance.map(TeamEntrance::barLink));
        if (entrance.isPresent()) {
            var side = new Aside(entrance.get().navigation());
            side.addClassName("customer-team-nav");
            var body = new Div(side, column);
            body.addClassName("customer-team-body");
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
        asking.set(!(routeContent instanceof StandsAlone));
        if (routeContent != null) {
            content.getElement().appendChild(routeContent.getElement());
        }
    }

    @Override
    public void removeRouterLayoutContent(HasElement oldContent) {
        content.getElement().removeAllChildren();
    }
}

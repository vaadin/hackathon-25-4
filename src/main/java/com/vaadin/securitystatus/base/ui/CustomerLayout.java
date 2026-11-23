package com.vaadin.securitystatus.base.ui;

import org.springframework.beans.factory.ObjectProvider;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.HasElement;
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
            ObjectProvider<ShellLookup> lookup) {
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
        getContent().add(AppBar.customer(dataRefresh, authentication), column);
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

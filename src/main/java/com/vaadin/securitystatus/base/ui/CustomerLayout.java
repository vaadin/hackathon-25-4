package com.vaadin.securitystatus.base.ui;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.securitystatus.data.DataRefreshSignal;

/**
 * The shell of the public routes: the app bar with the lookup mount, and no
 * navigation menu.
 */
@Layout
@AnonymousAllowed
public final class CustomerLayout extends Composite<Div> implements RouterLayout {

    private final Div lookupMount = new Div();
    private final Main main = new Main();

    CustomerLayout(DataRefreshSignal dataRefresh, AuthenticationContext authentication) {
        addClassName("customer-shell");
        lookupMount.addClassName("lookup-mount");
        var appBar = new AppBar(dataRefresh, authentication);
        appBar.addBeforeSignOut(lookupMount);
        getContent().add(appBar, main);
    }

    /**
     * Where the lookup component is mounted, once, by the feature that owns it.
     */
    public Div getLookupMount() {
        return lookupMount;
    }

    @Override
    public void showRouterLayoutContent(HasElement content) {
        main.getElement().removeAllChildren();
        if (content != null) {
            main.getElement().appendChild(content.getElement());
        }
    }

    @Override
    public void removeRouterLayoutContent(HasElement oldContent) {
        main.getElement().removeAllChildren();
    }
}

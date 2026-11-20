package com.vaadin.securitystatus.base.ui;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.server.menu.MenuConfiguration;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.securitystatus.base.security.Roles;
import com.vaadin.securitystatus.data.DataRefreshSignal;

import jakarta.annotation.security.RolesAllowed;

/**
 * The shell of the internal routes: the shared app bar plus a drawer with the
 * internal destinations and the assistant mount. The drawer lists the routes
 * that declare a {@code @Menu}, whose title is a bundle key.
 */
@RolesAllowed(Roles.SECURITY)
public final class InternalLayout extends AppLayout {

    private final Div assistantMount = new Div();

    InternalLayout(DataRefreshSignal dataRefresh, AuthenticationContext authentication) {
        addClassName("internal-shell");
        setPrimarySection(Section.NAVBAR);

        var toggle = new DrawerToggle();
        toggle.setAriaLabel(getTranslation("shell.drawer.toggle"));

        var appBar = new AppBar(dataRefresh, authentication);
        addToNavbar(toggle, appBar);

        var nav = new SideNav();
        nav.setLabel(getTranslation("shell.nav.label"));
        MenuConfiguration.getMenuEntries().forEach(
                entry -> nav.addItem(new SideNavItem(getTranslation(entry.title()), entry.path())));

        assistantMount.addClassName("assistant-mount");
        addToDrawer(nav, assistantMount);
    }

    /**
     * Where the assistant panel is mounted by the feature that owns it.
     */
    public Div getAssistantMount() {
        return assistantMount;
    }
}

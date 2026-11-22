package com.vaadin.securitystatus.base.ui;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.upload.UploadButton;
import com.vaadin.flow.component.upload.UploadFileList;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.server.menu.MenuConfiguration;
import org.springframework.beans.factory.ObjectProvider;
import com.vaadin.securitystatus.base.ai.AiSurfaces;
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
// The assistant's panel and the workbench's paste box arrive through a bean,
// which the production bundle's scanner does not follow, D-79 and D-149.
@Uses(MessageList.class)
@Uses(MessageInput.class)
@Uses(UploadButton.class)
@Uses(UploadFileList.class)
// The form AI controller's field marker is no component, so its module is named
// as the controller's internal FormFieldMarker names it, at the platform's version.
@NpmPackage(value = "@vaadin/field-highlighter", version = "25.4.0-beta1")
@JsModule("@vaadin/field-highlighter/src/vaadin-ai-field-marker.js")
public final class InternalLayout extends AppLayout {

    private final Div assistantMount = new Div();

    InternalLayout(DataRefreshSignal dataRefresh, AuthenticationContext authentication,
            ObjectProvider<AiSurfaces> ai) {
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
        ai.ifAvailable(surfaces -> assistantMount.add(surfaces.chatPanel()));
        addToDrawer(nav, assistantMount);
    }

    /**
     * Where the assistant panel is mounted by the feature that owns it.
     */
    public Div getAssistantMount() {
        return assistantMount;
    }
}

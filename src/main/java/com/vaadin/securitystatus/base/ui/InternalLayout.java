package com.vaadin.securitystatus.base.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.html.Aside;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Nav;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.upload.UploadButton;
import com.vaadin.flow.component.upload.UploadFileList;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.menu.MenuConfiguration;
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.securitystatus.base.ai.AiSurfaces;
import com.vaadin.securitystatus.base.security.Roles;
import com.vaadin.securitystatus.data.DataRefreshSignal;

import org.springframework.beans.factory.ObjectProvider;

import jakarta.annotation.security.RolesAllowed;

/**
 * The shell of the internal routes, in the prototype's distribution: a left
 * panel that holds what the route gives it, the explorer's filter and
 * products, and a bar over the content with the drawer toggle and the page's
 * H1, D-203.
 *
 * <p>
 * What the prototype does not have stays, placed where it disturbs least: the
 * four destinations of {@code @Menu} are a row of links in the bar after the
 * heading, the trail, the measured chip, the dark switch and Sign out sit at
 * the bar's end, and the assistant is a panel on the right that the Assistant
 * button opens.
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
    private final Aside assistantPanel = new Aside();
    private final Div routeHolder = new Div();
    private final Div drawer = new Div();
    private final DrawerToggle toggle = new DrawerToggle();
    private final ValueSignal<Boolean> assistantOpen = new ValueSignal<>(false);
    private boolean closedByShell;

    InternalLayout(DataRefreshSignal dataRefresh, AuthenticationContext authentication,
            ObjectProvider<AiSurfaces> ai) {
        addClassName("internal-shell");
        setPrimarySection(Section.DRAWER);

        toggle.setAriaLabel(getTranslation("shell.drawer.toggle"));
        var heading = new ViewHeading();

        var nav = new Nav();
        nav.addClassName("internal-nav");
        nav.getElement().setAttribute("aria-label", getTranslation("shell.nav.label"));
        MenuConfiguration.getMenuEntries().forEach(entry -> {
            var link = new RouterLink(getTranslation(entry.title()), entry.menuClass());
            link.addClassName("internal-nav-link");
            nav.add(link);
        });

        var appBar = new AppBar(dataRefresh, authentication);
        addToNavbar(toggle, heading, nav, appBar);

        drawer.addClassName("internal-drawer");
        addToDrawer(drawer);

        routeHolder.addClassName("internal-route");
        var frame = new Div(routeHolder);
        frame.addClassName("internal-frame");
        setContent(frame);

        ai.ifAvailable(surfaces -> {
            assistantMount.addClassName("assistant-mount");
            assistantMount.add(surfaces.chatPanel());
            var close = new Button(getTranslation("shell.assistant.close"), click -> assistantOpen.set(false));
            close.addThemeVariants(ButtonVariant.TERTIARY);
            close.addClassName("assistant-close");
            assistantPanel.addClassName("assistant-aside");
            assistantPanel.getElement().setAttribute("aria-label", getTranslation("assistant.title"));
            assistantPanel.add(close, assistantMount);
            assistantPanel.bindVisible(assistantOpen);
            frame.add(assistantPanel);

            var open = new Button(getTranslation("shell.assistant.open"),
                    click -> assistantOpen.set(!assistantOpen.peek()));
            open.addThemeVariants(ButtonVariant.TERTIARY);
            open.addClassName("assistant-toggle");
            open.getElement().setAttribute("aria-expanded", "false");
            com.vaadin.flow.signals.Signal.effect(open, () -> open.getElement().setAttribute("aria-expanded",
                    String.valueOf(assistantOpen.get())));
            appBar.addBeforeEnd(open);
        });
    }

    @Override
    public void showRouterLayoutContent(HasElement content) {
        routeHolder.getElement().removeAllChildren();
        if (content != null) {
            routeHolder.getElement().appendChild(content.getElement());
        }
        drawer.removeAll();
        if (content instanceof DrawerContent giving) {
            Component panel = giving.drawerContent();
            drawer.add(panel);
            toggle.setVisible(true);
            removeClassName("no-drawer");
            if (closedByShell && !isOverlay()) {
                // Shut by a route without a panel, so opened again; on a narrow window the
                // panel overlays and stays shut until the toggle, as the prototype's does.
                setDrawerOpened(true);
            }
            closedByShell = false;
        } else {
            toggle.setVisible(false);
            addClassName("no-drawer");
            closedByShell = isDrawerOpened() || closedByShell;
            setDrawerOpened(false);
        }
    }

    @Override
    public void removeRouterLayoutContent(HasElement oldContent) {
        if (oldContent != null) {
            oldContent.getElement().removeFromParent();
        }
    }

    /**
     * Where the assistant panel is mounted by the feature that owns it.
     */
    public Div getAssistantMount() {
        return assistantMount;
    }

    /** Opens or shuts the assistant panel on the right, for the tests and the bar's button. */
    public void showAssistant(boolean open) {
        assistantOpen.set(open);
    }
}

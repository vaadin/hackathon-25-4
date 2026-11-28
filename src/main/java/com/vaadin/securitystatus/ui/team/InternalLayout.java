package com.vaadin.securitystatus.ui.team;

import static com.vaadin.securitystatus.ui.common.Parts.css;

import org.springframework.beans.factory.ObjectProvider;

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
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.upload.UploadButton;
import com.vaadin.flow.component.upload.UploadFileList;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.securitystatus.data.DataRefreshSignal;
import com.vaadin.securitystatus.security.Roles;
import com.vaadin.securitystatus.ui.common.AiSurfaces;
import com.vaadin.securitystatus.ui.common.AppBar;
import com.vaadin.securitystatus.ui.common.ViewHeading;

import jakarta.annotation.security.RolesAllowed;

/**
 * The shell of the internal routes: a left panel with the destinations, the
 * customer's page first, and under them what the route gives it, the
 * explorer's filter and products, D-237 and D-245; a bar over the content with
 * the drawer toggle, the page's H1, the trail, the measured chip, the scheme
 * choice and Sign out; and the assistant as a panel on the right that the
 * Assistant button opens.
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
    private final Div routePanel = new Div();
    private final DrawerToggle toggle = new DrawerToggle();
    private final ValueSignal<Boolean> assistantOpen = new ValueSignal<>(false);

    InternalLayout(DataRefreshSignal dataRefresh, AuthenticationContext authentication,
            ObjectProvider<AiSurfaces> ai) {
        addClassName("internal-shell");
        setPrimarySection(Section.DRAWER);

        toggle.setAriaLabel(getTranslation("shell.drawer.toggle"));
        var heading = new ViewHeading();

        var nav = new TeamNavigation();

        var appBar = new AppBar(dataRefresh, authentication);
        addToNavbar(toggle, heading, appBar);

        routePanel.addClassName("internal-route-panel");
        drawer.addClassName("internal-drawer");
        var brand = css(new Div(AppBar.icon(), new Span(getTranslation("shell.brand"))), "internal-brand");
        drawer.add(brand, nav, routePanel);
        addToDrawer(drawer);

        routeHolder.addClassName("internal-route");
        var frame = css(new Div(routeHolder), "internal-frame");
        setContent(frame);

        ai.stream().filter(AiSurfaces::offered).findFirst().ifPresent(surfaces -> {
            assistantMount.addClassName("assistant-mount");
            assistantMount.add(surfaces.chatPanel());
            var close = new Button(VaadinIcon.CLOSE.create(), click -> assistantOpen.set(false));
            close.setAriaLabel(getTranslation("shell.assistant.close"));
            close.setTooltipText(getTranslation("shell.assistant.close"));
            close.addThemeVariants(ButtonVariant.TERTIARY);
            close.addClassName("assistant-close");
            assistantPanel.addClassName("assistant-aside");
            assistantPanel.getElement().setAttribute("aria-label", getTranslation("assistant.title"));
            assistantPanel.add(close, assistantMount);
            assistantPanel.bindVisible(assistantOpen);
            frame.add(assistantPanel);

            var open = new Button(VaadinIcon.CHAT.create(), click -> assistantOpen.set(!assistantOpen.peek()));
            open.addThemeVariants(ButtonVariant.TERTIARY);
            open.setAriaLabel(getTranslation("shell.assistant.open"));
            open.setTooltipText(getTranslation("shell.assistant.open"));
            open.addClassName("assistant-toggle");
            open.getElement().setAttribute("aria-expanded", "false");
            Signal.effect(open, () -> open.getElement().setAttribute("aria-expanded",
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
        routePanel.removeAll();
        if (content instanceof DrawerContent giving) {
            Component panel = giving.drawerContent();
            routePanel.add(panel);
        }
        routePanel.setVisible(routePanel.getComponentCount() > 0);
    }

    @Override
    public void removeRouterLayoutContent(HasElement oldContent) {
        if (oldContent != null) {
            oldContent.getElement().removeFromParent();
        }
    }
}

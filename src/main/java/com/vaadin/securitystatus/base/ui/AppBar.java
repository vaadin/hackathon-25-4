package com.vaadin.securitystatus.base.ui;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.breadcrumbs.Breadcrumbs;
import com.vaadin.flow.component.breadcrumbs.BreadcrumbsItem;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.NavigationState;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouteReference;
import com.vaadin.flow.router.RouterState;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.securitystatus.data.DataRefreshSignal;

/**
 * The app bar both shells share: the breadcrumb trail, the measured chip, the dark
 * mode switch and,
 * for a signed in session, the way out.
 */
public final class AppBar extends Composite<Header> {

    static final String VAADIN_HOME = "https://vaadin.com";

    private static final DateTimeFormatter MEASURED_DATE = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC);

    private final Breadcrumbs breadcrumbs = new Breadcrumbs(Breadcrumbs.Mode.MANUAL);

    AppBar(DataRefreshSignal dataRefresh, AuthenticationContext authentication) {
        addClassName("app-bar");

        breadcrumbs.setAriaLabel(getTranslation("shell.breadcrumbs.label"));
        breadcrumbs.setI18n(new Breadcrumbs.BreadcrumbsI18n().setMoreItems(getTranslation("shell.breadcrumbs.more")));
        var routerState = UI.getCurrent().routerStateSignal();
        Signal.effect(this, () -> showTrail(routerState.get()));

        var generatedAt = dataRefresh.generatedAt();
        var measured = new Badge(generatedAt.map(
                at -> at == null ? "" : getTranslation("shell.measured", MEASURED_DATE.format(at))));
        measured.addClassName("measured-chip");
        measured.bindVisible(generatedAt.map(Objects::nonNull));

        getContent().add(breadcrumbs, measured, new ThemeSwitch());
        if (authentication.isAuthenticated()) {
            var signOut = new Button(getTranslation("shell.signOut"), click -> authentication.logout());
            signOut.addThemeVariants(ButtonVariant.TERTIARY);
            signOut.addClassName("sign-out");
            getContent().add(signOut);
        }
    }

    private static boolean isRegistered(Class<? extends Component> target) {
        return RouteConfiguration.forSessionScope().isRouteRegistered(target);
    }

    private void showTrail(RouterState state) {
        breadcrumbs.removeAll();
        breadcrumbs.add(new BreadcrumbsItem(getTranslation("shell.crumb.vaadin"), VAADIN_HOME));
        if (state.navigationTarget() == null) {
            return;
        }
        List<RouteReference> hierarchy = RouteConfiguration.forSessionScope()
                .getRouteHierarchy(state.navigationTarget(), state.routeParameters());
        var router = VaadinService.getCurrent().getRouter();
        if (hierarchy.stream().noneMatch(entry -> isRegistered(entry.navigationTarget()))) {
            // An error view has no place in the hierarchy, so the trail leads home.
            router.resolveNavigationTarget(new Location("")).map(NavigationState::getNavigationTarget)
                    .ifPresent(home -> router.resolvePageTitle(home, RouteParameters.empty())
                            .filter(text -> !text.isBlank())
                            .ifPresent(title -> breadcrumbs.add(new BreadcrumbsItem(title, home))));
            return;
        }
        for (int i = 0; i < hierarchy.size(); i++) {
            RouteReference entry = hierarchy.get(i);
            var title = router.resolvePageTitle(entry.navigationTarget(), entry.routeParameters())
                    .filter(text -> !text.isBlank());
            if (title.isEmpty()) {
                continue;
            }
            boolean leaf = i == hierarchy.size() - 1;
            breadcrumbs.add(leaf ? new BreadcrumbsItem(title.get())
                    : new BreadcrumbsItem(title.get(), entry.navigationTarget(), entry.routeParameters()));
        }
    }
}

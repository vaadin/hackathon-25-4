package com.vaadin.securitystatus.base.ui;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.router.RouterState;
import com.vaadin.flow.server.VaadinService;

/**
 * The heading of a view. It reads the same resolved page title the document
 * title is written from, so the two cannot drift.
 */
public final class ViewHeading extends Composite<H1> {

    public ViewHeading() {
        addClassName("view-heading");
        getContent().bindText(UI.getCurrent().routerStateSignal().map(ViewHeading::titleOf));
    }

    private static String titleOf(RouterState state) {
        if (state.navigationTarget() == null) {
            return "";
        }
        return VaadinService.getCurrent().getRouter()
                .resolvePageTitle(state.navigationTarget(), state.routeParameters()).orElse("");
    }
}

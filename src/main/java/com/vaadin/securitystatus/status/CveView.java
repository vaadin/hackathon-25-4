package com.vaadin.securitystatus.status;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParent;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.securitystatus.base.ui.ViewHeading;

/** The answer by advisory. Epic 05 builds it; for now the heading names the ids asked. */
@Route("cve/:ids/:version?")
@RouteParent(HomeView.class)
@PageTitle("cve.title")
@AnonymousAllowed
public final class CveView extends Composite<Div> {

    public static final String IDS = "ids";

    public static final String VERSION = "version";

    public CveView() {
        getContent().add(new ViewHeading());
    }
}

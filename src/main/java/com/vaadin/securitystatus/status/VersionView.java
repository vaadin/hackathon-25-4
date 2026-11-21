package com.vaadin.securitystatus.status;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParent;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.securitystatus.base.ui.ViewHeading;

/** The answer by version. Epic 04 builds it; for now the heading names the version asked. */
@Route("version/:version")
@RouteParent(HomeView.class)
@PageTitle("version.title")
@AnonymousAllowed
public final class VersionView extends Composite<Div> {

    public static final String VERSION = "version";

    public VersionView() {
        getContent().add(new ViewHeading());
    }
}

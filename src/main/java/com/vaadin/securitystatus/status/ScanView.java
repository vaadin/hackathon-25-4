package com.vaadin.securitystatus.status;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParent;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.securitystatus.base.ui.ViewHeading;

/** The scanner report. Epic 06 builds it; for now only the heading. */
@Route("scan/:version?")
@RouteParent(HomeView.class)
@PageTitle("scan.title")
@AnonymousAllowed
public final class ScanView extends Composite<Div> {

    public static final String VERSION = "version";

    public ScanView() {
        getContent().add(new ViewHeading());
    }
}

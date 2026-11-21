package com.vaadin.securitystatus.status;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParent;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.securitystatus.base.ui.ViewHeading;

/** Every advisory. Epic 07 builds it; for now only the heading. */
@Route("advisories")
@RouteParent(HomeView.class)
@PageTitle("advisories.title")
@AnonymousAllowed
public final class AdvisoriesView extends Composite<Div> {

    public AdvisoriesView() {
        getContent().add(new ViewHeading());
    }
}

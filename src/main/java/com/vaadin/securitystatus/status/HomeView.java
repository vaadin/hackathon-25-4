package com.vaadin.securitystatus.status;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.securitystatus.base.ui.ViewHeading;

@Route("")
@PageTitle("home.title")
@AnonymousAllowed
public final class HomeView extends Composite<Div> {

    public HomeView() {
        addClassName("home-view");
        getContent().add(new ViewHeading());
    }
}

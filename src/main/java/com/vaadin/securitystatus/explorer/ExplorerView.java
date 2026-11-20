package com.vaadin.securitystatus.explorer;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.securitystatus.base.security.Roles;
import com.vaadin.securitystatus.base.ui.InternalLayout;
import com.vaadin.securitystatus.base.ui.ViewHeading;

import jakarta.annotation.security.RolesAllowed;

@Route(value = "findings", layout = InternalLayout.class)
@PageTitle("explorer.title")
@Menu(order = 0, title = "explorer.title")
@RolesAllowed(Roles.SECURITY)
public final class ExplorerView extends Composite<Div> {

    public ExplorerView() {
        addClassName("explorer-view");
        getContent().add(new ViewHeading());
    }
}

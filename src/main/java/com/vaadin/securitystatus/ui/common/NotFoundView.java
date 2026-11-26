package com.vaadin.securitystatus.ui.common;

import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.RouteNotFoundError;
import com.vaadin.flow.server.HttpStatusCode;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.securitystatus.ui.customer.CustomerLayout;

/**
 * Flow's own not found view, kept inside the customer shell. Access control
 * refuses a route the reader may not open as not found, so an address that
 * names a real route lands on the home page instead, which says no more about
 * the route than the not found page did.
 */
@ParentLayout(CustomerLayout.class)
@AnonymousAllowed
public class NotFoundView extends RouteNotFoundError {

    @Override
    public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<NotFoundException> parameter) {
        if (VaadinService.getCurrent().getRouter().resolveNavigationTarget(event.getLocation()).isPresent()) {
            event.forwardTo("");
            return HttpStatusCode.OK.getCode();
        }
        return super.setErrorParameter(event, parameter);
    }
}

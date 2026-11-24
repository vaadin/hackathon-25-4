package com.vaadin.securitystatus.ui.common;

import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.RouteNotFoundError;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.securitystatus.ui.customer.CustomerLayout;

/**
 * Flow's own not found view, kept inside the customer shell.
 */
@ParentLayout(CustomerLayout.class)
@AnonymousAllowed
public class NotFoundView extends RouteNotFoundError {
}

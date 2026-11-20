package com.vaadin.securitystatus.base.ui;

import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.RouteNotFoundError;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * Flow's own not found view, kept inside the customer shell.
 */
@ParentLayout(CustomerLayout.class)
@AnonymousAllowed
public class NotFoundView extends RouteNotFoundError {
}

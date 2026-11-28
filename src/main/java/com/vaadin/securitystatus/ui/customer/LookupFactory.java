package com.vaadin.securitystatus.ui.customer;

import com.vaadin.flow.component.Component;

/**
 * What the customer shell mounts under its app bar: the lookup. The feature
 * that owns the lookup provides it as a bean, so the shell names no feature.
 */
public interface LookupFactory {

    /** A new lookup for one shell, which keeps it for the life of the window. */
    Component create();
}

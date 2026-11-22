package com.vaadin.securitystatus.base.ui;

import com.vaadin.flow.component.Component;

/**
 * A route of the internal shell that has something for the left panel. The
 * explorer puts its filter and its products there, as the prototype does; a
 * route that does not implement this gets no left panel and no toggle.
 */
public interface DrawerContent {

    /** What the left panel holds while this route is shown. */
    Component drawerContent();
}

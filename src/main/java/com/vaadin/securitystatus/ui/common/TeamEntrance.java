package com.vaadin.securitystatus.ui.common;

import com.vaadin.flow.component.Component;

/**
 * How the customer pages let a team member reach the team's pages, when the
 * team's part of the application is there at all. The customer side never
 * names the team's classes: it asks for this bean and draws what it gives,
 * and an application without the team has no such bean, D-254.
 */
public interface TeamEntrance {

    /** Whether the reader is a team member, and so gets what follows. */
    boolean admits();

    /** The team's navigation, drawn beside the customer column. */
    Component navigation();

    /** A control for the customer bar that leads to the team's pages. */
    Component barLink();
}

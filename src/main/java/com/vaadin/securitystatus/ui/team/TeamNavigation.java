package com.vaadin.securitystatus.ui.team;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;

/**
 * The team's destinations, in the order the team works, D-245 and D-252: the
 * measurements, what the customer sees of them, the verdicts written from
 * them, the catalogue, and the application's own health. The internal shell
 * and, for a team member, the customer shell draw the same one, D-253.
 */
public final class TeamNavigation extends Composite<SideNav> {

    public TeamNavigation() {
        SideNav nav = getContent();
        nav.addClassName("internal-nav");
        nav.getElement().setAttribute("aria-label", getTranslation("shell.nav.label"));
        var findings = item("shell.nav.findings", "findings");
        findings.addItem(item("shell.nav.spring", "findings/spring"));
        findings.setExpanded(true);
        nav.addItem(findings, item("shell.nav.customer", ""), item("shell.nav.verdicts", "findings/workbench"),
                item("shell.nav.advisories", "findings/advisories"), item("shell.nav.diagnostics", "diagnostics"));
    }

    private SideNavItem item(String key, String path) {
        var item = new SideNavItem(getTranslation(key), path);
        item.addClassName("internal-nav-link");
        return item;
    }
}

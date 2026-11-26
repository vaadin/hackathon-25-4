package com.vaadin.securitystatus.ui.team;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.icon.VaadinIcon;
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
        var findings = item("shell.nav.findings", "findings", VaadinIcon.BUG);
        findings.addItem(item("shell.nav.spring", "findings/spring", VaadinIcon.CUBE));
        // Closed until its toggle opens it, and closed again by every navigation outside the
        // Spring Boot page. The item opens itself whenever it is the current page, on the same
        // window events, so it is closed in the browser right after it did. The listener lives in
        // the browser with the element, and an earlier attach's is replaced, so nothing to release.
        whenAttached(ui -> {
            findings.getElement().executeJs("""
                const types = ['popstate', 'vaadin-navigated', 'side-nav-location-changed'];
                if (this.__closeOnNavigation) {
                    types.forEach(type => window.removeEventListener(type, this.__closeOnNavigation));
                }
                this.__closeOnNavigation = () => setTimeout(() => {
                    if (!this.querySelector('vaadin-side-nav-item')?.current) this.expanded = false;
                });
                types.forEach(type => window.addEventListener(type, this.__closeOnNavigation));
                this.__closeOnNavigation();""");
            return () -> {
            };
        });
        nav.addItem(findings, item("shell.nav.customer", "", VaadinIcon.EYE),
                item("shell.nav.verdicts", "findings/workbench", VaadinIcon.GAVEL),
                item("shell.nav.advisories", "findings/advisories", VaadinIcon.SHIELD),
                item("shell.nav.diagnostics", "diagnostics", VaadinIcon.DASHBOARD));
    }

    private SideNavItem item(String key, String path, VaadinIcon icon) {
        var item = new SideNavItem(getTranslation(key), path, icon.create());
        item.addClassName("internal-nav-link");
        return item;
    }
}

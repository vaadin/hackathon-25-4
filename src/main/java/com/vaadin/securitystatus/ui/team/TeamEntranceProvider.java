package com.vaadin.securitystatus.ui.team;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.securitystatus.security.Roles;
import com.vaadin.securitystatus.ui.common.TeamEntrance;

/** The team's way in from the customer pages: its navigation and the shield in the bar, D-246, D-253. */
@SpringComponent
class TeamEntranceProvider implements TeamEntrance {

    private final AuthenticationContext authentication;

    TeamEntranceProvider(AuthenticationContext authentication) {
        this.authentication = authentication;
    }

    @Override
    public boolean admits() {
        return authentication.hasRole(Roles.SECURITY);
    }

    @Override
    public Component navigation() {
        return new TeamNavigation();
    }

    @Override
    public Component barLink() {
        var team = new Anchor("findings", VaadinIcon.SHIELD.create());
        team.getElement().setAttribute("router-link", true);
        team.getElement().setAttribute("aria-label", team.getTranslation("shell.teamPages"));
        Tooltip.forComponent(team).setText(team.getTranslation("shell.teamPages"));
        team.addClassName("team-pages");
        return team;
    }
}

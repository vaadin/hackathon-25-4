package com.vaadin.securitystatus.ui.team;

import java.util.List;

import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.startup.ApplicationRouteRegistry;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.securitystatus.ui.customer.AdvisoriesView;
import com.vaadin.securitystatus.ui.customer.CveView;

/**
 * The customer screens the team also reads inside its own shell, keeping its
 * side panel: the advisories list and the answer by advisory, D-237, D-246.
 * Registered from here, so the customer views carry no trace of the team,
 * D-254.
 */
@SpringComponent
class TeamRoutes implements VaadinServiceInitListener {

    @Override
    public void serviceInit(ServiceInitEvent event) {
        var routes = RouteConfiguration
                .forRegistry(ApplicationRouteRegistry.getInstance(event.getSource().getContext()));
        routes.setRoute("findings/advisories", AdvisoriesView.class, List.of(InternalLayout.class));
        routes.setRoute("findings/cve/:ids/:version?", CveView.class, List.of(InternalLayout.class));
    }
}

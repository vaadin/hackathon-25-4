package com.vaadin.securitystatus.status;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.signals.Signal;
import com.vaadin.securitystatus.base.ui.DataOutageNotice;
import com.vaadin.securitystatus.base.ui.ViewHeading;
import com.vaadin.securitystatus.data.DataRefreshSignal;
import com.vaadin.securitystatus.data.DataUnreachableException;
import com.vaadin.securitystatus.data.SecurityDataService;
import com.vaadin.securitystatus.data.model.CasesIndex;

/**
 * The front door. For now one card per followed line with its newest measured
 * release, redrawn whenever a new measurement is published. Epic 03 gives the
 * cards their state, their action line and their link.
 */
@Route("")
@PageTitle("home.title")
@AnonymousAllowed
public final class HomeView extends Composite<Div> {

    private final SecurityDataService data;
    private final Div cards = new Div();

    public HomeView(SecurityDataService data, DataRefreshSignal refresh) {
        this.data = data;
        addClassName("home-view");
        cards.addClassName("line-cards");
        getContent().add(new ViewHeading(), cards);

        var generatedAt = refresh.generatedAt();
        Signal.effect(this, () -> {
            generatedAt.get();
            showLines();
        });
    }

    private void showLines() {
        cards.removeAll();
        CasesIndex index;
        try {
            index = data.index();
        } catch (DataUnreachableException unreachable) {
            cards.add(new DataOutageNotice());
            return;
        }
        for (CasesIndex.Line line : index.lines()) {
            if (line.isPrerelease()) {
                continue;
            }
            var minor = new Span(line.line());
            minor.addClassName("line-card-minor");
            var newest = new Span(line.version().toString());
            newest.addClassName("line-card-version");
            var card = new Div(minor, newest);
            card.addClassName("line-card");
            cards.add(card);
        }
    }
}

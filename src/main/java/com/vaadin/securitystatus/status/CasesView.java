package com.vaadin.securitystatus.status;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParent;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.signals.Signal;
import com.vaadin.securitystatus.base.ui.DataOutageNotice;
import com.vaadin.securitystatus.base.ui.ViewHeading;
import com.vaadin.securitystatus.data.DataRefreshSignal;
import com.vaadin.securitystatus.data.DataUnreachableException;
import com.vaadin.securitystatus.data.SecurityDataService;
import com.vaadin.securitystatus.data.model.CaseCatalog;
import com.vaadin.securitystatus.data.model.CasesIndex;

/**
 * Every state the customer screens can show, for demos and reviews, in the
 * source page's three groups. Unlisted: no menu entry and no link anywhere.
 * {@code /cases/<id>} goes straight to the live example of one case, so a
 * demo script can name a state rather than a release that happens to show it
 * this week.
 */
@Route("cases/:id?")
@RouteParent(HomeView.class)
@PageTitle("cases.title")
@AnonymousAllowed
public final class CasesView extends Composite<Div> implements BeforeEnterObserver {

    public static final String ID = "id";

    private final SecurityDataService data;
    private final Div body = new Div();

    public CasesView(SecurityDataService data, DataRefreshSignal refresh) {
        this.data = data;
        addClassName("cases-view");
        body.addClassName("cases-body");
        getContent().add(new ViewHeading(), body);

        var generatedAt = refresh.generatedAt();
        Signal.effect(this, () -> {
            generatedAt.get();
            draw();
        });
    }

    /** A case named in the address opens its example; one with no live example shows the catalogue. */
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Optional<String> asked = event.getRouteParameters().get(ID);
        if (asked.isEmpty()) {
            return;
        }
        try {
            CasesIndex index = data.index();
            data.cases().cases().stream().filter(item -> item.id().equals(asked.get())).findFirst()
                    .map(item -> CaseRules.place(item, index)).flatMap(CaseRules.Placed::target)
                    .ifPresentOrElse(
                            target -> event.forwardTo(target.view(), target.parameters(), target.query()),
                            () -> event.forwardTo(CasesView.class));
        } catch (DataUnreachableException unreachable) {
            event.forwardTo(CasesView.class);
        }
    }

    private void draw() {
        body.removeAll();
        Map<CaseRules.Group, List<CaseRules.Placed>> groups;
        try {
            groups = CaseRules.grouped(data.cases(), data.index());
        } catch (DataUnreachableException unreachable) {
            body.add(new DataOutageNotice());
            return;
        }
        var lead = new Paragraph(getTranslation("cases.lead"));
        lead.addClassName("section-lead");
        body.add(lead);
        for (CaseRules.Group group : CaseRules.Group.values()) {
            body.add(group(group, groups.get(group)));
        }
    }

    private Component group(CaseRules.Group group, List<CaseRules.Placed> cases) {
        String key = "cases.group." + group.name();
        var heading = new H2(getTranslation(key, cases.size()));
        var section = new Section(heading);
        section.addClassNames("cases-group", "cases-" + group.name().toLowerCase(java.util.Locale.ROOT));
        if (cases.isEmpty()) {
            var empty = new Paragraph(getTranslation("cases.group.empty"));
            empty.addClassName("cases-empty");
            section.add(empty);
            return section;
        }
        var rows = new Div();
        rows.addClassName("cases-rows");
        for (CaseRules.Placed placed : cases) {
            rows.add(row(placed));
        }
        section.add(rows);
        return section;
    }

    private Component row(CaseRules.Placed placed) {
        CaseCatalog.Case item = placed.item();
        var row = new Div();
        row.addClassName("case-row");
        row.getElement().setAttribute("data-case", item.id());

        var title = new Span(item.title().orElse(item.id()));
        title.addClassName("case-title");
        row.add(title);
        item.screen().ifPresent(screen -> {
            var where = new Span(screen);
            where.addClassName("case-screen");
            row.add(where);
        });
        if (placed.group() == CaseRules.Group.RENDERABLE && item.count() != null) {
            var count = new Span(getTranslation("cases.count", item.count()));
            count.addClassName("case-count");
            row.add(count);
        }
        placed.target().ifPresent(target -> {
            var link = new RouterLink(getTranslation("cases.seeOne"), target.view(), target.parameters());
            link.setQueryParameters(target.query());
            link.addClassName("case-link");
            row.add(link);
        });
        item.needs().ifPresent(needs -> {
            var says = new Span(needs);
            says.addClassName("case-needs");
            row.add(says);
        });
        return row;
    }
}

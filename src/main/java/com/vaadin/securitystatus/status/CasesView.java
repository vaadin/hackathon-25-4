package com.vaadin.securitystatus.status;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
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
        getContent().add(body);

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
        CaseCatalog catalog;
        try {
            catalog = data.cases();
            groups = CaseRules.grouped(catalog, data.index());
        } catch (DataUnreachableException unreachable) {
            body.add(heading(new H2(getTranslation("cases.heading")), Optional.empty()), new DataOutageNotice());
            return;
        }
        // The prototype's heading, aside and tip as the answer's H2; the title stays the trail's leaf, D-195.
        body.add(heading(new H2(getTranslation("cases.heading")), catalog.generatedAt()
                .map(at -> getTranslation("cases.aside", at.atZone(java.time.ZoneOffset.UTC).toLocalDate().toString()))));
        var tip = new Paragraph(getTranslation("cases.tip"));
        tip.addClassName("section-tip");
        body.add(tip);
        for (CaseRules.Group group : CaseRules.Group.values()) {
            body.add(group(group, groups.get(group)));
        }
    }

    private Component group(CaseRules.Group group, List<CaseRules.Placed> cases) {
        String key = "cases.group." + group.name();
        // The aside is a count for two groups and a sentence for the third; passed as text, not grouped.
        var section = new Section(heading(new H3(getTranslation(key)),
                Optional.of(getTranslation(key + ".aside", String.valueOf(cases.size())))));
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

    private static Component heading(com.vaadin.flow.component.HtmlContainer title, Optional<String> aside) {
        var row = new Div(title);
        row.addClassName("section-row");
        aside.ifPresent(text -> {
            var span = new Span(text);
            span.addClassName("section-aside");
            row.add(span);
        });
        return row;
    }

    private Component row(CaseRules.Placed placed) {
        CaseCatalog.Case item = placed.item();
        var row = new Div();
        row.addClassNames("case-row", "case-" + placed.group().name().toLowerCase(java.util.Locale.ROOT));
        row.getElement().setAttribute("data-case", item.id());

        // The count first, as the prototype sets it, without grouping. None for a case that cannot be
        // drawn, and 0 under none today only when the catalogue counts 0, rule 4, D-195.
        Optional<Integer> count = Optional.ofNullable(item.count()).filter(n -> switch (placed.group()) {
            case RENDERABLE -> true;
            case NONE_TODAY -> n == 0;
            case IMPOSSIBLE -> false;
        });
        count.ifPresent(n -> {
            var number = new Span(String.valueOf(n));
            number.addClassName("case-count");
            row.add(number);
        });

        var what = new Div();
        what.addClassName("case-what");
        var title = new Span(item.title().orElse(item.id()));
        title.addClassName("case-title");
        what.add(title);
        item.screen().ifPresent(screen -> {
            var where = new Span(screen);
            where.addClassName("case-screen");
            what.add(where);
        });
        item.needs().ifPresent(needs -> {
            var says = new Span(getTranslation("cases.needs", needs));
            says.addClassName("case-needs");
            what.add(says);
        });
        row.add(what);

        if (placed.group() == CaseRules.Group.RENDERABLE) {
            placed.target().ifPresent(target -> {
                var link = new RouterLink(getTranslation("cases.seeOne"), target.view(), target.parameters());
                link.setQueryParameters(target.query());
                link.addClassName("case-link");
                row.add(link);
            });
        } else {
            var chip = new Badge(getTranslation("cases.chip." + placed.group().name()));
            chip.addClassName("case-chip");
            if (placed.group() == CaseRules.Group.IMPOSSIBLE) {
                chip.addThemeVariants(BadgeVariant.ERROR);
            }
            row.add(chip);
        }
        return row;
    }
}

package com.vaadin.securitystatus.ui.customer;

import static com.vaadin.securitystatus.ui.common.Parts.css;

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
import jakarta.annotation.security.PermitAll;
import com.vaadin.flow.signals.Signal;
import com.vaadin.securitystatus.data.DataRefreshSignal;
import com.vaadin.securitystatus.data.DataUnreachableException;
import com.vaadin.securitystatus.data.SecurityDataService;
import com.vaadin.securitystatus.data.model.CaseCatalog;
import com.vaadin.securitystatus.data.model.CasesIndex;
import com.vaadin.securitystatus.ui.common.DataOutageNotice;

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
@PermitAll
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
        var tip = css(new Paragraph(getTranslation("cases.tip")), "section-tip");
        body.add(tip);
        for (CaseRules.Group group : CaseRules.Group.values()) {
            body.add(group(group, groups.get(group)));
        }
    }

    private Component group(CaseRules.Group group, List<CaseRules.Placed> cases) {
        String key = "cases.group." + group.name();
        // The aside is a count for two groups and a sentence for the third; passed as text, not grouped.
        var section = css(new Section(heading(new H3(getTranslation(key)),
                Optional.of(getTranslation(key + ".aside", String.valueOf(cases.size()))))), "cases-group", "cases-" + group.name().toLowerCase(java.util.Locale.ROOT));
        if (cases.isEmpty()) {
            var empty = css(new Paragraph(getTranslation("cases.group.empty")), "cases-empty");
            section.add(empty);
            return section;
        }
        var rows = css(new Div(), "cases-rows");
        for (CaseRules.Placed placed : cases) {
            rows.add(row(placed));
        }
        section.add(rows);
        return section;
    }

    private static Component heading(com.vaadin.flow.component.HtmlContainer title, Optional<String> aside) {
        var row = css(new Div(title), "section-row");
        aside.ifPresent(text -> {
            var span = css(new Span(text), "section-aside");
            row.add(span);
        });
        return row;
    }

    private Component row(CaseRules.Placed placed) {
        CaseCatalog.Case item = placed.item();
        var row = css(new Div(), "case-row", "case-" + placed.group().name().toLowerCase(java.util.Locale.ROOT));
        row.getElement().setAttribute("data-case", item.id());

        // The count first, as the prototype sets it, without grouping. None for a case that cannot be
        // drawn, and 0 under none today only when the catalogue counts 0, rule 4, D-195.
        Optional<Integer> count = Optional.ofNullable(item.count()).filter(n -> switch (placed.group()) {
            case RENDERABLE -> true;
            case NONE_TODAY -> n == 0;
            case IMPOSSIBLE -> false;
        });
        count.ifPresent(n -> {
            var number = css(new Span(String.valueOf(n)), "case-count");
            row.add(number);
        });

        var what = css(new Div(), "case-what");
        var title = css(new Span(item.title().orElse(item.id())), "case-title");
        what.add(title);
        item.screen().ifPresent(screen -> {
            var where = css(new Span(screen), "case-screen");
            what.add(where);
        });
        item.needs().ifPresent(needs -> {
            var says = css(new Span(getTranslation("cases.needs", needs)), "case-needs");
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
            var chip = css(new Badge(getTranslation("cases.chip." + placed.group().name())), "case-chip");
            if (placed.group() == CaseRules.Group.IMPOSSIBLE) {
                chip.addThemeVariants(BadgeVariant.ERROR);
            }
            row.add(chip);
        }
        return row;
    }
}

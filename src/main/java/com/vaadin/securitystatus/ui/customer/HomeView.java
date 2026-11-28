package com.vaadin.securitystatus.ui.customer;

import static com.vaadin.securitystatus.ui.common.Parts.css;
import static com.vaadin.securitystatus.ui.common.Parts.decorative;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
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
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLink;
import jakarta.annotation.security.PermitAll;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.securitystatus.customer.LookupState;
import com.vaadin.securitystatus.data.DataRefreshSignal;
import com.vaadin.securitystatus.data.DataUnreachableException;
import com.vaadin.securitystatus.data.SecurityDataService;
import com.vaadin.securitystatus.data.VersionNumber;
import com.vaadin.securitystatus.data.model.Assessment;
import com.vaadin.securitystatus.data.model.CasesIndex;
import com.vaadin.securitystatus.ui.common.DataOutageNotice;
import com.vaadin.securitystatus.ui.common.nav.Address;
import com.vaadin.securitystatus.ui.common.nav.QueryFlag;
import com.vaadin.securitystatus.customer.VersionRules;

/**
 * The front door: every followed line at a glance, the advisories reviewed
 * most recently, and the pointer for findings Spring Boot manages. Redrawn
 * whenever a new measurement is published.
 */
@Route("")
// The lookup is on every customer route, mounted by the shell through the LookupFactory
// bean, so no route's bytecode names it and the production bundle scanner never finds
// its components. Named here, on the one route whose components load eagerly.
@Uses(Lookup.class)
@PageTitle("home.title")
@PermitAll
public final class HomeView extends Composite<Div> implements BeforeEnterObserver {




    private final SecurityDataService data;
    private final ValueSignal<Boolean> prereleases = new ValueSignal<>(false);
    private final Div body = new Div();
    private final Div linesHolder = new Div();
    private final Div recentHolder = new Div();
    private final Div pointerHolder = new Div();

    public HomeView(SecurityDataService data, DataRefreshSignal refresh, LookupState lookup) {
        this.data = data;
        addClassName("home-view");
        body.addClassName("home-body");
        // The customer shell draws the page's H1, the purpose and the lookup above this.
        getContent().add(body);
        // The three parts under By version only; the other tabs show nothing under the lookup until
        // they have an answer, as the prototype does, D-273.
        for (Div part : List.of(linesHolder, recentHolder, pointerHolder)) {
            part.addClassName("home-part");
            part.bindVisible(lookup.tab().map(LookupState.Tab.VERSION::equals));
        }

        var generatedAt = refresh.generatedAt();
        Signal.effect(this, () -> {
            generatedAt.get();
            show(prereleases.get());
        });
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        prereleases.set(Address.has(event.getLocation().getQueryParameters(), QueryFlag.PRE));
    }

    private void show(boolean withPrereleases) {
        body.removeAll();
        linesHolder.removeAll();
        recentHolder.removeAll();
        pointerHolder.removeAll();
        CasesIndex index;
        try {
            index = data.index();
        } catch (DataUnreachableException unreachable) {
            body.add(new DataOutageNotice());
            return;
        }
        // The lines, the recently reviewed advisories and the Spring Boot pointer, D-273.
        linesHolder.add(linesSection(index, withPrereleases));
        recentHolder.add(recentSection(index));
        pointerHolder.add(pointer());
        body.add(linesHolder, recentHolder, pointerHolder);
    }

    // The lines

    private Component linesSection(CasesIndex index, boolean withPrereleases) {
        var aside = css(new Span(getTranslation("home.lines.lead")), "section-aside");
        var heading = css(new Div(new H2(getTranslation("home.lines.title")), aside), "section-heading");
        var cards = css(new Div(), "line-cards");
        for (CasesIndex.Line line : VersionRules.shownLines(index, withPrereleases)) {
            cards.add(card(index, line));
        }
        var tip = css(new Paragraph(getTranslation("home.lines.tip")), "home-tip");
        return css(new Section(heading, cards, tip), "home-lines");
    }

    private Component card(CasesIndex index, CasesIndex.Line line) {
        VersionNumber spoken = VersionRules.spokenFor(line);
        Optional<CasesIndex.Summary> summary = index.summaryOf(spoken);
        Optional<String> state = summary.flatMap(CasesIndex.Summary::state);

        var card = css(new RouterLink(VersionView.class, new RouteParameters(VersionView.VERSION, spoken.toString())), "line-card");
        state.ifPresent(word -> card.addClassName("state-" + word));

        var minor = css(new Span(line.line()), "line-card-minor");
        var top = css(new Div(minor), "line-card-top");
        tierBadge(line).ifPresent(top::add);
        summary.flatMap(row -> stateIcon(row)).ifPresent(top::add);
        card.add(top);

        summary.flatMap(this::stateWord).ifPresent(word -> card.add(css(new Span(word), "line-card-state")));

        var action = css(new Span(actionLine(index, line, summary, spoken)), "line-card-action");
        card.add(action);

        summary.flatMap(CasesIndex.Summary::scannedAt).or(line::scannedAt).map(OffsetDateTime::toLocalDate)
                .ifPresent(day -> {
                    var scanned = css(new Span(getTranslation("home.card.scanned", day.toString())), "line-card-scanned");
                    card.add(scanned);
                });
        return card;
    }

    /** What Vaadin says about the line, on the card: a mark for extended maintenance, its sentence on hover. */
    private Optional<Component> tierBadge(CasesIndex.Line line) {
        return line.support().filter(support -> support.tier().filter("extended"::equals).isPresent())
                .map(support -> {
                    var badge = css(new Badge(getTranslation("home.card.extended")), "line-card-tier");
                    extendedSentence(support).ifPresent(text -> Tooltip.forComponent(badge).setText(text));
                    return badge;
                });
    }

    /**
     * The hover text of the Extended mark: the prototype's sentence when the
     * line publishes its label, its major and the day free maintenance ended,
     * else the label alone.
     */
    private Optional<String> extendedSentence(CasesIndex.Support support) {
        return support.label().map(label -> support.major()
                .flatMap(major -> support.freeEnded()
                        .map(ended -> getTranslation("home.card.extended.title", label, major, ended)))
                .orElse(label));
    }

    private static boolean notAssessedOnly(CasesIndex.Summary row) {
        return count(row.triage()) > 0 && count(row.risk()) == 0;
    }

    private static int count(Integer value) {
        return value == null ? 0 : value;
    }

    private Optional<String> stateWord(CasesIndex.Summary row) {
        return row.state().map(state -> {
            if ("secure".equals(state)) {
                return getTranslation("state.secure");
            }
            if (notAssessedOnly(row)) {
                return getTranslation("state.notAssessed");
            }
            if ("fix_available".equals(state)) {
                return getTranslation("state.fixAvailable");
            }
            int open = count(row.risk()) + count(row.triage());
            return getTranslation(open == 1 ? "state.open.one" : "state.open.many", open);
        });
    }

    private static Optional<Component> stateIcon(CasesIndex.Summary row) {
        return row.state().map(state -> {
            // The prototype's marks: a plain tick for secure, a filled circle with a bar for a finding open.
            VaadinIcon shape = "secure".equals(state) ? VaadinIcon.CHECK
                    : notAssessedOnly(row) ? VaadinIcon.CLOCK
                            : "fix_available".equals(state) ? VaadinIcon.ARROW_CIRCLE_UP
                                    : VaadinIcon.EXCLAMATION_CIRCLE;
            Icon icon = decorative(shape.create(), "line-card-icon");
            return icon;
        });
    }

    /** Upgrade to X when the index names one, else On X or later, else Newest is X. */
    private String actionLine(CasesIndex index, CasesIndex.Line line, Optional<CasesIndex.Summary> summary,
            VersionNumber spoken) {
        Optional<VersionNumber> upgrade = summary.flatMap(CasesIndex.Summary::upgradeTo);
        if (upgrade.isPresent()) {
            return getTranslation("home.card.upgrade", upgrade.get().toString());
        }
        return VersionRules.secureFrom(index, line.line())
                .map(from -> getTranslation("home.card.secureFrom", from.toString()))
                .orElseGet(() -> getTranslation("home.card.newest", spoken.toString()));
    }

    // Recently reviewed

    private Component recentSection(CasesIndex index) {
        var heading = css(new Div(new H2(getTranslation("home.recent.title"))), "section-heading");
        index.counts().map(CasesIndex.Counts::cves).ifPresent(total -> {
            var all = css(new RouterLink(getTranslation("home.recent.all", total), AdvisoriesView.class), "recent-all");
            heading.add(all);
        });
        var rows = css(new Div(), "recent-rows");
        for (Assessment verdict : VersionRules.recent(index)) {
            rows.add(recentRow(index, verdict));
        }
        return css(new Section(heading, rows), "home-recent");
    }

    private Component recentRow(CasesIndex index, Assessment verdict) {
        Optional<String> cve = verdict.cves().stream().findFirst();
        var id = css(new Span(cve.or(verdict::id).orElse("")), "recent-id");
        var parts = new ArrayList<Component>();
        parts.add(id);
        VersionRules.firstSentence(verdict).ifPresent(says -> parts.add(css(new Span(says), "recent-says")));
        String applicability = verdict.applicability().orElse("in_triage");
        Optional.ofNullable(index.labels().get(applicability)).ifPresent(words -> {
            var chip = css(new Badge(words), "verdict-chip", "verdict-" + applicability);
            if ("risk".equals(applicability)) {
                // The one verdict that asks something of the reader stands out, as in the prototype.
                chip.addThemeVariants(BadgeVariant.ERROR);
            }
            parts.add(chip);
        });
        verdict.assessedAt().ifPresent(day -> parts.add(css(new Span(day.toString()), "recent-date")));
        if (cve.isPresent()) {
            Icon chevron = decorative(VaadinIcon.ANGLE_RIGHT.create(), "recent-chevron");
            parts.add(chevron);
        }
        Component[] children = parts.toArray(Component[]::new);
        if (cve.isPresent()) {
            var row = new RouterLink(CveView.class, new RouteParameters(CveView.IDS, cve.get()));
            row.add(children);
            row.addClassName("recent-row");
            return row;
        }
        return css(new Div(children), "recent-row");
    }

    // The designer's pointer

    private Component pointer() {
        return css(new Div(new H3(getTranslation("home.pointer.title")),
                new Paragraph(getTranslation("home.pointer.text"))), "home-pointer");
    }
}

package com.vaadin.securitystatus.ui.customer;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

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
import com.vaadin.flow.server.auth.AnonymousAllowed;
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

/**
 * The front door: every followed line at a glance, the advisories reviewed
 * most recently, and the pointer for findings Spring Boot manages. Redrawn
 * whenever a new measurement is published.
 */
@Route("")
// The lookup is on every customer route, mounted by the shell through the ShellLookup
// bean, so no route's bytecode names it and the production bundle scanner never finds
// its components. Named here, on the one route whose components load eagerly.
@Uses(Lookup.class)
@PageTitle("home.title")
@AnonymousAllowed
public final class HomeView extends Composite<Div> implements BeforeEnterObserver {


    static final int RECENT_ROWS = 8;

    private static final Pattern SENTENCE_END = Pattern.compile("(?<=\\.)\\s");

    private final SecurityDataService data;
    private final LookupState lookup;
    private final ValueSignal<Boolean> prereleases = new ValueSignal<>(false);
    private final Div body = new Div();
    private final Div linesHolder = new Div();
    private final Div recentHolder = new Div();
    private final Div pointerHolder = new Div();

    public HomeView(SecurityDataService data, DataRefreshSignal refresh, LookupState lookup) {
        this.data = data;
        this.lookup = lookup;
        addClassName("home-view");
        body.addClassName("home-body");
        // The customer shell draws the page's H1, the purpose and the lookup above this.
        getContent().add(body);
        shownUnder(linesHolder, LookupState.Tab.VERSION);
        shownUnder(recentHolder, LookupState.Tab.IDS);
        shownUnder(pointerHolder, LookupState.Tab.REPORT);

        var generatedAt = refresh.generatedAt();
        Signal.effect(this, () -> {
            generatedAt.get();
            show(prereleases.get());
        });
    }

    private void shownUnder(Div holder, LookupState.Tab tab) {
        holder.addClassName("home-part");
        holder.bindVisible(lookup.tab().map(tab::equals));
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
        // Each tab of the lookup gets what helps its own question, D-241: the lines to the version
        // tab, the recently reviewed advisories to the ids tab, the Spring Boot pointer to the report tab.
        linesHolder.add(linesSection(index, withPrereleases));
        recentHolder.add(recentSection(index));
        pointerHolder.add(pointer());
        body.add(linesHolder, recentHolder, pointerHolder);
    }

    // The lines

    /** The lines a customer can be on, newest first: the followed ones, prereleases only when asked. */
    public static List<CasesIndex.Line> shownLines(CasesIndex index, boolean withPrereleases) {
        var followed = index.lineRule().map(rule -> new HashSet<>(rule.followed())).orElse(null);
        return index.lines().stream()
                .filter(line -> followed == null || followed.isEmpty() || followed.contains(line.line()))
                .filter(line -> withPrereleases || !line.isPrerelease())
                .sorted(Comparator.comparing(CasesIndex.Line::version).reversed()).toList();
    }

    /** The release a line's card speaks for: the newest one measured, which is not always the head. */
    static VersionNumber spokenFor(CasesIndex.Line line) {
        return line.measured().orElse(line.version());
    }

    /**
     * The oldest release of the line from which every newer one is secure, or
     * empty when the newest is not. "On X or later" is only true when everything
     * above X is secure too.
     */
    static Optional<VersionNumber> secureFrom(CasesIndex index, String line) {
        VersionNumber answer = null;
        var versions = new ArrayList<>(index.byLine().getOrDefault(line, List.of()));
        versions.sort(Comparator.reverseOrder());
        for (VersionNumber version : versions) {
            boolean secure = index.summaryOf(version).flatMap(CasesIndex.Summary::state).filter("secure"::equals)
                    .isPresent();
            if (!secure) {
                break;
            }
            answer = version;
        }
        return Optional.ofNullable(answer);
    }

    private Component linesSection(CasesIndex index, boolean withPrereleases) {
        var aside = new Span(getTranslation("home.lines.lead"));
        aside.addClassName("section-aside");
        var heading = new Div(new H2(getTranslation("home.lines.title")), aside);
        heading.addClassName("section-heading");
        var cards = new Div();
        cards.addClassName("line-cards");
        for (CasesIndex.Line line : shownLines(index, withPrereleases)) {
            cards.add(card(index, line));
        }
        var tip = new Paragraph(getTranslation("home.lines.tip"));
        tip.addClassName("home-tip");
        var section = new Section(heading, cards, tip);
        section.addClassName("home-lines");
        return section;
    }

    private Component card(CasesIndex index, CasesIndex.Line line) {
        VersionNumber spoken = spokenFor(line);
        Optional<CasesIndex.Summary> summary = index.summaryOf(spoken);
        Optional<String> state = summary.flatMap(CasesIndex.Summary::state);

        var card = new RouterLink(VersionView.class, new RouteParameters(VersionView.VERSION, spoken.toString()));
        card.addClassName("line-card");
        state.ifPresent(word -> card.addClassName("state-" + word));

        var minor = new Span(line.line());
        minor.addClassName("line-card-minor");
        var top = new Div(minor);
        top.addClassName("line-card-top");
        tierBadge(line).ifPresent(top::add);
        summary.flatMap(row -> stateIcon(row)).ifPresent(top::add);
        card.add(top);

        summary.flatMap(this::stateWord).ifPresent(word -> {
            var verdict = new Span(word);
            verdict.addClassName("line-card-state");
            card.add(verdict);
        });

        var action = new Span(actionLine(index, line, summary, spoken));
        action.addClassName("line-card-action");
        card.add(action);

        summary.flatMap(CasesIndex.Summary::scannedAt).or(line::scannedAt).map(OffsetDateTime::toLocalDate)
                .ifPresent(day -> {
                    var scanned = new Span(getTranslation("home.card.scanned", day.toString()));
                    scanned.addClassName("line-card-scanned");
                    card.add(scanned);
                });
        return card;
    }

    /** What Vaadin says about the line, on the card: a mark for extended maintenance, its sentence on hover. */
    private Optional<Component> tierBadge(CasesIndex.Line line) {
        return line.support().filter(support -> support.tier().filter("extended"::equals).isPresent())
                .map(support -> {
                    var badge = new Badge(getTranslation("home.card.extended"));
                    badge.addClassName("line-card-tier");
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
            VaadinIcon shape = "secure".equals(state) ? VaadinIcon.CHECK_CIRCLE
                    : notAssessedOnly(row) ? VaadinIcon.CLOCK
                            : "fix_available".equals(state) ? VaadinIcon.ARROW_CIRCLE_UP : VaadinIcon.WARNING;
            Icon icon = shape.create();
            icon.addClassName("line-card-icon");
            icon.getElement().setAttribute("aria-hidden", "true");
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
        return secureFrom(index, line.line())
                .map(from -> getTranslation("home.card.secureFrom", from.toString()))
                .orElseGet(() -> getTranslation("home.card.newest", spoken.toString()));
    }

    // Recently reviewed

    /**
     * The newest verdicts, one row per advisory. A verdict is written per package
     * and per range, so one advisory can arrive two or three times on the day it
     * is judged.
     */
    static List<Assessment> recent(CasesIndex index) {
        var seen = new HashSet<String>();
        var rows = new ArrayList<Assessment>();
        for (Assessment verdict : index.recent()) {
            String key = verdict.cves().stream().findFirst().or(verdict::id).orElse(null);
            if (key == null || !seen.add(key)) {
                continue;
            }
            rows.add(verdict);
            if (rows.size() == RECENT_ROWS) {
                break;
            }
        }
        return rows;
    }

    /** The first sentence of what the verdict says it means, impact first. */
    static Optional<String> firstSentence(Assessment verdict) {
        return verdict.impact().or(verdict::commentary).map(String::strip)
                .map(text -> SENTENCE_END.split(text, 2)[0]);
    }

    private Component recentSection(CasesIndex index) {
        var heading = new Div(new H2(getTranslation("home.recent.title")));
        heading.addClassName("section-heading");
        index.counts().map(CasesIndex.Counts::cves).ifPresent(total -> {
            var all = new RouterLink(getTranslation("home.recent.all", total), AdvisoriesView.class);
            all.addClassName("recent-all");
            heading.add(all);
        });
        var rows = new Div();
        rows.addClassName("recent-rows");
        for (Assessment verdict : recent(index)) {
            rows.add(recentRow(index, verdict));
        }
        var section = new Section(heading, rows);
        section.addClassName("home-recent");
        return section;
    }

    private Component recentRow(CasesIndex index, Assessment verdict) {
        Optional<String> cve = verdict.cves().stream().findFirst();
        var id = new Span(cve.or(verdict::id).orElse(""));
        id.addClassName("recent-id");
        var parts = new ArrayList<Component>();
        parts.add(id);
        firstSentence(verdict).ifPresent(says -> {
            var text = new Span(says);
            text.addClassName("recent-says");
            parts.add(text);
        });
        String applicability = verdict.applicability().orElse("in_triage");
        Optional.ofNullable(index.labels().get(applicability)).ifPresent(words -> {
            var chip = new Badge(words);
            chip.addClassNames("verdict-chip", "verdict-" + applicability);
            if ("risk".equals(applicability)) {
                // The one verdict that asks something of the reader stands out, as in the prototype.
                chip.addThemeVariants(BadgeVariant.ERROR);
            }
            parts.add(chip);
        });
        verdict.assessedAt().ifPresent(day -> {
            var when = new Span(day.toString());
            when.addClassName("recent-date");
            parts.add(when);
        });
        if (cve.isPresent()) {
            Icon chevron = VaadinIcon.ANGLE_RIGHT.create();
            chevron.addClassName("recent-chevron");
            chevron.getElement().setAttribute("aria-hidden", "true");
            parts.add(chevron);
        }
        Component[] children = parts.toArray(Component[]::new);
        if (cve.isPresent()) {
            var row = new RouterLink(CveView.class, new RouteParameters(CveView.IDS, cve.get()));
            row.add(children);
            row.addClassName("recent-row");
            return row;
        }
        var row = new Div(children);
        row.addClassName("recent-row");
        return row;
    }

    // The designer's pointer

    private Component pointer() {
        var box = new Div(new H3(getTranslation("home.pointer.title")),
                new Paragraph(getTranslation("home.pointer.text")));
        box.addClassName("home-pointer");
        return box;
    }
}

package com.vaadin.securitystatus.ui.customer;

import static com.vaadin.securitystatus.ui.common.Parts.answerTable;
import static com.vaadin.securitystatus.ui.common.Parts.css;
import static com.vaadin.securitystatus.ui.common.Parts.decorative;
import static com.vaadin.securitystatus.ui.customer.AnswerParts.heading;
import static com.vaadin.securitystatus.ui.customer.AnswerParts.verdictState;

import java.io.Serializable;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Code;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.Table;
import com.vaadin.flow.component.html.TableRow;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.dom.DomEventFilter;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsSnippets;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouteParent;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.securitystatus.customer.VersionRules;
import com.vaadin.securitystatus.data.DataRefreshSignal;
import com.vaadin.securitystatus.data.DataUnreachableException;
import com.vaadin.securitystatus.data.SecurityDataService;
import com.vaadin.securitystatus.data.VersionNumber;
import com.vaadin.securitystatus.data.model.CasesIndex;
import com.vaadin.securitystatus.data.model.SpringBootLatest;
import com.vaadin.securitystatus.data.model.SpringBootSupply;
import com.vaadin.securitystatus.data.model.VersionAnswer;
import com.vaadin.securitystatus.ui.common.DataOutageNotice;
import com.vaadin.securitystatus.ui.common.HelpDialog;
import com.vaadin.securitystatus.ui.common.ViewHeading;

import jakarta.annotation.security.PermitAll;

/**
 * The answer by version, the screen a support reply links at: the verdict
 * card, then the sections in the order of feature 04, each drawn only when
 * its list has something in it.
 *
 * <p>
 * The address carries the whole answer. A section the reader opens writes its
 * flag through {@link VersionAddress}, and only that section is drawn again,
 * so the reader stays where they were. A new version is drawn whole and the
 * page goes to the top, rule 6.11.7 of the source page.
 */
@Route("version/:version")
@RouteParent(HomeView.class)
@PageTitle("version.title")
@PermitAll
public final class VersionView extends Composite<Div> implements BeforeEnterObserver {

    public static final String VERSION = "version";

    /** The class a finding the address named carries for a moment, so the reader sees what the link was for. */
    static final String REVEALED = "revealed";

    /** Around a word set in code type while a sentence passes through the bundle; never shown. */
    private static final String CODE = "\u0000";

    /** Declared so the build bundles it and nothing is compiled from a string in the browser. */
    @JsDefinition
    public interface PageJs extends Serializable {

        /** A new answer starts at its top. */
        @JsExpression("window.scrollTo(0, 0)")
        void toTop();
    }

    /** Which clicks on a section link the screen answers in place. A modified click stays a link. */
    @JsDefinition
    public interface LinkClicksJs extends Serializable {

        @JsExpression("return event.button === 0 && !event.ctrlKey && !event.metaKey && !event.shiftKey && !event.altKey")
        DomEventFilter plainClick();
    }

    static final LinkClicksJs CLICKS = JsSnippets.of(LinkClicksJs.class);

    /** The shape of the Spring Boot section: the state's own choice unless the address names one. */
    enum BootShape {
        DEFAULT, TABLE, NOTE
    }

    /** What the screen is drawn from: the index and the version's own document, when there is one. */
    record Loaded(@Nullable CasesIndex index, VersionNumber version, Optional<VersionAnswer> measured) {

        boolean unreachable() {
            return index == null;
        }
    }

    private final SecurityDataService data;
    private final Clock clock;

    private final ValueSignal<String> asked = new ValueSignal<>("");
    private final ValueSignal<BootShape> boot = new ValueSignal<>(BootShape.DEFAULT);
    private final ValueSignal<Boolean> ruled = new ValueSignal<>(false);
    private final ValueSignal<Boolean> disc = new ValueSignal<>(false);
    private final ValueSignal<Boolean> prereleases = new ValueSignal<>(false);
    private final ValueSignal<Optional<ZoneId>> zone = new ValueSignal<>(Optional.empty());
    private final Signal<Loaded> answer;

    /** The address on screen. A section link reads it when clicked and keeps its own target in step with it. */
    private final ValueSignal<VersionAddress> shown = new ValueSignal<>(VersionAddress.of(""));
    private VersionAddress address = VersionAddress.of("");
    private @Nullable String drawnVersion;
    private boolean selfNavigation;
    private @Nullable String pendingReveal;

    private final Div card = section("version-card-slot");
    private final Div noFix = section("version-nofix");
    private final Div triage = section("version-triage");
    private final Div fixes = section("version-fixes");
    private final Div spring = section("version-boot");
    private final Div discontinued = section("version-disc");
    private final Div details = section("version-details");
    private final Div ruledOut = section("version-ruled");
    private final Div lines = section("version-lines");

    /** The findings drawn with layers, by advisory, so an address naming one can open it. */
    private final Map<String, FindingCard> findings = new HashMap<>();
    private final Map<String, Component> rows = new HashMap<>();
    private FindingCard.@Nullable Layers loneLayers;
    private @Nullable String loneAdvisory;

    public VersionView(SecurityDataService data, DataRefreshSignal refresh, Clock clock) {
        this.data = data;
        this.clock = clock;
        addClassName("version-view");
        var body = css(new Div(card, noFix, triage, fixes, spring, discontinued, details, ruledOut, lines), "version-body");
        // No visible "Vaadin X" above the card: its headline names the release, as in the prototype,
        // D-270. The heading stays for screen readers, so the page still reads H1, H2, H3.
        getContent().add(ViewHeading.hiddenAnswer(), body);

        var generatedAt = refresh.generatedAt();
        answer = Signal.computed(() -> {
            generatedAt.get();
            return load(asked.get());
        });

        Signal.effect(this, () -> drawAnswer(answer.get()));
        Signal.effect(spring, () -> drawSpringBoot(answer.get(), boot.get()));
        Signal.effect(discontinued, () -> drawDiscontinued(answer.get(), disc.get()));
        Signal.effect(ruledOut, () -> drawRuledOut(answer.get(), ruled.get()));
        Signal.effect(lines, () -> drawLines(answer.get(), prereleases.get()));

        whenAttached(ui -> {
            ui.getPage().retrieveExtendedClientDetails(details -> zone.set(Optional.of(details.getZoneId())));
            return null;
        });
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String version = event.getRouteParameters().get(VERSION).orElse("");
        address = VersionAddress.of(version, event.getLocation());
        shown.set(address);
        boolean ours = selfNavigation;
        selfNavigation = false;
        boolean newQuestion = !version.equals(drawnVersion);
        drawnVersion = version;

        boot.set(address.boot() == null ? BootShape.DEFAULT : address.boot() ? BootShape.TABLE : BootShape.NOTE);
        ruled.set(address.ruled());
        disc.set(address.disc());
        prereleases.set(address.prereleases());
        asked.set(version);

        var ui = event.getUI();
        if (newQuestion) {
            ui.beforeClientResponse(this, context -> getElement().executeJs(PageJs.class).toTop());
        }
        String open = address.open();
        pendingReveal = open != null && !ours ? open : null;
        if (pendingReveal != null && !newQuestion) {
            // Nothing is drawn again for the same version, so the finding is already on screen.
            revealAfterThisRequest();
        }
    }

    /** Revealed once everything of this request is drawn, the sections behind flags included. */
    private void revealAfterThisRequest() {
        getUI().ifPresent(ui -> ui.beforeClientResponse(this, context -> {
            String advisory = pendingReveal;
            pendingReveal = null;
            if (advisory != null) {
                reveal(advisory);
            }
        }));
    }

    // Loading

    private Loaded load(String version) {
        VersionNumber number = VersionNumber.of(version.isBlank() ? "0" : version);
        try {
            CasesIndex index = data.index();
            return new Loaded(index, number, data.version(number));
        } catch (DataUnreachableException unreachable) {
            return new Loaded(null, number, Optional.empty());
        }
    }

    // The answer, drawn whole when the version or the measurement changes

    private void drawAnswer(Loaded current) {
        for (Div slot : List.of(card, noFix, triage, fixes, details)) {
            slot.removeAll();
        }
        findings.clear();
        rows.clear();
        loneLayers = null;
        loneAdvisory = null;
        if (current.unreachable()) {
            card.add(new DataOutageNotice());
            return;
        }
        CasesIndex index = current.index();
        if (current.measured().isEmpty()) {
            card.add(unmeasuredCard(index, current.version()));
            return;
        }
        VersionAnswer measured = current.measured().get();
        List<String> stuckRisks = VersionRules.stuckRisks(measured);
        String lone = stuckRisks.size() == 1 ? stuckRisks.get(0) : null;
        String open = address.open();

        card.add(verdictCard(index, measured, stuckRisks, lone));
        if (stuckRisks.size() > 1) {
            drawNoFix(index, measured, stuckRisks, open);
        }
        drawTriage(measured);
        drawFixes(index, measured);
        if (lone != null) {
            var said = VersionRules.verdictsFor(measured, lone);
            var layers = new FindingCard.Layers(measured, index, lone, said,
                    index.advisory(lone), true);
            loneLayers = layers;
            loneAdvisory = lone;
            details.add(heading(getTranslation("version.details.title", lone), null), layers);
        }
        if (pendingReveal != null) {
            revealAfterThisRequest();
        }
    }

    // The verdict card

    private Component verdictCard(CasesIndex index, VersionAnswer measured, List<String> stuckRisks, @Nullable String lone) {
        String state = measured.state().orElse("");
        var box = css(new Section(), "verdict-card", "state-" + (state.isEmpty() ? "none" : state));

        stateWord(state).ifPresent(word -> box.add(verdictState(stateIcon(state), word)));

        Words words = words(index, measured, stuckRisks);
        words.title().ifPresent(title -> box.add(new H3(title)));
        if (!words.why().isEmpty()) {
            box.add(css(new Paragraph(String.join(" ", words.why())), "verdict-why"));
        }

        var badges = badges(index, measured);
        if (badges.getComponentCount() > 0) {
            box.add(badges);
        }

        if (lone != null) {
            box.add(new FindingCard(measured, index, lone, false, false));
        }

        var actions = css(new Div(), "verdict-actions");
        VersionRules.upgradeTo(measured).ifPresent(to -> VersionRules.releaseNotes(index, to).ifPresent(url -> {
            var notes = css(new Anchor(url, getTranslation("version.action.releaseNotes", to.toString())), "verdict-action", "primary");
            actions.add(notes);
        }));
        if (!measured.fixable().isEmpty() && VersionRules.upgradeTo(measured).isPresent()) {
            var what = css(new Button(getTranslation("version.action.fixes"), click -> scrollToFixes()), "what-it-fixes");
            what.addThemeVariants(ButtonVariant.TERTIARY);
            actions.add(what);
        }
        if ("unresolved".equals(state)) {
            actions.add(notifyMe());
            VersionRules.secureMove(index, measured.version(), measured.lineName())
                    .ifPresent(move -> {
                        String text = move.sameLine()
                                ? getTranslation("version.action.move.same", move.to().toString())
                                : getTranslation("version.action.move.other", move.to().toString(),
                                        measured.lineName());
                        var link = css(new RouterLink(text, VersionView.class,
                                new RouteParameters(VERSION, move.to().toString())), "verdict-action", "secure-move");
                        actions.add(link);
                    });
        }
        measured.scannedAt().ifPresent(scanned -> actions.add(scannedLine(scanned)));
        if (actions.getComponentCount() > 0) {
            box.add(actions);
        }
        return box;
    }

    /** The headline and the sentences under it, each claim made only where the measurement carries it. */
    private record Words(Optional<String> title, List<String> why) {
    }

    private Words words(CasesIndex index, VersionAnswer measured, List<String> stuckRisks) {
        String version = measured.version().toString();
        int open = VersionRules.openCount(measured);
        var why = new ArrayList<String>();
        switch (measured.state().orElse("")) {
            case "secure" -> {
                why.add(getTranslation("version.card.secure.why"));
                if (!measured.theirs().isEmpty() || !measured.springPinned().isEmpty()) {
                    why.add(getTranslation("version.card.secure.why.boot"));
                }
                if (!measured.discontinued().isEmpty()) {
                    why.add(getTranslation("version.card.secure.why.disc"));
                }
                return new Words(Optional.of(getTranslation("version.card.secure.title", version)), why);
            }
            case "fix_available" -> {
                Optional<VersionNumber> to = VersionRules.upgradeTo(measured);
                to.filter(target -> target.line().equals(measured.version().line()))
                        .ifPresent(target -> why.add(getTranslation(plural("version.card.fix.why", open), version, open,
                                target.toString())));
                return new Words(to.map(target -> getTranslation("version.card.fix.title", target.toString())), why);
            }
            case "unresolved" -> {
                Optional<VersionNumber> to = VersionRules.upgradeTo(measured);
                Optional<VersionAnswer.Upgrade> upgrade = measured.upgrade();
                if (to.isPresent()) {
                    Integer clears = upgrade.map(VersionAnswer.Upgrade::clears).orElse(null);
                    Integer remains = upgrade.map(VersionAnswer.Upgrade::remains).orElse(null);
                    if (clears != null) {
                        why.add(getTranslation(plural("version.card.partial.clears", clears), to.get().toString(),
                                clears));
                    }
                    if (remains != null) {
                        why.add(getTranslation(plural("version.card.partial.rest", remains)));
                        return new Words(Optional.of(getTranslation(plural("version.card.partial.title", remains),
                                to.get().toString(), remains)), why);
                    }
                    return new Words(Optional.of(getTranslation("version.card.fix.title", to.get().toString())), why);
                }
                if (stuckRisks.isEmpty()) {
                    int unjudged = measured.triage().size();
                    return new Words(unjudged == 0 ? Optional.empty()
                            : Optional.of(getTranslation(plural("version.card.triage.title", unjudged), unjudged)), why);
                }
                boolean covered = stuckRisks.stream()
                        .allMatch(id -> VersionRules.hasWorkaround(VersionRules.verdictsFor(measured, id)));
                if (covered) {
                    why.add(getTranslation(plural("version.card.stuck.workaround", stuckRisks.size())));
                }
                if (VersionRules.isNewestOfItsLine(index, measured)) {
                    why.add(getTranslation("version.card.stuck.newest", version));
                }
                if (stuckRisks.size() != 1) {
                    why.add(getTranslation("version.card.stuck.below"));
                }
                if (measured.triage().isEmpty()) {
                    why.add(getTranslation("version.card.stuck.rest"));
                }
                return new Words(Optional.of(getTranslation(plural("version.card.stuck.title", stuckRisks.size()),
                        stuckRisks.size())), why);
            }
            default -> {
                return new Words(Optional.empty(), why);
            }
        }
    }

    private Div badges(CasesIndex index, VersionAnswer measured) {
        var badges = css(new Div(), "verdict-badges");
        String line = measured.lineName();
        if (measured.version().isPrerelease()) {
            badges.add(badge(getTranslation("version.badge.prerelease")));
        }
        VersionRules.upgradeTo(measured).filter(to -> to.line().equals(line)).ifPresent(to -> {
            badges.add(badge(getTranslation("version.badge.maintenance")));
            badges.add(badge(getTranslation("version.badge.sameMinor", line)));
        });
        index.lines().stream().filter(candidate -> candidate.line().equals(line)).findFirst()
                .flatMap(CasesIndex.Line::support).ifPresent(support -> {
                    support.label().ifPresent(label -> {
                        var tier = badge(label);
                        tier.addClassName("tier-" + support.tier().orElse("none"));
                        badges.add(tier);
                    });
                    if (support.tier().filter("extended"::equals).isPresent()) {
                        Optional<VersionNumber> from = support.subscriptionFrom();
                        if (from.isPresent() && from.get().line().equals(line)) {
                            badges.add(badge(getTranslation("version.badge.subscription", from.get().toString())));
                        } else {
                            support.freeEnded().ifPresent(
                                    ended -> badges.add(badge(getTranslation("version.badge.freeEnded", ended))));
                        }
                    }
                });
        return badges;
    }

    private static Badge badge(String text) {
        return css(new Badge(text), "verdict-badge");
    }

    /** The design's button, live, saying it is not built and what that would take, rule 6.11.15. */
    private Component notifyMe() {
        var dialog = HelpDialog.dialog(getTranslation("version.notify.title"),
                new Paragraph(getTranslation("version.notify.asks")), new Paragraph(getTranslation("version.notify.why")),
                new Paragraph(getTranslation("version.notify.needs")));

        var button = css(new Button(getTranslation("version.action.notify"), VaadinIcon.BELL.create(),
                click -> dialog.open()), "notify-me");
        button.addThemeVariants(ButtonVariant.TERTIARY);
        return new Span(button, dialog);
    }

    /** The scan date, and once the reader's timezone is known, how long ago that is in their calendar. */
    private Component scannedLine(OffsetDateTime scanned) {
        var icon = decorative(VaadinIcon.CLOCK.create());
        var text = new Span();
        text.bindText(zone.map(known -> scannedText(scanned, known)));
        return css(new Span(icon, text), "verdict-scanned");
    }

    private String scannedText(OffsetDateTime scanned, Optional<ZoneId> readersZone) {
        if (readersZone.isEmpty()) {
            return getTranslation("version.scanned", scanned.atZoneSameInstant(ZoneOffset.UTC).toLocalDate().toString());
        }
        LocalDate day = scanned.atZoneSameInstant(readersZone.get()).toLocalDate();
        LocalDate today = clock.instant().atZone(readersZone.get()).toLocalDate();
        long days = ChronoUnit.DAYS.between(day, today);
        if (days == 0) {
            return getTranslation("version.scanned.today", day.toString());
        }
        if (days == 1) {
            return getTranslation("version.scanned.yesterday", day.toString());
        }
        if (days > 1) {
            return getTranslation("version.scanned.days", day.toString(), days);
        }
        return getTranslation("version.scanned", day.toString());
    }

    /** A version nobody measured, answered where it was asked, rule 6.9.7. */
    private Component unmeasuredCard(CasesIndex index, VersionNumber version) {
        var box = css(new Section(), "verdict-card", "state-unmeasured");
        box.add(verdictState(VaadinIcon.QUESTION_CIRCLE_O, getTranslation("version.card.unmeasured.word")), new H3(getTranslation("version.card.unmeasured.title", version.toString())));

        Optional<VersionRules.Nearest> nearest = VersionRules.nearestMeasured(index, version);
        String why = nearest.map(found -> switch (found.kind()) {
            case LINE -> getTranslation("version.card.unmeasured.line", found.version().toString(), version.line());
            case MAJOR -> getTranslation("version.card.unmeasured.major", version.line(), VersionRules.major(version),
                    found.version().toString());
            case OLDEST -> getTranslation("version.card.unmeasured.oldest", version.toString(),
                    found.version().toString());
        }).orElseGet(() -> getTranslation("version.card.unmeasured.none", version.line()));
        box.add(css(new Paragraph(why), "verdict-why"));
        nearest.ifPresent(found -> {
            var see = css(new RouterLink(getTranslation("version.card.unmeasured.see", found.version().toString()),
                    VersionView.class, new RouteParameters(VERSION, found.version().toString())), "verdict-action", "primary", "nearest-measured");
            var actions = css(new Div(see), "verdict-actions");
            box.add(actions);
        });
        return box;
    }

    // Findings with no fix yet, the not assessed notice, and what the upgrade fixes

    private void drawNoFix(CasesIndex index, VersionAnswer measured, List<String> stuckRisks, @Nullable String open) {
        String version = measured.version().toString();
        int openCount = VersionRules.openCount(measured);
        String aside = VersionRules.upgradeTo(measured)
                .map(to -> getTranslation(plural("version.noFix.aside", openCount), stuckRisks.size(), openCount, version,
                        to.toString()))
                .orElseGet(() -> count(stuckRisks.size()));
        noFix.add(heading(getTranslation("version.noFix.title"), aside));
        List<String> ordered = VersionRules.worstFirst(stuckRisks, index);
        boolean named = open != null && ordered.contains(open);
        for (int i = 0; i < ordered.size(); i++) {
            String advisory = ordered.get(i);
            boolean opened = named ? advisory.equals(open) : i == 0;
            var finding = new FindingCard(measured, index, advisory, true, opened);
            findings.put(advisory, finding);
            finding.layers().means().ifPresent(means -> means.addOpenedChangeListener(change -> {
                if (change.isFromClient()) {
                    readerOpened(advisory, change.isOpened());
                }
            }));
            noFix.add(finding);
        }
    }

    /** The reader opened or closed a finding: the address says so, and nothing moves. */
    private void readerOpened(String advisory, boolean opened) {
        if (opened) {
            write(address.withOpen(advisory));
        } else if (advisory.equals(address.open())) {
            write(address.withOpen(null));
        }
    }

    private void drawTriage(VersionAnswer measured) {
        List<String> ids = measured.triage();
        if (ids.isEmpty()) {
            return;
        }
        var chip = css(new Badge(getTranslation("state.notAssessed")), "status-chip", "state-in_triage");
        var text = new Paragraph(getTranslation(plural("version.triage.text", ids.size()), ids.size()));
        var list = css(new Paragraph(), "triage-ids");
        List<String> named = ids.subList(0, Math.min(VersionRules.TRIAGE_NAMED, ids.size()));
        for (int i = 0; i < named.size(); i++) {
            if (i > 0) {
                list.add(new Text(getTranslation("version.list.separator")));
            }
            list.add(AnswerParts.advisoryLink(named.get(i), measured.version().toString(), "finding-id"));
        }
        if (ids.size() > VersionRules.TRIAGE_NAMED) {
            list.add(new Text(" "));
            var more = css(new Span(getTranslation("version.triage.more", ids.size() - VersionRules.TRIAGE_NAMED)), "triage-more");
            list.add(more);
        }
        var notice = css(new Div(chip, new Div(text, list)), "triage-notice");
        notice.getElement().setAttribute("role", "note");
        triage.add(notice);
    }

    private void drawFixes(CasesIndex index, VersionAnswer measured) {
        Optional<VersionNumber> to = VersionRules.upgradeTo(measured);
        if (measured.fixable().isEmpty() || to.isEmpty()) {
            return;
        }
        String version = measured.version().toString();
        String line = measured.lineName();
        int openCount = VersionRules.openCount(measured);
        fixes.add(heading(getTranslation("version.fixes.title", to.get().toString()),
                getTranslation(plural("version.fixes.aside", openCount), measured.fixable().size(), openCount, version)));
        var list = css(new Div(), "fix-rows");
        List<String> ordered = VersionRules.worstFirst(measured.fixable(), index);
        for (String advisory : ordered.subList(0, Math.min(VersionRules.FIXES_SHOWN, ordered.size()))) {
            var row = fixRow(index, measured, advisory, line, to.get());
            rows.put(advisory, row);
            list.add(row);
        }
        if (ordered.size() > VersionRules.FIXES_SHOWN) {
            var more = css(new Div(new Span(getTranslation("version.fixes.more", ordered.size() - VersionRules.FIXES_SHOWN))), "fix-row", "fix-more");
            list.add(more);
        }
        fixes.add(list);
    }

    private Component fixRow(CasesIndex index, VersionAnswer measured, String advisory, String line, VersionNumber to) {
        Optional<CasesIndex.AdvisoryCatalogEntry> entry = index.advisory(advisory);
        var row = css(new RouterLink(CveView.class, CveView.parameters(advisory, measured.version().toString())), "fix-row");
        row.getElement().setAttribute("data-advisory", advisory);
        var id = css(new Span(advisory), "fix-id");
        row.add(id);
        AnswerParts.severity(this, entry.flatMap(CasesIndex.AdvisoryCatalogEntry::severity)).ifPresent(row::add);
        AnswerParts.ownerChip(this, entry.flatMap(CasesIndex.AdvisoryCatalogEntry::fixOwner)).ifPresent(row::add);
        Optional<String> where = entry.flatMap(found -> found.packages().stream().findFirst()).map(VersionRules::shortName)
                .or(() -> entry.flatMap(found -> found.modules().stream().findFirst()));
        Optional<String> impact = VersionRules.oneLine(VersionRules.verdictsFor(measured, advisory))
                .or(() -> entry.flatMap(CasesIndex.AdvisoryCatalogEntry::applicability).map(index.labels()::get));
        List<String> says = new ArrayList<>();
        where.ifPresent(says::add);
        impact.ifPresent(says::add);
        if (!says.isEmpty()) {
            row.add(css(new Span(String.join(" · ", says)), "fix-says"));
        }
        var end = css(new Badge(VersionRules.fixedIn(index, advisory, line)
                .map(release -> getTranslation("version.fixes.fixedIn", release.toString()))
                .orElseGet(() -> getTranslation("version.fixes.goneBy", to.toString()))), "status-chip", "state-fix_available", "fix-end");
        row.add(end);
        Icon chevron = decorative(VaadinIcon.ANGLE_RIGHT.create(), "fix-chevron");
        row.add(chevron);
        return row;
    }

    private void scrollToFixes() {
        fixes.getElement().executeJs(Element.ScrollIntoViewJs.class).scrollIntoView();
    }

    // Spring Boot, in the two shapes of rule 6.11.6

    private void drawSpringBoot(Loaded current, BootShape shape) {
        spring.removeAll();
        if (current.unreachable() || current.measured().isEmpty()) {
            return;
        }
        CasesIndex index = current.index();
        VersionAnswer measured = current.measured().get();
        if (measured.theirs().isEmpty() && measured.springPinned().isEmpty()) {
            return;
        }
        boolean table = switch (shape) {
            case TABLE -> true;
            case NOTE -> false;
            case DEFAULT -> !VersionRules.stuckRisks(measured).isEmpty();
        };
        if (!table) {
            var note = css(new Div(new H3(getTranslation("version.boot.note.title")),
                    new Paragraph(withCode(getTranslation("version.boot.note.text",
                            code(getTranslation("version.boot.property"))))),
                    sectionLink(getTranslation("version.boot.show"), at -> at.withBoot(true))), "boot-note");
            spring.add(note);
            return;
        }
        int total = VersionRules.bootTotal(measured);
        var aside = new Span(new Span(count(total)), new Span(" · "),
                sectionLink(getTranslation("version.section.hide"), at -> at.withBoot(false)));
        spring.add(heading(getTranslation("version.boot.title"), aside));

        Optional<VersionNumber> declared = measured.springBoot();
        Optional<SpringBootLatest> newer = declared.map(boot -> index.springBootLatest().get(boot.toString()));
        Optional<VersionNumber> target = newer.flatMap(found -> found.patch().or(found::minor));
        var tip = new ArrayList<String>();
        int pinned = measured.springPinned().size();
        if (pinned > 0) {
            tip.add(getTranslation(plural("version.boot.pinned", pinned), pinned));
        }
        declared.ifPresent(boot -> tip.add(getTranslation("version.boot.declares", boot.toString())));
        Optional<String> supply = declared.flatMap(boot -> supplySentence(boot, index.springBootSupply().get(boot.toString())));
        if (supply.isPresent()) {
            tip.add(supply.get());
        } else {
            target.ifPresent(release -> tip.add(getTranslation("version.boot.newest", release.toString())));
        }
        var paragraph = new Paragraph(withCode(getTranslation("version.boot.tip",
                code(getTranslation("version.boot.property")))));
        if (!tip.isEmpty()) {
            paragraph.add(new Text(" " + String.join(" ", tip)));
        }
        paragraph.addClassName("section-tip");
        spring.add(paragraph);

        List<String> all = VersionRules.bootRows(measured, index);
        List<String> listed = all.subList(0, Math.min(VersionRules.BOOT_ROWS, all.size()));
        String fix = target.map(release -> getTranslation("version.boot.fix.to", release.toString()))
                .orElseGet(() -> getTranslation("version.boot.fix.any"));
        spring.add(advisoryTable(index, measured, listed, fix));
        if (total > listed.size()) {
            var more = css(new Paragraph(getTranslation(measured.theirsRisk().isEmpty() ? "version.boot.more.all"
                    : "version.boot.more.risk", total - listed.size())), "section-tip");
            spring.add(more);
        }
    }

    /** Where the patches of the declared Spring Boot line come from, when that is not simply open. */
    private Optional<String> supplySentence(VersionNumber boot, @Nullable SpringBootSupply supply) {
        if (supply == null || supply.state().isEmpty() || supply.commercial().isEmpty() || supply.open().isEmpty()) {
            return Optional.empty();
        }
        String line = boot.line();
        return switch (supply.state().get()) {
            case "ended" -> Optional.of(getTranslation("version.boot.ended", line, supply.open().get().toString(),
                    supply.commercial().get().toString()));
            case "commercial" -> Optional.of(getTranslation("version.boot.commercial", line,
                    supply.open().get().toString(), supply.commercial().get().toString()));
            default -> Optional.empty();
        };
    }

    // Behind their flags: discontinued and ruled out

    private void drawDiscontinued(Loaded current, boolean open) {
        discontinued.removeAll();
        if (current.unreachable() || current.measured().isEmpty()) {
            return;
        }
        VersionAnswer measured = current.measured().get();
        List<String> ids = measured.discontinued();
        if (ids.isEmpty()) {
            return;
        }
        CasesIndex index = current.index();
        String version = measured.version().toString();
        discontinued.add(heading(getTranslation("version.disc.title"), count(ids.size())));
        String tip = measured.discontinuedThrough().isEmpty() ? getTranslation("version.disc.tip.unnamed", version)
                : getTranslation("version.disc.tip", measured.discontinuedThrough().stream().map(VersionView::code)
                        .collect(Collectors.joining(getTranslation("version.list.separator"))), version);
        var paragraph = css(new Paragraph(new Span(withCode(tip)), new Text(" "), sectionLink(
                getTranslation(open ? "version.section.hide" : "version.section.show"), at -> at.withDisc(!open))), "section-tip");
        discontinued.add(paragraph);
        if (open) {
            discontinued.add(advisoryTable(index, measured, VersionRules.worstFirst(ids, index),
                    getTranslation("version.disc.fix")));
        }
    }

    private void drawRuledOut(Loaded current, boolean open) {
        ruledOut.removeAll();
        if (current.unreachable() || current.measured().isEmpty()) {
            return;
        }
        VersionAnswer measured = current.measured().get();
        List<String> all = new ArrayList<>(measured.ruledOut());
        all.addAll(measured.fromBuild());
        if (all.isEmpty()) {
            return;
        }
        CasesIndex index = current.index();
        List<String> ordered = VersionRules.worstFirst(all, index);
        ruledOut.add(heading(getTranslation("version.ruled.title"), count(ordered.size())));
        var paragraph = css(new Paragraph(new Span(getTranslation("version.ruled.tip")), new Text(" "), sectionLink(
                getTranslation(open ? "version.section.hide" : "version.section.show"), at -> at.withRuled(!open))), "section-tip");
        ruledOut.add(paragraph);
        if (open) {
            var grid = answerTable(getTranslation("version.ruled.col.advisory"), getTranslation("version.ruled.col.why"));
            for (String advisory : ordered) {
                TableRow row = grid.addRow();
                var id = row.addDataCell(AnswerParts.advisoryLink(advisory, measured.version().toString(), "finding-id"));
                Optional<String> why = measured.fromBuild().contains(advisory)
                        ? Optional.of(getTranslation("version.ruled.build"))
                        : index.advisory(advisory)
                                .flatMap(CasesIndex.AdvisoryCatalogEntry::applicability)
                                .map(index.labels()::get);
                // A reason the data does not carry has no cell: the id spans the row instead of
                // standing beside an empty one that reads as a reason, D-60.
                why.ifPresentOrElse(row::addDataCell, () -> id.setColspan(2));
                row.getElement().setAttribute("data-advisory", advisory);
            }
            ruledOut.add(grid);
        }
    }

    // All supported versions

    private void drawLines(Loaded current, boolean withPrereleases) {
        lines.removeAll();
        if (current.unreachable()) {
            return;
        }
        CasesIndex index = current.index();
        String readers = current.measured().flatMap(VersionAnswer::line).orElse(current.version().line());
        lines.add(heading(getTranslation("version.lines.title"), getTranslation("version.lines.aside")));
        var grid = answerTable(getTranslation("version.lines.col.minor"), getTranslation("version.lines.col.newest"),
                getTranslation("version.lines.col.status"), getTranslation("version.lines.col.open"),
                getTranslation("version.lines.col.maintenance"), getTranslation("version.lines.col.scanned"));
        grid.addClassName("lines-table");
        for (CasesIndex.Line line : VersionRules.tableLines(index, readers, withPrereleases)) {
            VersionNumber spoken = VersionRules.spokenFor(line);
            Optional<CasesIndex.Summary> summary = index.summaryOf(spoken);
            boolean yours = line.line().equals(readers);
            TableRow row = grid.addRow();
            row.addClassName("line-row");
            if (yours) {
                row.addClassName("yours");
            }
            row.addRowHeaderCell(yours ? getTranslation("version.lines.yours", line.line()) : line.line());

            var newest = new Span(new RouterLink(spoken.toString(), VersionView.class,
                    new RouteParameters(VERSION, spoken.toString())));
            VersionRules.secureFrom(index, line.line()).filter(from -> !from.equals(spoken)).ifPresent(from -> {
                var safe = css(new Span(getTranslation("version.lines.safeFrom", from.toString())), "safe-from");
                newest.add(new Text(" "), safe);
            });
            row.addDataCell(newest);

            var status = row.addDataCell();
            summary.flatMap(CasesIndex.Summary::state).flatMap(this::stateWord).ifPresent(word -> {
                var chip = css(new Badge(word), "status-chip", "state-" + summary.get().state().orElse(""));
                status.add(chip);
            });
            row.addDataCell(summary.flatMap(CasesIndex.Summary::open).map(String::valueOf).orElse(""));
            row.addDataCell(line.support().flatMap(CasesIndex.Support::label).orElse(""));
            row.addDataCell(summary.flatMap(CasesIndex.Summary::scannedAt).or(line::scannedAt)
                    .map(scanned -> scanned.toLocalDate().toString()).orElse(""));
        }
        lines.add(grid);
    }

    // Revealing what an address names

    /**
     * Opens the finding the address names, brings it into view and marks it for
     * a moment. A finding that is not on this version, or not in a section on
     * screen, is ignored: nothing is invented for it.
     */
    private void reveal(String advisory) {
        Component target = null;
        FindingCard finding = findings.get(advisory);
        if (finding != null) {
            finding.layers().means().ifPresent(means -> means.setOpened(true));
            target = finding;
        } else if (advisory.equals(loneAdvisory) && loneLayers != null) {
            loneLayers.all().forEach(layer -> layer.setOpened(!"full".equals(layer.getElement().getAttribute("data-layer"))));
            target = details;
        } else if (rows.containsKey(advisory)) {
            target = rows.get(advisory);
        } else {
            target = tableRow(advisory);
        }
        if (target == null) {
            return;
        }
        target.getElement().getClassList().add(REVEALED);
        target.getElement().executeJs(Element.ScrollIntoViewJs.class).scrollIntoView();
    }

    private @Nullable Component tableRow(String advisory) {
        for (Div slot : List.of(spring, discontinued, ruledOut)) {
            Optional<Component> row = slot.getChildren().filter(Table.class::isInstance).map(Table.class::cast)
                    .flatMap(table -> table.getBodyRows().stream())
                    .filter(candidate -> advisory.equals(candidate.getElement().getAttribute("data-advisory")))
                    .map(Component.class::cast).findFirst();
            if (row.isPresent()) {
                return row.get();
            }
        }
        return null;
    }

    // Writing the address

    /** Every section toggle goes through here, and the view keeps the reader where they are. */
    private void write(VersionAddress next) {
        selfNavigation = true;
        next.go(UI.getCurrentOrThrow());
    }

    /**
     * A section control: a real link to the address it would write, so it can
     * be copied or opened in a new tab, and a plain click answered in place.
     */
    private Anchor sectionLink(String text, UnaryOperator<VersionAddress> change) {
        var link = css(new Anchor(change.apply(shown.peek()).href(), text), "section-link");
        link.getElement().setAttribute("router-ignore", true);
        Signal.effect(link, () -> link.setHref(change.apply(shown.get()).href()));
        link.getElement().addEventListener("click", click -> write(change.apply(shown.peek())))
                .setFilterExpression(CLICKS.plainClick()).preventDefault();
        return link;
    }

    // Small parts

    private static Div section(String className) {
        return css(new Div(), className);
    }

    /** The dependency, advisory and fix table of the Spring Boot and discontinued sections. */
    private Table advisoryTable(CasesIndex index, VersionAnswer measured, List<String> advisories, String fix) {
        var grid = answerTable(getTranslation("version.boot.col.dependency"), getTranslation("version.boot.col.advisory"),
                getTranslation("version.boot.col.fix"));
        for (String advisory : advisories) {
            TableRow row = grid.addRow();
            row.addDataCell(packages(index, advisory));
            row.addDataCell(AnswerParts.advisoryLink(advisory, measured.version().toString(), "finding-id"));
            row.addDataCell(fix);
            row.getElement().setAttribute("data-advisory", advisory);
        }
        return grid;
    }

    private Component packages(CasesIndex index, String advisory) {
        String text = index.advisory(advisory)
                .map(entry -> String.join(", ", entry.packages().stream().limit(2).toList())).orElse("");
        return new Span(text);
    }

    /** Marks a word of the data or of the bundle to be set in code type, inside a translated sentence. */
    private static String code(String word) {
        return CODE + word + CODE;
    }

    /** A translated sentence with its marked words in {@code code} elements, the rest as text. */
    static Component[] withCode(String text) {
        String[] parts = text.split(CODE, -1);
        var out = new ArrayList<Component>();
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].isEmpty()) {
                continue;
            }
            out.add(i % 2 == 1 ? new Code(parts[i]) : new Text(parts[i]));
        }
        return out.toArray(Component[]::new);
    }

    private String count(int n) {
        return getTranslation(plural("version.count", n), n);
    }

    private static String plural(String key, int n) {
        return key + (n == 1 ? ".one" : ".many");
    }

    private Optional<String> stateWord(String state) {
        return switch (state) {
            case "secure" -> Optional.of(getTranslation("state.secure"));
            case "fix_available" -> Optional.of(getTranslation("state.fixAvailable"));
            case "unresolved" -> Optional.of(getTranslation("state.unresolved"));
            default -> Optional.empty();
        };
    }

    private static VaadinIcon stateIcon(String state) {
        return switch (state) {
            // The glyph alone: the stylesheet draws the prototype's filled circle round it.
            case "secure" -> VaadinIcon.CHECK;
            case "fix_available" -> VaadinIcon.ARROW_UP;
            default -> VaadinIcon.EXCLAMATION;
        };
    }
}

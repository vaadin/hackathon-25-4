package com.vaadin.securitystatus.explorer;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.securitystatus.base.security.Roles;
import com.vaadin.securitystatus.base.ui.AdvisoryIds;
import com.vaadin.securitystatus.base.ui.DataOutageNotice;
import com.vaadin.securitystatus.base.ui.DrawerContent;
import com.vaadin.securitystatus.base.ui.InternalLayout;
import com.vaadin.securitystatus.data.DataRefreshSignal;
import com.vaadin.securitystatus.data.DataUnreachableException;
import com.vaadin.securitystatus.data.SecurityDataService;
import com.vaadin.securitystatus.data.VersionNumber;
import com.vaadin.securitystatus.data.model.ExplorerFront;
import com.vaadin.securitystatus.data.model.ExplorerFront.Line;
import com.vaadin.securitystatus.data.model.ExplorerVersion;
import com.vaadin.securitystatus.explorer.ExplorerAnswer.Asked;
import com.vaadin.securitystatus.explorer.ExplorerAnswer.Shape;
import com.vaadin.securitystatus.explorer.ExplorerAnswer.Split;
import com.vaadin.securitystatus.explorer.ExplorerRules.Product;

import jakarta.annotation.security.RolesAllowed;

/**
 * One exact release, in the prototype's distribution, D-216 to D-218: the
 * left panel holds the filter, which holds the version on screen, and the
 * products, counted on this release and shared with the explorer through
 * {@link ProductSelection}; the content is the explorer's series card for this
 * release, opened, with the advice, the files and the two halves, and the
 * legend the "?" opens. A release nobody measured is answered here, rule
 * 6.9.7. The heading names the version and the document title follows, rule
 * 6.1.7.
 */
@Route(value = "findings/release/:line/:version", layout = InternalLayout.class)
@PageTitle("release.title")
@RolesAllowed(Roles.SECURITY)
public final class ReleaseView extends Composite<Div> implements BeforeEnterObserver, DrawerContent {

    public static final String LINE = "line";
    public static final String VERSION = "version";

    private final SecurityDataService data;
    private final ProductSelection selection;
    private final SeriesParts parts;

    private final ValueSignal<Optional<VersionNumber>> shown = new ValueSignal<>(Optional.empty());
    private final ValueSignal<Boolean> legendOpen = new ValueSignal<>(false);

    private final Div panel = new Div();
    private final TextField lookup = new TextField();
    private final Div profile = new Div();
    private final Div body = new Div();
    private final Paragraph footer = new Paragraph();

    public ReleaseView(SecurityDataService data, DataRefreshSignal refresh, ProductSelection selection) {
        this.data = data;
        this.selection = selection;
        this.parts = new SeriesParts(this, data, selection);
        addClassNames("release-view", "explorer-view");
        buildPanel();

        body.addClassName("release-body");
        footer.addClassName("release-footer");
        var main = new Div(parts.legendButton(legendOpen), back(), body, footer);
        main.addClassNames("explorer-main", "release-main");
        getContent().add(main, parts.legend(legendOpen));
        Shortcuts.addShortcutListener(this, () -> legendOpen.set(false), Key.ESCAPE);

        var generatedAt = refresh.generatedAt();
        var off = selection.off();
        Signal.effect(this, () -> {
            generatedAt.get();
            Set<String> products = off.get();
            shown.get().ifPresent(version -> draw(version, products));
        });
        whenAttached(ui -> {
            if (!selection.unsettled()) {
                return null;
            }
            try {
                Set<String> names = new LinkedHashSet<>();
                catalogue(data.explorerFront()).stream().filter(Product::switchable)
                        .forEach(product -> names.add(product.name()));
                // The choice the explorer remembers, read here too, as the prototype's page reads it.
                selection.readStorage(ui, names, restored -> {
                    if (selection.unsettled()) {
                        selection.select(restored);
                    }
                });
            } catch (DataUnreachableException unreachable) {
                // Nothing to restore against; the default stays.
            }
            return null;
        });
    }

    @Override
    public Component drawerContent() {
        return panel;
    }

    /**
     * The one way back, above the content and in the same place on every
     * deep page, rule 6.1.6.
     */
    static RouterLink back() {
        var back = new RouterLink(ExplorerView.class);
        back.add(VaadinIcon.ANGLE_LEFT.create(), new Span(back.getTranslation("release.back")));
        back.addClassName("release-back");
        return back;
    }

    /** The address of one release's page. */
    static RouteParameters parameters(VersionNumber version) {
        return new RouteParameters(Map.of(LINE, version.line(), VERSION, version.toString()));
    }

    private static RouterLink releaseLink(String text, VersionNumber version) {
        return new RouterLink(text, ReleaseView.class, parameters(version));
    }

    private static List<Product> catalogue(ExplorerFront front) {
        return ExplorerAnswer.catalogue(ExplorerRules.products(front), front);
    }

    // The left panel

    private void buildPanel() {
        panel.addClassNames("explorer-panel", "release-panel");
        var heading = new H2(getTranslation("explorer.lookup.heading"));
        heading.addClassName("explorer-panel-heading");
        lookup.addClassNames("explorer-search", "release-lookup");
        lookup.setAriaLabel(getTranslation("explorer.lookup.heading"));
        lookup.setHelperText(getTranslation("release.lookup.helper"));
        lookup.addValueChangeListener(change -> {
            if (change.isFromClient()) {
                jump(change.getValue());
            }
        });
        var filter = new Button(VaadinIcon.SEARCH.create(), click -> jump(lookup.getValue()));
        filter.addThemeVariants(ButtonVariant.PRIMARY);
        filter.setAriaLabel(getTranslation("explorer.search.button"));
        filter.addClassName("explorer-filter");
        var form = new Div(lookup, filter);
        form.addClassName("explorer-form");
        var section = new Section(heading, form);
        section.addClassName("explorer-lookup");
        profile.addClassName("explorer-profile");
        panel.add(section, profile);
    }

    /** A full version is a place and opens its release; anything else is a question for the explorer. */
    private void jump(String typed) {
        UI ui = UI.getCurrentOrThrow();
        Optional<VersionNumber> version = ExplorerRules.fullVersion(typed);
        if (version.isPresent()) {
            ui.navigate(ReleaseView.class, parameters(version.get()));
        } else {
            ExplorerView.search(typed).go(ui);
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String line = event.getRouteParameters().get(LINE).orElse("");
        VersionNumber version = VersionNumber.of(event.getRouteParameters().get(VERSION).orElse(""));
        if (!version.line().equals(line)) {
            // A release is answered under its real line, the address corrected in place.
            event.forwardTo(ReleaseView.class, parameters(version));
            return;
        }
        lookup.setValue(version.toString());
        try {
            selection.useDefault(ExplorerAnswer.defaultOff(catalogue(data.explorerFront())));
        } catch (DataUnreachableException unreachable) {
            // The outage is said where the card would be.
        }
        Optional<VersionNumber> next = Optional.of(version);
        if (next.equals(shown.peek())) {
            draw(version, selection.off().peek());
        } else {
            shown.set(next);
        }
    }

    // Drawing

    private void draw(VersionNumber version, Set<String> off) {
        body.removeAll();
        profile.removeAll();
        footer.setVisible(false);
        ExplorerFront front;
        Optional<ExplorerVersion> record;
        try {
            front = data.explorerFront();
            record = data.explorerVersion(version);
        } catch (DataUnreachableException unreachable) {
            body.add(new DataOutageNotice());
            return;
        }
        Optional<Line> own = ReleaseRules.lineOf(front, version);
        if (record.isPresent()) {
            card(front, record.get(), off);
        } else if (own.isPresent() && own.get().releases().contains(version)) {
            unpublished(own.get(), version);
        } else {
            unmeasured(front, version);
        }
    }

    // A measured release

    private void card(ExplorerFront front, ExplorerVersion record, Set<String> chosen) {
        List<Product> registry = ExplorerRules.products(front);
        List<Product> catalogue = ExplorerAnswer.catalogue(registry, front);
        Line line = ReleaseRules.asLine(front, record);
        boolean twoShapes = ReleaseRules.measuredWithoutSpring(record);
        String starter = catalogue.stream().filter(Product::switchesShape).map(Product::name).findFirst().orElse("");
        Set<String> off = new LinkedHashSet<>(chosen);
        if (!twoShapes) {
            // Only one shape was measured, so the starter cannot be taken out here, D-216.
            off.remove(starter);
        }
        Shape shape = ExplorerAnswer.shape(front, catalogue, off);
        Split split = ExplorerAnswer.split(line, shape, Asked.nothing());
        ExplorerRules.Badge state = ExplorerAnswer.state(line, shape);

        drawProducts(line, catalogue, shape, off, chosen, starter, twoShapes, record.version());

        var summary = new Div();
        summary.addClassNames("explorer-row-summary", "release-card-top");
        var name = new Span(record.version().toString());
        name.addClassNames("explorer-row-line", "release-card-version");
        var in = new Span(getTranslation("release.card.in", line.line()));
        in.addClassNames("explorer-row-version", "release-card-line");
        summary.add(name, in);
        record.scannedAt().map(OffsetDateTime::toLocalDate).ifPresent(day -> {
            var scanned = new Span(getTranslation("explorer.row.scannedOn", day.toString()));
            scanned.addClassNames("explorer-row-scanned", "release-card-scanned");
            scanned.getElement().setAttribute("title", getTranslation("explorer.row.scanned.title"));
            summary.add(scanned);
        });
        summary.add(parts.vaadinBadge(state));
        ExplorerAnswer.springState(line, shape).ifPresent(theirs -> {
            String against = record.springBootDeclared().or(record::springBoot).map(VersionNumber::toString)
                    .orElseGet(() -> getTranslation("release.badge.spring.measured"));
            summary.add(parts.springBadge(theirs, getTranslation("release.badge.spring.title", against)));
        });
        parts.sentence(line, shape, state).ifPresent(text -> {
            var says = new Span(text);
            says.addClassName("explorer-row-says");
            summary.add(says);
        });
        summary.add(parts.rings(line, catalogue, shape, Asked.nothing()));

        var detail = new Div();
        detail.addClassNames("explorer-detail", "release-detail");
        if (!split.measured()) {
            detail.add(SeriesParts.hint(getTranslation("explorer.detail.unmeasured")));
        } else {
            detail.add(SeriesParts.lead("explorer-detail-summary", getTranslation("explorer.summary.all",
                    split.ours().size() + split.theirs().size() + split.build().size(),
                    getTranslation("explorer.summary.split", split.ours().risk().size(),
                            split.ours().pending().size(), split.theirs().size(), split.build().size(),
                            split.ours().ruled().size()))));
            advice(front, record, line, shape, split).ifPresent(detail::add);
            detail.add(new ReleaseFiles(data, record.version(), ReleaseRules.files(record.scannerFiles()), true));
            Set<String> optional = Set.copyOf(front.profile().map(ExplorerFront.Profile::off).orElse(List.of()));
            parts.halves(detail, line, split, new FindingGrid.Routes(registry, shape, shape.hidden(line), optional),
                    true);
        }

        var card = new Details();
        card.setSummary(summary);
        card.add(detail);
        card.setOpened(true);
        card.addClassNames("explorer-row", "release-card", "state-" + state.name().toLowerCase(java.util.Locale.ROOT));
        card.getElement().setAttribute("data-version", record.version().toString());
        body.add(card);

        footer.setVisible(true);
        footer.setText(record.scannedAt().map(OffsetDateTime::toLocalDate)
                .map(day -> getTranslation("release.footer.scanned", day.toString()) + " ")
                .orElse("") + getTranslation("release.footer"));
    }

    /**
     * The products of this release in the left panel: every product of the
     * catalogue, each counted on this release as if it were on, D-216. A chip
     * changes the session's choice, which the explorer shares, and the card
     * redraws in place: the address of this page names a release, not
     * products.
     */
    private void drawProducts(Line line, List<Product> catalogue, Shape shape, Set<String> off, Set<String> chosen,
            String starter, boolean twoShapes, VersionNumber version) {
        var titleRow = new Div();
        titleRow.addClassName("explorer-profile-title");
        var title = new H2(getTranslation("explorer.profile.heading"));
        title.addClassName("explorer-panel-heading");
        var mark = new Button(VaadinIcon.INFO_CIRCLE_O.create());
        mark.addThemeVariants(ButtonVariant.TERTIARY);
        mark.addClassName("explorer-hint-mark");
        mark.setAriaLabel(getTranslation("release.profile.hint"));
        mark.setTooltipText(getTranslation("release.profile.hint"));
        titleRow.add(title, mark);

        var chips = new Div();
        chips.addClassName("explorer-chips");
        for (Product product : catalogue) {
            String name = product.name();
            boolean cannot = shape.cannot().contains(name);
            boolean stillStarter = !twoShapes && name.equals(starter);
            boolean isOn = shape.on().contains(name);
            int brings = cannot ? 0 : ExplorerAnswer.arrivesThrough(line, name, shape.with(name), Asked.nothing())
                    .size();
            chips.add(chip(product, isOn, cannot, stillStarter, brings, chosen, version));
        }
        profile.add(titleRow, chips);
    }

    private Component chip(Product product, boolean isOn, boolean cannot, boolean stillStarter, int brings,
            Set<String> chosen, VersionNumber version) {
        boolean still = product.fixed() || cannot || stillStarter;
        var chip = new NativeButton();
        chip.addClassNames("explorer-chip", isOn ? "on" : "off");
        if (product.fixed() || stillStarter) {
            chip.addClassName("fixed");
        }
        if (cannot) {
            chip.addClassName("impossible");
        }
        chip.getElement().setAttribute("role", "switch");
        chip.getElement().setAttribute("aria-checked", String.valueOf(isOn));
        chip.getElement().setAttribute("data-product", product.name());
        if (still) {
            chip.setEnabled(false);
            chip.getElement().setAttribute("aria-disabled", "true");
        } else {
            chip.addClickListener(click -> {
                var next = new LinkedHashSet<>(chosen);
                if (isOn) {
                    next.add(product.name());
                } else {
                    next.remove(product.name());
                }
                selection.select(next);
            });
        }
        var box = new Span();
        box.addClassNames("chip-box", isOn ? "ticked" : "none");
        box.getElement().setAttribute("aria-hidden", "true");
        var name = new Span(product.name());
        name.addClassName("chip-name");
        chip.add(box, name);
        if (product.discontinued()) {
            var mark = new Span(getTranslation("explorer.products.discontinued"));
            mark.addClassName("chip-discontinued");
            chip.add(mark);
        }
        var number = new Span(String.valueOf(brings));
        number.addClassName("chip-count");
        if (isOn && brings > 0) {
            number.addClassNames("product-ring", "ring-" + product.ring());
        } else {
            number.addClassName("none");
        }
        chip.add(number);
        chip.getElement().setAttribute("title", stillStarter
                ? getTranslation("release.chip.starter", version.toString())
                : chipTitle(product, isOn, cannot));
        return chip;
    }

    private String chipTitle(Product product, boolean isOn, boolean cannot) {
        String gone = product.discontinued() ? getTranslation("explorer.chip.gone") + " " : "";
        if (cannot) {
            return gone + getTranslation("explorer.chip.cannot");
        }
        if (product.fixed()) {
            return gone + getTranslation("release.chip.fixed");
        }
        return gone + getTranslation(isOn ? "release.chip.on" : "release.chip.off") + " "
                + getTranslation("explorer.chip.narrows");
    }

    /**
     * What to do about it, D-218: the Vaadin half moves with an update within
     * the series, counted over the reader's own findings, and the half Spring
     * Boot brings moves with Spring Boot, which no Vaadin release can do.
     */
    private Optional<Component> advice(ExplorerFront front, ExplorerVersion record, Line line, Shape shape,
            Split split) {
        var advice = new Div();
        advice.addClassName("release-advice");
        Optional<VersionNumber> newest = record.newest();
        if (newest.isPresent() && newest.get().compareTo(record.version()) > 0) {
            List<String> removed = ReleaseRules.removedBy(record, line, shape);
            Paragraph says;
            if (removed.isEmpty()) {
                says = SeriesParts.lead("release-advice-update",
                        getTranslation("release.advice.removesNone", newest.get().toString()));
            } else {
                String named = AdvisoryIds.HERE;
                if (removed.size() > ReleaseRules.NAMED) {
                    named = getTranslation("release.advice.removes.more", named, removed.size() - ReleaseRules.NAMED);
                }
                says = SeriesParts.lead("release-advice-update", "");
                AdvisoryIds.fill(says,
                        getTranslation("release.advice.removes", newest.get().toString(), removed.size(), named),
                        removed.stream().limit(ReleaseRules.NAMED).toList());
            }
            says.add(new Text(" "));
            says.add(releaseLink(getTranslation("release.advice.newer", newest.get().toString()), newest.get()));
            advice.add(says);
        } else if (newest.filter(record.version()::equals).isPresent()) {
            advice.add(SeriesParts.lead("release-advice-update",
                    getTranslation("release.advice.newest", line.line())));
        }
        int spring = split.theirs().size();
        if (spring > 0 && !shape.plain()) {
            advice.add(springAdvice(front, record, spring));
        }
        return advice.getComponentCount() == 0 ? Optional.empty() : Optional.of(advice);
    }

    private Component springAdvice(ExplorerFront front, ExplorerVersion record, int spring) {
        var sentences = new ArrayList<String>();
        sentences.add(getTranslation("release.advice.spring.count", spring));
        Optional<VersionNumber> reference = record.springBoot();
        Optional<VersionNumber> declared = record.springBootDeclared();
        boolean mismatch = reference.isPresent() && declared.isPresent() && !reference.get().equals(declared.get());
        if (mismatch) {
            sentences.add(getTranslation("release.advice.spring.mismatch", declared.get().toString(),
                    reference.get().toString()));
        } else if (reference.isPresent() && declared.isPresent()) {
            sentences.add(getTranslation("release.advice.spring.same", reference.get().toString()));
        } else if (reference.isPresent()) {
            sentences.add(getTranslation("release.advice.spring.reference", reference.get().toString()));
        }
        // No entry in spring_boot_latest means nobody could look, which is said by saying nothing, D-115.
        Optional<ReleaseRules.SpringTargets> targets = ReleaseRules.springTargets(front, record);
        var ways = new ArrayList<String>();
        targets.ifPresent(found -> {
            found.patch().ifPresent(patch -> ways.add(getTranslation("release.advice.spring.patch", patch.toString())));
            found.minor().ifPresent(minor -> ways.add(getTranslation("release.advice.spring.minor", minor.toString())));
        });
        if (ways.isEmpty() && mismatch) {
            ways.add(getTranslation("release.advice.spring.declaredLater", reference.get().toString()));
        }
        if (!ways.isEmpty()) {
            sentences.add(getTranslation("release.advice.spring.targets",
                    String.join(getTranslation("release.advice.spring.and"), ways)));
        } else if (targets.isPresent()) {
            boolean sold = Optional.ofNullable(front.springBootSupply().get(targets.get().from().toString()))
                    .flatMap(supply -> supply.commercial()).isPresent();
            sentences.add(getTranslation(sold ? "release.advice.spring.none.commercial" : "release.advice.spring.none"));
        }
        var says = SeriesParts.lead("release-advice-spring", String.join(" ", sentences));
        says.add(new Text(" "));
        var link = new RouterLink(getTranslation("release.advice.spring.link"), SpringView.class);
        link.addClassName("release-spring-link");
        says.add(link);
        says.add(new Text("."));
        return says;
    }

    // A release without a record

    private void unpublished(Line line, VersionNumber version) {
        body.add(SeriesParts.lead("release-unpublished",
                getTranslation("release.unpublished", version.toString(), line.line())));
        if (line.version().equals(version) && Boolean.TRUE.equals(line.scannerFiles())) {
            body.add(new ReleaseFiles(data, version));
        }
    }

    /**
     * A release nobody measured, answered where it was asked, rule 6.9.7:
     * the release that has an answer, and the line's other measured ones.
     */
    private void unmeasured(ExplorerFront front, VersionNumber version) {
        Optional<Line> own = ReleaseRules.lineOf(front, version);
        Optional<Line> answer = own.or(() -> ReleaseRules.sameMajor(front, version));
        if (answer.isEmpty()) {
            body.add(SeriesParts.lead("release-unmeasured",
                    getTranslation("release.unmeasured.unfollowed", version.toString())));
            var all = new RouterLink(getTranslation("release.unmeasured.all"), ExplorerView.class);
            all.addClassName("release-go");
            var go = new Paragraph(all);
            go.addClassName("explorer-hint");
            body.add(go);
            return;
        }
        Line line = answer.get();
        String sentence;
        if (own.isEmpty()) {
            sentence = getTranslation("release.unmeasured.major", version.toString(), version.line(),
                    ReleaseRules.major(line.line()), line.line(), line.version().toString());
        } else if (version.compareTo(line.version()) > 0) {
            sentence = getTranslation("release.unmeasured.ahead", version.toString(), line.line(),
                    line.version().toString());
        } else {
            sentence = getTranslation("release.unmeasured.line", version.toString(), line.line(),
                    line.version().toString());
        }
        body.add(SeriesParts.lead("release-unmeasured", sentence));
        var go = releaseLink(getTranslation("release.unmeasured.go", line.version().toString()), line.version());
        go.addClassName("release-go");
        var goes = new Paragraph(go);
        goes.addClassName("explorer-hint");
        body.add(goes);

        List<VersionNumber> others = ReleaseRules.others(line, line.version());
        if (!others.isEmpty()) {
            var more = new Paragraph(getTranslation("release.others", line.line()) + " ");
            more.addClassNames("explorer-hint", "release-others");
            List<VersionNumber> named = others.stream().limit(ReleaseRules.OTHERS).toList();
            for (int i = 0; i < named.size(); i++) {
                if (i > 0) {
                    more.add(new Text(", "));
                }
                var link = releaseLink(named.get(i).toString(), named.get(i));
                link.addClassName("release-other");
                more.add(link);
            }
            if (others.size() > ReleaseRules.OTHERS) {
                more.add(new Text(" " + getTranslation("release.others.more", others.size() - ReleaseRules.OTHERS)));
            }
            body.add(more);
        }
    }
}

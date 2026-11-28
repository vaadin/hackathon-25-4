package com.vaadin.securitystatus.ui.team.explorer;

import static com.vaadin.securitystatus.ui.common.Parts.css;
import static com.vaadin.securitystatus.ui.common.Parts.decorative;
import static com.vaadin.securitystatus.ui.common.Parts.paragraph;
import static com.vaadin.securitystatus.ui.team.explorer.SeriesParts.hint;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.securitystatus.data.DataRefreshSignal;
import com.vaadin.securitystatus.data.DataUnreachableException;
import com.vaadin.securitystatus.data.SecurityDataService;
import com.vaadin.securitystatus.data.VersionNumber;
import com.vaadin.securitystatus.data.model.ExplorerFront.History;
import com.vaadin.securitystatus.data.model.ExplorerFront.Line;
import com.vaadin.securitystatus.data.model.ExplorerFront;
import com.vaadin.securitystatus.security.Roles;
import com.vaadin.securitystatus.team.explorer.ExplorerAnswer.Asked;
import com.vaadin.securitystatus.team.explorer.ExplorerAnswer.Shape;
import com.vaadin.securitystatus.team.explorer.ExplorerAnswer.Split;
import com.vaadin.securitystatus.team.explorer.ExplorerAnswer;
import com.vaadin.securitystatus.team.explorer.ExplorerRules.Product;
import com.vaadin.securitystatus.team.explorer.ExplorerRules;
import com.vaadin.securitystatus.team.explorer.ReleaseRules;
import com.vaadin.securitystatus.ui.common.DataOutageNotice;
import com.vaadin.securitystatus.ui.common.nav.Address;
import com.vaadin.securitystatus.ui.common.nav.QueryFlag;
import com.vaadin.securitystatus.ui.team.DrawerContent;
import com.vaadin.securitystatus.ui.team.InternalLayout;

import jakarta.annotation.security.RolesAllowed;

/**
 * The security team's front door, in the prototype's distribution: the left
 * panel holds the filter and the products, the content one row per maintained
 * series, each opened in place under itself, one at a time, D-203, D-204.
 *
 * <p>
 * The address carries the search, {@code ?q}, the opened series, {@code
 * ?open}, and the products turned off, {@code ?off=<name>,<name>}, written by
 * {@link Address}. No {@code ?off} is the default, everything on but what
 * Vaadin no longer develops; a bare {@code ?off} is everything on, D-206. The
 * selection is remembered in {@link ProductSelection} and only ever applied by
 * writing it into the address.
 */
@Route(value = "findings", layout = InternalLayout.class)
@PageTitle("explorer.title")
@Menu(order = 0, title = "explorer.menu")
@RolesAllowed(Roles.SECURITY)
public final class ExplorerView extends Composite<Div> implements BeforeEnterObserver, DrawerContent {

    static final String PATH = "findings";

    private final SecurityDataService data;
    private final ProductSelection selection;
    private final SeriesParts parts;

    private final ValueSignal<String> query = new ValueSignal<>("");
    private final ValueSignal<Optional<String>> opened = new ValueSignal<>(Optional.empty());
    private final ValueSignal<Boolean> legendOpen = new ValueSignal<>(false);

    private final Div panel = new Div();
    private final TextField search = new TextField();
    private final Div note = new Div();
    private final Div profile = new Div();
    private final Paragraph lead = new Paragraph();
    private final H2 heading = new H2();
    private final Div rows = new Div();
    private final Div outage = new Div();

    public ExplorerView(SecurityDataService data, DataRefreshSignal refresh, ProductSelection selection) {
        this.data = data;
        this.selection = selection;
        addClassName("explorer-view");

        this.parts = new SeriesParts(this, data, selection);
        buildPanel();

        var help = parts.legendButton(legendOpen);

        lead.addClassName("explorer-lead");
        heading.addClassName("explorer-series-heading");
        rows.addClassName("explorer-rows");
        var series = css(new Section(heading, rows), "explorer-series");
        var main = css(new Div(help, outage, lead, series), "explorer-main");

        getContent().add(main, parts.legend(legendOpen));
        Shortcuts.addShortcutListener(this, () -> legendOpen.set(false), Key.ESCAPE);

        var generatedAt = refresh.generatedAt();
        var off = selection.off();
        Signal.effect(this, () -> {
            generatedAt.get();
            draw(off.get(), query.get(), opened.get());
        });
        whenAttached(ui -> {
            if (!selection.unsettled()) {
                return null;
            }
            try {
                ExplorerFront front = data.explorerFront();
                Set<String> names = new LinkedHashSet<>();
                catalogue(front).stream().filter(Product::switchable).forEach(product -> names.add(product.name()));
                selection.readStorage(ui, names, restored -> {
                    if (selection.unsettled()) {
                        address(query.peek(), new LinkedHashSet<>(restored), opened.peek()).go(ui);
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

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        QueryParameters asked = event.getLocation().getQueryParameters();
        String q = Address.first(asked, QueryFlag.Q).orElse("");
        Optional<String> open = Address.first(asked, QueryFlag.OPEN).filter(text -> !text.isBlank());
        ExplorerFront front;
        try {
            front = data.explorerFront();
        } catch (DataUnreachableException unreachable) {
            set(q, open);
            return;
        }
        List<Product> catalogue = catalogue(front);
        Set<String> defaults = ExplorerAnswer.defaultOff(catalogue);
        if (Address.has(asked, QueryFlag.OFF)) {
            List<String> written = Address.list(asked, QueryFlag.OFF);
            List<String> known = ExplorerAnswer.known(written, catalogue);
            selection.select(known);
            if (!known.equals(written) || Set.copyOf(known).equals(defaults)) {
                // Rule 6.2.7: a name that hides nothing here does not stay in the address, and the
                // default is written as no flag at all.
                event.forwardTo(PATH, address(q, new LinkedHashSet<>(known), open).query());
                return;
            }
        } else if (selection.remembers(defaults)) {
            // Rule 6.7.5: what the reader chose earlier is written into the address, never applied behind it.
            event.forwardTo(PATH, address(q, selection.off().peek(), open).query());
            return;
        } else {
            selection.useDefault(defaults);
        }
        set(q, open);
    }

    private void set(String q, Optional<String> open) {
        if (!q.equals(query.peek())) {
            query.set(q);
        }
        if (!open.equals(opened.peek())) {
            opened.set(open);
        }
        if (!search.getValue().equals(q)) {
            search.setValue(q);
        }
    }

    // Writing the address

    /** The explorer's address for a search alone, which the release page sends a search to. */
    static Address search(String q) {
        return Address.of(PATH).with(QueryFlag.Q, q.isBlank() ? Optional.empty() : Optional.of(q.strip()));
    }

    /**
     * The explorer's address for a search, a selection and an opened series:
     * no {@code ?off} when the selection is the default, a bare one when
     * everything is on, the names in catalogue order otherwise.
     */
    private Address address(String q, Set<String> off, Optional<String> open) {
        return address(q, off, open, false);
    }

    /**
     * The same, and with {@code said} the selection is written even when it is
     * the default: a control that changes the products says so in the address,
     * which the view then writes away, so the remembered selection follows the
     * reader back to the default rather than being restored over it.
     */
    private Address address(String q, Set<String> off, Optional<String> open, boolean said) {
        Address address = search(q).with(QueryFlag.OPEN, open);
        try {
            List<Product> catalogue = catalogue(data.explorerFront());
            if (!said && off.equals(ExplorerAnswer.defaultOff(catalogue))) {
                return address;
            }
            if (off.isEmpty()) {
                return address.with(QueryFlag.OFF, Optional.of(""));
            }
            return address.list(QueryFlag.OFF, ExplorerAnswer.known(new ArrayList<>(off), catalogue));
        } catch (DataUnreachableException unreachable) {
            return address.list(QueryFlag.OFF, new ArrayList<>(off));
        }
    }

    private void go(Set<String> off) {
        address(query.peek(), off, opened.peek(), true).go(UI.getCurrentOrThrow());
    }

    private void submit(String typed) {
        UI ui = UI.getCurrentOrThrow();
        Optional<VersionNumber> version = ExplorerRules.fullVersion(typed);
        if (version.isPresent()) {
            // The source shortcut: a full version is a release to read, not a filter, D-103.
            ui.navigate(ReleaseView.class, ReleaseView.parameters(version.get()));
            return;
        }
        address(typed, selection.off().peek(), opened.peek()).go(ui);
    }

    private static List<Product> catalogue(ExplorerFront front) {
        return ExplorerAnswer.catalogue(ExplorerRules.products(front), front);
    }

    // The left panel

    private void buildPanel() {
        panel.addClassName("explorer-panel");

        var lookupHeading = css(new H2(getTranslation("explorer.lookup.heading")), "explorer-panel-heading");
        search.addClassName("explorer-search");
        search.setAriaLabel(getTranslation("explorer.lookup.heading"));
        search.setClearButtonVisible(true);
        search.addValueChangeListener(change -> {
            if (change.isFromClient()) {
                submit(change.getValue());
            }
        });
        var filter = new Button(VaadinIcon.SEARCH.create(), click -> submit(search.getValue()));
        filter.addThemeVariants(ButtonVariant.PRIMARY);
        filter.setAriaLabel(getTranslation("explorer.search.button"));
        filter.addClassName("explorer-filter");
        var form = css(new Div(search, filter), "explorer-form");
        note.addClassName("explorer-note");
        var lookup = css(new Section(lookupHeading, form, note), "explorer-lookup");

        profile.addClassName("explorer-profile");
        panel.add(lookup, profile);
    }

    // Drawing

    private void draw(Set<String> off, String q, Optional<String> open) {
        outage.removeAll();
        profile.removeAll();
        rows.removeAll();
        note.removeAll();
        ExplorerFront front;
        try {
            front = data.explorerFront();
        } catch (DataUnreachableException unreachable) {
            outage.add(new DataOutageNotice());
            lead.setVisible(false);
            heading.setVisible(false);
            return;
        }
        heading.setVisible(true);
        List<Product> registry = ExplorerRules.products(front);
        List<Product> catalogue = ExplorerAnswer.catalogue(registry, front);
        Shape shape = ExplorerAnswer.shape(front, catalogue, off);
        Asked asked = ExplorerAnswer.asked(q, front);

        drawProducts(front, catalogue, off, shape, asked);
        drawNote(asked);

        var counts = new LinkedHashMap<ExplorerRules.Badge, Integer>();
        int shown = 0;
        for (Line line : front.lines()) {
            if (!asked.keeps(line)) {
                continue;
            }
            Split split = ExplorerAnswer.split(line, shape, asked);
            if (asked.ids().isPresent() && split.ours().size() + split.theirs().size() == 0) {
                continue;
            }
            shown++;
            ExplorerRules.Badge state = ExplorerAnswer.state(line, shape);
            counts.merge(state, 1, Integer::sum);
            boolean isOpen = open.filter(line.line()::equals).isPresent();
            rows.add(row(front, line, catalogue, shape, asked, state, isOpen));
        }

        int all = front.lines().size();
        heading.setText(asked.raw().isEmpty() ? getTranslation("explorer.heading", all)
                : getTranslation("explorer.heading.filtered"));
        lead.setVisible(!asked.raw().isEmpty());
        if (!asked.raw().isEmpty()) {
            lead.setText(leadText(asked, shown, all, counts));
        }
    }

    private String leadText(Asked asked, int shown, int all, Map<ExplorerRules.Badge, Integer> counts) {
        if (shown == 0) {
            return ExplorerAnswer.number(asked.raw())
                    ? getTranslation("explorer.lead.unfollowed", all, asked.raw())
                    : getTranslation("explorer.lead.none", all, asked.raw());
        }
        var said = new ArrayList<String>();
        if (counts.getOrDefault(ExplorerRules.Badge.VULNERABLE, 0) > 0) {
            said.add(getTranslation("explorer.lead.vulnerable", counts.get(ExplorerRules.Badge.VULNERABLE)));
        }
        if (counts.getOrDefault(ExplorerRules.Badge.UNJUDGED, 0) > 0) {
            said.add(getTranslation("explorer.lead.unjudged", counts.get(ExplorerRules.Badge.UNJUDGED)));
        }
        if (counts.getOrDefault(ExplorerRules.Badge.NOT_AFFECTED, 0) > 0) {
            said.add(getTranslation("explorer.lead.clear", counts.get(ExplorerRules.Badge.NOT_AFFECTED)));
        }
        return shown == all ? getTranslation("explorer.lead.all", all, asked.raw(), String.join(", ", said))
                : getTranslation("explorer.lead.some", shown, all, asked.raw(), String.join(", ", said));
    }

    /** What the search found or did not, said under the field, D-209. */
    private void drawNote(Asked asked) {
        if (asked.raw().isEmpty()) {
            return;
        }
        if (!asked.found()) {
            if (ExplorerAnswer.number(asked.raw())) {
                note.add(says(getTranslation("explorer.note.unfollowed", asked.raw())),
                        hint(getTranslation("explorer.note.unfollowed.hint")));
            } else {
                note.add(says(getTranslation("explorer.note.nothing")),
                        hint(getTranslation("explorer.note.nothing.hint")));
            }
            return;
        }
        int ids = asked.ids().map(Set::size).orElse(0);
        List<History> past = ids == 0 ? asked.past().stream().limit(8).toList() : List.of();
        for (History entry : past) {
            var line = css(new Paragraph(entry.cve() + ": "), "explorer-note-says");
            boolean first = true;
            for (var span : new java.util.TreeMap<>(entry.lines()).entrySet()) {
                String until = span.getValue().until().map(VersionNumber::toString).orElse("");
                line.add((first ? "" : " ") + getTranslation("explorer.past.until", span.getKey(), until) + " ");
                first = false;
                Optional<VersionNumber> fixed = span.getValue().fixedIn();
                if (fixed.isPresent()) {
                    line.add(getTranslation("explorer.past.gone", fixed.get().toString()) + " ");
                    line.add(releaseLink(span.getKey(), fixed.get(),
                            getTranslation("explorer.past.see", fixed.get().toString())));
                    line.add(".");
                } else {
                    line.add(getTranslation("explorer.past.kept"));
                }
            }
            note.add(line);
        }
        if (!past.isEmpty()) {
            String more = asked.past().size() > past.size()
                    ? " " + getTranslation("explorer.past.more", asked.past().size() - past.size())
                    : "";
            note.add(hint(getTranslation("explorer.past.hint") + more));
        }
        if (asked.kind() == ExplorerAnswer.Kind.VERSION) {
            note.add(hint(getTranslation("explorer.note.series", String.join(", ", asked.series()))));
            return;
        }
        if (ids == 0) {
            return;
        }
        String advisories = getTranslation("explorer.count.advisories", ids);
        String text = asked.kind() == ExplorerAnswer.Kind.PACKAGE
                ? getTranslation("explorer.note.package", asked.raw(), advisories)
                : getTranslation("explorer.note.ids", advisories, asked.raw());
        if (!asked.past().isEmpty()) {
            text += getTranslation("explorer.note.past", getTranslation("explorer.count.others", asked.past().size()));
        } else {
            text += ".";
        }
        note.add(hint(text));
    }

    // The products

    private void drawProducts(ExplorerFront front, List<Product> catalogue, Set<String> off, Shape shape,
            Asked asked) {
        Set<String> defaults = ExplorerAnswer.defaultOff(catalogue);
        Set<String> plainOnly = shape.plain() ? Set.of(shape.starter()) : Set.of();
        Shape everything = ExplorerAnswer.shape(front, catalogue, plainOnly);

        var chips = css(new Div(), "explorer-chips");
        for (Product product : catalogue) {
            String name = product.name();
            boolean cannot = shape.cannot().contains(name);
            boolean isOn = product.fixed() || (!cannot && !off.contains(name));
            int brings = cannot ? 0 : ExplorerAnswer.chipCount(front, name, shape, asked);
            boolean decided = off.contains(name) != defaults.contains(name);
            boolean offer = product.fixed() || cannot || decided || brings > 0
                    || ExplorerAnswer.chipCount(front, name, everything, Asked.nothing()) > 0;
            if (offer) {
                chips.add(chip(product, isOn, cannot, brings, off));
            }
        }

        var titleRow = css(new Div(), "explorer-profile-title");
        var title = css(new H2(getTranslation("explorer.profile.heading")), "explorer-panel-heading");
        titleRow.add(title);
        if (chips.getComponentCount() == 0) {
            profile.add(titleRow, hint(getTranslation("explorer.profile.none")));
            return;
        }
        var mark = new Button(VaadinIcon.INFO_CIRCLE_O.create());
        mark.addThemeVariants(ButtonVariant.TERTIARY);
        mark.addClassName("explorer-hint-mark");
        mark.setAriaLabel(getTranslation("explorer.profile.hint"));
        mark.setTooltipText(getTranslation("explorer.profile.hint"));
        titleRow.add(mark);

        profile.add(titleRow, allBox(catalogue, off, shape), chips);
    }

    /**
     * The box at the head of the list, D-206: ticked when every product
     * counted by default is on, a dash when some are, empty when none.
     * Ticked, it turns them all off; otherwise it goes back to the default.
     */
    private Component allBox(List<Product> catalogue, Set<String> off, Shape shape) {
        List<String> counted = ExplorerAnswer.countedByDefault(catalogue).stream()
                .filter(name -> !shape.cannot().contains(name)).toList();
        long on = counted.stream().filter(name -> !off.contains(name)).count();
        boolean all = !counted.isEmpty() && on == counted.size();
        String state = all ? "ticked" : on > 0 ? "some" : "none";

        Set<String> target;
        if (all) {
            target = new LinkedHashSet<>(off);
            catalogue.stream().filter(Product::switchable).forEach(product -> target.add(product.name()));
        } else {
            target = ExplorerAnswer.defaultOff(catalogue);
        }
        var link = new RouterLink(ExplorerView.class);
        link.setQueryParameters(address(query.peek(), target, opened.peek(), true).query());
        link.addClassNames("explorer-all", state);
        link.getElement().setAttribute("aria-label",
                getTranslation(all ? "explorer.all.off" : "explorer.all.on"));
        link.getElement().setAttribute("data-state", state);
        var box = decorative(new Span(), "chip-box", state);
        link.add(box);

        var product = css(new Span(getTranslation("explorer.profile.product")), "explorer-col-name");
        var advisories = css(new Span(getTranslation("explorer.profile.advisories")), "explorer-col-count");
        var head = css(new Div(link, product, advisories), "explorer-chips-head");
        head.setVisible(!counted.isEmpty());
        return head;
    }

    /**
     * One product: a real link to the address with this product toggled, rule
     * 6.2.13, or, for one the reader cannot change, a still mark; its box, its
     * name, and its number in its ring, D-207.
     */
    private Component chip(Product product, boolean isOn, boolean cannot, int brings, Set<String> off) {
        boolean still = product.fixed() || cannot;
        Component chip;
        if (still) {
            chip = new Span();
            chip.getElement().setAttribute("aria-disabled", "true");
        } else {
            var next = new LinkedHashSet<>(off);
            if (isOn) {
                next.add(product.name());
            } else {
                next.remove(product.name());
            }
            var link = new RouterLink(ExplorerView.class);
            link.setQueryParameters(address(query.peek(), next, opened.peek(), true).query());
            chip = link;
        }
        var element = chip.getElement();
        element.getClassList().addAll(List.of("explorer-chip", isOn ? "on" : "off"));
        if (product.fixed()) {
            element.getClassList().add("fixed");
        }
        if (cannot) {
            element.getClassList().add("impossible");
        }
        element.setAttribute("role", "switch");
        element.setAttribute("aria-checked", String.valueOf(isOn));
        element.setAttribute("data-product", product.name());

        for (Component part : parts.chipParts(product, isOn, brings)) {
            element.appendChild(part.getElement());
        }
        Tooltip.forComponent(chip).setText(parts.chipTitle("explorer", product, isOn, cannot));
        return chip;
    }


    // The series rows

    private Component row(ExplorerFront front, Line line, List<Product> catalogue, Shape shape, Asked asked,
            ExplorerRules.Badge state, boolean isOpen) {
        var summary = css(new Div(), "explorer-row-summary");
        var name = css(new Span(line.line()), "explorer-row-line");
        var version = css(new Span(line.version().toString()), "explorer-row-version");
        summary.add(name, version);
        line.scannedAt().map(OffsetDateTime::toLocalDate).ifPresent(day -> {
            var scanned = css(new Span(getTranslation("explorer.row.scannedOn", day.toString())), "explorer-row-scanned");
            Tooltip.forComponent(scanned).setText(getTranslation("explorer.row.scanned.title"));
            summary.add(scanned);
        });
        summary.add(parts.vaadinBadge(state));
        ExplorerAnswer.springState(line, shape).ifPresent(theirs -> {
            String against = line.springBootDeclared().or(line::springBoot).map(VersionNumber::toString)
                    .orElseGet(() -> getTranslation("explorer.badge.spring.declares"));
            summary.add(parts.springBadge(theirs, getTranslation("explorer.badge.spring.title", against)));
        });
        parts.sentence(line, shape, state).ifPresent(text -> {
            var says = css(new Span(text), "explorer-row-says");
            summary.add(says);
        });
        summary.add(parts.rings(line, catalogue, shape, asked));

        var details = new Details();
        details.setSummary(summary);
        details.addClassNames("explorer-row", "state-" + state.name().toLowerCase(java.util.Locale.ROOT));
        details.getElement().setAttribute("data-line", line.line());
        details.setOpened(isOpen);
        if (isOpen) {
            details.add(detail(front, line, catalogue, shape, asked));
        }
        details.addOpenedChangeListener(change -> {
            if (change.isFromClient()) {
                // One at a time, and the one the address names, D-204.
                address(query.peek(), selection.off().peek(),
                        change.isOpened() ? Optional.of(line.line()) : Optional.empty()).go(UI.getCurrentOrThrow());
            }
        });
        return details;
    }

    // The opened series

    private Component detail(ExplorerFront front, Line line, List<Product> catalogue, Shape shape, Asked asked) {
        var detail = css(new Div(), "explorer-detail");
        detail.getElement().setAttribute("data-line", line.line());

        // No title, no close button and no Spring switch: the row above names the line and release, its
        // chevron shuts it and the starter's chip in the side panel chooses the shape, D-238, D-265, D-266.

        Split split = ExplorerAnswer.split(line, shape, asked);
        if (!split.measured()) {
            detail.add(hint(getTranslation("explorer.detail.unmeasured")));
            return detail;
        }
        detail.add(paragraph("explorer-detail-summary", getTranslation("explorer.summary.all",
                split.ours().size() + split.theirs().size() + split.build().size(),
                getTranslation("explorer.summary.split", split.ours().risk().size(), split.ours().pending().size(),
                        split.theirs().size(), split.build().size(), split.ours().ruled().size()))));
        if (shape.plain()) {
            detail.add(hint(getTranslation("explorer.detail.plain")));
        }

        Set<String> optional = Set.copyOf(front.profile().map(ExplorerFront.Profile::off).orElse(List.of()));
        var routes = new FindingList.Routes(catalogue, shape, shape.hidden(line), optional);
        parts.halves(detail, line, split, routes, false);

        detail.add(new ReleaseFiles(data, line.version(), ReleaseRules.files(line.scannerFiles()), true));
        others(line).ifPresent(detail::add);

        var release = css(new RouterLink(getTranslation("explorer.detail.release", line.version().toString()),
                ReleaseView.class, new RouteParameters(Map.of(ReleaseView.LINE, line.line(), ReleaseView.VERSION,
                        line.version().toString()))), "explorer-release-link");
        detail.add(release);
        return detail;
    }

    /** The other releases of the series this row answers for, each a link to its page, D-212. */
    private Optional<Component> others(Line line) {
        List<VersionNumber> others = line.releases().stream().filter(release -> !release.equals(line.version()))
                .toList();
        if (others.isEmpty()) {
            return Optional.empty();
        }
        var list = css(new Paragraph(getTranslation("explorer.releases", line.line()) + " "), "explorer-hint", "explorer-releases");
        for (int i = 0; i < others.size(); i++) {
            if (i > 0) {
                list.add(", ");
            }
            list.add(releaseLink(line.line(), others.get(i), others.get(i).toString()));
        }
        return Optional.of(list);
    }

    private static RouterLink releaseLink(String line, VersionNumber version, String text) {
        return new RouterLink(text, ReleaseView.class,
                new RouteParameters(Map.of(ReleaseView.LINE, line, ReleaseView.VERSION, version.toString())));
    }

    // Small parts

    private static Paragraph says(String text) {
        return paragraph("explorer-note-says", text);
    }
}

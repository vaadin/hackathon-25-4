package com.vaadin.securitystatus.ui.team.explorer;

import static com.vaadin.securitystatus.ui.common.Parts.css;
import static com.vaadin.securitystatus.ui.common.Parts.decorative;
import static com.vaadin.securitystatus.ui.common.Parts.paragraph;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.details.DetailsVariant;
import com.vaadin.flow.component.html.Aside;
import com.vaadin.flow.component.html.DescriptionList;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.securitystatus.data.DataUnreachableException;
import com.vaadin.securitystatus.data.SecurityDataService;
import com.vaadin.securitystatus.data.VersionNumber;
import com.vaadin.securitystatus.data.model.ExplorerFront;
import com.vaadin.securitystatus.data.model.ExplorerFront.Finding;
import com.vaadin.securitystatus.data.model.ExplorerFront.Line;
import com.vaadin.securitystatus.data.model.ExplorerVersion;
import com.vaadin.securitystatus.team.explorer.ExplorerAnswer.Asked;
import com.vaadin.securitystatus.team.explorer.ExplorerAnswer.Groups;
import com.vaadin.securitystatus.team.explorer.ExplorerAnswer.Shape;
import com.vaadin.securitystatus.team.explorer.ExplorerAnswer.Split;
import com.vaadin.securitystatus.team.explorer.ExplorerAnswer;
import com.vaadin.securitystatus.team.explorer.ExplorerRules.Product;
import com.vaadin.securitystatus.team.explorer.ExplorerRules;
import com.vaadin.securitystatus.ui.common.DataOutageNotice;
import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.dom.Element;

/**
 * What a series row and an opened series draw, shared by the explorer and the
 * release page, so the card of one release and the row of its series are one
 * card with a different subject, as the prototype draws them, D-216: the
 * badges, the sentence, the rings, the two halves with their groups and hints,
 * the build only group, and the legend the "?" opens.
 */
final class SeriesParts {

    static final List<String> KINDS = List.of("RISK", "UNJUDGED", "ANSWERED");

    private final Component owner;
    private final SecurityDataService data;
    private final ProductSelection selection;

    SeriesParts(Component owner, SecurityDataService data, ProductSelection selection) {
        this.owner = owner;
        this.data = data;
        this.selection = selection;
    }

    // A line's row and its detail

    /** What follows the name in a line's summary: when it was scanned, the two badges, the sentence and the rings. */
    void summaryTail(Div summary, Optional<OffsetDateTime> scannedAt, Line line, Shape shape,
            ExplorerRules.LineState state, List<Product> catalogue, Asked asked, String springTitle) {
        scannedAt.map(OffsetDateTime::toLocalDate).ifPresent(day -> {
            var scanned = css(new Span(owner.getTranslation("explorer.row.scannedOn", day.toString())),
                    "explorer-row-scanned");
            Tooltip.forComponent(scanned).setText(owner.getTranslation("explorer.row.scanned.title"));
            summary.add(scanned);
        });
        summary.add(vaadinBadge(state));
        ExplorerAnswer.springState(line, shape).ifPresent(theirs -> summary.add(springBadge(theirs, springTitle)));
        sentence(line, shape, state).ifPresent(text -> summary.add(css(new Span(text), "explorer-row-says")));
        summary.add(rings(line, catalogue, shape, asked));
    }

    /** The detail's first sentence: how many findings the line has, and how they split. */
    Paragraph splitSummary(Split split) {
        return paragraph("explorer-detail-summary", owner.getTranslation("explorer.summary.all",
                split.ours().size() + split.theirs().size() + split.build().size(),
                owner.getTranslation("explorer.summary.split", split.ours().risk().size(),
                        split.ours().triage().size(), split.theirs().size(), split.build().size(),
                        split.ours().ruledOut().size())));
    }

    /** What every finding row of a line is drawn against: the products, the shape and the optional ones. */
    static FindingList.RowContext rowContext(ExplorerFront front, List<Product> catalogue, Shape shape, Line line) {
        Set<String> optional = Set.copyOf(front.profile().map(ExplorerFront.Profile::off).orElse(List.of()));
        return new FindingList.RowContext(catalogue, shape, shape.hidden(line), optional);
    }

    // The left panel

    /**
     * The panel's search: its heading, the field and the button, both of which
     * hand what was typed to {@code submit}, then whatever the page shows under it.
     */
    Section lookup(TextField field, Consumer<String> submit, Component... after) {
        var heading = css(new H2(owner.getTranslation("explorer.lookup.heading")), "explorer-panel-heading");
        field.addClassName("explorer-search");
        field.setAriaLabel(owner.getTranslation("explorer.lookup.heading"));
        field.addValueChangeListener(change -> {
            if (change.isFromClient()) {
                submit.accept(change.getValue());
            }
        });
        var filter = css(new Button(VaadinIcon.SEARCH.create(), click -> submit.accept(field.getValue())),
                "explorer-filter");
        filter.addThemeVariants(ButtonVariant.PRIMARY);
        filter.setAriaLabel(owner.getTranslation("explorer.search.button"));
        var section = css(new Section(heading, css(new Div(field, filter), "explorer-form")), "explorer-lookup");
        section.add(after);
        return section;
    }

    /** The products' title row, with nothing to explain when there are no products to choose. */
    Div profileTitle() {
        return css(new Div(css(new H2(owner.getTranslation("explorer.profile.heading")), "explorer-panel-heading")),
                "explorer-profile-title");
    }

    /** The products' title row with the mark that explains what choosing them does. */
    Div profileTitle(String hintKey) {
        var mark = css(new Button(VaadinIcon.INFO_CIRCLE_O.create()), "explorer-hint-mark");
        mark.addThemeVariants(ButtonVariant.TERTIARY);
        mark.setAriaLabel(owner.getTranslation(hintKey));
        mark.setTooltipText(owner.getTranslation(hintKey));
        var row = profileTitle();
        row.add(mark);
        return row;
    }

    // The product chips

    /** A product chip's classes and switch semantics, whatever element it is drawn as. */
    static void dressChip(Element chip, Product product, boolean isOn, boolean fixed, boolean cannot) {
        chip.getClassList().addAll(List.of("explorer-chip", isOn ? "on" : "off"));
        if (fixed) {
            chip.getClassList().add("fixed");
        }
        if (cannot) {
            chip.getClassList().add("impossible");
        }
        chip.setAttribute("role", "switch");
        chip.setAttribute("aria-checked", String.valueOf(isOn));
        chip.setAttribute("data-product", product.name());
    }

    /** The products left off once a chip is clicked: one that was on goes off, one that was off comes back. */
    static Set<String> toggled(Set<String> off, String name, boolean isOn) {
        var next = new LinkedHashSet<>(off);
        if (isOn) {
            next.add(name);
        } else {
            next.remove(name);
        }
        return next;
    }

    /** What a product chip holds: its box, its name, the discontinued mark and the count of what it brings. */
    Component[] chipParts(Product product, boolean isOn, int brings) {
        var parts = new ArrayList<Component>();
        parts.add(decorative(new Span(), "chip-box", isOn ? "ticked" : "none"));
        parts.add(css(new Span(product.name()), "chip-name"));
        if (product.discontinued()) {
            parts.add(css(new Span(owner.getTranslation("explorer.products.discontinued")), "chip-discontinued"));
        }
        parts.add(isOn && brings > 0
                ? css(new Span(String.valueOf(brings)), "chip-count", "product-ring", "ring-" + product.ring())
                : css(new Span(String.valueOf(brings)), "chip-count", "none"));
        return parts.toArray(Component[]::new);
    }

    /** A chip's hover text; the screen names its own words for what the number counts. */
    String chipTitle(String screen, Product product, boolean isOn, boolean cannot) {
        String gone = product.discontinued() ? owner.getTranslation("explorer.chip.gone") + " " : "";
        if (cannot) {
            return gone + owner.getTranslation("explorer.chip.cannot");
        }
        if (product.fixed()) {
            return gone + owner.getTranslation(screen + ".chip.fixed");
        }
        return gone + owner.getTranslation(isOn ? screen + ".chip.on" : screen + ".chip.off") + " " + owner.getTranslation("explorer.chip.narrows");
    }

    // The row

    static Badge badge(String side, ExplorerRules.LineState state, String text) {
        return css(new Badge(text), "explorer-badge", side, "badge-" + state.name().toLowerCase(Locale.ROOT));
    }

    /** "Vaadin: <state>" with what the state means on hover. */
    Badge vaadinBadge(ExplorerRules.LineState state) {
        var vaadin = badge("vaadin", state, owner.getTranslation("explorer.badge.vaadin", owner.getTranslation("explorer.state." + state.name())));
        Tooltip.forComponent(vaadin).setText(owner.getTranslation("explorer.state." + state.name() + ".means"));
        return vaadin;
    }

    /** "Spring Boot: <state>", the Spring Boot it was measured against said on hover. */
    Badge springBadge(ExplorerRules.LineState state, String title) {
        var spring = badge("spring", state, owner.getTranslation("explorer.badge.spring", owner.getTranslation("explorer.state." + state.name())));
        Tooltip.forComponent(spring).setText(title);
        return spring;
    }

    /** What the row says beside the badge, counted over the same findings as the badge, D-205. */
    Optional<String> sentence(Line line, Shape shape, ExplorerRules.LineState state) {
        List<Finding> mine = ExplorerAnswer.stateFindings(line, shape);
        long risks = mine.stream().filter(finding -> ExplorerRules.stateOf(finding) == ExplorerRules.LineState.VULNERABLE)
                .count();
        long open = mine.stream().filter(finding -> ExplorerRules.stateOf(finding) == ExplorerRules.LineState.UNJUDGED)
                .count();
        switch (state) {
            case VULNERABLE:
                return Optional.of(open > 0 ? owner.getTranslation("explorer.says.riskOpen", (int) risks, (int) open)
                        : owner.getTranslation("explorer.says.risk", (int) risks));
            case UNJUDGED:
                return Optional.of(!ExplorerAnswer.isShapeMeasured(line, shape) || mine.isEmpty()
                        ? owner.getTranslation("explorer.says.unmeasured")
                        : owner.getTranslation("explorer.says.unjudged", (int) open));
            default:
                Groups theirs = ExplorerAnswer.split(line, shape, Asked.nothing()).theirs();
                int waiting = theirs.risk().size() + theirs.triage().size();
                return waiting > 0 ? Optional.of(owner.getTranslation("explorer.says.spring", waiting)) : Optional.empty();
        }
    }

    /** One ring per product that is on, in catalogue order, with its count, the noughts drawn too, D-207. */
    Component rings(Line line, List<Product> catalogue, Shape shape, Asked asked) {
        var rings = css(new Span(), "explorer-rings");
        for (Product product : catalogue) {
            if (!shape.on().contains(product.name())) {
                continue;
            }
            int count = ExplorerAnswer.arrivesThrough(line, product.name(), shape, asked).size();
            var ring = css(new Span(String.valueOf(count)), "explorer-ring");
            if (count > 0) {
                ring.addClassNames("product-ring", "ring-" + product.ring());
                Tooltip.forComponent(ring).setText(owner.getTranslation("explorer.ring.some", count, product.name()));
            } else {
                ring.addClassName("none");
                Tooltip.forComponent(ring).setText(owner.getTranslation("explorer.ring.none", product.name()));
            }
            ring.getElement().setAttribute("data-product", product.name());
            rings.add(ring);
        }
        return rings;
    }

    // The opened series

    /**
     * The two halves in the prototype's order, D-205, D-212: From Vaadin with
     * its groups, the nothing waiting hint, the frontend and untraced hints,
     * what only switched off products bring, With Spring Boot with its groups,
     * then the build only group the application keeps. {@code release} draws
     * the release page's variant: every group shut on arrival, the switched
     * off sentence in the release's words, and no sentence under With Spring
     * Boot, whose advice the card has already given.
     */
    void halves(Div into, Line line, Split split, FindingList.RowContext routes, boolean release) {
        boolean halves = split.theirs().size() > 0 && !routes.shape().plain();
        if (halves) {
            into.add(halfTitle("explorer.half.ours"));
        }
        groups(into, line, "", split.ours(), routes, release);
        if (split.ours().risk().isEmpty() && split.ours().triage().isEmpty()) {
            into.add(hint(owner.getTranslation("explorer.detail.nothing")));
        }

        // On the release page the prototype counts every finding of Vaadin's with no route, the pinned
        // and the bundled ones too, D-216; the explorer keeps the count it was verified with.
        List<Finding> blind = release
                ? ExplorerAnswer.findingsOf(line, routes.shape(), Asked.nothing()).stream()
                        .filter(finding -> "vaadin".equals(finding.owner().orElse("")) && finding.modules().isEmpty()
                                && ExplorerAnswer.mine(finding, routes.shape(), line))
                        .toList()
                : split.unattributed().stream().filter(finding -> finding.bundledIn().isEmpty()).toList();
        int frontend = (int) blind.stream().filter(ExplorerRules::frontend).count();
        if (frontend > 0) {
            into.add(hint(owner.getTranslation("explorer.detail.frontend", frontend)));
        }
        if (blind.size() - frontend > 0) {
            into.add(hint(owner.getTranslation("explorer.detail.untraced", blind.size() - frontend)));
        }
        if (split.elsewhere() > 0) {
            into.add(hint(owner.getTranslation("explorer.detail.elsewhere", split.elsewhere())));
        }

        if (halves) {
            var half = css(new Div(), "explorer-half");
            half.add(halfTitle("explorer.half.theirs"));
            if (!release) {
                var says = css(new Paragraph(), "explorer-hint");
                String against = line.springBoot().map(VersionNumber::toString)
                        .orElseGet(() -> owner.getTranslation("explorer.half.theirs.unknown"));
                var text = new StringBuilder(owner.getTranslation("explorer.half.theirs.lead", split.theirs().size(), against));
                line.springBootDeclared().filter(declared -> line.springBoot().isPresent()
                        && !declared.equals(line.springBoot().get())).ifPresent(declared -> text.append(' ')
                                .append(owner.getTranslation("explorer.half.theirs.declared", declared.toString())));
                text.append(' ').append(owner.getTranslation("explorer.half.theirs.moves")).append(' ');
                says.add(text.toString());
                var link = css(new RouterLink(owner.getTranslation("explorer.half.theirs.link"), SpringView.class), "explorer-spring-link");
                says.add(link);
                half.add(says);
            }
            groups(half, line, "SPRING_", split.theirs(), routes, release);
            into.add(half);
        }

        if (!split.build().isEmpty()) {
            into.add(group(line, "BUILD", new Span(owner.getTranslation("explorer.group.BUILD", split.build().size())), null,
                    "explorer.group.BUILD.note", split.build(), false, routes));
        }
    }

    private Component halfTitle(String key) {
        var title = css(new Paragraph(owner.getTranslation(key)), "explorer-half-title");
        Tooltip.forComponent(title).setText(owner.getTranslation(key + ".title"));
        return title;
    }

    private void groups(Div into, Line line, String prefix, Groups groups, FindingList.RowContext routes,
            boolean shut) {
        List<List<Finding>> members = List.of(groups.risk(), groups.triage(), groups.ruledOut());
        for (int i = 0; i < KINDS.size(); i++) {
            List<Finding> findings = members.get(i);
            if (findings.isEmpty()) {
                continue;
            }
            String kind = KINDS.get(i);
            // The count in bold and the words after it, as the prototype sets a group's heading.
            var count = css(new Span(String.valueOf(findings.size())), "explorer-group-count");
            var heading = new Span(count, new Span(" " + owner.getTranslation("explorer.half.group." + kind)));
            into.add(group(line, prefix + kind, heading, owner.getTranslation("explorer.half.group." + kind + ".title"), null, findings,
                    !shut && !"ANSWERED".equals(kind), routes));
        }
    }

    private Component group(Line line, String key, Span label, String title, String noteKey,
            List<Finding> findings, boolean startsOpen, FindingList.RowContext routes) {
        label.addClassName("explorer-group-label");
        if (title != null) {
            Tooltip.forComponent(label).setText(title);
        }
        var details = new Details(label);
        details.addThemeVariants(DetailsVariant.AURA_NO_PADDING);
        details.addClassNames("explorer-group", "group-" + key.toLowerCase(Locale.ROOT));
        details.getElement().setAttribute("data-group", key);
        details.setOpened(selection.isOpen(key, startsOpen));
        details.addOpenedChangeListener(change -> {
            if (change.isFromClient()) {
                // Rule 6.7.4: recorded when it happens, keyed on the part of the heading that does not move.
                selection.open(key, change.isOpened());
            }
        });
        if (noteKey != null) {
            details.add(paragraph("explorer-group-note", owner.getTranslation(noteKey)));
        }
        ExplorerRules.Capped capped = ExplorerRules.capped(findings);
        details.add(new FindingList(capped.shown(), routes, finding -> findingDetails(line.version(), finding,
                routes.shape().plain()), cve -> paths(line.version(), cve, routes.shape().plain())));
        if (capped.more() > 0) {
            details.add(paragraph("explorer-group-more", owner.getTranslation("explorer.group.more", capped.more())));
        }
        return details;
    }

    /**
     * The paths each product brings an advisory by, read from the release's
     * own record, which the data layer caches; nothing when it is not
     * published or cannot be reached, and the dot then names only its product.
     */
    private Map<String, List<String>> paths(VersionNumber version, String cve, boolean springOff) {
        try {
            return data.explorerVersion(version).flatMap(record -> record.findings().stream()
                    .filter(candidate -> candidate.cve().equals(cve)).findFirst())
                    .map(found -> ExplorerAnswer.pathsByProduct(found, springOff)).orElse(Map.of());
        } catch (DataUnreachableException unreachable) {
            return Map.of();
        }
    }

    /**
     * An opened finding, read from the release's own record, which is fetched
     * the first time a finding of that release is opened and cached by the
     * data layer.
     */
    private Component findingDetails(VersionNumber version, Finding finding, boolean springOff) {
        Optional<ExplorerVersion.Finding> full;
        try {
            full = data.explorerVersion(version).flatMap(record -> record.findings().stream()
                    .filter(candidate -> candidate.cve().equals(finding.cve())).findFirst());
        } catch (DataUnreachableException unreachable) {
            return css(new Div(new DataOutageNotice()), "explorer-finding");
        }
        return new FindingDetail(finding.cve(), version, full, springOff);
    }

    // The legend

    /** The "?" and the panel it opens, in the prototype's words, D-211. */
    Button legendButton(ValueSignal<Boolean> open) {
        var help = css(new Button("?", click -> open.set(true)), "explorer-help");
        help.setAriaLabel(owner.getTranslation("explorer.legend.open"));
        help.addThemeVariants(ButtonVariant.PRIMARY);
        help.bindVisible(open.map(shown -> !shown));
        return help;
    }

    Aside legend(ValueSignal<Boolean> open) {
        var legend = css(new Aside(), "explorer-legend");
        var title = new H2(owner.getTranslation("explorer.legend.heading"));
        var close = new Button(VaadinIcon.CLOSE.create(), click -> open.set(false));
        close.addThemeVariants(ButtonVariant.TERTIARY);
        close.setAriaLabel(owner.getTranslation("explorer.legend.close"));
        close.addClassName("explorer-legend-close");
        var top = css(new Div(title, close), "explorer-legend-top");
        legend.add(top, hint(owner.getTranslation("explorer.legend.lead")));

        var states = new LinkedHashMap<String, String>();
        for (ExplorerRules.LineState state : ExplorerRules.LineState.values()) {
            states.put(owner.getTranslation("explorer.state." + state.name()), owner.getTranslation("explorer.state." + state.name() + ".means"));
        }
        legend.add(new H3(owner.getTranslation("explorer.legend.badges")), terms(states));

        var groups = new LinkedHashMap<String, String>();
        KINDS.forEach(kind -> groups.put(owner.getTranslation("explorer.half.group." + kind), owner.getTranslation("explorer.half.group." + kind + ".title")));
        legend.add(new H3(owner.getTranslation("explorer.legend.groups")), terms(groups));

        var depth = new LinkedHashMap<String, String>();
        depth.put(owner.getTranslation("explorer.legend.reviewed"), owner.getTranslation("explorer.legend.reviewed.means"));
        depth.put(owner.getTranslation("explorer.legend.reproduced"), owner.getTranslation("explorer.legend.reproduced.means"));
        legend.add(new H3(owner.getTranslation("explorer.legend.depth")), terms(depth));

        legend.add(new H3(owner.getTranslation("explorer.legend.measuring")), hint(owner.getTranslation("explorer.legend.measuring.shape")),
                hint(owner.getTranslation("explorer.legend.measuring.missing")));
        legend.bindVisible(open);
        return legend;
    }

    private static DescriptionList terms(Map<String, String> entries) {
        var list = css(new DescriptionList(), "explorer-legend-list");
        entries.forEach((term, description) -> list.add(new DescriptionList.Term(term),
                new DescriptionList.Description(description)));
        return list;
    }

    // Small parts

    static Paragraph hint(String text) {
        return paragraph("explorer-hint", text);
    }
}

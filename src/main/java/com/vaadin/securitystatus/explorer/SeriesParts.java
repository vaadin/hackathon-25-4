package com.vaadin.securitystatus.explorer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Aside;
import com.vaadin.flow.component.html.DescriptionList;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.securitystatus.base.ui.DataOutageNotice;
import com.vaadin.securitystatus.data.DataUnreachableException;
import com.vaadin.securitystatus.data.SecurityDataService;
import com.vaadin.securitystatus.data.VersionNumber;
import com.vaadin.securitystatus.data.model.ExplorerFront.Finding;
import com.vaadin.securitystatus.data.model.ExplorerFront.Line;
import com.vaadin.securitystatus.data.model.ExplorerVersion;
import com.vaadin.securitystatus.explorer.ExplorerAnswer.Asked;
import com.vaadin.securitystatus.explorer.ExplorerAnswer.Groups;
import com.vaadin.securitystatus.explorer.ExplorerAnswer.Shape;
import com.vaadin.securitystatus.explorer.ExplorerAnswer.Split;
import com.vaadin.securitystatus.explorer.ExplorerRules.Product;

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

    private String t(String key, Object... params) {
        return owner.getTranslation(key, params);
    }

    // The row

    static Badge badge(String side, ExplorerRules.Badge state, String text) {
        var badge = new Badge(text);
        badge.addClassNames("explorer-badge", side, "badge-" + state.name().toLowerCase(Locale.ROOT));
        return badge;
    }

    /** "Vaadin: <state>" with what the state means on hover. */
    Badge vaadinBadge(ExplorerRules.Badge state) {
        var vaadin = badge("vaadin", state, t("explorer.badge.vaadin", t("explorer.state." + state.name())));
        vaadin.getElement().setAttribute("title", t("explorer.state." + state.name() + ".means"));
        return vaadin;
    }

    /** "Spring Boot: <state>", the Spring Boot it was measured against said on hover. */
    Badge springBadge(ExplorerRules.Badge state, String title) {
        var spring = badge("spring", state, t("explorer.badge.spring", t("explorer.state." + state.name())));
        spring.getElement().setAttribute("title", title);
        return spring;
    }

    /** What the row says beside the badge, counted over the same findings as the badge, D-205. */
    Optional<String> sentence(Line line, Shape shape, ExplorerRules.Badge state) {
        List<Finding> mine = ExplorerAnswer.stateFindings(line, shape);
        long risks = mine.stream().filter(finding -> ExplorerRules.stateOf(finding) == ExplorerRules.Badge.VULNERABLE)
                .count();
        long open = mine.stream().filter(finding -> ExplorerRules.stateOf(finding) == ExplorerRules.Badge.UNJUDGED)
                .count();
        switch (state) {
            case VULNERABLE:
                return Optional.of(open > 0 ? t("explorer.says.riskOpen", (int) risks, (int) open)
                        : t("explorer.says.risk", (int) risks));
            case UNJUDGED:
                return Optional.of(!ExplorerAnswer.measured(line, shape) || mine.isEmpty()
                        ? t("explorer.says.unmeasured")
                        : t("explorer.says.unjudged", (int) open));
            default:
                Groups theirs = ExplorerAnswer.split(line, shape, Asked.nothing()).theirs();
                int waiting = theirs.risk().size() + theirs.pending().size();
                return waiting > 0 ? Optional.of(t("explorer.says.spring", waiting)) : Optional.empty();
        }
    }

    /** One ring per product that is on, in catalogue order, with its count, the noughts drawn too, D-207. */
    Component rings(Line line, List<Product> catalogue, Shape shape, Asked asked) {
        var rings = new Span();
        rings.addClassName("explorer-rings");
        for (Product product : catalogue) {
            if (!shape.on().contains(product.name())) {
                continue;
            }
            int count = ExplorerAnswer.arrivesThrough(line, product.name(), shape, asked).size();
            var ring = new Span(String.valueOf(count));
            ring.addClassName("explorer-ring");
            if (count > 0) {
                ring.addClassNames("product-ring", "ring-" + product.ring());
                ring.getElement().setAttribute("title", t("explorer.ring.some", count, product.name()));
            } else {
                ring.addClassName("none");
                ring.getElement().setAttribute("title", t("explorer.ring.none", product.name()));
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
    void halves(Div into, Line line, Split split, FindingGrid.Routes routes, boolean release) {
        boolean halves = split.theirs().size() > 0 && !routes.shape().plain();
        if (halves) {
            into.add(halfTitle("explorer.half.ours"));
        }
        groups(into, line, "", split.ours(), routes, release);
        if (split.ours().risk().isEmpty() && split.ours().pending().isEmpty()) {
            into.add(hint(t("explorer.detail.nothing")));
        }

        // On the release page the prototype counts every finding of Vaadin's with no route, the pinned
        // and the bundled ones too, D-216; the explorer keeps the count it was verified with.
        List<Finding> blind = release
                ? ExplorerAnswer.findingsOf(line, routes.shape(), Asked.nothing()).stream()
                        .filter(finding -> "vaadin".equals(finding.owner().orElse("")) && finding.modules().isEmpty()
                                && ExplorerAnswer.mine(finding, routes.shape(), line))
                        .toList()
                : split.unattributed().stream().filter(finding -> finding.bundledIn().isEmpty()).toList();
        int frontend = (int) blind.stream().filter(FindingGrid::frontend).count();
        if (frontend > 0) {
            into.add(hint(t("explorer.detail.frontend", frontend)));
        }
        if (blind.size() - frontend > 0) {
            into.add(hint(t("explorer.detail.untraced", blind.size() - frontend)));
        }
        if (split.elsewhere() > 0) {
            into.add(hint(t("explorer.detail.elsewhere", split.elsewhere())));
        }

        if (halves) {
            var half = new Div();
            half.addClassName("explorer-half");
            half.add(halfTitle("explorer.half.theirs"));
            if (!release) {
                var says = new Paragraph();
                says.addClassName("explorer-hint");
                String against = line.springBoot().map(VersionNumber::toString)
                        .orElseGet(() -> t("explorer.half.theirs.unknown"));
                var text = new StringBuilder(t("explorer.half.theirs.lead", split.theirs().size(), against));
                line.springBootDeclared().filter(declared -> line.springBoot().isPresent()
                        && !declared.equals(line.springBoot().get())).ifPresent(declared -> text.append(' ')
                                .append(t("explorer.half.theirs.declared", declared.toString())));
                text.append(' ').append(t("explorer.half.theirs.moves")).append(' ');
                says.add(text.toString());
                var link = new RouterLink(t("explorer.half.theirs.link"), SpringView.class);
                link.addClassName("explorer-spring-link");
                says.add(link);
                half.add(says);
            }
            groups(half, line, "SPRING_", split.theirs(), routes, release);
            into.add(half);
        }

        if (!split.build().isEmpty()) {
            into.add(group(line, "BUILD", t("explorer.group.BUILD", split.build().size()), null,
                    "explorer.group.BUILD.note", split.build(), false, routes));
        }
    }

    private Component halfTitle(String key) {
        var title = new Paragraph(t(key));
        title.addClassName("explorer-half-title");
        title.getElement().setAttribute("title", t(key + ".title"));
        return title;
    }

    private void groups(Div into, Line line, String prefix, Groups groups, FindingGrid.Routes routes,
            boolean shut) {
        List<List<Finding>> members = List.of(groups.risk(), groups.pending(), groups.ruled());
        for (int i = 0; i < KINDS.size(); i++) {
            List<Finding> findings = members.get(i);
            if (findings.isEmpty()) {
                continue;
            }
            String kind = KINDS.get(i);
            String heading = t("explorer.half.group", findings.size(), t("explorer.half.group." + kind));
            into.add(group(line, prefix + kind, heading, t("explorer.half.group." + kind + ".title"), null, findings,
                    !shut && !"ANSWERED".equals(kind), routes));
        }
    }

    private Component group(Line line, String key, String heading, String title, String noteKey,
            List<Finding> findings, boolean startsOpen, FindingGrid.Routes routes) {
        var label = new Span(heading);
        label.addClassName("explorer-group-label");
        if (title != null) {
            label.getElement().setAttribute("title", title);
        }
        var details = new Details(label);
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
            details.add(lead("explorer-group-note", t(noteKey)));
        }
        ExplorerRules.Capped capped = ExplorerRules.capped(findings);
        details.add(new FindingGrid(capped.shown(), routes, finding -> findingDetails(line.version(), finding,
                routes.shape().plain())));
        if (capped.more() > 0) {
            details.add(lead("explorer-group-more", t("explorer.group.more", capped.more())));
        }
        return details;
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
            var box = new Div(new DataOutageNotice());
            box.addClassName("explorer-finding");
            return box;
        }
        return new FindingDetail(finding.cve(), version, full, springOff);
    }

    // The legend

    /** The "?" and the panel it opens, in the prototype's words, D-211. */
    Button legendButton(ValueSignal<Boolean> open) {
        var help = new Button("?", click -> open.set(true));
        help.addClassName("explorer-help");
        help.setAriaLabel(t("explorer.legend.open"));
        help.addThemeVariants(ButtonVariant.PRIMARY);
        help.bindVisible(open.map(shown -> !shown));
        return help;
    }

    Aside legend(ValueSignal<Boolean> open) {
        var legend = new Aside();
        legend.addClassName("explorer-legend");
        var title = new H2(t("explorer.legend.heading"));
        var close = new Button(VaadinIcon.CLOSE.create(), click -> open.set(false));
        close.addThemeVariants(ButtonVariant.TERTIARY);
        close.setAriaLabel(t("explorer.legend.close"));
        close.addClassName("explorer-legend-close");
        var top = new Div(title, close);
        top.addClassName("explorer-legend-top");
        legend.add(top, hint(t("explorer.legend.lead")));

        var states = new LinkedHashMap<String, String>();
        for (ExplorerRules.Badge state : ExplorerRules.Badge.values()) {
            states.put(t("explorer.state." + state.name()), t("explorer.state." + state.name() + ".means"));
        }
        legend.add(new H3(t("explorer.legend.badges")), terms(states));

        var groups = new LinkedHashMap<String, String>();
        KINDS.forEach(kind -> groups.put(t("explorer.half.group." + kind), t("explorer.half.group." + kind + ".title")));
        legend.add(new H3(t("explorer.legend.groups")), terms(groups));

        var depth = new LinkedHashMap<String, String>();
        depth.put(t("explorer.legend.reviewed"), t("explorer.legend.reviewed.means"));
        depth.put(t("explorer.legend.reproduced"), t("explorer.legend.reproduced.means"));
        legend.add(new H3(t("explorer.legend.depth")), terms(depth));

        legend.add(new H3(t("explorer.legend.measuring")), hint(t("explorer.legend.measuring.shape")),
                hint(t("explorer.legend.measuring.missing")));
        legend.bindVisible(open);
        return legend;
    }

    private static DescriptionList terms(Map<String, String> entries) {
        var list = new DescriptionList();
        list.addClassName("explorer-legend-list");
        entries.forEach((term, description) -> list.add(new DescriptionList.Term(term),
                new DescriptionList.Description(description)));
        return list;
    }

    // Small parts

    static Paragraph lead(String className, String text) {
        var paragraph = new Paragraph(text);
        paragraph.addClassName(className);
        return paragraph;
    }

    static Paragraph hint(String text) {
        return lead("explorer-hint", text);
    }
}

package com.vaadin.securitystatus.ui.team.explorer;

import static com.vaadin.securitystatus.ui.common.Parts.css;
import static com.vaadin.securitystatus.ui.common.Parts.decorative;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.securitystatus.data.model.ExplorerFront.Finding;
import com.vaadin.securitystatus.team.explorer.ExplorerAnswer;
import com.vaadin.securitystatus.team.explorer.ExplorerRules.Product;
import com.vaadin.securitystatus.team.explorer.ExplorerRules;

/**
 * The findings of one group, each a disclosure of its own as the prototype
 * draws it, D-255: the advisory, the severity, how many of the products that
 * bring it are the reader's, and a dot for each of those, with the opened
 * finding underneath. The explorer's opened line and the release page draw
 * the same one. The words of the third part are the prototype's, D-208.
 */
final class FindingList extends Composite<Div> {

    /**
     * What a row needs to say who brings a finding: the products of the
     * catalogue, the reading of the page, the products the shape cannot
     * contain on this line, and the products a reader chooses rather than gets
     * with Vaadin.
     */
    record RowContext(List<Product> catalogue, ExplorerAnswer.Shape shape, List<String> hidden, Set<String> optional) {
    }

    /** How many paths a dot's tooltip names before it counts the rest, the prototype's figure. */
    static final int PATHS_SHOWN = 4;

    private final RowContext routes;
    private final List<Finding> findings;
    private final Function<String, Map<String, List<String>>> paths;

    /**
     * {@code paths} answers, for an advisory, the paths each product brings it
     * by, for the tooltips of its dots; an empty map when the release record
     * cannot say.
     */
    FindingList(List<Finding> findings, RowContext routes, Function<Finding, Component> details,
            Function<String, Map<String, List<String>>> paths) {
        this.routes = routes;
        this.paths = paths;
        this.findings = List.copyOf(findings);
        var list = getContent();
        list.addClassName("explorer-findings");
        for (Finding finding : findings) {
            list.add(row(finding, details));
        }
    }

    /** The findings listed, in their order. */
    List<Finding> findings() {
        return findings;
    }

    /** The row of one finding, for the views and tests that open it. */
    Optional<Details> rowOf(String cve) {
        return getContent().getChildren().filter(Details.class::isInstance).map(Details.class::cast)
                .filter(row -> cve.equals(row.getElement().getAttribute("data-cve"))).findFirst();
    }

    /** One finding, shut, its detail built the first time it is opened so a shut row reads no record. */
    private Details row(Finding finding, Function<Finding, Component> details) {
        var summary = css(new Div(), "explorer-finding-summary");
        summary.add(advisoryId(finding));
        Component severity = severity(finding);
        if (severity != null) {
            summary.add(severity);
        }
        summary.add(through(finding));
        Component dots = dots(finding);
        if (dots != null) {
            summary.add(dots);
        }
        var row = css(new Details(summary), "explorer-finding-row");
        row.getElement().setAttribute("data-cve", finding.cve());
        row.addOpenedChangeListener(change -> {
            if (change.isOpened() && row.getContent().findAny().isEmpty()) {
                row.add(details.apply(finding));
            }
        });
        return row;
    }

    /** Rule 4: a finding with no severity gets no element in the cell, rather than an empty one. */
    private @Nullable Component severity(Finding finding) {
        return finding.severity().<Component>map(word -> {
            return css(new Badge(getTranslation("severity." + word)), "explorer-severity", "severity-" + word);
        }).orElse(null);
    }

    /** The words after the severity, with the products named in the tooltip. */
    private Component through(Finding finding) {
        Optional<List<String>> named = ExplorerAnswer.routes(finding, routes.shape(), routes.hidden());
        var words = css(new Span(), "explorer-through");
        if (named.isPresent() && !named.get().isEmpty()) {
            List<String> all = named.get();
            if (all.stream().allMatch(routes.optional()::contains)) {
                String only = String.join(" " + getTranslation("explorer.through.or") + " ", all);
                words.setText(getTranslation("explorer.through.only", only));
                Tooltip.forComponent(words).setText(getTranslation("explorer.through.only.title", only));
                words.addClassName("only");
                return words;
            }
            List<String> mine = all.stream().filter(routes.shape().on()::contains).toList();
            words.setText(mine.size() == all.size() ? getTranslation("explorer.through.all", all.size())
                    : getTranslation("explorer.through.some", String.valueOf(mine.size()), all.size()));
            String title = mine.isEmpty() ? getTranslation("explorer.through.noneYours")
                    : getTranslation("explorer.through.yours", String.join(", ", mine));
            if (mine.size() < all.size()) {
                title += " " + getTranslation("explorer.through.also",
                        String.join(", ", all.stream().filter(name -> !mine.contains(name)).toList()));
            }
            Tooltip.forComponent(words).setText(title);
            return words;
        }
        if (finding.bundledIn().isPresent()) {
            words.setText(getTranslation("explorer.through.bundled", finding.bundledIn().get()));
        } else if (ExplorerRules.frontend(finding)) {
            words.setText(getTranslation("explorer.through.frontend"));
            Tooltip.forComponent(words).setText(getTranslation("explorer.through.frontend.title"));
        } else {
            words.setText(getTranslation("explorer.through.none"));
            words.addClassName("dim");
        }
        return words;
    }

    /** One dot per product that brings it and is on, in catalogue order, with the product's ring colour. */
    private @Nullable Component dots(Finding finding) {
        Optional<List<String>> named = ExplorerAnswer.routes(finding, routes.shape(), routes.hidden());
        if (named.isEmpty() || named.get().isEmpty()) {
            return null;
        }
        var dots = css(new Span(), "explorer-dots");
        Map<String, List<String>> byProduct = paths.apply(finding.cve());
        for (Product product : routes.catalogue()) {
            if (named.get().contains(product.name()) && routes.shape().on().contains(product.name())) {
                var dot = ring(product);
                dot.addClassName("dot");
                Tooltip.forComponent(dot).setText(dotTitle(product.name(), byProduct));
                dots.add(dot);
            }
        }
        return dots.getComponentCount() == 0 ? null : dots;
    }

    /**
     * What a dot says on hover, as the prototype says it: the product, then
     * the paths it brings the advisory by, one per line, the first four and a
     * count of the rest.
     */
    private String dotTitle(String product, Map<String, List<String>> byProduct) {
        var lines = new ArrayList<String>();
        lines.add(getTranslation("explorer.dot", product));
        List<String> mine = byProduct.getOrDefault(product, List.of());
        mine.stream().limit(PATHS_SHOWN).forEach(lines::add);
        if (mine.size() > PATHS_SHOWN) {
            lines.add(getTranslation("explorer.dot.more", mine.size() - PATHS_SHOWN));
        }
        return String.join("\n", lines);
    }

    /** One product's ring, coloured by its place in the catalogue. */
    static Span ring(Product product) {
        var ring = decorative(new Span(), "product-ring", "ring-" + product.ring());
        return ring;
    }

    private static Span advisoryId(Finding finding) {
        return css(new Span(finding.cve()), "advisory-id");
    }
}

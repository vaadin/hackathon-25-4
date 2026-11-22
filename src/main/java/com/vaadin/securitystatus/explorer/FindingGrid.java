package com.vaadin.securitystatus.explorer;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridI18n;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.securitystatus.data.model.ExplorerFront.Finding;
import com.vaadin.securitystatus.explorer.ExplorerRules.Product;

/**
 * The findings of one group: advisory, severity, how many of the products
 * that bring it are the reader's, and a dot for each of those, with the opened
 * finding as the row's item details. The explorer's opened line and the
 * release page draw the same one. The words of the third column are the
 * prototype's, D-208.
 */
final class FindingGrid extends Grid<Finding> {

    /**
     * What a row needs to say who brings a finding: the products of the
     * registry, the reading of the page, the products the shape cannot contain
     * on this line, and the products a reader chooses rather than gets with
     * Vaadin.
     */
    record Routes(List<Product> registry, ExplorerAnswer.Shape shape, List<String> hidden, Set<String> optional) {
    }

    private final Routes routes;

    FindingGrid(List<Finding> findings, Routes routes, Function<Finding, Component> details) {
        this.routes = routes;
        var grid = this;
        grid.addClassName("explorer-findings");
        grid.addThemeVariants(GridVariant.NO_BORDER, GridVariant.WRAP_CELL_CONTENT);
        grid.setI18n(new GridI18n().setSorter(getTranslation("explorer.grid.sorter"))
                .setSelectRow(getTranslation("explorer.grid.selectRow"))
                .setSelectAll(getTranslation("explorer.grid.selectAll"))
                .setSelectAllUnavailable(getTranslation("explorer.grid.selectAllUnavailable")));
        grid.addColumn(Finding::cve).setHeader(getTranslation("explorer.grid.advisory")).setSortable(true)
                .setComparator(Finding::cve).setAutoWidth(true).setFlexGrow(0);
        grid.addComponentColumn(this::severity).setHeader(getTranslation("explorer.grid.severity"))
                .setSortable(true).setComparator(finding -> ExplorerRules.severityRank(finding.severity()))
                .setAutoWidth(true).setFlexGrow(0);
        grid.addComponentColumn(this::through).setHeader(getTranslation("explorer.grid.through")).setFlexGrow(1);
        grid.addComponentColumn(this::dots).setHeader(getTranslation("explorer.grid.routes")).setAutoWidth(true)
                .setFlexGrow(0);
        grid.setItemDetailsRenderer(new ComponentRenderer<>(details::apply));
        grid.setAllRowsVisible(true);
        grid.setItems(findings);
    }

    /** Every product on and nothing hidden: the release page, which narrows nothing, D-111. */
    static Routes everything(List<Product> registry, ExplorerAnswer.Shape shape, Set<String> optional) {
        return new Routes(registry, shape, List.of(), optional);
    }

    /** Rule 4: a finding with no severity gets no element in the cell, rather than an empty one. */
    private @Nullable Component severity(Finding finding) {
        return finding.severity().<Component>map(word -> {
            var badge = new Span(getTranslation("severity." + word));
            badge.addClassNames("explorer-severity", "severity-" + word);
            return badge;
        }).orElse(null);
    }

    /** The words of the third column, with the products named in the tooltip. */
    private Component through(Finding finding) {
        Optional<List<String>> named = ExplorerAnswer.routes(finding, routes.shape(), routes.hidden());
        var words = new Span();
        words.addClassName("explorer-through");
        if (named.isPresent() && !named.get().isEmpty()) {
            List<String> all = named.get();
            if (all.stream().allMatch(routes.optional()::contains)) {
                String only = String.join(" " + getTranslation("explorer.through.or") + " ", all);
                words.setText(getTranslation("explorer.through.only", only));
                words.getElement().setAttribute("title", getTranslation("explorer.through.only.title", only));
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
            words.getElement().setAttribute("title", title);
            return words;
        }
        if (finding.bundledIn().isPresent()) {
            words.setText(getTranslation("explorer.through.bundled", finding.bundledIn().get()));
        } else if (frontend(finding)) {
            words.setText(getTranslation("explorer.through.frontend"));
            words.getElement().setAttribute("title", getTranslation("explorer.through.frontend.title"));
        } else {
            words.setText(getTranslation("explorer.through.none"));
            words.addClassName("dim");
        }
        return words;
    }

    /** A frontend finding, which every application carries inside the framework's bundle. */
    static boolean frontend(Finding finding) {
        String ecosystem = finding.ecosystem().orElse("");
        return "npm".equals(ecosystem) || "javascript".equals(ecosystem);
    }

    /** One dot per product that brings it and is on, in registry order, with the product's ring colour. */
    private @Nullable Component dots(Finding finding) {
        Optional<List<String>> named = ExplorerAnswer.routes(finding, routes.shape(), routes.hidden());
        if (named.isEmpty() || named.get().isEmpty()) {
            return null;
        }
        var dots = new Span();
        dots.addClassName("explorer-dots");
        for (Product product : routes.registry()) {
            if (named.get().contains(product.name()) && routes.shape().on().contains(product.name())) {
                var dot = ring(product);
                dot.addClassName("dot");
                dot.getElement().setAttribute("title", getTranslation("explorer.dot", product.name()));
                dots.add(dot);
            }
        }
        return dots.getComponentCount() == 0 ? null : dots;
    }

    /** One product's ring, coloured by its place in the registry. */
    static Span ring(Product product) {
        var ring = new Span();
        ring.addClassNames("product-ring", "ring-" + product.ring());
        ring.getElement().setAttribute("aria-hidden", "true");
        return ring;
    }
}

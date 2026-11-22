package com.vaadin.securitystatus.explorer;

import java.util.List;
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
 * The findings of one group: advisory, severity, the products it arrives
 * through and their rings, with the opened finding as the row's item
 * details. The explorer's opened line and the release page draw the same one.
 */
final class FindingGrid extends Grid<Finding> {

    private final List<Product> registry;
    private final Set<String> off;

    FindingGrid(List<Finding> findings, List<Product> registry, Set<String> off,
            Function<Finding, Component> details) {
        this.registry = registry;
        this.off = off;
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
        grid.addColumn(this::through).setHeader(getTranslation("explorer.grid.through")).setFlexGrow(1);
        grid.addComponentColumn(this::dots).setHeader(getTranslation("explorer.grid.routes")).setAutoWidth(true)
                .setFlexGrow(0);
        grid.setItemDetailsRenderer(new ComponentRenderer<>(details::apply));
        grid.setAllRowsVisible(true);
        grid.setItems(findings);
    }

    /** Rule 4: a finding with no severity gets no element in the cell, rather than an empty one. */
    private @Nullable Component severity(Finding finding) {
        return finding.severity().<Component>map(word -> {
            var badge = new Span(getTranslation("severity." + word));
            badge.addClassNames("explorer-severity", "severity-" + word);
            return badge;
        }).orElse(null);
    }

    /** Through how many of its products a finding arrives, or the one it rides inside. */
    private String through(Finding finding) {
        if (finding.bundledIn().isPresent()) {
            return getTranslation("explorer.through.bundled", finding.bundledIn().get());
        }
        List<Product> through = ExplorerRules.through(finding, registry);
        if (through.isEmpty()) {
            return getTranslation("explorer.through.none");
        }
        if (through.size() == 1) {
            return getTranslation("explorer.through.only", through.get(0).name());
        }
        long on = through.stream().filter(product -> !off.contains(product.name())).count();
        return on == through.size() ? getTranslation("explorer.through.all", through.size())
                : getTranslation("explorer.through.some", on, through.size());
    }

    private @Nullable Component dots(Finding finding) {
        List<Product> products = ExplorerRules.through(finding, registry);
        if (products.isEmpty()) {
            return null;
        }
        var dots = new Span();
        dots.addClassName("explorer-dots");
        for (Product product : products) {
            var dot = ring(product);
            dot.addClassName("dot");
            if (off.contains(product.name())) {
                dot.addClassName("off");
            }
            dot.getElement().setAttribute("title", product.name());
            dots.add(dot);
        }
        return dots;
    }

    /** One product's ring, coloured by its place in the registry. */
    static Span ring(Product product) {
        var ring = new Span();
        ring.addClassNames("product-ring", "ring-" + product.ring());
        ring.getElement().setAttribute("aria-hidden", "true");
        return ring;
    }
}

package com.vaadin.securitystatus.status;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.clipboard.Clipboard;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.grid.contextmenu.GridMenuItem;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Nav;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouteParent;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.securitystatus.base.ai.AiSurfaces;
import com.vaadin.securitystatus.base.nav.Address;
import com.vaadin.securitystatus.base.security.Roles;
import com.vaadin.securitystatus.base.nav.QueryFlag;
import com.vaadin.securitystatus.base.ui.DataOutageNotice;
import com.vaadin.securitystatus.base.ui.ViewHeading;
import com.vaadin.securitystatus.data.DataRefreshSignal;
import com.vaadin.securitystatus.data.DataUnreachableException;
import com.vaadin.securitystatus.data.SecurityDataService;
import com.vaadin.securitystatus.data.model.CasesIndex;
import com.vaadin.securitystatus.data.model.CasesIndex.AdvisoryCatalogEntry;

/**
 * Every advisory the index carries, worst first, narrowed by one verdict
 * through {@code ?verdict=}. A row opens the answer by CVE; its context menu
 * copies the id, opens the advisory's own record, or carries the id to the
 * lookup to be asked about on the reader's version.
 */
@Route("advisories")
@RouteParent(HomeView.class)
@PageTitle("advisories.title")
@AnonymousAllowed
// The ask arrives through a bean, which the production bundle's scanner does not follow, D-149.
@Uses(MessageInput.class)
public final class AdvisoriesView extends Composite<Div> implements BeforeEnterObserver {

    /** What the list is drawn from: the index, or nothing when it could not be read. */
    private record Loaded(Optional<CasesIndex> index, List<AdvisoryCatalogEntry> ordered) {
    }

    private final SecurityDataService data;
    private final LookupState lookup;
    private final ValueSignal<Optional<String>> verdict = new ValueSignal<>(Optional.empty());
    /** The advisories a plain language ask narrowed the list to, or empty for no ask. */
    private final ValueSignal<Optional<Set<String>>> narrowed = new ValueSignal<>(Optional.empty());
    private final Grid<AdvisoryCatalogEntry> grid = new Grid<>();
    private final GridContextMenu<AdvisoryCatalogEntry> menu = new GridContextMenu<>(grid);
    private final Nav pills = new Nav();
    private final Paragraph total = new Paragraph();
    private final Div outage = new Div();
    /** The index's customer words for the verdicts, as last drawn. */
    private Map<String, String> labels = Map.of();

    public AdvisoriesView(SecurityDataService data, DataRefreshSignal refresh, LookupState lookup,
            AuthenticationContext authentication, ObjectProvider<AiSurfaces> ai) {
        this.data = data;
        this.lookup = lookup;
        addClassName("advisories-view");
        pills.addClassName("verdict-pills");
        pills.getElement().setAttribute("aria-label", getTranslation("advisories.pills.label"));
        total.addClassName("advisories-total");
        outage.addClassName("advisories-outage");
        configureGrid();
        configureMenu();
        getContent().add(ViewHeading.ofAnswer(), outage, pills);
        // The ask exists only for the team, and the security context is asked
        // once, here: an anonymous reader gets no box and so no model call, D-141.
        if (authentication.hasRole(Roles.SECURITY)) {
            ai.ifAvailable(surfaces -> getContent().add(surfaces.advisoryAsk(narrowed::set)));
        }
        getContent().add(total, grid);

        var generatedAt = refresh.generatedAt();
        Signal<Loaded> loaded = Signal.computed(() -> {
            generatedAt.get();
            return load();
        });
        Signal.effect(this, () -> draw(loaded.get(), verdict.get(), narrowed.get()));
    }

    /**
     * The grid of the list, for whatever attaches to it later: the AI grid
     * controller of epic 11 drives this instance.
     */
    public Grid<AdvisoryCatalogEntry> grid() {
        return grid;
    }

    GridContextMenu<AdvisoryCatalogEntry> menu() {
        return menu;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        verdict.set(AdvisoryRules.verdict(Address.first(event.getLocation().getQueryParameters(),
                QueryFlag.VERDICT)));
    }

    private Loaded load() {
        try {
            CasesIndex index = data.index();
            return new Loaded(Optional.of(index), AdvisoryRules.ordered(index));
        } catch (DataUnreachableException unreachable) {
            return new Loaded(Optional.empty(), List.of());
        }
    }

    // Drawing

    private void draw(Loaded loaded, Optional<String> asked, Optional<Set<String>> ask) {
        outage.removeAll();
        pills.removeAll();
        boolean reachable = loaded.index().isPresent();
        pills.setVisible(reachable);
        total.setVisible(reachable);
        grid.setVisible(reachable);
        if (!reachable) {
            outage.add(new DataOutageNotice());
            grid.setItems(List.of());
            return;
        }
        CasesIndex index = loaded.index().get();
        List<AdvisoryCatalogEntry> all = loaded.ordered();
        Map<String, Integer> counts = AdvisoryRules.counts(all);
        // A verdict no advisory carries has no pill, so it cannot narrow the list either.
        Optional<String> shown = asked.filter(counts::containsKey);
        List<AdvisoryCatalogEntry> rows = ask.map(ids -> AdvisoryRules.filtered(all, shown).stream()
                .filter(row -> ids.contains(row.cve())).toList()).orElseGet(() -> AdvisoryRules.filtered(all, shown));

        pills.add(pill(getTranslation("advisories.pill.all", all.size()), Optional.empty(), shown.isEmpty()));
        counts.forEach((word, count) -> pills.add(pill(
                getTranslation("advisories.pill", verdictWords(index, word), count), Optional.of(word),
                shown.filter(word::equals).isPresent())));

        // Rule 6.3.11: the total, and under a filter the shown count against it.
        total.setText(ask.isPresent() ? getTranslation("advisories.narrowed", rows.size(), all.size())
                : shown.isEmpty() ? getTranslation("advisories.total", all.size())
                        : getTranslation("advisories.shown", rows.size(), all.size(), verdictWords(index, shown.get())));

        labels = index.labels();
        grid.setItems(query -> rows.stream().skip(query.getOffset()).limit(query.getLimit()), query -> rows.size());
    }

    /** A pill is a real link writing the flag, so it can be opened in a new tab and the back button undoes it. */
    private Component pill(String text, Optional<String> word, boolean active) {
        var link = new RouterLink(text, AdvisoriesView.class);
        link.setQueryParameters(Address.of("advisories").with(QueryFlag.VERDICT, word).query());
        link.addClassName("verdict-pill");
        word.ifPresent(value -> link.addClassName("verdict-" + value));
        if (active) {
            link.addClassName("active");
            link.getElement().setAttribute("aria-current", "true");
        }
        return link;
    }

    /** The customer's words for a verdict: the index's label, else our short word for it. */
    private String verdictWords(CasesIndex index, String word) {
        return Optional.ofNullable(index.labels().get(word)).orElseGet(() -> getTranslation("verdict.short." + word));
    }

    // The grid

    private void configureGrid() {
        grid.addClassName("advisories-grid");
        grid.addThemeVariants(GridVariant.ROW_STRIPES);
        grid.addComponentColumn(this::idCell).setHeader(getTranslation("advisories.col.id")).setKey("id")
                .setAutoWidth(true).setFlexGrow(0);
        grid.addComponentColumn(this::severityCell).setHeader(getTranslation("advisories.col.severity"))
                .setKey("severity").setAutoWidth(true).setFlexGrow(0);
        grid.addComponentColumn(this::ownerCell).setHeader(getTranslation("advisories.col.owner")).setKey("owner")
                .setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(row -> String.join(", ", row.packages())).setHeader(getTranslation("advisories.col.package"))
                .setKey("package").setFlexGrow(1).setTooltipGenerator(row -> String.join(", ", row.packages()));
        grid.addComponentColumn(this::verdictCell).setHeader(getTranslation("advisories.col.verdict"))
                .setKey("verdict").setAutoWidth(true).setFlexGrow(0);
        grid.addItemClickListener(click -> open(click.getItem()));
    }

    private void open(AdvisoryCatalogEntry row) {
        UI.getCurrent().navigate(CveView.class, new RouteParameters(CveView.IDS, row.cve()));
    }

    private Component idCell(AdvisoryCatalogEntry row) {
        var id = new Span(row.cve());
        id.addClassName("advisory-id");
        return id;
    }

    /** Rule 4: no severity the index carries, no element in the cell, rather than an empty one. */
    private @Nullable Component severityCell(AdvisoryCatalogEntry row) {
        return row.severity().flatMap(VersionRules::severity).<Component>map(known -> {
            var cell = new Span(getTranslation("severity." + known));
            cell.addClassNames("severity", "severity-" + known);
            return cell;
        }).orElse(null);
    }

    /** Whose fix it is, by the fix owner rule of the version screen, D-51: no chip for an owner nobody established. */
    private @Nullable Component ownerCell(AdvisoryCatalogEntry row) {
        return row.fixOwner().flatMap(FindingCard::ownerKey).<Component>map(key -> {
            var chip = new Badge(getTranslation(key));
            chip.addClassName("owner-chip");
            return chip;
        }).orElse(null);
    }

    private @Nullable Component verdictCell(AdvisoryCatalogEntry row) {
        return row.applicability().flatMap(word -> chipWords(word).map(words -> {
            var chip = new Badge(words);
            chip.addClassNames("verdict-chip", "verdict-" + word);
            return (Component) chip;
        })).orElse(null);
    }

    /** The index's label for a verdict, else our short word for one of the closed vocabulary, else nothing. */
    private Optional<String> chipWords(String word) {
        return Optional.ofNullable(labels.get(word)).or(() -> AdvisoryRules.VERDICTS.contains(word)
                ? Optional.of(getTranslation("verdict.short." + word)) : Optional.empty());
    }

    // The context menu

    /**
     * Built again for the row it opens on, through the 25.4 dynamic content
     * provider: the record link and the id copied depend on the row, and the
     * clipboard action has to be armed before the click that runs it.
     */
    private void configureMenu() {
        menu.addClassName("advisories-menu");
        menu.setDynamicContentProvider(context -> {
            AdvisoryCatalogEntry row = context.item();
            if (row == null) {
                return false;
            }
            menu.removeAll();
            fill(row);
            return true;
        });
    }

    private void fill(AdvisoryCatalogEntry row) {
        String id = row.cve();
        GridMenuItem<AdvisoryCatalogEntry> copy = menu.addItem(getTranslation("advisories.menu.copy", id));
        copy.addClassName("menu-copy");
        copy.getElement().setAttribute("data-answer", id);
        Clipboard.onClick(copy).writeText(id, copied -> {
            var done = Notification.show(getTranslation("advisories.copy.done", id));
            done.addThemeVariants(NotificationVariant.SUCCESS);
        }, failure -> {
            var failed = Notification.show(getTranslation("advisories.copy.failed"));
            failed.addThemeVariants(NotificationVariant.ERROR);
        });

        var record = new Anchor(CveRules.recordUrl(id),
                getTranslation("advisories.menu.record", AdvisoryRules.recordHost(id)));
        record.setTarget(AnchorTarget.BLANK);
        record.getElement().setAttribute("rel", "noopener");
        menu.addItem(record).addClassName("menu-record");

        menu.addItem(getTranslation("advisories.menu.ask", id), click -> ask(id)).addClassName("menu-ask");
    }

    /**
     * Carries the id to the lookup's advisory tab, with the version the reader
     * already gave, and puts the caret where the question is finished.
     */
    private void ask(String id) {
        lookup.ids().set(id);
        if (lookup.idsVersion().peek().isBlank()) {
            String known = LookupInput.cleanVersion(lookup.version().peek());
            if (!known.isEmpty()) {
                lookup.idsVersion().set(known);
            }
        }
        lookup.tab().set(LookupState.Tab.IDS);
        UI.getCurrent().getChildren().flatMap(AdvisoriesView::lookups).findFirst().ifPresent(Lookup::focusIdsVersion);
    }

    private static java.util.stream.Stream<Lookup> lookups(Component component) {
        if (component instanceof Lookup found) {
            return java.util.stream.Stream.of(found);
        }
        return component.getChildren().flatMap(AdvisoriesView::lookups);
    }
}

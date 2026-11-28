package com.vaadin.securitystatus.ui.customer;

import static com.vaadin.securitystatus.ui.common.Parts.css;
import static com.vaadin.securitystatus.ui.common.Parts.decorative;

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
import com.vaadin.flow.component.badge.BadgeVariant;
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
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Nav;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParent;
import com.vaadin.flow.router.RouterLink;
import jakarta.annotation.security.PermitAll;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.securitystatus.customer.AdvisoryRules;
import com.vaadin.securitystatus.customer.CveRules;
import com.vaadin.securitystatus.customer.LookupInput;
import com.vaadin.securitystatus.customer.LookupState;
import com.vaadin.securitystatus.customer.VersionRules;
import com.vaadin.securitystatus.data.DataRefreshSignal;
import com.vaadin.securitystatus.data.DataUnreachableException;
import com.vaadin.securitystatus.data.SecurityDataService;
import com.vaadin.securitystatus.data.model.CasesIndex.AdvisoryCatalogEntry;
import com.vaadin.securitystatus.data.model.CasesIndex;
import com.vaadin.securitystatus.security.Roles;
import com.vaadin.securitystatus.ui.common.AiSurfaces;
import com.vaadin.securitystatus.ui.common.DataOutageNotice;
import com.vaadin.securitystatus.ui.common.nav.Address;
import com.vaadin.securitystatus.ui.common.nav.QueryFlag;

/**
 * Every advisory the index carries, worst first, narrowed by one verdict
 * through {@code ?verdict=}. A row opens the answer by CVE; its context menu
 * copies the id, opens the advisory's own record, or carries the id to the
 * lookup to be asked about on the reader's version.
 */
@Route("advisories")
@RouteParent(HomeView.class)
@PageTitle("advisories.title")
@PermitAll
// The ask arrives through a bean, which the production bundle's scanner does not follow, D-149.
@Uses(MessageInput.class)
public final class AdvisoriesView extends Composite<Div> implements BeforeEnterObserver {

    /** What the list is drawn from: the index, or nothing when it could not be read. */
    private record Loaded(Optional<CasesIndex> index, List<AdvisoryCatalogEntry> ordered) {
    }

    private final SecurityDataService data;
    private final LookupState lookup;
    private final ValueSignal<String> path = new ValueSignal<>("advisories");
    private final ValueSignal<Optional<String>> verdict = new ValueSignal<>(Optional.empty());
    /** The advisories a plain language ask narrowed the list to, or empty for no ask. */
    private final ValueSignal<Optional<Set<String>>> narrowed = new ValueSignal<>(Optional.empty());
    private final Grid<AdvisoryCatalogEntry> grid = new Grid<>();
    private final GridContextMenu<AdvisoryCatalogEntry> menu = new GridContextMenu<>(grid);
    private final Nav pills = new Nav();
    private final Paragraph total = new Paragraph();
    private final Span aside = new Span();
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
        // The prototype's heading, aside and tip as the answer's H2; the view's title stays the
        // trail's leaf and the document's title, D-191.
        var heading = css(new Div(new H2(getTranslation("advisories.heading")), aside), "section-row");
        aside.addClassName("section-aside");
        var tip = css(new Paragraph(getTranslation("advisories.tip")), "section-tip");
        getContent().add(heading, tip, outage, pills);
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
        Signal.effect(this, () -> {
            path.get();
            draw(loaded.get(), verdict.get(), narrowed.get());
        });
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
        // The list is served at /advisories and, inside the internal shell, at /findings/advisories:
        // its own links keep the address it was opened at, so the team keeps its side panel, D-237.
        path.set(event.getLocation().getPath());
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
        aside.setVisible(reachable);
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

        aside.setText(getTranslation("advisories.aside", all.size()));
        pills.add(pill(getTranslation("advisories.pill.all"), all.size(), Optional.empty(), shown.isEmpty()));
        counts.forEach((word, count) -> pills.add(pill(verdictWords(index, word), count, Optional.of(word),
                shown.filter(word::equals).isPresent())));

        // Rule 6.3.11: the total, and under a filter the shown count against it.
        total.setText(ask.isPresent() ? getTranslation("advisories.narrowed", rows.size(), all.size())
                : shown.isEmpty() ? getTranslation("advisories.total", all.size())
                        : getTranslation("advisories.shown", rows.size(), all.size(), verdictWords(index, shown.get())));

        labels = index.labels();
        grid.setItems(query -> rows.stream().skip(query.getOffset()).limit(query.getLimit()), query -> rows.size());
    }

    /** A pill is a real link writing the flag, so it can be opened in a new tab and the back button undoes it. */
    private Component pill(String text, int count, Optional<String> word, boolean active) {
        var link = new RouterLink(text, AdvisoriesView.class);
        // The count in its own lighter span, as the prototype sets it; passed as text, not a quantity to group.
        var number = css(new Span(String.valueOf(count)), "pill-count");
        link.add(number);
        Address address = Address.of(path.peek()).with(QueryFlag.VERDICT, word);
        link.setQueryParameters(address.query());
        link.getElement().setAttribute("href", address.href());
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
        return AdvisoryRules.label(index, word, this::getTranslation).orElseGet(() -> getTranslation("verdict.short." + word));
    }

    // The grid

    private void configureGrid() {
        grid.addClassName("advisories-grid");
        // Long package names wrap rather than being cut at the column's width, D-194.
        grid.addThemeVariants(GridVariant.ROW_STRIPES, GridVariant.WRAP_CELL_CONTENT);
        grid.addComponentColumn(this::idCell).setHeader(getTranslation("advisories.col.id")).setKey("id")
                .setAutoWidth(true).setFlexGrow(0);
        grid.addComponentColumn(this::severityCell).setHeader(getTranslation("advisories.col.severity"))
                .setKey("severity").setAutoWidth(true).setFlexGrow(0);
        grid.addComponentColumn(this::ownerCell).setHeader(getTranslation("advisories.col.owner")).setKey("owner")
                .setAutoWidth(true).setFlexGrow(0);
        grid.addComponentColumn(this::packagesCell).setHeader(getTranslation("advisories.col.package"))
                .setKey("package").setFlexGrow(1);
        grid.addComponentColumn(this::verdictCell).setHeader(getTranslation("advisories.col.verdict"))
                .setKey("verdict").setAutoWidth(true).setFlexGrow(0);
        grid.addItemClickListener(click -> open(click.getItem()));
    }

    private void open(AdvisoryCatalogEntry row) {
        answer(row.cve(), "");
    }

    private Component idCell(AdvisoryCatalogEntry row) {
        return css(new Span(row.cve()), "advisory-id");
    }

    /** Rule 4: no severity the index carries, no element in the cell, rather than an empty one. */
    private @Nullable Component severityCell(AdvisoryCatalogEntry row) {
        return row.severity().flatMap(VersionRules::severity).<Component>map(known -> {
            return css(new Span(getTranslation("severity." + known)), "severity", "severity-" + known);
        }).orElse(null);
    }

    /** Whose fix it is, by the fix owner rule of the version screen, D-51: no chip for an owner nobody established. */
    private @Nullable Component ownerCell(AdvisoryCatalogEntry row) {
        return row.fixOwner().flatMap(FindingCard::ownerKey).<Component>map(key -> {
            return css(new Badge(getTranslation(key)), "owner-chip");
        }).orElse(null);
    }

    /** Every package, one to a line, whole: none is cut at the column's width, D-194. */
    private @Nullable Component packagesCell(AdvisoryCatalogEntry row) {
        if (row.packages().isEmpty()) {
            return null;
        }
        var cell = css(new Div(), "advisory-packages");
        row.packages().forEach(name -> cell.add(new Span(name)));
        return cell;
    }

    /**
     * The listed verdict, the risk one in the error badge and every other in
     * the plain one, then the row's chevron, as the prototype and the home
     * page's recent rows draw them, D-194.
     */
    private @Nullable Component verdictCell(AdvisoryCatalogEntry row) {
        return AdvisoryRules.listed(row).flatMap(word -> chipWords(word).map(words -> {
            var chip = css(new Badge(words), "verdict-chip", "verdict-" + word);
            if ("risk".equals(word)) {
                chip.addThemeVariants(BadgeVariant.ERROR);
            }
            Icon chevron = decorative(VaadinIcon.ANGLE_RIGHT.create(), "advisory-chevron");
            var cell = css(new Span(chip, chevron), "advisory-verdict");
            return (Component) cell;
        })).orElse(null);
    }

    /** The index's label for a verdict, else our short word for one of the closed vocabulary, else nothing. */
    private Optional<String> chipWords(String word) {
        return Optional.ofNullable(labels.get(word))
                .or(() -> "not_current".equals(word) ? Optional.of(getTranslation("verdict.label.not_current"))
                        : Optional.empty())
                .or(() -> AdvisoryRules.VERDICTS.contains(word)
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

        String version = knownVersion();
        menu.addItem(version.isEmpty() ? getTranslation("advisories.menu.ask", id)
                : getTranslation("advisories.menu.askOnVersion", id), click -> ask(id, version))
                .addClassName("menu-ask");
    }

    /** Opens the answer for the id on the side the list was opened from, the team's or the customer's. */
    private void answer(String id, String version) {
        // The list's own address says where it was opened: under it, the answer opens the same way.
        String where = path.peek();
        String base = where.substring(0, Math.max(0, where.lastIndexOf("advisories"))) + "cve/";
        UI.getCurrent().navigate(base + id + (version.isEmpty() ? "" : "/" + version));
    }

    /** The version the reader already gave the lookup, beside ids or on its own, or nothing. */
    private String knownVersion() {
        String beside = LookupInput.cleanVersion(lookup.idsVersion().peek());
        return beside.isEmpty() ? LookupInput.cleanVersion(lookup.version().peek()) : beside;
    }

    /** Opens the answer for the id, on the reader's version when there is one. */
    private void ask(String id, String version) {
        answer(id, version);
    }
}

package com.vaadin.securitystatus.explorer;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.checkbox.Switch;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridI18n;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.masterdetaillayout.MasterDetailLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
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
import com.vaadin.securitystatus.base.nav.Address;
import com.vaadin.securitystatus.base.nav.QueryFlag;
import com.vaadin.securitystatus.base.security.Roles;
import com.vaadin.securitystatus.base.ui.DataOutageNotice;
import com.vaadin.securitystatus.base.ui.InternalLayout;
import com.vaadin.securitystatus.base.ui.ViewHeading;
import com.vaadin.securitystatus.data.DataRefreshSignal;
import com.vaadin.securitystatus.data.DataUnreachableException;
import com.vaadin.securitystatus.data.SecurityDataService;
import com.vaadin.securitystatus.data.VersionNumber;
import com.vaadin.securitystatus.data.model.ExplorerFront;
import com.vaadin.securitystatus.data.model.ExplorerFront.Finding;
import com.vaadin.securitystatus.data.model.ExplorerFront.Line;
import com.vaadin.securitystatus.data.model.ExplorerVersion;
import com.vaadin.securitystatus.explorer.ExplorerRules.Group;
import com.vaadin.securitystatus.explorer.ExplorerRules.Product;

import jakarta.annotation.security.RolesAllowed;

/**
 * The security team's front door: one row per maintained line, narrowed by
 * the products an application uses. The series list is the master of a
 * Master Detail Layout and the opened line its detail, so choosing a line
 * replaces the one open before, rule 6.3.4's spirit. The search and the
 * products are on top of the list.
 *
 * <p>
 * The address carries the search, {@code ?q}, and the products turned off,
 * {@code ?off=<name>,<name>}, written by {@link Address}. The selection is
 * remembered in {@link ProductSelection} and only ever applied by writing it
 * into the address.
 */
@Route(value = "findings", layout = InternalLayout.class)
@PageTitle("explorer.title")
@Menu(order = 0, title = "explorer.title")
@RolesAllowed(Roles.SECURITY)
public final class ExplorerView extends Composite<Div> implements BeforeEnterObserver {

    static final String PATH = "findings";

    private final SecurityDataService data;
    private final ProductSelection selection;

    private final ValueSignal<String> query = new ValueSignal<>("");
    private final ValueSignal<Optional<String>> opened = new ValueSignal<>(Optional.empty());

    private final MasterDetailLayout layout = new MasterDetailLayout();
    private final TextField search = new TextField();
    private final Div products = new Div();
    private final Div rows = new Div();
    private final Div outage = new Div();

    public ExplorerView(SecurityDataService data, DataRefreshSignal refresh, ProductSelection selection) {
        this.data = data;
        this.selection = selection;
        addClassName("explorer-view");

        search.addClassName("explorer-search");
        search.setLabel(getTranslation("explorer.search.label"));
        search.setHelperText(getTranslation("explorer.search.helper"));
        search.setClearButtonVisible(true);
        search.setPrefixComponent(VaadinIcon.SEARCH.create());
        search.addValueChangeListener(change -> {
            if (change.isFromClient()) {
                submit(change.getValue());
            }
        });

        products.addClassName("explorer-products");
        rows.addClassName("explorer-rows");
        var master = new Div(search, products, rows);
        master.addClassName("explorer-master");

        layout.addClassName("explorer-layout");
        layout.setMasterSize("26rem");
        layout.setDetailSize("24rem", true);
        layout.setMaster(master);
        layout.addBackdropClickListener(click -> opened.set(Optional.empty()));
        layout.addDetailEscapePressListener(escape -> opened.set(Optional.empty()));

        getContent().add(new ViewHeading(), outage, layout);

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
                Set<String> names = new HashSet<>();
                ExplorerRules.products(data.explorerFront()).stream().filter(Product::switchable)
                        .forEach(product -> names.add(product.name()));
                selection.readStorage(ui, names, restored -> {
                    if (selection.unsettled()) {
                        address(query.peek(), restored).go(ui);
                    }
                });
            } catch (DataUnreachableException unreachable) {
                // Nothing to restore against; everything stays on.
            }
            return null;
        });
    }

    /** The master detail layout, for the tests. */
    MasterDetailLayout layout() {
        return layout;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        QueryParameters asked = event.getLocation().getQueryParameters();
        String q = Address.first(asked, QueryFlag.Q).orElse("");
        List<Product> registry;
        try {
            registry = ExplorerRules.products(data.explorerFront());
        } catch (DataUnreachableException unreachable) {
            query.set(q);
            return;
        }
        if (Address.has(asked, QueryFlag.OFF)) {
            List<String> written = Address.list(asked, QueryFlag.OFF);
            List<String> known = ExplorerRules.known(written, registry);
            selection.select(known);
            if (!known.equals(written) || written.isEmpty()) {
                // Rule 6.2.7: a name that hides nothing here does not stay in the address, and a bare
                // ?off, everything on, is written away once it has cleared the remembered selection.
                event.forwardTo(PATH, address(q, known).query());
                return;
            }
        } else if (selection.remembers()) {
            // Rule 6.7.5: what the reader turned off earlier is written into the address, never applied behind it.
            List<String> remembered = ExplorerRules.known(new ArrayList<>(selection.off().peek()), registry);
            event.forwardTo(PATH, address(q, remembered).query());
            return;
        }
        if (!q.equals(query.peek())) {
            query.set(q);
        }
        if (!search.getValue().equals(q)) {
            search.setValue(q);
        }
    }

    // Writing the address

    /** The explorer's address for a search and a selection. */
    static Address address(String q, List<String> off) {
        return Address.of(PATH).with(QueryFlag.Q, q.isBlank() ? Optional.empty() : Optional.of(q.strip()))
                .list(QueryFlag.OFF, off);
    }

    /**
     * The address that says everything is on. A bare {@code ?off} is read as
     * an empty selection and written away again, so turning everything back
     * on is not undone by the remembered selection.
     */
    static Address allOn(String q) {
        return address(q, List.of()).with(QueryFlag.OFF, Optional.of(""));
    }

    private void submit(String typed) {
        UI ui = UI.getCurrentOrThrow();
        Optional<VersionNumber> version = ExplorerRules.fullVersion(typed);
        if (version.isPresent()) {
            // The source shortcut: a full version is a release to read, not a filter.
            ui.navigate(ReleaseView.class, new RouteParameters(Map.of(ReleaseView.LINE, version.get().line(),
                    ReleaseView.VERSION, version.get().toString())));
            return;
        }
        List<String> off = new ArrayList<>(selection.off().peek());
        (off.isEmpty() ? allOn(typed) : address(typed, ordered(off))).go(ui);
    }

    private List<String> ordered(Iterable<String> names) {
        var wanted = new HashSet<String>();
        names.forEach(wanted::add);
        try {
            return ExplorerRules.known(new ArrayList<>(wanted), ExplorerRules.products(data.explorerFront()));
        } catch (DataUnreachableException unreachable) {
            return new ArrayList<>(wanted);
        }
    }

    // Drawing

    private void draw(Set<String> off, String q, Optional<String> open) {
        outage.removeAll();
        products.removeAll();
        rows.removeAll();
        ExplorerFront front;
        try {
            front = data.explorerFront();
        } catch (DataUnreachableException unreachable) {
            outage.add(new DataOutageNotice());
            layout.setVisible(false);
            return;
        }
        layout.setVisible(true);
        List<Product> registry = ExplorerRules.products(front);
        Set<String> named = ExplorerRules.named(front, q);

        var shownLines = new ArrayList<Line>();
        for (Line line : front.lines()) {
            List<Finding> visible = ExplorerRules.searched(line, ExplorerRules.visible(line, off, registry), q,
                    named);
            if (q.isBlank() || ExplorerRules.namesLine(line, q) || !visible.isEmpty()) {
                shownLines.add(line);
            }
        }

        Optional<Line> openLine = open.flatMap(name -> shownLines.stream().filter(line -> line.line().equals(name))
                .findFirst());
        // The counts are about the opened line, or every line shown when none is open.
        List<Line> scope = openLine.map(List::of).orElse(shownLines);
        drawProducts(registry, off, q, scope);

        if (!q.isBlank()) {
            rows.add(lead("explorer-search-count",
                    getTranslation("explorer.search.count", shownLines.size(), front.lines().size())));
        }
        for (Line line : shownLines) {
            rows.add(row(line, registry, off, q, named, openLine.filter(line::equals).isPresent()));
        }
        layout.setDetail(openLine.map(line -> (Component) detail(line, registry, off, q, named)).orElse(null));
    }

    private void drawProducts(List<Product> registry, Set<String> off, String q, List<Line> scope) {
        var all = new Checkbox(getTranslation("explorer.products.all"));
        all.addClassName("explorer-all");
        boolean allOn = off.isEmpty();
        all.setValue(allOn);
        all.setIndeterminate(!allOn && !ExplorerRules.allOff(off, registry));
        all.addValueChangeListener(change -> {
            if (change.isFromClient()) {
                Set<String> next = ExplorerRules.allToggled(off, registry);
                (next.isEmpty() ? allOn(q) : address(q, ordered(next))).go(UI.getCurrentOrThrow());
            }
        });

        var heading = new H2(getTranslation("explorer.products.heading"));
        heading.addClassName("explorer-products-heading");
        var chips = new Div();
        chips.addClassName("explorer-chips");
        int silent = 0;
        for (Product product : registry) {
            if (!product.switchable()) {
                continue;
            }
            boolean isOff = off.contains(product.name());
            int count = ExplorerRules.marginal(product, scope, off, registry);
            if (count == 0 && !isOff) {
                // Rule 6.5.6: a control that cannot hide anything is not shown. Only one that
                // carries findings here is counted in the sentence; one that carries none is no control.
                if (carries(product, scope, off, registry)) {
                    silent++;
                }
                continue;
            }
            chips.add(chip(product, isOff, count, off, q));
        }
        products.add(heading, all, chips);
        if (silent > 0) {
            products.add(lead("explorer-silent", getTranslation("explorer.products.silent", silent)));
        }
    }

    private static boolean carries(Product product, List<Line> scope, Set<String> off, List<Product> registry) {
        boolean springOff = ExplorerRules.springOff(off, registry);
        return scope.stream().flatMap(line -> ExplorerRules.measured(line, springOff).stream())
                .anyMatch(finding -> finding.modules().contains(product.index()));
    }

    /**
     * One product: a real link to the address with this product toggled, so
     * it can be copied or opened in a new tab, rule 6.2.13, with its ring, its
     * name and its marginal count, rule 6.5.8.
     */
    private Component chip(Product product, boolean isOff, int count, Set<String> off, String q) {
        var next = new LinkedHashSet<>(off);
        if (isOff) {
            next.remove(product.name());
        } else {
            next.add(product.name());
        }
        Address target = next.isEmpty() ? allOn(q) : address(q, ordered(next));
        var link = new RouterLink(ExplorerView.class);
        link.setQueryParameters(target.query());
        link.addClassNames("explorer-chip", isOff ? "off" : "on");
        link.getElement().setAttribute("role", "switch");
        link.getElement().setAttribute("aria-checked", String.valueOf(!isOff));
        link.getElement().setAttribute("data-product", product.name());
        var name = new Span(product.name());
        name.addClassName("chip-name");
        var number = new Span(String.valueOf(count));
        number.addClassName("chip-count");
        link.add(ring(product), name, number);
        if (product.discontinued()) {
            var mark = new Span(getTranslation("explorer.products.discontinued"));
            mark.addClassName("chip-discontinued");
            link.add(mark);
        }
        link.getElement().setAttribute("title", getTranslation(
                isOff ? "explorer.chip.show" : "explorer.chip.hide", count, product.name()));
        return link;
    }

    // The series rows

    private Component row(Line line, List<Product> registry, Set<String> off, String q, Set<String> named,
            boolean isOpen) {
        List<Finding> visible = ExplorerRules.searched(line, ExplorerRules.visible(line, off, registry), q, named);
        var button = new NativeButton();
        button.addClassNames("explorer-row", isOpen ? "open" : "closed");
        button.getElement().setAttribute("aria-pressed", String.valueOf(isOpen));
        button.getElement().setAttribute("data-line", line.line());
        button.addClickListener(click -> opened.set(isOpen ? Optional.empty() : Optional.of(line.line())));

        var top = new Div();
        top.addClassName("explorer-row-top");
        var name = new Span(line.line());
        name.addClassName("explorer-row-line");
        var version = new Span(line.version().toString());
        version.addClassName("explorer-row-version");
        top.add(name, version);
        line.scannedAt().map(OffsetDateTime::toLocalDate).ifPresent(day -> {
            var scanned = new Span(getTranslation("explorer.row.scanned", day.toString()));
            scanned.addClassName("explorer-row-scanned");
            top.add(scanned);
        });

        var badges = new Div();
        badges.addClassName("explorer-row-badges");
        badges.add(badge("vaadin", ExplorerRules.vaadinBadge(visible),
                getTranslation("explorer.badge.vaadin", getTranslation(badgeKey(ExplorerRules.vaadinBadge(visible))))));
        springBadge(line, visible, ExplorerRules.springOff(off, registry)).ifPresent(badges::add);

        var summary = new Paragraph(summary(line, visible, off, registry));
        summary.addClassName("explorer-row-summary");

        button.add(top, badges, summary, rings(visible, registry));
        return button;
    }

    private Optional<Component> springBadge(Line line, List<Finding> visible, boolean springOff) {
        if (Boolean.TRUE.equals(line.noSpring())) {
            return Optional.empty();
        }
        if (springOff) {
            var without = new Badge(getTranslation("explorer.badge.spring.without"));
            without.addClassNames("explorer-badge", "spring", "badge-without");
            return Optional.of(without);
        }
        ExplorerRules.Badge state = ExplorerRules.springBadge(visible);
        String words = getTranslation(badgeKey(state));
        String text = line.springBoot()
                .map(version -> getTranslation("explorer.badge.spring.version", version.toString(), words))
                .orElseGet(() -> getTranslation("explorer.badge.spring", words));
        return Optional.of(badge("spring", state, text));
    }

    private static Badge badge(String side, ExplorerRules.Badge state, String text) {
        var badge = new Badge(text);
        badge.addClassNames("explorer-badge", side, "badge-" + state.name().toLowerCase(java.util.Locale.ROOT));
        return badge;
    }

    private static String badgeKey(ExplorerRules.Badge state) {
        return "explorer.badge.state." + state.name();
    }

    /** One sentence: what is shown against the whole, and how it splits into the groups. */
    private String summary(Line line, List<Finding> visible, Set<String> off, List<Product> registry) {
        Map<Group, List<Finding>> groups = ExplorerRules.grouped(visible);
        int total = ExplorerRules.measured(line, ExplorerRules.springOff(off, registry)).size();
        String split = getTranslation("explorer.summary.split", size(groups, Group.RISK),
                size(groups, Group.UNJUDGED), size(groups, Group.SPRING), size(groups, Group.BUILD),
                size(groups, Group.ANSWERED));
        return visible.size() == total ? getTranslation("explorer.summary.all", total, split)
                : getTranslation("explorer.summary.shown", visible.size(), total, split);
    }

    private static int size(Map<Group, List<Finding>> groups, Group group) {
        return groups.getOrDefault(group, List.of()).size();
    }

    /** One ring per product the shown findings arrive through, in registry order. */
    private Component rings(List<Finding> visible, List<Product> registry) {
        var counts = new int[registry.size()];
        for (Finding finding : visible) {
            for (Product product : ExplorerRules.through(finding, registry)) {
                counts[product.index()]++;
            }
        }
        var rings = new Div();
        rings.addClassName("explorer-rings");
        for (Product product : registry) {
            if (counts[product.index()] > 0) {
                var ring = ring(product);
                ring.getElement().setAttribute("title",
                        getTranslation("explorer.ring", product.name(), counts[product.index()]));
                rings.add(ring);
            }
        }
        return rings;
    }

    private static Span ring(Product product) {
        var ring = new Span();
        ring.addClassNames("product-ring", "ring-" + product.ring());
        ring.getElement().setAttribute("aria-hidden", "true");
        return ring;
    }

    // The opened line

    private Component detail(Line line, List<Product> registry, Set<String> off, String q, Set<String> named) {
        var detail = new Div();
        detail.addClassName("explorer-detail");
        detail.getElement().setAttribute("data-line", line.line());

        var close = new Button(VaadinIcon.CLOSE.create(), click -> opened.set(Optional.empty()));
        close.addThemeVariants(ButtonVariant.TERTIARY);
        close.setAriaLabel(getTranslation("explorer.detail.close"));
        close.addClassName("explorer-detail-close");
        var title = new H2(getTranslation("explorer.detail.title", line.line(), line.version().toString()));
        title.addClassName("explorer-detail-title");
        var head = new Div(title, close);
        head.addClassName("explorer-detail-head");
        detail.add(head);

        boolean springOff = ExplorerRules.springOff(off, registry);
        detail.add(springSwitch(line, registry, springOff, off, q));

        List<Finding> measured = ExplorerRules.measured(line, springOff);
        List<Finding> visible = ExplorerRules.searched(line, ExplorerRules.visible(line, off, registry), q, named);
        detail.add(lead("explorer-detail-summary", summary(line, visible, off, registry)));
        int hidden = ExplorerRules.visible(line, off, registry).size() < measured.size()
                ? measured.size() - ExplorerRules.visible(line, off, registry).size()
                : 0;
        if (hidden > 0) {
            detail.add(lead("explorer-hidden", getTranslation("explorer.detail.hidden", hidden)));
        }

        Map<Group, List<Finding>> groups = ExplorerRules.grouped(visible);
        groups.forEach((group, findings) -> detail.add(group(line, group, findings, registry, off, springOff)));

        if (Boolean.TRUE.equals(line.scannerFiles())) {
            detail.add(new ReleaseFiles(data, line.version()));
        }
        var release = new RouterLink(getTranslation("explorer.detail.release", line.version().toString()),
                ReleaseView.class, new RouteParameters(Map.of(ReleaseView.LINE, line.line(), ReleaseView.VERSION,
                        line.version().toString())));
        release.addClassName("explorer-release-link");
        detail.add(release);
        return detail;
    }

    /**
     * The switch between the full measurement and the one without Spring. It
     * is the Spring Boot starter's chip in another shape, so it writes the same
     * address. A line measured without Spring only says so instead.
     */
    private Component springSwitch(Line line, List<Product> registry, boolean springOff, Set<String> off,
            String q) {
        var holder = new Div();
        holder.addClassName("explorer-spring");
        Optional<Product> starter = registry.stream().filter(Product::switchesShape).findFirst();
        var toggle = new Switch(getTranslation("explorer.spring.switch"));
        toggle.addClassName("explorer-spring-switch");
        holder.add(toggle);
        if (Boolean.TRUE.equals(line.noSpring()) || starter.isEmpty()) {
            toggle.setValue(true);
            toggle.setEnabled(false);
            holder.add(lead("explorer-spring-only", getTranslation("explorer.spring.only")));
            return holder;
        }
        toggle.setValue(springOff);
        toggle.addValueChangeListener(change -> {
            if (change.isFromClient()) {
                var next = new LinkedHashSet<>(off);
                if (change.getValue()) {
                    next.add(starter.get().name());
                } else {
                    next.remove(starter.get().name());
                }
                (next.isEmpty() ? allOn(q) : address(q, ordered(next))).go(UI.getCurrentOrThrow());
            }
        });
        return holder;
    }

    private Component group(Line line, Group group, List<Finding> findings, List<Product> registry,
            Set<String> off, boolean springOff) {
        String key = group.name();
        String heading = group == Group.SPRING && line.springBoot().isPresent()
                ? getTranslation("explorer.group.SPRING.version", line.springBoot().get().toString(),
                        findings.size())
                : getTranslation("explorer.group." + key, findings.size());
        var details = new Details(heading);
        details.addClassNames("explorer-group", "group-" + key.toLowerCase(java.util.Locale.ROOT));
        details.getElement().setAttribute("data-group", key);
        details.setOpened(selection.isOpen(key, group.startsOpen()));
        details.addOpenedChangeListener(change -> {
            if (change.isFromClient()) {
                // Rule 6.7.4: recorded when it happens, keyed on the part of the heading that does not move.
                selection.open(key, change.isOpened());
            }
        });
        if (group == Group.SPRING || group == Group.BUILD) {
            details.add(lead("explorer-group-note", getTranslation("explorer.group." + key + ".note")));
        }
        ExplorerRules.Capped capped = ExplorerRules.capped(findings);
        details.add(findingsGrid(line, capped.shown(), registry, off, springOff));
        if (capped.more() > 0) {
            details.add(lead("explorer-group-more", getTranslation("explorer.group.more", capped.more())));
        }
        return details;
    }

    private Grid<Finding> findingsGrid(Line line, List<Finding> findings, List<Product> registry, Set<String> off,
            boolean springOff) {
        var grid = new Grid<Finding>();
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
        grid.addColumn(finding -> through(finding, registry, off)).setHeader(getTranslation("explorer.grid.through"))
                .setFlexGrow(1);
        grid.addComponentColumn(finding -> dots(finding, registry, off))
                .setHeader(getTranslation("explorer.grid.routes")).setAutoWidth(true).setFlexGrow(0);
        grid.setItemDetailsRenderer(new ComponentRenderer<>(finding -> findingDetails(line, finding, springOff)));
        grid.setAllRowsVisible(true);
        grid.setItems(findings);
        return grid;
    }

    private Component severity(Finding finding) {
        var badge = new Span(finding.severity().map(word -> getTranslation("severity." + word)).orElse(""));
        finding.severity().ifPresent(word -> badge.addClassName("severity-" + word));
        badge.addClassName("explorer-severity");
        return badge;
    }

    /** Through how many of its products a finding arrives, or the one it rides inside. */
    private String through(Finding finding, List<Product> registry, Set<String> off) {
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

    private Component dots(Finding finding, List<Product> registry, Set<String> off) {
        var dots = new Span();
        dots.addClassName("explorer-dots");
        for (Product product : ExplorerRules.through(finding, registry)) {
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

    /**
     * An opened finding, read from the release's own record, which is fetched
     * the first time a finding of that release is opened. A field the record
     * does not carry is left out, rule 6.4.11.
     */
    private Component findingDetails(Line line, Finding finding, boolean springOff) {
        var box = new Div();
        box.addClassName("explorer-finding");
        Optional<ExplorerVersion.Finding> full;
        try {
            full = data.explorerVersion(line.version())
                    .flatMap(version -> version.findings().stream()
                            .filter(candidate -> candidate.cve().equals(finding.cve())).findFirst());
        } catch (DataUnreachableException unreachable) {
            box.add(new DataOutageNotice());
            return box;
        }
        if (full.isEmpty()) {
            box.add(lead("explorer-finding-missing",
                    getTranslation("explorer.finding.missing", line.version().toString())));
        }
        full.ifPresent(found -> {
            found.summary().ifPresent(text -> box.add(field("summary", text)));
            found.impact().ifPresent(text -> box.add(field("impact", text)));
            found.workaround().ifPresent(text -> box.add(field("workaround", text)));
            found.fixedIn().map(version -> getTranslation("explorer.finding.fixedIn", version.toString()))
                    .or(() -> found.fixExpected()).ifPresent(text -> box.add(field("fix", text)));
            routes(found, springOff).ifPresent(box::add);
        });
        var record = new Anchor(recordUrl(finding.cve()), getTranslation("explorer.finding.record", finding.cve()));
        record.setTarget(AnchorTarget.BLANK);
        record.addClassName("explorer-finding-record");
        box.add(record);
        return box;
    }

    private Component field(String name, String text) {
        var label = new H3(getTranslation("explorer.finding." + name));
        var body = new Paragraph(text);
        var field = new Div(label, body);
        field.addClassNames("explorer-field", "field-" + name);
        return field;
    }

    /** One row per package and resolved version, each with its routes, rule 6.4.16 and 6.4.20. */
    private Optional<Component> routes(ExplorerVersion.Finding found, boolean springOff) {
        var list = new Div();
        for (ExplorerVersion.PackageFinding coordinate : found.packages()) {
            List<String> versions = springOff ? coordinate.versionsWithoutSpring() : coordinate.versions();
            List<ExplorerVersion.Route> routes = springOff ? coordinate.routesWithoutSpring() : coordinate.routes();
            if (springOff && versions.isEmpty() && routes.isEmpty()) {
                continue;
            }
            var entry = new Div();
            entry.addClassName("explorer-coordinate");
            var name = new Span(versions.isEmpty() ? coordinate.packageName()
                    : coordinate.packageName() + "@" + String.join(", ", versions));
            name.addClassName("explorer-coordinate-name");
            entry.add(name);
            if (routes.isEmpty()) {
                entry.add(lead("explorer-route-none", getTranslation("explorer.finding.noRoute")));
            }
            for (ExplorerVersion.Route route : routes) {
                var steps = new ArrayList<String>();
                route.entry().ifPresent(steps::add);
                steps.addAll(route.steps());
                var path = new Span(String.join(" › ", steps));
                path.addClassName("explorer-route");
                entry.add(path);
            }
            list.add(entry);
        }
        if (list.getComponentCount() == 0) {
            return Optional.empty();
        }
        var label = new H3(getTranslation("explorer.finding.routes"));
        var field = new Div(label, list);
        field.addClassNames("explorer-field", "field-routes");
        return Optional.of(field);
    }

    private static String recordUrl(String id) {
        return id.startsWith("GHSA-") ? "https://github.com/advisories/" + id
                : "https://www.cve.org/CVERecord?id=" + id;
    }

    private static Paragraph lead(String className, String text) {
        var paragraph = new Paragraph(text);
        paragraph.addClassName(className);
        return paragraph;
    }
}

package com.vaadin.securitystatus.explorer;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.securitystatus.base.security.Roles;
import com.vaadin.securitystatus.base.ui.DataOutageNotice;
import com.vaadin.securitystatus.base.ui.InternalLayout;
import com.vaadin.securitystatus.base.ui.ViewHeading;
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
 * One exact release read in full: the explorer's card for it, with the
 * advice, the downloads and a field to jump to another release. A release
 * nobody measured is answered here, rule 6.9.7, with the line's newest
 * release and its other measured ones offered. The heading names the version
 * and the document title follows, rule 6.1.7.
 */
@Route(value = "findings/release/:line/:version", layout = InternalLayout.class)
@PageTitle("release.title")
@RolesAllowed(Roles.SECURITY)
public final class ReleaseView extends Composite<Div> implements BeforeEnterObserver {

    public static final String LINE = "line";
    public static final String VERSION = "version";

    private final SecurityDataService data;
    private final ProductSelection selection;
    private final TextField lookup = new TextField();
    private final Div body = new Div();

    public ReleaseView(SecurityDataService data, ProductSelection selection) {
        this.data = data;
        this.selection = selection;
        addClassName("release-view");
        body.addClassName("release-body");

        lookup.addClassName("release-lookup");
        lookup.setLabel(getTranslation("release.lookup.label"));
        lookup.setHelperText(getTranslation("release.lookup.helper"));
        lookup.setPrefixComponent(VaadinIcon.SEARCH.create());
        lookup.addValueChangeListener(change -> {
            if (change.isFromClient()) {
                jump(change.getValue());
            }
        });

        getContent().add(new ViewHeading(), back(), lookup, body);
    }

    /**
     * The one way back, above the content and in the same place on both deep
     * pages, rule 6.1.6.
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

    /** A full version is a place and opens its release; anything else is a question for the explorer. */
    private void jump(String typed) {
        UI ui = UI.getCurrentOrThrow();
        Optional<VersionNumber> version = ExplorerRules.fullVersion(typed);
        if (version.isPresent()) {
            ui.navigate(ReleaseView.class, parameters(version.get()));
        } else if (!typed.isBlank()) {
            ExplorerView.address(typed, List.of()).go(ui);
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
        body.removeAll();
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
            body.add(card(front, record.get()));
        } else if (own.isPresent() && own.get().releases().contains(version)) {
            unpublished(own.get(), version);
        } else {
            unmeasured(front, version);
        }
    }

    // A measured release

    private Component card(ExplorerFront front, ExplorerVersion record) {
        List<Product> registry = ExplorerRules.products(front);
        List<Finding> findings = ReleaseRules.findings(record, front.moduleNames());
        Map<Group, List<Finding>> groups = ExplorerRules.grouped(findings);

        var card = new Div();
        card.addClassName("release-card");
        card.getElement().setAttribute("data-version", record.version().toString());

        var top = new Div();
        top.addClassName("release-card-top");
        var name = new Span(record.version().toString());
        name.addClassName("release-card-version");
        top.add(name);
        record.line().ifPresent(line -> {
            var in = new Span(getTranslation("release.card.in", line));
            in.addClassName("release-card-line");
            top.add(in);
        });
        record.scannedAt().map(OffsetDateTime::toLocalDate).ifPresent(day -> {
            var scanned = new Span(getTranslation("explorer.row.scanned", day.toString()));
            scanned.addClassName("release-card-scanned");
            top.add(scanned);
        });

        var badges = new Div();
        badges.addClassName("explorer-row-badges");
        ExplorerRules.Badge ours = ExplorerRules.vaadinBadge(findings);
        badges.add(badge("vaadin", ours, getTranslation("explorer.badge.vaadin", getTranslation(badgeKey(ours)))));
        if (!Boolean.TRUE.equals(record.noSpring())) {
            ExplorerRules.Badge theirs = ExplorerRules.springBadge(findings);
            String words = getTranslation(badgeKey(theirs));
            badges.add(badge("spring", theirs, record.springBoot()
                    .map(boot -> getTranslation("explorer.badge.spring.version", boot.toString(), words))
                    .orElseGet(() -> getTranslation("explorer.badge.spring", words))));
        }

        var summary = new Paragraph(getTranslation("explorer.summary.all", findings.size(),
                getTranslation("explorer.summary.split", size(groups, Group.RISK), size(groups, Group.UNJUDGED),
                        size(groups, Group.SPRING), size(groups, Group.BUILD), size(groups, Group.ANSWERED))));
        summary.addClassName("release-card-summary");

        card.add(top, badges, summary, rings(findings, registry));
        card.add(advice(front, record, size(groups, Group.SPRING)));
        card.add(new ReleaseFiles(data, record.version(), ReleaseRules.files(record.scannerFiles())));
        groups.forEach((group, members) -> card.add(group(record, group, members, registry)));
        return card;
    }

    private static int size(Map<Group, List<Finding>> groups, Group group) {
        return groups.getOrDefault(group, List.of()).size();
    }

    private static Badge badge(String side, ExplorerRules.Badge state, String text) {
        var badge = new Badge(text);
        badge.addClassNames("explorer-badge", side,
                "badge-" + state.name().toLowerCase(java.util.Locale.ROOT));
        return badge;
    }

    private static String badgeKey(ExplorerRules.Badge state) {
        return "explorer.badge.state." + state.name();
    }

    /** One ring per product the findings arrive through, in registry order, as on the explorer's row. */
    private Component rings(List<Finding> findings, List<Product> registry) {
        var counts = new int[registry.size()];
        for (Finding finding : findings) {
            for (Product product : ExplorerRules.through(finding, registry)) {
                counts[product.index()]++;
            }
        }
        var rings = new Div();
        rings.addClassName("explorer-rings");
        for (Product product : registry) {
            if (counts[product.index()] > 0) {
                var ring = FindingGrid.ring(product);
                ring.getElement().setAttribute("title",
                        getTranslation("explorer.ring", product.name(), counts[product.index()]));
                rings.add(ring);
            }
        }
        return rings;
    }

    /**
     * What to do about it: the Vaadin half moves with an update within the
     * line, rule 6.3.9, and the half Spring Boot brings moves with Spring
     * Boot, which no Vaadin release can do.
     */
    private Component advice(ExplorerFront front, ExplorerVersion record, int spring) {
        var advice = new Div();
        advice.addClassName("release-advice");
        Optional<VersionNumber> newest = record.newest();
        String line = record.line().orElse(record.version().line());
        if (newest.isPresent() && newest.get().compareTo(record.version()) > 0) {
            List<ExplorerVersion.Finding> removed = ReleaseRules.removedBy(record);
            Paragraph says;
            if (removed.isEmpty()) {
                says = lead("release-advice-update", getTranslation("release.advice.removesNone",
                        newest.get().toString(), line));
            } else {
                String named = String.join(", ", removed.stream().limit(ReleaseRules.NAMED)
                        .map(ExplorerVersion.Finding::cve).toList());
                says = lead("release-advice-update", removed.size() > ReleaseRules.NAMED
                        ? getTranslation("release.advice.removes.more", newest.get().toString(), line,
                                removed.size(), named, removed.size() - ReleaseRules.NAMED)
                        : getTranslation("release.advice.removes", newest.get().toString(), line, removed.size(),
                                named));
            }
            says.add(new Text(" "));
            says.add(releaseLink(getTranslation("release.advice.newer", newest.get().toString()), newest.get()));
            advice.add(says);
        } else if (newest.filter(record.version()::equals).isPresent()) {
            advice.add(lead("release-advice-update", getTranslation("release.advice.newest", line)));
        }
        if (spring > 0) {
            advice.add(springAdvice(front, record, spring));
        }
        return advice;
    }

    private Component springAdvice(ExplorerFront front, ExplorerVersion record, int spring) {
        var sentences = new ArrayList<String>();
        sentences.add(getTranslation(spring == 1 ? "release.advice.spring.one" : "release.advice.spring.many",
                spring));
        Optional<VersionNumber> reference = record.springBoot();
        Optional<VersionNumber> declared = record.springBootDeclared();
        if (reference.isPresent() && declared.isPresent() && !reference.get().equals(declared.get())) {
            sentences.add(getTranslation("release.advice.spring.mismatch", declared.get().toString(),
                    reference.get().toString()));
        } else if (reference.isPresent() && declared.isPresent()) {
            sentences.add(getTranslation("release.advice.spring.same", reference.get().toString()));
        } else if (reference.isPresent()) {
            sentences.add(getTranslation("release.advice.spring.reference", reference.get().toString()));
        }
        // No entry in spring_boot_latest means nobody could look, which is said by saying nothing.
        ReleaseRules.springTargets(front, record).ifPresent(targets -> {
            var ways = new ArrayList<String>();
            targets.patch().ifPresent(
                    patch -> ways.add(getTranslation("release.advice.spring.patch", patch.toString())));
            targets.minor().ifPresent(
                    minor -> ways.add(getTranslation("release.advice.spring.minor", minor.toString())));
            boolean sold = Optional.ofNullable(front.springBootSupply().get(targets.from().toString()))
                    .flatMap(supply -> supply.commercial()).isPresent();
            sentences.add(ways.isEmpty()
                    ? getTranslation(sold ? "release.advice.spring.none.commercial" : "release.advice.spring.none",
                            targets.from().toString())
                    : getTranslation("release.advice.spring.targets", targets.from().toString(),
                            String.join(getTranslation("release.advice.spring.and"), ways)));
        });
        var says = lead("release-advice-spring", String.join(" ", sentences));
        says.add(new Text(" "));
        var link = new RouterLink(getTranslation("release.advice.spring.link"), SpringView.class);
        link.addClassName("release-spring-link");
        says.add(link);
        return says;
    }

    private Component group(ExplorerVersion record, Group group, List<Finding> findings, List<Product> registry) {
        String key = group.name();
        String heading = group == Group.SPRING && record.springBoot().isPresent()
                ? getTranslation("explorer.group.SPRING.version", record.springBoot().get().toString(),
                        findings.size())
                : getTranslation("explorer.group." + key, findings.size());
        var details = new Details(heading);
        details.addClassNames("explorer-group", "group-" + key.toLowerCase(java.util.Locale.ROOT));
        details.getElement().setAttribute("data-group", key);
        details.setOpened(selection.isOpen(key, group.startsOpen()));
        details.addOpenedChangeListener(change -> {
            if (change.isFromClient()) {
                selection.open(key, change.isOpened());
            }
        });
        if (group == Group.SPRING || group == Group.BUILD) {
            details.add(lead("explorer-group-note", getTranslation("explorer.group." + key + ".note")));
        }
        ExplorerRules.Capped capped = ExplorerRules.capped(findings);
        details.add(new FindingGrid(capped.shown(), registry, Set.of(),
                finding -> new FindingDetail(finding.cve(), record.version(), record.findings().stream()
                        .filter(candidate -> candidate.cve().equals(finding.cve())).findFirst(), false)));
        if (capped.more() > 0) {
            details.add(lead("explorer-group-more", getTranslation("explorer.group.more", capped.more())));
        }
        return details;
    }

    // A release without a record

    private void unpublished(Line line, VersionNumber version) {
        body.add(lead("release-unpublished", getTranslation("release.unpublished", version.toString(), line.line())));
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
            body.add(lead("release-unmeasured",
                    getTranslation("release.unmeasured.unfollowed", version.toString())));
            return;
        }
        Line line = answer.get();
        String sentence;
        if (own.isEmpty()) {
            sentence = getTranslation("release.unmeasured.major", version.toString(), version.line(),
                    ReleaseRules.major(line.line()),
                    line.line(), line.version().toString());
        } else if (version.compareTo(line.version()) > 0) {
            sentence = getTranslation("release.unmeasured.ahead", version.toString(), line.line(),
                    line.version().toString());
        } else {
            sentence = getTranslation("release.unmeasured.line", version.toString(), line.line(),
                    line.version().toString());
        }
        body.add(lead("release-unmeasured", sentence));
        var go = releaseLink(getTranslation("release.unmeasured.go", line.version().toString()), line.version());
        go.addClassName("release-go");
        body.add(new Paragraph(go));

        List<VersionNumber> others = ReleaseRules.others(line, line.version());
        if (!others.isEmpty()) {
            var more = new Paragraph(new Span(getTranslation("release.others", line.line())));
            more.addClassName("release-others");
            for (VersionNumber other : others.stream().limit(ReleaseRules.OTHERS).toList()) {
                more.add(new Text(" "));
                var link = releaseLink(other.toString(), other);
                link.addClassName("release-other");
                more.add(link);
            }
            if (others.size() > ReleaseRules.OTHERS) {
                more.add(new Span(" " + getTranslation("release.others.more", others.size() - ReleaseRules.OTHERS)));
            }
            body.add(more);
        }
    }

    private static Paragraph lead(String className, String text) {
        var paragraph = new Paragraph(text);
        paragraph.addClassName(className);
        return paragraph;
    }
}

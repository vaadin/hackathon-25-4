package com.vaadin.securitystatus.ui.customer;

import static com.vaadin.securitystatus.ui.common.Parts.css;
import static com.vaadin.securitystatus.ui.customer.AnswerParts.pairs;
import static com.vaadin.securitystatus.ui.customer.AnswerParts.pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.markdown.Markdown;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.securitystatus.customer.VersionRules;
import com.vaadin.securitystatus.customer.VerdictFor;
import com.vaadin.securitystatus.data.model.Assessment;
import com.vaadin.securitystatus.data.model.CasesIndex;
import com.vaadin.securitystatus.data.model.VersionAnswer;

/**
 * One finding with no fix yet, as the design draws it: the id, how bad, who
 * owns it, whether a workaround exists, then what to do now and what the fix
 * waits on, and the three layers under it. Everything the verdict does not
 * carry is left out, never written empty.
 */
final class FindingCard extends Composite<Div> {

    private final String advisory;
    private final Layers layers;

    /**
     * @param layered false for the finding drawn inside the red card, whose
     *            layers the design puts at the foot of the screen instead
     */
    FindingCard(VersionAnswer answer, CasesIndex index, String advisory, boolean layered, boolean open) {
        this.advisory = advisory;
        var card = getContent();
        card.addClassName("finding-card");
        card.getElement().setAttribute("data-advisory", advisory);

        List<VerdictFor> said = VersionRules.verdictsFor(answer, advisory);
        Optional<CasesIndex.AdvisoryCatalogEntry> entry = Optional.ofNullable(index.advisories().get(advisory));

        var head = css(new Div(advisoryLink(advisory, answer)), "finding-head");
        entry.flatMap(CasesIndex.AdvisoryCatalogEntry::severity).flatMap(this::severity).ifPresent(head::add);
        entry.flatMap(CasesIndex.AdvisoryCatalogEntry::fixOwner).flatMap(FindingCard::ownerKey)
                .map(this::ownerChip).ifPresent(head::add);
        var vulnerable = css(new Badge(getTranslation(VersionRules.hasWorkaround(said)
                ? "version.finding.vulnerable.workaround" : "version.finding.vulnerable")), "status-chip", "state-unresolved");
        head.add(vulnerable);
        card.add(head);

        for (VerdictFor one : said) {
            var route = css(new Div(), "finding-route");
            if (said.size() > 1 && !one.packages().isEmpty()) {
                var through = css(new Paragraph(getTranslation("version.finding.through", String.join(", ", one.packages()))), "finding-through");
                route.add(through);
            }
            doAndFix(one.assessment()).ifPresent(route::add);
            if (route.getComponentCount() > 0) {
                card.add(route);
            }
        }

        layers = new Layers(answer, index, advisory, said, entry, open);
        if (layered) {
            card.add(layers);
        }
    }

    String advisory() {
        return advisory;
    }

    Layers layers() {
        return layers;
    }

    static RouterLink advisoryLink(String advisory, VersionAnswer answer) {
        return css(new RouterLink(advisory, CveView.class, CveView.parameters(advisory, answer.version().toString())),
                "finding-id", "advisory-id");
    }

    private Optional<Component> severity(String word) {
        return VersionRules.severity(word).map(known -> {
            return css(new Span(getTranslation("severity." + known)), "severity", "severity-" + known);
        });
    }

    private Component ownerChip(String key) {
        return css(new Badge(getTranslation(key)), "owner-chip");
    }

    /** Whose fix it is, in the chip's words: Vaadin's or the dependency's. An owner nobody established has no chip. */
    static Optional<String> ownerKey(String owner) {
        return switch (owner) {
            case "vaadin" -> Optional.of("version.finding.owner.vaadin");
            case "upstream" -> Optional.of("version.finding.owner.dependency");
            default -> Optional.empty();
        };
    }

    /** The two columns the design draws: what to do now, and what the fix waits on. Absent ones are left out. */
    private Optional<Component> doAndFix(Assessment verdict) {
        var columns = css(new Div(), "finding-columns");
        verdict.workaround().ifPresent(text -> columns.add(column(getTranslation("version.finding.do"), new Markdown(text))));
        var fix = new ArrayList<Component>();
        verdict.fixExpected().ifPresent(text -> fix.add(new Markdown(text)));
        if (!verdict.fixPrs().isEmpty()) {
            var tracking = css(new Paragraph(new Span(getTranslation("version.finding.tracking"))), "finding-tracking");
            verdict.fixPrs().forEach(url -> tracking.add(pullRequest(url)));
            fix.add(tracking);
        }
        if (!fix.isEmpty()) {
            columns.add(column(getTranslation("version.finding.fix"), fix.toArray(Component[]::new)));
        }
        return columns.getComponentCount() == 0 ? Optional.empty() : Optional.of(columns);
    }

    private static Div column(String heading, Component... body) {
        var column = new Div(new H4(heading));
        column.add(body);
        column.addClassName("finding-column");
        return column;
    }

    /**
     * A pull request named the way people say it, repository and number. Only
     * an https address becomes a link; anything else the data carries is shown
     * as the text it is, D-262.
     */
    static Component pullRequest(String url) {
        String[] parts = url.split("/");
        String name = parts.length >= 4 ? parts[parts.length - 3] + "#" + parts[parts.length - 1] : url;
        if (!url.regionMatches(true, 0, "https://", 0, 8)) {
            return css(new Span(url), "pull-request");
        }
        return css(new Anchor(url, name), "pull-request");
    }

    /**
     * The three layers of one finding: what it means for the reader, the
     * modules and the fix, and the full advisory. A layer the data gives
     * nothing to is not drawn.
     */
    static final class Layers extends Composite<Div> {

        private final List<Details> opened = new ArrayList<>();
        private Details means;

        Layers(VersionAnswer answer, CasesIndex index, String advisory, List<VerdictFor> said,
                Optional<CasesIndex.AdvisoryCatalogEntry> entry, boolean open) {
            getContent().addClassName("finding-layers");
            Optional<Assessment> first = said.stream().findFirst().map(VerdictFor::assessment);

            var meansBody = pairs();
            first.flatMap(Assessment::impact)
                    .ifPresent(text -> meansBody.add(pair(getTranslation("version.layer.exploited"), new Markdown(text))));
            first.flatMap(Assessment::applicability).or(() -> entry.flatMap(CasesIndex.AdvisoryCatalogEntry::applicability))
                    .map(index.labels()::get)
                    .ifPresent(label -> meansBody.add(pair(getTranslation("version.layer.assessment"), new Paragraph(label))));
            entry.map(CasesIndex.AdvisoryCatalogEntry::packages).filter(list -> !list.isEmpty())
                    .ifPresent(list -> meansBody.add(pair(getTranslation("version.layer.source"),
                            new Paragraph(String.join(", ", list)))));
            means = layer("means", getTranslation("version.layer.means"), meansBody, open);

            var modulesBody = pairs();
            entry.map(CasesIndex.AdvisoryCatalogEntry::modules).filter(list -> !list.isEmpty()).ifPresent(list -> {
                var note = css(new Paragraph(getTranslation("version.layer.affected.note")), "layer-note");
                modulesBody.add(pair(getTranslation("version.layer.affected"), new Paragraph(String.join(", ", list)), note));
            });
            var tracking = new ArrayList<Component>();
            first.map(Assessment::fixPrs).filter(list -> !list.isEmpty()).ifPresent(list -> {
                var prs = new Paragraph();
                list.forEach(url -> prs.add(pullRequest(url)));
                prs.addClassName("finding-tracking");
                tracking.add(prs);
            });
            first.flatMap(Assessment::handover).ifPresent(text -> tracking.add(new Markdown(text)));
            if (!tracking.isEmpty()) {
                modulesBody.add(pair(getTranslation("version.finding.tracking"), tracking.toArray(Component[]::new)));
            }
            layer("modules", getTranslation("version.layer.modules"), modulesBody, open);

            var full = css(new Div(), "layer-full");
            var facts = new ArrayList<String>();
            entry.flatMap(CasesIndex.AdvisoryCatalogEntry::severity).flatMap(VersionRules::severity)
                    .ifPresent(known -> facts.add(getTranslation("version.layer.severity",
                            getTranslation("severity." + known))));
            entry.map(CasesIndex.AdvisoryCatalogEntry::packages).filter(list -> !list.isEmpty())
                    .ifPresent(list -> facts.add(getTranslation("version.layer.reaches", String.join(", ", list))));
            first.flatMap(Assessment::depth).ifPresent(depth -> facts.add(getTranslation("version.layer.depth", depth)));
            first.flatMap(Assessment::assessedAt)
                    .ifPresent(day -> facts.add(getTranslation("version.layer.written", day.toString())));
            if (!facts.isEmpty()) {
                var says = css(new Paragraph(String.join(" ", facts)), "layer-note");
                full.add(says);
            }
            var record = new RouterLink(getTranslation("version.layer.record", advisory), CveView.class,
                    CveView.parameters(advisory, answer.version().toString()));
            full.add(new Paragraph(record));
            layer("full", getTranslation("version.layer.full"), full, false);
        }

        /** The first layer, which a link to the finding opens and which the reader opening it writes into the address. */
        Optional<Details> means() {
            return Optional.ofNullable(means);
        }

        List<Details> all() {
            return opened;
        }

        private Details layer(String key, String title, Div body, boolean open) {
            if (body.getComponentCount() == 0) {
                return null;
            }
            var details = AnswerParts.layer(key, title, body, open);
            getContent().add(details);
            opened.add(details);
            return details;
        }
    }
}

package com.vaadin.securitystatus.ui.team.explorer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Code;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.securitystatus.data.VersionNumber;
import com.vaadin.securitystatus.data.model.ExplorerVersion;

/**
 * An opened finding, read from its release's record, drawn as the prototype
 * draws it, D-255: the coordinate and the advisory's title, then what Vaadin
 * decided as one panel of four labelled fields, what it means, what Vaadin
 * says, what to do today and the fix, each cut to its first sentences with the
 * rest one click away, then the routes and the record link, D-213. A field
 * the record does not carry is left out, rule 6.4.11; a release whose record
 * is not published says so in one sentence.
 */
final class FindingDetail extends Composite<Div> {

    /** How much of a field shows before "more", the prototype's figure. */
    static final int BRIEF = 240;

    /** The first sentences of a text, and whether anything was left for "more". */
    record Brief(String text, boolean cut) {
    }

    FindingDetail(String cve, VersionNumber release, Optional<ExplorerVersion.Finding> record,
            boolean springOff) {
        var box = getContent();
        box.addClassName("explorer-finding");
        if (record.isEmpty()) {
            box.add(lead("explorer-finding-missing", getTranslation("explorer.finding.missing", release.toString())));
        }
        record.ifPresent(found -> {
            List<Coordinate> coordinates = coordinates(found, springOff);
            var top = new Div();
            top.addClassName("explorer-finding-top");
            coordinates.forEach(coordinate -> top.add(pill(coordinate.name())));
            found.summary().ifPresent(text -> top.add(lead("explorer-finding-title", text)));
            if (top.getComponentCount() > 0) {
                box.add(top);
            }
            var verdict = new Div();
            verdict.addClassName("explorer-verdict");
            found.impact().ifPresent(text -> verdict.add(field("impact", text)));
            found.commentary().ifPresent(text -> verdict.add(field("commentary", text)));
            found.workaround().ifPresent(text -> verdict.add(field("workaround", text)));
            found.fixedIn().map(version -> getTranslation("explorer.finding.fixedIn", version.toString()))
                    .or(found::fixExpected).ifPresent(text -> verdict.add(field("fix", text)));
            if (verdict.getComponentCount() > 0) {
                box.add(verdict);
            }
            routes(coordinates).ifPresent(box::add);
        });
        var link = new Anchor(recordUrl(cve), getTranslation("explorer.finding.record"));
        link.setTarget(AnchorTarget.BLANK);
        link.addClassName("explorer-finding-record");
        box.add(link);
    }

    /** Where an advisory's own record is: GitHub's database for a GHSA id, cve.org otherwise. */
    static String recordUrl(String id) {
        return id.startsWith("GHSA-") ? "https://github.com/advisories/" + id
                : "https://www.cve.org/CVERecord?id=" + id;
    }

    /**
     * The first sentences of a field, up to about a paragraph, cut where a
     * sentence ends, or at a word when no sentence ends late enough. The rest
     * is the same text, never rewritten.
     */
    static Brief brief(String text) {
        String whole = text.strip();
        if (whole.length() <= BRIEF) {
            return new Brief(whole, false);
        }
        int stop = -1;
        int at = whole.indexOf(". ");
        while (at >= 0 && at < BRIEF) {
            stop = at;
            at = whole.indexOf(". ", at + 1);
        }
        if (stop < 60) {
            return new Brief(whole.substring(0, BRIEF).replaceFirst("\\s+\\S*$", ""), true);
        }
        return new Brief(whole.substring(0, stop + 1), true);
    }

    /** A label and its text, the text cut by {@link #brief} with "more" at the end of its last line. */
    private Component field(String name, String text) {
        var label = new Span(getTranslation("explorer.finding." + name));
        label.addClassName("explorer-field-label");
        Brief shown = brief(text);
        var body = new Paragraph(shown.text());
        if (shown.cut()) {
            var more = new Button(getTranslation("explorer.finding.more"));
            more.addThemeVariants(ButtonVariant.TERTIARY);
            more.addClassName("explorer-more");
            Tooltip.forComponent(more).setText(getTranslation("explorer.finding.more.title"));
            more.addClickListener(click -> {
                body.removeAll();
                body.setText(text.strip());
            });
            body.add(new Text(" "), more);
        }
        var field = new Div(label, body);
        field.addClassNames("explorer-field", "field-" + name);
        return field;
    }

    /**
     * The paths each product brings this advisory by, in the shape being read,
     * the steps after the product joined as the prototype joins them.
     */
    static Map<String, List<String>> pathsByProduct(ExplorerVersion.Finding found, boolean springOff) {
        var byProduct = new LinkedHashMap<String, List<String>>();
        for (ExplorerVersion.PackageFinding coordinate : found.packages()) {
            for (ExplorerVersion.Route route : springOff ? coordinate.routesWithoutSpring() : coordinate.routes()) {
                route.entry().ifPresent(product -> byProduct.computeIfAbsent(product, name -> new ArrayList<>())
                        .add(String.join(" -> ", route.steps())));
            }
        }
        return byProduct;
    }

    /** One package and the versions the shape being read resolves, with the routes to it. */
    private record Coordinate(String name, List<String> paths) {
    }

    /** Every coordinate of the finding, rule 6.4.16 and 6.4.20, in the shape the page reads. */
    private static List<Coordinate> coordinates(ExplorerVersion.Finding found, boolean springOff) {
        var coordinates = new ArrayList<Coordinate>();
        for (ExplorerVersion.PackageFinding coordinate : found.packages()) {
            List<String> versions = springOff ? coordinate.versionsWithoutSpring() : coordinate.versions();
            List<ExplorerVersion.Route> routes = springOff ? coordinate.routesWithoutSpring() : coordinate.routes();
            if (springOff && versions.isEmpty() && routes.isEmpty()) {
                continue;
            }
            var paths = new ArrayList<String>();
            for (ExplorerVersion.Route route : routes) {
                var steps = new ArrayList<String>();
                route.entry().ifPresent(steps::add);
                steps.addAll(route.steps());
                paths.add(String.join(" › ", steps));
            }
            coordinates.add(new Coordinate(versions.isEmpty() ? coordinate.packageName()
                    : coordinate.packageName() + " " + String.join(", ", versions), paths));
        }
        return coordinates;
    }

    /** The routes, one per line; the coordinate is named again only where there are several. */
    private Optional<Component> routes(List<Coordinate> coordinates) {
        if (coordinates.isEmpty()) {
            return Optional.empty();
        }
        var list = new Div();
        list.addClassName("explorer-routes");
        boolean several = coordinates.size() > 1;
        for (Coordinate coordinate : coordinates) {
            if (several) {
                list.add(lead("explorer-coordinate-name", coordinate.name()));
            }
            if (coordinate.paths().isEmpty()) {
                list.add(lead("explorer-route-none", getTranslation("explorer.finding.noRoute")));
            }
            for (String path : coordinate.paths()) {
                var line = new Code(path);
                line.addClassName("explorer-route");
                list.add(line);
            }
        }
        return Optional.of(list);
    }

    private static Component pill(String coordinate) {
        var code = new Code(coordinate);
        code.addClassName("explorer-finding-pkg");
        return code;
    }

    private static Paragraph lead(String className, String text) {
        var paragraph = new Paragraph(text);
        paragraph.addClassName(className);
        return paragraph;
    }
}

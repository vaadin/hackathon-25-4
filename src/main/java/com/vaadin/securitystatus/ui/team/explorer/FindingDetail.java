package com.vaadin.securitystatus.ui.team.explorer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.securitystatus.data.VersionNumber;
import com.vaadin.securitystatus.data.model.ExplorerVersion;

/**
 * An opened finding, read from its release's record: the summary, what it
 * means, what Vaadin says, what to do today, the fix and the routes per
 * coordinate, under the prototype's headings, D-213. A field the
 * record does not carry is left out, rule 6.4.11; a release whose record is
 * not published says so in one sentence. The advisory record link is always
 * there, because it is built from the id.
 */
final class FindingDetail extends Composite<Div> {

    FindingDetail(String cve, VersionNumber release, Optional<ExplorerVersion.Finding> record,
            boolean springOff) {
        var box = getContent();
        box.addClassName("explorer-finding");
        if (record.isEmpty()) {
            box.add(lead("explorer-finding-missing", getTranslation("explorer.finding.missing", release.toString())));
        }
        record.ifPresent(found -> {
            found.summary().ifPresent(text -> box.add(lead("explorer-finding-summary", text)));
            found.impact().ifPresent(text -> box.add(field("impact", text)));
            found.commentary().ifPresent(text -> box.add(field("commentary", text)));
            found.workaround().ifPresent(text -> box.add(field("workaround", text)));
            found.fixedIn().map(version -> getTranslation("explorer.finding.fixedIn", version.toString()))
                    .or(found::fixExpected).ifPresent(text -> box.add(field("fix", text)));
            routes(found, springOff).ifPresent(box::add);
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

    private static Paragraph lead(String className, String text) {
        var paragraph = new Paragraph(text);
        paragraph.addClassName(className);
        return paragraph;
    }
}

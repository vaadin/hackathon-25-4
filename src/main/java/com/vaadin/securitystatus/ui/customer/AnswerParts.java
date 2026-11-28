package com.vaadin.securitystatus.ui.customer;

import static com.vaadin.securitystatus.ui.common.Parts.css;
import static com.vaadin.securitystatus.ui.common.Parts.decorative;

import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.securitystatus.data.Severity;

/** The parts the version, CVE and finding answers are built from, so each is drawn the same way. */
final class AnswerParts {

    private AnswerParts() {
    }

    /** An advisory id that opens its answer, at the version given when there is one. */
    static RouterLink advisoryLink(String advisory, String version, String className) {
        return css(new RouterLink(advisory, CveView.class, CveView.parameters(advisory, version)), className,
                "advisory-id");
    }

    /** The severity a record names, in its colour, or nothing for one the data does not establish. */
    static Optional<Component> severity(Component on, Optional<String> word) {
        return word.flatMap(Severity::known)
                .map(known -> css(new Span(on.getTranslation("severity." + known)), "severity", "severity-" + known));
    }

    /** Whose fix it is, by the fix owner rule, D-51, or nothing for an owner nobody established. */
    static Optional<Component> ownerChip(Component on, Optional<String> owner) {
        return owner.flatMap(FindingCard::ownerKey).map(key -> css(new Badge(on.getTranslation(key)), "owner-chip"));
    }

    /** The verdict card's first row: its mark, then its word. */
    static Div verdictState(VaadinIcon mark, String word) {
        return css(new Div(decorative(mark.create(), "verdict-icon"), new Span(word)), "verdict-state");
    }

    /** One collapsible layer of an answer, named by its key for the page's own links. */
    static Details layer(String key, String title, Component body, boolean open) {
        var details = css(new Details(title, body), "finding-layer");
        details.getElement().setAttribute("data-layer", key);
        details.setOpened(open);
        return details;
    }

    /** The two column grid of headed pairs inside a layer. */
    static Div pairs() {
        return css(new Div(), "layer-pairs");
    }

    static Div pair(String heading, Component... body) {
        var pair = new Div(new H4(heading));
        pair.add(body);
        return pair;
    }

    static Paragraph note(String text) {
        return css(new Paragraph(text), "layer-note");
    }

    /** A section's H3 with an optional aside on the same row: a count as text, or a component. */
    static Component heading(String title, @Nullable Object aside) {
        return heading(new H3(title), aside);
    }

    /** A section's heading, at whatever level the page gives it, with an optional aside on the same row. */
    static Component heading(Component title, @Nullable Object aside) {
        var row = css(new Div(title), "section-row");
        if (aside instanceof String text) {
            row.add(css(new Span(text), "section-aside"));
        } else if (aside instanceof Component component) {
            component.getElement().getClassList().add("section-aside");
            row.add(component);
        }
        return row;
    }
}

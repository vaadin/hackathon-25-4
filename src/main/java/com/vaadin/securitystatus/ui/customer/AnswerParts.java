package com.vaadin.securitystatus.ui.customer;

import static com.vaadin.securitystatus.ui.common.Parts.css;
import static com.vaadin.securitystatus.ui.common.Parts.decorative;

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;

/** The parts the version, CVE and finding answers are built from, so each is drawn the same way. */
final class AnswerParts {

    private AnswerParts() {
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
        var row = css(new Div(new H3(title)), "section-row");
        if (aside instanceof String text) {
            row.add(css(new Span(text), "section-aside"));
        } else if (aside instanceof Component component) {
            component.getElement().getClassList().add("section-aside");
            row.add(component);
        }
        return row;
    }
}

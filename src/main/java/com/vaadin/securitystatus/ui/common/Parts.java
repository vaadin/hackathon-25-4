package com.vaadin.securitystatus.ui.common;

import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Table;

/**
 * The small builders every view reaches for. {@code addClassName} returns
 * nothing, so without these a component and its class take two lines each
 * time; with them, one.
 */
public final class Parts {

    private Parts() {
    }

    /** The component, carrying these classes. */
    public static <C extends HasStyle> C css(C component, String... classNames) {
        component.addClassNames(classNames);
        return component;
    }

    /** The component, carrying these classes and hidden from screen readers: a mark its words already say. */
    public static <C extends HasStyle & HasElement> C decorative(C component, String... classNames) {
        component.getElement().setAttribute("aria-hidden", "true");
        return css(component, classNames);
    }

    /** A paragraph of text with one class: the hints, leads and notes of every screen. */
    public static Paragraph paragraph(String className, String text) {
        return css(new Paragraph(text), className);
    }

    /** A table of an answer, in the prototype's frame, with its header row. */
    public static Table answerTable(String... headers) {
        var table = css(new Table(), "answer-table", "vaadin-themed-html");
        table.addHeaderRow(headers);
        return table;
    }
}

package com.vaadin.securitystatus.status;

import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.Tag;

/** The {@code <mark>} element, for the part of a coordinate that differs from its neighbours. */
@Tag("mark")
final class Mark extends HtmlContainer {

    Mark(String text) {
        setText(text);
    }
}

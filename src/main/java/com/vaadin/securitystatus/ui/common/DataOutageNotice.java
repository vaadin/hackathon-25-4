package com.vaadin.securitystatus.ui.common;

import com.vaadin.flow.component.html.Paragraph;

/**
 * The one sentence a screen shows when it has no data to answer from: nothing
 * cached and the published measurements out of reach. Shown in place of the
 * answer, never beside a stale one, which carries its own date instead.
 */
public final class DataOutageNotice extends Paragraph {

    public DataOutageNotice() {
        setText(getTranslation("data.unreachable"));
        addClassName("data-outage");
        getElement().setAttribute("role", "status");
    }
}

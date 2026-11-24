package com.vaadin.securitystatus.ui.common;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Section;

/**
 * The one shape a hint takes, rule 6.11.3 of the source spec: a question mark
 * on the thing the hint is about, opening a dialog rendered closed. The title
 * of the dialog is also the mark's accessible name and its tooltip, because a
 * mark with no words has to say what it opens before it is clicked. The dialog
 * closes by its own button, the backdrop and Escape.
 */
public final class HelpDialog extends Composite<Button> {

    private final Dialog dialog = new Dialog();

    public HelpDialog(String title, Component... body) {
        var mark = getContent();
        mark.setText("?");
        mark.addClassName("help-mark");
        mark.addThemeVariants(ButtonVariant.TERTIARY);
        mark.setAriaLabel(title);
        mark.setTooltipText(title);
        mark.addClickListener(click -> dialog.open());

        dialog.addClassName("help-dialog");
        dialog.setHeaderTitle(title);
        dialog.add(body);
        var close = new Button(getTranslation("help.close"), click -> dialog.close());
        close.addThemeVariants(ButtonVariant.PRIMARY);
        dialog.getFooter().add(close);
    }

    /** One part of a hint: a heading and the paragraphs under it. */
    public static Section section(String heading, String... paragraphs) {
        var section = new Section(new H4(heading));
        for (String paragraph : paragraphs) {
            section.add(new Paragraph(paragraph));
        }
        return section;
    }

    public boolean isOpened() {
        return dialog.isOpened();
    }
}

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

    public HelpDialog(String title, Component... body) {
        var dialog = dialog(title, body);
        var mark = getContent();
        mark.setText("?");
        mark.addClassName("help-mark");
        mark.setAriaLabel(title);
        mark.setTooltipText(title);
        mark.addClickListener(click -> dialog.open());
    }

    /** The dialog a hint opens: its title, its body and the one button that closes it. */
    public static Dialog dialog(String title, Component... body) {
        var dialog = new Dialog(title, body);
        dialog.addClassName("help-dialog");
        var close = new Button(dialog.getTranslation("help.close"), click -> dialog.close());
        close.addThemeVariants(ButtonVariant.PRIMARY);
        dialog.getFooter().add(close);
        return dialog;
    }

    /** One part of a hint: a heading and the paragraphs under it. */
    public static Section section(String heading, String... paragraphs) {
        var section = new Section(new H4(heading));
        for (String paragraph : paragraphs) {
            section.add(new Paragraph(paragraph));
        }
        return section;
    }
}

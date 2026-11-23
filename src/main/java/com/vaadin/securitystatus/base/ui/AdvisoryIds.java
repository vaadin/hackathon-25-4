package com.vaadin.securitystatus.base.ui;

import java.util.List;

import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Span;

/**
 * Advisory ids inside a translated sentence, each drawn whole: an id is read
 * and searched as one word, so it never breaks at its hyphens.
 */
public final class AdvisoryIds {

    /** Passed to the translation where the ids go. */
    public static final String HERE = "\u0000";

    private AdvisoryIds() {
    }

    /** Adds the sentence to the target with the ids, comma separated, in place of {@link #HERE}. */
    public static void fill(HasComponents target, String sentence, List<String> ids) {
        int at = sentence.indexOf(HERE);
        if (at < 0) {
            target.add(new Text(sentence));
            return;
        }
        target.add(new Text(sentence.substring(0, at)));
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) {
                target.add(new Text(", "));
            }
            var id = new Span(ids.get(i));
            id.addClassName("advisory-id");
            target.add(id);
        }
        target.add(new Text(sentence.substring(at + HERE.length())));
    }
}

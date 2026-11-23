package com.vaadin.securitystatus.base.ui;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.ColorScheme;
import com.vaadin.flow.component.page.WebStorage;
import com.vaadin.flow.component.select.Select;

/**
 * The color scheme choice: follow the system, light or dark. It starts on the
 * remembered choice or, failing that, on the system, and a change by the
 * reader is remembered. Choosing the system again forgets the choice.
 */
public final class ThemeSwitch extends Composite<Select<ThemeSwitch.Scheme>> {

    /** The three positions, in the order the select lists them. */
    public enum Scheme {
        SYSTEM, LIGHT, DARK
    }

    ThemeSwitch() {
        addClassName("theme-switch");
        Select<Scheme> select = getContent();
        select.setAriaLabel(getTranslation("shell.theme.label"));
        select.setItems(Scheme.values());
        select.setItemLabelGenerator(scheme -> getTranslation("shell.theme." + scheme.name().toLowerCase()));
        select.addValueChangeListener(change -> {
            if (change.isFromClient() && change.getValue() != null) {
                UI ui = change.getSource().getUI().orElseThrow();
                apply(ui, change.getValue());
                switch (change.getValue()) {
                    case SYSTEM -> WebStorage.removeItem(ui, WebStorage.Storage.LOCAL_STORAGE, ThemeMemory.KEY);
                    case LIGHT -> WebStorage.setItem(ui, WebStorage.Storage.LOCAL_STORAGE, ThemeMemory.KEY,
                            ThemeMemory.LIGHT);
                    case DARK -> WebStorage.setItem(ui, WebStorage.Storage.LOCAL_STORAGE, ThemeMemory.KEY,
                            ThemeMemory.DARK);
                }
            }
        });
        addAttachListener(attach -> restore(attach.getUI()));
    }

    private void restore(UI ui) {
        WebStorage.getItem(ui, WebStorage.Storage.LOCAL_STORAGE, ThemeMemory.KEY).thenAccept(remembered -> {
            Scheme scheme = ThemeMemory.DARK.equals(remembered) ? Scheme.DARK
                    : ThemeMemory.LIGHT.equals(remembered) ? Scheme.LIGHT : Scheme.SYSTEM;
            if (scheme != Scheme.SYSTEM) {
                apply(ui, scheme);
            }
            getContent().setValue(scheme);
        });
    }

    private static void apply(UI ui, Scheme scheme) {
        ui.getPage().setColorScheme(switch (scheme) {
            case SYSTEM -> ColorScheme.Value.LIGHT_DARK;
            case LIGHT -> ColorScheme.Value.LIGHT;
            case DARK -> ColorScheme.Value.DARK;
        });
    }
}

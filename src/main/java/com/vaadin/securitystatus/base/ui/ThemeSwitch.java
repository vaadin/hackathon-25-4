package com.vaadin.securitystatus.base.ui;

import java.util.EnumMap;
import java.util.Map;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.page.ColorScheme;
import com.vaadin.flow.component.page.WebStorage;

/**
 * The color scheme choice: a half light, half dark button whose menu offers
 * following the system, light or dark, the current one checked. It starts on
 * the remembered choice or, failing that, on the system, and a change by the
 * reader is remembered. Choosing the system again forgets the choice, D-239.
 */
public final class ThemeSwitch extends Composite<MenuBar> {

    /** The three positions, in the order the menu lists them. */
    public enum Scheme {
        SYSTEM, LIGHT, DARK
    }

    private final Map<Scheme, MenuItem> items = new EnumMap<>(Scheme.class);

    ThemeSwitch() {
        addClassName("theme-switch");
        MenuBar bar = getContent();
        bar.addThemeVariants(MenuBarVariant.TERTIARY);
        String label = getTranslation("shell.theme.label");
        MenuItem button = bar.addItem(VaadinIcon.ADJUST.create());
        button.setAriaLabel(label);
        bar.setTooltipText(button, label);
        for (Scheme scheme : Scheme.values()) {
            MenuItem item = button.getSubMenu()
                    .addItem(getTranslation("shell.theme." + scheme.name().toLowerCase()), click -> choose(scheme));
            item.setCheckable(true);
            item.addClassName("theme-" + scheme.name().toLowerCase());
            items.put(scheme, item);
        }
        addAttachListener(attach -> restore(attach.getUI()));
    }

    private void choose(Scheme scheme) {
        UI ui = getUI().orElseThrow();
        apply(ui, scheme);
        mark(scheme);
        switch (scheme) {
            case SYSTEM -> WebStorage.removeItem(ui, WebStorage.Storage.LOCAL_STORAGE, ThemeMemory.KEY);
            case LIGHT -> WebStorage.setItem(ui, WebStorage.Storage.LOCAL_STORAGE, ThemeMemory.KEY, ThemeMemory.LIGHT);
            case DARK -> WebStorage.setItem(ui, WebStorage.Storage.LOCAL_STORAGE, ThemeMemory.KEY, ThemeMemory.DARK);
        }
    }

    private void restore(UI ui) {
        WebStorage.getItem(ui, WebStorage.Storage.LOCAL_STORAGE, ThemeMemory.KEY).thenAccept(remembered -> {
            Scheme scheme = ThemeMemory.DARK.equals(remembered) ? Scheme.DARK
                    : ThemeMemory.LIGHT.equals(remembered) ? Scheme.LIGHT : Scheme.SYSTEM;
            if (scheme != Scheme.SYSTEM) {
                apply(ui, scheme);
            }
            mark(scheme);
        });
    }

    private void mark(Scheme chosen) {
        items.forEach((scheme, item) -> item.setChecked(scheme == chosen));
        getElement().setAttribute("data-scheme", chosen.name().toLowerCase());
    }

    private static void apply(UI ui, Scheme scheme) {
        ui.getPage().setColorScheme(switch (scheme) {
            case SYSTEM -> ColorScheme.Value.LIGHT_DARK;
            case LIGHT -> ColorScheme.Value.LIGHT;
            case DARK -> ColorScheme.Value.DARK;
        });
    }
}

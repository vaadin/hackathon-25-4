package com.vaadin.securitystatus.base.ui;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.page.ColorScheme;
import com.vaadin.flow.component.page.WebStorage;

/**
 * The color scheme choice: one icon button that steps through following the
 * system, light and dark, its icon saying which one is on, a half circle, a
 * sun or a moon. It starts on the remembered choice or, failing that, on the
 * system, and a change by the reader is remembered. Coming round to the
 * system again forgets the choice, D-247.
 */
public final class ThemeSwitch extends Composite<Button> {

    /** The three positions, in the order a click steps through them. */
    public enum Scheme {
        SYSTEM(VaadinIcon.ADJUST), LIGHT(VaadinIcon.SUN_O), DARK(VaadinIcon.MOON);

        final VaadinIcon icon;

        Scheme(VaadinIcon icon) {
            this.icon = icon;
        }

        Scheme next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    private Scheme current = Scheme.SYSTEM;

    ThemeSwitch() {
        addClassName("theme-switch");
        getContent().addThemeVariants(ButtonVariant.TERTIARY);
        getContent().addClickListener(click -> choose(current.next()));
        mark(Scheme.SYSTEM);
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

    private void mark(Scheme scheme) {
        current = scheme;
        Button button = getContent();
        button.setIcon(scheme.icon.create());
        String says = getTranslation("shell.theme.now", name(scheme), name(scheme.next()));
        button.setAriaLabel(says);
        button.setTooltipText(says);
        getElement().setAttribute("data-scheme", scheme.name().toLowerCase());
    }

    private String name(Scheme scheme) {
        return getTranslation("shell.theme." + scheme.name().toLowerCase());
    }

    private static void apply(UI ui, Scheme scheme) {
        ui.getPage().setColorScheme(switch (scheme) {
            case SYSTEM -> ColorScheme.Value.LIGHT_DARK;
            case LIGHT -> ColorScheme.Value.LIGHT;
            case DARK -> ColorScheme.Value.DARK;
        });
    }
}

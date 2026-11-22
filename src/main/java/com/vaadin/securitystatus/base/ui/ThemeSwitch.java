package com.vaadin.securitystatus.base.ui;

import java.io.Serializable;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.checkbox.Switch;
import com.vaadin.flow.component.page.ColorScheme;
import com.vaadin.flow.component.page.JavaScriptResult;
import com.vaadin.flow.component.page.WebStorage;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;

/**
 * The dark mode switch. It starts where the remembered choice or, failing
 * that, the system preference puts it, and a change by the reader is
 * remembered.
 */
public final class ThemeSwitch extends Composite<Switch> {

    /** Declared so the build bundles it and nothing is compiled from a string in the browser. */
    @JsDefinition
    public interface SystemPreferenceJs extends Serializable {

        @JsExpression("return window.matchMedia('(prefers-color-scheme: dark)').matches")
        JavaScriptResult<Boolean> prefersDark();
    }

    ThemeSwitch() {
        addClassName("theme-switch");
        getContent().setLabel(getTranslation("shell.theme.dark"));
        getContent().addValueChangeListener(change -> {
            if (change.isFromClient()) {
                boolean dark = change.getValue();
                apply(change.getSource().getUI().orElseThrow(), dark);
                WebStorage.setItem(ThemeMemory.KEY, dark ? ThemeMemory.DARK : ThemeMemory.LIGHT);
            }
        });
        addAttachListener(attach -> restore(attach.getUI()));
    }

    private void restore(UI ui) {
        WebStorage.getItem(ui, WebStorage.Storage.LOCAL_STORAGE, ThemeMemory.KEY).thenAccept(remembered -> {
            if (ThemeMemory.DARK.equals(remembered) || ThemeMemory.LIGHT.equals(remembered)) {
                boolean dark = ThemeMemory.DARK.equals(remembered);
                apply(ui, dark);
                getContent().setValue(dark);
            } else {
                getElement().executeJs(SystemPreferenceJs.class).prefersDark()
                        .then(prefersDark -> getContent().setValue(prefersDark));
            }
        });
    }

    private static void apply(UI ui, boolean dark) {
        ui.getPage().setColorScheme(dark ? ColorScheme.Value.DARK : ColorScheme.Value.LIGHT);
    }
}

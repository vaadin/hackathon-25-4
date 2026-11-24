package com.vaadin.securitystatus.ui.common;

/**
 * Where the reader's light or dark choice is remembered, and the script that
 * applies it before the first paint. Without a remembered choice the system
 * preference stands.
 */
public final class ThemeMemory {

    public static final String KEY = "security-status.theme";

    static final String DARK = "dark";
    static final String LIGHT = "light";

    /**
     * Runs in the document head, ahead of every stylesheet. A browser that
     * refuses storage throws, and the system preference wins silently.
     */
    public static final String FIRST_PAINT_SCRIPT = """
            try {
              var remembered = localStorage.getItem('%s');
              if (remembered === '%s' || remembered === '%s') {
                document.documentElement.style.colorScheme = remembered;
              }
            } catch (unavailable) {
            }
            """.formatted(KEY, DARK, LIGHT);

    private ThemeMemory() {
    }
}

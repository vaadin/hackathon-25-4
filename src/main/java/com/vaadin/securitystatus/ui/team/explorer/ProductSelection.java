package com.vaadin.securitystatus.ui.team.explorer;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.WebStorage;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;

/**
 * What the reader arranged on the explorer, for the whole session and, through
 * the browser's local storage, between visits: which products are off and
 * which groups they opened or closed, rules 6.7.1 to 6.7.8 of the source page.
 *
 * <p>
 * The address is the truth for the products: the view sets the selection from
 * every address it arrives at, and a remembered selection is only ever applied
 * by writing it into the address, rule 6.7.5. Storage that throws or comes
 * back empty leaves the default, silently: everything on but what Vaadin no
 * longer develops, D-206. An empty value stored is everything on.
 */
@SpringComponent
@VaadinSessionScope
public class ProductSelection {

    static final String OFF_KEY = "security-status.explorer.off";
    static final String GROUPS_KEY = "security-status.explorer.groups";

    private final ValueSignal<Set<String>> off = new ValueSignal<>(Set.of());
    private final ValueSignal<Map<String, Boolean>> groups = new ValueSignal<>(Map.of());

    /** Whether an address or the storage has said anything about the products in this session. */
    private boolean settled;
    /** Whether the storage was asked in this session. */
    private boolean storageAsked;
    /** Names the storage remembered that this data has no product for, kept so they are not forgotten. */
    private final Set<String> elsewhere = new LinkedHashSet<>();

    /** The products turned off, by registry name. */
    public Signal<Set<String>> off() {
        return off;
    }

    /** Which groups the reader opened or closed by hand, by their stable key. */
    public Signal<Map<String, Boolean>> groups() {
        return groups;
    }

    /** The selection an address asked for, already cleaned of names this data does not have. */
    void select(Collection<String> names) {
        settled = true;
        var next = Set.copyOf(names);
        if (!next.equals(off.peek())) {
            off.set(next);
        }
        save();
    }

    /**
     * A selection to write into an address that carries none: what this
     * session decided, when it differs from the default, D-206.
     */
    boolean remembers(Set<String> defaults) {
        return settled && !off.peek().equals(defaults);
    }

    /** What is off when nothing has said otherwise: the default, which settles nothing and is not saved. */
    void useDefault(Set<String> defaults) {
        if (!settled && !off.peek().equals(defaults)) {
            off.set(Set.copyOf(defaults));
        }
    }

    /** Whether nothing has said anything about the products yet in this session. */
    boolean unsettled() {
        return !settled;
    }

    /** Records a group opened or closed by hand, merged into what is remembered, rules 6.7.3 and 6.7.4. */
    void open(String key, boolean opened) {
        var next = new LinkedHashMap<>(groups.peek());
        next.put(key, opened);
        groups.set(next);
        save();
    }

    /** Whether a group is open: as the reader left it, or as it starts. */
    boolean isOpen(String key, boolean startsOpen) {
        return groups.peek().getOrDefault(key, startsOpen);
    }

    /**
     * Asks the browser once per session for what an earlier visit remembered.
     * Groups merge under what this session already decided; products are
     * handed to {@code restore}, which writes them into the address, and only
     * while nothing else has settled the selection.
     */
    void readStorage(UI ui, Set<String> products, Consumer<List<String>> restore) {
        if (storageAsked) {
            return;
        }
        storageAsked = true;
        WebStorage.getItem(ui, WebStorage.Storage.LOCAL_STORAGE, GROUPS_KEY).thenAccept(stored -> {
            var merged = new LinkedHashMap<>(parseGroups(stored));
            merged.putAll(groups.peek());
            groups.set(merged);
        });
        WebStorage.getItem(ui, WebStorage.Storage.LOCAL_STORAGE, OFF_KEY).thenAccept(stored -> {
            if (stored == null) {
                // Nothing remembered: the default stands.
                return;
            }
            List<String> names = parseNames(stored);
            names.stream().filter(name -> !products.contains(name)).forEach(elsewhere::add);
            List<String> known = names.stream().filter(products::contains).toList();
            if (!settled && !Set.copyOf(known).equals(off.peek())) {
                restore.accept(known);
            }
        });
    }

    private void save() {
        var names = new LinkedHashSet<>(off.peek());
        names.addAll(elsewhere);
        UI ui = UI.getCurrent();
        if (ui == null) {
            // Nothing to write to outside a request of a UI; the session still holds it.
            return;
        }
        WebStorage.setItem(ui, WebStorage.Storage.LOCAL_STORAGE, OFF_KEY, String.join(",", names));
        var written = new StringBuilder();
        groups.peek().forEach((key, opened) -> {
            if (!written.isEmpty()) {
                written.append(',');
            }
            written.append(key).append('=').append(opened ? '1' : '0');
        });
        WebStorage.setItem(ui, WebStorage.Storage.LOCAL_STORAGE, GROUPS_KEY, written.toString());
    }

    static List<String> parseNames(@Nullable String stored) {
        if (stored == null || stored.isBlank()) {
            return List.of();
        }
        var names = new LinkedHashSet<String>();
        for (String name : stored.split(",")) {
            if (!name.isBlank()) {
                names.add(name.trim());
            }
        }
        return List.copyOf(names);
    }

    static Map<String, Boolean> parseGroups(@Nullable String stored) {
        var parsed = new LinkedHashMap<String, Boolean>();
        if (stored == null || stored.isBlank()) {
            return parsed;
        }
        for (String pair : stored.split(",")) {
            int equals = pair.indexOf('=');
            if (equals > 0) {
                String value = pair.substring(equals + 1).trim();
                if ("1".equals(value) || "0".equals(value)) {
                    parsed.put(pair.substring(0, equals).trim(), "1".equals(value));
                }
            }
        }
        return parsed;
    }
}

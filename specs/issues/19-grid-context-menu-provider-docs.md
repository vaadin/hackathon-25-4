Withdrawn: already fixed. vaadin/docs#6587, merged on 2026-11-22, moved the Grid page example, its text and the 25.4 upgrade guide to `setDynamicContentProvider`.

REPO: vaadin/docs
TITLE: The Grid context menu example uses setDynamicContentHandler, deprecated for removal in 25.4, and no page shows setDynamicContentProvider
---
### Description

25.4 deprecates `GridContextMenu.setDynamicContentHandler(SerializablePredicate<T>)` for removal and adds `setDynamicContentProvider(SerializablePredicate<DynamicContentContext<T>>)`, whose context carries the row's item and the clicked column. The Grid page's Context Menu section still teaches the deprecated method, and no page names the new one or `DynamicContentContext`.

### Why it matters

Code copied from the page compiles with a removal warning, and a reader who wants the menu to depend on the column, the reason the provider exists, has only the javadoc to find it in. The example is also the only place that shows how a grid menu changes per row, which is the case the provider is for.

### Expected

The Context Menu example on the Grid page uses `setDynamicContentProvider`, reading `context.item()`, with a sentence on `context.column()` and on returning false to keep the menu closed, for example on the header. A "new in 25.4" note beside it, as other 25.4 arrivals have.

### Reproduce

1. Open `components/grid` for Flow, section Context Menu, on the 25.4 documentation.
2. The example `GridContextMenuExample.java` (`src/main/java/com/vaadin/demo/component/grid/GridContextMenuExample.java` in vaadin/docs) ends with `setDynamicContentHandler(person -> { if (person == null) return false; ... })`.
3. The javadoc of that method in 25.4.0-beta2: "Deprecated. Use setDynamicContentProvider(SerializablePredicate) instead, which also receives the clicked column."
4. A search of vaadin/docs for `setDynamicContentProvider` or `DynamicContentContext` finds nothing.

Found on 25.4.0-beta2.

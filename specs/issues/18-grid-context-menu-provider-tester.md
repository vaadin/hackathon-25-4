REPO: vaadin/browserless-test
TITLE: GridContextMenuTester cannot open a menu that uses the 25.4 dynamic content provider
---
### Description

25.4 deprecates `GridContextMenu.setDynamicContentHandler` for removal and adds `setDynamicContentProvider`, which also receives the clicked column. Opening such a menu with `GridContextMenuTester` throws inside the grid:

```java
menu.setDynamicContentProvider(context -> { ...; return true; });

test(grid).contextMenu(0).open();
// java.lang.NullPointerException: Cannot invoke "tools.jackson.databind.JsonNode.asString()"
//   because the return value of "tools.jackson.databind.node.ObjectNode.get(String)" is null
//   at com.vaadin.flow.component.grid.contextmenu.GridContextMenu.onBeforeOpenMenu(GridContextMenu.java:389)
//   at com.vaadin.flow.component.grid.contextmenu.GridContextMenuTester.requestMenu(GridContextMenuTester.java:508)
```

`GridContextMenuTester.requestMenu` puts only `key` in the event detail of `vaadin-context-menu-before-open`. With a provider set, `onBeforeOpenMenu` also reads `internalColumnId`, which the browser sends, and dereferences it without a check. `open(row, columnKey)` fails the same way: it sets the column on the grid's properties, not in the event.

### Why it matters

Every grid context menu that moves off the deprecated handler, as the deprecation asks, can no longer be opened in a browserless test, with an exception that points into the grid rather than the tester. The deprecated handler still works, so the tester quietly pins code to an API marked for removal.

### Expected

`GridContextMenuTester.open` sends `internalColumnId` beside `key`: the column of `open(row, columnKey)`, and when no column is named, the first visible column, which is where a right click on the row lands most often. Separately, `GridContextMenu.onBeforeOpenMenu` could treat a missing `internalColumnId` as no column instead of throwing.

### Workaround

The test fires the before open event itself, through `ElementListenerMap`, with the row's key and a column's internal id read through `com.vaadin.browserless.internal.GridContextMenuSupport`, then drives the open menu with `test(menu)`.

### Reproduce

Two browserless tests on one view with two grids and the same menu, one built through `setDynamicContentProvider`, one through the deprecated `setDynamicContentHandler`. Each opens the menu on the first row with `test(grid).contextMenu(0).open()` and reads the item texts. On 25.4.0-beta2 with browserless-test 25.4.0-beta1 they printed:

```
setDynamicContentHandler: [Email Ada]
setDynamicContentProvider: java.lang.NullPointerException: Cannot invoke "tools.jackson.databind.JsonNode.asString()" because the return value of "tools.jackson.databind.node.ObjectNode.get(String)" is null
```

A reproduction is the second line; a fix prints `setDynamicContentProvider: [Email Ada]`.

Found on 25.4.0-beta2, browserless-test 25.4.0-beta1. `requestMenu` on the main branch of browserless-test is unchanged.

REPO: vaadin/browserless-test
COMMENT-ON: 29
---
Still so on 25.4.0-beta2 with browserless-test 25.4.0-beta1, and on 25.3.5, measured from the 25.4 hackathon, where an explorer grid opens a finding's details on a row click.

A grid with an item details renderer and the default `setDetailsVisibleOnClick(true)` opens a row's details in the browser on a click. `GridTester.clickRow(0)` fires the item click event, but the details stay closed: opening them on click is done by the web component, so nothing on the server toggles them. Once the test opens them itself with `grid.setDetailsVisible(item, true)`, a query does find the details component, so it is only the click that is missing.

### Reproduce

Two tests on one grid of two names whose details renderer is `new Span("Details of " + name)`. On 25.4.0-beta2 they printed:

```
details visible after clickRow(0): false
details visible after setDetailsVisible: true
Span "Details of Ada" found by a query: 1
```

25.3.5 prints the same. A fix prints `true` on the first line.

### Expected

`clickRow` does what the browser does with the grid's own click behaviour: toggles the row's details when `isDetailsVisibleOnClick()` is true, as `select` already follows the selection mode.

Found on 25.4.0-beta2, browserless-test 25.4.0-beta1, and the same on 25.3.5.

The project: [`20-grid-item-details-tester.zip`](https://github.com/vaadin/hackathon-25-4/raw/COMMIT_SHA/specs/issues/projects/20-grid-item-details-tester.zip), 4 KB, sources only, `mvn test`. It prints the three lines above.

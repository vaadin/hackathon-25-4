REPO: vaadin/flow-components
TITLE: GridAIController cannot narrow a grid the application already has
---
### Description

`GridAIController` takes only a `Grid<AIDataRow>`, and when a turn ends it replaces the grid's columns and data provider with ones built from the model's SQL. An application whose list is a typed grid, `Grid<Advisory>` with its own columns, renderers, context menu, order and lazy data provider, cannot let the model narrow that list: the constructor does not accept it, and a `Grid<AIDataRow>` the application configured loses its configuration at the first turn.

```java
new GridAIController(new Grid<Person>(), provider);   // does not compile

Grid<AIDataRow> grid = new Grid<>();
grid.addColumn(row -> "...").setKey("mine");
new GridAIController(grid, provider).restoreState(new GridState("SELECT name AS \"Name\" FROM people"));
// grid.getColumns(): [Name] and the application's column is gone
```

### Why it matters

The most common ask of a list screen is not "build me a table" but "show me only the ones that ...": the critical advisories still open on a line, the orders of one customer. The list already exists, with its formatting, actions and order, and the model's job is a filter. Today an application has to choose between losing its grid and not using the controller.

### Expected

In order of usefulness:

1. A filter mode: the controller drives an existing `Grid<T>` by handing the application the ids, or a predicate, that the model's query selected, and leaves columns and data provider alone. The query validation and the schema tools stay as they are.
2. A state change event that carries the rows of the settled query, so an application can narrow its own grid without running the query a second time. This touches #9205, which reconsiders `addStateChangeListener` on these controllers.

### Workaround

The controller drives a `Grid<AIDataRow>` that is never shown, over an in memory copy of the data. A state change listener runs the settled query once more, reads the id column, and narrows the real grid to those ids. The system prompt and a probe in `DatabaseProvider.executeQuery(String, QueryPage)` require the id as the first column.

### Reproduce

The two snippets above, as a test that also prints the columns of a typed grid for comparison:

| Line | Printed |
| --- | --- |
| typed grid columns | `[name/Name, team/Team]` |
| before | `[mine/Mine]` |
| after | `[Name/Name, Team/Team]` |

The typed grid cannot be passed at all; the `Grid<AIDataRow>` loses the column the application gave it.

Found on 25.4.0-beta2. The same constructor is on 25.3.5.

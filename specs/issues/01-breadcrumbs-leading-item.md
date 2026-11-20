REPO: vaadin/flow-components
TITLE: Breadcrumbs in ROUTER mode cannot take a leading item that is not a route
---
### Description

A trail usually starts with something that is not a view of the application: the company site, the product, a portal. In `ROUTER` mode there is nowhere to put it:

```java
var breadcrumbs = new Breadcrumbs();
breadcrumbs.addComponentAsFirst(new BreadcrumbsItem("Example Corp", "https://example.com"));
```

```
IllegalStateException: Cannot modify breadcrumb items manually in Mode.ROUTER. Switch to Mode.MANUAL to manage items directly.
```

### Why it matters

One fixed item forces `MANUAL` mode, and `MANUAL` mode means rewriting everything `ROUTER` mode does: listening to navigation, walking `RouteConfiguration.getRouteHierarchy`, resolving each title with `Router.resolvePageTitle`, linking the ancestors and leaving the leaf without a path. Ours is about thirty lines that exist only to add one link.

### Expected

Any one of these:

1. Prefix and suffix items that `ROUTER` mode keeps around the trail it resolves, for example `breadcrumbs.setPrefixItems(...)`.
2. A hook that receives the resolved items before they are rendered.
3. A public method that returns the resolved trail as items, so a `MANUAL` trail is one call plus the extra item.

### Workaround

`MANUAL` mode, rebuilt in a `Signal.effect` over `UI.routerStateSignal()`.

### Reproduce

1. Run the project and open `http://localhost:8101/details`.
2. The trail reads `Orders > Order details`, resolved from `@RouteParent` and `@PageTitle`.
3. Under it the page prints the outcome of `addComponentAsFirst` on that same component: the exception above.

Both views call one method, `OrdersView.trailWithBrandLink()`, which is the whole reproduction.

### Getting the project

[`01-breadcrumbs-root.zip`](https://github.com/vaadin/hackathon-25-4/raw/COMMIT_SHA/specs/issues/projects/01-breadcrumbs-root.zip), 3 KB, sources only. Unzip it and:

```
mvn spring-boot:run
```

Found on 25.4.0-beta2.

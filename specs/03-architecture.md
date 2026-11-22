# 03 Architecture

## Package layout

```
com.vaadin.securitystatus/
  Application.java
  base/          the shell, breadcrumbs, theme, signals helpers, error handling, i18n
  data/          PagesClient, SecurityDataService, DataRefreshSignal, read models, VersionNumber
  status/        the customer screens: home, lookup, version, cve, scan, advisories, cases
  explorer/      the internal front door, release and spring pages
  workbench/     the verdict queue, the draft form, the YAML export
  assistant/     the AI surfaces and their tools
  diagnostics/   the event bus listeners and the diagnostics view
```

A feature package owns its views, its view models and its tests. `base` and `data` are the only packages a feature may depend on, and features never depend on each other: the assistant asks `data`, not `status`. Every package has a `package-info.java` with `@NonNullApi`.

## Views and routing

Flow views, Java only. The route tree mirrors the questions, not the implementation:

| Route | View | Access |
| --- | --- | --- |
| `/` | HomeView | public |
| `/version/{version}` | VersionView | public |
| `/cve/{ids}` and `/cve/{ids}/{version}` | CveView, ids comma separated | public |
| `/scan` and `/scan/{version}` | ScanView | public |
| `/advisories` | AdvisoriesView | public |
| `/cases` and `/cases/{id}` | CasesView | public, unlisted |
| `/login` | LoginView | public |
| `/findings` | ExplorerView | `SECURITY` |
| `/findings/release/{line}/{version}` | ReleaseView | `SECURITY` |
| `/findings/spring` | SpringView | `SECURITY` |
| `/findings/workbench` | WorkbenchView | `SECURITY` |
| `/diagnostics` | DiagnosticsView | `SECURITY` |

The customer routes hang under one `@RouteParent` layout that carries the app bar with Breadcrumbs and the measured chip, then one centered column opening every page with the H1, the purpose and the lookup above the route's content, the prototype's distribution, D-164 and D-169; the internal routes under a second parent, `InternalLayout`, whose bar holds the drawer toggle, the page's H1, the internal destinations as links, the measured chip, the dark switch, the Assistant button and Sign out, whose left panel holds only what the route gives it through `DrawerContent`, the filter and products of the explorer and of the release page, D-216, and nothing for the Spring Boot page, the workbench and diagnostics, and whose right side holds the assistant panel, shut on arrival, D-203. Two parents because the two halves are two whole answers, which is rule 6.10.2 of the source spec: neither links into the middle of the other.

### Query flags

The source pages encode every open section in the address so an answer can be shared mid conversation, and that carries over as query parameters: `?boot`, `?ruled`, `?disc` and `?open=<cve>` on the version screen, `?secure` for the scan screen's show secure findings switch, `?verdict=<applicability>` on advisories, `?pre` on home, and `?q`, `?open=<line>` plus `?off=<name>,<name>` on the explorer, where no `?off` is the default selection and a bare `?off` is everything on, D-204, D-206. The source page's `?marks` flag is not carried over: it toggled marks about parts of the Figma design the static page could not build, a concern this rebuild does not have. One navigation helper in `base` writes them, because the source project learned the hard way that a control building its own address drops half the question: rule 6.11.13, a control that narrows a screen cannot take the question with it.

Scroll discipline carries over too: the viewport goes to the top when the question changes, screen or version or ids, and stays put when only a query flag changes. Opening a section must not scroll away from the control that opened it.

## Signals

The 25.3 rule continues: cross component state is a signal.

- `DataRefreshSignal`, shared, application wide: the newest `generated_at`. Bound by the measured chip, the home cards and the explorer badges.
- `LookupState`, `@VaadinSessionScope`: the typed version, the pasted ids, the active tab, the parsed report summary. The lookup keeps state across tab switches and navigation, which is a stated behaviour of the source page.
- `ProductSelection`, `@VaadinSessionScope`, explorer only: which product codes are off, persisted as a cookie equivalent through the typed `WebStorage` accessors so it survives between visits, and always written into the address when it narrows, per rule 6.7.5.
- Derived values with `Signal.computed`, lists through `bindChildren`, grids through `Signal.effect`.

## The lookup, once

One component, three tabs, version, advisory ids, report, mounted in the customer parent layout, in its column under the H1 and the purpose, and feeding `LookupState`. Submitting navigates to the route that answers; clearing a field or Escape withdraws the question and navigates home. The input cleanup carries over: `V24.9.20`, `Vaadin 24.9.20` and `24.9.20` are the same question, and a CVE pasted into the version field jumps to the advisory tab. The slash key focuses the lookup from anywhere, through a shortcut with a JS filter so it does not fire while typing in another field.

## Grids and tables

Advisory lists are `Grid` with lazy data providers over the cached JSON. Static shaped facts, the Spring Boot dependency table, the coordinate rows inside a finding, are the `Table` family, because they are documents rather than data sets: no sort, no virtualisation, semantic header cells for a screen reader.

## Downloads

Everything leaving the application goes through the 25.4 `Download` API: the triage CSV composed in memory, the audit PDF, and the proxied per release files. No `StreamResource`, no anchor wired by hand; one helper in `base` so content type, filename and disposition are decided in one place.

## Dialogs

The source design's rule 6.11.3 carries over as a component: `HelpDialog`, opened from a question mark control beside the label it explains, closed by its own button, the backdrop and Escape. Everything explanatory lives in one of these, never inline under a designed control.

## Error handling

A route whose parameter names something unmeasured renders the answer the source spec demands, in place: "we have not measured X", the nearest measured release offered, nothing redirected. A data source outage renders the one stale or unreachable sentence from `02-data-source.md`. There is no custom error view beyond Flow's not found for genuinely malformed addresses.

## i18n

One bundle, English. Every user visible string in Java goes through it, enforced by `NoHardcodedStringsTest`. One language is a recorded cut from the 25.3 method: the discipline is kept, the second bundle is not, and adding one back is mechanical.

Where a feature document says the settled wording, the words are transcribed into the bundle from the two named inputs during that epic: `docs/requirements.md` section 6 of vaadin/cve-status, which quotes wording wherever the wording was the decision, and the live pages. The suite does not copy those strings in, on purpose: they are already specified once, in a document this repository cannot drift from, and a second copy here would be the two place drift the source project kept getting burned by. The bundle is the record of what was transcribed.

## What we are not doing

No Hilla endpoints and no React: one UI technology. No client side templates. No websocket push beyond what signals use. No service worker or PWA: the readers of this page arrive from a ticket, use it and leave. No database reads on the render path: H2 exists for the workbench draft alone.

# 03 Architecture

## Package layout

| Package | Holds |
| --- | --- |
| root | `Application`, the application shell: Aura, `styles.css`, the favicon `icons/favicon.svg` |
| `data` | `PagesClient`, `PagesCache`, `SecurityDataService`, `DataRefreshSignal`, `DataConfiguration`, `VersionNumber`, `Severity`, `ExportKind`, `ExportFile`, and the read models in `data/model` |
| `security` | `SecurityConfig`, `CasSecurity`, `ProfileGuard`, `TeamRule`, `SignInFailures`, `Roles`, `SessionExpiredNotice`, and the CSP: `CspPolicy`, `CspOnBootstrapPage`, `CspReportController`, `CspViolations` |
| `customer` | The customer's rules, no components: `LookupState`, `ScanState`, `LookupInput`, `VersionRules`, `CveRules`, `AdvisoryRules`, `CaseRules`, `VerdictFor`, `ReportParser`, `ReportFormat`, `ScannerReport`, `Triage`, `TriageFiles` |
| `team/explorer`, `team/workbench`, `team/assistant`, `team/diagnostics` | The team's rules, no components: the explorer, release and Spring Boot rules; the verdict queue rules, the drafts and their repository, the YAML export; the AI providers, prompts and tools; the event bus listeners and counters |
| `ui/common` | What both UIs draw: `AppBar`, `LoginView`, `ThemeSwitch`, `ThemeMemory`, `ViewHeading`, `HelpDialog`, `Downloads`, `DataOutageNotice`, `AdvisoryIds`, `Parts` (the small builders every view shares), `TranslatedPageTitleGenerator`, the extension points `TeamEntrance` and `AiSurfaces`, and `nav` with `Address` and `QueryFlag` |
| `ui/customer` | `CustomerLayout`, the customer views (home, version, CVE, scan, advisories, cases), `Lookup` with `LookupFactory` and `LookupProvider`, `FindingCard`, `AnswerParts` (the parts every answer is built from), `Mark`, `VersionAddress`, `PdfPreview`, `NotFoundView` |
| `ui/team` | `InternalLayout`, `TeamNavigation`, `DrawerContent`, `TeamRoutes`, `TeamEntranceProvider`, and per area: `explorer` (the explorer, release and Spring Boot views, with `SeriesParts`, `FindingList`, `FindingDetail`, `ProductSelection`, `ReleaseFiles`), `workbench`, `assistant`, `diagnostics` |

Two UIs, D-254. The customer UI is `customer` plus `ui/customer`; the team UI is `team` plus `ui/team`, and it reads the customer views as one more page of its own. Every component, view and layout lives under `ui`; the packages outside it hold no components. Nothing outside `team` and `ui/team` imports them: the customer shell finds the team through the `TeamEntrance` interface in `ui/common`, which `ui/team` implements as a Spring bean, the team registers its own aliases of customer views (`findings/advisories`, `findings/cve`) in `TeamRoutes`, and the AI surfaces are reached through `AiSurfaces`. The team's CSS sits in `styles/team/`. To ship the customer UI alone, delete `team/`, `ui/team/`, `styles/team/` with its imports in `styles.css`, the team's tests, and its specifications, features and epics 08 to 12 and the rows of `01-foundation.md` and of US-1.2 in its epic that `ShellSecurityBrowserlessTest` verifies; `TeamIsRemovableTest` fails the build when a source or a test outside `team` and `ui/team` names a team class, when a route, `Composite` or `AppLayout` lives outside `ui`, or when a stylesheet outside `styles/team` styles a team class prefix (`internal-`, `customer-team`, `assistant-`, `explorer-`, `workbench-`, `diagnostics-`). Every package that holds classes, except the root, has a `package-info.java` marked JSpecify `@NullMarked`.

## Views and routing

Flow views, Java only. The route tree mirrors the questions, not the implementation:

| Route | View | Access |
| --- | --- | --- |
| `/` | HomeView | signed in, D-258 |
| `/version/{version}` | VersionView | signed in, D-258 |
| `/cve/{ids}` and `/cve/{ids}/{version}` | CveView, ids comma separated | signed in, D-258 |
| `/scan` and `/scan/{version}` | ScanView | signed in, D-258 |
| `/advisories` | AdvisoriesView | signed in, D-258 |
| `/cases` and `/cases/{id}` | CasesView | signed in, unlisted |
| `/login` | LoginView, drawn alone outside every shell, D-276 | public |
| `/findings` | ExplorerView | `SECURITY` |
| `/findings/release/{line}/{version}` | ReleaseView | `SECURITY` |
| `/findings/spring` | SpringView | `SECURITY` |
| `/findings/workbench` | WorkbenchView | `SECURITY` |
| `/findings/advisories` | AdvisoriesView inside the internal shell, registered by `TeamRoutes`, D-237, D-254 | `SECURITY` |
| `/findings/cve/{ids}` and `/findings/cve/{ids}/{version}` | CveView inside the internal shell, registered by `TeamRoutes`, D-246 | `SECURITY` |
| `/diagnostics` | DiagnosticsView | `SECURITY` |

Three frames. The customer routes are drawn in `CustomerLayout`, the automatic `@Layout`: the app bar, then one centered column opening every page with the H1, the purpose and the lookup above the route's content, the prototype's distribution, D-164, D-169. `@RouteParent(HomeView)` on each customer view gives the breadcrumb trail its hierarchy only, never the rendering. Signed in with the team role, the customer column has the team's side navigation beside it, hidden below 800 pixels, D-253. The internal routes are drawn in `InternalLayout`, an App Layout. Its left panel holds, from the top, the brand with the app icon, the team's side navigation (Findings with Spring Boot under it, Customer view, Verdicts, Advisories, Diagnostics, D-245, D-252), and under them, as a card, what the route gives through `DrawerContent`: the filter and products of the explorer and of the release page, D-216, D-251. Its bar holds the drawer toggle, the page's H1, the trail, the measured chip, the scheme button, the Assistant button and Sign out. Its right side holds the assistant panel, shut on arrival, D-203, D-237. The sign in page is drawn in neither, D-276. The customer and internal shells are two because the two halves are two whole answers, rule 6.10.2 of the source spec: neither links into the middle of the other.

### Query flags

The source pages encode every open section in the address so an answer can be shared mid conversation, and that carries over as query parameters: `?boot`, `?ruled`, `?disc`, `?pre` and `?open=<cve>` on the version screen, `?secure` for the scan screen's show secure findings switch, `?verdict=<applicability>` on advisories, `?pre` on home, `?q`, `?open=<line>` plus `?off=<name>,<name>` on the explorer, and `?series=<line>` on the workbench, where no `?off` is the default selection and a bare `?off` is everything on, D-204, D-206. The source page's `?marks` flag is not carried over: it toggled marks about parts of the Figma design the static page could not build, a concern this rebuild does not have. One writer in `ui/common/nav`, `Address` over the `QueryFlag` enum, writes them all, D-93, because the source project learned the hard way that a control building its own address drops half the question: rule 6.11.13, a control that narrows a screen cannot take the question with it.

Scroll discipline carries over too: the viewport goes to the top when the question changes, screen or version or ids, and stays put when only a query flag changes. Opening a section must not scroll away from the control that opened it.

## Signals

The 25.3 rule continues: cross component state is a signal.

- `DataRefreshSignal`, shared, application wide: the newest `generated_at`. Bound by the measured chip, the home cards and the explorer badges.
- `LookupState`, `@VaadinSessionScope`: the active tab, one version for every tab, D-274, and the pasted ids. `ScanState`, also session scoped, holds the read report and the last refusal. The lookup keeps state across tab switches and navigation, which is a stated behaviour of the source page.
- `ProductSelection`, `@VaadinSessionScope`, explorer and release page: which products are off, by name, and which groups were opened by hand, kept through the typed `WebStorage` accessors under `security-status.explorer.off` and `security-status.explorer.groups`, an empty stored value meaning everything on, and always written into the address when it narrows, rule 6.7.5, D-206, D-214.
- Derived values with `Signal.computed`, lists through `bindChildren`, grids through `Signal.effect`.

## The lookup, once

One component, three tabs, version, advisory ids, report, mounted in the customer parent layout, in its column under the H1 and the purpose, and feeding `LookupState`. Submitting navigates to the route that answers; clearing a field or Escape withdraws the question and navigates home. The input cleanup carries over: `V24.9.20`, `Vaadin 24.9.20` and `24.9.20` are the same question, and a CVE pasted into the version field jumps to the advisory tab. Enter in any field asks the question, as Check does, D-240. The slash key focuses the lookup from anywhere but a field, through a `keydown` listener with a declared JavaScript filter rather than a shortcut registration, because the shortcut filter is internal, draft 12b.

## Grids and tables

The advisory catalogue is a `Grid` with a lazy data provider over the cached JSON. The explorer's findings are one `Details` per finding, D-264. Static shaped facts, the Spring Boot dependency table, the coordinate rows inside a finding, are the `Table` family, because they are documents rather than data sets: no sort, no virtualisation, semantic header cells for a screen reader.

## Downloads

Everything leaving the application goes through the 25.4 `Download` API: the triage CSV composed in memory, the audit PDF, and the proxied per release files. No `StreamResource`, no anchor wired by hand; one helper, `ui/common/Downloads`, so content type, filename and disposition are decided in one place.

## Dialogs

The source design's rule 6.11.3 carries over as a component: `HelpDialog`, opened from a question mark control beside the label it explains, closed by its own button, the backdrop and Escape. Everything explanatory lives in one of these, never inline under a designed control.

## Error handling

A route whose parameter names something unmeasured renders the answer the source spec demands, in place: "we have not measured X", the nearest measured release offered, nothing redirected. A data source outage renders the one stale or unreachable sentence from `02-data-source.md`. `NotFoundView`, Flow's not found view kept inside the customer shell, sends an address that names a registered route the reader may not open to the home page, and shows not found for any other address, `04-security.md`. A data outage shows `DataOutageNotice`, one sentence in place of the answer.

## i18n

One bundle, English. Every user visible string in Java goes through it, enforced by `NoHardcodedStringsTest`. One language is a recorded cut from the 25.3 method: the discipline is kept, the second bundle is not, and adding one back is mechanical.

Where a feature document says the settled wording, the words are transcribed into the bundle from the two named inputs during that epic: `docs/requirements.md` section 6 of vaadin/cve-status, which quotes wording wherever the wording was the decision, and the live pages. The suite does not copy those strings in, on purpose: they are already specified once, in a document this repository cannot drift from, and a second copy here would be the two place drift the source project kept getting burned by. The bundle is the record of what was transcribed.

## What we are not doing

No Hilla and no React: one UI technology, and the pom carries no Hilla starter. No client side templates. No websocket push beyond what signals use. No service worker or PWA: the readers of this page arrive from a ticket, use it and leave. No database reads on the render path: H2 exists for the workbench draft alone.

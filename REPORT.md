# Build report

How this application was built on Vaadin 25.4.0-beta2: what exists, what 25.4 did as advertised, what it got in the way of, how the rebuild compares with the two JavaScript apps it replaces, and what is not finished. The numbers are the ones the gates and the files on disk give at the end of epic 13; nothing here is rounded.

The specifications were written on 20 November 2026 and the thirteen epics ran unattended, one session each, from 20 November to 22 November. Every choice a person would have been asked about is a row in `DECISIONS.md`, D-01 to D-161, and everything only a person can do is a row in `NEEDS-A-PERSON.md`, P-01 to P-19.

## Timeline

Commit times, local, from `git log`. An epic is two commits: the work, and its record.

| Epic | Committed | Result |
| --- | --- | --- |
| Specifications | 20 Nov 13:28 | 13 cross cutting documents, 13 feature documents, 13 epics |
| 01 Foundation and shell | 20 Nov 16:02 | Two shells, sign in, roles, our own enforced policy, theme memory |
| 02 Data client | 21 Nov 02:30 | The Pages client, the cache, the shared refresh signal, read models |
| 03 Home and lookup | 21 Nov 03:33 | Line cards, the three tab lookup |
| 04 Answer by version | 21 Nov 09:30 | The verdict card and everything under it |
| 05 Answer by CVE | 21 Nov 10:30 | One answer per id, copy answer |
| 06 Scanner report triage | 21 Nov 11:12 | Four report formats, CSV and PDF, the PDF preview |
| 07 Advisories and cases | 21 Nov 12:55 | The catalogue grid, the dynamic context menu, `/cases` |
| 08 Explorer front door | 22 Nov 00:09 | Master detail explorer, product chips, the five groups |
| 09 Release and Spring Boot | 22 Nov 00:34 | One release in full, the Spring Boot page |
| 10 Verdict workbench | 22 Nov 01:21 | Queue, verdict form, YAML, the handover into GitHub, drafts in H2; 274 tests |
| 11 Assistant | 22 Nov 02:40 | Chat, tools, Grid AI, Form AI, cassettes |
| 12 Diagnostics | 22 Nov 03:26 | Navigation, request and session counts live; 308 tests |
| 13 Tests, sweep and demo polish | 22 Nov, this record | The sweeps, the tables reconciled, this report and `DEMO.md` |

## What exists

Eleven routes over one data service. The customer screens: home with the lookup, the answer by version, the answer by CVE, the scanner triage, the advisories catalogue and the case catalogue. The team's screens, behind the `SECURITY` role: the explorer, one release, the Spring Boot page, the verdict workbench and diagnostics, with the assistant in the drawer of every one. Everything read comes from the published JSON of vaadin/cve-status through one cache; the only thing written is a verdict draft, to an H2 file, and a verdict leaves only through GitHub's own new file editor.

115 Java sources under `src/main/java`, 53 under `src/test/java`. The gates, both run with `OPENAI_API_KEY` and `OPENAI_TOKEN` removed from the environment, D-132:

| Gate | Command | Result |
| --- | --- | --- |
| Default | `env -u OPENAI_API_KEY -u OPENAI_TOKEN ./mvnw clean verify` | Green: 324 tests |
| Browser | `env -u OPENAI_API_KEY -u OPENAI_TOKEN ./mvnw clean verify -Pit` | Green: 324 tests, then 21 browser tests against the production jar |

The default gate needs no licence only because the default build runs with `commercialWithBanner`, D-80; without it the gate was proven red on a machine with no Vaadin key, P-12.

## What epic 13 changed

- The rule 4 sweep, `AbsenceSweepBrowserlessTest`: for each of eleven screens one fixture or constructed record with absent fields is opened and the whole page walked, and every blank cell is accounted for by row and column. Its first run found real empty elements: the advisories grid drew an empty `Span` for an absent severity, owner or verdict, the explorer grid for an absent severity or route, and three screens used a `Span` holding a space between a sentence and its link. Those now draw nothing, D-159. A test of our own also proved order dependent on the first `-Pit` run, because a document another test class had read stayed cached; the sweep now forgets the cache before each constructed record.
- The literal sweep lost two exemptions that hid nothing, D-158.
- The 25.4 browserless additions where earlier epics left gaps: the upload testers' size gate and the drop zone, in `ScanViewBrowserlessTest`, and the chat panel tested on its own with the JUnit 6 `BrowserlessExtension`, a scripted provider, a failed turn and a streamed answer, `AssistantPanelBrowserlessTest`.
- The `it` and `production` profiles always build a fresh production bundle, D-156. The `-Pit` log of epic 12 said `A production mode bundle build is not needed`; the `-Pit` log of epic 13 says `Checking if a production mode bundle build is needed`, then `Frontend build requested.`, and Vite builds the bundle.
- The theme switch reads the system preference through the 25.4 typed `JavaScriptResult<Boolean>`, D-157.
- US-8.1's overlay below 800 pixels was measured in a browser and ticked, D-160; two ticks the code does not bear out were withdrawn, D-161.
- Every feature document has a Still open section written from the code, and the overview's coverage table has its own.

## What worked exactly as advertised

- Signals across the whole application: `ValueSignal`, `SharedValueSignal` for state every session sees, `Signal.computed`, `bindText`, `bindVisible`, `bindChildren`, and effects bound to a component's life.
- The modular upload, `UploadManager` with `UploadButton` and `UploadDropZone`, a custom `UploadHandler` that parses the request stream, and its browserless testers, which apply the browser's own size and count gate before anything reaches the handler.
- The `Download` API from a button click, and the PDF Viewer behind its feature flag.
- `GridContextMenu.setDynamicContentProvider`, which builds the menu per row and carries the clicked column.
- FormLayout side labels with `LabelTextAlign`, `BeanValidationBinder` with validation groups, and `NativeForm` with a submit `NativeButton` for a GET into another site.
- `NavigationStartedEvent`, `NavigationEndedEvent` with its sealed outcomes, `RequestEndedEvent`, and the typed accessors of the extended client details.
- Declared JavaScript, `@JsDefinition` with `@JsExpression` through `executeJs(Class)` and `JsSnippets`, which logs no call site under the `warn` policy mode, and the typed `JavaScriptResult`.
- The AI orchestrator with a controller's tools, the tool call limit, the Form AI controller and `UploadCapture.ENVIRONMENT`; and browserless testing of an AI view with a scripted provider, exactly as its page shows.

## Problems found in 25.4.0-beta2

Every one is a row in `specs/FEEDBACK-25.4.md` or `specs/FEEDBACK-PLATFORM.md`, with the workaround and the file it lives in, and went through `specs/issues/`: a duplicate search, a draft, and a reproducer built and run outside this repository. 18 drafts are ready: 8 for vaadin/flow, 4 for vaadin/flow-components, 3 for vaadin/browserless-test and 3 for vaadin/docs; 5 of them are comments on existing issues rather than new ones. Nothing has been posted.

| Draft | Repository | Finding |
| --- | --- | --- |
| 01 | flow-components | Breadcrumbs in `ROUTER` mode cannot take a leading item that is not a route |
| 03b | flow, comment on #25576 | A stylesheet not named `styles.css` is refused by a secured application, declared or imported, and the same default hides the not found view |
| 07b | flow, comment on #26391 | No server side way to apply a remembered color scheme before first paint, measured |
| 08 | docs | The policy page does not say how `vaadin.csp=strict` behaves: no header in development, the bootstrap page only |
| 10 | flow | The bootstrap page is served without the headers Spring Security writes |
| 11 | flow | A shared signal changed by another thread before a component is attached never reaches its bindings |
| 12b | flow, comment on #5473 | A shortcut cannot be kept quiet while the reader types; the 25.4 `ShortcutFilterJs` is internal |
| 13 | flow-components | `bindEnabled` is refused beside disable on click, also in the new `UNTIL_RESPONSE` mode |
| 14 | flow | `QueryParameters` writes its parameters back in hash order |
| 15 | flow | The Clipboard API's error callback takes a type from an internal package |
| 16 | browserless-test | A file served by `Download.onClick` cannot be fetched from a test |
| 17 | docs | A component built by an injected bean is left out of the production bundle, hidden by the pre-compiled bundle |
| 18 | browserless-test | `GridContextMenuTester` cannot open a menu that uses the 25.4 dynamic content provider |
| 19 | docs | The Grid context menu example uses the handler deprecated for removal in 25.4 |
| 20b | browserless-test, comment on #29 | `GridTester.clickRow` does not open a row's item details |
| 21 | flow-components | `GridAIController` cannot narrow a grid the application already has |
| 22 | flow-components | A failed AI turn writes a fixed English sentence, and the limit exception has no accessors |
| 23b | flow, comment on #26338 | A route added after the production bundle was built ships without its layout's components while the build says no build is needed |

Several of these also reproduce on 25.3 or earlier and live in `FEEDBACK-PLATFORM.md`. Three rows died when their reproducer or the documentation was read, drafts 02, 04 and 06, and four drafts were corrected by their own reproduction; the ledger in `specs/issues/README.md` keeps them. Rows that need no draft say why there: the record of loosened policy directives, the `form-action` note that belongs on the `NativeForm` page vaadin/docs#6625 is writing, and the four data needs toward vaadin/cve-status. One row is not ready: the starter's `architecture.md`, which needs a wizard generated project to check against.

Epic 13 found no new platform finding. Its sweep found faults in our own code, above.

## The 25.4 coverage, honestly

The overview claims typed JavaScript, `JsExpression`, `JsSnippet`, `callJsFunction`, return values and `addJsInitializer`, for epics 04 and 05. What the code uses is declared snippets with `@JsExpression`, through `executeJs(Class)` and `JsSnippets.of`, and one typed return value, the theme switch's `JavaScriptResult<Boolean>`, D-157. `callJsFunction` and `addJsInitializer` are used nowhere. The copy answer control of epic 05 uses the platform's Clipboard API, which does the job without a hand written call, D-63, and no piece of existing code has a client side lifecycle an initializer would fit; this epic adds no behaviour, so none was invented.

The other gaps against the coverage table: `ShortcutFilterJs` is internal, so the slash key is a filtered `keydown` listener, draft 12b; `CspMode` and the strict mode are not used, our own policy is, D-02; the JUnit 6 extensions serve the one test with no Spring context, since the documentation says they do not replace `SpringBrowserlessTest`; `QueryPage` pages no tool result, D-143; `RequestStartedEvent` is not listened to. The arrivals no epic claims are listed in `specs/00-overview.md` with a decision each.

## Compared with the JavaScript originals

The originals are two static pages on GitHub Pages, the customer status page and the findings explorer, which compute everything in the reader's browser from the same JSON and keep the team's screens behind a client side gate. The rebuild reads the same JSON on the server through one cache and answers the same questions, with these differences, each recorded where it was decided:

- A real sign in with a role replaces the client side gate; the data stays public, as it is on Pages.
- Every screen has a real address with its question in it, such as `/version/24.9.21?boot` or `/advisories?verdict=risk`, instead of a hash; the hash addresses are not kept, because the JavaScript pages stay published.
- A scanner report is read on the server and not kept; the triage files are produced there, CSV and PDF.
- The workbench keeps verdict drafts in a server side store shared by the team, and hands the YAML to GitHub's editor as the original does.
- New: the assistant, internal only, which answers from tool calls over the same data; diagnostics; the case catalogue as a route.
- Left out: the self contained email file, the RSS feeds, the redirect stubs, and a second language, with the bundle discipline kept, `specs/00-overview.md`.
- Where the data does not carry a fact, the page omits it, which holds the rebuild to the original's own rule. Two things the original's specification asks for cannot be built from the published data at all: ordering advisories by age, D-81, and an impact column, D-82. Both are data needs toward vaadin/cve-status.

Each feature's review in `specs/reviews/` compares the screen with the live page on the same question, as far as the live pages' sign in gate allowed: the team's screens could not be compared past it, P-03. Eight reviews pass what their feature document asks; the foundation, the data client, the explorer front door and the workbench fail as whole screens against the reference for reasons each review names, and the findings that are matters of design wait for a person, P-02 to P-19.

## What is not done

Eight checkboxes stay unticked across the specifications, each with its reason in the feature's Still open section:

| Where | Checkbox | Why |
| --- | --- | --- |
| Epic 03, US-3.1 | ComboBox with `PartialMatchMode` | Refused beside custom values on beta2, D-40 |
| Epic 05, US-5.2 | Success read back through the typed JS call | The Clipboard API does it, D-63 |
| Epic 07, US-7.1 | Grid in the source order | No published date to order by age, D-81, D-161 |
| Epic 07, Definition of Done | Every acceptance criterion checked | AC1 below |
| Feature 07, AC1 | Order is severity, then age, then id | No date in the data, D-92 |
| Epic 11, US-11.2 | The five tools, `ToolException` refusals, `QueryPage` paging | `QueryPage` pages no tool result, D-143 |
| Epic 12, US-12.1 | Service init listener registering CSP and interception listeners | They reach the view through signals on their own classes, D-151 |
| Epic 12, US-12.1 | The signal singleton with counters and ring tails | The rings are not in the singleton, D-151, D-161 |

Not verified unattended, and waiting for a person in `NEEDS-A-PERSON.md`: the interactive polish pass, P-01; the reference pages past their sign in gate, P-03; the development mode without a licence, P-12; a real submit into GitHub's editor, P-17; the camera on a phone and the live model's wording, P-18; and the wording written without a source on every screen. The demo walk of `DEMO.md` was made on the fixtures in development mode; the production mode is exercised only by the `-Pit` browser tests.

## The posting script, dry run

`specs/issues/post.sh` without `--confirm`, run on the clean tree after the two commits of epic 13. It sends nothing. The SHA it prints is the record commit before this output was added to it, so it differs from the final one; the warning about the remote is expected, since nothing is pushed.

```
HEAD is 31f7dbecb3b9248213848b25538ec162700fde30
origin is git@github.com:vaadin/hackathon-25-4.git

! HEAD is on no remote branch. Push first, or every project link will 404.

WOULD POST to vaadin/flow-components: Hackathon 25.4: Breadcrumbs in ROUTER mode cannot take a leading item that is not a route  (from 01-breadcrumbs-leading-item.md, 1916 bytes)
    and add it to platform#9642 under "## Hackathon"
WOULD COMMENT on vaadin/flow#25576  (from 03b-comment-on-flow-25576.md, 2290 bytes)
WOULD COMMENT on vaadin/flow#26391  (from 07b-comment-on-flow-26391.md, 1627 bytes)
WOULD POST to vaadin/docs: Hackathon 25.4: The Content Security Policy page does not say how the strict csp mode behaves  (from 08-csp-page-strict-mode.md, 2552 bytes)
    and add it to platform#9642 under "## Hackathon"
WOULD POST to vaadin/flow: Hackathon 25.4: The bootstrap page is served without the headers Spring Security writes  (from 10-bootstrap-page-without-security-headers.md, 2469 bytes)
    and add it to platform#9642 under "## Hackathon"
WOULD POST to vaadin/flow: Hackathon 25.4: A shared signal changed by another thread before a component is attached never reaches its bindings  (from 11-shared-signal-changed-before-attach.md, 3476 bytes)
    and add it to platform#9642 under "## Hackathon"
WOULD COMMENT on vaadin/flow#5473  (from 12b-comment-on-flow-5473.md, 2334 bytes)
WOULD POST to vaadin/flow-components: Hackathon 25.4: Button refuses bindEnabled beside disable on click, also in the new UNTIL_RESPONSE mode  (from 13-bind-enabled-until-response.md, 2481 bytes)
    and add it to platform#9642 under "## Hackathon"
WOULD POST to vaadin/flow: Hackathon 25.4: QueryParameters does not keep the order of the parameters, so an address comes back reordered  (from 14-query-parameters-order.md, 2345 bytes)
    and add it to platform#9642 under "## Hackathon"
WOULD POST to vaadin/flow: Hackathon 25.4: The error callback of the Clipboard API takes a type from an internal package  (from 15-clipboard-error-internal.md, 2349 bytes)
    and add it to platform#9642 under "## Hackathon"
WOULD POST to vaadin/browserless-test: Hackathon 25.4: A file served by Download.onClick cannot be fetched from a test, as an Anchor's can  (from 16-download-onclick-testing.md, 2536 bytes)
    and add it to platform#9642 under "## Hackathon"
WOULD POST to vaadin/docs: Hackathon 25.4: The @Uses note on the production build page covers reflection only, and an injected component is left out the same way  (from 17-injected-component-bundle.md, 3573 bytes)
    and add it to platform#9642 under "## Hackathon"
WOULD POST to vaadin/browserless-test: Hackathon 25.4: GridContextMenuTester cannot open a menu that uses the 25.4 dynamic content provider  (from 18-grid-context-menu-provider-tester.md, 3196 bytes)
    and add it to platform#9642 under "## Hackathon"
WOULD POST to vaadin/docs: Hackathon 25.4: The Grid context menu example uses setDynamicContentHandler, deprecated for removal in 25.4, and no page shows setDynamicContentProvider  (from 19-grid-context-menu-provider-docs.md, 1625 bytes)
    and add it to platform#9642 under "## Hackathon"
WOULD COMMENT on vaadin/browserless-test#29  (from 20b-comment-on-browserless-test-29.md, 1614 bytes)
WOULD POST to vaadin/flow-components: Hackathon 25.4: GridAIController cannot narrow a grid the application already has  (from 21-grid-ai-typed-grid.md, 2783 bytes)
    and add it to platform#9642 under "## Hackathon"
WOULD POST to vaadin/flow-components: Hackathon 25.4: A failed AI turn writes a fixed English sentence the application cannot replace  (from 22-ai-failure-text.md, 2422 bytes)
    and add it to platform#9642 under "## Hackathon"
WOULD COMMENT on vaadin/flow#26338  (from 23b-comment-on-flow-26338.md, 3276 bytes)

Dry run. Nothing was sent. Re-run with --confirm to post.
```

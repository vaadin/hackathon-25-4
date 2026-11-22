# Build report

How this application was built on Vaadin 25.4.0-beta2: what exists, what 25.4 did as advertised, what it got in the way of, how the rebuild compares with the two JavaScript apps it replaces, and what is not finished. The numbers are the ones the gates and the files on disk give at the close of the alignment with the prototype, after epic 13; nothing here is rounded.

The specifications were written on 20 November 2026 and the thirteen epics ran unattended, one session each, from 20 November to 22 November. On 22 November six more sessions aligned every screen with the live prototype past its gate, and a seventh closed the pass. Every choice a person would have been asked about is a row in `DECISIONS.md`, D-01 to D-228, and everything only a person can do is a row in `NEEDS-A-PERSON.md`, P-01 to P-20.

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
| 13 Tests, sweep and demo polish | 22 Nov 04:12 | The sweeps, the tables reconciled, this report and `DEMO.md`; 324 tests |
| Prototype: customer shell, home, lookup | 22 Nov 14:14 | The prototype's H1, purpose and centered column; D-162 to D-173 |
| Prototype: answer by version | 22 Nov 14:41 | Headings, tinted card, prototype wording; D-174 to D-185 |
| Prototype: answer by CVE, advisories, cases | 22 Nov 15:32 | `theirs_risk` answered as the prototype does, pills in its order, cases in its words; D-186 to D-196; 332 tests |
| Prototype: scanner triage | 22 Nov 16:03 | The report card, the CSV's lines and columns, the prototype's wording; D-197 to D-202 |
| Prototype: explorer and internal shell | 22 Nov 16:51 | The left panel holds the filter and products, series open in place, two halves; D-203 to D-215; 336 tests |
| Prototype: release, Spring Boot, workbench | 22 Nov 17:26 | The release page as one opened series, Spring Boot rows, the verdict queue; D-216 to D-225; 340 tests |
| Prototype: closing session | 22 Nov, this record | Three leftovers, the specifications, the ledgers, this report and `DEMO.md`; D-226 to D-228 |

## What exists

Eleven routes over one data service. The customer screens: home with the lookup, the answer by version, the answer by CVE, the scanner triage, the advisories catalogue and the case catalogue. The team's screens, behind the `SECURITY` role: the explorer, one release, the Spring Boot page, the verdict workbench and diagnostics, with the assistant in a panel on the right of every one, opened from the bar, D-203. Everything read comes from the published JSON of vaadin/cve-status through one cache; the only thing written is a verdict draft, to an H2 file, and a verdict leaves only through GitHub's own new file editor.

118 Java sources under `src/main/java`, 53 under `src/test/java`. The gates, both run with `OPENAI_API_KEY` and `OPENAI_TOKEN` removed from the environment, D-132:

| Gate | Command | Result |
| --- | --- | --- |
| Default | `env -u OPENAI_API_KEY -u OPENAI_TOKEN ./mvnw clean verify` | Green: 340 tests |
| Browser | `env -u OPENAI_API_KEY -u OPENAI_TOKEN ./mvnw clean verify -Pit` | Green: 340 tests, then 21 browser tests against the production jar |

At the end of epic 13 the two gates were green with 324 tests, and with 324 tests and 21 browser tests; the prototype sessions added 16 tests.

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

Several of these also reproduce on 25.3 or earlier and live in `FEEDBACK-PLATFORM.md`. Three rows died when their reproducer or the documentation was read, drafts 02, 04 and 06, and four drafts were corrected by their own reproduction; the ledger in `specs/issues/README.md` keeps them. Rows that need no draft say why there: the record of loosened policy directives, the `form-action` note that belongs on the `NativeForm` page vaadin/docs#6625 is writing, and the eight data needs toward vaadin/cve-status, four of them found during the alignment with the prototype. One row is not ready: the starter's `architecture.md`, which needs a wizard generated project to check against.

Epic 13 found no new platform finding. Its sweep found faults in our own code, above. The alignment with the prototype found no new platform finding either: what it found were four data needs and two defects of the prototype itself, in the next section but one.

## The 25.4 coverage, honestly

The overview claims typed JavaScript, `JsExpression`, `JsSnippet`, `callJsFunction`, return values and `addJsInitializer`, for epics 04 and 05. What the code uses is declared snippets with `@JsExpression`, through `executeJs(Class)` and `JsSnippets.of`, and one typed return value, the theme switch's `JavaScriptResult<Boolean>`, D-157. `callJsFunction` and `addJsInitializer` are used nowhere. The copy answer control of epic 05 uses the platform's Clipboard API, which does the job without a hand written call, D-63, and no piece of existing code has a client side lifecycle an initializer would fit; this epic adds no behaviour, so none was invented.

The other gaps against the coverage table: `ShortcutFilterJs` is internal, so the slash key is a filtered `keydown` listener, draft 12b; `CspMode` and the strict mode are not used, our own policy is, D-02; the JUnit 6 extensions serve the one test with no Spring context, since the documentation says they do not replace `SpringBrowserlessTest`; `QueryPage` pages no tool result, D-143; `RequestStartedEvent` is not listened to. The arrivals no epic claims are listed in `specs/00-overview.md` with a decision each.

## Compared with the JavaScript originals

The originals are two static pages on GitHub Pages, the customer status page and the findings explorer, which compute everything in the reader's browser from the same JSON and keep the team's screens behind a client side gate. The rebuild reads the same JSON on the server through one cache and answers the same questions, with these differences, each recorded where it was decided:

- A real sign in with a role replaces the client side gate; the data stays public, as it is on Pages.
- Every screen has a real address with its question in it, such as `/version/24.9.21?boot` or `/advisories?verdict=risk`, instead of a hash; the hash addresses are not kept, because the JavaScript pages stay published.
- A scanner report is read on the server and not kept; the triage files are produced there, CSV and PDF.
- The workbench keeps verdict drafts in a server side store shared by the team, and hands the YAML to GitHub's editor as the original does.
- New: the assistant, internal only, which answers from tool calls over the same data; diagnostics. The case catalogue is a route of its own, `/cases`, where the original has it as a screen of its page.
- Left out: the self contained email file, the RSS feeds, the redirect stubs, and a second language, with the bundle discipline kept, `specs/00-overview.md`.
- Where the data does not carry a fact, the page omits it, which holds the rebuild to the original's own rule. Two things the original's specification asks for cannot be built from the published data at all: ordering advisories by age, D-81, and an impact column, D-82. Both are data needs toward vaadin/cve-status.

Each feature's review in `specs/reviews/` compares the screen with the live page on the same question, as far as the live pages' sign in gate allowed at the time: the team's screens could not be compared past it then, P-03. Eight reviews passed what their feature document asked; the foundation, the data client, the explorer front door and the workbench failed as whole screens against the reference for reasons each review names. The comparison past the gate came afterwards, with the prototype's accounts the person gave, and is the next section; it closed P-03 and answered most of the design findings of P-02 to P-19.

## The comparison with the prototype

**Method.** After epic 13 the person gave the accounts of the prototype's two gates, the customer page and the team's findings pages of vaadin/cve-status, and made two choices: keep everything the application has, and reorder its screens to the prototype's distribution, adding what the prototype shows when the published data carries it. Six unattended sessions, driven by `.claude/commands/align-prototype.md`, took one group of screens each. Each started the application on the live published JSON, the data the prototype reads, asked both sides the same questions, the same versions, ids, report files and releases, and wrote the comparison before changing anything, element by element in the prototype's order with an action per row: move, add, transcribe wording, keep as app extra, or data not published. The comparisons and the screenshots of both sides are in `specs/reviews/prototype/`, one file per screen. Where the prototype's numbers were in doubt its sources were read, `web/findings/answer.js`, `card.js`, `home.js`, `release.js`, `spring.js` and `bench.js`, for the rules, never for code. Each session checked the result in a browser with real clicks and compared the whole text of each state again. The accounts were never written into the repository.

**Per screen, what moved and what was added.**

| Screen | Moved or added | Kept as app extras | Review |
| --- | --- | --- | --- |
| Customer shell, home, lookup | One H1, "Security status", and the purpose on every customer page, the lookup under them in a centered column of 912 pixels, My account in the trail, the measured chip's hover text, the Extended mark's sentence, the risk chip red and the line cards tinted, D-162 to D-172 | The dark switch, Sign out, tabs that do not navigate, the help of the version tab, the note for ids that do not parse, the impact sentence whole, D-165, D-171 | `home.md` |
| Answer by version | The headline and sections as H3 under the answer's H2, the card tinted with its state in capitals, `code` for the managed property and the products, the ids comma separated, the chevrons, D-174 to D-177 | The oldest measured release offered below every line, the relative scan date, absent fields left out where the prototype prints their names, D-182, D-184, D-185 | `version.md` |
| Answer by CVE | The same card and headings, line cards four to a row and tinted, an advisory in `theirs_risk` answered "Does not affect you" as the prototype answers it, superseding D-65, the prototype's wording, the verdict's commentary in the evidence, one retry of a 503, and an address whose ids do not parse answered "not in anything we scanned", D-186 to D-188, D-190, D-196, D-226 | The GHSA alias answer, the copy answer, the reader's line card following the version document, "Does not affect you" on a line that never reported it, D-189 | `cve.md` |
| Advisories | The prototype's heading, aside and tip, the verdict a row is listed under, pills in its order, every package whole, the red risk chip and the chevron, D-191, D-192, D-194 | The severity column, the shown count line, the context menu, the team's ask box, D-193 | `advisories.md` |
| Cases | The prototype's title, heading, tip, groups, order by count and chips, D-195 | The `needs` sentence only where the catalogue carries it | `cases.md` |
| Scanner triage | The report card as one row, the version question above it, the headline as H3, tinted buckets, the CSV's three comment lines and the prototype's columns with Severity, a secure row naming only a verdict that settled it, the prototype's wording, D-197 to D-200, D-202 | The file read on the server and not kept, the actions that name who moves, the PDF and its preview, the downloads of an unmeasured version, a CycloneDX finding counted once, D-201 | `scan.md` |
| Explorer and internal shell | The left panel holds only the filter and products, the destinations and the Assistant button in the bar, the series opened in place with `?open`, two halves of three groups, the catalogue of products with its default, chip counts as if on, rings with counts, the prototype's rows, search, legend and opened finding, D-203 to D-213, D-215 | The four destinations, the assistant, the trail, the Spring switch, the count sentence, the build only group, every control a history entry, the remembered groups, D-210, D-214 | `explorer.md` |
| Release page | The explorer's series row for one release, opened, shared through `SeriesParts`, its own left panel with the products counted on that release, the prototype's H1, legend, advice, files and footer, D-216 to D-218 | The count of five, the build only group, the scan date, the Read link, the sentences of D-110, D-115 and D-118 | `release.md` |
| Spring Boot page | One row per Spring Boot release shut on arrival, the supply in a few words, the prototype's groups and words, D-219 | The table header, the supply sentence, the GHSA link | `spring.md` |
| Workbench | "The verdict queue", one disclosure per package with the evidence the queue publishes, the advisories as ticks, the fill from a verdict the package carries, the commentary required to export, D-220 to D-223 | The summary in figures, the live YAML, Save, Export in two steps, Copy, the drafts, the paste box, the id rule, D-124 | `workbench.md` |

**The numbers that now match.** On the same live data on 22 November: the advisories pills, All 534, affects you 150, not assessed yet 1, not on a current release 121 and every other count, in the prototype's order; the explorer's product chips, `vaadin` 27, `vaadin-spring-boot-starter` 259, `control-center-starter` 136, and every one of the ten rows' badges, sentence and rings number for number, 24.10.14 "Vaadin: Not affected", "Spring Boot: Vulnerable", "26 findings arrive with your Spring Boot"; the opened 24.4 series, 25 a real risk, 50 answered, 13 in the frontend bundle, 156 with Spring Boot; on the release page of 24.9.21 the chips 42, 88 and 85, "removes 18 of these" where the application had said 22, and every group count; the Spring Boot page's "41 findings arrive with this release, 24 of them a real risk" on 3.5.16; the workbench's rows and the 61 ticks of tomcat-embed-core; the scanner triage of the eleven fixture files on seven versions, word for word but for the rows listed in `scan.md`.

**Observations about vaadin/cve-status.** Two things the comparison found are defects of the prototype rather than of the application or of Vaadin. They are recorded here for the person, with no draft:

- On a CycloneDX report the prototype names components by their `bom-ref`, "otel", "bcprov", rather than by the coordinate the reference resolves to, and counts the file's own findings twice, once as the file reports them and once as the advisory the measurements supply for the same coordinate: 55, 2 and 21 against the application's 52, 1 and 18 on the same file, `specs/reviews/prototype/scan.md`, D-70, D-201.
- The per row sentence of the explorer and the release page, "N more nobody has assessed yet", counts without the products nobody chooses, so a finding that arrives only through a frontend package is in the "not assessed yet" group under the row and not in the sentence over it: 6 over a group of 11 on 24.9.21, 52 over 55 on 24.4.20, 100 over 123 on 24.9.0. The application counts the sentence over the same findings as the group, D-224.

**What the prototype could not answer.** Its copy answer and its Notify me are "Not built yet", so whether a copied answer should carry a link stays open, P-10. It reads a report in the browser, so it says nothing about the progress of a large upload, P-13. It has no context menu on the advisories, so whether to announce ours stays open, P-14. No version on the published data is red only through findings nobody judged, so neither side shows the triage headline of D-53, P-08. Its customer header shows a subscription chip written into its markup, which no published document carries, and it marks three data as missing itself: whether a release changes the public API, the date an advisory was published, and why an impossible case cannot be drawn; all four are data need rows in `specs/FEEDBACK-25.4.md`. Its gate is not a sign in form and it has no diagnostics, so P-02 and P-19 are untouched. Its hand over into GitHub is the same kind of link as the application's, so P-17's submission is still the person's. The text links under 24 pixels and the advisory ids breaking at their hyphen were not measured against it, P-07, P-16; on the application alone the closing session found one id of six still breaking at a hyphen in the update sentence of 24.9.21 at 1280 pixels, and three at 390.

**After the pass.** The closing session fixed three leftovers: an address on the CVE screen whose ids do not parse now gets the not in our measurements card, D-226; the explorer rules that only their own tests used are gone, D-227; and the `PartialMatchMode` task of epic 03 reads as removed rather than open, D-228. `NEEDS-A-PERSON.md` has 7 rows resolved and 13 open, 7 of them only in part; `DECISIONS.md` has 228 rows, numbered without a gap, of which 27 are superseded in whole or in part by a later row, 45 confirmed by a later row, 1 extended by a later row, and 155 stand as written.

## What is not done

Seven checkboxes stay unticked across the specifications, each with its reason in the feature's Still open section. The eighth at the end of epic 13, US-3.1's `PartialMatchMode`, is no longer a checkbox: the requirement was removed with D-173 and the task reads as removed, D-228.

| Where | Checkbox | Why |
| --- | --- | --- |
| Epic 05, US-5.2 | Success read back through the typed JS call | The Clipboard API does it, D-63 |
| Epic 07, US-7.1 | Grid in the source order | No published date to order by age, D-81, D-161 |
| Epic 07, Definition of Done | Every acceptance criterion checked | AC1 below |
| Feature 07, AC1 | Order is severity, then age, then id | No date in the data, D-92 |
| Epic 11, US-11.2 | The five tools, `ToolException` refusals, `QueryPage` paging | `QueryPage` pages no tool result, D-143 |
| Epic 12, US-12.1 | Service init listener registering CSP and interception listeners | They reach the view through signals on their own classes, D-151 |
| Epic 12, US-12.1 | The signal singleton with counters and ring tails | The rings are not in the singleton, D-151, D-161 |

Not verified unattended, and waiting for a person in `NEEDS-A-PERSON.md`: the interactive polish pass, P-01; the visual findings of epic 01, P-02; the development mode without a licence, P-12; a real submit into GitHub's editor, P-17; the camera on a phone and the live model's wording, P-18; the diagnostics view's wording and findings, P-19; the rehearsal with the key and in production mode, P-20; and the parts of P-07, P-08, P-10, P-13, P-14 and P-16 the prototype could not answer. The reference pages past their sign in gate, P-03, were compared by the alignment. The demo walk of `DEMO.md` was made again on the aligned screens, on the fixtures in development mode; the production mode is exercised only by the `-Pit` browser tests.

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

# Where the work stopped

Epics 01 to 06 are implemented and committed. Epic 06 has every checkbox ticked. Epic 05 keeps one unticked task of US-5.2, D-63, and epic 03 one, D-40. `./mvnw clean verify` is green with 172 tests, `./mvnw clean verify -Pit` with 172 tests and 19 browser tests, both run with the Vaadin key of this machine. Without a key the default gate is green only through `commercialWithBanner`, D-80 and P-12. Nothing is pushed and nothing is posted.

The pass runs unattended, one epic per session, driven by `.claude/commands/implement-epic.md`.

## Next

1. Epic 07, advisories and cases, `specs/user-stories/epic-07-advisories-and-cases.md`, then 08 to 13 in order. 07 replaces the `AdvisoriesView` placeholder. Epic 08 reuses `base/ui/Downloads`, now with `onClick` for a button. The typed JavaScript return values, `callJsFunction` and `addJsInitializer`, which the overview gave to the copy answer control, are still unexercised, D-63: a later epic with a real need for them claims them, or the coverage table says they were not used.
2. At the end, with a person: `NEEDS-A-PERSON.md`, then `DECISIONS.md` row by row, then the issue drafts one by one.

## Where the decisions are

`DECISIONS.md`, numbered and never renumbered, D-50 to D-59 from epic 04, D-60 to D-66 from epic 05, D-67 to D-80 from epic 06. Read it before changing anything it covers. `NEEDS-A-PERSON.md` holds what only a person can do, P-07 and P-08 from epic 04, P-09 and P-10 from epic 05, P-11 to P-13 from epic 06. P-12 matters most: the default gate was proven red without a licence and is green only through `commercialWithBanner`, D-80.

## What exists now

- `base/ui`: `AppBar` shared by both shells with Breadcrumbs, the measured chip, the dark switch and Sign out. `CustomerLayout` is the `@Layout` of public routes and mounts the lookup under the bar through a `ShellLookup` bean. `InternalLayout`, its drawer built from `@Menu`, `getAssistantMount()` for epic 11. `ViewHeading`, `NotFoundView`, `ThemeSwitch`, `ThemeMemory`, `HelpDialog`.
- `base/i18n/TranslatedPageTitleGenerator`: the one writer of titles, route parameters arrive as `{0}`.
- `base/security`: `SecurityConfig` with `team` and `guest`, `LoginView`, `Roles`, `SessionExpiredNotice`, `CspPolicy`, `CspOnBootstrapPage`, `CspViolations` with its `/csp-report` endpoint.
- `data`: `PagesClient` is the only class that talks to the network. `PagesCache`, `SecurityDataService`, `VersionNumber`, the read models in `data/model`, `DataRefreshSignal`, `ExportKind`, `ExportFile`.
- `status`: `HomeView` with the line cards, `shownLines`, `spokenFor`, `secureFrom`. `Lookup` over the session scoped `LookupState`. Since epic 04 `VersionView` is the whole answer by version: the verdict card, findings with no fix yet, the not assessed notice, what the upgrade fixes, Spring Boot in two shapes, discontinued, details for a single finding, ruled out, all supported versions, and the not measured answer. `VersionAddress` is the one writer of its address, `?boot`, `?ruled`, `?disc`, `?open`, `?pre`. `VersionRules` answers the questions without components: the secure move, the nearest measured release, worst first, verdicts per advisory, release notes. `FindingCard` draws one finding and its three layers. `ScanView` and `AdvisoriesView` are still placeholders, D-37.
- `status/CveView` is the whole answer by advisory since epic 05: one `Section.cve-answer` per id in the order asked, each with its verdict card, the line cards across supported versions and the three layers of details. `CveRules` answers the questions without components: the fix for the reader, the settled state, the verdicts with their coordinates, the marked difference between coordinates. `Mark` is the `<mark>` element. The copy answer control is `Clipboard.onClick(button).writeText(text, onCopied, onError)`; the copied text is also on the button as `data-answer`, which the tests read. A GHSA alias is resolved only against the other asked advisories, D-62, because the data has no alias map, a data need row in `specs/FEEDBACK-25.4.md`.
- Section links on the version screen are real links with `router-ignore`, a plain click answered in place by a declared filter and `preventDefault`, each a history entry. A new version scrolls to the top through `VersionView.PageJs`; What it fixes and `?open` scroll through `Element.ScrollIntoViewJs`.
- `status/ScanView` is the whole scanner triage since epic 06, at `/scan` and `/scan/{version}`, `?secure` showing the secure rows. The report tab of `Lookup` takes the file through the modular upload, `UploadManager` with `UploadDropZone` and `UploadButton`, and a custom `UploadHandler`, `Lookup.receive`, that parses the request stream and hands the result to the session through `UI.access`. `ReportParser` reads the four formats by shape into a `ScannerReport`, `ReportFormat` lists them and is the one source of the formats line and the help, `Triage` answers it against `ScanIndex` and the version document, `TriageFiles` writes the CSV and the PDF, with PDFBox. `ScanState` is the session scoped holder of the read report and of the last refusal. `base/ui/PdfPreview` is the one door to the PDF Viewer and its feature flag, and `base/ui/Downloads.onClick` puts a download on a button through `Download.onClick`.
- Hand written scanner reports for the tests are under `src/test/resources/scan/`, one scan of an application on 24.9.21 in each of the four formats, plus edge cases. `FixturePages.offline()` gives a unit test the data service with no Spring context.
- Styles: `styles.css` imports `shell.css`, `data.css`, `lookup.css`, `home.css`, `version.css`, `cve.css`, `scan.css`.
- Feature flags are in the committed `src/main/resources/vaadin-featureflags.properties`, the PDF Viewer's for now. The `--status-*` and `--severity-*` tokens color the version screen too.
- Tests answer from `src/test/resources/pages/`, refreshed in epic 04 to `generated_at` 2026-11-21T03:33:59Z with `cases/v/24.7.19.json` added, D-50. Eight advisory documents since epic 05, D-61: one per state on 24.9.21. Four version states: 25.3.5 secure, 24.9.21 fix available, 24.4.28 unresolved with 25 stuck, 24.7.19 unresolved with one. `VersionViewBrowserlessTest`, `VersionRulesTest` and `ScrollActionsIT` are epic 04's, `CveViewBrowserlessTest` and `CopyAnswerIT` epic 05's.
- Guard rails: `NoHardcodedStringsTest`, `SpecConsistencyTest`, `NetworkBoundaryTest`.
- `specs/issues/`: twelve ready drafts, from epic 04 `14-query-parameters-order.md`, from epic 05 `15-clipboard-error-internal.md`, from epic 06 `16-download-onclick-testing.md` and `17-injected-component-bundle.md`, three withdrawn, twelve reproducer zips. `post.sh` dry run lists all of them.

## Known traps

- `./mvnw verify -Pit` runs the application in production mode, `./mvnw` in development mode. A browser test that passes against one and fails against the other is telling you they differ.
- The bootstrap page leaves without Spring Security's headers. `CspOnBootstrapPage` sets the policy there. Any other header wanted on that page needs the same.
- After a form sign in Chrome's save password bubble takes the first real click. A browser test that signs in switches the password manager off, as `CspIT` does.
- Read the documentation page for a behaviour before writing a feedback row. Three of the first nine rows were documented.
- A reproducer is zipped as `pom.xml src/main/java src/main/resources`, never `src` whole: running it makes Vaadin write five megabytes of generated files there.
- A stylesheet outside `/styles.css`, `/themes/**`, `/assets/**` needs a permit rule, declared or imported, for signed in users too.
- A public view needs `@AnonymousAllowed`, an internal one `@RolesAllowed(Roles.SECURITY)`.
- A string sent to `executeJs` is logged by the `warn` mode. Declare JavaScript in a `@JsDefinition` interface, as `ThemeSwitch` does.
- A local `ValueSignal` in an application scoped bean throws when a second session reads it. Shared state uses `SharedValueSignal`, and a browserless test calls `runPendingSignalsTasks()` after changing one.
- The return to the asked route is proven in `ShellSecurityBrowserlessTest` by posting the sign in on the session of the refused navigation.
- `@RouteParent` is the logical hierarchy for the trail, not the rendering layout.
- `./mvnw` has no hot reload. Restart after a Java change, `./mvnw process-resources` after a CSS change, `VAADIN_LAUNCH_BROWSER=false` to keep the browser closed. The dev loop is installed and measured in `specs/10-dev-loop.md`. Never run both.
- After deleting sources, stale classes in `target/` fail the next test run.
- `vaadin:install-dev-cli` wrote `.vaadin/`, `.agents/` and `.claude/skills/vaadin-devloop`. They are untracked.
- The live reference pages show a sign in gate, so the visual reviews compared only the gate and the header behind it.
- Signals refuse `get()` outside a reactive context. In a listener, a click handler or a navigation observer read them with `peek()`; `get()` belongs in `Signal.effect`, `Signal.computed` and the `bind*` lambdas.
- An effect may not write a signal it depends on. A listener that a server side change also fires, such as the selected change of `Tabs`, writes only when the value differs, as `Lookup` does.
- `Button.bindEnabled` is refused beside `setDisableOnClick`, in either order, `UNTIL_RESPONSE` too. Use `Signal.effect` with `setEnabled`, draft 13.
- `ComboBox.setPartialMatchMode` throws once custom values are allowed, as its documentation says.
- A combo box's typed text is not its value. Read it through the `filter` property with a `filter-changed` listener, and empty it with the component's own `clear()`.
- JavaScript aimed at an element that is invisible waits until it is shown, documented on the calling JavaScript page. Run it from a visible ancestor, passing the element as a parameter.
- A combo box dropdown left open on a field that gets hidden covers the page when the field is shown again. Close it on every tab change.
- Queries in browserless tests return only visible components. A field of a hidden tab is reached by walking the tree, as `LookupBrowserlessTest.inLookup` does.
- A browserless window opens on home before the test body runs, so navigating to home again reuses that view. Leave and come back to rebuild it.
- `FixturePages.cold()` moves the refresh signal, and an open view reads the index again at once. Replace a fixture after `cold()` and then call `revalidate()`, or the committed one is cached first.
- A keyboard filter is browser JavaScript, so a browserless test proves only the server half: it fires the event with `"@" + filter.getSnippetKey()` set to what the browser would have returned. The browser half is checked with Playwright.
- The first page load after adding a component or a `@JsDefinition` rebuilds the development bundle, about ten seconds.
- A refresh of the fixtures moves every date the tests assert on. `FixturePages.PINNED_GENERATED_AT`, `FixturePages.NOW` and the literals in the data and home tests move with it, as they did in D-50.
- A component built inside one effect and holding a binding or effect of its own is tracked on its own: toggling a flag that only a section link reads does not redraw the sections around it. `VersionViewBrowserlessTest` asserts the same instances survive.
- A link drawn once and kept while other flags move must read the address when clicked, not when drawn. `VersionView.sectionLink` keeps its `href` in step through the `shown` signal.
- `QueryParameters` writes parameters in hash order, whatever order they were built or parsed in, draft 14. Compare addresses as maps in tests, `VersionViewBrowserlessTest.assertAddress`.
- A browserless test that fires a DOM event at a listener with a declared filter must answer the filter itself, `"@" + filter.getSnippetKey()` set to true, and goes through `ElementListenerMap`, because the testers' `fireDomEvent` is protected. The documentation recommends routing intent through a public method instead.
- Work for the end of a request, such as revealing what `?open` names, goes in `UI.beforeClientResponse` after the drawing effect has run, not from `beforeEnter` alone: in a browserless test the callback registered in `beforeEnter` ran before the view was drawn. A small reproducer did not show it, so it is not a finding.
- Verdict prose carries words a browser cannot break, `<opentelemetry.version>1.62.0</opentelemetry.version>`. Markdown in a grid column needs `min-width: 0` and `overflow-wrap: anywhere`, or a phone scrolls sideways.
- The platform has a Clipboard API since 25.2, `Clipboard.onClick(component)`, with an observed form that reports success and failure back to the server. Read `flow/browser-apis/clipboard-api` before writing JavaScript for a browser API: there may already be a Java one.
- Chrome grants a clipboard write inside a click whatever `Browser.setPermission` says, so a refused write cannot be produced by denying the permission. `CopyAnswerIT` replaces `navigator.clipboard.writeText` with a rejecting one through `Page.addScriptToEvaluateOnNewDocument`. Reading the clipboard back needs `clipboard-read` granted through the DevTools protocol; in the Playwright browser a `readText` without it hangs for half an hour.
- A list from `List.of` in a sort throws inside the drawing effect, and the whole view stays blank with only a server log line. Copy into an `ArrayList` before sorting.
- Two published documents can disagree: the advisory document's `on_versions` called 24.9.26 a risk for CVE-2026-89407 while the version document and the index call 24.9.26 secure. The by CVE screen follows the version document for the reader's line, D-64.
- An absent value inside a table row is no empty cell: the cell before it spans the column, `TableCell.setColspan`, as the ruled out table does since D-60.

- Adding a component outside the pre-compiled production bundle, the PDF Viewer here, makes every production build create an application bundle, and its scanner follows only bytecode reachable from routes. The lookup, which the shell gets through the `ShellLookup` bean, was missing from it: `@Uses(Lookup.class)` on `HomeView`, D-79, draft 17. A new component reached only through a bean needs the same.
- With a commercial component in the application bundle the default build fails on a machine without a licence. `commercialWithBanner` on the default plugin keeps the gate green there, D-80. To check it without moving anybody's key, run in a copy of the tree with `MAVEN_OPTS=-Duser.home=<empty dir>`, `MAVEN_USER_HOME=~/.m2` and `-Dmaven.repo.local=~/.m2/repository`.
- An `UploadHandler` runs on the upload request outside the session lock. Touch signals and components only inside `event.getUI().access(...)`. The browserless `UploadButton` tester runs it and the access task within the call.
- A `Download.onClick` button carries its address only in an attribute named `data-flow-download-` and an id. `ScanBrowser.downloadAddressOf` reads it so a test fetches instead of clicking, draft 16. No browserless tester fetches it.
- The file input of `vaadin-upload-button` is in its shadow root: Selenium's `getShadowRoot().findElement(By.cssSelector("input[type=file]")).sendKeys(path)` uploads a real file.
- The fixtures carry no version document of 24.9.26, the release the not measured offer names for 24.9.x, so following that offer in a test lands on another not measured answer.

## Left undone

- Epic 06: the review's major issues are in `specs/reviews/06-scan-triage/REVIEW.md` and P-13: no severity anywhere and rows in the report's order, the empty right side at 1920 wide, the help mark alone under the drop zone, no progress for a large upload. The wording without a source is P-11. Whether the default build should carry Vaadin's banner without a licence, and the development mode without one, is P-12: only the `verify` gate was run without the key. The live reference past its gate was not compared, P-03. The PDF's layout was checked by eye on one page; a report long enough for several pages is only exercised by the parser, not looked at.
- Epic 05: US-5.2's typed JavaScript task is unticked, D-63. The review's major issues are in `specs/reviews/05-answer-by-cve/REVIEW.md` and P-10, the wording without a source is P-09. The live reference past its gate was not compared, P-03. A version given as a prerelease line other than the reader's is not exercised by a fixture. `?open` on the CVE screen, which the source page uses to open the second layer, is not built: the feature asks for only the first layer open on arrival.
- Epic 04: verdict prose is drawn whole, so on 24.7.19 the red card's actions fall below the fold at 1920 by 1080; the right 40 percent of a wide screen is empty; text links are 19.5 pixels high. All in `specs/reviews/04-answer-by-version/REVIEW.md` and P-07. The wording without a source is P-08. The fixes cap of 25 is code, not exercised by any fixture: 24.9.21 has 12. The prerelease badge for a measured prerelease is not exercised either, no prerelease version document is a fixture.
- Epic 03: the version field uses no `PartialMatchMode`, D-40. The report tab's upload is disabled until epic 06, D-42. The review's major issues are in `specs/reviews/03-home-and-lookup/REVIEW.md` and P-06. The wording without a source is P-05.
- Review findings in `specs/reviews/02-data-client/REVIEW.md`, and P-04.
- Review findings in `specs/reviews/01-foundation/REVIEW.md`.
- The enforced policy is verified with push, sign in, home with the lookup, and the version screen in development mode. Uploads and the PDF viewer were verified in epic 06, in production mode in `ScanUploadIT` and `TriageDownloadIT`, with nothing loosened. The AI panel arrives in epic 11 and has to pass `CspIT` and leave the `warn` log clean.

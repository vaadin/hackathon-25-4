# Where the work stopped

Epics 01, 02 and 03 are implemented and committed. Epic 03 has every checkbox ticked but one: the version field cannot use `PartialMatchMode` beside free text, D-40. `./mvnw clean verify` is green with 96 tests, `./mvnw clean verify -Pit` with 96 tests and 9 browser tests. Nothing is pushed and nothing is posted.

The pass runs unattended, one epic per session, driven by `.claude/commands/implement-epic.md`.

## Next

1. Epic 04, answer by version, `specs/user-stories/epic-04-answer-by-version.md`, then 05 to 13 in order. Epic 04 replaces the placeholder `status/VersionView`, 05 `CveView`, 06 `ScanView` and wires the report tab's upload, 07 `AdvisoriesView`.
2. At the end, with a person: `NEEDS-A-PERSON.md`, then `DECISIONS.md` row by row, then the issue drafts one by one.

## Where the decisions are

`DECISIONS.md`, numbered and never renumbered, D-37 to D-49 from epic 03. Read it before changing anything it covers. `NEEDS-A-PERSON.md` holds what only a person can do, P-05 and P-06 from epic 03.

## What exists now

- `base/ui`: `AppBar` shared by both shells with Breadcrumbs, the measured chip, the dark switch and Sign out. `CustomerLayout` is the `@Layout` of public routes and mounts the lookup under the bar through a `ShellLookup` bean, so `base` names no feature. `InternalLayout` is named by internal views in `@Route(layout = ...)`, its drawer lists every route with a `@Menu` whose title is a bundle key, `getAssistantMount()` is for epic 11. `ViewHeading` is the H1 of a view and reads the resolved title. `NotFoundView` keeps Flow's not found view in the shell. `ThemeSwitch` and `ThemeMemory` hold the dark mode. `HelpDialog` is the one shape of a hint: a question mark whose name is the dialog's title, and `HelpDialog.section` for a heading and its paragraphs.
- `base/i18n/TranslatedPageTitleGenerator`: the one writer of titles. A route puts a bundle key in `@PageTitle`, route parameters arrive as `{0}`, `{1}`.
- `base/security`: `SecurityConfig` with `team` and `guest`, `LoginView`, `Roles`, `SessionExpiredNotice`, `CspPolicy`, `CspOnBootstrapPage`, `CspViolations` with its `/csp-report` endpoint.
- `data`: `PagesClient` is the only class that talks to the network. `PagesCache`, `SecurityDataService`, `VersionNumber`, the read models in `data/model`, `DataRefreshSignal`, `ExportKind` and `ExportFile`, as epic 02 left them.
- `status` since epic 03: `HomeView` with the line cards, Recently reviewed and the designer's pointer, redrawn when the refresh signal moves, `?pre` for prereleases. `HomeView.shownLines`, `spokenFor` and `secureFrom` are the line rules the version screen will want too. `Lookup` is the three tab lookup over the session scoped `LookupState`, fed by `LookupProvider`; `LookupInput` cleans versions and splits ids. `Lookup.KeysJs` holds the declared keyboard filters. `VersionView`, `CveView`, `ScanView`, `AdvisoriesView` are placeholders with a heading, D-37.
- The answering routes: `/version/{version}`, `/cve/{ids}/{version}` with the ids comma separated and the version optional, `/scan/{version}`, `/advisories`. Each navigation writes its question into `LookupState`.
- Styles: `styles.css` imports `styles/shell.css`, `styles/data.css`, `styles/lookup.css`, `styles/home.css`. The `--status-*` tokens now color the card states.
- Tests answer from `src/test/resources/pages/`. `FixturePages` and `FixtureTransport` wire them in, `FixturePagesServer` serves them to a browser test. `HomeBrowserlessTest`, `LookupBrowserlessTest`, `LookupInputTest` are epic 03's.
- Guard rails: `NoHardcodedStringsTest`, `SpecConsistencyTest`, `NetworkBoundaryTest`.
- `specs/issues/`: eight ready drafts, 01, 03b, 07b, 08, 10, 11 and from epic 03 `12b-comment-on-flow-5473.md` and `13-bind-enabled-until-response.md`, three withdrawn, eight reproducer zips. `post.sh` dry run lists all of them.

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


## Left undone

- Epic 03: the version field uses no `PartialMatchMode`, D-40, so its task stays unticked. The report tab's upload is disabled until epic 06, D-42. The review's major issues are in `specs/reviews/03-home-and-lookup/REVIEW.md` and P-06. The wording without a source is P-05.
- Review findings in `specs/reviews/02-data-client/REVIEW.md`, and P-04: the unreachable sentence and the download labels are our wording.
- Review findings in `specs/reviews/01-foundation/REVIEW.md`: the 22px switch, no way back to the system color scheme, no favicon, an empty title on the not found view, no help line on the sign in form.
- The enforced policy is verified with push, sign in and now the home page with the lookup. Uploads, the AI panel and the PDF viewer arrive in later epics and each has to pass `CspIT` and leave the `warn` log clean.

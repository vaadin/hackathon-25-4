# Where the work stopped

Epics 01 and 02 are implemented and committed, every checkbox ticked. `./mvnw clean verify -Pit` is green with 58 tests and 9 browser tests. Nothing is pushed and nothing is posted.

The pass runs unattended, one epic per session, driven by `.claude/commands/implement-epic.md`. The session for epic 02 was cut twice by a dropped connection and was closed by hand: the gates and the commits are verified, this file is not as rich in traps for epic 02 as it is for epic 01.

## Next

1. Epic 03, home and the lookup, `specs/user-stories/epic-03-home-and-lookup.md`, then 04 to 13 in order.
2. At the end, with a person: `NEEDS-A-PERSON.md`, then `DECISIONS.md` row by row, then the issue drafts one by one.

## Where the decisions are

`DECISIONS.md`, numbered and never renumbered. Read it before changing anything it covers. `NEEDS-A-PERSON.md` holds what only a person can do.

## What exists now

- `base/ui`: `AppBar` shared by both shells with Breadcrumbs, the measured chip, the dark switch and Sign out. `CustomerLayout` is the `@Layout` of public routes, `getLookupMount()` is for epic 03. `InternalLayout` is named by internal views in `@Route(layout = ...)`, its drawer lists every route with a `@Menu` whose title is a bundle key, `getAssistantMount()` is for epic 11. `ViewHeading` is the H1 of a view and reads the resolved title. `NotFoundView` keeps Flow's not found view in the shell. `ThemeSwitch` and `ThemeMemory` hold the dark mode.
- `base/i18n/TranslatedPageTitleGenerator`: the one writer of titles. A route puts a bundle key in `@PageTitle`, route parameters arrive as `{0}`, `{1}`.
- `base/security`: `SecurityConfig` with `team` and `guest`, `LoginView`, `Roles`, `SessionExpiredNotice`, `CspPolicy`, `CspOnBootstrapPage`, `CspViolations` with its `/csp-report` endpoint. `CspViolations.newestFirst()` is what the diagnostics view of epic 12 reads.
- `data` since epic 02: `PagesClient` is the only class that talks to the network, with ETag revalidation. `PagesCache` holds the time to live rules, single flight and stale over error. `SecurityDataService` is what views and tools call, and it throws `DataUnreachableException` only on a cold cache with no network. `VersionNumber` compares versions, never a string comparison. `data/model` has the read models as records, mapped one to one from the published JSON. `DataRefreshSignal` is published when the index moves. `ExportKind` and `ExportFile` are the per release machine files.
- `base/ui/Downloads` is the one helper for anything that leaves the application, on the 25.4 `Download` API. `base/ui/DataOutageNotice` is the one unreachable sentence.
- Tests answer from `src/test/resources/pages/`, byte for byte copies of the published JSON. `scripts/refresh-fixtures.sh` is the only thing that changes them and the only task that needs the network. `FixturePages` and `FixtureTransport` wire them in, `FixturePagesServer` serves them to a browser test. The `Clock` is fixed in tests. `NetworkBoundaryTest` fails if anything outside `data` reaches for HTTP.
- `styles.css`: the `--status-*` and `--severity-*` tokens. Nothing consumes them yet.
- Guard rails: `NoHardcodedStringsTest` and `SpecConsistencyTest`. Ticking the last checkbox of a story or a feature makes its test classes mandatory.
- `specs/issues/`: the procedure in `README.md`, the template, five ready drafts, three withdrawn ones, five reproducer zips.

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

## Left undone

- Review findings in `specs/reviews/02-data-client/REVIEW.md`, and P-04 in `NEEDS-A-PERSON.md`: the unreachable sentence and the download labels are our wording.
- Review findings in `specs/reviews/01-foundation/REVIEW.md`: the 22px switch, no way back to the system color scheme, no favicon, an empty title on the not found view, no help line on the sign in form.
- The enforced policy is verified with push and sign in only. Uploads, the AI panel and the PDF viewer arrive in later epics and each has to pass `CspIT` and leave the `warn` log clean.
- The chip wording "Measured {0}" with an ISO date matches the live header. Epic 02 owns the final wording.

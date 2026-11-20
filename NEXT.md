# Where the work stopped

Epic 01, foundation and shell, is implemented: US-1.1 to US-1.5, every checkbox ticked. `./mvnw clean verify -Pit` is green with 17 tests and 6 browser tests. Nothing is committed: the epic commits once, and nobody has asked for the commit yet.

## Next

1. Commit epic 01, and separately the record of the work: `NEXT.md`, `specs/reviews/`, the feedback files and `specs/issues/`.
2. Push, then `./specs/issues/post.sh` for a dry run and `--confirm` to post. Five entries are ready: three issues and two comments. Posting adds the issues to `vaadin/platform#9642`.
3. Epic 02, the data client, `specs/user-stories/epic-02-data-client.md`.

## Decisions waiting for a person

Each was taken to keep moving and is cheap to reverse.

| Decision | Why it was taken | The alternative |
| --- | --- | --- |
| `anyRequest(permitAll)` in `SecurityConfig` | With the default deny all, an unknown address sends an anonymous reader to the login page and a signed in one to a bare 403 | Keep deny all and accept that, which is the framework's design. As it stands, any future endpoint that is not a Vaadin view is open by default |
| Our own enforced CSP with `'unsafe-inline'` and `'unsafe-eval'`, `vaadin.csp=warn` | The 25.4 strict mode sends nothing in development and covers only the bootstrap page | `vaadin.csp=strict` and no policy of ours: stricter in production, nothing at all while developing |
| The drawer lists only routes that exist | A link to a missing route would be an invention | List the four destinations the feature names from the start |
| No custom access denied view | Not asked for by the story | The documented `HasErrorParameter<AccessDeniedException>` view, so a signed in user without the role reads "not allowed" and not "not found" |
| Partial stylesheets in `/styles/` with a permit rule | Found before knowing the public list | Move them under `/assets/` or `/themes/`, which are public by default, and delete the rule |
| No session limit, so `VaadinExpiredSessionStrategy` is idle | The two demo accounts are shared | Set `maximumSessions` and the 25.4 strategy becomes live |
| Breadcrumbs trail is Vaadin, Security status, leaf | The feature document says so | Add "My account" as the live page has |
| Placeholder `HomeView` and `ExplorerView` | A shell needs a route to render and a boundary needs a route to guard | None, epics 03 and 08 fill them |

## What exists now

- `base/ui`: `AppBar` shared by both shells with Breadcrumbs, the measured chip, the dark switch and Sign out. `CustomerLayout` is the `@Layout` of public routes, `getLookupMount()` is for epic 03. `InternalLayout` is named by internal views in `@Route(layout = ...)`, its drawer lists every route with a `@Menu` whose title is a bundle key, `getAssistantMount()` is for epic 11. `ViewHeading` is the H1 of a view and reads the resolved title. `NotFoundView` keeps Flow's not found view in the shell. `ThemeSwitch` and `ThemeMemory` hold the dark mode.
- `base/i18n/TranslatedPageTitleGenerator`: the one writer of titles. A route puts a bundle key in `@PageTitle`, route parameters arrive as `{0}`, `{1}`.
- `base/security`: `SecurityConfig` with `team` and `guest`, `LoginView`, `Roles`, `SessionExpiredNotice`, `CspPolicy`, `CspOnBootstrapPage`, `CspViolations` with its `/csp-report` endpoint. `CspViolations.newestFirst()` is what the diagnostics view of epic 12 reads.
- `data/DataRefreshSignal`: a `SharedValueSignal<Instant>`, empty until epic 02 calls `publish`.
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

- Review findings in `specs/reviews/01-foundation/REVIEW.md`: the 22px switch, no way back to the system color scheme, no favicon, an empty title on the not found view, no help line on the sign in form.
- The enforced policy is verified with push and sign in only. Uploads, the AI panel and the PDF viewer arrive in later epics and each has to pass `CspIT` and leave the `warn` log clean.
- The chip wording "Measured {0}" with an ISO date matches the live header. Epic 02 owns the final wording.

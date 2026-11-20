# 00 Overview

## What we are building

Security Status 25.4 is a rebuild of the two JavaScript apps that vaadin/cve-status publishes on GitHub Pages, as one Vaadin Flow application. The customer half answers one question, "is my Vaadin version safe, and what do I do", asked three ways: by typing a version, by pasting one or more advisory ids, or by uploading a scanner report. The internal half is the security team's reading of the same measurements: an explorer of every measured release narrowed by the products an application actually uses, a Spring Boot page, and the workbench where verdicts are composed, ranked by how much each one would close.

The rebuild exists for two reasons. First, the originals are hand rolled JavaScript with no framework: every table, dialog, router and store is bespoke code that Vaadin components replace outright, so the comparison between the two is itself a result. Second, Vaadin 25.4 (beta2) and 25.3 ship a large batch of new APIs that are best judged inside a real application rather than in isolated snippets, and this application is real: the same support engineers who answer tickets from the current pages can answer them from this one.

Every feature here exists because the current pages have it or a reader needs it. Where a 25.4 or 25.3 API fits that need, the specification names the API. Where it does not, we do not force it.

## What the originals are, and what carries over

The behaviour contract is `docs/requirements.md` section 6 of vaadin/cve-status, which specifies both pages rule by rule, and the live pages themselves: <https://vaadin.github.io/cve-status/> and <https://vaadin.github.io/cve-status/findings/>. Three of its rules carry over verbatim because they are about honesty rather than rendering, and they bind every screen here:

1. **Missing data degrades to silence and never to an invention.** A field the data does not carry is omitted, never rendered empty and never filled with wording of ours.
2. **A version missing from a table is one nobody measured, which is not the same as one with nothing wrong**, and the page says so where it bites.
3. **Not assessed yet is said out loud.** A finding nobody has judged is never folded into secure and never dressed as a risk.

What does not carry over, each a decision rather than an accident:

| Original rule | Here | Why |
| --- | --- | --- |
| The pages fetch nothing and open from `file://` | The server fetches, the browser talks only to the server | That rule existed because the explorer had no server. This application is a server |
| The scanner report never leaves the browser | The report is parsed in the request and never persisted, and the screen says so | A Vaadin upload goes to the server by construction. The promise that matters, nobody keeps your bill of materials, is kept and stated |
| A client side sign in gate per page | Real authentication for the internal area, none for the customer area | The gates were a deterrent on a static host with no access control. A server has the real thing, so the deterrent would be a step backwards |
| The hash address grammar `#version/...` | Real routes, `/version/...`, with the query flags kept | Flow routing is path based. The JS pages stay published, so no inbound link breaks |
| Pixel fidelity to the Figma file | The same structure and the same answers, drawn with Vaadin components and Aura | The design was a contract for a hand rolled page. Rebuilding it pixel for pixel in a component framework would mean fighting the components, which inverts the point of the exercise |

## Hard rules

1. Nothing is copied from the JavaScript apps. They are read for behaviour only. No file, no function, no CSS rule is carried over.
2. Signals first. Cross component state is a signal, not a listener chain and not a mutable field read from three places.
3. Views never fetch. The `data` package is the only place that talks to the network, and views read it through signals and services.
4. The page never invents a value. Absent is omitted. This is rule 4.7 of the source project and it outranks visual completeness.
5. Every string a user can read comes from the translation bundle. One language, English, but no literal in Java, so the sweep that adds a second language is mechanical.
6. A feature is done when its acceptance criteria are checked, `./mvnw verify` is green, and the test cases named in its feature document exist as real tests.
7. No hacks. Where a platform API leaves no reasonable way to do something, the workaround is written plainly, kept small, and reported so it can be deleted when the platform is fixed. A workaround nobody wrote down is a hack.
8. Every workaround is recorded. A missing API, an awkward one, a bug routed around, or something that turned out to be impossible goes into `specs/FEEDBACK-25.4.md`, or into `specs/FEEDBACK-PLATFORM.md` when it is older than this release. Finding these is half the reason this application exists.

## How the work is committed

The specifications are written once, before any implementation, and they land as one commit. Judging them is easier when they arrive whole: a feature document that appears next to the code that implements it is a description, not a specification.

After that, each epic ends in a single commit carrying everything it changed: the implementation, whatever it had to change in shared code, the shell, the theme, the data layer, and its tests, all together.

What never travels with an epic is the record of the work rather than the product: the build report, the findings for the Vaadin teams, the note for whoever opens the next session. Those are written as things are discovered and committed on their own at the end.

## How the work is scheduled

Four passes, and what may happen in each is part of the specification because mixing them is what produces a codebase nobody can review.

| Pass | What happens | What does not |
| --- | --- | --- |
| Specification | These documents, written and validated whole, in one commit | No implementation |
| Implementation | One unattended session per epic, each epic ending in one commit, tests written with the code | No interactive polish |
| Polish, step one: vibe coding | Interactive, with the dev loop running and a browser open. Behaviour and appearance are argued out view by view and changed in seconds | No tests are run and none are written, even at the cost of a red suite |
| Polish, step two | Unattended. The specifications are brought up to what the screens now do, the tests are written or repaired, and the gate is green again | No new behaviour |

The epics map onto the passes like this: epics 01 to 11 are the implementation pass, then polish step one runs interactively, then epic 12 lands, because it instruments screens rather than shaping them and instrumenting a screen still being redesigned is work done twice, and epic 13 is polish step two itself, which is why its feature document adds no behaviour.

## Licensing

There is a Vaadin licence, so a commercial component is the default choice wherever it is the right tool, and choosing a core component in its place needs a reason. What that comes to here: the AI controllers drive the assistant, the workbench form and the advisories grid, `PdfViewer` previews the audit export, and Charts draws the workbench queue summary if epic 10 keeps it. GridPro, Crud and Dashboard have no work here that plain Grid and layouts do not do better, because almost everything on these screens is read only, and that absence is itself a data point for the 25.4 report.

No code here checks a licence, degrades, hides a feature or offers a substitute for one. Vaadin enforces its own terms: a production frontend build without a licence fails, and development marks what is unlicensed.

## Actors

| Actor | Authenticated | What they do |
| --- | --- | --- |
| Customer with a version | no | Types a version, reads the verdict, follows the upgrade advice, downloads nothing |
| Customer with advisory ids | no | Pastes one or more CVE or GHSA ids, with or without a version, and reads what each means for them |
| Auditor with a scanner report | no | Uploads an osv-scanner, OWASP Dependency-Check, bomber or CycloneDX JSON file and takes away a triaged answer, as CSV and as a PDF preview |
| Support engineer | no | Answers a ticket from the same screens, and copies the answer with one control |
| Security team member | role `SECURITY` | Reads the explorer, the release and Spring Boot pages, works the verdict workbench, asks the assistant |

## Phases and epics

Phase 1 is the customer page, complete, and demoable on its own. Phase 2 is the internal half plus the assistant.

| Phase | Epic | Size |
| --- | --- | --- |
| 1 | 01 Foundation and shell | M |
| 1 | 02 The data client | M |
| 1 | 03 Home and the lookup | M |
| 1 | 04 Answer by version | L |
| 1 | 05 Answer by CVE | M |
| 1 | 06 Scanner report triage | L |
| 1 | 07 Advisories and cases | S |
| 2 | 08 Explorer front door | L |
| 2 | 09 Release and Spring Boot pages | M |
| 2 | 10 Verdict workbench | M |
| 2 | 11 Assistant | L |
| 2 | 12 Diagnostics | S |
| 2 | 13 Tests, sweep and demo polish | M |

If time runs out, cut phase 2 from the bottom of this order: 12, 13, 09, 10. Epics 08 and 11 are not on the cut list: the explorer is the internal half of the brief and the assistant is a MUST.

## Use cases

These are the acceptance tests, written in the language of the actor. Each one is expanded in the feature document that owns it.

### Customer with a version

| # | Use case |
| --- | --- |
| V1 | Type a version, in any of the spellings people use, and get the verdict card: secure, fix available, or unresolved |
| V2 | Read why: the badges, the one sentence, and the date it was measured |
| V3 | Follow "View release notes" to the right repository for that major |
| V4 | Jump from the card to the list of what the upgrade fixes |
| V5 | Read the findings that have no fix yet, each with what to do now and what the fix waits on |
| V6 | Read the Spring Boot section in the shape the state calls for: a pointer note, or the table with the supply status |
| V7 | Show and hide the findings that were looked at and ruled out, and the ones only a discontinued product brings |
| V8 | Read the all supported versions table with their own line marked "yours" |
| V9 | Ask about a version nobody measured and be answered where they asked, with the nearest measured release offered |
| V10 | Share the answer: the address carries the whole question and every open section |

### Customer with advisory ids

| # | Use case |
| --- | --- |
| C1 | Paste one or several ids, separated however they come, and get one verdict card per id |
| C2 | Give a version too and have every card answer for that version, "fixed in X, you are on Y" |
| C3 | Read the per line cards: how each maintained line stands against this advisory, theirs marked "yours" |
| C4 | Open the details: what it means for them, the modules and the fix, and the advisory with the evidence |
| C5 | Copy the answer for their ticket with one control |
| C6 | Ask about an id we never measured and be told that plainly |

### Auditor with a scanner report

| # | Use case |
| --- | --- |
| S1 | Drop a report file and have its format detected: osv-scanner, OWASP Dependency-Check, bomber, CycloneDX |
| S2 | Have the Vaadin version read from the file, or be asked for it only when the file does not name one |
| S3 | Read the headline: one upgrade clears N of M, or N findings need attention |
| S4 | Read the three buckets: already secure, fix available, unresolved, with counts and detail |
| S5 | Read the open findings table with owner and action per row, and show the secure ones on demand |
| S6 | Download the triage as CSV, and preview the audit report as a PDF before taking it |
| S7 | Learn what is assessed and what is not, counted against their own file |
| S8 | See skipped entries accounted for, never silently dropped |

### Home

| # | Use case |
| --- | --- |
| H1 | See every followed line at a glance: state, newest release, what to move to, scan date |
| H2 | See the recently reviewed advisories and reach the full list |
| H3 | Reach the lookup from anywhere, and switch tabs without losing what was typed |
| H4 | Focus the lookup with the keyboard, and withdraw a question by clearing the field |

### Security team

| # | Use case |
| --- | --- |
| T1 | Sign in, and be refused without the role |
| T2 | Read one row per maintained line, with the Vaadin and Spring Boot badges and per product rings |
| T3 | Narrow everything to the products their application uses, and keep that selection between visits |
| T4 | Open a line and read the groups: a real risk, not assessed yet, answered |
| T5 | Search by advisory, version or dependency, and jump to a release by typing its version |
| T6 | Download the machine files per release: JSON, CSV, VEX, suppressions, osv-scanner, bomber |
| T7 | Read one release in full on its own page |
| T8 | Read what Spring Boot brings, grouped by Spring Boot release, with the supply status |
| W1 | Read the verdict queue ranked by how much one verdict would close |
| W2 | Compose a verdict in a real form with validation, and read the YAML it produces |
| W3 | Open GitHub's new file editor with the YAML and the right path prefilled |
| W4 | Keep a draft between sessions |
| A1 | Ask the assistant "is 24.9.21 safe" and get the measured answer with the source named |
| A2 | Ask "what does CVE X mean for me" and get the verdict, not an opinion |
| A3 | Photograph or screenshot a scanner line and have the assistant read it and answer it |
| A4 | Ask the advisories grid for a slice in plain language |
| D1 | Open diagnostics and read navigation outcomes, request timings and session counts, live |

## Glossary

| Term | Meaning |
| --- | --- |
| Line | A maintained minor, such as 24.9, with one newest measured release |
| Measurement | One scan of one release: its findings, its dependency trees, when |
| Advisory | A CVE or GHSA record, with severity, summary and aliases |
| Verdict | Vaadin's written assessment of one advisory on one package: `risk`, `configuration`, `no_risk`, `not_affected`, `false_positive`, `fixed`, or `not_current`. `in_triage` is the absence of one |
| State of a version | `secure` when nothing is open, `fix_available` when the newest release of the line clears everything open, `unresolved` otherwise |
| Owner | Who can change the vulnerable version: Vaadin, Spring Boot, the library, or the reader |
| Severity | One of exactly four words: Critical, High, Moderate, Low, worst first in every ordering |
| Theirs | A finding every route of which comes from the reader's own Spring Boot, not from Vaadin |
| From the build | A finding only build tooling carries. Nothing deployed contains it |

## What we deliberately leave out

The nightly measurement pipeline, verdict publication, and everything else that writes to vaadin/cve-status: this application reads the published JSON and the one thing it composes, a verdict, leaves through GitHub's own editor as it does today. The self contained email file, the RSS feeds and the redirect stubs: they are publishing artifacts of the static site, not screens. The client side gates, replaced by real authentication. The hash address compatibility, because the JS pages stay published and keep their addresses. A second interface language, cut to one, with the bundle discipline kept so adding one back is mechanical.

Also out of scope: GridPro, Crud, Dashboard and Spreadsheet, because nothing here edits tabular data in place and the one dashboard like screen is the home page the design already shapes; Collaboration Kit, because shared signals cover the one live surface; Hilla and React, because this is a Flow application; native image builds.

## 25.4 feature coverage

Every feature lands in exactly one epic unless two are named. If an epic is cut, this table says what coverage is lost. The inventory behind it is the documentation server's own list of what 25.4 introduced, read on 2026-11-20.

| 25.4 feature | Epic |
| --- | --- |
| Content Security Policy: `ContentSecurityPolicy`, `CspMode`, violation reporting | 01 |
| `VaadinExpiredSessionStrategy` | 01 |
| Login success and failure handling in the security configurer | 01 |
| Typed web storage access for the theme memory | 01 |
| Typed JavaScript from the server: `JsExpression`, `JsSnippet`, `callJsFunction`, return values, `addJsInitializer` | 05, the copy answer control, and 04, the scroll to the fixes list |
| Keyboard shortcuts with JS filters: `ShortcutFilterJs` | 03, the slash shortcut and Escape to withdraw |
| `DisableOnClickMode` | 03, the Check button while a question is answered |
| The `Download` API, content types, URL postfix, download from a component click | 06, and epic 08 reuses it for the per release files |
| PDF Viewer | 06, the audit report preview |
| `UploadCapture` | 11, photographing a scanner line |
| `GridContextMenu` dynamic content | 07, the advisories context menu |
| FormLayout side labels, `LabelTextAlign` | 10, the verdict form |
| `NativeForm` with method and enctype | 10, the GET into GitHub's new file editor |
| `NavigationStartedEvent`, `NavigationEndedEvent` and its outcomes | 12 |
| `RequestStartedEvent`, `RequestEndedEvent` | 12 |
| Extended client details typed accessors | 12 |
| Browserless testers for uploads and constraints, JUnit 6 extensions, AI view testing | 13, and every epic's tests inherit them |
| Multi select combo box selection change, value change modes on fields | 03, the lookup fields |

### 25.4 arrivals no epic claims

The brief asks for all of 25.4, so what is not claimed is named, with a decision. An API left out for a reason is a finding; one left out because nobody looked is a gap.

| Arrival | Decision |
| --- | --- |
| `Audio`, `Video`, `Media`, `Source` | Out. Nothing on these screens plays media, and a demo jingle would be the forced use the first section forbids |
| `Slider` | Out. Severity here is four words with fixed meanings, and the design filters them with labelled pills a reader can name in a ticket. A range slider would be vaguer and prettier, which is the wrong trade for a security page |
| OAuth2 and Keycloak additions: `KeycloakOidcUserMapper`, role mapping | Out. The demo authenticates two in memory users, because standing up an identity provider for a hackathon demo proves the provider rather than the application. The security spec names where it would plug in |
| `NativeButton.Type` | Not claimed on its own. If the workbench's `NativeForm` uses a submit button it arrives there for free |
| `CompiledLitTemplate` | Out by hard rule 1 of the source project and this one: no templates |
| Quarkus startup recorder | Out. This is a Spring application |
| Media preload, `IFrame` reload | Out with the media components |

## 25.3 and earlier 25.x feature coverage

25.3 had its own hackathon and this application does not re prove its whole surface. A 25.3 feature appears here only where a real screen needs it, the rest is recorded as inherited, and the two 25.2 arrivals this application leans on are listed with their real vintage.

| 25.3 feature | Epic |
| --- | --- |
| AI orchestrator, message list, input, tool calling, `ToolException`, conversation history | 11 |
| Form AI controller | 11, filling the workbench verdict form |
| Grid AI controller | 11, the advisories grid |
| Switch | 01, the theme toggle, and 08, the show secure toggles |
| Markdown | 05, impact and commentary prose, and 10, the YAML preview |
| Table family | 04, the Spring Boot table, and 05, the coordinates |
| Grid i18n API | 08 |
| ComboBox partial match mode | 03, the version lookup |
| Shared signals, `SharedNodeSignal` | 02, the refresh that reaches every session |
| Service event bus | 12 |
| Dev Loop CLI | Tooling, `specs/10-dev-loop.md` |
| Breadcrumbs, a 25.2 arrival | 01, the app bar |
| Master Detail Layout, a 25.2 arrival | 08, the series list and its open row |
| Date picker metadata, DateTimePicker defaults | Out. Nothing here picks a date. Recorded so nobody claims coverage |
| Kotlin in Copilot | Out. One language in this codebase |

# 08 Testing

## Tiers

| Tier | Suffix | Runs with | For |
| --- | --- | --- | --- |
| Unit | `*Test` | `./mvnw test` | Parsing, triage, version comparison, cache policy, the YAML export |
| Browserless | `*BrowserlessTest` | `./mvnw test` | Every view behaviour that does not need pixels: rendering, navigation, signals, refusals |
| End to end | `*IT` | `./mvnw verify -Pit` | The closed list below |

Browserless wherever possible, a browser only where it must: a download's bytes, a file upload through the real widget, the theme switch before first paint, CSP in a real browser. The browserless additions this release ships are used deliberately: the JUnit 6 `BrowserlessExtension` drives the one test that needs no Spring context while every other view test extends `SpringBrowserlessTest`, the component testers drive uploads and constraints, and the AI view testing support drives the assistant against the mock provider.

## The closed IT list

The browser tier stays small because it is slow and flaky in proportion to its size. It changes only by editing this list and saying why.

| Class | Proves |
| --- | --- |
| `ScanUploadIT` | A real file through the real upload, headline and buckets correct |
| `CopyAnswerIT` | The clipboard write of the composed ticket answer, which only a real browser grants |
| `ScrollActionsIT` | The What it fixes scroll and the `?open` highlight, which are viewport facts |
| `TriageDownloadIT` | The CSV and the PDF arrive with the right type, name and content |
| `ExportsProxyIT` | One proxied per release file streams through with its content type |
| `ThemeFirstPaintIT` | A dark preference paints dark before first paint, no flash |
| `CspIT` | The policy header is enforced and the application still works |
| `AssistantPanelIT` | One exchange against the mock provider in a real browser |
| `DenyByDefaultIT` | With no session, only the sign in and the static assets answer, and every other page and method goes to sign in, D-235, D-258 |

## Determinism

- No network: the data service answers from the fixtures in `src/test/resources/pages/` through the same cache code paths, loaded by a test configuration that points `PagesClient` at the classpath.
- No wall clock: the `Clock` bean is fixed in the test profile, because "scanned today" wording depends on it.
- No live model: cassettes, per `06-ai.md`. Tests that really call one carry the `live-ai` tag, which the pom's `surefire.excludedGroups` keeps out of every default run; CI's live job lifts the exclusion explicitly. A `live-ai` test sits in the unit tier by suffix but is not part of the gate, and this sentence is the rule that makes that honest.
- No `Thread.sleep`: framework waits, or `runPendingSignalsTasks()` after mutating a shared signal from another session.

## Multi user

The refresh signal is the one shared surface: `RefreshBroadcastBrowserlessTest` opens two sessions through `BrowserlessApplicationContext`, moves `generated_at` in the fixture backed service, and asserts both measured chips moved. Close windows, then users, then the context.

## What an assertion has to prove

An assertion proves the rule, not the happy path through it. The named traps from the source project and the 25.3 hackathon, kept because each one once produced a passing test around a broken feature:

- A layout box is not visibility. Assert `checkVisibility`, not `getBoundingClientRect`.
- An empty state passing is not the rule holding: a test of "full slots are refused" must first fill a slot. Positive control before negative conclusion.
- Rule 4 is testable: for one advisory fixture with a missing workaround, assert the workaround row is absent, not empty.
- Every refusal in `04-security.md` has a test that proves it says no.

## Consistency

`SpecConsistencyTest` reads every feature document's test case table and fails the build when a named class is missing. The table is the contract; this test is what makes it one. It reads only rows whose tier cell is one word and the "Verified by" lines of fully ticked stories, so class and method names in prose are not checked. `NoHardcodedStringsTest`, `NetworkBoundaryTest` and `TeamIsRemovableTest` are the other three guard rails. The gate ran 387 test methods on 2026-11-28.

## Natural language test cases

Every feature document carries its test case table with Given, When, Then, a tier and a class name. The table is written at specification time and implemented with the epic, which is how the 25.3 hackathon kept tests from being an afterthought written to pass.

# Demo script

One walk, from a customer's question to a filed verdict draft, in two lengths: five minutes for a room that wants to see 25.4, fifteen for a room that wants to see the application. Everything here works on a laptop with no model key and no network, because it runs on the committed fixtures. The assistant replays recorded answers without a key and says so above the chat, which is itself worth showing.

The walk was made end to end on the fixtures at the close of epic 13, with real clicks in a browser, in development mode, without the model key. The two things it did not do are named where they come: pressing Open in GitHub, which is an act on a real repository, P-17, and asking the live model, P-18.

## Before you start

Serve the fixtures, which stand in for GitHub Pages, and start the application pointed at them:

```
mkdir -p /tmp/pages && ln -sfn "$PWD/src/test/resources/pages" /tmp/pages/cve-status
(cd /tmp/pages && python3 -m http.server 8082 --bind 127.0.0.1) &
env -u OPENAI_API_KEY VAADIN_LAUNCH_BROWSER=false ./mvnw \
  -Dspring-boot.run.arguments="--securitystatus.pages.base-url=http://localhost:8082/cve-status/"
```

The fixtures were published at `generated_at` 2026-11-21T10:13:39Z, so the chip in the bar reads Measured 2026-11-21 and every number below is the same on every run. Without the base URL the application reads the live pages, and the numbers move with them. Sign in as `team` with the password `team` for the internal screens; the customer screens need no sign in.

`/cases` is the map: every state the customer screens can show, each with a release or an advisory that shows it today, and a See one link that opens it. When a step below needs another example, take it from there.

## The data that shows each state

| State | Where | What you see |
| --- | --- | --- |
| Secure | `/version/25.3.5` | Secure |
| Fix available | `/version/24.9.21` | Fix available, upgrade to 24.9.26, 12 findings all gone there, 11 advisories not assessed yet |
| Unresolved, many stuck | `/version/24.4.28` | 25 findings open on the newest of the line |
| Unresolved, one stuck | `/version/24.7.19` | 1 finding open |
| Advisory affects you | `/cve/CVE-2026-45292/24.9.21` | Affects you, through Spring Boot |
| Not assessed yet | `/cve/CVE-2026-102276/24.9.21` | Still being reviewed |
| Discontinued product | `/cve/CVE-2026-8763/24.9.21` | Affects you, only through control-center-starter, which no Vaadin release moves |
| Scanner false positive | `/cve/CVE-2021-33604/24.9.21` | Does not affect you |
| Not reported on this release | `/cve/CVE-2019-25028/24.9.21` | Does not affect you, not reported on 24.9.21 |
| A scanner report | `src/test/resources/scan/osv-scanner.json` on the report tab | One upgrade clears 1 of your 6 open findings: 3 already secure, 1 fix available, 5 unresolved |
| The catalogue | `/advisories` | All 535, with a pill per verdict |
| A release in full | `/findings/release/24.9/24.9.21` | The explorer's card for one release, its advice and its files |

## Five minutes

1. **The customer asks** (`/`). Type `24.9.21` into Your Vaadin version and press Check. The answer is one card: Fix available, upgrade to 24.9.26, all 12 findings gone there, and 11 advisories still being reviewed, so the version stays red until they are. Nothing on the page is wording the data does not carry: a field the data leaves out is not drawn, which `AbsenceSweepBrowserlessTest` walks screen by screen.
   *25.4 to point at: `DisableOnClickMode.UNTIL_RESPONSE` on Check, and the slash key that focuses the lookup from anywhere but a field.*

2. **The customer pastes advisories** (`/cve/CVE-2026-45292,CVE-2026-102276,CVE-2026-8763,CVE-2021-33604,CVE-2019-25028/24.9.21`, or the By CVE tab). Five answers in the order asked, five different states. Copy answer for your ticket puts the composed answer on the clipboard.
   *25.4 to point at: the observed Clipboard API reads the browser's success back to the server.*

3. **The customer uploads the scanner report** (the report tab, Choose a file, `src/test/resources/scan/osv-scanner.json`). The triage lands at `/scan/24.9.21`: what to whitelist, what one upgrade clears, what waits on Spring Boot or the library, with the CSV and the audit PDF to download and the PDF previewed in place. The file is never stored.
   *25.4 to point at: the `Download` API from a button click, the PDF Viewer behind its feature flag.*

4. **The team writes the verdict** (sign in as `team`, `/findings/workbench`). Click the `com.fasterxml.jackson.core:jackson-core` row: the form fills with the package and its five advisories, and the sentence under the YAML says this verdict answers 5 advisories on 8 series. Choose Used, no risk, type `*` in Vaadin versions and one sentence of impact. The YAML follows the typing. Save draft: Last saved by team, and the draft is in the list below for everybody. Export: the form validates the Ready group and enables Open in GitHub, a plain HTML form GET into GitHub's new file editor with the path and the text prefilled.
   *Stop before Open in GitHub unless you are signed in to github.com and mean it, P-17. 25.4 to point at: `NativeForm` and `NativeButton.Type.SUBMIT`, FormLayout side labels, the validation groups.*

## Fifteen minutes

Everything above, plus:

5. **The catalogue** (`/advisories`). The pills filter by verdict with their counts and write `?verdict` to the address. Right click a row: the context menu is built per row by the 25.4 dynamic content provider, copy the id, open the record, ask about it on my version. Above the grid, the ask box takes "critical ones still open on 24.9" and narrows the list; without a key it replays the recorded answer.

6. **The cases** (`/cases`). Three groups: what cannot be rendered because the data does not carry it, what has no live example today, and the 31 renderable cases with today's counts. Click See one on Advisory judged risk: it opens `CVE-2016-1000027` on 23.6.19.

7. **The explorer** (`/findings`). One row per line with its Vaadin and Spring Boot badges and the counts in five groups. Switch a product chip off and the address says what is off. Open 25.3: the line opens beside the list, and below a window of 800 pixels it overlays it. Type a full version in the search to land on its release.
   *25.2 and 25.3 to point at: Master Detail Layout, the Grid i18n texts, web storage for the remembered selection.*

8. **One release, and Spring Boot** (`/findings/release/24.9/24.9.21`, then What Spring Boot brings). The release page is the explorer's card for one release, the advice, and its files. The Spring Boot page groups everything by the Spring Boot release the lines were measured with, and says whether the next patch is free or commercial.

9. **The assistant** (the drawer, any internal screen). Without a key it replays recorded answers and says so. Ask "Is 24.9.21 safe?", "Is CVE-2026-45292 a problem on 24.9.21?" or "What verdict next?": each answer names the tool call it came from. Press Picture to attach a screenshot of a scanner line; on a phone the button asks for the back camera, which nobody has tried on a device yet, P-18.
   *25.4 to point at: `UploadCapture.ENVIRONMENT`, the tool call limit, the AI components' browserless testing.*

10. **Diagnostics** (`/diagnostics`). Every navigation of this walk counted by route and outcome from the 25.4 navigation events, request timings per minute, sessions open now, the reader's browser from the typed extended client details, and the tails of policy violations and assistant exchanges.

## If something goes wrong

| Symptom | What to do |
| --- | --- |
| Every number is different from this script | The application is reading the live pages. Start it with the `securitystatus.pages.base-url` argument above |
| The page says the data is unreachable | The fixture server on 8082 is not running |
| The assistant says it has no recording of the question | Without a key it answers only the recorded questions. Use one of the three in step 9, or start with the key for live answers |
| Open in GitHub stays disabled | Press Export first; any edit after it withdraws it again until the next Export |
| A layout's components are missing in production mode only | A stale local production bundle, draft 23b. The `production` and `it` profiles now always build a fresh one, D-156 |
| Real clicks do nothing right after signing in | Chrome's save password bubble takes the first click. Click once on an empty area |

## What to say about the beta

This application was built on 25.4.0-beta2 from its specifications, and everything it could not do the obvious way is written down in `specs/FEEDBACK-25.4.md` and `specs/FEEDBACK-PLATFORM.md`, each with the workaround and the file it lives in, and reduced to a draft and a reproducer in `specs/issues/`. The three worth saying out loud: the Grid AI controller cannot narrow a grid the application already has, a shared signal changed by another thread before a component is attached never reaches its bindings, and a route added after the production bundle was built ships without its layout's components while the build says no build is needed. `REPORT.md` has the rest.

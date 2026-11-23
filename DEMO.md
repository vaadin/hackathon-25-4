# Demo script

One walk, from a customer's question to a filed verdict draft, in two lengths: five minutes for a room that wants to see 25.4, fifteen for a room that wants to see the application. Everything here works on a laptop with no network, because it runs on the committed fixtures. The assistant needs a model key in production mode: without one a production build does not offer it at all, D-236, while development mode replays recorded answers and says so above the chat.

The walk was made end to end on the fixtures at the close of epic 13, and again on the screens aligned with the prototype at the close of that pass, on 22 November, with real clicks and typing in an isolated browser context, in development mode, without the model key. The two things it did not do are named where they come: pressing Open in GitHub, which is an act on a real repository, P-17, and asking the live model, P-18.

## Before you start

Serve the fixtures, which stand in for GitHub Pages, and start the application pointed at them:

```
mkdir -p /tmp/pages && ln -sfn "$PWD/src/test/resources/pages" /tmp/pages/cve-status
(cd /tmp/pages && python3 -m http.server 8082 --bind 127.0.0.1) &
env -u OPENAI_API_KEY VAADIN_LAUNCH_BROWSER=false ./mvnw \
  -Dspring-boot.run.arguments="--securitystatus.pages.base-url=http://localhost:8082/cve-status/"
```

For the live assistant, keep `OPENAI_API_KEY` in the environment, that is drop `-u OPENAI_API_KEY`, and the `ai` profile turns on by itself; the log says at startup which mode answers. For production mode, add `-Pproduction`, which needs the Vaadin licence. To show the assistant on a phone, open `http://<the laptop's address on the local network>:8080` on the same Wi-Fi.

To run a second instance beside one already on 8080, which holds the drafts file, add `PORT=8090 SECURITYSTATUS_DRAFTS_DIR=<an empty directory>` in front of `./mvnw`.

The fixtures were published at `generated_at` 2026-11-21T10:13:39Z, so the chip in the bar reads Measured 2026-11-21 and every number below is the same on every run; only the relative scan date, "7 days ago", follows the clock. Without the base URL the application reads the live pages, and the numbers move with them. Sign in as `team` with the password `team` for the internal screens; the customer screens need no sign in.

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
| A release in full | `/findings/release/24.9/24.9.21` | The explorer's series row for that release, opened, with its own products panel: "Updating to 24.9.26 removes 18 of these", the Spring Boot paragraph, the files |
| Spring Boot | `/findings/spring` | One shut row per Spring Boot release, 3.5.16 "41 findings arrive with this release, 24 of them a real risk", "free patches ended · 3.5.18 is commercial" |

## Five minutes

1. **The customer asks** (`/`). Every customer page opens with the same H1, Security status, the purpose and the lookup in one centered column. Type `24.9.21` into Your Vaadin version and press Check; if the list of versions is still open over the button, Enter closes it first. The answer opens under the same lookup, the version still in the field, and is one card, tinted, FIX AVAILABLE: Fix available, upgrade to 24.9.26, all 12 findings gone there, and 11 advisories still being reviewed, so the version stays red until they are. Nothing on the page is wording the data does not carry: a field the data leaves out is not drawn, which `AbsenceSweepBrowserlessTest` walks screen by screen.
   *25.4 to point at: `DisableOnClickMode.UNTIL_RESPONSE` on Check, and the slash key that focuses the lookup from anywhere but a field.*

2. **The customer pastes advisories** (`/cve/CVE-2026-45292,CVE-2026-102276,CVE-2026-8763,CVE-2021-33604,CVE-2019-25028/24.9.21`, or the By CVE tab). Five answers in the order asked, five different states: affects you through Spring Boot, not assessed yet, affects you through control-center-starter, does not affect you, not reported on 24.9.21. Copy answer for your ticket puts the composed answer on the clipboard, with the page's address as its last line, D-229; the answer for an advisory asked without a version has no copy, it answers nothing yet. Choosing another tab of the lookup under an answer goes back to the home page with what was typed, D-230. An address typed by hand with an id that is no CVE or GHSA id, `/cve/GHSA-aaaa-bbbb-cccc`, answers that it is not in anything we scanned, D-226.
   *25.4 to point at: the observed Clipboard API reads the browser's success back to the server.*

3. **The customer uploads the scanner report** (Upload scanner report, Choose a file at the end of the card, `src/test/resources/scan/osv-scanner.json`). The card becomes the file's name over "Vaadin 24.9.21 · 9 findings · osv-scanner", with the help mark and Replace file, and the triage lands at `/scan/24.9.21`: "One upgrade clears 1 of your 6 open findings", 3 already secure, 1 fix available, 5 unresolved, what waits on Spring Boot or the library, with the CSV and the audit PDF to download and Preview the audit report to see the PDF in place. The file is never stored.
   *25.4 to point at: the `Download` API from a button click, the PDF Viewer behind its feature flag.*

4. **The team writes the verdict** (sign in as `team`, Verdict workbench in the bar, `/findings/workbench`). The verdict queue is one row per package. Open `com.fasterxml.jackson.core:jackson-core`: what the queue knows of it, the versions it resolves to on each series, the chains it arrives by, the scanners that report it, "Of 5 advisories here, upstream has fixed 5." Open Write a verdict inside it: the six verdicts the package already carries, each with "fill from", one tick per advisory with its severity, series and upstream fix, and the form, prefilled with the package and the five advisories. Choose Used, no risk, type `*` in Vaadin versions, one sentence of impact and one sentence in Why, which Export requires. The YAML follows the typing, and the sentence under it reads "This closes 5 advisories on 8 series". Save draft: Last saved by team, and the draft is in the list below for everybody. Export: the form validates the Ready group and enables Open in GitHub, a plain HTML form GET into GitHub's new file editor with the path `data/assessments/maven-com-fasterxml-jackson-core-jackson-core-<date>.yaml` and the text prefilled.
   *Stop before Open in GitHub unless you are signed in to github.com and mean it, P-17. 25.4 to point at: `NativeForm` and `NativeButton.Type.SUBMIT`, FormLayout side labels, the validation groups.*

## Fifteen minutes

Everything above, plus:

5. **The catalogue** (`/advisories`). "Every advisory we have measured", 535 in all. The pills, affects you 150 first and not on a current release 122 last, filter by verdict and write `?verdict` to the address. Right click a row: the context menu is built per row by the 25.4 dynamic content provider, copy the id, open the record, and Ask about it, on my version once a version was given, which opens that answer, D-230. Signed in as the team, the ask box between the pills and the list takes "critical ones still open on 24.9" and narrows the list; without a key it replays the recorded answer.

6. **The cases** (`/cases`, Every case). Three groups in the prototype's words: what the design asks for and the measurements cannot give, 5 of them; cases that do not occur today; and the 31 cases you can go and look at, the most frequent first. Click See one on Advisory judged risk: it opens `CVE-2016-1000027` on 23.6.19.

7. **The explorer** (`/findings`, Findings in the bar). The left panel holds the filter and the products with what each brings, `vaadin` 27, `vaadin-spring-boot-starter` 259, `control-center-starter` off by default. One row per series with its Vaadin and Spring Boot badges, its sentence and a ring per product. Switch `kubernetes-kit-starter` off and the address says what is off. Open 25.3: it opens in place under its row, `?open=25.3`, in two halves, From Vaadin and With Spring Boot, then the files and the other measured releases. The round "?" opens the legend. Type a full version, `24.9.21`, in the filter and press Enter to land on its release. Below a window of 800 pixels the left panel overlays the content.
   *25.2 to 25.4 to point at: the App Layout's drawer filled per route, the Grid i18n texts, web storage for the remembered selection.*

8. **One release, and Spring Boot** (`/findings/release/24.9/24.9.21`, then What Spring Boot brings in the bar). The release page is the explorer's row for that one release, opened, with its own left panel counting the products on it: "Updating to 24.9.26 removes 18 of these", the Spring Boot paragraph, the files, the two halves. The Spring Boot page has one shut row per Spring Boot release the lines were measured with, saying how many findings arrive with it and whether free patches have ended; open 3.5.16 for the supply table, the advice and the groups.

9. **The assistant** (Assistant in the bar of any internal screen opens the panel on the right). With a key the live model answers; in development mode without one it replays recorded answers and says so. Ask "Is 24.9.21 safe?", "Is CVE-2026-45292 a problem on 24.9.21?" or "What verdict next?": each answer names the tool call it came from. Press Picture to attach a screenshot of a scanner line; on a phone the button asks for the back camera, P-18.
   *25.4 to point at: `UploadCapture.ENVIRONMENT`, the tool call limit, the AI components' browserless testing.*

10. **Diagnostics** (`/diagnostics`). A board of six cards, D-232: every navigation of this walk counted by route, completed, refused or broke, from the 25.4 navigation events, request timings per minute, sessions open now, the reader's browser from the typed extended client details, policy violations and assistant exchanges. The pencil turns on editing: drag a card, resize it, remove it, add it back with the plus, restore the default six; the browser remembers the arrangement. The line above the board says how long ago counting started.
   *25.4 to point at: the Dashboard with its i18n, `RequestEndedEvent`, and the unlocked effect it led to, vaadin/flow#27187.*

## If something goes wrong

| Symptom | What to do |
| --- | --- |
| Every number is different from this script | The application is reading the live pages. Start it with the `securitystatus.pages.base-url` argument above |
| The page says the data is unreachable | The fixture server on 8082 is not running |
| There is no Assistant button in production mode | No model key in the environment: a production build without a model does not offer the assistant, D-236. Start it with the key |
| The assistant says it has no recording of the question | Without a key it answers only the recorded questions. Use one of the three in step 9, or start with the key for live answers |
| Open in GitHub stays disabled | Press Export first; any edit after it withdraws it again until the next Export. "Required to export" under Why means the commentary is empty, which the cve-status writer refuses, D-223 |
| Check does nothing on the version field | The list of measured versions is open over the button. Press Enter or click elsewhere first |
| A layout's components are missing in production mode only | A stale local production bundle, draft 23b. The `production` and `it` profiles now always build a fresh one, D-156 |
| Real clicks do nothing right after signing in | Chrome's save password bubble takes the first click. Click once on an empty area |

## What to say about the beta

This application was built on 25.4.0-beta2 from its specifications, and everything it could not do the obvious way is written down in `specs/FEEDBACK-25.4.md` and `specs/FEEDBACK-PLATFORM.md`, each with the workaround and the file it lives in, and reduced to a draft and a reproducer in `specs/issues/`. The three worth saying out loud: the Grid AI controller cannot narrow a grid the application already has, a shared signal changed by another thread before a component is attached never reaches its bindings, and a route added after the production bundle was built ships without its layout's components while the build says no build is needed. `REPORT.md` has the rest.

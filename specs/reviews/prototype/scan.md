# Prototype comparison: the scanner report triage

Compared on 2026-11-22, both sides reading the same published data, `generated_at` 2026-11-22T05:30:11Z: the prototype at https://vaadin.github.io/cve-status/ signed in through its client side gate with the customer account the person gave, and this application in development mode on live data, without the model key, on its own port. Both at 1920 by 1080 and at 390 by 844. Screenshots: `prototype-scan.png` and `prototype-scan-390.png` for the prototype, `app-scan-before.png` and `app-scan-390-before.png` for the application before this change, `app-scan.png` and `app-scan-390.png` after it, all with `osv-scanner.json`.

How the prototype routes: the report tab navigates to `#scan`, which draws the lookup and nothing under it; a file read in the browser moves to `#scan/<version>` when the file names its release and stays on `#scan` when it does not; "Answer for this version" moves to `#scan/<version>`; the secure rows are `#scan/<version>?secure=1`. The application's routes are the same, `/scan`, `/scan/<version>`, `?secure`; its report tab does not navigate, D-39, confirmed by D-165.

The same files were put to both sides through the file chooser, the application's hand written fixtures under `src/test/resources/scan/`: `osv-scanner.json`, `dependency-check.json`, `bomber.json`, `cyclonedx.json` (one scan of 24.9.21 in the four formats), `cyclonedx-components-only.json`, `cyclonedx-unmeasured.json` (24.9.99), `bomber-no-version.json`, `dependency-check-skipped.json`, `osv-scanner-empty.json`, `not-a-report.json` and `not-json.txt`; the files that name no release were answered for 24.9.21, and `bomber-no-version.json` was answered again for 24.4.28, 24.7.19, 25.3.5, 23.6.19, 14.14.9, 24.10.14 and 24.9.15 (a release never measured). For each, the whole text of the page with and without the secure rows, the help dialog and the triage download were taken from both sides and compared line by line; downloads were fetched, never clicked through to the browser. Past the shell the two agree word for word except where a row below says otherwise.

The comparison was written before anything was changed. The Action column is one of: move, add, transcribe wording, keep as app extra, data not published. A row whose two sides already agree says so.

## The report tab

| Prototype | App before | Action |
| --- | --- | --- |
| Before a file: one card, "Drop your report here, or choose a file", the line "CycloneDX, OWASP Dependency-Check, osv-scanner or bomber exports. Read in this browser.", the help mark, "Choose a file" | The drop zone with its title and the button, the formats line beside it, the help mark on a line of its own | Move: one row, the title and the line, then the help mark and the button, D-199. "Read in this browser." is not transcribed: here the file is read on the server and not kept, D-67, so the app's own sentences say that instead |
| With a file: the file's name as the title, "Vaadin 24.9.21 · 9 findings · osv-scanner" under it, the help mark, "Replace file" | "Drop your report here, or choose a file", Replace file, then "osv-scanner.json · Vaadin 24.9.21 · 9 findings · osv-scanner" | Move: the name as the title and the line without it, D-199 |
| No release in the file: "Your Vaadin version" and "Answer for this version" on one row above the card, and under the card "This report lists only the packages that carry a finding, so it does not name the Vaadin release it was taken from. Enter it and every row becomes an answer." | The field and the button under the card; "Your report names no Vaadin release. Enter yours above and every row becomes an answer for it." in the answer below | Move and transcribe: the row above the card and the sentence under it, D-199. Ours stays for a bill of materials, of which the prototype's sentence would be false |
| After an answer the field and its button are gone | They stay | Keep as app extra: the reader can change the version without uploading again, D-199 |
| A file that is not a report: "That file is not an osv-scanner, OWASP Dependency-Check, bomber or CycloneDX report. Those four are what this page can read." | "... a CycloneDX, OWASP Dependency-Check, osv-scanner or bomber report. Those four are what this page reads." | Transcribe "can read", D-200. The order stays the parser's, the one source of the formats line and the help, rule 6.11.21 |
| Not JSON: "That file is not JSON. The XML forms of these reports are not read." | "That file is not JSON, and the XML and HTML forms of these reports are not read." and the four formats | Transcribe the prototype's two sentences; the four formats named after them stay, feature 06's edge case, D-200 |
| The chooser offers `.json` files only | Any file name, read by its shape | Keep as app extra, D-76, D-201 |
| The help: what is assessed, with "Those are the rows above that carry a verdict."; the file counted, "The remaining 1 coordinate is not"; four recipes, CycloneDX with a second command for the frontend half; the closing JSON paragraph | The same sections, our wording of the same sentences, the frontend command inside a sentence, and "Your file is not kept" as the second section | Transcribe the prototype's sentences and its frontend command, D-200. "Your file is not kept" stays and moves last, D-199. The recipes keep the parser's order, rule 6.11.21, where the prototype lists osv-scanner second |
| The help counts a CycloneDX file's components by their bom-ref: 19 dependencies, "billing, vaadin" among the unknown | 10 dependencies, the unknown named by their artifact | Keep: the application resolves the references, D-70 |

## The answer

| Prototype | App before | Action |
| --- | --- | --- |
| No title: the headline is the first heading under the lookup, an H2 | H2 "Scanner report", then the headline as another H2 | Keep the title as app extra, D-169; the headline moves to H3 under it, D-202 |
| "One upgrade clears 1 of your 6 open findings" | The same | None, already equal |
| "Upgrade to 24.9.26, a maintenance release in the minor you already use. The remaining 5 need a Spring Boot change or are waiting on a fix." | "... need a Spring Boot change, a fix of your own, or are waiting on a fix." | Transcribe, D-200 |
| Nothing left after the upgrade: "The remaining 0 need a Spring Boot change or are waiting on a fix." | The sentence left out | Keep as app extra, D-201 |
| No finding on the version: "Nothing in this report affects your version" | "Nothing in this report affects 24.9.21" | Transcribe, D-200 |
| An empty report: "Nothing in this report affects your version" | "This report carries no findings" | Keep as app extra, D-201 |
| Three buckets tinted by state with a border all round, the count in the state's color | A white card with a colored start border | Move: tinted, D-202 |
| Bucket details: "Not affected or already fixed in 24.9.21. Safe to whitelist.", "All fixed by upgrading to 24.9.26." | The same | None, already equal |
| A detail over a zero count: "All fixed by upgrading to 24.9.26.", "Cleared by one upgrade.", "Still needs attention." | Left out | Keep as app extra: a sentence over nothing, D-201 |
| Unresolved: "2 awaiting a vendor release, 1 waiting on a fix, 1 not assessed yet, 1 not ours to answer." | "2 awaiting a Spring Boot change, 1 awaiting the library, 1 not assessed yet, 1 not ours to answer" | Transcribe, with the full stop, D-200. On 24.4.28 the prototype counts the library's row and Vaadin's under "2 waiting on a fix"; the app keeps "1 awaiting a Vaadin fix" apart: keep as app extra, D-201 |
| "Download triage for your audit", "View 24.9.26 release notes" | The download, "Preview the audit report", the release notes | Keep the preview as app extra, moved after the release notes, D-201 |
| A version never measured, file named: "Your report was taken from Vaadin 24.9.99 and we have no scan of that release. The rows below say what each advisory is, and not what it means for you. The newest release of that line we have measured is 24.9.26, which is not your release: whatever moved between the two is in none of this." One paragraph | "We have no scan of Vaadin 24.9.99. ..." and "The newest release of 24.9 we have measured is" in a second paragraph, with the Not measured chip | Transcribe and move to one paragraph; "We have no scan of Vaadin X." stays when the reader typed the version, as the prototype says it for 24.9.15, D-200. The chip stays: keep as app extra |
| A version never measured: no download | Both files, named for the asked version | Keep as app extra, D-74, D-201 |

## The table

| Prototype | App before | Action |
| --- | --- | --- |
| "Open findings", and at its right "Show the 3 secure findings", which reads "Hide the 3 secure findings" once they are shown | The same heading, a Switch whose label stays "Show" | Transcribe "Hide", D-200; the Switch stays, keep as app extra |
| Columns CVE, Component, Owner, Verdict, Action; rows in the report's order; no severity | The same | None, already equal. This answers P-13: no severity column on screen and the report's order |
| The component as the short name with the coordinate as its tooltip | The same | None, already equal |
| A secure row: "Already secure" alone | "Already secure" and the catalogue's word, "scanner false positive", "used, no risk", and on a row the version does not report, often "affects you" | Move: the word only where the verdict settled the row, ruled out or build only, D-198. Kept as app extra there, AC2 and D-73 |
| "Reported on a coordinate Vaadin brings, and not on a release we measure" | "... and on no release we measure" | Transcribe, D-200 |
| "Not on anything Vaadin ships" | "Not on anything Vaadin ships: billing-core is yours to assess" | Keep as app extra: rule 6.11.20, the action names who moves, D-72, D-201 |
| "Awaiting a fix" | "Awaiting a fix from Vaadin" | Keep as app extra, the same reason |
| Discontinued: "Arrives only through a product Vaadin no longer develops; its page says the override to apply" | "...: override the version in your build" | Keep as app extra: the advisory's page carries no workaround, D-188, so it says no override, D-201 |
| No version: "Name your Vaadin version above and this becomes an answer" | The same | None, already equal |
| A version never measured: every row "Unresolved" and "Name your Vaadin version above and this becomes an answer" | The catalogue's word and "We have no scan of 24.9.99: the verdict shown is the one the advisory has across the releases we measure" | Keep as app extra: the reader did name a version, D-74 |
| CycloneDX: the file's own findings named by their bom-ref, "otel", "bcprov", "override its otel version", CVE-2099-0002 on `flow-server` called the reader's own, and each supplied advisory listed again: 55, 2 and 21 | Each advisory once on its resolved coordinate: 52, 1 and 18 | Keep: the application's count is the one the file supports, D-70, D-201 |
| No rows: the header row alone | "Nothing to list." | Keep as app extra, D-201 |
| Skipped entries: "2 entries in your report carried findings with no coordinate, so they could not be matched: legacy-tools.zip, bundled-js/app.min.js." | "... could not be matched. Named in the file: ..." | Transcribe, D-200 |
| Nothing at the foot | "Your report was read when it uploaded and the file was not stored. ..." | Keep as app extra, D-67 |

## The downloads

| Prototype | App before | Action |
| --- | --- | --- |
| `vaadin-security-triage-24.9.21.csv`, `text/csv` | The same name and type | None, already equal |
| Three comment lines: "# Vaadin security status, triage of an uploaded report", "# Vaadin version: 24.9.21", "# Measurements of: 2026-11-22T05:30:11Z" | One line, "# Triage of an uploaded scanner report against Vaadin 24.9.21, from the measurements generated at ..." | Transcribe, D-197 |
| Columns `advisory,bucket,owner,coordinate,severity,answer` | CVE, Component, Owner, Verdict, Action, Bucket | Move to the prototype's order and add Severity, which the uploaded file carries, the scanner's own word lower case, "moderate" from osv-scanner, "medium" from Dependency-Check. The Verdict column stays last, keep as app extra. Header words and bucket values stay words, D-197 |
| No PDF | The audit PDF, previewed in the PDF Viewer | Keep as app extra; it carries the same seven columns, D-197 |

## At 390 pixels

Neither side scrolls sideways, `scrollWidth` 390 on both. The prototype stacks the buckets and lets its table run past the edge; the application stacks the buckets, wraps the card's row and scrolls the table inside its section. The application's behaviour at that width is kept.

## Data

Nothing the prototype shows on this screen is absent from what the application reads: the severity of the CSV comes from the uploaded file, and from the index for an advisory the measurements supply. No data need row.

## Verified after the change

In development mode on live data, in a tab of an isolated Playwright context, with Playwright's own clicks and its file chooser: the eleven files and the seven versions were put again and their whole text compared with the prototype's; what differs is a row above, nothing else. The card reads the file's name over "Vaadin 24.9.21 · 9 findings · osv-scanner" with the help mark and Replace file at its right; with `bomber-no-version.json` the field and Answer for this version sit on a row above the card and the prototype's sentence under it, gone once 24.9.21 is answered. The Switch, clicked, reads "Hide the 3 secure findings". The CSV fetched from the download's address opens with the three comment lines and the seven columns, "moderate" for CVE-2026-2742 as in the prototype. The PDF fetched from the preview's download is `application/pdf`. At 390 by 844 `scrollWidth` is 390.

# Prototype comparison: the answer by CVE

Compared on 2026-11-22, both sides reading the same published data, `generated_at` of that day: the prototype at https://vaadin.github.io/cve-status/ signed in through its client side gate with the customer account the person gave, asked through its hash route `#cve/<ids>` and `#cve/<ids>/<version>`, and this application in development mode on live data, without the model key, asked through `/cve/<ids>` and `/cve/<ids>/<version>`. Both at 1920 by 1080 and at 390 by 844. Screenshots: `prototype-cve.png` and `prototype-cve-390.png` (CVE-2026-89407 on 24.9.21), `prototype-cve-theirs.png` (CVE-2026-47886 on 24.9.21, the case of D-65) for the prototype, `app-cve-before.png` and `app-cve-390-before.png` for the application before this change, `app-cve.png` and `app-cve-390.png` after it.

The same questions were put to both, 23 of them: one id in each state the data shows on 24.9.21, CVE-2026-89407 (fixed in the upgrade), CVE-2026-47886 (`theirs_risk`, a risk verdict whose every route is the reader's Spring Boot), CVE-2026-45292 (`spring_pinned`), CVE-2026-8763 (a discontinued product), CVE-2026-102276 (not assessed yet), CVE-2025-67030 (`from_build`), CVE-2026-41707 (`theirs`, judged false positive); CVE-2016-1000027 on 23.6.19 (`theirs_risk` again), CVE-2026-29145 on 24.7.19 (configuration), CVE-2026-89407 on 24.10.14 (affects you, no fix on the line), on 25.3.5 (not reported on a line that reported it) and on 24.4.28 (not reported on a line that never did), CVE-2024-38821 on 24.4.14 (in triage on no current release); CVE-2026-89407 with no version; CVE-2099-99999 with and without a version (unknown); GHSA-gr4j-r575-g665 alone and on 14.14.9 (a GHSA with no CVE id); GHSA-p6pp-m3f8-5c89 alone and beside its CVE (an alias of CVE-2026-89407); three ids at once on 24.9.21, two ids with no version, and CVE-2026-89407 on 24.9.15, a release never measured. The whole text of each answer, with every layer of the details opened on seven of them, was taken from both sides and compared line by line. Past the shell the two agree word for word except where a row below says otherwise.

The comparison was written before anything was changed. The Action column is one of: move, add, transcribe wording, keep as app extra, data not published. A row whose two sides already agree says so.

## Above the answer

| Prototype | App before | Action |
| --- | --- | --- |
| The shell's H1 "Security status", the purpose and the lookup on its By CVE tab with the ids and the version in the fields | The same, `home.md` | None, already equal |
| No title of the answer: the verdict card follows the lookup | H2 with the ids asked, the view's title | Keep as app extra, D-169. The headline and the sections move one level down, H3, as on the version screen, D-186 |
| Several ids: "3 advisories" with the aside "Answered for 24.9.21", or "Enter your version above to answer for it" | The same words | None, already equal |
| A version never measured, 24.9.15: each card the measured card, with the sentence "Enter your version above and this answers for it" although a version was entered | One note above the cards, "We have not measured Vaadin 24.9.15 ...", the link to answer for 24.9.26 instead, and the measured card without the sentence asking for a version | Keep as app extra: the note and the link are feature 05's edge case, and asking for a version the reader already gave says something false |

## The verdict card

| Prototype | App before | Action |
| --- | --- | --- |
| A card tinted by state with a border all round; the state word in capitals, "FIX AVAILABLE", "AFFECTS YOU", "DOES NOT AFFECT YOU", "FIXED FOR YOU", "MEASURED", "NOT IN OUR MEASUREMENTS" | A white card with a thick colored start border, the same words in sentence case | Move: the card tinted and the word in capitals through CSS, as on the version screen, D-186 |
| Headline as an H2: "CVE-2026-89407 is fixed in 24.9.26" | The same words, an H2 sibling of the title | Move: H3, D-186 |
| `theirs_risk`, CVE-2026-47886 on 24.9.21 and CVE-2016-1000027 on 23.6.19: "DOES NOT AFFECT YOU", "X does not affect V", the why is the impact alone, a green card | "Affects you", "X affects V through Spring Boot", the impact and the sentence "Every route to it comes from the Spring Boot your application declares ...", a red card, D-65 | Move: the prototype's state, word and headline, D-187 supersedes D-65. The Spring Boot sentence stays after the impact: keep as app extra, it says where the verdict lives |
| `spring_pinned`, CVE-2026-45292: "AFFECTS YOU", "affects 24.9.21 through Spring Boot", the pinned sentence | The same | None, already equal |
| Discontinued, CVE-2026-8763: why "It is reported on your version and the verdict calls it real. It reaches your application only through control-center-starter, which Vaadin no longer develops, so no Vaadin release moves it. If you use it, the workaround below is the exit." | The middle sentence only; the last one only when a verdict carries a workaround, which this one does not | Transcribe the first sentence. The last stays conditional: keep as app extra, the prototype's layer below shows the workaround as missing, so "the workaround below" points at nothing, D-188 |
| Not affected with no impact sentence, CVE-2024-38821 on 24.4.14: why "We measured this one and it does not reach a Vaadin application on your version." | No why at all | Transcribe, D-188 |
| `from_build`, CVE-2025-67030: why is the impact alone | "It is in the build classpath only, not in what you deploy." before the impact | Keep as app extra: the sentence says what `from_build` means, P-09 |
| Not reported on a line that never reported it, CVE-2026-89407 on 24.4.28: "FIXED FOR YOU" | "Does not affect you", D-66 | Keep as app extra: "fixed" claims there was something to fix on that line, which the advisory's own coverage contradicts. D-66 confirmed, D-189 |
| Not reported on a line that did report it, 25.3.5: "FIXED FOR YOU" | The same | None, already equal |
| Unknown id, CVE-2099-99999: "NOT IN OUR MEASUREMENTS", "is not in anything we scanned", the sentence | The same when GitHub Pages answers 404; the whole page's outage notice when it answers 503, which it did for that path in four of six requests on the day | Keep the words. The 503 is retried once before it counts as an outage, D-190 |
| A GHSA alias asked alone, GHSA-p6pp-m3f8-5c89: "is not in anything we scanned", which is false for an alias of a tracked CVE | "We do not track GHSA-p6pp-m3f8-5c89 under that id" and the sentence to look up its CVE id, D-62 | Keep as app extra: D-62 confirmed, D-189. The alias map stays a data need |
| The alias beside its CVE: two cards, the second "not in anything we scanned" | One card, the alias named under it as "You asked for it as ..." | Keep as app extra, D-62 |
| An address whose ids do not parse, `#cve/GHSA-aaaa-bbbb-cccc` (letters a GHSA never uses): "not in anything we scanned" | The H2 with what was typed and nothing under it | Not changed: the lookup refuses such an id with "Not recognised", D-48, so only a hand typed address reaches this. Left for the person, noted in `NEXT.md` |
| Actions: "View 24.9.26 release notes", "Copy answer for your ticket", scan date at the right | The same | None for the order. The prototype's copy action opens "Not built yet ... nothing composes that text"; the application composes it from the card's own words, D-63. Keep as app extra |

## Across supported versions

| Prototype | App before | Action |
| --- | --- | --- |
| H2 "Across supported versions" with the aside "Your version is 24.9.21" or "Enter your version above" | The same, an H2 | Move: H3, D-186 |
| Cards four to a row in the column, each tinted by state with a border all round, the minor large; the reader's card outlined, "24.9 · yours" | Six to a row, a colored start border, the reader's card on a grey ground | Move: four to a row, tinted, the reader's card outlined, D-186 |
| The words of every card: Fixed, Affects you, Never affected, Not affected, Measured on X, Newest is X, Nothing to do | The same words on every card compared | None, already equal |
| The reader's card when the upgrade clears it: "Affects you", "In 24.9.26 and later" | "Fix available", "In 24.9.26 and later", D-64 | Keep as app extra: D-64 confirmed, the card beside the headline says the headline's answer, D-189 |

## Details

| Prototype | App before | Action |
| --- | --- | --- |
| H2 "Details", or "Details for X" when several ids were asked | The same, an H2 | Move: H3, D-186 |
| Three layers, the first open | The same | None, already equal |
| Layer one: "Can your deployed application be exploited?" with the impact; "Vaadin's assessment" with the catalogue's label as published, "affects you", "used, no risk", "not on a current release"; "Source", "Comes from Vaadin · package", "not established yet · package" | The same pairs, the label and the origin capitalised, "Affects you", "Not established yet"; for `not_current` our sentence "No release we follow reports it any more" | Transcribe: the label and the origin as published, and the catalogue's own label for `not_current`, D-188 |
| Layer one with no impact on the version: "affects you. Enter your version above for the answer on it.", "affects you. Not reported on 24.9.21.", "not assessed yet. It is reported on 24.9.21 and nobody has judged it yet." | The row left out, or "It is reported on 24.9.21 and nobody has judged it yet." alone | Keep as app extra: the label is already the next row, and "Not reported on 24.9.21" under a card that says it reaches the reader contradicts it, D-185 |
| Layer two: "The dependency in the box" as a sentence, "com.fasterxml.jackson.core:jackson-core 2.21.4, as we scanned the newest 24.9 release. The library fixes it in 2.21.7." | A table Line, Package, As we scanned the newest release of the line | Transcribe: one sentence per package when a version was asked, the table kept for the measured card, where every line is listed, D-188 |
| Layer two, missing fields: `assessments[].workaround` and `assessments[].fix_expected` printed as missing | Left out | Keep as app extra, rule 4, D-185 |
| Layer two, several verdicts: "This advisory reaches Vaadin through 2 packages and carries a verdict for each. The others are below." and one "Through <package>" block each | The same sentence without "The others are below", then a table, coordinate with the differing part marked, verdict, what it means | Keep as app extra: rule 6.4.9 marks the difference, which the table does |
| Layer three: "Severity high, as the advisory databases rate the library." | "Severity High, ..." | Transcribe: the severity as the data writes it, D-188 |
| Layer three on a version: the commentary of the reader's verdict, "The version is your Spring Boot's: 3.5.16, the newest 3.5 release, pins jackson-core 2.21.4 ..." | Absent: the screen read the commentary from the version document's copy of the verdict, which carries none; the advisory document's copy of the same verdict does | Add: the commentary read from the advisory document by the verdict's id, D-196. Found by the opened layers' comparison after the first change |
| Layer three when the reader's version has no verdict for the advisory, CVE-2026-8763 on 24.9.21: the prototype marks "no verdict for your version" and still prints a commentary from the advisory document, written for "a Vaadin 24.7 application" | No commentary | Keep as app extra: a verdict written for another line is not the reader's, rule 4 |
| Layer three: the summary, the commentary, then one paragraph "Reported by our scans on 24.9.0 to 24.9.26, 26 releases, of the 24.9 line. The verdict is written for Vaadin >=24.9,<24.10." | The same, the range and the scope in two paragraphs | Move: one paragraph, D-188 |
| "First seen by our scans 2026-11-03. Published" and the publication date marked missing | "First seen by our scans 2026-11-03." | Data not published: the same gap as D-179, the row in `specs/FEEDBACK-25.4.md` |
| "Read the record for CVE-2026-89407. Your scanner may print it as GHSA-p6pp-m3f8-5c89." | The same without the full stop after the link | Transcribe the full stop, D-188 |

## At 390 pixels

Neither side scrolls sideways at 390, `scrollWidth` 390 on both. The prototype stacks the line cards one or two to a row; the application does the same with its grid, and turns the coordinate tables into stacked blocks. The application's behaviour at that width is kept.

## Verified after the change

In development mode on live data, in a tab of an isolated Playwright context: the 19 questions of the first pass were asked again and their whole text compared with the prototype's; what differs is a row above, nothing else. CVE-2026-89407 on 24.9.21 reads H2 the id, H3 the headline, Across supported versions and Details; the card is tinted with FIX AVAILABLE in capitals, the line cards sit four to a row, tinted, 24.9 outlined. CVE-2026-47886 on 24.9.21 reads DOES NOT AFFECT YOU on a green card with the Spring Boot sentence after the impact. CVE-2099-99999 answers "not in anything we scanned" again, with or without a version. With real clicks: Affected modules and the fix and The advisory, and the evidence opened on CVE-2026-89407 on 24.9.21 show the box sentence, the commentary and "Read the record for CVE-2026-89407.". The version screen's card was read after the change: still tinted, capitals and H3, so widening the scoped rules to `.cve-view` left it as it was. At 390 by 844 `scrollWidth` is 390.

On 2026-11-28 the person compared this page with the prototype again on live data, CVE-2026-45292: it takes the prototype's look with Vaadin components, D-271, and its heading above the answer is visually hidden, D-270.

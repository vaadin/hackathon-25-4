# Prototype comparison: the answer by version

Compared on 2026-11-22, both sides reading the same published data, `generated_at` of that day: the prototype at https://vaadin.github.io/cve-status/ signed in through its client side gate with the customer account the person gave, asked through its hash route `#version/<v>`, and this application in development mode on live data, without the model key, asked through `/version/<v>`. Both at 1920 by 1080 and at 390 by 844. Screenshots: `prototype-version.png` (24.7.19, whole page), `prototype-version-fix.png` (24.9.21, first screen) and `prototype-version-390.png` for the prototype, `app-version-before.png` and `app-version-390-before.png` for the application before this change, `app-version.png` and `app-version-390.png` after it.

The same questions were put to both: 24.7.19 (unresolved, one finding with a workaround), 24.4.28 (unresolved, 25 findings), 14.14.5 (unresolved with an upgrade that clears part), 24.9.21 (fix available, with findings not assessed yet), 7.7.30 (fix available), 25.3.5 (secure), 25.4.0-beta2 (a measured prerelease), 24.9.15 (a release of a followed line that was never scanned), 24.8.3 and 25.0.4 (a line nobody follows inside a followed major), 6.0.0 (below every line), 99.1.0 (no line at all), and the flags `ruled`, `disc`, `boot` and `open`. The whole text of each answer was taken from both sides and compared line by line: past the shell, the two agree word for word except where a row below says otherwise.

The comparison was written before anything was changed. The Action column is one of: move, add, transcribe wording, keep as app extra, data not published. A row whose two sides already agree says so.

## Above the answer

| Prototype | App before | Action |
| --- | --- | --- |
| The shell's H1 "Security status", the purpose and the lookup with the asked version in the field | The same, `home.md` | None, already equal |
| The lookup's tip "Tip: link your project and we'll remember your version and tell you when its status changes." | Absent, D-166 | None, D-166 stands |
| No title of the answer: the verdict card follows the lookup | H2 "Vaadin 24.7.19", the view's title, between the lookup and the card | Keep as app extra, D-169. The headings under it move one level down so the page reads H1, H2, H3 in order, D-174 |

## The verdict card

| Prototype | App before | Action |
| --- | --- | --- |
| A card tinted by state, light red for unresolved, with a red border all round | A white card with a thick colored start border | Move: tint the card by state as the home cards are, D-175 |
| The state mark and the state word in capitals, "UNRESOLVED", "FIX AVAILABLE", "SECURE", "NOT MEASURED" | The same icon and word, in sentence case | Move: the word in capitals through CSS, so the bundle keeps one word per state, D-175 |
| Headline as an H2: "1 vulnerability has no fix yet", "25 vulnerabilities have no fix yet", "Upgrade to 24.9.26", "Vaadin 25.3.5 is secure" | The same words, as an H2 | Move: the headline becomes an H3 under the view's H2, D-174 |
| The why paragraph | The same words on every version compared | None, already equal |
| Badges: "Maintenance release", "Same minor: 24.9" on a same line upgrade, the tier label, "Free maintenance ended 2026-06-16" | The same badges with the same words | None, already equal |
| A badge "No API changes" beside Same minor on a same line upgrade, marked by the prototype itself as something nothing measures | Absent | Data not published: the prototype's own note says no measurement carries it. Not added, D-178 |
| "Prerelease" badge on 25.4.0-beta2 | The app has it, the prototype does not | Keep as app extra: the version is a prerelease by its own number |
| The single unfixed finding drawn inside the card: id link, severity, owner chip, "Vulnerable, workaround available", the two columns Do this now and Fix | The same | None, already equal |
| Owner chip "Vaadin issue" on findings whose `fix_owner` is `unknown`, as on `vaadin` ones | No chip for `unknown`, D-51 | None: D-51 stands against the prototype, rule 4, D-180 |
| Actions on a fix available card: "View 24.9.26 release notes", "What it fixes" | The same | None, already equal. D-52 confirmed: no release notes on a secure card on either side, D-181 |
| Actions on a red card: "Notify me when it is fixed", "Or move to 24.10.14, which is secure, since no 24.7 release is" | The same words | None, already equal |
| The Notify me dialog: "Not built yet"; "The design asks for "Notify me when it is fixed" and "link your project"."; why, about a page of static files; needs, the page living in My account on vaadin.com | The same title and first sentence without the project link, and why and needs sentences of ours, D-55 | None: the prototype's why and needs describe a static page this application is not, and the project link is not here, D-166. D-55 confirmed, D-183 |
| Scan line "Scanned 2026-11-22" at the right end of the actions | "Scanned 2026-11-22, today", at the right end, wrapping under the actions when they are long | Keep as app extra: the relative words are D-59, confirmed, D-184 |

## The sections, in order

The order of the sections is the same on both sides for every version compared: findings with no fix yet, the not assessed notice, what the upgrade fixes, Spring Boot, discontinued, details for a single finding, ruled out, all supported versions.

| Prototype | App before | Action |
| --- | --- | --- |
| Section headings as H2, a right aligned aside on the same line | The same, H2 | Move: H3 under the view's H2, D-174 |
| "Findings with no fix yet" with "25 findings", then one bordered card per finding, the first with its first two layers open; with `?open=<id>` that finding's first two layers open and the first's closed | The same, the same open layers on arrival with and without `?open` | None, already equal. D-56 confirmed, D-181 |
| Inside a finding with no workaround or fix text: the field names `assessments[].workaround` and `assessments[].fix_expected` shown as missing; "Tracking: no pull request or issue recorded for this one" | The column, the row or the pair is left out | Keep as app extra: rule 4, the absent field is omitted, D-185 |
| Not assessed yet: a chip, the sentence, the ids as links separated by commas | The same chip and sentence, the ids as links side by side with no separator | Transcribe: the ids separated by commas, D-177 |
| On that notice, "Published" followed by the advisory publication date, marked by the prototype as not carried by the measurements | Absent | Data not published, a data need row in `specs/FEEDBACK-25.4.md`, D-179 |
| "What 24.9.26 fixes for you" with "12 of the 12 findings on 24.9.21", then rows: id, severity, owner chip, package and impact, "Fixed in X" or "Gone by X", and a chevron | The same rows and words without the chevron | Add the decorative chevron, as on the home page's recent rows, D-177 |
| Spring Boot findings: aside "106 findings · Hide them", the tip with `<tomcat.version>` in code type, the table Dependency, CVE, How to fix, "94 more arrive the same way ..." | The same words and table; the property in plain type | Move: the property in a `code` element, D-176 |
| Spring Boot note shape on a version with no unfixed finding: the note with a Show link | The same | None, already equal |
| Findings only a discontinued product brings: aside "4 findings", the tip naming `control-center-starter` in code type, "Show them", the table whose fix column reads "Override the dependency version in your own build; the advisory's page says the measured one" | The same tip in plain type, and the fix column "Override the dependency version in your own build, the advisory page says the measured one" | Move: the product name in a `code` element; transcribe the fix wording, D-176 |
| Details for CVE-2026-45292: three layers, the first two open | The same | None, already equal |
| Findings we looked at and ruled out: aside "103 findings", the tip, "Show them"; open, the reason column shows the raw applicability code, `false_positive`, `configuration`, `no_risk` | The same; open, the reason column shows the catalogue's label for the code | Keep as app extra: the label is the published word for the code, `labels` in the index |
| All supported versions: aside "Status is for the newest release of each minor", Minor, Newest release with "safe from X", Status chip, Open, Maintenance, Scanned, the reader's row "24.7 · yours" | The same | None, already equal |
| Section links are hash links, `#version/24.7.19?boot=0`, `?disc=1`, `?ruled=1`, each click a history entry | Real links to the address they write, a plain click answered in place, each a history entry, D-58 | None: D-58 confirmed, D-181 |

## Not measured

| Prototype | App before | Action |
| --- | --- | --- |
| 24.9.15, 24.8.3, 25.0.4, 99.1.0: the card with the sentence and the See X instead link | The same words and link | None, already equal |
| 6.0.0, below every followed line: "We follow the lines Vaadin still releases, and 6.0 is not one of them. Nothing here can answer for it, which is not the same as it being safe." and no link | "... 6.0.0 is older than every one of them. The oldest release we measure is 7.7.0. ..." and See 7.7.0 instead, D-57 | Keep as app extra: D-57 confirmed under the person's choice to keep what the app has, D-182 |
| A version red only through findings nobody judged | Not observable: no version in the published summary is in that state on this data | D-53 stays, not answered by the prototype |

## At 390 pixels

The prototype's page scrolls sideways at 390, `scrollWidth` 540: the tables keep all their columns. The application hides the Maintenance column of the versions table and wraps everything else, `scrollWidth` 390. The application's behaviour at that width is kept.

## Verified after the change

In development mode on live data, in a tab of an isolated Playwright context, with real clicks: 24.7.19 at 1920 reads H1 Security status, H2 Vaadin 24.7.19, then H3 for the card's headline and each of the five sections; the card is tinted red with UNRESOLVED in capitals; `<tomcat.version>` and `control-center-starter` are `code` elements. Show them under ruled out and then under discontinued wrote `?ruled=1` and `?ruled=1&disc=1`, the discontinued table's fix column reads in the prototype's words, and Back closed the discontinued table again. Notify me opened the dialog. 24.9.21 shows its eleven not assessed ids separated by commas and twelve fix rows each ending with a chevron. At 390 by 844 the page does not scroll sideways, `scrollWidth` 390. The CVE screen shares the card's classes; the new rules are scoped to `.version-view` so it keeps its look, which was checked by reading the selectors, not in a browser.

# Prototype comparison: the verdict workbench

Compared on 2026-11-22, both sides reading the same published `data/queue.json`, 101 packages and 213 verdicts: the prototype at https://vaadin.github.io/cve-status/findings/workbench.html past its client side gate with the internal account the person gave, and this application in development mode on live data, on port 8090, without the model key, signed in as the team, with its drafts in a scratch directory. Both at 1920 by 1080. Screenshots: `prototype-workbench.png`; `app-workbench-before.png` before this change and `app-workbench.png` after it.

The states put to both: arrival over every series; the series 14.14 and 24.9; the first row, node-forge, opened; org.apache.tomcat.embed:tomcat-embed-core opened with its verdict form, which carries 17 verdicts to fill from; a package carrying exactly one verdict opened; a composed verdict. The whole text was compared line by line. The prototype's rules were read in `web/findings/bench.js`; no code was copied. Nothing was submitted to github.com on either side.

## The page

| Prototype | App before | Action |
| --- | --- | --- |
| H1 "The verdict queue" in the bar, the document title the same | "Verdict workbench" | Transcribe wording; the destination in the bar stays "Verdict workbench", as the explorer's stays "Findings", D-220 |
| "All series" back link | Absent | Add |
| The lead: "1 finding carry no verdict on the heads of the series we follow, over 1 package. The first row below is worth 100 per cent of that on its own." | "One finding carries no verdict on the series we follow, over one package. The first row is worth 100 per cent of that on its own." | Transcribe wording, with the singular kept |
| The hint: "A verdict is keyed by a package with no version and answers for every series at once, ..." | Absent | Add, transcribe wording |
| The series as chips: "every series", "25.4 0", ... with a tooltip "N advisories to judge on X"; a click replaces the address | Links "Every series", "25.4 (0)", `?series`, D-130 | Transcribe wording; `?series` stays, D-130 |
| No summary | The summary in figures, by severity and by series, between the filter and the queue, D-119 | Keep as app extra, moved after the queue and shut, where it no longer pushes the queue down, the finding of the epic 10 review |
| One row per package, a disclosure: the count it would close, the package, "to judge" or "answered", "21 advisories on 1 series, 14.14", ". Spring Boot owns the version, so a bump is not one of the answers here" | A grid: Closes, Package, Advisories, Series, Version owned by | Move: one disclosure per package with the prototype's row, D-220. The grid's columns are all in the row's words |
| The series of a row in text order, "14.14, 23.6, 24.10" | Newest first | Keep as app extra: every other list here is newest first, as the prototype's own comment asks |
| Several rows can be open at once | One row chosen at a time | Keep as app extra: the one form and its live YAML belong to the row opened, so one row is open at a time |
| Under the row: "Already in the check list of X (N of M). ..." and "We could not read the list of 7.7, 8.32, so nothing is claimed about them." | Absent; the still red branches only after a silencing verdict | Add, transcribe wording |
| On a series, "You are looking at 14.14, and N advisories of this package reach other series only. They are all listed below, ..." | The same in our words, above the form | Transcribe wording, moved into the row |

## The evidence

| Prototype | App before | Action |
| --- | --- | --- |
| "Resolves to": each resolved version and "on" the series | Absent | Add, from `resolved`, D-221 |
| "Arrives by": each chain joined with arrows, "declared by X" | Absent | Add, from `routes`. The prototype prefixes the entry to a chain that already starts with it, "webpack-dev-server → webpack-dev-server → ..."; the application writes the entry once, D-221 |
| "CPE matched" and "vendor:product:target_sw. A target_sw that is not this ecosystem is a collision and settles the verdict on its own." | Absent | Add, from `cpes`, D-221 |
| "Reported by bomber, osv-scanner." | Absent | Add, from `scanners` |
| "Of 61 advisories here, upstream has fixed 42, and osv.dev does not list 19 against the version we resolve, ..." | Absent | Add, from `fixed_upstream` and `absent_upstream` |
| "Frontend. Compare the upstream fix date with ..." for an npm or javascript package | Absent | Add, transcribe wording |

## The verdict

| Prototype | App before | Action |
| --- | --- | --- |
| "Write a verdict", "composes the YAML and the pull request, writes nothing by itself", a fold inside the row, shut | The form under the grid, "A verdict on X" | Move: the one form opens inside the row chosen, in a fold of that name, D-220 |
| "This package already carries a verdict" with one "fill from X" chip per verdict, its applicability beside it; with one verdict the form arrives filled | Absent | Add, from the queue's `assessments`, D-222 |
| Filling: the applicability, the depth back to cursory, affects, who, why, impact, workaround, waiting on, release with the fix, pull requests, supersedes set to it, its advisories ticked; "Filled from X, which answers N advisories here. Composing now writes a new file that supersedes it; ..." and the warning when its range does not reach the series shown | Absent | Add, transcribe wording, D-222 |
| "Which advisories this verdict answers": one tick per advisory with its severity, its series, "fixed in X", "not listed: ...", "already X in Y" or "answered on other series by Y, not here"; the unjudged ticked | A text field of ids, prefilled with the unjudged, D-124 | Add: the ticks above the field, which they write; the field stays for ids typed by hand: keep as app extra, D-221 |
| "What we are saying": "affects you: It is real and it reaches a Vaadin application. ...", the six words with their sentences | "Affects you" and the five others, no sentence | Transcribe wording |
| "How far it was taken" with "read, not run: ..." and the hint "The page prints this, ..." | The four words, no sentence, no hint | Transcribe wording |
| The field labels and hints: "Which Vaadin versions it applies to", "Who assessed it", "Why" with "Refused empty by the writer, ...", "What it means for your application", "What the reader can do today", "What it is waiting on" with "Prose. Nothing checks it.", "The release that will carry the fix", "Pull requests that fix it", "What was checked, so a reviewer can repeat it", "Supersedes" | Our own labels and hints | Transcribe wording where the prototype has a label or a hint; the application's own hints stay where the prototype has none |
| `affects` arrives as `*` | Empty | Keep as app extra: an absent key is the cve-status loader's default `*` already, D-122 |
| The id: the purl slug and, for one advisory, its number; a year suffix when superseding | `<purl slug>[-<series>]-<date>`, D-124 | Keep as app extra, D-124 |
| "Compose the verdict": refuses no advisory and an empty why, then the YAML, "This closes N advisories on M series, a, b, which is K findings off the page. It also silences them ..." or "It silences nothing: ...", "It belongs at data/assessments/X.yaml. ...", the still red branches, "Open it as a new file on GitHub" | The YAML live as the person types, Export validating the `Ready` group, the reach and the platform sentences, the path, Save, Copy, Open in GitHub | Transcribe wording for the reach, the platform and the path sentences; the rest stays: keep as app extra, D-127 |
| The YAML carries the store's header comment, `null` for empty keys | No header, empty keys omitted, D-122 | Keep as app extra, D-122 |
| "Then run python scripts/ci/check_assessments.py, ..." | Absent | Add, transcribe wording |
| No drafts | Saved drafts, everybody's, D-131 | Keep as app extra |
| No paste box | The paste box of epic 11 | Keep as app extra |
| Footer: "The heads of the followed series only, ..." | Absent | Add, transcribe wording |

## Data

Every datum the prototype shows is in `data/queue.json`: `resolved`, `routes`, `cpes`, `scanners`, `acknowledged`, `fixed_upstream`, `absent_upstream`, `absent_reason`, `absent_about`, `git_only`, `by_line`, `said_by_line`, `assessments`. The application's read model already carried all of them except the verdicts' own fields, which `assessments` carries. No data need row.

## Verified after the change

In development mode on live data, on port 8090, in an isolated Playwright context signed in as the team, at 1920 by 1080, with real clicks; nothing was submitted to github.com. The lead, the hint, the chips "every series", "25.4 0" to "14.14 1" and every row read as the prototype's, "1 node-forge to judge 21 advisories on 1 series, 14.14", with the series newest first. org.apache.tomcat.embed:tomcat-embed-core opened: "We could not read the list of 8.32, 7.7, so nothing is claimed about them.", the four resolved versions, the two chains with "declared by", the three CPEs and their hint, "Reported by bomber, dependency-check, osv-scanner.", "Of 61 advisories here, upstream has fixed 42, and osv.dev does not list 19 ...", the 17 verdicts to fill from with their applicability, and 61 ticks, each with its severity, series, upstream fix or reason and "already risk in ...". com.vaadin:vaadin-swing-kit-flow opened arrived filled from its one verdict, "Filled from maven-com-vaadin-vaadin-swing-kit-flow, which answers 1 advisory here. ...", `supersedes` set, depth read, not run; Export enabled the `NativeForm`, inspected and not pressed: GET to `https://github.com/vaadin/cve-status/new/main`, target `_blank`, `filename` `data/assessments/maven-com-vaadin-vaadin-swing-kit-flow-2026-11-22.yaml`, `value` the YAML; "This closes 1 advisory on 6 series, ..., which is 6 findings off the page; ...", the platform sentence naming 23.6, the path sentence. On `?series=24.9`, opening highcharts shut the first row and moved the form there; unticking its first advisory took it out of the advisories field; "fill from npm-highcharts-2026-107588-25-3" filled the form and said "Its range, >=25.3.1,<25.4, does not reach 24.9, ...". At 760 pixels the page does not scroll sideways.

On 2026-11-28 the person compared the queue with the prototype again on live data: it takes the prototype's look with Vaadin components, D-275.

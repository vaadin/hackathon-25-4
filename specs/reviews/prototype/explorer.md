# Prototype comparison: the explorer front door and the internal shell

Compared on 2026-11-22, both sides reading the same published data, `generated_at` of that day: the prototype at https://vaadin.github.io/cve-status/findings/ signed in through its client side gate with the internal account the person gave, and this application in development mode on live data, on port 8090, without the model key, signed in as the team. Both at 1920 by 1080. Screenshots: `prototype-explorer.png` (arrival, nothing chosen), `prototype-explorer-open.png` (24.4 opened) and `prototype-explorer-legend.png` (the legend open) for the prototype; `app-explorer-before.png` and `app-explorer-open-before.png` (14.14 opened) for the application before this change; `app-explorer.png`, `app-explorer-open.png` and `app-explorer-legend.png` after it.

The same questions were put to both: arrival with nothing chosen; the discontinued product turned on; every product a reader can switch turned off ("Count only what comes with Vaadin"); the Spring Boot starter off; the searches `CVE-2026-45292` (an id), `jackson-databind` (a coordinate), `24.9` (a series), `24.9.21` (an older release), `19.0` (a line nobody follows), `zzz` (nothing) and `CVE-2024-38808`; the lines 24.7, 24.4 and 14.14 opened; one finding opened, CVE-2026-45292 on 24.7. The whole text of the left panel, of every series row and of each opened line was taken from both sides and compared line by line. The prototype's behaviour was read in its sources, `web/findings/home.js`, `answer.js` and `card.js` of vaadin/cve-status, for the rules behind the numbers; no code was copied.

The comparison was written before anything was changed. The Action column is one of: move, add, transcribe wording, keep as app extra, data not published. A row whose two sides already agree says so.

## The internal shell

| Prototype | App before | Action |
| --- | --- | --- |
| A left panel about 315 pixels wide holding the explorer's filter and its products, and nothing else | The drawer of `InternalLayout`: the four destinations from `@Menu`, then the assistant panel | Move: the drawer holds what the route gives it, the explorer's filter and products, D-203 |
| A top bar over the content: the drawer toggle, the H1 "Vaadin security status", a theme toggle at the right | The bar: the drawer toggle, the trail Vaadin, Security status, Findings, the measured chip, the dark switch, Sign out; the H1 "Findings" inside the content | Move: the H1 goes into the bar after the toggle, drawn by the shell from the route's title, and the explorer's title is the prototype's, D-203, D-215. The trail, the measured chip, the dark switch and Sign out stay at the right of the bar: keep as app extra |
| No navigation between the internal pages: index links to `spring.html` from each series and to `release.html` from the releases list; `spring.html` and `workbench.html` have no drawer and only a link back to `index.html`; nothing links to the workbench | The four destinations in the drawer | Keep as app extra, moved: the four destinations, Findings, What Spring Boot brings, Verdict workbench and Diagnostics, are a row of links in the bar after the H1, so each stays one click away without taking the left panel, D-203 |
| A page with no filter has no drawer and no toggle | Every internal page has the drawer | Move: a route that gives the shell no drawer content gets none, and the toggle is hidden, D-203 |
| Below 800 pixels the left panel overlays the content behind a scrim | The drawer overlays at the App Layout's own breakpoint | None, already equal in kind: the App Layout's drawer is the overlay, D-204 |
| No assistant | The assistant panel under the destinations in the drawer | Keep as app extra, moved: a panel on the right opened by an Assistant button in the bar, shut on arrival, D-203 |
| A theme toggle that cycles the theme | The dark switch | None: the same control in another shape |

## The left panel: the filter

| Prototype | App before | Action |
| --- | --- | --- |
| Section heading "Filter by advisory, version or dependency", a field and a Filter button with a search icon | A field labelled "Search" with the helper "An advisory, a version or a dependency. A full version opens its release." at the top of the master pane | Move into the left panel; transcribe wording: the heading. The helper sentence stays: keep as app extra. Add the Filter button |
| Submitted on Enter or with the button; emptying the field clears the search at once | Submitted on Enter or on leaving the field, D-103 | None for Enter; add the button and the clearing. Leaving the field still submits: keep as app extra, D-209 |
| A whole release we measured, `24.9.21`, goes to its release page | Any full version goes to its release page, measured or not, D-103 | None for a measured release. A release nobody measured still goes to the release page, which answers it, D-118: keep as app extra, D-209 |
| A number is read as a version first: `24.9` keeps the 24.9 series; then an id, then a coordinate of `packages` | Lines whose minor or version contains the text, then ids and coordinates | Move: the prototype's reading and its order, D-209 |
| Under the field, the note: "Showing 1 advisory matching CVE-2026-45292.", "Showing what arrives through a coordinate matching jackson-databind: 15 advisories.", "Showing the 24.9 series and nothing else.", "We do not follow 19.0. ..." with its second paragraph, "We report nothing under that on any series we follow." with its second paragraph | Absent | Add, transcribe wording, D-209 |
| `24.9.21` typed: "You asked for 24.9.21. This answers for 24.9.26, the newest of that series: see 24.9.21." | The release page of 24.9.21 | Keep as app extra: the jump of D-103, D-209 |
| An advisory no series carries any more: "CVE: 24.9 carried this up to X. It is gone in Y: see Y." from `history` | Absent | Add: `front.json` carries `history`, D-209 |

## The left panel: the products

| Prototype | App before | Action |
| --- | --- | --- |
| Heading "Narrow the list to your application" with a mark whose tooltip says "Everything is counted until you say otherwise, ..." | Heading "Products" | Transcribe wording, the heading and the mark's sentence, D-207 |
| A header row: the select all box, "Product", "Advisories" | A checkbox "All products" | Move: the box heads the list with the two column words, D-206 |
| The box is ticked when every product counted by default is on, a dash when some are, empty when none; ticked, it turns every one of them off, the Spring Boot starter included ("Count only what comes with Vaadin"); otherwise it goes back to the default and forgets the remembered choice ("Count every product") | Ticked when everything is on; from everything on it turns the optional products off, D-96 | Move to the prototype's states and targets, D-206 |
| One chip per product of the profile's `on` and `off` lists that `module_names` knows: 15 here. `vaadin` first, ticked and still; the discontinued product last, marked "discontinued", off until somebody turns it on | One chip per product of `module_names` that would hide something, 11 here, among them `@vaadin/router` and `@vaadin/react-components-pro`, which no reader chooses; no chip for `vaadin`; "9 more products bring only findings you also have through the ones shown." | Move: the profile's catalogue, in its order, `vaadin` drawn ticked and still, the discontinued product off by default, D-206. The sentence about silent products goes with the rule it counted |
| A chip's number is how many different advisories on the page arrive through it, asked as if it were on: vaadin 27, vaadin-spring-boot-starter 259, control-center-starter 136 | The marginal count, summed per line: vaadin-spring-boot-starter 822 | Move: distinct advisories over the series shown, D-207 |
| Each chip's tooltip: "Listed. Click to leave it out. ...", "Not listed. Click to add it. ...", "You have this because you have Vaadin, ...", "Vaadin no longer develops this. ..." | "Turning X off hides N" | Transcribe wording, D-207 |
| With the Spring Boot starter off, the products that need Spring are drawn off and cannot be clicked | They stay clickable | Add, D-206 |
| A colored ring on each chip holding its number, grey when off | A ring before the name, the number after it | Move: the count in the ring, D-207 |

## The main area

| Prototype | App before | Action |
| --- | --- | --- |
| A round "?" at the top right of the content opens a panel on the right, "What the words on this page mean", with The badge on a series, The groups inside a series, How far a verdict was taken, What this page is measuring; Close, Escape | Absent | Add, transcribe wording, D-211 |
| Under a filter, a lead: "4 maintained series of 10 match CVE-2026-45292: 2 are vulnerable, 2 are not affected.", "None of the 10 maintained series carries anything matching zzz.", "We follow 10 maintained series and 19.0 is not one of them." | "8 of 10 lines match" | Transcribe wording, D-209 |
| H2 "Series (10 maintained)", "Series" under a filter | No heading | Add, transcribe wording |
| One row per series, opened in place under itself, one at a time; the opened series in the address, `?open=24.4` | A row per line in a Master Detail Layout, the opened line in the detail pane, not in the address, D-94, D-102 | Move: each row is a disclosure opened under itself, one at a time, the opened line in the address as `?open`, D-204 |
| Row: the series, the release, "scanned 2026-11-22" with a tooltip, "Vaadin: Not affected", "Spring Boot: Vulnerable" with a tooltip naming the Spring Boot it was measured against, a sentence, one ring per product with its count, noughts drawn | The line, the release, "Scanned 2026-11-22", "Vaadin: not affected", "Spring Boot 3.5.16: not affected", a sentence of five counts, one ring per product that brings anything, no number | Transcribe wording: the scanned word, the badges, their tooltips; the Spring Boot version moves into the tooltip, D-205. Add the counts in the rings and the noughts, D-207 |
| The badge answers for the findings Vaadin can move, `owner` vaadin and not `spring_pins`, through the products on: 24.10 and 24.9 "Vaadin: Not affected" | Everything not Spring Boot's or the build's: 24.10 and 24.9 "Vaadin: vulnerable", because two findings whose version Spring Boot pins counted as ours | Move: the prototype's halves, D-205 |
| The row's sentence: "1 finding here is a real risk. Each one says what to do.", "13 findings here are a real risk, and 1 more nobody has assessed yet. ...", "26 findings arrive with your Spring Boot" | "112 findings: 2 to act on, 0 not assessed yet, 41 from Spring Boot, 0 only in the build, 69 answered." | Transcribe wording. The five counts move into the opened line: keep as app extra, D-212 |
| A series marked at its start by its state's color | A plain card | Add |

## An opened series

| Prototype | App before | Action |
| --- | --- | --- |
| No title: the panel opens under the row | "Line 14.14, release 14.14.9" and a close button | Keep as app extra, D-212 |
| No Spring switch: the starter's chip does it | The switch "Without Spring Boot", and "This line was measured without Spring only." | Keep as app extra, D-212 |
| "From Vaadin", with a tooltip, when the other half is there | Absent | Add, transcribe wording, D-205 |
| "13 a real risk", "1 not assessed yet", open; "61 answered, nothing to do", shut; each with its tooltip | "To act on (13)", "Not assessed yet (1)", "From your Spring Boot 2.7.18 (132)", "Only in the build (12)", "Answered (61)" | Transcribe wording; move: the groups are per half, D-205. The build only group has no place in the prototype, which leaves those findings out: keep as app extra, after both halves |
| "Nothing on this release is waiting for you: ..." when neither the risk nor the unassessed group has anything | Absent | Add, transcribe wording |
| "Of these, 31 findings are in the frontend bundle the framework publishes, ..." | Absent | Add, transcribe wording |
| "14 findings on this release arrive only through products you have switched off, ..." | "14 findings are hidden because every product they arrive through is off." | Transcribe wording, D-212 |
| "With Spring Boot", with a tooltip; "132 findings on this series come with Spring Boot rather than with Vaadin, measured against 2.7.18. Upgrading Spring Boot is what moves them and no Vaadin release can. See what Spring Boot brings", then its own risk, unassessed and answered groups | One Spring Boot group with a note | Move, transcribe wording, add the link to the Spring Boot page, D-205 |
| "For your own tooling:" and JSON, CSV always, VEX, dependency-check, osv-scanner, bomber where the release has scanner files, each with a tooltip | "Files of 14.14.9" and six files only when the release has scanner files, D-109 | Transcribe wording; move: JSON and CSV always, D-212 |
| "Other measured releases of 14.14: 14.14.8, ..." linking each release page | Absent | Add from `releases`, D-212 |
| No link to the head release | "Release page of 14.14.9" | Keep as app extra |
| A finding row: id, severity, "through 4 products", "through 0 of 1 product", "only with vaadin-swing-kit-client", "bundled inside X", "in the frontend bundle Vaadin publishes", "we could not establish which product brings this", each with a tooltip naming the products; dots for the products that are on | A grid row: id, severity, "only with X" for one product, "through N products", "through N of M products", "bundled inside X", "no product recorded"; a dot per product, off ones dimmed | Transcribe wording, D-208. The grid stays: keep as app extra |
| An opened finding: the summary, "What it means for your application", "What Vaadin says", "What you can do today", "The fix", the routes, "Read the advisory"; long texts cut with "more" | Summary, "What it means", "What you can do today", "Fix", "Routes", "Advisory record of X", texts whole, no commentary | Transcribe wording; add "What Vaadin says" from `commentary`; the summary without its heading, D-213. Texts stay whole: keep as app extra |

## Behaviour

| Prototype | App before | Action |
| --- | --- | --- |
| Products in the hash relative to the default, `#control-center-starter`, `#-vaadin-spring-boot-starter`, `#-all`; the search and the opened series in the query; every change replaces the address in place | `?off` names what is off, `?q` the search; each change is a navigation and a history entry, D-98 | Keep the grammar of `specs/03-architecture.md`, `?off` and `?q`, now with `?open`: no `?off` is the default, which leaves the discontinued product off; a bare `?off` is everything on; an `?off` equal to the default is written away, D-206. The history entries stay, D-210 |
| The choice of products remembered in local storage and restored when the address names none | The same in two keys of our own, D-99 | None in kind, D-214 |
| Groups open by their kind each time, not remembered | Groups opened or closed by hand remembered, D-99 | Keep as app extra, D-214 |
| The finding details are fetched the first time a finding of the release is opened | The same, D-104 | None, D-104 confirmed, D-214 |

## Data

Every datum the prototype shows on this screen is in `data/front.json` or the release's record: `profile`, `module_names`, `spring_pins`, `without_spring`, `needs_spring`, `releases`, `history`, `bundled_in`, `ecosystem`, `commentary`. No data need row.

## Verified after the change

In development mode on live data, on port 8090, in a new tab of an isolated Playwright context signed in as the team by typing, at 1920 by 1080, with real clicks: on arrival the left panel holds only the filter and the fifteen products, `vaadin` 27, `vaadin-spring-boot-starter` 259, `control-center-starter` 136 off and marked discontinued, the same numbers as the prototype; every one of the ten rows reads the prototype's badges, sentence and rings, number for number, 24.10.14 "Vaadin: Not affected", "Spring Boot: Vulnerable", "26 findings arrive with your Spring Boot", 1 86 2 0 5 0 3 29 22 0 0 0 1 3. 24.4 opened under its row, `?open=24.4`, and its panel read as the prototype's: From Vaadin, 25 a real risk, 50 answered, the frontend bundle sentence with 13, With Spring Boot with 156 findings measured against 3.2.12, 66 a real risk, 90 answered, the six files and the 28 other releases; the application's build only group, title, switch, count sentence and release link are there too. The "?" opened the legend and Escape shut it. A click on `control-center-starter` wrote a bare `?off` and turned 24.7 to "5 findings here are a real risk", as the prototype says with it on. `CVE-2026-45292` typed and Enter: the four series, the lead "4 maintained series of 10 match CVE-2026-45292: 2 are vulnerable, 2 are not affected." and the note "Showing 1 advisory matching CVE-2026-45292.", word for word the prototype's. The box at the head of the list turned everything off, leaving the rows of the prototype's `#-all` state, 23.6 "12 findings here are a real risk", and a second click went back to the default address. The Assistant button opened the panel on the right. The four destination links reached the Spring Boot page, the workbench and Diagnostics, each with its H1 in the bar, no left panel and no toggle, and back to the explorer; the release page of 24.9.21 renders with its H1 in the bar. At 760 pixels the left panel arrives shut as an overlay, the toggle opens it, and the page does not scroll sideways, `scrollWidth` 760. At 1280 and 1024 the bar wraps onto a second row, the destinations on the first, the trail, the chip, the switch, Assistant and Sign out on the second, and Sign out stays on screen and takes the click; `CspIT` at 1280 found it off screen before the bar was allowed to wrap.

The other internal pages were only checked to render and stay reachable under the new shell; they are aligned in their own session.

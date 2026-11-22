# Prototype comparison: the release page

Compared on 2026-11-22, both sides reading the same published data, `generated_at` 2026-11-22T05:30:11Z: the prototype at https://vaadin.github.io/cve-status/findings/release.html signed in through its client side gate with the internal account the person gave, and this application in development mode on live data, on port 8090, without the model key, signed in as the team. Both at 1920 by 1080. Screenshots: `prototype-release.png` for the prototype on 24.9.21; `app-release-before.png` for the application before this change and `app-release.png` after it.

The same releases were put to both: 24.9.21 (an older release of a followed line, with a Spring Boot half), 24.9.26 (the newest of its line, scanner files), 24.9.0 (the oldest measured, many unjudged), 24.4.20 (an older line, no scanner files), 14.14.9, 7.7.56 (measured without Spring only), and the releases nobody measured: 24.9.99 (ahead of its line), 23.2.8 (a line nobody follows, its major followed) and 19.0.1 (a major nobody follows). The whole text of the left panel and of the content was taken from both sides and compared line by line. The prototype's rules were read in its sources, `web/findings/release.js`, `answer.js` and `card.js` of vaadin/cve-status; no code was copied.

The comparison was written before anything was changed. The Action column is one of: move, add, transcribe wording, keep as app extra, data not published.

## The shell and the left panel

| Prototype | App before | Action |
| --- | --- | --- |
| The left panel the front door has: "Filter by advisory, version or dependency", the field holding the version on screen, the Filter button | No left panel; a field "Another release" with a helper above the card | Move: the field goes into the left panel through `DrawerContent`, under the prototype's heading, with the Filter button. A full version opens its release, anything else goes to the explorer with `?q`, D-117. The helper sentence stays: keep as app extra |
| "Narrow the list to your application" with a mark whose tooltip says "Everything is counted until you say otherwise, so this card answers for the whole release until you narrow it. ..." | Absent: no product narrowing on the release page, D-111 | Add, transcribe wording |
| One chip per product of the catalogue, all fifteen always offered, each with its count on this release asked as if it were on: on 24.9.21 vaadin 42, vaadin-spring-boot-starter 88, control-center-starter 85 and off, marked discontinued; no select all box | Absent | Add: the explorer's chips, counted on this release, every catalogue product offered, D-216 |
| Turning a chip writes the same choice the front door keeps and redraws in place; the page has no address of its own for it | Absent | Add: a chip changes the session's `ProductSelection`, which the explorer reads and writes too, and its web storage, and the card redraws in place. Each chip is a button, not a link, since the release page's address names no products, D-216 |
| The Spring Boot starter is a control only where the release was measured without Spring; elsewhere it is drawn ticked and still: "Nobody has measured X without Spring, so this screen can only answer for an application that has it. ..." | Absent | Add, transcribe wording, D-216 |
| A release nobody measured: the products are taken off the panel and the toggle hidden, the filter stays | Absent | Move: the products are not drawn for a release without a record; the filter stays. The toggle stays: the shell shows it for any route with a panel. Keep as app extra |
| H1 in the bar "Vaadin 24.9.21 security status", the document title the same | "Release 24.9.21" | Transcribe wording, D-217 |
| A round "?" at the top right opens the legend the front door has | Absent | Add: the explorer's legend, shared, D-217 |
| "All series" back link | "All series" back link | None |

## The card

| Prototype | App before | Action |
| --- | --- | --- |
| The card is the front door's series row, opened: "24.9.21", "in 24.9", "Vaadin: Vulnerable", "Spring Boot: Vulnerable" with a tooltip naming the Spring Boot the release declares, the row's sentence, the rings | "24.9.21", "in 24.9", "Scanned 2026-11-22", "Vaadin: vulnerable", "Spring Boot 3.5.15: vulnerable", then the sentence of five counts and rings without numbers | Move to the explorer's row shape: badges by the halves, the Spring Boot version in the tooltip, the prototype's sentence, one ring per product on with its count. The scanned date moves to the footer as the prototype has it; it stays in the card's row as well: keep as app extra. The sentence of five counts stays inside the card, now counted over the halves: keep as app extra |
| The badges and the groups are counted over the halves of D-205: on 24.9.21 70 findings with Spring Boot, a pinned Jackson among them | The badges already answer by the halves; the groups still owner first, D-95: 41 from Spring Boot | Move: the groups follow the halves, so badge and groups cannot disagree, D-216. This was the carry over of session five |
| Everything is narrowed by the products on: 18 findings that only `control-center-starter` brings are not listed and are said in one sentence | Nothing narrowed, D-111 | Add: the release is counted for the products on, the explorer's rule, D-216. Supersedes D-111 |
| Advice, first paragraph: "You are not on the newest of this series. Updating to 24.9.26 removes 18 of these: ... and 12 more. It is a patch of the line you are already on." counted over the reader's findings of ours with a `fixed_in` | "Updating to 24.9.26, a patch of the 24.9 line you are on, removes 22 of these: ...", every finding not Spring Boot's, worst first, D-112 | Transcribe wording; move: counted over the reader's own findings, in the record's order, D-218 |
| On the newest: no update paragraph | "This is the newest measured release of 24.9." | Keep as app extra |
| "Read 24.9.26" is absent; the update paragraph names the release only | A link to the newer release after the paragraph | Keep as app extra |
| Advice, second paragraph: "70 findings on this release come with Spring Boot rather than with Vaadin. Measured against 3.5.15, which is also what this release declares. There is somewhere to upgrade to: Spring Boot 3.5.16, the newest patch of that line. That is where fixes for those arrive, and no Vaadin release moves them. See what Spring Boot brings." | "41 findings ... They were measured against Spring Boot 3.5.15, which this release also declares. Spring Boot 3.5.15 can move to 3.5.16, the newest patch of that line. ... What Spring Boot brings" | Transcribe wording, counted over the Spring Boot half, D-218 |
| "For your own tooling:" JSON, CSV, and VEX, dependency-check, osv-scanner, bomber where the record has scanner files, each with its tooltip | "Files of 24.9.21" with "JSON record", "CSV" | Transcribe wording: the explorer's `tooling` shape, D-212 |
| "From Vaadin" with its tooltip, when the other half has anything; "a real risk", "not assessed yet", "answered, nothing to do", all three shut on arrival | "To act on (2)", "Not assessed yet (15)", "From your Spring Boot 3.5.15 (41)", "Only in the build (1)", "Answered (76)" | Move, transcribe wording: the explorer's halves and groups, shut on arrival as the prototype's are, a group opened by hand remembered as on the explorer, D-214, D-216 |
| "Nothing on this release is waiting for you: ..." | Absent | Add, transcribe wording |
| "Of these, 13 findings are in the frontend bundle the framework publishes, ..." | Absent | Add, transcribe wording |
| "18 findings on this release arrive only through products you have switched off, so they are not listed here. They still count towards the state above, which answers for the release rather than for one application." | Absent | Add, transcribe wording |
| "With Spring Boot" with its tooltip and its three groups | One "From your Spring Boot" group | Move, transcribe wording |
| No build only group | "Only in the build (1)" | Keep as app extra, after both halves, as on the explorer |
| A finding row and an opened finding are the front door's | The explorer's grid and finding detail | None: shared since D-208 and D-213 |
| "Same answer as X." on a second finding that carries the same workaround | Absent | Not added: the opened finding shows the workaround whole; recorded as a difference |
| Footer: "Scanned 2026-11-22. Measured against an application shaped like the starter of its own major." | Absent | Add, transcribe wording |

## A release nobody measured

| Prototype | App before | Action |
| --- | --- | --- |
| 24.9.99: "We hold no measurement of 24.9.99. The newest maintenance release of 24.9 is 24.9.26, and that is the one to update to: it is a patch of the line you are already on. It is the release we measure and answer for." | "We hold no measurement of 24.9.99. The newest measured release of 24.9 is 24.9.26, which is older than it." | Keep as app extra: telling a reader ahead of the newest measured release to update to it sends them backwards, D-118 |
| An older release of the line: the same sentence | "We hold no measurement of X. The newest measured release of 24.9 is 24.9.26, a patch of the line you are already on, and the one to update to." | Transcribe wording |
| 23.2.8: "We hold no measurement of 23.2.8. 23.2 is out of maintenance. The line we follow on Vaadin 23 is 23.6, whose newest release is 23.6.19, and that is the one to update to. It is the release we measure and answer for." | "We hold no measurement of 23.2.8, and 23.2 is not a line we follow. ..." | Transcribe wording |
| 19.0.1: "We hold no measurement of 19.0.1, and we do not follow that series. Vaadin measures the maintained lines, and the status page lists them: moving to one of those is what gets you an answer." and "See the maintained series" | "... Nobody measured it, which is not the same as nothing wrong with it." and no link | Transcribe wording; add the link to the explorer |
| "What 24.9.26 carries" | "What 24.9.26 carries" | None |
| "Other releases of 24.9 we have measured: 24.9.25, ... 24.9.13 and 13 more", comma separated | "Other measured releases of 24.9: ..." space separated | Transcribe wording |
| A listed release whose record does not load is answered as not measured | "X is a measured release of Y, and its record is not published, ..." D-110 | Keep as app extra, D-110 |

## Data

Every datum the prototype shows on this page is in `data/front.json` or the release's record: `profile`, `module_names`, `needs_spring`, `without_spring`, `no_spring`, `spring_pins`, `fixed_in`, `spring_boot`, `spring_boot_declared`, `spring_boot_latest`, `scanner_files`, `scanned_at`. No data need row.

## Verified after the change

In development mode on live data, on port 8090, in a new tab of an isolated Playwright context signed in as the team by typing, at 1920 by 1080, with real clicks. The whole text of the panel and the content was taken again for the nine releases above and compared with the prototype's. On 24.9.21, 24.9.26, 24.4.20, 24.9.0, 14.14.9 and 7.7.56 every chip number, every ring, every group count, the update sentence with its count and its six ids, the Spring Boot paragraph, the files, the frontend sentence and the switched off sentence read as the prototype's, 24.9.21 "vaadin 42, vaadin-spring-boot-starter 88, control-center-starter 85", "removes 18 of these", "70 findings on this release come with Spring Boot", "1 a real risk", "11 not assessed yet", "34 answered, nothing to do", "25 a real risk", "45 answered, nothing to do". The differences left are deliberate: the row sentence says "and 11 more nobody has assessed yet" over the group of 11 where the prototype says 6, D-224; the count of five, the build only group, the scan date on the row, the "Read 24.9.26" link and the sentence on the newest release are app extras; 24.9.99 keeps the sentence of D-118. The not measured answers for 23.2.8 and 19.0.1 are the prototype's words, and for an older release of a followed line in `ReleaseViewBrowserlessTest`, 24.9.15 on the fixtures. A click on `control-center-starter` turned it on, listed what only it brings and took the switched off sentence away; a click on the Spring Boot starter read the release without Spring; the explorer opened afterwards wrote that choice into its address, `?off=vaadin-spring-boot-starter`, and the explorer's choice came back to the release page. The "?" opened the legend and Escape shut it. "24.4.20" typed in the panel's field and Enter opened that release; "jackson" and the Filter button went to the explorer with `?q=jackson`. At 760 pixels the panel arrives shut as an overlay and the page does not scroll sideways.

The starter drawn ticked and still, on a release measured with Spring only such as 7.7.56, and its sentence were not looked at in the browser; they are read from the code and the prototype's source only.

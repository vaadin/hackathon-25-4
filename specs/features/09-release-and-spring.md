# Feature 09: Release and Spring Boot pages

## Overview

One release in full, and what Spring Boot brings, the two deep reads the explorer links into. Covers T7 and T8. The layout and the answers are the prototype's, compared past its gate on 2026-11-22 in `specs/reviews/prototype/release.md` and `spring.md`.

## Behaviour

### The release page

- `/findings/release/{line}/{version}`, under the internal shell. The H1 in the bar and the document title read "Vaadin X security status", rule 6.1.7, D-217.
- The left panel holds "Filter by advisory, version or dependency", a field holding the version on screen and a Filter button: a full version opens its release, anything else goes to the explorer with `?q`, D-117. Under it "Narrow the list to your application" with the prototype's explanation on a mark, and every product of the catalogue, each counted on this release as if it were on. A product turned on or off here is the reader's choice for the explorer too, kept in the session and the browser's storage, and the card redraws in place; the page's address names a release, not products. The Spring Boot starter is a control only where the release was measured without Spring, and is otherwise drawn ticked and still with the prototype's sentence, D-216.
- The content: the round "?" opening the explorer's legend, the "All series" link back, rule 6.1.6, and the card: the explorer's series row for this release, opened, with its version, "in X", the scan date, "Vaadin: <state>", "Spring Boot: <state>" with the Spring Boot the release declares on hover, the row's sentence and one ring per product that is on, D-216. Badge, sentence and groups are counted over the halves of D-205 and the products on, so they cannot disagree, D-224.
- Inside the card: the count of what the release carries, the advice, the files, then From Vaadin with "a real risk", "not assessed yet" and "answered, nothing to do", "Nothing on this release is waiting for you" when nothing is, the frontend bundle and untraced hints, what only products switched off bring, With Spring Boot with its three groups, and the build only group the application keeps. Every group is shut on arrival. A group opened or shut by hand is remembered by its kind, shared with the explorer, so a kind left open there opens here too, D-214.
- The advice, D-218. On a release older than the newest of its series: "You are not on the newest of this series. Updating to X removes N of these: ... It is a patch of the line you are already on.", counted over the reader's own findings fixed after this release, naming at most six in the record's order followed by "and N more", then the link "Read X". When the newest carries all of them: "X is the newest measured release of this series and it carries all of these too, so updating within the series does not move them. It is still the release to be on." On the newest: "This is the newest measured release of L." Then, when Spring Boot brings anything, how many, what it was measured against, where Spring Boot can move within its major and "See what Spring Boot brings".
- The files: "For your own tooling:" JSON and CSV always, VEX, dependency-check, osv-scanner and bomber where the record says `scanner_files`, rule 6.3.14, each streamed through the proxy of epic 02.
- A footer: "Scanned D. Measured against an application shaped like the starter of its own major."

### The Spring Boot page

- `/findings/spring`, titled "What Spring Boot brings", with the prototype's lead and the "All series" link back. Its destination is "Spring Boot", nested under Findings in the side navigation; that group shows only while the Spring Boot page is the current one, D-245.
- One row per Spring Boot release the lines were measured with, newest first, shut on arrival: the release, "Vaadin a, b", its state, "N findings arrive with this release, M of them a real risk", and, only where free patches have ended, the supply in a few words, D-219.
- Opened, a row shows, in order: a table with the columns Supply, Release and What is known, holding the open release ("what you have", "measured on Vaadin a: closes N of these M" or "not measured yet") and, where newer, the commercial one ("Spring Enterprise, not measured" or "the last there was, and nobody sells it now"); the supply in a sentence; the advice from `spring_boot_latest` with the paragraph on the version property; and the groups "we have judged a real risk", open, then "nobody has judged yet" and "we have answered, nothing to do", shut, each capped at 150. Each finding shows its id linking to its record, CVE to NVD and GHSA to GitHub, D-113; its severity pill; "we have judged this a real risk", "nobody has judged this" or "we have answered this, nothing to do"; and "gone on X" where a measured Spring Boot upgrade closes it. The row's state is the Spring Boot badge, dashed, with the state word alone.
- The caveat, which says either that every series on a Spring Boot release resolves the same set or which releases do not, and the footer, in the prototype's words.

## Edge cases

| Scenario | Behaviour |
| --- | --- |
| A release address for a version not measured | "We hold no measurement of X." with the line's newest release to update to, linked as "What V carries", then "Other releases of L we have measured:", the first twelve separated by commas and "and N more", D-118 |
| A version newer than the newest measured release of its line | Says the newest measured one is older, rather than offering it as an update, D-118 |
| A version of a line nobody follows any more, its major followed | The prototype's sentence naming the followed line of the major |
| A major nobody follows | The prototype's sentence and "See the maintained series" |
| A release listed by the front door whose record is not published | Says so, and offers the files the line has, D-110 |
| A version that belongs to another line than the address claims | Forwarded to the address of its own line |
| A release measured with Spring only | Its starter is drawn ticked and still |
| A Spring release with supply ended | The row says so and still lists its findings |
| No measured line records its Spring Boot | "No measured line records the Spring Boot release it was measured with." and nothing else |

## Acceptance criteria

### AC1: The release page is whole
- [x] Card, advice, downloads and lookup render for a measured release
- [x] The not measured answer renders in place for the rest

### AC2: Spring groups tell the supply truth
- [x] Each group names its release and its supply status from the data
- [x] NVD links match the advisories

## Test cases

| Id | Given | When | Then | Tier | Verified by |
| --- | --- | --- | --- | --- | --- |
| REL-01 | A measured release | Opening its address | The card and advice match the fixture | browserless | `ReleaseViewBrowserlessTest` |
| REL-02 | An unmeasured version | Opening its address | The in place answer | browserless | `ReleaseViewBrowserlessTest` |
| REL-03 | A product turned off on a release | Reading the card, then the explorer | The card narrows in place and the explorer has the same choice | browserless | `ReleaseViewBrowserlessTest` |
| SPR-01 | The spring fixture | Opening the page | Groups per release with supply wording | browserless | `SpringViewBrowserlessTest` |

## Still open

Written from the code as it stands at the close of the prototype alignment, 2026-11-22, after session six rebuilt both pages, D-216 to D-219. Each line names where the code shows it and the decision or draft behind it.

- The release page's row sentence counts what nobody has assessed over the same findings as the groups; the prototype's sentence counts fewer, `SeriesParts.sentence`, D-224.
- The release page rings a finding through every product on its route, as the prototype's does, because the record carries no `needs_spring`, `ReleaseRules.asLine`, D-216.
- A GHSA id on the Spring Boot page links to GitHub's advisory database, not to NVD, `SpringRules.recordUrl`, D-113.
- An unmeasured release lists twelve of the line's measured releases and counts the rest, not all of them, `ReleaseView.unmeasured`, D-118.
- The prototype's "Same answer as X" on a second finding carrying the same workaround is not built: each finding shows its own text, cut as D-264 says, `FindingDetail`.
- The app keeps extras the prototype does not have: on the release page the count of five, the build only group, the scan date on the row, the "Read X" link and the sentences of D-110, D-115 and D-118; on the Spring Boot page the table header, the supply sentence inside an opened release and the GHSA link, D-218, D-219.
- The "nothing newer published openly" sentence of the release advice is exercised only on live data: no fixture record sits on a Spring Boot whose `spring_boot_latest` entry is empty, `ReleaseView.springAdvice`.
- Advisory ids no longer break at their hyphens, D-231: ids inside the update sentence are drawn through `AdvisoryIds`, measured on seven screens at 1280 and 390.
- No browser test covers either page; the closed list of `08-testing.md` names none, and both were checked in a browser against the prototype in session six.

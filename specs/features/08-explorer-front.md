# Feature 08: Explorer front door

## Overview

The security team's reading: every maintained series, narrowed by the products an application actually uses. The behaviour contract is sections 6.2 and 6.5 to 6.7 of the source requirements, and the layout and the answers are the prototype's, compared past its gate on 2026-11-22 in `specs/reviews/prototype/explorer.md`. Covers T2 to T6.

## Behaviour

### The layout

- The internal shell's left panel holds the filter and the products and nothing else, D-203. Below the App Layout's breakpoint it overlays, which the component gives for free, as the prototype's does below 800 pixels.
- The bar over the content holds the drawer toggle and the H1 "Vaadin security status"; the internal destinations, the trail, the measured chip, the dark switch, the Assistant button and Sign out follow it as app extras.
- The content: a round "?" at the top right, the lead under a search, the heading "Series (10 maintained)", or "Series" under a search, and one row per maintained series. Each row is a disclosure opened in place under itself, one at a time; the opened series is in the address as `?open=<line>`, D-204.
- The "?" opens the legend on the right, "What the words on this page mean", with the prototype's sections: The badge on a series, The groups inside a series, How far a verdict was taken, What this page is measuring. Close and Escape shut it, D-211.

### Search

- "Filter by advisory, version or dependency": one field and a Filter button, held in `?q`, submitted on Enter or with the button; clearing the field clears the search. A number is read as a version first and keeps the series it names, then an advisory id, then an advisory no series carries any more, read from `history`, then a coordinate of `packages`, D-209.
- The note under the field says what was found in the prototype's words: "Showing N advisories matching X.", "Showing what arrives through a coordinate matching X: N advisories.", "Showing the X series and nothing else.", the series that last carried an advisory no series carries any more, "We do not follow X. ..." for a number nothing matches, "We report nothing under that on any series we follow." for a name. The lead over the series counts what is standing and its states.
- A full version typed goes to its release page, the source shortcut, D-103.

### Products

- "Narrow the list to your application", with the prototype's explanation on a mark beside it. One chip per product of the profile's `on` and `off` lists that `module_names` knows: what comes with Vaadin first, drawn ticked and still; then the choices; then what Vaadin no longer develops, marked "discontinued". A product nobody chooses, a frontend package, is no control and always counts as on, D-206.
- Everything is on except what Vaadin no longer develops. The address names what is off, `?off=<name>,<name>`, by product name until the short code export lands, per the data need recorded in `specs/FEEDBACK-25.4.md`; no `?off` is the default and a bare `?off` is everything on. The selection is remembered through web storage and restored only when the address carries no selection of its own, rules 6.2.4, 6.7.3, 6.7.6.
- The box heading the list, beside "Product" and "Advisories", is ticked when every product counted by default is on, a dash when some are, empty when none. Ticked, it turns them all off, "Count only what comes with Vaadin"; otherwise it goes back to the default, "Count every product".
- A finding of ours is the reader's when any product it arrives through is on, or is no control, and always when no product is recorded, rules 6.5.11 and 6.5.12.
- A chip's number is how many different advisories on the series shown arrive through it, asked as if it were on, D-207. Each chip says on hover what it does, in the prototype's words.
- Turning the Spring Boot starter off reads every series on its measurement without Spring; the products that need Spring cannot be had there and are drawn off and still.

### The series rows

- One row per maintained series: the series, the release, "scanned <date>", "Vaadin: <state>", "Spring Boot: <state>" when anything is Spring Boot's, a sentence, and one ring per product that is on with its count, noughts drawn so a product keeps its column.
- The two halves, D-205: the Vaadin badge answers for the findings Vaadin can move, owner `vaadin` on a version Spring Boot does not pin, through the products on; the Spring Boot badge for Spring Boot's own findings and the versions it pins. A state is Vulnerable, Not assessed yet or Not affected, each with the prototype's explanation on hover.
- The sentence: "N findings here are a real risk. Each one says what to do.", with ", and M more nobody has assessed yet" when there are; on a series clear of ours, "N findings arrive with your Spring Boot" when that half has anything open.

### An opened series

- From Vaadin, when the other half has anything: "a real risk" and "not assessed yet", open; "answered, nothing to do", shut; each with the prototype's explanation on hover, worst first. "Nothing on this release is waiting for you: ..." when neither of the first two has anything.
- The frontend bundle sentence, the untraced sentence, and the sentence for what only a switched off product brings.
- With Spring Boot: "N findings on this series come with Spring Boot rather than with Vaadin, measured against X. ... See what Spring Boot brings", linking the Spring Boot page, then its own three groups.
- "For your own tooling:" with JSON and CSV always, and VEX, dependency-check, osv-scanner and bomber where the release has scanner files, each streamed through the proxy of epic 02 with the `Download` API. Then "Other measured releases of X", each a link to its release page, D-212.
- Each finding row: id, severity, "only with X", "through N products", "through M of N products", "bundled inside X", "in the frontend bundle Vaadin publishes" or "we could not establish which product brings this", and a dot per product that is on, D-208. Opening it loads the release's record lazily and shows the summary, What it means for your application, What Vaadin says, What you can do today, The fix, the routes and Read the advisory, D-213. Groups cap at 150 with the remainder counted. Grid texts localise through the Grid i18n API.
- App extras kept, D-212: the title and close button at the top, the switch "Without Spring Boot", the sentence of five counts, the build only group after both halves, and the link to the head release's page.

## Edge cases

| Scenario | Behaviour |
| --- | --- |
| A product nothing anywhere arrives through | Not offered, unless it is fixed, cannot be had, or the reader decided about it |
| An `?off` name matching no product on this data | Dropped from the address, rule 6.2.7 |
| An `?off` naming exactly the default | Written away |
| Storage empty or unreadable | The default, silently |
| A line measured without Spring only | The Spring switch explains itself on that line |
| The shape without Spring not measured on a series | The series reads Not assessed yet and its panel says the second scan has not been made |

## Acceptance criteria

### AC1: The filter is the source filter
- [x] Off only addressing by registry name, unknown names dropped
- [x] A chip's count is the advisories through it, and does not move when it is turned off
- [x] A finding with two products needs both off to hide
- [x] The no product finding never hides

### AC2: Rows and groups
- [x] Badges match the fixture per line
- [x] Group open state persists across a reload, merged
- [x] The 150 cap counts its remainder

### AC3: Search
- [x] `?q` narrows rows and survives navigation
- [x] A full version typed lands on the release page

### AC4: Downloads
- [x] Each file fetches with its content type through the proxy

## Test cases

| Id | Given | When | Then | Tier | Verified by |
| --- | --- | --- | --- | --- | --- |
| EXP-01 | The front fixture | Opening findings as team | One row per line, badges match | browserless | `ExplorerBrowserlessTest` |
| EXP-02 | Two chips off | Reading the address | `?off` names them and the default's discontinued product, nothing else | browserless | `ExplorerBrowserlessTest` |
| EXP-03 | A finding of ours through two products | One off | Still shown; both off, hidden and counted as switched off | browserless | `ExplorerFilterBrowserlessTest` |
| EXP-04 | A chip's count | Turning it off, then searching | The count stays, then narrows to what the search names | browserless | `ExplorerFilterBrowserlessTest` |
| EXP-05 | Groups opened, reload | Reopening the line | The same groups open | browserless | `ExplorerBrowserlessTest` |
| EXP-06 | A version typed in search | Submitting | The release route | browserless | `ExplorerBrowserlessTest` |
| EXP-07 | The six download links | Fetching each | 200 with the expected content type | testbench | `ExportsProxyIT` |
| EXP-08 | Spring starter chip off | Reading the line | The without Spring measurement renders, with no Spring Boot badge | browserless | `ExplorerFilterBrowserlessTest` |
| EXP-09 | A series, an id, nothing, a number nobody follows | Searching each | The note and the lead in the prototype's words | browserless | `ExplorerBrowserlessTest` |
| EXP-10 | The "?" | Pressing it | The legend with its four sections | browserless | `ExplorerBrowserlessTest` |
| EXP-11 | A finding Spring Boot pins | Reading its series | It is on the Spring Boot badge and in the With Spring Boot half | unit | `ExplorerRulesTest` |

## Still open

Written from the code at the close of the prototype alignment, 2026-11-22. Each line names where the code shows it and the decision or draft behind it.

- Every control is a navigation and a history entry, where the prototype replaces the address in place, `ExplorerView.address`, D-210, D-98.
- A full version goes to its release page measured or not, where the prototype answers an unmeasured one in the note, `ExplorerView.submit`, D-209; so the prototype's "You asked for X. This answers for Y" note is never reached here.
- `?off` addresses products by registry name; switch to the short codes when `module_codes` lands in `data/front.json`, data need in `FEEDBACK-25.4.md`.
- The texts of an opened finding stay whole where the prototype cuts them with "more", and its routes keep this application's layout rather than the prototype's arrows, `FindingDetail`, D-213.
- The row sentence counts what nobody has assessed over the same findings as the groups under it, where the prototype's sentence leaves out findings that arrive only through a frontend package and so prints fewer than its own group, 6 over 11 on 24.9.21, `SeriesParts.sentence`, `ExplorerAnswer.stateFindings`, D-224. An observation about vaadin/cve-status in `REPORT.md`, not a Vaadin finding.
- The build only group after both halves is an app extra: the prototype puts `build` findings in neither half, D-205, D-212.
- The 150 cap is proven by `ExplorerRulesTest` alone: no fixture group reaches 150.
- The Master Detail Layout of the 25.2 coverage is no longer used anywhere since D-204; US-8.1 keeps its tasks for what they ask, the rows and the overlaying drawer, which the App Layout now gives.
- The release page draws this page's series row for one release, its groups by the same halves as its badges and its products in a left panel of its own, D-216; the two pages share the choice of products.
- Workaround to remove when browserless-test#29 is fixed, draft 20b: the tests open item details with `setDetailsVisible`, `ExplorerBrowserlessTest`.

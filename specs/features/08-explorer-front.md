# Feature 08: Explorer front door

## Overview

The security team's reading: every maintained series, narrowed by the products an application actually uses. The behaviour contract is sections 6.2 and 6.5 to 6.7 of the source requirements, and the layout and the answers are the prototype's, compared past its gate on 2026-11-22 in `specs/reviews/prototype/explorer.md`. Covers T2 to T6.

## Behaviour

### The layout

- The internal shell's left panel holds the destinations at its top and, on the explorer, the filter and the products under them, D-237, superseding D-203. An opened series has no close button of its own; its chevron shuts it, D-238. Below the App Layout's breakpoint it overlays, which the component gives for free, as the prototype's does below 800 pixels.
- The bar over the content holds the drawer toggle and the H1 "Vaadin security status". The trail, the measured chip, the colour scheme button, the Assistant button and Sign out follow it as app extras; the last three are icon buttons, each with a tooltip and an accessible name, D-247, D-248. The left panel opens with the application's icon and the name "CVE Status", then the team's side navigation, D-237, D-245, D-252. On the explorer, the filter and the products sit under the navigation as a card of their own, D-251.
- The content: a round "?" at the top right, the lead under a search, the heading "Series (10 maintained)", or "Series" under a search, and one row per maintained series. Each row is a disclosure opened in place under itself, one at a time; the opened series is in the address as `?open=<line>`, D-204.
- The "?" opens the legend on the right, "What the words on this page mean", with the prototype's sections: The badge on a series, The groups inside a series, How far a verdict was taken, What this page is measuring. Close and Escape shut it, D-211. The "?" hides while the legend is open.

### Search

- "Filter by advisory, version or dependency": one field with a clear button and a primary button showing a magnifying glass, named "Filter" for assistive technology. The search is held in `?q` and submitted on Enter, on leaving the field, or with the button, D-209, D-249; clearing the field clears the search. A number is read as a version first and keeps the series it names, then an advisory id, then an advisory no series carries any more, read from `history`, then a coordinate of `packages`, D-209.
- The note under the field says what was found in the prototype's words: "Showing N advisories matching X.", "Showing what arrives through a coordinate matching X: N advisories.", "Showing the X series and nothing else.", the series that last carried an advisory no series carries any more, "We do not follow X. ..." for a number nothing matches, "We report nothing under that on any series we follow." for a name. Under a number nobody follows the hint "That says nothing about what it carries: ..." follows, and under a name nothing matches, "That is not the same as it being harmless. ...".
- The lead over the series shows only under a search. When rows match it reads "All N maintained series match X: ..." or "M maintained series of N match X: ...", followed by "K are vulnerable", "K we have not assessed yet" and "K are not affected", leaving out a state with none. When nothing matches it reads "We follow N maintained series and X is not one of them." for a number and "None of the N maintained series carries anything matching X." otherwise. Under an advisory or coordinate search, a series with no matching finding is not listed.
- For an advisory no series carries any more, the note lists up to eight such advisories as "L carried this up to V. It is gone in F: see F." per series, or "The newest release of that series no longer reports it.", then the hint "If your own release is at or below the one named, you have it, and updating within the series is what removes it.", followed by "N other advisories matching this is in the same state." when more matched.
- A full version typed goes to its release page, the source shortcut, D-103.

### Products

- "Narrow the list to your application", with the prototype's explanation on a mark beside it. One chip per product of the profile's `on` and `off` lists that `module_names` knows: what comes with Vaadin first, drawn ticked and still; then the choices; then what Vaadin no longer develops, marked "discontinued". A product nobody chooses, a frontend package, is no control and always counts as on, D-206.
- Everything is on except what Vaadin no longer develops. The address names what is off, `?off=<name>,<name>`, by product name until the short code export lands, per the data need recorded in `specs/FEEDBACK-25.4.md`; no `?off` is the default and a bare `?off` is everything on. The selection is remembered through web storage and restored only when the address carries no selection of its own, rules 6.2.4, 6.7.3, 6.7.6.
- The box heading the list, beside "Product" and "Advisories", is ticked when every product counted by default is on, a dash when some are, empty when none. Ticked, it turns them all off, "Count only what comes with Vaadin"; otherwise it goes back to the default, "Count every product".
- A finding of ours is the reader's when any product it arrives through is on, or is no control, and always when no product is recorded, rules 6.5.11 and 6.5.12.
- A chip's number is how many different advisories on the series shown arrive through it, asked as if it were on, D-207. Each chip says on hover what it does, in the prototype's words.
- Turning the Spring Boot starter off reads every series on its measurement without Spring. The products that need Spring cannot be had there and are drawn off and still. An opened series then says "Measured on the application without Spring, which resolves its own versions rather than the ones a Spring Boot parent pins." That sentence stays; the one D-266 removed is the note that a line was measured without Spring only.

### The series rows

- One row per maintained series, a card whose 4 pixel start edge takes the colour of its Vaadin state: red for Vulnerable, amber for Not assessed yet, green for Not affected. Its chevron sits at the height of the first line, D-250. On one wrapping line come: the series in the accent text colour; the release in bold; "scanned <date>" in small secondary text, with what the date means on hover; the badge "Vaadin: <state>"; the badge "Spring Boot: <state>" when the series carries any finding that is Spring Boot's and the shape read is the one with Spring; then the sentence in secondary text. Under them, on a line of their own aligned to the end, is one ring per product that is on, in catalogue order. Every ring is the same width, so a product keeps its column on every row. A ring holds the count of advisories that arrive through it; it is coloured from the prototype's palette of six by catalogue position when the count is above nought, and grey with its nought otherwise, D-207. On hover a ring says "N findings here arrive through X. ..." or "Nothing here arrives through X. ...". The badges are small filled pills, the word in the state's colour on its tint, and the Spring Boot badge adds a dashed edge.
- The two halves, D-205: the Vaadin badge answers for the findings Vaadin can move, owner `vaadin` on a version Spring Boot does not pin, through the products on; the Spring Boot badge for Spring Boot's own findings and the versions it pins. A state is Vulnerable, Not assessed yet or Not affected, each with the prototype's explanation on hover. On the shape without Spring nothing pins a version, so every finding whose owner is `vaadin` is in the From Vaadin half, including those Spring Boot pins on the shape with Spring; that shape has no Spring Boot badge and no With Spring Boot half.
- The sentence: "N findings here are a real risk. Each one says what to do.", with ", and M more nobody has assessed yet" when there are; on a series clear of ours, "N findings arrive with your Spring Boot" when that half has anything open.

### An opened series

- From Vaadin, when the other half has anything: "a real risk" and "not assessed yet", open; "answered, nothing to do", shut; each with the prototype's explanation on hover, worst first. "Nothing on this release is waiting for you: ..." when neither of the first two has anything.
- The frontend bundle sentence, the untraced sentence, and the sentence for what only a switched off product brings.
- With Spring Boot: "N findings on this series come with Spring Boot rather than with Vaadin, measured against X. ... See what Spring Boot brings", linking the Spring Boot page, then its own three groups.
- An opened series reads in this order: "N findings: A to act on, B not assessed yet, C from Spring Boot, D only in the build, E answered."; the sentence of the shape without Spring when it is read; From Vaadin and its groups; the sentences above; With Spring Boot with its sentence, "See what Spring Boot brings" and its groups; "Only in the build (N)", shut, with its note "Only build tooling carries these. Nothing deployed contains them."; "For your own tooling:" with JSON and CSV always, and VEX, dependency-check, osv-scanner and bomber where the release has scanner files, each streamed through the proxy of epic 02; "Other measured releases of X:", each a link to its release page; and last the link "Release page of X", D-212. "a real risk" and "not assessed yet" start open and the rest start shut. A group opened or shut by hand is remembered in the browser's local storage by its kind, such as RISK or SPRING_ANSWERED, for every series and for the release page too, D-214.
- Each group lists its findings as the prototype does, one disclosure per finding with a hairline between them and no header row, D-264, superseding the grid of D-208: a chevron, the advisory id, the severity as a small lowercase pill tinted like its group and left out when the record has none, then the words of D-208: "only with X" or "only with X or Y" when every product that brings it is one a reader chooses, "through N products" when all of them are on, "through M of N products" otherwise, "bundled inside X", "in the frontend bundle Vaadin publishes" or "we could not establish which product brings this". At the end comes a filled dot per product that brings it and is on, in catalogue order and in its ring colour. A dot's tooltip reads "Arrives through X." followed by the paths that product brings it by, one per line with the steps joined by "->": the first four, then "and N more like these".
- Opening a finding reads the release's record the first time any finding of that release is opened, D-104. It draws the package coordinates as code pills with the versions the shape being read resolves, then the record's summary as a bold title. Then comes one flat panel with a rule down its left edge, holding the fields the record carries, each under an uppercase label: What it means for your application, What Vaadin says, What you can do today, and The fix, which reads "Fixed in X" or else the record's own words on what the fix waits for. A field longer than 240 characters is cut after the last sentence that ends before character 240; if that sentence ends before character 60, it is cut at the last whole word before 240. A cut field ends with a "… more" link that shows the whole text in place, D-264. Then the routes: one path per line in monospace, steps joined by "›", the coordinate named above its paths only when there are several, and "No route recorded." for a coordinate with none. Last, "Read the advisory", which opens the advisory's record in a new tab. When the release's record is not published, the finding says "The record of X is not published, so there are no details to show." and shows the link alone. Groups cap at 150, with "N more not shown." under them.
- App extras kept, D-212: the sentence of five counts, the build only group after both halves, and the link to the head release's page. There is no title, no close button and no "Without Spring Boot" switch, D-238, D-265, D-266: the row names the line, its chevron shuts it, and the starter's chip chooses the shape.

## Edge cases

| Scenario | Behaviour |
| --- | --- |
| A product nothing anywhere arrives through | Not offered, unless it is fixed, cannot be had, or the reader decided about it |
| An `?off` name matching no product on this data | Dropped from the address, rule 6.2.7 |
| An `?off` naming exactly the default | Written away |
| Storage empty or unreadable | The default, silently |
| A line measured without Spring only | Reads the one measurement it has, whatever the starter's chip says, D-266 |
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
| EXP-12 | A field longer than 240 characters | Opening its finding | Cut after its last sentence inside 240, or at a word, with "… more" | unit | `BriefTest` |
| EXP-13 | A finding of 25.3 opened | Reading it | The record's fields under their labels, absent ones left out, Read the advisory | browserless | `ExplorerBrowserlessTest` |
| EXP-14 | A cold cache and a dead upstream | Opening findings | The one outage sentence | browserless | `ExplorerOutageBrowserlessTest` |

## Still open

Written from the code at the close of the prototype alignment, 2026-11-22. Each line names where the code shows it and the decision or draft behind it.

- Every control is a navigation and a history entry, where the prototype replaces the address in place, `ExplorerView.address`, D-210, D-98.
- A full version goes to its release page measured or not, where the prototype answers an unmeasured one in the note, `ExplorerView.submit`, D-209; so the prototype's "You asked for X. This answers for Y" note is never reached here.
- `?off` addresses products by registry name; switch to the short codes when `module_codes` lands in `data/front.json`, data need in `FEEDBACK-25.4.md`.
- The prototype's "Same answer as X" on a second finding with the same workaround is not built: each finding shows its own text, cut as D-264 says, `FindingDetail`.
- The row sentence counts what nobody has assessed over the same findings as the groups under it, where the prototype's sentence leaves out findings that arrive only through a frontend package and so prints fewer than its own group, 6 over 11 on 24.9.21, `SeriesParts.sentence`, `ExplorerAnswer.stateFindings`, D-224. An observation about vaadin/cve-status in `REPORT.md`, not a Vaadin finding.
- The build only group after both halves is an app extra: the prototype puts `build` findings in neither half, D-205, D-212.
- The 150 cap is proven by `ExplorerRulesTest` alone: no fixture group reaches 150.
- The Master Detail Layout of the 25.2 coverage is no longer used anywhere since D-204; US-8.1 keeps its tasks for what they ask, the rows and the overlaying drawer, which the App Layout now gives.
- The release page draws this page's series row for one release, its groups by the same halves as its badges and its products in a left panel of its own, D-216; the two pages share the choice of products.
- Workaround to remove when vaadin/web-components#13535 is fixed: outside the overlay the drawer is padded a whole 6 pixels, because Aura pads it from an unrounded 1.5vmin and the app icon at its top blurs on a 1x screen, `styles/team/shell.css`, draft `specs/issues/28-aura-app-layout-inset.md`.

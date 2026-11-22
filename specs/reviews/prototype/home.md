# Prototype comparison: the customer shell, the home page and the lookup

Compared on 2026-11-22, both sides reading the same published data, `generated_at` of that day: the prototype at https://vaadin.github.io/cve-status/ signed in through its client side gate with the customer account the person gave, and this application in development mode on live data, without the model key. Both at 1920 by 1080 and at 390 by 844. Screenshots: `prototype-home.png` and `prototype-home-390.png` for the prototype, `app-home-before.png` and `app-home-390-before.png` for the application before this change, `app-home.png` and `app-home-390.png` after it, and `app-version.png` for an answer page under the new shell.

The comparison was written before anything was changed. The Action column is one of: move, add, transcribe wording, keep as app extra, data not published. A row whose two sides already agree says so in its Action.

## The shell, on every customer route

| Prototype | App before | Action |
| --- | --- | --- |
| Header trail: Vaadin, then "My account" linking home, then "Security status" linking home, then the leaf of an answer page (the version, the advisory id, "Advisory" for an empty CVE page, "Scanner report") | Trail: Vaadin linking vaadin.com, "Security status", the leaf from the route hierarchy. No "My account" | Add "My account" linking home in the customer shell, D-07 superseded by D-162. The leaf stays the route's own title, and "Security status" on home stays the current page rather than a link: keep as app extra |
| Right of the header: a chip "Measured 2026-11-22" with the hover text "These are the real measurements of that date, not sample data" | Chip "Measured 2026-11-22", no hover text | Transcribe wording: the hover sentence, D-163 confirms D-10 and D-29 |
| A chip "Enterprise", static in the prototype's markup, not read from any document | Absent | Data not published: the application does not know the reader's subscription, a data need row in `specs/FEEDBACK-25.4.md` |
| Nothing else in the header | The dark mode switch, and Sign out for a signed in session | Keep as app extra, right of the measured chip |
| One centered column of 912 pixels, x 504 to 1416 at 1920 | The page across the full width, the lookup in a band of its own under the bar, 896 pixels wide at the left | Move: one centered column of 57rem for everything under the bar |
| H1 "Security status" at the top of every page, the answer pages included | An H1 per view, from the route's title: "Security status" on home, "Vaadin 24.9.21", "Scanner report" and so on elsewhere | Move: the shell draws H1 "Security status" once for every customer route. A view's own title stays under the lookup as the answer's H2: keep as app extra |
| Purpose paragraph: "Find out whether your Vaadin version is safe, and what to do if it isn't. Every supported version is scanned continuously and every finding is reviewed by the Vaadin team." | Absent | Transcribe wording, in the shell |
| The lookup in a tinted box under the purpose, on every page, above the answer | The lookup under the app bar, above the page heading | Move: into the column under the purpose, D-38 superseded by D-164 |

## The lookup

| Prototype | App before | Action |
| --- | --- | --- |
| Three tabs: By version, By CVE, Upload scanner report | The same three tabs, the same words | None, already equal |
| A tab change navigates: By CVE opens `#cve/`, an empty answer page; Upload opens `#scan` | A tab change shows the tab's fields and does not navigate, D-39 | Keep as app extra: the home page and an answer stay under the lookup while the reader picks a tab. D-165 confirms D-39 against the prototype |
| By version: label "Your Vaadin version", a search field with the placeholder "e.g. 25.3.5", a Check button | The same label, a combo box over the measured versions with the same placeholder, a Check button disabled while empty, a question mark opening "How to enter your version" | None for the label, placeholder and button. The combo box, the disabled state and the help stay: keep as app extra |
| Tip under the fields: "Tip: link your project and we'll remember your version and tell you when its status changes." | Absent | Not added: it promises project linking and change notices, which this application does not have, D-166 |
| By CVE: "CVE numbers, one or more" with a question mark opening "How to use this lookup", and "Your version" beside it | The same labels and help | None, already equal |
| Ids that do not parse are dropped without a word | "Not recognised: foo" under the lookup, D-48 | Keep as app extra, P-05 resolved |
| Upload: drop zone "Drop your report here, or choose a file", the formats line, a help mark, "Choose a file" | The same, epic 06 | None for the drop zone, the button and the help. The prototype's formats line ends "Read in this browser.", which is false here, where the file is read on the server, so it is not transcribed; the app's own help says where the file goes |

## Home, under the lookup

| Prototype | App before | Action |
| --- | --- | --- |
| H2 "Supported versions at a glance" with the aside "Status of the latest maintenance release in each minor" right aligned on the same line | The same H2, the aside as a paragraph under it | Move: the aside beside the heading, wrapping under it on a phone |
| Cards four to a row in the column, one per followed line, newest first: 25.3, 24.10, 24.9, 24.7, 24.4, 23.6, 14.14, 8.32, 7.7 | The same nine cards, newest first, nine to a row across 1872 pixels | Move: the grid now fills the 912 pixel column, four to a row |
| Card: minor, Extended mark, state icon at the right, state word, action line, Scanned date. The card tinted green when secure, red when findings are open | The same parts in the same order, a neutral card with a colored word and icon | Add the tint by state, in the application's own status colors |
| Extended mark hover: "Extended maintenance. Free maintenance for Vaadin 24 ended on 2026-06-16, and every release since then needs a subscription. Whether yours covers it is between you and Vaadin: we do not know what a given customer holds." | Hover is the label alone, "Extended maintenance", D-44 | Transcribe wording: the sentence is built from the line's published `support.label`, `support.major` and `support.free_ended`, so it is data, D-167 supersedes D-44. The label alone remains when either of the other two is absent |
| Extended mark on eight of nine cards | The same | None, already equal, P-06 resolved |
| State words: Secure, 1 finding open, 25 findings open | The same words on the same cards | None, already equal |
| Action lines: "On 25.3.5 or later", "Newest is 24.7.19" | The same lines on the same cards | None, already equal. No card shows Upgrade to X on this data, so the order of D-46 is not answered |
| A card with no summary row | Not observable: every line has one on this data | D-45 stays, not answered by the prototype |
| Card link: `#version/<head>`, 24.10.14 for 24.10 | `version/24.10.14` | None, already equal |
| Paragraph "Running an older maintenance release? Enter it above. If we have not measured it, the answer is to move to the latest one in your minor." | The same paragraph | None, already equal |
| H2 "Recently reviewed" with the link "All 534 advisories" at the right | The same | None, already equal |
| Eight rows in a bordered box: id, the impact sentence on one line cut with an ellipsis, the verdict words, the date, a chevron | Eight rows, the same ids in the same order, the impact sentence whole, the verdict in a chip, the date, no chevron, no box | Add the chevron and the box. The sentence stays whole: keep as app extra, a cut sentence hides words a reader may need |
| Verdict words: "affects you" in a red chip, every other verdict in plain bold words | Every verdict in the same chip | Move: the risk verdict in the error variant of the badge, the others in the plain one. P-06 resolved |
| Box "Scanner flagged Tomcat, Spring or logback?" with "Those are managed by Spring Boot. Check your version above and we'll show them separately with the fix." | The same box and words | None, already equal. D-168 confirms D-47 |

## At 390 pixels

The prototype stacks the cards one to a row, keeps the tabs reachable with Upload scanner report wrapping to a second line, lets the trail collide with the measured chip, and lets the recent rows run off the right edge with the dates cut. The application stacks the cards, scrolls the tab strip sideways behind an arrow, folds the middle of the trail into the breadcrumbs' overflow menu, puts the measured chip and the switch on a row of their own, and wraps a recent row's parts, with no horizontal scroll of the page: `scrollWidth` 390 at a 390 viewport after the change. The application's behaviour at that width is kept.

## Verified after the change

In development mode on live data, in a tab of an isolated Playwright context, with real clicks and typing: 24.9.21 typed in the version field and Check clicked, the version page opened under the same H1, purpose and lookup with 24.9.21 still in the field; 24.7.19 asked the same way, then My account clicked in the trail, then By CVE and By version clicked: home again, the address unchanged by the tab clicks, 24.7.19 still in the field, one H1 on the page. The CVE, advisories and scanner report screens were opened by address and render in the column with their title as the answer's H2: `.playwright-mcp` shots, not kept. The advisories grid is narrower in the column and cuts long package names with an ellipsis, as it already did at smaller windows.

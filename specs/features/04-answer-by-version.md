# Feature 04: Answer by version

## Overview

The screen a support reply links at: one version, one verdict, and everything a reader does about it. The behaviour contract is section 6.3 and 6.11 of the source project's requirements, reproduced with Vaadin components. Covers V1 to V10.

## Behaviour

The screen follows the prototype's distribution, compared past its gate in `specs/reviews/prototype/version.md`. Under the customer shell's H1, purpose and lookup, in the centered column of D-170, the answer's title "Vaadin X" is its H2, kept for screen readers and visually hidden, D-169, D-270; the verdict card's headline names the release. Everything under it is an H3: the card's headline and each section heading, D-174.

### The verdict card

- A card tinted by its state, in the application's status colors, D-175: the state mark and the state word in capitals, secure, fix available, unresolved or not measured, then the headline and the why sentences, each in the wording the source page settled on. Secure says explicitly that it includes findings judged not to apply.
- The card follows the prototype's look, D-269: tinted by its state, with a 32 pixel filled circular mark, a tick for secure, an up arrow for fix available, an exclamation for unresolved, a question mark for not measured; the state word in capitals beside the mark, then a 28 pixel headline. Every answer table, Spring Boot, discontinued, ruled out and all supported versions, is drawn as the prototype's grid: a rounded frame on white, a quiet 12 pixel header, 14 pixel rows split by hairlines, plain links and small filled state chips. The Spring Boot note is a grey panel with its link at the right.
- Badges, in this order, each only where the data carries it: "Prerelease" for a prerelease version; "Maintenance release" and "Same minor: L" together when the upgrade stays on the reader's line; the line's support label; and, for an extended line, "Subscription required from X" when that release is on the reader's line, else "Free maintenance ended D". A badge the data does not support is not rendered: the prototype's "No API changes" is not, because nothing measures it, D-178.
- A version with exactly one unfixed finding draws that finding inside the card, rule 6.11.9: id, severity, owner chip, the vulnerable chip, and the two columns Do this now and Fix.
- Actions, in this order, each only where it applies: "View X release notes", a link to the target release's notes in the right repository per major from `release_repos`, only beside an upgrade, D-52; "What it fixes", a text button shown only when the fixes section is, scrolling to it through the typed scroll call 25.4 adds to the element API; on an unresolved card only, "Notify me when it is fixed" with a bell, and "Or move to X, which is secure", reading "Or move to X, which is secure, since no L release is" when the move leaves the line. The secure move follows the source rules 6.11.8 and 6.11.23: prefer the reader's line, stay inside their major, never older, never a prerelease.
- Notify me when it is fixed opens the not built dialog saying what it would take, per rule 6.11.15, in the prototype's title and first sentence and sentences of ours after them, D-55, D-183. It stays unbuilt here on purpose: building it means standing up subscriptions, which is out of scope and said so.
- The scan line at the end of the actions: the absolute date, then today, yesterday or N days ago in the reader's timezone once it is known, D-59, an extra over the prototype's date alone, D-184.

### The sections, top to bottom

1. **Findings with no fix yet**, when there are two or more, with the count aside: one card per finding with id, severity, the owner chip, the vulnerable and workaround chip, the do this now and fix columns, tracking PR links, and the three layers: what this means for you, affected modules and the fix, full advisory and technical evidence. The first card, or the one `?open` names, has its first two layers open. Prose renders through Markdown, absent fields are omitted, D-185. The owner chip reads "Vaadin issue" for `vaadin`, "Dependency issue" for `upstream`, and is absent for `unknown`, D-51, D-180. Cards are ordered worst severity first. Opening a card's first layer by hand writes `?open=<id>` into the address and shutting it removes the flag, without scrolling. The chip reads "Vulnerable", or "Vulnerable, workaround available". A tracking pull request is named repository#number, and is a link only when its address is https; otherwise it is plain text, D-262.
2. **Not assessed yet notice**: a chip, the sentence, and up to twelve ids separated by commas then a counted remainder, D-177. When the advisories were published is not said: the data does not carry it, D-179.
3. **What X fixes for you**, X the upgrade, with the aside "N of the M findings on V", drawn only when there is an upgrade and it clears something: one row per cleared advisory, worst severity first, each a link to `/cve/<id>/<version>`: id, severity, owner chip, the package's short name and the one line impact joined by a middle dot, a chip "Fixed in X" or "Gone by X", and a chevron. At most 25 rows, then "and N more, all cleared by the same upgrade".
4. **Spring Boot**, drawn only when the version has findings Spring Boot brings or pins, in the two shapes of rule 6.11.6. With no `?boot` the shape is the table when the version has at least one finding with no fix yet, and the note otherwise; `?boot=1` forces the table and `?boot=0` the note. The note is a grey panel headed "Your scanner may also flag Spring Boot dependencies", its sentence with the managed property in code type, and the link "See Spring Boot findings". The table shape is headed "Spring Boot findings" with the count and "Hide them", then the tip with the managed property in code type, D-176, the pinned count, the release Spring Boot declares, and the supply sentence, commercial through Broadcom or ended, or else the newest release of the line; then the table of dependency (the first two packages), CVE and how to fix, "Upgrade Spring Boot to X" or "Upgrade Spring Boot, or override the managed version", at most 12 rows, then "N more arrive the same way.", with "The ones listed are the ones we judged a real risk." when risks are listed first.
5. **Findings only a discontinued product brings**, with the count aside: the tip naming the product in code type, a show control behind `?disc`, and the table whose fix column says to override the version in your own build, D-176.
6. **Details for the single unfixed finding**, only when there is exactly one: its three layers, the first two open.
7. **Findings we looked at and ruled out**, with the count aside: the tip, a show control behind `?ruled` reading "Show them" or "Hide them", and the table of advisory and the catalogue's label for the reason. The table lists the ruled out advisories and those found only in the build, worst first; a build only one reads "In the build classpath only, not in what you deploy", and an advisory with no catalogued reason has its id span the row, D-60.
8. **All supported versions**, with "Status is for the newest release of each minor" aside: minor, newest release with "safe from X" when it differs from the newest, status chip, open count, maintenance, scanned. The followed lines, newest first; prerelease lines only with `?pre` or when the reader's own line is one; the reader's line reads "L · yours".

### Addresses and scroll

- Every section toggle writes its flag through the one navigation helper, so the address always carries the whole answer. A section control is a link to the address it writes, and each click is a history entry, D-58.
- The viewport goes to the top when the version changes and stays put when a flag changes, rule 6.11.7.

### Not measured

- A version nobody measured is answered in place, never by a redirect. The card shows the state word Not measured with a question mark, the headline "We have not measured X", one sentence naming the nearest measured release of the line, of the major, or the oldest measured, and the action "See Y instead" linking to it. No section follows except All supported versions. Below every followed line the app names the oldest release it measures, an extra over the prototype, D-57, D-182.

## Edge cases

| Scenario | Behaviour |
| --- | --- |
| A prerelease version asked directly | Answered, with its kind named, whatever `?pre` says, because a typed question is always answered |
| A version below every followed line | The not measured answer with the oldest measured release named |
| An advisory list longer than the cap | The cap plus a counted remainder, rule 6.6.10 scaled to this screen |
| `?open=<cve>` naming a finding on this version | Its accordion opens, the page scrolls to it, and the card carries a highlight style that fades after about two seconds, so the reader sees what the link was for |
| `?open` naming a finding not on this version | Ignored, nothing invented |
| The upgrade target equals the asked version | The card is secure or unresolved by state; the fixes section does not render against itself |

## Acceptance criteria

### AC1: The card tells the state's truth
- [x] Each of the three states renders its wording and badge from the fixture that carries it
- [x] The single unfixed finding renders inside the card, and two render as the list
- [x] The secure move offer never leaves the major and names the minor when it leaves the line

### AC2: Sections match the data
- [x] Every section renders only when its list is non empty
- [x] Absent verdict fields are omitted, never empty
- [x] The caps count their remainders

### AC3: The address is the whole answer
- [x] Toggling boot, ruled, disc and open rewrites the query without scrolling
- [x] A pasted address with flags opens with those sections open
- [x] Changing the version scrolls to the top

### AC4: Not measured is answered in place
- [x] An unmeasured version renders the sentence, the offer and the table on its own route

## Test cases

| Id | Given | When | Then | Tier | Verified by |
| --- | --- | --- | --- | --- | --- |
| VER-01 | The secure fixture version | Opening it | Secure card, its sentence, no fixes section | browserless | `VersionViewBrowserlessTest` |
| VER-02 | The fix available fixture | Opening it | The upgrade named, the fixes rows match the fixture | browserless | `VersionViewBrowserlessTest` |
| VER-03 | The unresolved fixture with two stuck findings | Opening it | Two finding cards with their layers | browserless | `VersionViewBrowserlessTest` |
| VER-04 | The unresolved fixture with one stuck finding | Opening it | The finding inside the card and the details section at the foot | browserless | `VersionViewBrowserlessTest` |
| VER-05 | `?boot` toggled | Clicking the Spring Boot link | The table shape, the address updated, no scroll jump | browserless | `VersionViewBrowserlessTest` |
| VER-06 | `?open=<cve>` on its version | Opening the address | That accordion is open and in view | browserless | `VersionViewBrowserlessTest` |
| VER-07 | An unmeasured version | Opening it | The not measured answer in place with the nearest offer | browserless | `VersionViewBrowserlessTest` |
| VER-08 | A fixture with an absent workaround | Opening the finding | No workaround row at all | browserless | `VersionViewBrowserlessTest` |
| VER-09 | The all versions table | Rendering for a version on line X | Line X marked yours | browserless | `VersionViewBrowserlessTest` |
| VER-10 | The What it fixes action | Clicking it | The viewport moves to the fixes section | testbench | `ScrollActionsIT` |
| VER-11 | The unresolved fixture with one stuck finding | Opening it | One H1 from the shell, the answer's title as the only H2 and visually hidden, the card's headline and the sections as H3 | browserless | `VersionViewBrowserlessTest` |
| VER-12 | `?boot=1&disc=1` on a version with both | Opening the address | The managed property and the product in code type, the discontinued fix column in the prototype's words | browserless | `VersionViewBrowserlessTest` |

## Still open

Written from the code at the close of the prototype alignment, D-174 to D-185 included. Each line names where the code shows it and the decision or draft behind it.

- View release notes is offered only beside an upgrade, and links the notes of the target release, `VersionView`, D-52.
- The three layers are three separate `Details`, `FindingCard`, not an `Accordion`: several can be open at once. US-4.2 keeps its tick on that basis, D-161.
- The third layer shows severity, packages, depth and date, plus a link to the advisory screen; the verdict's `justification`, `commentary` and `affects` are not drawn, `FindingCard`.
- A fixes row with no one line impact shows the catalogue's applicability label in its place, `VersionView`.
- The Spring Boot supply sentence is written only for `commercial` and `ended`, and only when both release numbers are present; an `open` supply shows the newest release of the line instead, `VersionView`.
- The all versions table leaves a cell empty for an absent open count, support label or scan date, `VersionView`, and the Spring Boot and discontinued tables do the same for an advisory with no catalogue entry. `AbsenceSweepBrowserlessTest` holds this to exactly those cells, D-159.
- An unresolved version with no upgrade and nothing stuck is headed with our own wording, "N findings are not assessed yet", D-53, P-08. The prototype could not answer it: no version on the published data is in that state.
- The prototype's "No API changes" badge and the publication date on the not assessed notice are not drawn: nothing published carries them, D-178, D-179, both data need rows in `specs/FEEDBACK-25.4.md`.
- Where a verdict carries no workaround, fix text or tracking, the field is left out, where the prototype prints the missing field's name; the ruled out table shows the catalogue's label where the prototype prints the raw code, D-185.
- The verdict prose is drawn whole, as on the prototype, so on 24.7.19 the red card's actions sit low in the card, P-07. Text links under 24 pixels were not measured against the prototype and are as they were.
- At 390 pixels the versions table hides its Maintenance column and the page does not scroll sideways, where the prototype's does, `specs/reviews/prototype/version.md`.
- Workaround to remove when draft 14 is answered: the query parameters reorder flags, so `VersionAddress` writes a fixed order and the version tests compare the query as a map.

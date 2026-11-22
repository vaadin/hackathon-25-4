# Feature 04: Answer by version

## Overview

The screen a support reply links at: one version, one verdict, and everything a reader does about it. The behaviour contract is section 6.3 and 6.11 of the source project's requirements, reproduced with Vaadin components. Covers V1 to V10.

## Behaviour

The screen follows the prototype's distribution, compared past its gate in `specs/reviews/prototype/version.md`. Under the customer shell's H1, purpose and lookup, in the centered column of D-170, the answer opens with its title, "Vaadin X", as the H2, D-169. Everything under it is an H3: the verdict card's headline and each section heading, D-174.

### The verdict card

- A card tinted by its state, in the application's status colors, D-175: the state mark and the state word in capitals, secure, fix available, unresolved or not measured, then the headline and the why sentences, each in the wording the source page settled on. Secure says explicitly that it includes findings judged not to apply.
- Badges where the data carries them: prerelease, maintenance release, same minor, the support tier, subscription required from X, free maintenance ended X. A badge the data does not support is not rendered: the prototype's "No API changes" is not, because nothing measures it, D-178.
- A version with exactly one unfixed finding draws that finding inside the card, rule 6.11.9: id, severity, owner chip, the vulnerable chip, and the two columns Do this now and Fix.
- Actions: View release notes, linking to `release_repos` for the right repository per major, only beside an upgrade, D-52; What it fixes, scrolling to the fixes section through `Element.ScrollIntoViewJs`, the typed scroll call 25.4 adds to the element API; and, on a red card, Notify me when it is fixed and Or move to X which is secure, following the source rule 6.11.8 and 6.11.23: prefer the reader's line, stay inside their major, name the minor when the move crosses, never older, never a prerelease.
- Notify me when it is fixed opens the not built dialog saying what it would take, per rule 6.11.15, in the prototype's title and first sentence and sentences of ours after them, D-55, D-183. It stays unbuilt here on purpose: building it means standing up subscriptions, which is out of scope and said so.
- The scan line at the end of the actions: the absolute date, then today, yesterday or N days ago in the reader's timezone once it is known, D-59, an extra over the prototype's date alone, D-184.

### The sections, top to bottom

1. **Findings with no fix yet**, when there are two or more, with the count aside: one card per finding with id, severity, the owner chip, the vulnerable and workaround chip, the do this now and fix columns, tracking PR links, and the three layers: what this means for you, affected modules and the fix, full advisory and technical evidence. The first card, or the one `?open` names, has its first two layers open. Prose renders through Markdown, absent fields are omitted, D-185. The owner chip reads "Vaadin issue" for `vaadin`, "Dependency issue" for `upstream`, and is absent for `unknown`, D-51, D-180.
2. **Not assessed yet notice**: a chip, the sentence, and up to twelve ids separated by commas then a counted remainder, D-177. When the advisories were published is not said: the data does not carry it, D-179.
3. **What the upgrade fixes**, with "N of the M findings on X" aside: one row per cleared advisory, id, severity, owner chip, package and one line impact, fixed in or gone by, and a chevron, capped at 25 with the remainder counted.
4. **Spring Boot**, in the two shapes of rule 6.11.6, switched by `?boot`: the note with a show link, or the heading with the count and Hide them, the tip with the managed property in code type, D-176, the supply status sentence, open, commercial through Broadcom, or ended, and the table of dependency, CVE and how to fix, with the remainder counted under it.
5. **Findings only a discontinued product brings**, with the count aside: the tip naming the product in code type, a show control behind `?disc`, and the table whose fix column says to override the version in your own build, D-176.
6. **Details for the single unfixed finding**, only when there is exactly one: its three layers, the first two open.
7. **Findings we looked at and ruled out**, with the count aside: the tip, a show control behind `?ruled`, and the table of advisory and the catalogue's label for the reason.
8. **All supported versions**, with "Status is for the newest release of each minor" aside: minor, newest release with safe from X, status chip, open count, maintenance, scanned, the reader's line marked yours.

### Addresses and scroll

- Every section toggle writes its flag through the one navigation helper, so the address always carries the whole answer. A section control is a link to the address it writes, and each click is a history entry, D-58.
- The viewport goes to the top when the version changes and stays put when a flag changes, rule 6.11.7.

### Not measured

- A version nobody measured is answered in place: the card says we have not measured X, why, and See the nearest measured release, then the versions table. Never a redirect. Below every followed line the app names the oldest release it measures, an extra over the prototype, D-57, D-182.

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
| VER-11 | The unresolved fixture with one stuck finding | Opening it | One H1 from the shell, the answer's title as the only H2, the card's headline and the sections as H3 | browserless | `VersionViewBrowserlessTest` |
| VER-12 | `?boot=1&disc=1` on a version with both | Opening the address | The managed property and the product in code type, the discontinued fix column in the prototype's words | browserless | `VersionViewBrowserlessTest` |

## Still open

Written from the code at the end of epic 13. Each line names where the code shows it and the decision or draft behind it.

- View release notes is offered only beside an upgrade, and links the notes of the target release, `VersionView.verdictCard`, D-52.
- The three layers are three separate `Details`, `FindingCard.Layers`, not an `Accordion`: several can be open at once. US-4.2 keeps its tick on that basis, D-161.
- The third layer shows severity, packages, depth and date, plus a link to the advisory screen; the verdict's `justification`, `commentary` and `affects` are not drawn, `FindingCard.Layers`.
- A fixes row with no one line impact shows the catalogue's applicability label in its place, `VersionView.fixRow`.
- The Spring Boot supply sentence is written only for `commercial` and `ended`, and only when both release numbers are present; an `open` supply shows the newest release of the line instead, `VersionView.supplySentence`.
- The all versions table leaves a cell empty for an absent open count, support label or scan date, `VersionView.drawLines`, and the Spring Boot and discontinued tables do the same for an advisory with no catalogue entry, `VersionView.packages`. `AbsenceSweepBrowserlessTest` holds this to exactly those cells, D-159.
- An unresolved version with no upgrade and nothing stuck is headed with our own wording, "N findings are not assessed yet", D-53, P-08. The prototype could not answer it: no version on the published data is in that state.
- Workaround to remove when draft 14 is answered: `QueryParameters` reorders flags, so `VersionAddress.address` writes a fixed order and `VersionViewBrowserlessTest.assertAddress` compares the query as a map.

# Review: epic 07, advisories and cases

Compared on `/advisories` and `/cases` at 1920 by 1080, dark scheme, development mode, against the live data of 2026-11-21, and at 375 wide in a tab of its own. The reference, `https://vaadin.github.io/cve-status/`, shows its sign in gate, so `reference-gate.png` is the gate and nothing past it was compared, P-03. The source page has no full advisories list a customer reaches; the home's "All N advisories" link is the only door to this screen in either application.

Screenshots: `new-advisories.png`, `new-cases.png` (1920 by 1080), `reference-gate.png` (the gate). The phone width was measured, not saved: both pages are 375 pixels wide at a 375 pixel viewport, and the grid scrolls sideways inside itself.

## The support engineer

- The whole catalogue is one screen: 535 rows, worst first, each with severity, who fixes it, the package and the verdict in the customer's words. A ticket that names a CVE can be answered by finding the row, but there is no search box: the reader scrolls, or uses the browser's find, which sees only the rows the grid has rendered.
- There is no impact line. The feature asks for one and the index does not carry it, D-82. The engineer has to open the row to read what the advisory means.
- There is no date. "Oldest first" is not applied because nothing says when an advisory was published, D-81. Inside one severity the order is the id's text order, so `CVE-2026-102469` comes before `CVE-2026-8763`.
- The verdict pills are real links: a filtered list can be pasted into a ticket as `/advisories?verdict=risk`. The count on each pill is what it shows, and the line under the pills says "150 of 535 advisories shown".
- The right click menu copies the id, opens the record at cve.org or GitHub, and carries the id to the lookup with the reader's version filled in. It is invisible: nothing on the screen says a right click does anything, and on a phone a long press is the only way in. Every action it offers is also reachable without it, except Copy.
- The row is clickable as a whole and opens the answer by CVE, with a pointer cursor. A keyboard user reaches a row but Enter on it was not checked.
- The verdict chips are one color for every verdict. "affects you" and "scanner false positive" look alike at a glance; the home's recently reviewed rows draw them the same way.
- The cases screen reads as a checklist for a demo: three groups, each case in the catalogue's words, a count and a See one link that lands on a real example. An impossible case says what data it needs.

## The pedantic analyst

| Metric | Reference | New | Delta | Verdict |
| --- | --- | --- | --- | --- |
| Facts above the fold for the same question | Not measurable, sign in gate | 8 pills with counts, the total, 18 rows of 5 cells | Unknown | Not compared |
| Clicks from arrival to the verdict | Not measurable | 0 for the list; 1 to filter by a verdict; 1 to open a row's answer | Unknown | Not compared |
| Data area as a percentage of the viewport | Not measurable | The grid is 1280 of 1920 pixels wide and 760 of 1080 high, starting 386 pixels down under the lookup and the pills: 47 percent of the viewport | Unknown | Fail on its own terms: the right third of the screen is empty, as on every other customer screen |
| Base font size | Not measurable | 16 px | Unknown | Pass |
| Smallest click target | Not measurable | 34 px pills; 35.5 px rows; See one links 22 px before this review, about 30 px after the fix below | Unknown | Pass after the fix |

## Issues

### Critical

None found.

### Major

1. No impact column, D-82: the feature's sixth column is missing because the data does not carry it.
2. No way to find one advisory in 535 rows other than scrolling. The feature does not ask for a search, epic 11's grid AI is meant to fill this for signed in users only, and a customer gets nothing.
3. The context menu is undiscoverable: no hint, no menu button in the row.

### Minor

1. Verdict chips share one color.
2. At 375 wide the grid shows the id and the severity and scrolls sideways for the rest.
3. The pills wrap to five lines at 375 wide, pushing the grid below the fold.
4. The See one links were 22 px high: fixed in this review with padding, `.case-link`.
5. The group headings and the lead of the cases screen are wording of ours, P-14.

## Deliberate improvements

- Filter pills are links with the flag in the address, and the back button undoes a filter.
- The record link is a real anchor, so it opens in a new tab with no popup blocker in the way and no JavaScript sent.
- A case whose example stopped showing its state is moved to none today instead of linking to a screen that no longer shows it.

## Verdict

Pass, with the three major issues recorded for the polish pass and P-14.

# Review: the explorer front door

Reviewed on 2026-11-22 against the application in development mode on 8080, signed in as `team`, live data of 2026-11-21T10:13:39Z, at 1920 by 1080 in dark mode. Screenshot: `new-explorer.png`, line 24.9 open with `flow-webpush` and `vaadin-testbench` off, the Answered group opened by hand.

The reference, <https://vaadin.github.io/cve-status/findings/>, is behind its sign in gate, P-03. Nothing past the gate was compared: every statement below about the reference comes from `docs/requirements.md` section 6, not from the page.

## The support engineer

- The source explorer answers inside a version: a series, then a version, then its findings and filter. Here the front door shows only the newest measured release of each line. A ticket about 24.9.21 cannot be answered on this screen; typing the version jumps to a release page that today says only which release the line is read at. Until epic 09 fills that page, the explorer cannot answer for an older release at all.
- A finding is not addressable on its own. The source writes the finding into the address, rule 6.2.10, and opens its section on arrival; here the opened line and the opened finding live only in the view, D-102, so a link pasted into a ticket opens the list with nothing open.
- The filter writes a new history entry for every chip, D-98. The source rewrites the address in place. Five chips toggled are five presses of Back.
- The routes of an opened finding list every coordinate's every route, including those through products the reader turned off. On CVE-2026-89407 that is nine routes for a reader who said they have two of those products.
- A release whose record is not published shows one sentence and the record link. That is honest, and it is also every line but one in the test fixtures; on the live data every line has a record.
- The "N more products bring only findings you also have" sentence counted 27 products, most of which carry no finding anywhere, so it read as if 27 controls were withheld. Fixed during the review: only a product that carries a finding in scope and hides none of them is counted now.

## The pedantic analyst

| Metric | Reference | New | Delta | Verdict |
| --- | --- | --- | --- | --- |
| Facts above the fold for the same question | Not measured, behind the gate | 4 line rows with 2 badges, a scan date and a summary sentence each; with a line open, its summary and 13 finding rows | n/a | Not comparable |
| Clicks from arrival to the verdict of one line | Not measured | 0 for the badges, 1 to open the line's groups | n/a | Not comparable |
| Data area as a percentage of the viewport | Not measured | Master 416 by 879 plus detail 1338 by 879 out of 1920 by 1080: 75 percent | n/a | Not comparable |
| Base font size | Not measured | 16 px | n/a | Not comparable |
| Smallest click target | Not measured | Chips 34 px high; the route dots are 10 px but are not controls | n/a | Passes 24 px |

## Issues

### Critical

None found.

### Major

1. No answer for a release other than a line's newest until epic 09, see above.
2. The opened line and finding are not in the address, D-102, against rules 6.2.1 and 6.2.10 of the source.
3. Every filter change is a history entry, D-98, against rule 6.2.12.
4. Fixed during the review: the silent product sentence counted products that carry nothing.

### Minor

1. The detail grid's Through column takes all the spare width, which pushes the product dots to the far right edge at 1920 wide, 900 pixels from the id they belong to.
2. Fixed during the review: the detail title read "24.9: 24.9.26" and now reads "Line 24.9, release 24.9.26".
3. Opening a group with a long grid logs "ResizeObserver loop completed with undelivered notifications" once in the browser console.
4. The chip addresses written by the router encode the comma, `off=flow-webpush%2Cvaadin-testbench`, while the helper's own `href` writes it bare. Both read the same.
5. Routes of a finding through a product that is off are not dimmed or left out.

## Deliberate improvements

- The selection, the groups opened by hand and the search are restored from the browser's storage into the address in a new session, verified by signing out and in again.
- The six machine files of the opened release are one click each, streamed through the server with the publisher's content type and a disposition naming the file: all six fetched 200 for 24.9.26.
- The Spring Boot switch is the starter's chip in another shape and writes the same address.

## Verdict

Fail as a whole answer, pass as the front door. The issues listed as major are recorded for a person in P-15.

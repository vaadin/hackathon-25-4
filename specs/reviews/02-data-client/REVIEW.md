# Review: epic 02, the data client

Reviewed on 2026-11-20 against the application in development mode on 8080, reading the live site, signed out for home and signed in as `team` for the findings page.

## What could and could not be compared

The epic shows three things to a reader: the measured chip, a skeletal card per line on home, and a list of machine files on the internal front door. The reference home page and the reference explorer both sit behind a sign in gate, which was not circumvented, so there is no `reference-*.png` here and nothing below is a side by side comparison. That is P-03 in `NEEDS-A-PERSON.md`. The screenshots are 1200 by 997, the size of the automated browser's window, and not the 1920 by 1080 the procedure asks for.

## The support engineer

- Home tells me nine minors and a release number each and nothing else. No state, no "upgrade to", no scan date, and a card is not a link. I cannot answer a ticket from this page. I am told epic 03 does that, and until it lands this page is a list.
- The prerelease line is not on home and is on the findings page. Nobody told me why the two differ.
- "Measured 2026-11-20" is a date and no time. The live header is the same, so I will not complain twice, but when the nightly publish is late I cannot tell this morning's from yesterday's at a glance of the day alone.
- When the data cannot be reached I get one grey sentence under the heading. It does not say when it last worked or that trying again later will help. It is honest and it is thin.
- The findings page offers five files for every line. For a release the publisher has no file for, the link is there and the download is a 404 with nothing on screen saying so.
- "osv-scanner config" and "bomber ignore list" are your words. The file names are `osv-scanner.toml` and `bomber-ignore.txt`, and those are what I would search for.

## The pedantic analyst

| Metric | Reference | New | Delta | Verdict |
| --- | --- | --- | --- | --- |
| Facts above the fold for the same question, home | Not measured, behind the gate | 19: nine minors, nine releases, one measured date | Unknown | Not comparable |
| Clicks from arrival to the verdict | Not measured | No verdict is reachable from home yet | Unknown | Fail until epic 03 |
| Data area as a percentage of the viewport, home | Not measured | About 15: 1152 by 176 of 1200 by 997 | Unknown | Mostly empty |
| Base font size | 16px, from the epic 01 review | 16px, chip 15px | 0 | Pass |
| Smallest click target | Not measured | The "CSV" link on findings, about 33 by 20 | Unknown | Small |

## Issues

### Critical

None that this epic owns. The home page does not answer a question, which is the scope of epic 03.

### Major

- A download link is offered for files the publisher may not have, and a 404 is silent on screen. Epic 08 owns the explorer and should offer only what exists, or say so. The documentation says a failed download is not shown by the UI unless a progress listener tells the reader.
- The outage sentence gives no date and no next step.

### Minor

- The "CSV" and "VEX" links are narrower and lower than a comfortable target.
- The prerelease line appears on findings and not on home, with no word about it.
- The link wording is ours and not the file names.
- Screenshots are not at the size the procedure names.

## Deliberate differences

- The chip moves on its own when a new measurement is published. The static pages go stale until reloaded. Verified in a real browser: the page stayed open, the index moved, and the chip and the 24.9 card changed with no reload.
- The files are downloaded from this application's origin and not linked across origins.

## Verdict

Fail as a screen, as expected of placeholders: nothing here is the designed page. Pass for what the epic set out to prove, that the data arrives, stays, and refreshes in place.

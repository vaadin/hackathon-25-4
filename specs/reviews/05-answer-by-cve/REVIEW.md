# Review: epic 05, answer by CVE

Compared on `/cve/CVE-2026-89407/24.9.21` at 1920 by 1080, dark scheme, development mode, and on `/cve/CVE-2026-54285/24.9.21` at 375 wide. The reference page for the same question, `https://vaadin.github.io/cve-status/#cve/CVE-2026-89407/24.9.21`, shows its sign in gate, so `reference-cve.png` is the gate and nothing behind it: every comparison with the reference below is with its source, `web/ee/screens/cve.js`, read for behaviour, and not with its pixels. P-03 covers the gate.

Screenshots: `new-cve.png` (1920 by 1080), `new-cve-phone.png` (375 wide, every layer opened), `reference-cve.png` (the gate).

## The support engineer

- The headline is the answer, "CVE-2026-89407 is fixed in 24.9.26", with the sentence under it naming the reader's version, as the source page draws it. Nothing moved below the fold that the reference had above it, as far as the source tells.
- Copy answer for your ticket works here, where the reference opens a "not built" dialog. The copied text is the card's own three lines, word, headline and sentence, and the scan date. It carries no link to this page, so a colleague reading the ticket cannot open the same answer from it. Adding one would be a fact the card does not show, which the definition of done forbids; it is a question for a person.
- The scan date on the card is the date of the reader's version, 2026-11-15, while the chip in the bar says Measured 2026-11-21. Two dates a reader can mistake for one another.
- An advisory the reader pasted as its GHSA id, with the CVE pasted beside it, collapses into one card. If the GHSA is pasted alone, the card says we do not track it under that id and to look up its CVE id. The reference says the advisory is in nothing we scanned, which is wrong for an alias, so this is a deliberate difference, D-62, but it is one more step for the engineer: they must find the CVE id themselves.
- The line card for the reader's own line says "Fix available, In 24.9.26 and later" while the advisory document says 24.9.26 is still affected. The card follows the version document and the headline, D-64. A reader who opens the 24.9 line from the card lands on 24.9.26 and its version screen, which is secure, so the two agree there.
- Without a version the first layer no longer quotes a verdict's impact as the reader's. The reference did quote the first verdict there. The verdicts are in the second layer with their coordinates instead, one click further.

## The pedantic analyst

| Metric | Reference | New | Delta | Verdict |
| --- | --- | --- | --- | --- |
| Facts above the fold for the same question | Not measurable, sign in gate | State word, headline, one sentence, release notes, copy, scan date, all nine line cards with word and detail, the first layer heading and its three pairs | Unknown | Not compared |
| Clicks from arrival to the verdict | Not measurable | 0 | Unknown | Not compared |
| Data area as a percentage of the viewport | Not measurable | 1152 of 1920 pixels wide, 60 percent; the right 768 pixels are empty | Unknown | Fail on its own terms: the same empty right side epic 04's review recorded |
| Base font size | Not measurable | 16 px | Unknown | Pass |
| Smallest click target | Not measurable | 34 px high, the layer headings; the line cards are 98 px high | Unknown | Pass against 24 px |

## Issues

### Critical

None found.

### Major

1. The right 40 percent of a 1920 wide screen is empty, as on the version screen. The line cards wrap to two rows where one row of nine would fit.
2. Nine line cards, five of them green and saying "Nothing to do" or "Measured on", push the details below the fold. A reader on 24.9 has to scroll past eight lines that are not theirs to reach what the advisory means for them.
3. "Vaadin's assessment: Affects you" in the first layer repeats the headline's state in the catalogue's lower case words, capitalised by us. On a fixed advisory, "Affects you" beside "is fixed in 24.9.26" reads like a contradiction, though both are true.
4. Two dates on one screen, Measured 2026-11-21 in the bar and Scanned 2026-11-15 on the card.

### Minor

1. "The verdict is written for Vaadin *." prints the verdict's `affects` field as it arrives; `*` means every version and reads as a typo.
2. The coordinates of CVE-2026-54285 share nothing, so the marked difference is each whole coordinate, which marks nothing in particular.
3. The page title of a list of ids is the ids joined by commas, nine of them in one tab title.
4. The Vaadin modules pair of an advisory with eight modules is one comma separated paragraph, hard to scan on a phone.
5. The copy button is a tertiary button beside a filled release notes button, so on a secure or not affected card, which has no release notes, the only action looks like a link.

## Deliberate improvements

- Copy answer for your ticket is built, with the notice saying whether the browser accepted the write.
- An unknown GHSA alias is not told it is in nothing we scanned, D-62.
- A risk from the reader's own Spring Boot is not answered "Does not affect you", D-65.
- A line that never reported the advisory is not called "Fixed for you", D-66.
- A version we never measured is said once above the cards, with a link to answer the same ids for the nearest measured release.
- On a phone the coordinate tables stack row by row, so no column is a letter wide.

## Verdict

Pass, with the four major issues recorded for a person, P-10. Nothing here invents a value, and the reference behind its gate could not be compared.

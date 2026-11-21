# Review: epic 06, scanner report triage

Compared on `/scan/24.9.21` after uploading `src/test/resources/scan/osv-scanner.json` at 1920 by 1080, dark scheme, development mode, and at 390 wide. The reference page for the same question, `https://vaadin.github.io/cve-status/#scan`, shows its sign in gate, so `reference-scan.png` is the gate and nothing behind it: every comparison with the reference below is with its source, `web/ee/screens/scan.js` and `web/ee/lib/triage.js`, read for behaviour, and not with its pixels. P-03 covers the gate.

Screenshots: `new-scan.png` (1920 by 1080), `reference-scan.png` (the gate). The phone width was measured, not saved: the page is 390 pixels wide at a 390 pixel viewport and the table scrolls inside its own section, 342 pixels of a 736 pixel table visible.

## The support engineer

- The headline, "One upgrade clears 1 of your 6 open findings", the sentence naming the upgrade, the three buckets and the two downloads are all above the fold, with the six open rows of the table under them. The reference draws the same pieces in the same order.
- The file never left the browser on the reference page. Here it is read on the server, and the screen and the help both say the file is not stored. A customer who was told the old promise by a colleague meets a different one, in writing.
- A row's Action names who has to move: Vaadin, Spring Boot, the library or the reader. The reference wrote "Awaiting a fix" on Vaadin's own stuck rows; this writes "Awaiting a fix from Vaadin". Wording of ours, P-11.
- The scanner's own severity is read from every format and shown nowhere: not in the table, not in the files. The engineer matching a customer's list by severity has to open each CVE. The feature's columns have no severity, so this follows the specification, but the reference's CSV had a severity column and this one does not.
- Rows keep the order of the report. A critical finding can sit at row 150 of a capped table.
- The downloads are buttons: there is no link to copy and paste into a ticket, and the download address is a session bound one anyway.
- The PDF preview opens in a dialog whose header says "Audit report" and whose viewer header says the file name again, two titles on one sheet.

## The pedantic analyst

| Metric | Reference | New | Delta | Verdict |
| --- | --- | --- | --- | --- |
| Facts above the fold for the same question | Not measurable, sign in gate | File, version, findings count and format in the card; headline; upgrade sentence; three counts with their detail; release notes; six table rows of five cells each | Unknown | Not compared |
| Clicks from arrival to the verdict | Not measurable | 2 and a file choice: the report tab, then Choose a file. A file that names no version adds typing it and Answer for this version | Unknown | Not compared |
| Data area as a percentage of the viewport | Not measurable | 1152 of 1920 pixels wide, 60 percent; the right 768 pixels are empty | Unknown | Fail on its own terms, as on the version and CVE screens |
| Base font size | Not measurable | 16 px | Unknown | Pass |
| Smallest click target | Not measurable | 20 px high, the CVE links in the table; 26 px after the fix in major issue 1 | Unknown | Fail against 24 px as reviewed, pass after the fix |

## Issues

### Critical

None found.

### Major

1. The CVE links in the table were 20 pixels high, under the 24 pixel target. Padded after this review and measured again in the browser: 26 pixels.
2. The right 40 percent of a 1920 wide screen is empty, while the table's Action column wraps.
3. No severity anywhere, though every format carries one, and rows are not ordered by it.
4. The help mark of the report tab sits alone on the line under the drop zone, away from the file line it explains.
5. A large upload shows no progress: there is no file list, so between choosing a file and the answer the drop zone looks unchanged.

### Minor

1. The CVE-2026-2742 row on `com.vaadin:vaadin` says "Already secure, scanner false positive" and "Not reported on 24.9.21": the verdict of the coordinate and the reason of the version, two reasons for one row.
2. The secure switch is a navigation, so each toggle is a history entry and Back flips it.
3. "Safe to whitelist" under Already secure is the reference's phrase and a judgement a customer's security team may not share.
4. On a phone the five column table is read by scrolling it sideways inside its section.
5. On the fixtures the nearest measured release offered for 24.9.99, 24.9.26, has no version document committed, so following the offer in a test lands on another not measured answer. Live data has it.

## Deliberate improvements

- Every action names who moves, rule 6.11.20, including Vaadin's own stuck rows.
- A version nobody measured gets the catalogue's verdict word per row and no headline or bucket counted against a release with no measurement, D-74.
- A replaced report is answered for its own version unless the reader named one, D-77.
- The audit PDF exists and previews in the viewer before it is taken; without the viewer's feature flag the same file is a plain download.
- Both files carry the whole triage with the version and the measurement date in their first line.

## Verdict

Pass, with the five major issues recorded for a person, P-13. Nothing on the screen or in the files invents a value, and the reference behind its gate could not be compared.

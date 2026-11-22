# Review: the verdict workbench, epic 10

Screens: `new-workbench.png`, the queue at 1920 by 1080 on arrival, and `new-workbench-form.png`, a restored draft of jackson-core with its YAML. The reference, the JavaScript workbench at `findings/workbench.html`, is behind its sign in gate, P-03, so no `reference-*.png` exists and every comparison below is against its source read for behaviour, `web/findings/bench.js` of vaadin/cve-status, not against its pixels.

## The support engineer

This screen is not one a support engineer answers tickets from; the people it serves are the security team writing verdicts. Read as one of them:

- The JavaScript queue opened each package as a fold with the evidence above the form: what the package resolves to per series, the dependency chains with who declares them, the CPEs matched, the scanners that reported it, and for each advisory whether upstream fixed it and why osv.dev does not list the version. None of that is here. The form is written blind: a person has to go to the explorer, or the terminal, to see what they are judging. This is the biggest loss against the reference.
- The reference ticked each advisory with a checkbox beside its severity, its series and what already answers it. Here the advisories are one text field, prefilled with all five ids for jackson-core, with no severity and no current verdict beside them.
- The reference filled the form from the verdict a package already carries, one click, and pointed `supersedes` at it. Here a row with every advisory answered opens a blank verdict for all of them, and `supersedes` has to be typed.
- The reference said, per package, which platform branches already list how many of its advisories. Here that knowledge appears only as the one sentence after a silencing applicability is chosen.
- The reference composed `assessed_by` from a field and `depth` defaulted to cursory; here depth starts unsaid and is omitted when left so, which the loader reads as cursory anyway. A reviewer reading the YAML cannot tell the two apart.
- The queue's grid shows 9 rows above the fold at 1920 by 1080 because the two summary tables take 450 pixels of height first. With every package at 0 the first screen is mostly zeros.
- The draft list lives under a form 1,700 pixels tall, so a restored draft means scrolling down past the whole form and back up.

## The pedantic analyst

| Metric | Reference | New | Delta | Verdict |
| --- | --- | --- | --- | --- |
| Facts above the fold for the same question, the queue on arrival | Not measured, behind the gate | Lead sentence, 11 series chips with counts, the total line, 4 severity rows, 10 series rows, 9 queue rows | n/a | Not comparable |
| Clicks from arrival to a composed YAML | 2 by source reading: open the package, open the form, then Compose | 1: choosing a row composes the YAML live | minus 1 | Better |
| Clicks from arrival to the GitHub editor | 3 by source reading, the link opens a new tab | 3 with a filled draft: row, Export, Open in GitHub; more for each field left to fill | 0 | Same |
| Data area as a percentage of the viewport, arrival | Not measured | Queue grid 350 of 1080 pixels high, 32 per cent; tables 41 per cent | n/a | Summary heavier than the queue |
| Base font size | Not measured | 16 pixels, the application's Aura base | n/a | |
| Smallest click target | Not measured | Series chips 32 pixels high; grid rows 35; Open in GitHub 36 | n/a | Pass at 24 |

## Issues

### Critical

None found that loses or invents a fact: absent fields produce no key, the numbers are the queue's own, and nothing is submitted without the `Ready` group passing.

### Major

1. No evidence beside the form: resolved versions, chains, CPEs, scanners, the upstream fix and the absent reason, all carried by `data/queue.json` and shown by the reference. The feature document does not list them, so they were not built; the reference has them for a reason.
2. Advisories are a text field, not a choice among the package's advisories with their severity and current verdict.
3. No fill from the verdict the package already carries, so superseding means retyping a verdict that exists.
4. The summary tables push the queue below the middle of the screen.

### Minor

1. The draft list is under the form, far from the queue it belongs to.
2. A saved draft keeps the date it was proposed with in its id, while `assessed_at` moves with the day the YAML is composed; the two can disagree a day later.
3. Every field shows the same helper text weight, so the four that `Ready` needs do not stand out until an export is refused.
4. The GitHub control is a native button styled by hand beside three Vaadin buttons; it matches in height and not in focus ring.
5. The series column of the grid lists every series of the package even under a series filter, which is right, but nothing marks the one filtered on.

## Deliberate improvements

- The YAML is live beside the form as one types, rather than composed by a button at the end.
- Drafts are kept on the server between sessions and between people, with who saved last, where the reference kept nothing.
- The export refuses a half draft field by field, with the fields marked, where the reference checked two things and composed the rest regardless.
- The series filter is a real address, `?series=24.9`, a link that opens in a new tab and survives a reload.
- The handover is measured on the address the browser will really send, CR LF included.

## Verdict

Fail against the reference for the person writing verdicts, because the evidence the reference shows beside each package is missing and the form is written blind. Pass for what the feature document asks: every acceptance criterion holds.

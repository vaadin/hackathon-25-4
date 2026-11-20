# Feature 05: Answer by CVE

## Overview

One or more advisory ids, with or without a version, answered the way screen 3 of the delivered design answers them. Covers C1 to C6.

## Behaviour

### The verdict cards

One card per id, and the state decides the words, taken from the source page's settled set: fixed in X when the reader's version is affected and a release clears it, affects you, not assessed yet, affects you through Spring Boot, affects you through a discontinued product, does not affect you, fixed for you, measured when no version was given, or not in our measurements. The headline with a version answers the one question, is this fixed for me, with the exact release to move to, and that release is on the reader's line by construction, rule 6.11.11.

- Actions: View release notes for the fix release, and Copy answer for your ticket, which here is built: it composes the card's facts into a short plain text answer and puts it on the clipboard through the Clipboard API, with the typed JS call reading back success, and confirms in one notice. The source page could not build this without a server and drew the not built dialog; this application has the server, and the difference is a line in the demo script.
- An id we never measured says so plainly and nothing else, rule C6.

### Across supported versions

One small card per maintained line, from `coverage`: affects you, fixed, never affected, reported, not measured yet, with the detail sentences, in X and later, a prerelease, enterprise only X, measured on X, nothing to do. The reader's line is marked yours and its card names the release the finding is gone in.

### Details

The design's heading, Details, with the owning id named when several ids were asked. Three layers, only the first open:

1. **What this means for you**: impact, Vaadin's assessment, and Source, where it reaches a Vaadin application from and the dependency it rides in on, unestablished said as such.
2. **Affected modules and the fix**: the modules, the dependency in the box, package, version, bundled in, fixed upstream in, per line, do this now, fix, who ships it, expected, PR links, handover, and other per package verdicts, each naming its coordinate with the differing part marked, rules 6.4.8 and 6.4.9.
3. **The advisory and the evidence**: severity, summary, commentary through Markdown, the measured affected range with its count, what the fix actually was, first seen, the link to cve.org or the GitHub advisory, aliases.

No badges on the verdict card, rule 6.11.12: who ships the fix lives in the fix column, aliases live beside the record.

### Multiple ids

Cards stack in the order asked. The details section exists once per id, each named. The version field applies to all of them.

## Edge cases

| Scenario | Behaviour |
| --- | --- |
| An id with no version given | The measured card: what the catalogue knows, no claims about the reader |
| An id fixed below the reader's version | Fixed for you, not reported on your version |
| Ids separated by newlines and semicolons mixed | All parsed, order kept, duplicates collapsed |
| An alias asked, GHSA for a CVE we track | Answered under the id we track, the asked alias shown beside it |
| A version given that we never measured | Each card answers the catalogue half and says the version is not measured, once, above the cards |

## Acceptance criteria

### AC1: One card per id, the right state
- [ ] Every state in the fixture set renders its wording
- [ ] The fix release named is on the reader's line
- [ ] An unknown id is answered plainly

### AC2: Copy answer works
- [ ] The clipboard receives the composed answer and the notice confirms
- [ ] The composed text names the id, the verdict, the reader's version and the move, and nothing the data does not carry

### AC3: The layers are the design's
- [ ] Only the first layer is open on arrival
- [ ] Absent fields are omitted
- [ ] Per package verdicts each name their coordinate

### AC4: Across versions tells each line's truth
- [ ] The reader's line is marked yours and names the release the finding is gone in

## Test cases

| Id | Given | When | Then | Tier | Verified by |
| --- | --- | --- | --- | --- | --- |
| CVE-01 | A fixed advisory and an affected version | Opening `/cve/<id>/<v>` | Fixed in X headline, release notes action | browserless | `CveViewBrowserlessTest` |
| CVE-02 | The same id, no version | Opening `/cve/<id>` | The measured card, no claims about the reader | browserless | `CveViewBrowserlessTest` |
| CVE-03 | Three ids mixed separators | Submitting the lookup | Three cards in asked order | browserless | `CveViewBrowserlessTest` |
| CVE-04 | An unknown id | Opening it | Not in our measurements, nothing else | browserless | `CveViewBrowserlessTest` |
| CVE-05 | The coverage fixture | Rendering across versions | Line cards match the fixture, yours marked | browserless | `CveViewBrowserlessTest` |
| CVE-06 | Copy answer | Clicking it | Clipboard holds the composed answer, notice shown | testbench | `CopyAnswerIT` |
| CVE-07 | A two package advisory | Opening layer two | Two verdicts, coordinates named, difference marked | browserless | `CveViewBrowserlessTest` |
| CVE-08 | An advisory with no workaround field | Layer one | No workaround row | browserless | `CveViewBrowserlessTest` |

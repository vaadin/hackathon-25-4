# Feature 05: Answer by CVE

## Overview

One or more advisory ids, with or without a version, answered the way screen 3 of the delivered design answers them, laid out as the live prototype lays it out, compared in `specs/reviews/prototype/cve.md`. Covers C1 to C6.

## Layout

Under the customer shell's H1, purpose and lookup, the ids asked are the answer's H2. Each id then reads, in order: its verdict card, Across supported versions, and Details. The card's headline and every section heading are H3, D-186. The card is tinted by its state with a border all round and the state word in capitals; the line cards sit four to a row in the 912 pixel column, each tinted by its state, the reader's own outlined.

## Behaviour

### The verdict cards

One card per id, and the state decides the words, taken from the source page's settled set: fixed in X when the reader's version is affected and a release clears it, affects you, not assessed yet, affects you through Spring Boot when Spring Boot pins the version, affects you through a discontinued product, does not affect you, fixed for you, measured when no version was given, or not in our measurements. A risk verdict whose every route is the reader's own Spring Boot, `theirs_risk`, does not affect them through Vaadin: the card says so, and its why says where the verdict lives, D-187. The headline with a version answers the one question, is this fixed for me, with the exact release to move to, and that release is on the reader's line by construction, rule 6.11.11.

- Actions: View release notes for the fix release, and Copy answer for your ticket, which here is built: it composes the card's facts into a short plain text answer and puts it on the clipboard through the platform's Clipboard API, whose observed form reads the browser's success back to the server, D-63, and confirms in one notice. The source page could not build this without a server and drew the not built dialog; this application has the server, and the difference is a line in the demo script.
- An id we never measured says so plainly and nothing else, rule C6. An entry of the address that is no CVE or GHSA id at all, which only a hand typed address carries, gets the same card in the same plain words, after the cards of the ids that parse, and is never looked up, D-226.

### Across supported versions

One small card per maintained line, from `coverage`: affects you, fixed, never affected, reported, not measured yet, with the detail sentences, in X and later, a prerelease, enterprise only X, measured on X, nothing to do. The reader's line is marked yours and its card names the release the finding is gone in.

### Details

The design's heading, Details, with the owning id named when several ids were asked. Three layers, only the first open:

1. **What this means for you**: impact, Vaadin's assessment in the catalogue's own words, and Source, where it reaches a Vaadin application from and the dependency it rides in on, unestablished said as such.
2. **Affected modules and the fix**: the modules, the dependency in the box, one sentence per package on the reader's line ("X 2.21.4, as we scanned the newest 24.9 release. The library fixes it in 2.21.7.") or a table of every line when no version was given, do this now, fix, who ships it, expected, PR links, handover, and other per package verdicts, each naming its coordinate with the differing part marked, rules 6.4.8 and 6.4.9.
3. **The advisory and the evidence**: severity, summary, the commentary through Markdown, read from the advisory document's copy of the verdict, D-196, the measured affected range with its count and the range the verdict is written for in one paragraph, what the fix actually was, first seen, the link to cve.org or the GitHub advisory, aliases.

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
| An address whose ids do not parse, `/cve/GHSA-aaaa-bbbb-cccc` | The not in our measurements card for each entry that does not parse, "X is not in anything we scanned", as the prototype says, D-226 |

## Acceptance criteria

### AC1: One card per id, the right state
- [x] Every state in the fixture set renders its wording
- [x] The fix release named is on the reader's line
- [x] An unknown id is answered plainly

### AC2: Copy answer works
- [x] The clipboard receives the composed answer and the notice confirms
- [x] The composed text names the id, the verdict, the reader's version and the move, and nothing the data does not carry

### AC3: The layers are the design's
- [x] Only the first layer is open on arrival
- [x] Absent fields are omitted
- [x] Per package verdicts each name their coordinate

### AC4: Across versions tells each line's truth
- [x] The reader's line is marked yours and names the release the finding is gone in

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
| CVE-09 | An address whose id does not parse, alone and beside one that does | Opening it | The plain not in anything we scanned card for it, after the parsed id's answer, no copy action | browserless | `CveViewBrowserlessTest` |

## Still open

Written from the code at the close of the prototype alignment, D-186 to D-196 and D-226 included. Each line names where the code shows it and the decision or draft behind it.

- Copy answer reads success back through the platform's `Clipboard.onClick(button).writeText(text, onCopied, onError)`, not through a typed JavaScript call with a `JsExpression` return value; `callJsFunction` and `addJsInitializer` are used nowhere in the application, `CveView.copyButton`, D-63, D-157.
- The copy error callback stays a lambda so that `PromiseAction.Error`, in an internal package, is never named; remove that when the type becomes public, `CveView.copyButton`, draft 15.
- An alias asked alone, a GHSA for a CVE we track, is not answered under the tracked id: it resolves only when another asked id lists it as an alias, otherwise its card says the id is not tracked under that name, `CveView.resolve`, D-62, the alias map data need in `FEEDBACK-25.4.md`.
- An id fixed below the reader's version reads "Fixed for you" only when the reader's line ever reported it; on a line that never did, the word is "Does not affect you", `CveRules.state`, D-66.
- The reader's own line card follows the version document, not `coverage`, when the two disagree, `CveView.lineCard`, D-64, confirmed against the prototype as an app extra, D-189.
- Kept as app extras where the prototype says less or something false, D-189: the not measured note and its link, the `from_build` sentence, the per package table of every line with its marked difference, `cve.evidence.askedAs`, the copy answer itself, which the prototype does not build.
- Whether the copied answer should carry a link to the page is not answered, P-10: the prototype has no copy to compare with.
- An address whose ids do not parse is answered since D-226; it used to show the H2 and nothing under it.
- What the prototype shows and the data does not carry: the advisory's publication date after "First seen by our scans", a data need row in `FEEDBACK-25.4.md`, D-179.

# Feature 07: Advisories and cases

## Overview

The full catalogue for the reader who wants the list, and the demo case index. Covers H2's far end, and the cases screen the demos script against.

## Behaviour

### Advisories

- A Grid over `index.advisories`, sorted worst severity first, then oldest, then id, the source ordering rule 6.4.22.
- Columns: id, severity badge, owner chip, package, one line impact, verdict chip in customer words.
- Filter pills above it, one per verdict present, each with its count, driving `?verdict=<applicability>`. The counts are of rows shown against the filter, never a total beside a filtered list, rule 6.3.11.
- A row navigates to `/cve/<id>`.
- A context menu per row through `GridContextMenu` with dynamic content: copy the id, open the record at cve.org or GitHub, ask about this on my version, which jumps to the lookup with the id carried. Dynamic because the record link depends on the row.
- The Grid AI controller attaches here for signed in `SECURITY` sessions only, and the ask box simply does not exist for anyone else: the view asks the security context once at build time. The attachment itself is epic 11's work, AST-05, because it needs the provider plumbing; this epic ships the grid AI ready and unaware.

### Cases

- `/cases`, unlisted, no link from anywhere: the catalogue of every state the customer screens can show, in the source page's three groups, cannot be rendered, none today, renderable with a count and a See one link. Fed by `cases/cases.json`.
- It exists for demos and review: walking every state without hunting for data that exhibits it.

## Edge cases

| Scenario | Behaviour |
| --- | --- |
| A verdict with zero advisories | Its pill does not render |
| `?verdict=` naming an unknown value | Ignored, everything shown |
| A case pointing at data that no longer exhibits it | It moves to the none today group on render, never a broken link |

## Acceptance criteria

### AC1: The list is the catalogue
- [ ] Order is severity, then age, then id
- [x] Pills carry counts and write the flag through the navigation helper
- [x] Owner chips follow the fix owner rule

### AC2: The context menu acts
- [x] Copy id fills the clipboard
- [x] The record link matches the row's advisory

### AC3: Cases cover the states
- [x] Every renderable state has a working See one link

## Test cases

| Id | Given | When | Then | Tier | Verified by |
| --- | --- | --- | --- | --- | --- |
| ADV-01 | The index fixture | Opening advisories | Fixture order, worst first | browserless | `AdvisoriesBrowserlessTest` |
| ADV-02 | The risk pill | Clicking it | Only risk rows, `?verdict=risk`, count matches | browserless | `AdvisoriesBrowserlessTest` |
| ADV-03 | A row's context menu | Opening it | The items name the row's id and record host | browserless | `AdvisoriesBrowserlessTest` |
| ADV-04 | The cases fixture | Opening `/cases` | Three groups, counts, See one links resolve | browserless | `CasesBrowserlessTest` |

## Still open

Written from the code at the end of epic 13. Each line names where the code shows it and the decision or draft behind it.

- AC1: the list is ordered by severity, then by id; the oldest first step is not applied because the index carries no published date, `AdvisoryRules.ordered`, D-81, D-92. The same gap keeps US-7.1's first task, the grid in the source order, from being true.
- No one line impact column: `index.advisories` carries no impact text, `AdvisoriesView.configureGrid`, D-82.
- A case whose fact the index does not carry is checked only for its example's existence, so it can stay renderable after the data stops exhibiting it, `CaseRules.exhibits`, D-88.
- Workaround to remove when draft 21 is answered: the Grid AI controller drives a hidden `Grid<AIDataRow>` over an in memory copy of the catalogue, and the ids it settles on narrow the advisories grid, `assistant/AdvisoryAsk`, D-140.
- Workaround to remove when draft 17 is answered: `@Uses` on `AdvisoriesView` so that the production bundle carries the ask box, D-149.
- Workaround to remove when draft 18 is answered: `GridContextMenuTester.open` throws on a `setDynamicContentProvider` menu, so the test fires the before open event itself, `AdvisoriesBrowserlessTest.openMenu`.

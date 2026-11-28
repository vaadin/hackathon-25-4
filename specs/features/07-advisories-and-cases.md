# Feature 07: Advisories and cases

## Overview

The full catalogue for the reader who wants the list, and the demo case index. Covers H2's far end, and the cases screen the demos script against.

## Behaviour

### Advisories

Laid out as the live prototype lays it out, compared in `specs/reviews/prototype/advisories.md`.

- Under the customer shell's lookup, the H2 "Every advisory we have measured" with the aside "N in all, newest scan of every line we follow", and the prototype's tip, D-191. The view's title "All advisories" is the trail's leaf.
- Filter pills: All, then one per verdict present in the prototype's order, affects you first and not on a current release last, each with its count in lighter type, driving `?verdict=<applicability>`. A row is listed under its listed verdict: in triage on no current release is not on a current release, D-192. The counts are of rows shown against the filter, and the line under the pills says the shown count against the total, rule 6.3.11: "N advisories, worst first"; with a pill, "N of M advisories shown: <verdict words>"; after an ask, "N of M advisories match the ask, worst first".
- A Grid over `index.advisories`, sorted worst severity first, then oldest, then id, the source ordering rule 6.4.22. With no published date the oldest step is not applied, D-81, as in the prototype.
- Columns: id, severity, owner chip, every package whole and one to a line, the verdict in customer words, the risk one in the error badge, and a chevron ending the row, D-194. No impact column, D-82: the prototype has none either.
- A row opens `/cve/<id>` with no version, even when the lookup holds one. Only the menu's "Ask about <id> on my version" carries the lookup's version. Opened inside the team's shell at `/findings/advisories`, D-237, a row and Ask open the answer at `/findings/cve/...`, D-246.
- A context menu per row through `GridContextMenu` with dynamic content: copy the id, open the record at cve.org or GitHub, and "Ask about <id>", which opens the answer for the id, or "Ask about <id> on my version" when the reader already gave a version, which opens the answer on it, P-14, D-230. Dynamic because the record link depends on the row. Not in the prototype, kept, D-193.
- The Grid AI controller attaches here for signed in `SECURITY` sessions only, between the pills and the total line, and the ask box simply does not exist for anyone else: the view asks the security context once at build time. The attachment itself is epic 11's work, AST-05, because it needs the provider plumbing; this epic ships the grid AI ready and unaware. The box exists only when the application carries the team part and offers the assistant, D-236, D-254; without either there is no box and nothing in its place.

### Cases

Laid out as the live prototype's `#cases` lays it out, compared in `specs/reviews/prototype/cases.md`.

- `/cases`, unlisted, no link from anywhere, titled "Every case": the H2 "Every case, with a real example" with the catalogue's date, the prototype's tip, and every state the customer screens can show in the source page's three groups, each headed by the prototype's words with its aside: what the design asks for and the measurements cannot give, cases that do not occur today, cases you can go and look at. Fed by `cases/cases.json`.
- Within a group the most frequent first. A row: the count, the title with the screen word under it, "Would need:" and the catalogue's `needs` where it has one, and a See one link, or a cannot or none today chip, D-195.
- The groups come in the order impossible, none today, renderable, headed "What the design asks for and the measurements cannot give", "Cases that do not occur today" and "Cases you can go and look at", with the asides "N of them", "The layout exists and nothing reaches it" and "N of them". An empty group reads "None.". The count is shown for every renderable case, under none today only when it is 0, and never for an impossible case. The chip reads "cannot" in the error variant, or "none today" plain. A case with no title shows its id.
- `/cases/<id>` goes straight to the live example of that case, by a forward, so a demo script names a state rather than a release. It goes to `/cases` when the case has no live example or the data cannot be read.
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
| ADV-05 | A case id with a live example | Opening `/cases/<id>` | Its example's screen opens | browserless | `CasesBrowserlessTest` |

## Still open

Written from the code at the close of the prototype alignment, 2026-11-22. Each line names where the code shows it and the decision or draft behind it.

- AC1: the list is ordered by severity, then by id; the oldest first step is not applied because the index carries no published date, `AdvisoryRules`, D-81, D-92. The same gap keeps US-7.1's first task, the grid in the source order, from being true.
- No one line impact column: `index.advisories` carries no impact text, `AdvisoriesView`, D-82, and the prototype shows none either, D-193.
- No search over the catalogue for a customer: the prototype has none either, so none was added, P-14.
- The context menu is announced by nothing on the screen, and stays so: Manolo's call on 2026-11-23, P-14. The prototype has no menu, it is an app extra, D-193.
- An unknown `?verdict` is ignored and everything shown, where the prototype shows an empty list, D-84, D-193.
- Why an impossible case cannot be drawn is not published, only its `needs`, and for one case of five: a data need row in `FEEDBACK-25.4.md`, D-195.
- A case whose fact the index does not carry is checked only for its example's existence, so it can stay renderable after the data stops exhibiting it, `customer/CaseRules`, D-88.
- Workaround to remove when draft 21 is answered: the Grid AI controller drives a hidden `Grid<AIDataRow>` over an in memory copy of the catalogue, and the ids it settles on narrow the advisories grid, `ui/team/assistant/AdvisoryAsk`, D-140.
- Workaround to remove when draft 17 is answered: `@Uses` on `AdvisoriesView` so that the production bundle carries the ask box, D-149.
- Workaround to remove when draft 18 is answered: `GridContextMenuTester.open` throws on a `setDynamicContentProvider` menu, so the tests fire the before open event themselves, `AdvisoriesTesting`.

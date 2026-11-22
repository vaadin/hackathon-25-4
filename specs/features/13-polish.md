# Feature 13: Tests, sweep and demo polish

## Overview

The closing epic: the suites completed to their tables, the sweeps that hold the rules, and the demo script. It adds no behaviour, by the schedule in `00-overview.md`.

## Behaviour

- Every feature document's test case table is reconciled: each named class exists, each checkbox reflects reality, each Still open section is written by reading the code.
- The browserless additions of this release are exercised where the earlier epics left gaps: the upload testers against the scan flow, the JUnit 6 extensions in the shared test configuration, the AI view testing against the panel.
- The literal sweep: `NoHardcodedStringsTest` green with no exemptions beyond its stated list.
- Rule 4 sweep: for each screen, one fixture with absent fields walked and the omissions asserted.
- The demo script, `DEMO.md`: the walk from a customer question to a filed verdict draft, with the data that exhibits each state, leaning on `/cases`.
- `REPORT.md` and `NEXT.md` written, and `specs/FEEDBACK-25.4.md` reconciled against `specs/issues/`.

## Acceptance criteria

### AC1: The tables are true
- [x] `SpecConsistencyTest` passes with every feature table complete
- [x] Every Still open section is written from the code, not from memory

### AC2: The sweeps hold
- [x] No literal, no empty rendered absence, both by test

### AC3: The record is whole
- [x] Every feedback row has a draft, a reproducer or a stated reason it needs none
- [x] DEMO.md walks clean on the fixtures

## Test cases

| Id | Given | When | Then | Tier | Verified by |
| --- | --- | --- | --- | --- | --- |
| POL-01 | The whole suite | `./mvnw verify` | Green with no licence, key or network | unit | the gate itself |
| POL-02 | Every feature table | `SpecConsistencyTest` | Every named class exists | unit | `SpecConsistencyTest` |
| POL-03 | The absent field fixtures | Each screen | Omission, not emptiness | browserless | `AbsenceSweepBrowserlessTest` |

## Still open

Written from the code at the close of the prototype alignment. Each line names where the code shows it and the decision or draft behind it.

- The JUnit 6 `BrowserlessExtension` is used where it applies, `AssistantPanelBrowserlessTest`, the chat panel on its own with no Spring context. The documentation says the extensions do not replace `SpringBrowserlessTest`, so every view test still extends it; there is no shared extension in a common test configuration.
- `AbsenceSweepBrowserlessTest` walks one fixture or constructed record per screen with the omission asserted. It does not walk every absent field of every record: a field left out of the sweep is covered only by its feature's own tests.
- The sweep's screens were kept green through the prototype alignment, so its explorer, release, Spring Boot and workbench cases now walk the prototype's layouts, the series opened in place, D-204, the release page as one opened series, D-216, the Spring Boot rows shut on arrival, D-219, and the verdict queue, D-220. A blank grid cell is still accounted for by row and column, D-159.
- The demo walk of `DEMO.md` was made with real clicks on the committed fixtures served from a local port, in development mode, without the model key. The assistant's live answers and the hand over into GitHub's editor were not walked, P-17, P-18.

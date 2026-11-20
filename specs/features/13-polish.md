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
- [ ] `SpecConsistencyTest` passes with every feature table complete
- [ ] Every Still open section is written from the code, not from memory

### AC2: The sweeps hold
- [ ] No literal, no empty rendered absence, both by test

### AC3: The record is whole
- [ ] Every feedback row has a draft, a reproducer or a stated reason it needs none
- [ ] DEMO.md walks clean on the fixtures

## Test cases

| Id | Given | When | Then | Tier | Verified by |
| --- | --- | --- | --- | --- | --- |
| POL-01 | The whole suite | `./mvnw verify` | Green with no licence, key or network | unit | the gate itself |
| POL-02 | Every feature table | `SpecConsistencyTest` | Every named class exists | unit | `SpecConsistencyTest` |
| POL-03 | The absent field fixtures | Each screen | Omission, not emptiness | browserless | `AbsenceSweepBrowserlessTest` |

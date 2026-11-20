# Feature 12: Diagnostics

## Overview

The view that proves the 25.4 lifecycle events on real traffic. The contract is `specs/07-observability.md`. Covers D1.

## Behaviour

- Listeners registered once in a service init listener: navigation started and ended with outcomes, request started and ended, session lifecycle, CSP violations, assistant interception.
- A singleton of signals holds the counters and two ring buffered tails. Nothing persists.
- `/diagnostics`, role `SECURITY`: navigation outcomes per route, request timing summary, sessions now, the CSP tail, the assistant tail, and the reader's own extended client details through the typed accessors. All bound, all live.

## Edge cases

| Scenario | Behaviour |
| --- | --- |
| A navigation that fails or postpones | Counted under its outcome, which is the point of the enum |
| A restart | Zeros, and the view says counting since |
| The tails overflow | Oldest out, size fixed |

## Acceptance criteria

### AC1: Events are counted
- [ ] A navigation increments its route and outcome
- [ ] A failed navigation counts as failed

### AC2: The view is live
- [ ] A navigation in session two moves the numbers in session one without a reload

## Test cases

| Id | Given | When | Then | Tier | Verified by |
| --- | --- | --- | --- | --- | --- |
| DIA-01 | A fresh context | Three navigations, one failing | Counts per outcome match | browserless | `DiagnosticsBrowserlessTest` |
| DIA-02 | Two sessions | Session two navigates | Session one's numbers move | browserless | `DiagnosticsBrowserlessTest` |
| DIA-03 | The assistant used once | Reading the tail | The exchange is in it | browserless | `DiagnosticsBrowserlessTest` |

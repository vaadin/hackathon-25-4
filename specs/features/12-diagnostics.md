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
- [x] A navigation increments its route and outcome
- [x] A failed navigation counts as failed

### AC2: The view is live
- [x] A navigation in session two moves the numbers in session one without a reload

## Test cases

| Id | Given | When | Then | Tier | Verified by |
| --- | --- | --- | --- | --- | --- |
| DIA-01 | A fresh context | Three navigations, one failing | Counts per outcome match | browserless | `DiagnosticsBrowserlessTest` |
| DIA-02 | Two sessions | Session two navigates | Session one's numbers move | browserless | `DiagnosticsBrowserlessTest` |
| DIA-03 | The assistant used once | Reading the tail | The exchange is in it | browserless | `DiagnosticsBrowserlessTest` |

## Still open

Written from the code at the close of the prototype alignment. Each line names where the code shows it and the decision or draft behind it.

- CSP violations and the assistant tail are not service init listeners: they reach the view through signals on their own classes in `base`, `CspViolations.received()` and `AssistantTail.newestSignal()`, D-151.
- The two tails are not in the signal singleton: `DiagnosticsCounters` holds only the counters, and the rings live in `CspViolations`, 100 kept, and `AssistantTail`, 200 kept, D-151.
- `RequestStartedEvent` is not listened to: request timing comes from `RequestEndedEvent.getDuration()` alone, `DiagnosticsListeners`. No decision row.
- Sessions counts one person who just signed in as two, because the sign in page's session lives on until it times out, D-154.
- The extended client details are read once when the view opens, and only the viewport follows the window, `DiagnosticsView`, D-155.
- The colour scheme row shows `ExtendedClientDetails.getColorScheme()`, which reports what the application set, not the reader's preference. Known: flow#26390, `DiagnosticsView`.
- The postponed and not shown outcomes have never been produced on real traffic. Only the enum mapping test covers them, `Outcome.of`.
- Workaround to remove when flow#26338 is fixed, draft 23b: `forceProductionBuild` in the `it` and `production` profiles of `pom.xml`, D-156.
- The view has no left panel since D-203, so it gets no drawer toggle; it reaches the bar's destinations like every internal page. Since D-225 it has the content padding of the other internal pages, and a table wider than its column scrolls inside its panel, `styles/diagnostics.css`. The prototype has no diagnostics page: the whole view is an app extra.
- Both tasks of US-12.1 stay unticked: the first for the CSP and interception listeners and `RequestStartedEvent`, the second because the rings are not in the singleton, D-151, D-161.

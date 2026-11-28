# Feature 12: Diagnostics

## Overview

The view that proves the 25.4 lifecycle events on real traffic. The contract is `specs/07-observability.md`. Covers D1.

## Behaviour

- A service init listener registers navigation started and ended with their outcomes, request ended, and session start and end. Policy violations and assistant exchanges reach the view through signals of their own, D-151.
- A singleton holds the counters as signals: per route template, the first 100 routes and then "Other routes", and requests by the minute they ended in, 60 minutes kept. The tails are rings of their own: 100 policy violations and 200 assistant exchanges. Nothing persists.
- `/diagnostics`, role `SECURITY`: navigation outcomes per route, request timing summary, sessions now, the CSP tail, the assistant tail, and the reader's own extended client details through the typed accessors. All bound, all live. Each is a card on a `Dashboard`: Edit cards lets the reader move, resize and remove cards, Add a card brings a removed one back, Restore the default cards puts the six back in their order, and the browser remembers the arrangement in local storage, P-19, D-232. The board is the commercial Dashboard, D-232. The default cards, in order and width: Navigations 2, Requests 2, Sessions 1, Your browser 1, Policy violations 2, Assistant exchanges 2, each one row tall. Above them a line says "Counting started X, at T UTC, when the application started. Nothing here is stored: a restart sets every number back to zero." Requests says what is kept per request: "Kept per request: the minute it ended in, how long it took, whether it failed and the kind of handler that answered it. No address, header, body, session or user is recorded."

## Edge cases

| Scenario | Behaviour |
| --- | --- |
| A navigation that fails or postpones | Counted under its outcome, which is the point of the enum |
| A restart | Zeros, and the view says counting since |
| The tails overflow | Oldest out, size fixed |

## Acceptance criteria

### AC1: Events are counted
- [x] A navigation increments its route and outcome
- [x] A failed navigation counts as refused when the router answered not found or not allowed, and as broke otherwise, P-19

### AC2: The view is live
- [x] A navigation in session two moves the numbers in session one without a reload

## Test cases

| Id | Given | When | Then | Tier | Verified by |
| --- | --- | --- | --- | --- | --- |
| DIA-01 | A fresh context | Three navigations, one failing | Counts per outcome match | browserless | `DiagnosticsBrowserlessTest` |
| DIA-02 | Two sessions | Session two navigates | Session one's numbers move | browserless | `DiagnosticsBrowserlessTest` |
| DIA-03 | The assistant used once | Reading the tail | The exchange is in it | browserless | `DiagnosticsBrowserlessTest` |
| DIA-04 | A fresh browser | Opening the view | The six cards in the default order | browserless | `DiagnosticsBrowserlessTest` |
| DIA-05 | A remembered layout, partly unreadable | Opening the view | The readable cards come back, the rest left out | browserless | `DiagnosticsBrowserlessTest` |
| DIA-06 | A guest asking for a team page | The navigation ends | Counted as Refused | browserless | `DiagnosticsBrowserlessTest` |

## Still open

Written from the code at the close of the prototype alignment. Each line names where the code shows it and the decision or draft behind it.

- CSP violations and the assistant tail are not service init listeners: they reach the view through signals on their own classes, `security/CspViolations.received()` and `team/assistant/AssistantTail.newestSignal()`, D-151.
- The two tails are not in the signal singleton: `DiagnosticsCounters` holds only the counters, and the rings live in `CspViolations`, 100 kept, and `AssistantTail`, 200 kept, D-151.
- `RequestStartedEvent` is not listened to: request timing comes from `RequestEndedEvent.getDuration()` alone, `DiagnosticsListeners`. No decision row.
- Sessions counts one person who just signed in as two, because the sign in page's session lives on until it times out, D-154, and says so under the number, P-19.
- The extended client details are read once when the view opens, and only the viewport follows the window, `DiagnosticsView`, D-155.
- The colour scheme row shows `ExtendedClientDetails.getColorScheme()`, which reports what the application set, not the reader's preference. Known: flow#26390, `DiagnosticsView`.
- The postponed and not shown outcomes have never been produced on real traffic. Only the enum mapping test covers them, `Outcome.of`.
- Workaround to remove when flow#26338 is fixed, draft 23b: `forceProductionBuild` in the `it` and `production` profiles of `pom.xml`, D-156.
- Since D-237 the view has the team's side navigation in the left panel and the drawer toggle like every internal page, but no route panel. Since D-225 it has the content padding of the other internal pages, and a table wider than its column scrolls inside its panel, `styles/team/diagnostics.css`. The prototype has no diagnostics page: the whole view is an app extra.
- Both tasks of US-12.1 stay unticked: the first for the CSP and interception listeners and `RequestStartedEvent`, the second because the rings are not in the singleton, D-151, D-161.

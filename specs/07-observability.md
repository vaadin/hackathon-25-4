# 07 Observability

Small on purpose. This application's own diagnostics view is the product here, because the 25.4 arrivals it exercises are the point; a metrics stack behind it is not.

## What is listened to

All through the `VaadinService` event bus and the 25.4 lifecycle events, registered once in a service init listener:

| Event | What is kept |
| --- | --- |
| `NavigationStartedEvent`, `NavigationEndedEvent` | Route template, duration, and the outcome: completed, refused (not found or not allowed), broke (any other failure), not shown, postponed, D-233; at most 100 routes, the rest under one "Other routes". The outcome enum is new in 25.4 and the whole reason this view exists |
| `RequestEndedEvent` | Request counts and durations per minute and per handler kind, the last 60 minutes. `RequestStartedEvent` is not listened to |
| Session lifecycle | Live session count |
| CSP violations | The last N reports, because `04-security.md` turns enforcement on and somebody has to see what it catches |
| Assistant interception | The last N prompts and tool calls, from `06-ai.md` |

Counters live in a singleton holding signals, ring buffered, never persisted. Restart means zero, which a diagnostics view may say.

## The diagnostics view

`/diagnostics`, role `SECURITY`. A board of six cards on the commercial `Dashboard`, D-232: navigations by route and outcome, request timings, sessions now with the data's age, the reader's browser, policy violations, and assistant exchanges. Edit cards moves, resizes and removes them, Add a card brings a removed one back, Restore the default cards puts the six back, and the arrangement is kept in the browser's local storage. Every card is bound to a shared signal. Everything updates live through the signals, no refresh button, because a diagnostics view with a refresh button is a screenshot.

Extended client details, typed in 25.4, are shown for the reader's own session: timezone, pixel ratio, viewport. One session's facts, to prove the API, not a fleet view.

## What is not here

Observability Kit, Prometheus, OpenTelemetry: out. They instrument production fleets, this is a hackathon application with one instance, and the 25.3 hackathon already walked that path. The decision is recorded in `00-overview.md`'s unclaimed table. If the application ever runs for the team in earnest, the Kit drops in beside this view without replacing it.

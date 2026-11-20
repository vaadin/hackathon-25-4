# 07 Observability

Small on purpose. This application's own diagnostics view is the product here, because the 25.4 arrivals it exercises are the point; a metrics stack behind it is not.

## What is listened to

All through the `VaadinService` event bus and the 25.4 lifecycle events, registered once in a service init listener:

| Event | What is kept |
| --- | --- |
| `NavigationStartedEvent`, `NavigationEndedEvent` | Route, duration, and the outcome: completed, failed, not shown, postponed. The outcome enum is new in 25.4 and the whole reason this view exists |
| `RequestStartedEvent`, `RequestEndedEvent` | Request counts and durations, bucketed per minute |
| Session lifecycle | Live session count |
| CSP violations | The last N reports, because `04-security.md` turns enforcement on and somebody has to see what it catches |
| Assistant interception | The last N prompts and tool calls, from `06-ai.md` |

Counters live in a singleton holding signals, ring buffered, never persisted. Restart means zero, which a diagnostics view may say.

## The diagnostics view

`/diagnostics`, role `SECURITY`. Four panels bound to the signals: navigation outcomes as counts per route, request timing summary, sessions now, and the two event tails. Everything updates live through the signals, no refresh button, because a diagnostics view with a refresh button is a screenshot.

Extended client details, typed in 25.4, are shown for the reader's own session: timezone, pixel ratio, viewport. One session's facts, to prove the API, not a fleet view.

## What is not here

Observability Kit, Prometheus, OpenTelemetry: out. They instrument production fleets, this is a hackathon application with one instance, and the 25.3 hackathon already walked that path. The decision is recorded in `00-overview.md`'s unclaimed table. If the application ever runs for the team in earnest, the Kit drops in beside this view without replacing it.

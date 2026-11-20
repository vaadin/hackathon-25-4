# 10 The dev loop

## What it is

The coding agent dev loop that 25.3 introduced: a daemon that holds the application, applies edits, and answers with one exit code, so an agent can verify a change without a full restart. It is installed in epic 01 and it is the engine of the polish pass.

```
./mvnw vaadin:install-dev-cli
.vaadin/vaadin-dev start
.vaadin/vaadin-dev status
.vaadin/vaadin-dev apply
.vaadin/vaadin-dev restart
```

If the goal is still hidden behind the `flow` prefix on beta2, as it was on 25.3.0-beta1, that is a row for `specs/FEEDBACK-25.4.md` and the `flow:install-dev-cli` spelling is the workaround.

## What each kind of edit costs

Measured on 25.3 and to be re measured here in epic 01, because these numbers decide how the polish pass batches its edits:

| Edit | Expected cost |
| --- | --- |
| A method body in a view | Hot swap, under a second |
| A new bean or a signature change | Restart, several seconds |
| CSS under `META-INF/resources` | Push to the open browser, immediate |
| A translation bundle | Restart, so never batch one with a Java edit while iterating |

## Rules, learned in 25.3 and kept

1. Open the browser before the first `apply`. A CSS push needs a page already connected.
2. Never run the application through Maven and through the daemon at the same time.
3. Do not batch a resource edit with a Java edit while iterating: one resource turns a hot swap into a restart.
4. Run `./mvnw clean verify` after a dev loop session, because the daemon's compiler flags are not the build's and the gate is the gate.
5. The daemon compiles Java only. Anything else needs a restart.

## Verification inside the loop

The loop's answer is compile state, not behaviour. Behaviour is verified the `verify-ui.md` way, Playwright against the running application, accessibility snapshot first. During the vibe coding pass no tests run at all, by the schedule in `00-overview.md`, and the loop plus the open browser is the whole cycle.

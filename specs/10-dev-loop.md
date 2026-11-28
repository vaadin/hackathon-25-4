# 10 The dev loop

## What it is

The coding agent dev loop that 25.3 introduced: a daemon that holds the application, applies edits, and answers with one exit code, so an agent can verify a change without a full restart. It is installed in epic 01 and it is the engine of the polish pass.

```
./mvnw vaadin:install-dev-cli
.vaadin/vaadin-dev start
.vaadin/vaadin-dev status
.vaadin/vaadin-dev apply
.vaadin/vaadin-dev restart
.vaadin/vaadin-dev stop
.vaadin/vaadin-dev shutdown
```

`stop` stops the application, and `shutdown` stops the daemon too, which is the only way to change the environment it passes on.

Verified on 25.4.0-beta2 in US-1.5: `./mvnw vaadin:install-dev-cli` works under the `vaadin` prefix, so the 25.3 workaround is not needed. The daemon inherits the environment of the shell that starts it: `VAADIN_LAUNCH_BROWSER=false` there keeps it from opening a browser.

## What each kind of edit costs

Measured on 25.4.0-beta2 on 2026-11-20, on this application at the end of epic 01, one edit per `apply`. These numbers decide how the polish pass batches its edits:

| Edit | Measured | What the loop answered |
| --- | --- | --- |
| `start`, cold | 16.4 s | `running` |
| A method body in a view | 1.5 s | `hot-reload: redefineClasses(1)` |
| A new bean plus a constructor that takes it | 11.3 s | `restart: new Spring bean`, the context has no definition for it |
| CSS under `META-INF/resources` | 0.1 s | `hmr: 1 resource(s) copied, pushed 1 stylesheet(s) in place` |
| The translation bundle | 10.3 s | `restart: translations.properties changed (read only while the app starts)` |
| `shutdown` | 0.5 s | `stopping app it owns` |

The loop says why it restarted, every time. A restart costs ten to eleven seconds whatever caused it, so a bundle edit is seven times a Java edit and a hundred times a CSS edit.

## Rules, learned in 25.3 and kept

1. Open the browser before the first `apply`. A CSS push needs a page already connected.
2. Never run the application through Maven and through the daemon at the same time.
3. Do not batch a resource edit with a Java edit while iterating: one resource turns a hot swap into a restart.
4. Run `./mvnw clean verify` after a dev loop session. The daemon writes its own classes into `target/classes`, and Maven trusts them when they are newer than the sources, so without `clean` a build can pass over code that does not compile; the gate is the gate.
5. The daemon compiles Java only. Anything else needs a restart.

## Verification inside the loop

The loop's answer is compile state, not behaviour. Behaviour is verified the `verify-ui.md` way, Playwright against the running application, accessibility snapshot first. During the vibe coding pass no tests run at all, by the schedule in `00-overview.md`, and the loop plus the open browser is the whole cycle.

REPO: vaadin/flow
TITLE: A signal effect runs without the session lock when a RequestEndedEvent listener changes a shared signal
---
### Description

A `RequestEndedEvent` listener, new in 25.4, that writes a `SharedValueSignal` runs every element effect of the request's own UI on the request thread, without the session lock, and the effect fails:

```java
event.getSource().getEventBus().addListener(RequestEndedEvent.class,
        ended -> ENDED.update(count -> count + 1));

// in a view
Signal.effect(count, () -> count.setText("requests ended: " + ENDED.get()));
```

```
Exception in thread "http-nio-8125-exec-1" java.lang.IllegalStateException: Cannot access state in VaadinSession or UI without locking the session.
	at com.vaadin.flow.server.VaadinSession.checkHasLock(VaadinSession.java:683)
	at com.vaadin.flow.dom.ElementEffect.lambda$enableEffect$ff2b8a2a$1(ElementEffect.java:272)
```

The effect's own action throws, and then the error handler throws again on `getErrorHandler()`. `ElementEffect.enableEffect` decides with `UI.getCurrent() == ui` alone:

```java
if (UI.getCurrent() == ui) {
    // Run immediately if on the same UI
    command.run();
} else { ... ui.access ... }
```

When the request ends, the lock is already released but `UI.getCurrent()` still names the request's UI, so the effect takes the inline branch. Effects of other UIs go through the dispatcher and work.

### Why it matters

A request end listener is where request counters and timings belong, and the shared signal is how they reach a live view. The view that shows them is exactly the one whose own requests trigger the failure: the number stops moving after the first click, and the log fills with an exception per request. In the application where it was found, the lost update also took the response of the click with it, so a button on the same page did nothing.

### Expected

The inline branch checks that the current thread holds the session lock, `ui.getSession().hasLock()`, and otherwise the effect is dispatched through `ui.access` like any other thread's.

### Workaround

The listener clears the current instances around the signal write, so the effect sees no current UI and is dispatched:

```java
var current = CurrentInstance.getInstances();
CurrentInstance.clearAll();
try {
    ENDED.update(count -> count + 1);
} finally {
    CurrentInstance.restoreInstances(current);
}
```

### Reproduce

1. `mvn spring-boot:run`, and open <http://localhost:8125/>.
2. The page shows how many requests have ended, 3 on a fresh start. Press Click.
3. The number does not move however many times Click is pressed, and the log shows the exception above for the requests of this page.

With the workaround in `RequestCounter`, the number moves with every click, 34 to 36 after two, and the log stays clean.

`RequestEndedEvent` is new in 25.4, so the project does not compile on 25.3.5.

Found on 25.4.0-beta2.

### Getting the project

[`25-effect-unlocked-request-end.zip`](https://github.com/vaadin/hackathon-25-4/raw/COMMIT_SHA/specs/issues/projects/25-effect-unlocked-request-end.zip), 4 KB, sources only. Unzip it and:

```
mvn spring-boot:run
```

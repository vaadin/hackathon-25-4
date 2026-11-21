REPO: vaadin/flow
TITLE: A shared signal changed by another thread before a component is attached never reaches its bindings
---
### Description

A component binds a text to a `SharedValueSignal` in its constructor. Before the component is attached, another thread sets the signal. After the attach the bound text still shows the old value, and it stays there: a later round trip finds `peek()` returning the new value and the component still holding the old one.

```java
bound.bindText(signal);                       // in the constructor, value "initial"
// before the view is attached, in beforeEnter:
Thread other = Thread.ofPlatform().start(() -> signal.set("changed"));
other.join();
signal.peek();                                // "initial", on the request thread, after the join
// attached, rendered, and one round trip later:
signal.peek();                                // "changed"
bound.getText();                              // "initial"
```

The same `set` made on the request thread, at the same point, arrives: the text is "changed" when the view renders.

### Why it matters

It is silent and it is permanent for that page. The usual shape is a view that asks a service for something while it is built, and the service, on its own thread, publishes a fresher value to a signal every session watches. The session that caused the refresh is the one that never sees it. Everyone who opens the page afterwards does, so it looks like a problem that fixes itself on reload.

A browserless test does not show it: there a change made on the test thread runs the binding inline.

### Expected

What the Effects and Computed Signals page says under When Effects Run: "Apart from the first run when they're created, effects and bindings of detached components don't run. When the component is attached again, the effect runs if a dependency has changed in the meantime."

The dependency changed in the meantime, so the binding should run on attach whichever thread made the change.

### Workaround

Hand the change to `VaadinSession.access` when a session is current, so it runs once the request is over and the component is attached.

### Reproduce

1. `mvn spring-boot:run`, then open <http://localhost:8111/?by=other>.
2. The first line is the bound text, the second what the server saw before the attach. Press Ask again for the third line, which is one round trip later.
3. Open <http://localhost:8111/?by=same> and do the same. That is the control: the same change made on the request thread.

What we read on 25.4.0-beta2, with Chrome:

| Route | Bound text when rendered | Before the attach | One round trip later |
| --- | --- | --- | --- |
| `/?by=other` | `initial` | `attached=false, peek()=initial` | `peek()=changed, the bound text is initial` |
| `/?by=same` | `changed` | `attached=false, peek()=changed` | `peek()=changed, the bound text is changed` |

Three seconds of waiting on `/?by=other` before pressing the button change nothing, with `@Push` on. The server log agrees: the effect beside the binding prints `the effect ran with initial` once for `/?by=other` and never again.

Note the second column of the first row as well: on the request thread `peek()` still returns the old value after the thread that called `set` has finished.

### Getting the project

[`11-signal-changed-before-attach.zip`](https://github.com/vaadin/hackathon-25-4/raw/COMMIT_SHA/specs/issues/projects/11-signal-changed-before-attach.zip), 3 KB, sources only. Unzip it and:

```
mvn spring-boot:run
```

Found on 25.4.0-beta2, whose `flow-server` is 25.4.0-beta1.

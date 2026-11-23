REPO: vaadin/flow
TITLE: A shared signal changed by another thread before a component is attached never reaches its bindings
---
### Description

A component binds a text to a `SharedValueSignal` in its constructor. Before the component is attached, another thread sets the signal. After the attach the bound text still shows the old value, and it stays there: a later round trip finds `peek()` returning the new value and the component still holding the old one.

The same `set` made on the request thread, at the same point, arrives: the text is the new value when the view renders.

### Why it matters

It is silent and it is permanent for that page. The usual shape is a view that asks a service for something while it is built, and the service, on its own thread, publishes a fresher value to a signal every session watches. The session that caused the refresh is the one that never sees it. Everyone who opens the page afterwards does, so it looks like a problem that fixes itself on reload.

A browserless test does not show it: there a change made on the test thread runs the binding inline.

### Expected

What the Effects and Computed Signals page says under When Effects Run: "Apart from the first run when they're created, effects and bindings of detached components don't run. When the component is attached again, the effect runs if a dependency has changed in the meantime."

The dependency changed in the meantime, so the binding should run on attach whichever thread made the change.

### Workaround

Hand the change to `VaadinSession.access` when a session is current, so it runs once the request is over and the component is attached.

### Reproduce

One view, with `@Push` on the application shell:

```java
@Route("")
public class ChangedBeforeAttachView extends Div implements BeforeEnterObserver {

    private final SharedValueSignal<String> signal = new SharedValueSignal<>("initial");
    private final Span bound = new Span();
    private final Paragraph log = new Paragraph();

    public ChangedBeforeAttachView() {
        bound.bindText(signal);
        Signal.effect(this, () -> System.out.println("the effect ran with " + signal.get()));
        var later = new Paragraph();
        var ask = new NativeButton("Ask again", click -> later.setText(
                "a round trip later: peek()=" + signal.peek() + ", the bound text is " + bound.getText()));
        add(bound, log, ask, later);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String by = event.getLocation().getQueryParameters().getSingleParameter("by").orElse("other");
        if (by.equals("same")) {
            signal.set("changed");
        } else {
            Thread other = Thread.ofPlatform().start(() -> signal.set("changed"));
            try {
                other.join();
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        }
        log.setText("changed by the " + by + " thread before attach, attached=" + isAttached()
                + ", peek()=" + signal.peek());
    }
}
```

1. Open `/?by=other`. The first line is the bound text, the second what the server saw before the attach. Press Ask again for the third line, one round trip later.
2. Open `/?by=same` and do the same. That is the control: the same change made on the request thread.

What we read, the same on 25.4.0-beta2 and on 25.3.5, with Chrome:

| Route | Bound text when rendered | Before the attach | One round trip later |
| --- | --- | --- | --- |
| `/?by=other` | `initial` | `attached=false, peek()=initial` | `peek()=changed, the bound text is initial` |
| `/?by=same` | `changed` | `attached=false, peek()=changed` | `peek()=changed, the bound text is changed` |

Three seconds of waiting on `/?by=other` before pressing the button change nothing. The server log agrees: the effect beside the binding prints `the effect ran with initial` once for `/?by=other` and never again.

Note the second column of the first row as well: on the request thread `peek()` still returns the old value after the thread that called `set` has finished.

Found on 25.4.0-beta2 and 25.3.5.

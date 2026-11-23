REPO: vaadin/flow-components
TITLE: Button refuses bindEnabled beside disable on click, also in the new UNTIL_RESPONSE mode
---
### Description

A Check button that is disabled while its form is empty, and disabled on click until the server answers, so a slow answer cannot be asked twice:

```java
check.setDisableOnClick(DisableOnClickMode.UNTIL_RESPONSE);
check.bindEnabled(asked);   // IllegalStateException
```

```
IllegalStateException: Binding the enabled state to a signal is not supported when disable on click is active.
```

In the other order `setDisableOnClick` throws `Disable on click is not supported when the enabled state is bound to a signal.` Calling `setEnabled(false)` beside `UNTIL_RESPONSE` is accepted.

### Why it matters

The two are the obvious pair for any submit button, and both are the recommended way: the signals pages show `bindEnabled` on a submit button, the Button page shows `UNTIL_RESPONSE` for preventing double submits. Whoever combines them gets an exception at construction, and the way out is to write by hand, with `Signal.effect` and `setEnabled`, the binding the API refuses.

### Expected

`bindEnabled` accepted in `UNTIL_RESPONSE` mode. The Javadoc gives the reason for the refusal: disable on click changes the enabled state on its own and a one way binding cannot write that back to the signal. In `UNTIL_RESPONSE` the button enables itself again with the next response, so the bound state is the one that holds before and after the click, and there is nothing to write back. The restriction could stay for `UNTIL_ENABLED`, where it still has its reason.

If it has to stay, the Button page could say so next to the `UNTIL_RESPONSE` example, since today only the Javadoc does.

### Workaround

```java
Signal.effect(check, () -> check.setEnabled(asked.get()));
```

### Reproduce

Three calls on a new `Button`, each in its own test. On 25.4.0-beta2 they print:

```
RESULT setDisableOnClick(UNTIL_RESPONSE), then bindEnabled: IllegalStateException: Binding the enabled state to a signal is not supported when disable on click is active.
RESULT bindEnabled, then setDisableOnClick(UNTIL_RESPONSE): IllegalStateException: Disable on click is not supported when the enabled state is bound to a signal.
RESULT setDisableOnClick(UNTIL_RESPONSE), then setEnabled(false): accepted
```

Found on 25.4.0-beta2.

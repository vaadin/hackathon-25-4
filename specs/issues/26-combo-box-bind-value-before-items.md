REPO: vaadin/flow-components
TITLE: ComboBox.bindValue before setItems drops the bound value, with only a log line
---
### Description

A `ComboBox` bound to a signal that already holds a value loses that value when `bindValue` comes before `setItems`:

```java
var remembered = new ValueSignal<>("24.9.21");
var version = new ComboBox<String>();
version.setAllowCustomValue(true);
version.bindValue(remembered, remembered::set);
version.setItems(List.of("25.3.5", "24.9.21"));
// version.getValue() is null
```

The binding applies the signal's value at once, `setValue` refuses it because the combo box has no items yet, and the exception goes to the uncaught exception handler:

```
Exception in thread "main" java.lang.IllegalStateException: Cannot set a value for a ComboBox without items. Use setItems to populate items into the ComboBox before setting a value.
```

The view is built anyway, with an empty field. Once `setItems` runs, the binding does not try again, so the value only appears the next time the signal changes. In the other order, `setItems` then `bindValue`, the field shows `24.9.21`.

### Why it matters

Restoring a remembered value is the common case for a bound field, and nothing on the page says it failed. The order of two calls on the same field decides whether it works, and the rule that explains it, "no value without items", is written for `setValue`, which the application never called. In the application where it was found, a lookup field that remembers the reader's version came up empty on every page built after the version was set, with a stack trace in the log each time.

### Expected

In order of usefulness:

1. The binding waits: a value the combo box cannot take yet is applied when items arrive.
2. `bindValue` refuses at the call, with a message that says to set the items first, instead of a failure inside the effect.
3. The `bindValue` documentation of `ComboBox` says that items must be set first.

### Workaround

`setItems` is called before `bindValue`.

### Reproduce

Two routes that differ only in the order of the two calls, `/bind-first` and `/items-first`, navigated to in a browserless test that prints the field's value. On 25.4.0-beta2 it printed:

```
REPRO setItems, then bindValue: value 24.9.21
Exception in thread "main" java.lang.IllegalStateException: Cannot set a value for a ComboBox without items. Use setItems to populate items into the ComboBox before setting a value.
REPRO bindValue, then setItems: value null
```

`-Dvaadin.version=25.3.5` prints the same.

Found on 25.4.0-beta2, and the same on 25.3.5.

### Getting the project

[`26-combo-box-bind-value-before-items.zip`](https://github.com/vaadin/hackathon-25-4/raw/COMMIT_SHA/specs/issues/projects/26-combo-box-bind-value-before-items.zip), 5 KB, sources only. Unzip it and:

```
mvn test
```

It prints the lines above.

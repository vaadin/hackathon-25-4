REPO: vaadin/flow
TITLE: The error callback of the Clipboard API takes a type from an internal package
---
### Description

Every observed write and read of `ClipboardBinding` takes its error callback as `SerializableConsumer<PromiseAction.Error>`, and `PromiseAction` lives in `com.vaadin.flow.component.trigger.internal`:

```java
Clipboard.onClick(copy).writeText(text, copied -> ..., error -> ...);
// error is a com.vaadin.flow.component.trigger.internal.PromiseAction.Error
```

A lambda hides it. Anything that names the type does not: a shared error handler, a field, a method that builds the binding, a test. Each of them has to `import com.vaadin.flow.component.trigger.internal.PromiseAction`. The type also has no page in the published javadoc, which leaves internal packages out, so the `name()` and `message()` the Clipboard documentation page describes cannot be looked up there.

### Why it matters

The observed form is the one the documentation recommends for telling the user whether the copy worked, and the first application that writes one error handler for all its copy buttons imports an internal class, which the project says may change in any release.

#26576 proposed making the trigger and action API public and was closed, with the part that passes data between them kept internal because it is not settled yet. This asks for less: only the type that a public signature already exposes.

### Expected

The error record the callbacks receive is public API. Options in this order:

1. A public type of its own, such as `Clipboard.Error`, which leaves the trigger API's own decision untouched.
2. `PromiseAction` moved to a public package, as #26576 proposed.

### Workaround

A lambda at every call site, so the type is never named. Our copy answer control does that.

### Reproduce

A shared handler, as a real application declares it, compiles only with the internal import:

```java
import com.vaadin.flow.component.trigger.internal.PromiseAction;

static void copyFailed(PromiseAction.Error error) {
    Notification.show(error.message());
}
```

Reading the parameter type of `writeText(String, onCopied, onError)` by reflection prints, on 25.4.0-beta2 and on 25.3.5:

```
ClipboardBinding.writeText(String, onCopied, onError): onError accepts com.vaadin.flow.component.trigger.internal.PromiseAction$Error
```

Found on 25.4.0-beta2, and the same on 25.3.5.

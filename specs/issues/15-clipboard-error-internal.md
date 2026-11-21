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

The observed form is the one the documentation recommends for telling the user whether the copy worked, and the first application that writes one error handler for all its copy buttons imports an internal class, which the project says may change in any release. flow#26576 proposed moving `PromiseAction` out of the internal package and was closed without merging, so nothing tracks it now.

### Expected

The error record the callbacks receive is public API: a public `Clipboard.Error`, or `PromiseAction` moved to a public package as flow#26576 proposed. Options in that order, because the first keeps the trigger API's own decision separate.

### Workaround

A lambda at every call site, so the type is never named. Our copy answer control does that.

### Reproduce

`mvn test` runs one test. It declares the shared handler a real application would, which only compiles with the internal import, then reads the parameter type of `writeText(String, onCopied, onError)` by reflection, prints it and fails. On 25.4.0-beta2 it printed:

```
ClipboardBinding.writeText(String, onCopied, onError): onError accepts com.vaadin.flow.component.trigger.internal.PromiseAction$Error
```

`mvn test -Dvaadin.version=25.3.5` prints the same line.

### Getting the project

[`15-clipboard-error-internal.zip`](https://github.com/vaadin/hackathon-25-4/raw/COMMIT_SHA/specs/issues/projects/15-clipboard-error-internal.zip), 3 KB, sources only. Unzip it and:

```
mvn test
```

Found on 25.4.0-beta2, and the same on 25.3.5.

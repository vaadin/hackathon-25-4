REPO: vaadin/flow
COMMENT-ON: 5473
---
Still so on 25.4.0-beta2, measured from the 25.4 hackathon, where a lookup takes the slash key from anywhere on the page as the way to focus it.

`UI.addShortcutListener(() -> search.focus(), Key.of("/"))` fires inside a text field as well, and since a shortcut prevents the default, the slash cannot be typed there at all. Typing `a/b` into another field leaves `ab` in it and the focus in the search field.

25.4 is close to having the answer. The filter of a shortcut is now declared JavaScript, `ShortcutRegistration.ShortcutFilterJs`, and a DOM listener takes a declared `DomEventFilter` through `setFilterExpression`. But `ShortcutFilterJs` is marked for internal use and `ShortcutRegistration` has no way to add a condition of one's own. What we use instead is a plain listener with a declared filter, which also works under a content security policy without `unsafe-eval`:

```java
@JsDefinition
public interface SlashJs extends Serializable {

    @JsExpression("return event.key === '/' && !event.composedPath().some(node => node instanceof HTMLElement && (node.isContentEditable || ['INPUT', 'TEXTAREA', 'SELECT'].includes(node.tagName)))")
    DomEventFilter slashOutsideAField();
}

UI.getCurrent().getElement().addEventListener("keydown", event -> search.focus())
        .setFilterExpression(JsSnippets.of(SlashJs.class).slashOutsideAField()).preventDefault();
```

It loses what the shortcut API gives for free: the key and modifier matching, the lifecycle owner, the modal scope check. A `ShortcutRegistration` option that skips events from an editable element, as the issue suggests, or one that accepts a `DomEventFilter`, would keep those.

### Reproduce

Two routes in one project, the same page with the two registrations. Click Note and type `a/b`, then click the text above the fields and press `/`. On 25.4.0-beta2, Chrome:

| Route | Note after typing `a/b` | Focus after typing | Slash from outside a field |
| --- | --- | --- | --- |
| `/`, `addShortcutListener` | `ab` | Search | focuses Search |
| `/filtered`, listener with a declared filter | `a/b` | Note | focuses Search |

[`12-shortcut-while-typing.zip`](https://github.com/vaadin/hackathon-25-4/raw/COMMIT_SHA/specs/issues/projects/12-shortcut-while-typing.zip), 3 KB, sources only, `mvn spring-boot:run`, port 8112.

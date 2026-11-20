REPO: vaadin/flow
COMMENT-ON: 26391
---
A measurement for this from the 25.4 hackathon, on 25.4.0-beta2, for the part about the first page rendering with the preference.

An application that remembers "dark" through `WebStorage` can only apply it after attach, one round trip after the first paint. With the system in light mode and "dark" remembered, the reproducer prints:

```
first frame text color: rgb(0, 0, 0)
current text color:     rgb(255, 255, 255)
system prefers dark:    false
ExtendedClientDetails.getColorScheme(): DARK
Page.getColorScheme():                  DARK
```

The first frame is the light page, the dark one arrives after it. The only way we found to avoid the flash is an inline script in the document head, added through `AppShellSettings.addInlineWithContents`, that reads `localStorage` and sets `color-scheme` on the root element before any stylesheet. That works, and it leaves the storage key in two places, Java and a script string, and an inline script for a content security policy to allow.

So for the store proposed here, "readable when a session starts" is what removes the script: the value has to be in the bootstrap response, not one request later. A cookie does that, `localStorage` cannot.

The last two lines are #26390: the accessor reports what the application set, not what the system prefers.

### Reproduce

1. Run the project, open `http://localhost:8107/`, press **Remember dark**.
2. Reload, then press **Measure**.

[`07-color-scheme.zip`](https://github.com/vaadin/hackathon-25-4/raw/COMMIT_SHA/specs/issues/projects/07-color-scheme.zip), 3 KB, sources only, `mvn spring-boot:run`.

# Story implementation

## Before writing anything

- Read the feature document and the story. The acceptance criteria are the contract.
- Query the Vaadin MCP for every 25.4 API you are about to use, with `ui_language: "java"` and `vaadin_version: "25.4"`. Do not write from memory, this is a beta.
- Check `specs/CHANGELOG-RISK.md` if the API is preview. Use the adapter, add the fallback.

## Package layout

Feature packages under `com.vaadin.securitystatus`. A feature owns its views, its read models and its tests. Every package has a `package-info.java` with `@NonNullApi`.

## Rules that get code rejected

- Anything copied from the JavaScript apps in vaadin/cve-status.
- A user visible string literal in Java.
- A view fetching from the network. Only `data/` talks to Pages, and only through the cache.
- A value the data does not carry rendered as if it did. Absent means omitted.
- An `onAttach` or `onDetach` override where `whenAttached` would do.
- A `Thread.sleep` in a test.
- CSS outside `src/main/resources/META-INF/resources`.
- A commercial API used without the degradation `specs/CHANGELOG-RISK.md` names for it.

## Patterns to follow

- Constructor injection. No field injection in views.
- Cross view state in a `@VaadinSessionScope` bean holding signals.
- Derived values with `Signal.computed`, lists with `bindChildren`, grids with `Signal.effect(grid, () -> grid.setItems(...))`.
- Fields that feed a signal use `ValueChangeMode.EAGER`.
- Records for read models, mapped one to one from the Pages JSON field names. Never a display DTO with preformatted strings.
- Versions compared through the `VersionNumber` type in `data/`, never through string comparison.

## Gate

`./mvnw compile` and `./mvnw test`. Red build means stop and fix, not continue and hope.

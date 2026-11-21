# Feedback for the Vaadin teams: 25.4

Everything found while building this application on 25.4.0-beta2 that a Vaadin team could act on: an API that is missing, one that is awkward, one that is documented but absent, and every place where we had to write a workaround.

This file is part of the specifications on purpose. If this application is ever rebuilt from these specifications, whoever does it should hit none of this by surprise, and should check whether each entry is still true.

**How to add an entry.** One row per finding, in the table that matches its kind. Say what was expected, what happened, what we did instead, and where in this repository the workaround lives. If it is fixed later, do not delete the row: mark it fixed and in which version, because the code that works around it still has to be cleaned up.

**What happens to these.** Nothing goes straight from here to an issue tracker. The pipeline is `specs/issues/`: a draft per finding, a minimal reproducer per draft, a check against existing issues, and the posting script with its dry run. A row is a suspicion, a project is a fact. When an issue is opened, its link goes at the end of the row. A finding with no link is a finding nobody outside this repository has seen.

Routing, when drafting: an API on the server, a binder, a signal, the router goes to `vaadin/flow`. How a component behaves in the browser goes to `vaadin/web-components`. A Java component API and the AI family go to `vaadin/flow-components`. Browserless testing goes to `vaadin/browserless-test`. Something documented that does not exist goes to `vaadin/docs`, naming the page. A version or BOM problem, or a case where nobody can tell which module owns it, goes to `vaadin/platform`.

## Missing or absent APIs

| Finding | Expected | What happened | What we did instead | Where the workaround lives |
| --- | --- | --- | --- | --- |
| No server side way to apply a remembered color scheme before the first paint | `@ColorScheme`, or a sibling of it, that names a web storage key and lets the bootstrap page apply the stored value itself | `WebStorage` answers one round trip after the page is painted, so reading the choice from Java always paints the system scheme first and repaints. The 25.4 storage additions do not change when the answer arrives | An inline script in the document head, added through `AppShellSettings.addInlineWithContents`, sets `color-scheme` on the root element from `localStorage`. The storage key now lives in Java and in a script string | `base/ui/ThemeMemory.java`, `Application.configurePage`. Known: flow#26391. Measurement drafted as a comment in `issues/07b-comment-on-flow-26391.md`, ready |
| No typed accessor for the system's color scheme preference. Known: flow#26390, open | `ExtendedClientDetails` to say what the operating system prefers, next to the 25.4 typed accessors it gained | Corrected by the reproducer: `getColorScheme()` reports what the application set, `LIGHT_DARK` here, never `prefers-color-scheme`. A switch that must start in the right position has to ask the browser itself | A declared `@JsDefinition` expression with `matchMedia`, read once when the switch attaches | `base/ui/ThemeSwitch.java`. Mentioned in `issues/07b-comment-on-flow-26391.md` |
| A shortcut cannot be kept quiet while the reader types in a field: the 25.4 filter, `ShortcutFilterJs`, is internal. Known: flow#5473, open since 2019. Draft `specs/issues/12b-comment-on-flow-5473.md` | The slash key focuses the lookup from anywhere except while another field has focus, through the 25.4 "keyboard shortcuts with JS filters" | `UI.addShortcutListener(..., Key.of("/"))` fires inside a text field too, and since a shortcut prevents the default, the slash cannot be typed there at all. 25.4 moved the shortcut filter into a declared `ShortcutFilterJs`, but it is marked for internal use, and `ShortcutRegistration` has no way to add a condition | A plain `keydown` listener on the UI element with a declared `DomEventFilter` that passes the slash only when no field, editable region or select is on the event's path, and `preventDefault()` on the registration. The same for Escape on the lookup fields | `status/Lookup.KeysJs` |

## Awkward APIs

| Finding | Expected | What happened | What we did instead | Where the workaround lives |
| --- | --- | --- | --- | --- |
| **Withdrawn, documented.** `VaadinExpiredSessionStrategy` only acts under Spring Security concurrency control | The 25.4 strategy handles "the session expired" | Stated twice on `flow/security/spring/security-configuration`, which we read after writing this row. An ordinary timeout or a server restart never reaches it, and the two demo accounts are shared, so no session limit is set and the strategy is installed but idle | The one notice comes from `CustomizedSystemMessages` with the session expired notification enabled and no redirect URL. The strategy is still passed explicitly so a later session limit needs no other change | `base/security/SessionExpiredNotice.java`, `base/security/SecurityConfig.java`. Epitaph in `issues/06-expired-session-strategy.md` |
| `Button.bindEnabled` is refused beside disable on click, also in the 25.4 `UNTIL_RESPONSE` mode. Draft `specs/issues/13-bind-enabled-until-response.md` | A Check button that is disabled while the question is empty, `bindEnabled`, and disabled on click until the navigation answers, `setDisableOnClick(DisableOnClickMode.UNTIL_RESPONSE)` | `IllegalStateException: Binding the enabled state to a signal is not supported when disable on click is active.` The Javadoc gives the reason: the button changes its own enabled state and cannot write it back to the signal. In `UNTIL_RESPONSE` the button enables itself again with the next response, so there is nothing to write back, and the restriction outlives its reason. The documentation page of the new mode says nothing about it | A `Signal.effect` that calls `setEnabled`, which is what the binding would do | `status/Lookup.checkButton` |

## Bugs routed around

| Finding | Expected | What happened | What we did instead | Where the workaround lives |
| --- | --- | --- | --- | --- |
| A shared signal changed by another thread while a component is built and not yet attached never reaches that component's bindings. Draft `specs/issues/11-shared-signal-changed-before-attach.md`. Not tried on 25.3 | What `flow/ui-state/effects-computed` says: effects of detached components do not run, and "when the component is attached again, the effect runs if a dependency has changed in the meantime" | The first reader after a cold start found the moved index while its view was being built, the new `generated_at` was published from another thread, and that reader's measured chip stayed hidden for the life of the page, with the signal holding the value and every later page showing it. The reproducer shows the same with one `bindText`: the text stays at the old value after the attach and after a later round trip, while `peek()` returns the new one. The same change made on the request thread arrives. A browserless test does not show it, because there the change runs inline | The publish found by a reader is handed to `VaadinSession.access`, so it runs when the request is over and the components are attached. Verified in a browser: cold start, first reader, chip shown | `DataConfiguration.afterTheCurrentRequest` in `data/` |

## Documentation gaps

| Finding | Expected | What happened | What we did instead | Where the workaround lives |
| --- | --- | --- | --- | --- |
| The Content Security Policy page does not say how `vaadin.csp=strict` behaves | The page for the feature says when the header is sent, where the policy is adjusted and which responses carry it | In development mode strict sends no header at all, only production does. The hook, `IndexHtmlResponse.getContentSecurityPolicy()`, is on no page and was found by reading `IndexHtmlRequestHandler`. Only the bootstrap page carries the policy. So "every later epic develops under the policy" cannot be done with the 25.4 mode alone | Our own enforced policy on every response through Spring Security's header writer, built with the 25.4 `ContentSecurityPolicy` class, and the parameter at `warn`, which logs each call a strict policy would refuse | `base/security/CspPolicy.java`, `base/security/SecurityConfig.java`, `application.properties`. Draft `issues/08-csp-page-strict-mode.md`, ready |

## CSP directives the components forced open

Per `specs/04-security.md`, every loosened directive is a fact the teams want, recorded the moment it happens.

| Directive | Forced by | Why |
| --- | --- | --- |
| `script-src 'unsafe-inline'` | Flow, the bootstrap page | The client engine is started by an inline script. The 25.4 strict mode replaces this with a nonce, in production mode only. Our own head script for the theme memory rides on the same permission |
| `script-src 'unsafe-eval'` | Flow, the client engine, and the development tools | JavaScript the server sends as a string is compiled in the browser. After moving our one string `executeJs` to `@JsDefinition`, the `warn` mode logs no call site of ours, so what still needs this is the framework and the dev server |
| `style-src 'unsafe-inline'` | The components | Style attributes and injected style elements. Vaadin's own strict policy keeps this one too |
| `img-src data:` and `font-src data:` | Aura and the components | Icons and fonts inlined as data URLs. Vaadin's own strict policy has the same two |

## Data needs toward cve-status

Not Vaadin findings: fields a screen needed that the published JSON does not carry, per `specs/02-data-source.md`. Each becomes an additive export in the cve-status pipeline, never a computation here.

| Screen | Field | Why nothing present covers it |
| --- | --- | --- |
| Explorer front door | The short product codes, `module_codes`, in `data/front.json` | The off only address grammar wants the written down codes, which today exist only inside the 66 MB `data.json` no session can fetch. Until the export lands the explorer addresses products by name, the source spec's own 6.2.6 fallback |
| Answer by CVE | An alias map, GHSA id to the tracked id, in `cases/index.json` or as `cases/cve/<alias>.json` redirects | An advisory is published under its tracked id only, and the catalogue in the index carries no aliases, so a pasted GHSA alias of a tracked CVE cannot be answered without fetching every advisory document. Until the export lands the screen answers it only when another asked id names it, D-62 |

## The SDD starter

Observations about the start.vaadin.com spec driven starter and its templates, gathered while assembling the scaffold. Same discipline: a row, then a reproducer or a stated reason it needs none.

| Finding | Expected | What happened | What we did instead | Where |
| --- | --- | --- | --- | --- |
| The SDD framework's `architecture.md` describes a Hilla React split and JUnit 5 while the v25.4 walking skeleton it ships over is Flow only on JUnit 6 era tooling | The template matches the skeleton it lands on | Two documents disagree about the stack of a freshly generated project | Replaced the framework with this suite, per `specs/09-tooling.md` | Needs re checking against a wizard generated zip before it becomes a draft, because our assembly bypassed the wizard's own replacements |

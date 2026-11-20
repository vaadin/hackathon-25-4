# Feature 01: Foundation and shell

## Overview

The two parent layouts, the theme, security, CSP, the dev loop, and everything every other epic stands on. Covers V10 partially, H3, H4 partially, T1.

## Behaviour

### The customer shell

- One `@RouteParent` layout for the public routes: an app bar with Breadcrumbs reading Vaadin, Security status, then the leaf the route names; the measured chip bound to `DataRefreshSignal`; and the lookup component, mounted once (epic 03 fills its behaviour).
- No navigation menu. The customer screens are reached by asking, not by browsing, which is the source design's shape.
- The document title follows the heading, one writer, because a reader keeps several answers open in tabs.

### The internal shell

- A second parent for `/findings/**` and `/diagnostics`: the same app bar plus a drawer with the four internal destinations and the assistant panel mount point.
- Signed out users hitting it land on `/login` and return to where they aimed after signing in, through the security configurer's login view and success handling.

### Theme

- Aura. Dark and light per `05-theming.md`: Switch in the app bar, system default, remembered through the typed `WebStorage` accessors, decided before first paint.

### Security and CSP

- `VaadinSecurityConfigurer` with the two in memory users and the route rules of `04-security.md`.
- `ContentSecurityPolicy` enforced from this epic on, so every later epic develops under the policy instead of retrofitting it. Each directive a component forces open is recorded the moment it happens.
- `VaadinExpiredSessionStrategy` returns an expired session to its view with one notice.

### Tooling that lands here

- The dev loop installed and its costs measured into `specs/10-dev-loop.md`.
- `NoHardcodedStringsTest` and `SpecConsistencyTest` exist from this epic so every later epic is born under them.
- The example feature the walking skeleton ships is deleted: this application has no tasks and dead example code in a spec driven repo misleads the next agent.

## Edge cases

| Scenario | Behaviour |
| --- | --- |
| A route that matches nothing | Flow's not found view, with the app bar still present |
| The session expires mid reading | The same view reloads with one notice, lookup state gone |
| A CSP violation in development | Logged, visible later in diagnostics, never swallowed |
| Storage unavailable for the theme memory | System preference wins, silently, rule 6.7.8 of the source spec |

## Acceptance criteria

### AC1: Two shells
- [x] Public routes render under the customer shell with breadcrumbs and the measured chip
- [x] Internal routes refuse `guest` and anonymous, and return the signed in `team` user to the route they asked for

### AC2: Theme
- [x] Dark system preference paints dark with no light flash
- [x] The Switch overrides the system and the override survives a reload

### AC3: CSP
- [x] Responses carry the enforced policy header
- [x] The application boots and navigates with the policy on

### AC4: Guard rails
- [x] `NoHardcodedStringsTest` fails on a planted literal
- [x] `SpecConsistencyTest` fails on a planted phantom test name

## Test cases

| Id | Given | When | Then | Tier | Verified by |
| --- | --- | --- | --- | --- | --- |
| FND-01 | Anonymous | Opening `/findings` | Redirected to login, then back to `/findings` after signing in as `team` | browserless | `ShellSecurityBrowserlessTest` |
| FND-02 | Signed in as `guest` | Opening `/findings` | Refused | browserless | `ShellSecurityBrowserlessTest` |
| FND-03 | Any route | Rendering | Breadcrumbs name the leaf and the document title matches | browserless | `ShellBrowserlessTest` |
| FND-04 | A dark system preference | First paint | Dark, no flash | testbench | `ThemeFirstPaintIT` |
| FND-05 | Any response | Inspecting headers | The CSP header is present and enforcing | testbench | `CspIT` |
| FND-06 | A planted string literal in a view | Running the gate | `NoHardcodedStringsTest` fails naming the file | unit | `NoHardcodedStringsTest` |

# Feature 01: Foundation and shell

## Overview

The two parent layouts, the theme, security, CSP, the dev loop, and everything every other epic stands on. Covers V10 partially, H3, H4 partially, T1.

## Behaviour

### The customer shell

- One `@RouteParent` layout for the public routes: an app bar with Breadcrumbs reading Vaadin, My account, Security status, then the leaf the route names, as the prototype's trail does, D-162; the measured chip bound to `DataRefreshSignal` with the prototype's hover sentence, D-163; and under the bar one centered column of 912 pixels that opens every page with the H1 Security status, the purpose paragraph and the lookup component, mounted once (epic 03 fills its behaviour), above the route's content, D-164, D-169, D-170.
- No navigation menu. The customer screens are reached by asking, not by browsing, which is the source design's shape.
- The document title follows the heading, one writer, because a reader keeps several answers open in tabs.

### The internal shell

- A second parent for `/findings/**` and `/diagnostics`: the same app bar, with the drawer toggle, the page's H1 and the four internal destinations as links in it; a left panel holding only what the route gives it, the explorer's filter and products, as the prototype's does; and the assistant panel mount point in a panel on the right, opened from the bar, D-203.
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

## Still open

Written from the code at the end of epic 13. Each line names where the code shows it and the decision or draft behind it.

- The customer shell is an automatic `@Layout`, `base/ui/CustomerLayout`, not a `@RouteParent` layout; `@RouteParent(HomeView.class)` only gives the breadcrumb hierarchy of the status views.
- The lookup is mounted in the customer column under the H1 and the purpose, not in the bar, through the `ShellLookup` bean, `CustomerLayout`, D-164, which superseded D-38. A customer view's own title is the H2 of its answer, `ViewHeading.ofAnswer()`, D-169.
- `VaadinExpiredSessionStrategy` is installed but idle because there is no session limit, `SecurityConfig`, D-06; the one notice and the reload come from `CustomizedSystemMessages` in `base/security/SessionExpiredNotice`. US-1.2 keeps its tick on that basis, D-161.
- The enforced policy is our own, with `'unsafe-inline'` and `'unsafe-eval'` in `script-src`, `base/security/CspPolicy`, and `vaadin.csp=warn`, not the 25.4 strict mode; `CspMode` is not used, D-02, draft 08.
- Workaround to remove when draft 10 is answered: the policy is set again on the bootstrap page by `base/security/CspOnBootstrapPage`.
- Workaround to remove when flow#26391 is fixed, draft 07b: the remembered theme is applied before first paint by an inline head script, `ThemeMemory`, added in `Application.configurePage`.
- Workaround to remove when flow#26390 is fixed: the system preference is read with a declared `matchMedia` expression, `ThemeSwitch.SystemPreferenceJs`, which since epic 13 returns the typed `JavaScriptResult<Boolean>`, D-157.
- Workaround to remove when draft 01 is answered: Breadcrumbs run in `MANUAL` mode, with the hierarchy walk written by hand in `AppBar.showTrail`, D-08.
- Workaround to remove when flow#25576 is answered, draft 03b: `/styles/**` is permitted by hand in `SecurityConfig`, D-05, and `anyRequest(permitAll)` gives up deny by default so that an unknown address reaches the not found view, D-01.

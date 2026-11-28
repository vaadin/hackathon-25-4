# Feature 01: Foundation and shell

## Overview

The two parent layouts, the theme, security, CSP, the dev loop, and everything every other epic stands on. Covers V10 partially, H3, H4 partially, T1.

## Behaviour

### The customer shell

- One automatic layout for the customer routes, `CustomerLayout`; `@RouteParent(HomeView)` on the answers only gives the trail its hierarchy. It draws an app bar with Breadcrumbs reading Vaadin, My account, Security status, then the leaf the route names, as the prototype's trail does, D-162; the measured chip bound to `DataRefreshSignal` with the prototype's hover sentence, D-163; and under the bar one centered column of 912 pixels that opens every page with the H1 Security status, the purpose paragraph and the lookup component, mounted once (epic 03 fills its behaviour), above the route's content, D-164, D-169, D-170.
- The customer app bar holds, in this order: the app icon, the shield with the Vaadin mark from `icons/favicon.svg`, decorative and also the page's favicon; the breadcrumb trail; the measured chip; the color scheme button; the "Team pages" shield, for a team member when the team part is present, D-246, D-248; and Sign out as an icon button with a tooltip and accessible name, for any signed in session, D-12, D-248. The measured chip reads "Measured <yyyy-mm-dd>" in UTC, with the hover sentence "These are the real measurements of that date, not sample data", and is not drawn until a measurement has been read. The internal bar draws no app icon: the icon heads its left panel.
- No navigation menu for a customer. The customer screens are reached by asking, not by browsing, which is the source design's shape.
- Signed in with the team role, and only when the team part of the application is present, D-254, the customer shell draws the team's side navigation beside the customer column. Below 800 pixels that navigation is hidden and the Team pages shield in the bar leads back; above 800 pixels the shield is hidden, D-253. A reader without the role sees neither.
- The document title follows the heading, one writer, because a reader keeps several answers open in tabs.

### The internal shell

- A second shell for `/findings/**` and `/diagnostics`, `InternalLayout`: the same app bar, with the drawer toggle and the page's H1. Its left panel starts with the app icon and the name "CVE Status", then a SideNav of the team's destinations in this order: Findings, with Spring Boot under it, Customer view, Verdicts, Advisories, Diagnostics, D-237, D-245, D-252. What the route gives, the explorer's and the release page's filter and products, sits under the navigation as a card of its own, D-251. The assistant panel is on the right, opened from the bar, D-203, D-255.
- Signed out users hitting any page land on `/login` and return to where they aimed after signing in. The sign in page stands alone, outside the customer shell: no app bar, no breadcrumbs, no heading, purpose or lookup, only the card in the middle of the window, D-276, in the prototype's look, D-277. The card reads top to bottom: a brand row with the app icon, "Security status" and the color scheme button as a small ring; the H1 "Sign in" and the lead "Use your work account to continue."; Vaadin's `LoginForm` with its own title hidden, the labels Username and Password, the button Continue and no forgot password link; and the footer "Trouble signing in? Contact your account manager.". A failed sign in returns to `/login?error`, where the form shows "Incorrect username or password". The card is white with a soft shadow on a grey ground, dark navy in dark mode.

### Theme

- Aura. Dark and light per `05-theming.md`: one icon button in the app bar, with no menu, D-247. Each click steps System, Light, Dark and round again; its icon is a half circle, a sun or a moon, and its tooltip and accessible name read "Color scheme: <current>. Click for <next>.". System is the default. Light and Dark are remembered in local storage through the typed `WebStorage` accessors and applied before first paint; coming round to System forgets the choice.

### Security and CSP

- Every page needs a signed in user, D-258. Anyone signed in reads the customer pages; a signed in user whose verified email is on vaadin.com is the team. The Spring profile decides how people sign in: under `demo`, the default, the login view keeps the accounts `team` and `guest`; under `cas`, people sign in through vaadin.com's single sign on and `/login` sends the reader home, where the single sign on takes over. A production build that names neither profile refuses to start. Five failed sign ins for one user name within 15 minutes lock that name until the window passes, D-259. The route rules are those of `04-security.md`.
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
| A signed in reader opens a route the role does not allow, `guest` on `/findings` | The home page, as if the route did not exist, D-04; Sign out stays in the bar |
| A visitor who has not signed in opens any page | Sent to `/login`, and back to that page after signing in, D-258 |
| Signing out | Back to the application's root, which asks for sign in again |
| Five wrong passwords for one user name within 15 minutes | That name is locked until the window passes, D-259 |
| The session expires mid reading | The same view reloads after one notice, "Session expired. Click here or press Escape to reload this page."; the lookup state is gone |
| A CSP violation in development | Logged, visible later in diagnostics, never swallowed |
| Storage unavailable for the theme memory | System preference wins, silently, rule 6.7.8 of the source spec |

## Acceptance criteria

### AC1: Two shells
- [x] Customer routes render under the customer shell with breadcrumbs and the measured chip, and send a visitor who has not signed in to `/login`, D-258
- [x] Internal routes refuse `guest` and anonymous, and return the signed in `team` user to the route they asked for

### AC2: Theme
- [x] Dark system preference paints dark with no light flash
- [x] Light or Dark overrides the system and survives a reload; System forgets the choice

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
| FND-02 | Signed in as `guest` | Opening `/findings` | Lands on the home page, with no team navigation and Sign out still in the bar | browserless | `ShellSecurityBrowserlessTest` |
| FND-03 | Any route | Rendering | Breadcrumbs name the leaf and the document title matches | browserless | `ShellBrowserlessTest` |
| FND-04 | A dark system preference | First paint | Dark, no flash | testbench | `ThemeFirstPaintIT` |
| FND-05 | Any response | Inspecting headers | The CSP header is present and enforcing | testbench | `CspIT` |
| FND-06 | A planted string literal in a view | Running the gate | `NoHardcodedStringsTest` fails naming the file | unit | `NoHardcodedStringsTest` |
| FND-07 | Signed in as `guest` | Opening `/login`, then `/` | The sign in page has no shell H1 and no lookup; home has both | browserless | `ShellSecurityBrowserlessTest` |
| FND-08 | Anonymous | Opening `/` | Sent to `/login` | browserless | `ShellSecurityBrowserlessTest` |

## Still open

Written from the code at the close of the prototype alignment. Each line names where the code shows it and the decision or draft behind it.

- The customer shell is an automatic `@Layout`, `ui/customer/CustomerLayout`, not a `@RouteParent` layout; `@RouteParent(HomeView)` only gives the breadcrumb hierarchy of the answers.
- The lookup is mounted in the customer column under the H1 and the purpose, not in the bar, through the `LookupFactory` bean that `ui/customer/LookupProvider` gives, D-164, which superseded D-38. A customer answer's own title is an H2 kept for screen readers and visually hidden, D-169, D-270.
- The internal shell, `ui/team/InternalLayout`, draws the team's SideNav at the top of its left panel, in the order of D-245 and D-252, not from `@Menu`. What a route gives through `ui/team/DrawerContent`, the explorer's and the release page's filter and products, sits under it as a card, D-251. The page's H1 is drawn by the shell through `ViewHeading`; the assistant is an `Aside` on the right, shut on arrival, opened by the Assistant button before Sign out.
- Below about 1500 pixels the internal bar wraps, the navbar part wrapping in `styles/team/shell.css`, because it carries the H1, the trail, the chip, the scheme button, Assistant and Sign out. The destinations moved to the left panel, D-237. Not compared with the prototype, whose bar holds less.
- `VaadinExpiredSessionStrategy` is installed but idle because there is no session limit, `SecurityConfig`, D-06; the one notice and the reload come from the customized system messages of `security/SessionExpiredNotice`. US-1.2 keeps its tick on that basis, D-161.
- The enforced policy is our own, with `'unsafe-inline'` and `'unsafe-eval'` in `script-src`, `security/CspPolicy`, and `vaadin.csp=warn`, not the 25.4 strict mode; `CspMode` is not used, D-02, draft 08.
- Workaround to remove when draft 10 is answered: the policy is set again on the bootstrap page by `security/CspOnBootstrapPage`.
- Workaround to remove when flow#26391 is fixed, draft 07b: the remembered theme is applied before first paint by an inline head script, `ThemeMemory`, added in `Application.configurePage`.
- Removed on 2026-11-23: the declared `matchMedia` expression that read the system preference, D-157. The System choice follows the system through `color-scheme: light dark` and never needs to know what it prefers.
- Workaround to remove when draft 01 is answered: Breadcrumbs run in `MANUAL` mode, with the hierarchy walk written by hand in `AppBar`, D-08.
- Workaround to remove when flow#25576 is answered, draft 03b: `/styles/**` is permitted by hand in `SecurityConfig`, D-05, and, since D-235, deny by default holds again: only a GET that no secured route and no controller answers is let through, so an unknown address still reaches the not found view, `SecurityConfig`.
- FND-01, FND-02, FND-07 and FND-08 prove customer facts, the sign in page alone and the redirect for a visitor who has not signed in among them, in `ui/team/ShellSecurityBrowserlessTest`, a team package that goes when the team is removed, D-254. The tests stay there because they also walk the team's routes.

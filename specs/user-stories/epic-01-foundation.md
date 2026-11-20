# Epic 01: Foundation and shell

**Goal:** two shells, one theme, real security, and the guard rails every later epic is born under.
**Feature spec:** `specs/features/01-foundation.md`
**Dependencies:** none.

---

### US-1.1: The customer shell

**Tasks:**
- [x] `@RouteParent` layout with Breadcrumbs, the measured chip, the lookup mount
- [x] Document title follows the heading, one writer
- [x] Delete the walking skeleton's example feature

**25.x APIs:** Breadcrumbs, `@RouteParent`.
**Verified by:** `ShellBrowserlessTest`

---

### US-1.2: The internal shell and security

**As a** team member **I want** the internal screens behind a real sign in **so that** the deterrent gates of the static site become a boundary.

**Tasks:**
- [x] Internal parent with the drawer and assistant mount
- [x] `VaadinSecurityConfigurer`, login view, the two users, route rules
- [x] Return to the asked route after sign in
- [x] `VaadinExpiredSessionStrategy` with the one notice

**25.4 APIs:** login success and failure handling, expired session strategy.
**Verified by:** `ShellSecurityBrowserlessTest`

---

### US-1.3: Theme and dark mode

**Tasks:**
- [x] Aura tokens, the status and severity token pairs of `05-theming.md`
- [x] Switch in the app bar, system default, remembered through typed web storage, decided before first paint

**25.x APIs:** Switch, `WebStorage` typed accessors.
**Verified by:** `ThemeFirstPaintIT`

---

### US-1.4: Content Security Policy

**Tasks:**
- [x] `ContentSecurityPolicy` enforcing, violations logged
- [x] Every loosened directive recorded in `specs/FEEDBACK-25.4.md` as it happens

**25.4 APIs:** `ContentSecurityPolicy`, `CspMode`.
**Verified by:** `CspIT`

---

### US-1.5: Guard rails and the dev loop

**Tasks:**
- [x] `NoHardcodedStringsTest`, `SpecConsistencyTest`
- [x] Dev loop installed, costs measured into `specs/10-dev-loop.md`

**Verified by:** the two tests themselves

---

## Definition of Done

- [x] Every acceptance criterion in `features/01-foundation.md` is checked
- [x] The gate is green with the CSP on

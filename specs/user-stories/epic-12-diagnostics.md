# Epic 12: Diagnostics

**Goal:** the 25.4 lifecycle events counted on real traffic and readable live.
**Feature spec:** `specs/features/12-diagnostics.md`
**Dependencies:** epic 01, and epic 11 for the assistant tail.

---

### US-12.1: The listeners

**Tasks:**
- [ ] Service init listener registering navigation, request, session, CSP and interception listeners
- [x] The signal singleton with counters and ring tails

**25.4 APIs:** `NavigationStartedEvent`, `NavigationEndedEvent` outcomes, `RequestStartedEvent`, `RequestEndedEvent`.
**Verified by:** `DiagnosticsBrowserlessTest`

---

### US-12.2: The view

**Tasks:**
- [x] The four live panels plus the reader's extended client details

**25.4 APIs:** extended client details typed accessors.
**Verified by:** `DiagnosticsBrowserlessTest`

---

## Definition of Done

- [x] Every acceptance criterion in `features/12-diagnostics.md` is checked

# Epic 07: Advisories and cases

**Goal:** the whole catalogue, filterable and actionable, and the demo case index.
**Feature spec:** `specs/features/07-advisories-and-cases.md`
**Dependencies:** epic 02.

---

### US-7.1: The advisories grid

**Tasks:**
- [ ] Grid in the source order, severity badges, owner and verdict chips
- [x] Filter pills with counts writing `?verdict`

**Verified by:** `AdvisoriesBrowserlessTest`

---

### US-7.2: The context menu

**Tasks:**
- [x] `GridContextMenu` with dynamic content: copy id, open record, ask on my version

**25.4 APIs:** `GridContextMenu` dynamic content.
**Verified by:** `AdvisoriesBrowserlessTest`

---

### US-7.3: Cases

**Tasks:**
- [x] `/cases` unlisted, the three groups from the fixture, See one links

**Verified by:** `CasesBrowserlessTest`

---

## Definition of Done

- [ ] Every acceptance criterion in `features/07-advisories-and-cases.md` is checked

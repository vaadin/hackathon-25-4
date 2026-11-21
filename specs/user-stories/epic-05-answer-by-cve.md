# Epic 05: Answer by CVE

**Goal:** paste an id, get the exact release to move to, and copy the answer for the ticket.
**Feature spec:** `specs/features/05-answer-by-cve.md`
**Dependencies:** epics 02 and 03.

---

### US-5.1: The verdict cards

**Tasks:**
- [x] One card per id, the settled state set, multiple ids in asked order
- [x] The fixed in headline naming the reader's line release
- [x] The unknown id answered plainly

**Verified by:** `CveViewBrowserlessTest`

---

### US-5.2: Copy answer for your ticket

**As a** support engineer **I want** the answer on my clipboard in one click **so that** the ticket quotes the measurement, not my paraphrase.

**Tasks:**
- [x] Compose the plain text answer from the card's facts only
- [ ] Clipboard API write, success read back through the typed JS call, one notice

**25.x APIs:** Clipboard, `JsExpression` return values.
**Verified by:** `CopyAnswerIT`

---

### US-5.3: Across versions and the layers

**Tasks:**
- [x] Per line cards from coverage, yours marked and named
- [x] The three layers, first open, Source facts, absence omitted
- [x] Per package verdicts with coordinates, difference marked

**Verified by:** `CveViewBrowserlessTest`

---

## Definition of Done

- [x] Every acceptance criterion in `features/05-answer-by-cve.md` is checked
- [x] The copied answer never contains a fact the card does not show

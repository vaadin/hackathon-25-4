# Epic 02: The data client

**Goal:** one door to the network, a cache that keeps answering, and a refresh every session sees.
**Feature spec:** `specs/features/02-data-client.md`
**Dependencies:** epic 01.

---

### US-2.1: Client and cache

**Tasks:**
- [x] `PagesClient` with `RestClient`, ETag revalidation
- [x] TTLs, single flight, stale over error
- [x] The architecture test that keeps HTTP inside `data`

**Verified by:** `PagesCacheTest`

---

### US-2.2: Read models

**Tasks:**
- [x] Records per `01-domain-model.md`, unknown fields ignored, absent is absent
- [x] `VersionNumber` with numeric ordering

**Verified by:** `ReadModelTest`, `VersionNumberTest`

---

### US-2.3: The refresh signal

**As a** reader with a tab open overnight **I want** the measured chip to move when the nightly publish lands **so that** a stale answer is never dressed as today's.

**Tasks:**
- [x] `DataRefreshSignal` shared, scheduled revalidation, invalidation of the dependents
- [x] The measured chip and home cards bound to it

**25.3 APIs:** shared signals.
**Verified by:** `RefreshBroadcastBrowserlessTest`

---

### US-2.4: The exports passthrough

**Tasks:**
- [x] Streamed proxy of the per release files through the `Download` API, content types kept

**25.4 APIs:** `Download`.
**Verified by:** `ExportsProxyIT`

---

### US-2.5: Fixtures

**Tasks:**
- [x] `scripts/refresh-fixtures.sh` and the committed fixture set
- [x] The test configuration that points the client at the classpath

**Verified by:** every browserless test runs on them

---

## Definition of Done

- [x] Every acceptance criterion in `features/02-data-client.md` is checked
- [x] No view can reach the network, by test

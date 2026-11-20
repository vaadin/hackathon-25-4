# Epic 06: Scanner report triage

**Goal:** a scanner report in, a triaged answer out, and nothing kept.
**Feature spec:** `specs/features/06-scan-triage.md`
**Dependencies:** epics 02 and 03.

---

### US-6.1: Upload and parse

**Tasks:**
- [ ] Drop zone and button on the report tab, one file, replace swaps
- [ ] In memory `UploadHandler`, parsed in the request, discarded
- [ ] The four format parsers, detection by shape
- [ ] Version read from the file's coordinates, the conditional version field

**25.x APIs:** modular upload, upload handlers.
**Verified by:** `ReportParserTest`, `ScanUploadIT`

---

### US-6.2: Triage

**Tasks:**
- [ ] The three buckets with owner and action per `01-domain-model.md`
- [ ] Skipped entries counted
- [ ] The headline and bucket cards, the capped table, the secure Switch

**Verified by:** `TriageTest`, `ScanViewBrowserlessTest`

---

### US-6.3: Take it away

**As an** auditor **I want** the triage as a file **so that** the audit record carries the measurement.

**Tasks:**
- [ ] The CSV through the `Download` API, named for the version
- [ ] The PDF composed server side, previewed in `PdfViewer`, then downloaded

**25.4 APIs:** `Download`, `PdfViewer`.
**Verified by:** `TriageDownloadIT`

---

### US-6.4: The help that counts

**Tasks:**
- [ ] The format list held to the parser by test
- [ ] What is assessed counted against the file in hand
- [ ] The parsed and discarded sentence

**Verified by:** `ScanViewBrowserlessTest`

---

## Definition of Done

- [ ] Every acceptance criterion in `features/06-scan-triage.md` is checked
- [ ] No upload leaves a file behind, by the handler's contract test

# Epic 10: Verdict workbench

**Goal:** the queue ranked by what one verdict closes, and a form whose output is the YAML the pipeline takes.
**Feature spec:** `specs/features/10-workbench.md`
**Dependencies:** epics 02 and 08. The AI fill of this screen is epic 11's US-11.5, so this epic is complete without any provider plumbing.

---

### US-10.1: The queue

**Tasks:**
- [x] Grid over the queue fixture order, series filter in the address
- [x] The summary, Charts or figures, decided and recorded
- [x] Row selection prefills the form

**Verified by:** `WorkbenchBrowserlessTest`

---

### US-10.2: The draft form

**As a** team member **I want** a form that refuses an empty verdict **so that** what reaches GitHub answers something.

**Tasks:**
- [x] `VerdictDraft` entity, H2, binder with Draft and Ready groups
- [x] Side labels through `LabelTextAlign`
- [x] The closed vocabulary only, drafts listed and restored

**25.4 APIs:** FormLayout side labels.
**Verified by:** `WorkbenchBrowserlessTest`

---

### US-10.3: The YAML and the handoff

**Tasks:**
- [x] Live YAML in a Markdown code block, round trip tested, absent keys absent
- [x] The closing sentence from the queue numbers
- [x] `NativeForm` GET into GitHub's new file editor, the copy control, the URL length refusal
- [x] The platform PRs warning sentence

**25.x APIs:** `NativeForm`, Markdown.
**Verified by:** `VerdictYamlTest`, `WorkbenchBrowserlessTest`

---

## Definition of Done

- [x] Every acceptance criterion in `features/10-workbench.md` is checked
- [x] The YAML round trips on every fixture draft

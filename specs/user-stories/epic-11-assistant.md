# Epic 11: Assistant

**Goal:** answers grounded in the measurements, refusals that teach, and a camera that asks questions rather than answering them.
**Feature spec:** `specs/features/11-assistant.md`
**Dependencies:** epics 02, 08 and 10, whose workbench form US-11.6 fills.

---

### US-11.1: Providers and prompts

**Tasks:**
- [x] The mock provider with cassettes, the default everywhere
- [x] `SpringAILLMProvider` behind the `ai` profile
- [x] Prompt files under test as text
- [x] Request interception into the diagnostics tail

**25.3 APIs:** LLM providers, request interception.
**Verified by:** `AssistantBrowserlessTest`

---

### US-11.2: The panel and the tools

**As a** team member **I want** the assistant to quote the measurement or say it cannot **so that** its answers are citable in a ticket.

**Tasks:**
- [x] `AIOrchestrator`, `AIMessageList`, `AIInput` in the internal shell, history kept: since D-203 in a panel on the right opened by the Assistant button, no longer in the drawer
- [ ] The five tools, `ToolException` refusals, `QueryPage` paging
- [x] The limit sentence for `ToolCallLimitExceededException`

**25.x APIs:** orchestrator, tool calling, `QueryPage`, `ToolCallLimitExceededException`.
**Verified by:** `AssistantBrowserlessTest`

---

### US-11.3: Images in

**Tasks:**
- [x] Attachment with `UploadCapture`, extraction cassette, the tools answer

**25.4 APIs:** `UploadCapture`.
**Verified by:** `AssistantBrowserlessTest`

---

### US-11.4: Grid AI on advisories

**Tasks:**
- [x] The controller with query validation, the refusal in words

**25.3 APIs:** Grid AI controller.
**Verified by:** `GridAiBrowserlessTest`

---

### US-11.5: Fill the workbench form

**Tasks:**
- [x] The paste box on the workbench, Form AI controller, markers, nothing auto saved

**25.3 APIs:** Form AI controller.
**Verified by:** `WorkbenchAiBrowserlessTest`

---

### US-11.6: Live proof

**Tasks:**
- [x] The one `live-ai` tagged test and the browser exchange

**Verified by:** `LiveAssistantTest`, `AssistantPanelIT`

---

## Definition of Done

- [x] Every acceptance criterion in `features/11-assistant.md` is checked
- [x] The gate never calls a model

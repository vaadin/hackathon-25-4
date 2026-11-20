# Epic 11: Assistant

**Goal:** answers grounded in the measurements, refusals that teach, and a camera that asks questions rather than answering them.
**Feature spec:** `specs/features/11-assistant.md`
**Dependencies:** epics 02, 08 and 10, whose workbench form US-11.6 fills.

---

### US-11.1: Providers and prompts

**Tasks:**
- [ ] The mock provider with cassettes, the default everywhere
- [ ] `SpringAILLMProvider` behind the `ai` profile
- [ ] Prompt files under test as text
- [ ] Request interception into the diagnostics tail

**25.3 APIs:** LLM providers, request interception.
**Verified by:** `AssistantBrowserlessTest`

---

### US-11.2: The panel and the tools

**As a** team member **I want** the assistant to quote the measurement or say it cannot **so that** its answers are citable in a ticket.

**Tasks:**
- [ ] `AIOrchestrator`, `AIMessageList`, `AIInput` in the internal drawer, history kept
- [ ] The five tools, `ToolException` refusals, `QueryPage` paging
- [ ] The limit sentence for `ToolCallLimitExceededException`

**25.x APIs:** orchestrator, tool calling, `QueryPage`, `ToolCallLimitExceededException`.
**Verified by:** `AssistantBrowserlessTest`

---

### US-11.3: Images in

**Tasks:**
- [ ] Attachment with `UploadCapture`, extraction cassette, the tools answer

**25.4 APIs:** `UploadCapture`.
**Verified by:** `AssistantBrowserlessTest`

---

### US-11.4: Grid AI on advisories

**Tasks:**
- [ ] The controller with query validation, the refusal in words

**25.3 APIs:** Grid AI controller.
**Verified by:** `GridAiBrowserlessTest`

---

### US-11.5: Fill the workbench form

**Tasks:**
- [ ] The paste box on the workbench, Form AI controller, markers, nothing auto saved

**25.3 APIs:** Form AI controller.
**Verified by:** `WorkbenchAiBrowserlessTest`

---

### US-11.6: Live proof

**Tasks:**
- [ ] The one `live-ai` tagged test and the browser exchange

**Verified by:** `LiveAssistantTest`, `AssistantPanelIT`

---

## Definition of Done

- [ ] Every acceptance criterion in `features/11-assistant.md` is checked
- [ ] The gate never calls a model

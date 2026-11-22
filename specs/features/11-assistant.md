# Feature 11: Assistant

## Overview

The chat panel on the internal screens, its tools over the measured data, the image intake, the Grid AI attachment, and the Form AI fill of the workbench. The contract for providers, prompts and what a test can prove is `specs/06-ai.md`; this document is the screens. Covers A1 to A4 and the fill half of W2.

## Behaviour

### The panel

- `AIMessageList` and `AIInput` in the internal drawer, present on every internal screen, backed by one `AIOrchestrator` per session with history per `06-ai.md`.
- The five tools of `06-ai.md`, each reading `SecurityDataService`, refusing through `ToolException`, paging through `QueryPage`.
- A tool refusal renders as the assistant's own correction, the model asks again or says what it could not find; the limit exception renders as the gave up sentence. Neither is ever a stack trace.
- Every exchange is logged through request interception into the diagnostics tail.

### Images

- The input takes an attachment, a photographed or screenshotted scanner line, with `UploadCapture` offering the camera where there is one. The model reads the id and coordinate out of the image and then must answer through the tools. In the mock provider the cassette fixes the extraction, so the flow tests deterministically.

### Grid AI

- The advisories grid takes the controller per `06-ai.md`, with query validation on: show the critical ones still open on 24.9 narrows the grid, and an ask outside the grid's vocabulary is refused in words. `SECURITY` sessions only, per `features/07-advisories-and-cases.md`.

### Form AI on the workbench

- The paste box of `features/10-workbench.md`, built here because it needs the providers: prose in, the draft fields filled with markers, nothing saved until a person says so.

## Edge cases

| Scenario | Behaviour |
| --- | --- |
| A question about a version never measured | The tool refuses naming the nearest measured, the assistant relays that |
| A question whose answer the tools cannot ground | The measurements do not say, verbatim contract from `06-ai.md` |
| The model calling tools past the limit | The gave up sentence with the count |
| An image with no readable id | The assistant says what it could not read and asks for the id |
| No key and no profile | The mock provider answers, the demo banner says it is the mock |

## Acceptance criteria

### AC1: Grounded answers only
- [x] The version question produces a `versionStatus` call and quotes its result
- [x] The ungroundable question produces the contract sentence

### AC2: Refusals teach
- [x] The unmeasured version answer names the nearest measured
- [x] The limit renders as the sentence

### AC3: The image is a question, not an answer
- [x] The cassette image flow ends in a `cveStatus` call

### AC4: Grid AI narrows, never fabricates
- [x] The slice ask narrows the grid to matching rows
- [x] The out of vocabulary ask is refused in words

### AC5: The fill proposes, a person disposes
- [x] The paste fills only supported fields, with markers, and saves nothing

## Test cases

| Id | Given | When | Then | Tier | Verified by |
| --- | --- | --- | --- | --- | --- |
| AST-01 | The mock provider | Is 24.9.21 safe | One versionStatus call, the answer quotes state and date | browserless | `AssistantBrowserlessTest` |
| AST-02 | An unmeasured version asked | The exchange | The nearest measured named | browserless | `AssistantBrowserlessTest` |
| AST-03 | The ungroundable cassette | The exchange | The contract sentence | browserless | `AssistantBrowserlessTest` |
| AST-04 | The image cassette | Attaching the fixture image | Extraction then a cveStatus call | browserless | `AssistantBrowserlessTest` |
| AST-05 | The advisories grid | The slice ask | Rows narrow to the fixture's matching set | browserless | `GridAiBrowserlessTest` |
| AST-06 | A real browser, mock provider | One exchange in the panel | Message in, answer rendered | testbench | `AssistantPanelIT` |
| AST-07 | `-Pai` with a key | The canonical question | An answer grounded in a tool call arrives | unit, tagged live-ai, excluded from the gate per `08-testing.md` | `LiveAssistantTest` |
| AST-08 | A pasted GHSA paragraph, mock provider | AI fill on the workbench | The supported fields fill with markers, nothing saved | browserless | `WorkbenchAiBrowserlessTest` |

## Still open

Written from the code at the end of epic 13. Each line names where the code shows it and the decision or draft behind it.

- `QueryPage` pages no tool result: 25.4 defines it only for a lazily loading grid through a `DatabaseProvider`. `openFindings` pages through its own `offset` and `limit` arguments, at most 25 lines, and the other tools are unpaged, `AssistantTools.openFindings`, D-143.
- Request interception records only the prompt: tool calls and refusals reach the tail from the tools' own wrapper, and answers from the response listener, `AssistantPanel.intercept`, `AssistantTools.tools`, `AssistantPanel.ended`, D-146.
- Grid AI narrows a `Grid<AIDataRow>` nobody sees, over an in memory copy of the catalogue, and its ids then narrow the real grid. It is not the same grid with the same data provider. Workaround to remove when draft 21 is answered: `AdvisoryAsk`, `AdvisoryCatalogue`, D-140.
- "Still open on 24.9" is read as a verdict of `risk` or `in_triage` with no fix on 24.9 in `fixed_in`, because the index has no per line open state, `prompts/catalogue-schema.md`, D-142.
- Workaround to remove when draft 22 is answered: the turn's tool calls are counted by our own controller, and the orchestrator's fixed English failure text is overwritten with a bundle sentence, `AssistantTools.callsThisTurn`, `AssistantPanel.ended`, D-144.
- Workaround to remove when draft 17 is answered: `@Uses` on `InternalLayout` and `AdvisoriesView` keeps the AI web components in the production bundle, D-149.
- `QueryPage` keeps the second task of US-11.2 unticked, D-143.

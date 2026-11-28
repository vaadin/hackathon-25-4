# Feature 11: Assistant

## Overview

The chat panel on the internal screens, its tools over the measured data, the image intake, the Grid AI attachment, and the Form AI fill of the workbench. The contract for providers, prompts and what a test can prove is `specs/06-ai.md`; this document is the screens. Covers A1 to A4 and the fill half of W2.

## Behaviour

### The panel

- The assistant is a message list and a message input driven by one `AIOrchestrator` per session, with history per `06-ai.md`, in a panel on the right of the internal shell on every internal screen. It is shut on arrival and opened and shut by the chat bubble icon button in the bar, labelled "Assistant", D-203, D-248; a close icon labelled "Close the assistant" sits at its top. Above 800 pixels the panel is a 22rem column beside the page, with a background, as tall as the scrolling content and sticking there. Below 800 pixels it covers the whole page, D-255. A production build with no model has no Assistant button at all; recorded answers are offered only in development, in the tests and with `securitystatus.assistant.recorded=true`, D-236. Where recorded answers are used, the panel says "Recorded answers: no model is configured here, ...".
- The five tools of `06-ai.md`, each reading `SecurityDataService` and refusing through `ToolException`. `openFindings` pages through its own offset and limit, at most 25 lines, D-143.
- A tool refusal renders as the assistant's own correction, the model asks again or says what it could not find; the limit exception renders as the gave up sentence. Neither is ever a stack trace.
- Every exchange reaches the diagnostics tail: the prompt through request interception, tool calls and refusals from the tools, answers and failures when the turn ends, D-146.
- A session asks at most 30 prompts in 10 minutes across the chat, the advisory ask and the verdict fill; past that the prompt is refused with "That is enough questions for a few minutes. Ask again in a little while." The conversation keeps its last 40 messages and survives a new shell, D-260.

### Images

- The composer is one rounded bar, as phone chat apps draw it: a camera icon at the start, which takes one image of at most 5 MB, a photographed or screenshotted scanner line, and offers the back camera where there is one through `UploadCapture`; a text area at least three lines tall that grows; and the send arrow at the end, D-256. The model reads the id and coordinate out of the image and then must answer through the tools. In the mock provider the cassette fixes the extraction, so the flow tests deterministically.

### Grid AI

- The advisories grid takes the controller per `06-ai.md`, with query validation on: show the critical ones still open on 24.9 narrows the grid, and an ask outside the grid's vocabulary is refused in words. `SECURITY` sessions only, per `features/07-advisories-and-cases.md`.

### Form AI on the workbench

- The paste box of `features/10-workbench.md`, built here because it needs the providers: prose in, the draft fields filled and marked "Filled from the pasted text" with a confidence word, and "Put back what was there" on each. The model's instructions tell it never to write the verdict id, who assessed it or supersedes. Nothing is saved until a person says so.

## Edge cases

| Scenario | Behaviour |
| --- | --- |
| A question about a version never measured | The tool refuses naming the nearest measured, the assistant relays that |
| A question whose answer the tools cannot ground | The measurements do not say, verbatim contract from `06-ai.md` |
| The model calling tools past the limit | The gave up sentence with the count |
| An image with no readable id | The assistant says what it could not read and asks for the id |
| No model, development or tests | The recorded answers reply, and the panel says it is recorded answers |
| No model, production build | No Assistant button, no ask on the advisories list, no fill on the workbench, D-236 |

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
| AST-07 | `-Pai` with a key | The canonical question, tagged live-ai and excluded from the gate per `08-testing.md` | An answer grounded in a tool call arrives | unit | `LiveAssistantTest` |
| AST-08 | A pasted GHSA paragraph, mock provider | AI fill on the workbench | The supported fields fill with markers, nothing saved | browserless | `WorkbenchAiBrowserlessTest` |
| AST-09 | One session | 31 prompts inside 10 minutes, then later | The 31st refused, admitted again after the window; 40 messages kept | unit | `AssistantHistoryTest` |

## Still open

Written from the code at the close of the prototype alignment. Each line names where the code shows it and the decision or draft behind it.

- The panel is an `Aside` on the right of the internal content since D-203, shut on arrival and opened by the Assistant button in the bar, a signal in `InternalLayout`. A component hidden with `setVisible(false)` sends no children to the browser, so the message input exists in the page only once the panel has been opened; the tests open it through the `assistant-toggle` button first. The prototype has no assistant: the panel is an app extra, kept by the person's choice.
- The workbench's paste box sits in the form's column, which since D-220 moves into the package row opened and back to the fold at the foot of the page when the row shuts; `WorkbenchAiBrowserlessTest` fills the form wherever it is.
- `QueryPage` pages no tool result: 25.4 defines it only for a lazily loading grid through a `DatabaseProvider`. `openFindings` pages through its own `offset` and `limit` arguments, at most 25 lines, and the other tools are unpaged, `AssistantTools.openFindings`, D-143.
- Request interception records only the prompt: tool calls and refusals reach the tail from the tools' own wrapper, and answers from the response listener, `AssistantPanel.intercept`, `AssistantTools.tools`, `AssistantPanel.ended`, D-146.
- Grid AI narrows a `Grid<AIDataRow>` nobody sees, over an in memory copy of the catalogue, and its ids then narrow the real grid. It is not the same grid with the same data provider. Workaround to remove when draft 21 is answered: `AdvisoryAsk`, `AdvisoryCatalogue`, D-140.
- "Still open on 24.9" is read as a verdict of `risk` or `in_triage` with no fix on 24.9 in `fixed_in`, because the index has no per line open state, `prompts/catalogue-schema.md`, D-142.
- Workaround to remove when draft 22 is answered: the turn's tool calls are counted by our own controller, and the orchestrator's fixed English failure text is overwritten with a bundle sentence, `AssistantTools.callsThisTurn`, `AssistantPanel.ended`, D-144.
- Workaround to remove when draft 17 is answered: `@Uses` on `InternalLayout` and `AdvisoriesView` keeps the AI web components in the production bundle, D-149.
- `QueryPage` keeps the second task of US-11.2 unticked, D-143.

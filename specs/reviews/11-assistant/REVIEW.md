# Review: the assistant, epic 11

Screens, all at 1200 by 1000 in development mode, signed in as the team, with the cassette provider: `new-chat.png`, the chat in the drawer after "Is 24.9.21 safe?"; `new-chat-picture.png`, the panel after a picture of a scanner line was attached with a question no cassette records; `new-advisories-ask.png`, the advisories list narrowed by "show the critical ones still open on 24.9"; `new-workbench-fill.png`, the workbench after a pasted GHSA paragraph filled the form. The same three asks were then made once against the live model, without screenshots. There is no reference: neither JavaScript page has an assistant, an ask box or a paste box, so the comparison below is with the reference screens the surfaces sit on and with what a person would otherwise do by hand.

## The support engineer

The assistant is internal, so the support engineer meets it only as a member of the team. Read that way:

- The chat lives in the drawer of the internal shell, 338 pixels wide. An answer of four facts wraps over eight lines, and the date `2026-11-15T03:57:22Z` breaks in the middle. On a phone the drawer is closed, so the assistant is behind the menu toggle and shares the drawer with the navigation.
- A canned answer and a live one look the same. The only sign of the recorded mode is the paragraph under the title; once three exchanges have scrolled it away, nothing tells a reader whether the words came from a model or a cassette.
- Without a key the assistant answers only the questions it was recorded with, and any other question gets one sentence listing two examples. For a demo that is honest; for the team it means the default build has no assistant to speak of.
- Against the live model a turn took 20 to 25 seconds with only "Typing…" to look at. The orchestrator streams, but the model answers after its tool calls, so the stream starts late.
- The live answers quoted the tool results and named the source, `cases/v/24.9.21.json`, with its scan date. The first live answer began with "No" and a dash, which is a judgment the tool result does not state in those words.
- The workbench paste box filled the package, the advisories and the impact from the cassette. Live, the model filled the package, the advisories with the GHSA id beside the CVE, and put the description into Commentary and Evidence rather than Impact, then named the fields by their ids, `verdict-purl`. The prompt now asks for labels; that was not run against the model again.
- The advisories ask narrowed the list to 18 rows with the cassette and with the live model alike, and the line above the grid says "18 of 535 advisories match the ask". The ask itself does not say that a model reads it, nor that it is there only because the reader is signed in.

## The pedantic analyst

| Metric | Reference | New | Delta | Verdict |
| --- | --- | --- | --- | --- |
| Facts above the fold for "is 24.9.21 safe" | The version screen: the verdict card | The chat answer: state, scan date, risk count, triage count, upgrade target, what it clears and leaves, source; 7 facts in a 338 pixel column | n/a, another surface | Complete, cramped |
| Clicks from arrival to the verdict | Type the version, Check: 1 click | Type the question, Enter: 0 clicks, 20 to 25 seconds live | minus 1 click, plus the wait | Slower live |
| Clicks to narrow the advisories list to critical, open, one line | Not possible on the reference | 0, one sentence and Enter | n/a | New |
| Data area as a percentage of the viewport | Not measured | The chat's list is capped at 55 per cent of the viewport height inside the drawer; not measured further | n/a | |
| Base font size | 16 pixels, Aura | 16 pixels in the messages, 14 in the mock paragraph | 0 | |
| Smallest click target | Not measured | The Send and Narrow labels inside the message inputs; not measured | n/a | Not verified |

## Issues

### Critical

None found that invents a value. Every cassette sentence is a template filled from the tool result of the replay and dropped when a value is missing; the live answers checked quoted the tool results.

### Major

1. A live turn takes 20 to 25 seconds with no sign of which tool is running.
2. The live form fill chose Commentary and Evidence for the advisory's description and named fields by id; the prompt was tightened, not re-run, P-18.
3. The chat is confined to the drawer: narrow, and hidden on a phone.
4. Nothing on a message says whether it came from a cassette or a model once the banner has scrolled away.

### Minor

1. Message times use the browser's US format, "Nov 22, 2026, 2:24 AM", unlike every other date in the application.
2. A picture attached to a message shows as a thumbnail too small to read.
3. The ask over the advisories list sits between the pills and the total line with no label saying it is the assistant.
4. The live grid answer called the list "the grid"; the prompt now says "the list", not re-run.
5. The paste box's Fill button stays enabled after a fill, so a second press sends the same text again.

## Deliberate improvements

- The assistant refuses a version that was never measured by naming the nearest measured one, and says "The measurements do not say." when no tool answers.
- A tool call limit ends in a sentence with the count, never in "An error occurred".
- The advisories list keeps its own columns, menu and order when narrowed, and "Show every advisory" puts it back.
- The fill marks each field it wrote with a revert control, and saving stays the Save button's alone.
- Every prompt, tool call, refusal and turn ending is in the assistant tail for the diagnostics view.

## Verdict

Pass for what the feature document asks, with the major issues above recorded and the live wording left to P-18.

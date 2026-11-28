# Feature 10: Verdict workbench

## Overview

The queue of what one verdict would close, and the form that composes it. The output leaves through GitHub's own editor, exactly as the JavaScript workbench does, because publishing verdicts is cve-status's job, not this application's. Covers W1 to W4.

## Behaviour

The layout and the words are the prototype's, compared past its gate on 2026-11-22 in `specs/reviews/prototype/workbench.md`.

### The queue

- `/findings/workbench`, titled "The verdict queue", with its destination in the bar named "Verdict workbench", D-220. The "All series" link back, the lead, "N findings carry no verdict on the heads of the series we follow, over M packages. The first row below is worth P per cent of that on its own.", and the prototype's hint on what a row is.
- The series as chips, "every series" and one per series with what waits there, held in the address as `?series`, D-130.
- One disclosure per package of `data/queue.json`, ranked by how much one verdict would close: the count, the package, "to judge" or "answered", and "N advisories on M series, ..." with the Spring Boot sentence where Spring Boot owns the version, D-220. One row is open at a time.
- Opened, a row shows the check lists that already list its advisories and the branches nobody could read; on a series, the advisories that reach other series only; and the evidence: what it resolves to on each series, the chains that bring it and who declares them, the CPEs matched, the scanners that reported it, what upstream fixed or does not list, and the frontend hint for an npm package, D-221.
- The summary over the queue, counts by severity and by series in figures, D-119, after the queue and shut.

### The form

- Inside an opened row, the fold "Write a verdict" holds the one form, prefilled with the package and the advisories nobody has judged, or every advisory when all are answered, D-124. With no row open it waits at the foot of the page for a package typed by hand.
- "This package already carries a verdict": a "fill from X" per verdict the package carries; filling takes that verdict's words and promises, points `supersedes` at it and ticks the advisories it answered; a package carrying one verdict arrives filled from it, D-222, and one carrying several arrives filled from the newest that speaks on the series chosen, or the newest of all, D-278.
- "Which advisories this verdict answers": one tick per advisory, with its severity, its series and what is known of it; the ticks write the advisories field, D-221.
- A FormLayout over the `VerdictDraft` entity with `BeanValidationBinder`, validation groups per `01-domain-model.md`: `Draft` to save, `Ready` to export, the commentary among the required, D-223. Labels sit beside the fields through the 25.4 side label support with `LabelTextAlign`.
- Fields, labelled and explained in the prototype's words where it has them: verdict id, package coordinate, advisories, what we are saying from the closed vocabulary with each word's sentence, how far it was taken, which Vaadin versions it applies to, what it means for your application, what the reader can do today, why, what it is waiting on, the release that will carry the fix, the pull requests that fix it, what was checked, supersedes, who assessed it.
- A paste box above it: prose in, fields filled by the Form AI controller per `06-ai.md`, markers on what the model wrote, nothing saved until a person says so.
- Drafts persist in H2 and survive a restart. One draft per advisory and package pair, newest wins, listed at the foot of the page.

### The output

- The YAML the cve-status repository expects, rendered live beside the form in a Markdown code block as the reader types.
- "This closes N advisories on M series, ..., which is K findings off the page", with what it silences, from the queue's own numbers; the platform branches that would still go red; where the file belongs; and the check script to run, in the prototype's words.
- Open in GitHub: a `NativeForm` GET to `https://github.com/vaadin/cve-status/new/main` with `filename=data/assessments/<draft id>.yaml` and `value=<yaml>`, enabled once Export has validated the `Ready` group, D-127. The 7000 character cap of the JavaScript workbench holds. A copy control beside it for the YAML alone.

## Edge cases

| Scenario | Behaviour |
| --- | --- |
| Export asked on a half filled draft | Refused by the `Ready` group with the missing fields named by the binder |
| An applicability of risk with no impact prose | Refused: a risk verdict without an impact answers nothing |
| A YAML value too long for a GET URL | The GitHub control disables with one sentence and the copy control remains |
| Two team members drafting the same pair | Last save wins and the form says who saved last |

## Acceptance criteria

### AC1: The queue ranks truthfully
- [x] Order and numbers match the fixture
- [x] The series filter narrows and lives in the address

### AC2: The form holds the line
- [x] Draft saves with formats only, Ready requires the requireds
- [x] The vocabulary is the closed set, nothing else offered

### AC3: The YAML is the contract
- [x] The rendered YAML round trips through a YAML parser to the draft's fields
- [x] Absent optional fields produce no YAML keys

### AC4: The handoff works
- [x] The GitHub URL carries the path and the YAML, verified by decoding it
- [x] Above the length cap the link disables with one sentence and the copy control remains

## Test cases

| Id | Given | When | Then | Tier | Verified by |
| --- | --- | --- | --- | --- | --- |
| WRK-01 | The queue fixture | Opening the workbench | Fixture order, counts | browserless | `WorkbenchBrowserlessTest` |
| WRK-02 | A row selected | Reading the form | Package and advisory prefilled | browserless | `WorkbenchBrowserlessTest` |
| WRK-03 | A half draft | Save then export | Save passes, export refused naming fields | browserless | `WorkbenchBrowserlessTest` |
| WRK-04 | A ready draft | Reading the YAML | Round trips, no absent keys | unit | `VerdictYamlTest` |
| WRK-05 | A ready draft | The GitHub control | The decoded URL carries path and YAML | unit | `VerdictYamlTest` |
| WRK-06 | A restart | Reopening | The draft is still there | browserless | `WorkbenchBrowserlessTest` |

The AI fill's test case lives in `features/11-assistant.md` as AST-08, with the epic that builds it.

## Still open

Written from the code as it stands at the close of the prototype alignment, 2026-11-22, after session six rebuilt the queue, D-220 to D-223. Each line names where the code shows it and the decision or draft behind it.

- One row is open at a time, where the prototype opens as many as the reader likes: the form is one, `WorkbenchView.opened`, D-220.
- The platform pull request warning appears only for a verdict that silences, and only when some branch's check list misses an advisory of the package, `WorkbenchView.drawOutput`, `QueueRules.stillRed`.
- Open in GitHub is two steps, not one form: Export validates the `Ready` group, and only then is the `NativeForm` submit enabled. Any edit withdraws it, `WorkbenchView.export`, D-127.
- The YAML GitHub receives has CR LF line breaks, and the 7000 character cap is measured on the form encoded address, `VerdictYaml.handover`, D-129. Whether GitHub keeps the CR LF is P-17, the person's to check.
- The YAML carries no header comment and no null keys, where the prototype writes both, `VerdictYaml`, D-122.
- The `Ready` group follows `01-domain-model.md` only in part: it also requires impact for `no_risk` and the commentary, and it does not enforce a fix version when `fixExpected` names a release, `VerdictDraft`, `VerdictForm`, D-123, D-223.
- The queue fixture has no unjudged advisory, so the ranking by what one verdict would close on a series is proven only by `QueueRulesTest` on a queue built in the test.
- Kept as app extras the prototype does not have: the summary in figures, the live YAML beside the form, Save, Export in two steps, Copy, the drafts, the paste box and the id rule of D-124, D-220.
- No browser test covers the workbench; the closed list names none, and `CspIT` covers only its `form-action`, D-128.
- The Definition of Done says the YAML round trips on every fixture draft; there is no set of fixture drafts, `VerdictYamlTest` round trips one full draft and one minimal draft built in the test.

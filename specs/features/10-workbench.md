# Feature 10: Verdict workbench

## Overview

The queue of what one verdict would close, and the form that composes it. The output leaves through GitHub's own editor, exactly as the JavaScript workbench does, because publishing verdicts is cve-status's job, not this application's. Covers W1 to W4.

## Behaviour

### The queue

- A Grid over `data/queue.json`: one row per package, ranked by how much one verdict would close, with the advisories it answers, the series it touches, and a series filter held in the address.
- A summary over the queue, counts by severity and by line, drawn with Charts if the numbers earn a picture and left as figures if they do not; the epic decides and records which.
- Selecting a row opens the form with the package and advisory prefilled.

### The form

- A FormLayout over the `VerdictDraft` entity with `BeanValidationBinder`, validation groups per `01-domain-model.md`: `Draft` to save, `Ready` to export. Labels sit beside the fields through the 25.4 side label support with `LabelTextAlign`, because a verdict form is read down a narrow left edge.
- Fields: advisory id, package coordinate, applicability from the closed vocabulary, depth, affects, impact, workaround, commentary, fix expected, fix expected version, fix PRs, evidence, supersedes.
- A paste box above it: prose in, fields filled by the Form AI controller per `06-ai.md`, markers on what the model wrote, nothing saved until a person says so. The box is this screen's shape but epic 11's work, because it needs the provider plumbing: epic 10 ships the form complete without it, and US-11.5 adds the box.
- Drafts persist in H2 and survive a restart. One draft per advisory and package pair, newest wins, listed under the form.

### The output

- The YAML the cve-status repository expects, rendered live beside the form in a Markdown code block as the reader types, so what will be committed is what is being read.
- One sentence under it saying what this verdict would close, from the queue's own numbers.
- Open in GitHub: a `NativeForm` GET to `https://github.com/vaadin/cve-status/new/main` with `filename=data/assessments/<draft id>.yaml` and `value=<yaml>`, which opens GitHub's editor prefilled, where GitHub's authentication and review take over. The JavaScript workbench refuses the link above 7000 characters because browsers 414 somewhere past that, and this one keeps the same cap. A copy control beside it for the YAML alone.
- The warning the JavaScript workbench carries comes too: the matching vaadin/platform pull requests are still needed, as one sentence.

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

Written from the code at the end of epic 13. Each line names where the code shows it and the decision or draft behind it.

- The queue row shows how many advisories it answers, not which: the ids appear only once the row prefills the form, `WorkbenchView.configureQueue`, column `advisories`.
- The platform pull request warning appears only for a verdict that silences, and only when some branch's check list misses an advisory of the package. It is not always present, `WorkbenchView.drawOutput`, `QueueRules.stillRed`. No decision row.
- Open in GitHub is two steps, not one form: Export validates the `Ready` group, and only then is the `NativeForm` submit enabled. Any edit withdraws it, `WorkbenchView.export`, D-127.
- The YAML GitHub receives has CR LF line breaks, and the 7000 character cap is measured on the form encoded address, `VerdictYaml.handover`, D-129. Whether GitHub keeps the CR LF is P-17.
- The `Ready` group follows `01-domain-model.md` only in part: it also requires impact for `no_risk`, and it does not enforce a fix version when `fixExpected` names a release, `VerdictDraft`, `VerdictForm`, D-123.
- The Definition of Done says the YAML round trips on every fixture draft; there is no set of fixture drafts, `VerdictYamlTest` round trips one full draft and one minimal draft built in the test.

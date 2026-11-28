# Feature 10: Verdict workbench

## Overview

The queue of what one verdict would close, and the form that composes it. The output leaves through GitHub's own editor, exactly as the JavaScript workbench does, because publishing verdicts is cve-status's job, not this application's. Covers W1 to W4.

## Behaviour

The layout and the words are the prototype's, compared past its gate on 2026-11-22 in `specs/reviews/prototype/workbench.md`.

### The queue

- `/findings/workbench`, titled "The verdict queue", with its destination in the side navigation named "Verdicts", D-245. The "All series" link back, the lead, "N findings carry no verdict on the heads of the series we follow, over M packages. The first row below is worth P per cent of that on its own.", and the prototype's hint on what a row is. With nothing waiting the lead reads "Nothing on X is waiting for a verdict." followed by "The N packages below are all answered." when there are any. X is "the heads of the series we follow", or "the head of S" on a series.
- The series as chips in a row: "every series" first, then one per series, newest first, each a white pill with the number waiting there set back after the name and "N advisories to judge on X" on hover; the chosen one is tinted. The choice is held in the address as `?series`, D-130, D-275.
- One disclosure per package of `data/queue.json`, ranked by how much one verdict would close, D-275: a white card with a soft shadow and a 4 pixel start edge, amber when a verdict would close anything and green otherwise. Its summary holds the count it would close as the row's number in bold; the package name quiet, its purl on hover; the state as the explorer's badge, "to judge" in the colours of Not assessed yet or "answered" in those of Not affected; then "N advisories on M series, a, b", series newest first, followed by ". Spring Boot owns the version, so a bump is not one of the answers here" where Spring Boot owns it, D-220. One row is open at a time. On a series, a package that does not reach it is left out and the rest are ranked by what would close there, keeping the queue's order between equals.
- Opened, a row shows the check lists that already list its advisories and the branches nobody could read; on a series, the advisories that reach other series only; and the evidence: what it resolves to on each series, the chains that bring it and who declares them, the CPEs matched, the scanners that reported it, what upstream fixed or does not list, and the frontend hint for an npm package, D-221.
- After the queue come the fold for a package typed by hand, then the summary over the queue, "The queue in figures", shut, with counts by severity and by series in figures, D-119, then the footer in the prototype's words, then the saved drafts.

### The form

- Inside an opened row, after the evidence, the fold "Write a verdict", in capitals, with "composes the YAML and the pull request, writes nothing by itself" beside it, shut, holds the one form. It is prefilled with the package and the advisories nobody has judged, or every advisory when all are answered, D-124. With no row open, the form waits at the foot of the queue in a fold of its own, "Write a verdict for a package typed by hand", also shut.
- "This package already carries a verdict": one "fill from X" per verdict, with its applicability beside it. Filling copies the verdict's applicability, its range unless it is "*", who assessed it, why, the impact, the workaround, what it waits on, the promised releases and the pull requests; it sets the depth to read, not run, points `supersedes` at it, ticks the advisories it answered, and says "Filled from X, which answers N advisories here. ...". On a series its range does not reach, it adds "Its range, R, does not reach S, ...". A package with no draft kept arrives filled from its newest verdict by `assessed_at`, D-222, D-278. "Newest" is chosen among the verdicts that speak for one of its advisories on the chosen series, or among all of them when no series is chosen or none speaks there. A verdict with no date counts as the oldest, and a tie goes to the first met.
- "Which advisories this verdict answers": one tick per advisory, with its severity, its series and what is known of it; the ticks write the advisories field, D-221.
- The form is one column, its labels beside the fields, 10em wide and aligned to their end, through the 25.4 side labels of Form Layout. It binds the `VerdictDraft` entity with two levels of checking, defined in `01-domain-model.md`: saving a draft checks only formats, and exporting also requires what a verdict needs, the commentary among it, D-223.
- Fields, labelled and explained in the prototype's words where it has them: verdict id, package coordinate, advisories, what we are saying from the closed vocabulary with each word's sentence, how far it was taken, which Vaadin versions it applies to, what it means for your application, what the reader can do today, why, what it is waiting on, the release that will carry the fix, the pull requests that fix it, what was checked, supersedes, who assessed it. One line fields: verdict id, package, Vaadin versions, the release that will carry the fix, supersedes, who assessed it. Selects: what we are saying and how far it was taken, each word followed by its sentence. Every other field is a text area.
- The proposed verdict id is the purl without "pkg:", every run of characters other than letters and digits turned into one hyphen and lowercased, then the series the same way when one is chosen, then the day as yyyy-mm-dd, all joined by hyphens; the person may edit it, D-124.
- A paste box above the form, "Fill from prose", offered only where the assistant is, D-236: prose in, fields filled by the Form AI controller per `06-ai.md`, each marked "Filled from the pasted text". The model's instructions tell it never to write the verdict id, who assessed it or supersedes. Nothing is saved until a person presses Save.
- Drafts persist in H2 and survive a restart, one per advisory and package pair, newest wins. Under "Saved drafts" they are listed with the columns Verdict, Advisories, Package, Saved by and "Saved, UTC", or "No draft is saved yet." Clicking one shuts any open row and restores the draft into the fold at the foot, opened. Enter in a one line field of the form saves, as Save draft does; text areas keep Enter for a new line, D-256.

### The output

- The YAML the cve-status repository expects, rendered live beside the form in a Markdown code block as the reader types.
- "This closes N advisories on M series, ..., which is K findings off the page", with what it silences, from the queue's own numbers; the platform branches that would still go red; where the file belongs; and the check script to run, in the prototype's words.
- Open in GitHub: a `NativeForm` GET to `https://github.com/vaadin/cve-status/new/main` with `filename=data/assessments/<draft id>.yaml` and `value=<yaml>`, enabled once Export has validated the `Ready` group, D-127. It opens in a new tab. The 7000 character cap of the JavaScript workbench holds: above it the control disables and says "Too long for a link: N characters, where browsers refuse past 7000. Copy the YAML into P instead." A copy control beside it, "Copy the YAML", for the YAML alone.

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
| WRK-07 | A package carrying several verdicts | Opening it, then on 24.9 | Filled from the newest, then from the newest speaking on 24.9 | browserless | `WorkbenchBrowserlessTest` |
| WRK-08 | A one line field | Pressing Enter | The draft saves as Save does | browserless | `WorkbenchBrowserlessTest` |
| WRK-09 | A series chosen | Ranking the rows | By what would close there, the queue's order between equals | unit | `QueueRulesTest` |

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

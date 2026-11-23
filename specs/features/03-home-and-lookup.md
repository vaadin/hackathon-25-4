# Feature 03: Home and the lookup

## Overview

The front door: every maintained line at a glance, the recently reviewed advisories, and the lookup that carries all three ways of asking. Covers H1 to H4, and the entry halves of V1, C1, S1.

The layout follows the prototype, compared past its gate on 2026-11-22 in `specs/reviews/prototype/home.md`. Every customer page opens the same way in one centered column of 912 pixels: the H1 Security status, the purpose paragraph, the lookup in a tinted box. Home continues under it with the cards, the paragraph for older releases, Recently reviewed and the pointer box, in that order.

### The opening of every customer page

- The customer shell draws it, so every customer route gets it from one place: the H1 Security status, then the purpose, "Find out whether your Vaadin version is safe, and what to do if it isn't. Every supported version is scanned continuously and every finding is reviewed by the Vaadin team.", transcribed from the prototype, then the lookup, D-169, D-164.
- Home has no heading of its own under it. Another customer view's title, the version, the advisory id, Scanner report, All advisories, Cases, is the H2 of its answer, under the lookup.
- The app bar's trail reads Vaadin, My account, Security status, then the leaf; My account and Security status lead home. The measured chip carries the hover sentence "These are the real measurements of that date, not sample data", D-162, D-163.
- The prototype's Enterprise chip is not drawn: nothing published says what a reader subscribes to, a data need in `specs/FEEDBACK-25.4.md`.

## Behaviour

### The lookup

- One component, three tabs: By version, By CVE, Upload scanner report. Mounted once in the customer shell's column, state in the session scoped `LookupState` signals, so switching tabs or navigating never loses what was typed. A tab change shows the tab's fields; under an answer it also returns to the home page, so a new question never sits on top of the last answer, as the prototype does. Decided by Manolo on 2026-11-23, P-14, D-230, superseding D-39 and D-165.
- The prototype's tip under the fields, which promises project linking and change notices, is not shown: this application has neither, D-166.
- The version field cleans its input: `V24.9.20`, `Vaadin 24.9.20` and `24.9.20` are the same question. Its placeholder shows the newest measured head as an example, written fainter than any real value, and is never prefilled, rule 6.8.4 of the source spec.
- The version field is a ComboBox over the measured versions that takes free text, so typing `24.9` offers the releases of that line through the ordinary filter, and a version we never measured can still be submitted, because the not measured answer belongs to the version screen, not to the field. The prototype's field is a plain search field; the list of measured versions is an app extra. The requirement for `PartialMatchMode` is removed, D-173.
- The ids field takes one or more CVE or GHSA ids split on spaces, commas, semicolons or newlines, plus an optional version field beside it. A CVE pasted into the version field jumps to the ids tab with the id carried over.
- The report tab is a drop zone and a file button; its behaviour is epic 06. A version field appears on it only when the uploaded file does not name one.
- Check submits and navigates to the answering route. The button takes `DisableOnClickMode` so a slow answer cannot be asked twice.
- Clearing a field with its clear control or Escape withdraws the question and navigates home. The clear control is visible whenever the field holds something, not only on hover.
- The slash key focuses the lookup from anywhere, through a shortcut with a JS filter that stays quiet while another input has focus.
- Each tab offers its help as a `HelpDialog` from a question mark on the field's label, and the dialog explains the fields, offered full or empty. The prototype has help on the ids and report tabs only; the version tab's help is an app extra, P-05.

### The cards

- The section heading Supported versions at a glance, with the aside "Status of the latest maintenance release in each minor" at the right of its line.
- One card per followed line, newest first, from `index.lines`, four to a row in the column and one to a row on a phone: the minor, the Extended mark, the state icon at the right, the state word, Secure, Fix available, N findings open, or Not assessed yet, the action line, On X or later, Upgrade to X, or Newest is X, and Scanned date. The card is tinted by its state.
- The Extended mark's hover text is the prototype's sentence built from the line's published label, major and end of free maintenance, "Extended maintenance. Free maintenance for Vaadin 24 ended on 2026-06-16, and every release since then needs a subscription. Whether yours covers it is between you and Vaadin: we do not know what a given customer holds.", or the label alone when the line does not publish the other two, D-167.
- A card is one link, to `/version/<newest measured>`. The whole card is clickable and is a real link, openable in a new tab.
- Under the cards, "Running an older maintenance release? Enter it above. If we have not measured it, the answer is to move to the latest one in your minor."
- Prerelease lines render only with `?pre`, matching the source page.

### Recently reviewed

- The eight newest verdicts from `index.recent`, in one bordered box: id, the first sentence of the impact, whole, the verdict chip in customer words from `labels`, the date, and a chevron. Only the risk verdict, affects you, is drawn in the error variant, D-172, D-171. A link, All N advisories, to `/advisories`, at the right of the heading.
- A static pointer box, Scanner flagged Tomcat, Spring or logback?, in the designer's words, which send the reader to the version field above; the prototype's box has no link either, D-168.

## Edge cases

| Scenario | Behaviour |
| --- | --- |
| A line whose newest measurement is stale | The card says its scanned date plainly, nothing is highlighted |
| The ids field holds garbage among ids | The ids that parse are asked, the rest are reported as not recognised, in place |
| Submitting an empty lookup | Nothing happens, the button is disabled while the question is empty |
| A reader on a phone | The cards stack, the lookup tabs remain reachable, no horizontal scroll |

## Acceptance criteria

### AC1: The lookup keeps state
- [x] Switching tabs keeps the version and the ids
- [x] Navigating to an answer and back keeps them too
- [x] Escape in a full field clears it and navigates home

### AC2: Input is cleaned
- [x] `Vaadin 24.9.20` asks about `24.9.20`
- [x] A CVE pasted into the version field lands on the ids tab

### AC3: The cards tell the line's truth
- [x] State word and icon match the index state for every line
- [x] The action line names the index's `upgrade_to` when there is one
- [x] Prereleases appear only with `?pre`

### AC4: Keyboard
- [x] Slash focuses the lookup, except while typing elsewhere
- [x] Check is disabled on click until the navigation answers

### AC5: The prototype's distribution
- [x] Every customer route opens with the H1, the purpose and the lookup, in that order, above its own content
- [x] The trail passes through My account, and the measured chip carries its hover sentence
- [x] The Extended mark's hover sentence is built from published fields only, and falls back to the label
- [x] Only the risk verdict is drawn in the error variant in Recently reviewed

## Test cases

| Id | Given | When | Then | Tier | Verified by |
| --- | --- | --- | --- | --- | --- |
| HOM-01 | The index fixture | Opening home | One card per non prerelease line, newest first, with the fixture's states | browserless | `HomeBrowserlessTest` |
| HOM-02 | `?pre` | Opening home | The prerelease lines appear | browserless | `HomeBrowserlessTest` |
| HOM-03 | `Vaadin 24.9.20` typed | Check | The route is `/version/24.9.20` | browserless | `LookupBrowserlessTest` |
| HOM-04 | A CVE in the version field | Typing it | The ids tab is active with the id | browserless | `LookupBrowserlessTest` |
| HOM-05 | A version typed, tab switched twice | Reading the field | The version is still there | browserless | `LookupBrowserlessTest` |
| HOM-06 | An answer on screen | Escape in the lookup | Home, with the question withdrawn | browserless | `LookupBrowserlessTest` |
| HOM-07 | Focus in a dialog text field | Slash | No focus steal | browserless | `LookupBrowserlessTest` |
| HOM-08 | The recent list in the fixture | Opening home | Eight rows, first impact sentences, verdict chips in customer words, the risk chip alone in the error variant | browserless | `HomeBrowserlessTest` |
| HOM-09 | Home, and a nested customer route | Rendering | The column holds the H1, the purpose, the lookup and the content in that order, the trail passes through My account, the chip has its hover sentence | browserless | `ShellBrowserlessTest` |
| HOM-10 | The index fixture, then one without `free_ended` | Opening home | The Extended mark's hover is the full sentence, then the label alone | browserless | `HomeBrowserlessTest` |

## Still open

Written from the code at the close of the prototype alignment, after D-228. Each line names where the code shows it and the decision or draft behind it.

- The version ComboBox does not use `PartialMatchMode`, because 25.4.0-beta2 refuses it beside custom values; free text is kept, `Lookup.versionPanel`, D-40. The requirement left this document with D-173, and the task of US-3.1 that asked for it now reads as removed rather than open, D-228.
- The slash key is not a shortcut registration: it is a `keydown` listener on the UI with a declared filter, `Lookup.KeysJs`, because `ShortcutFilterJs` is internal. Workaround to remove when flow#5473 is answered, draft 12b; US-3.1 keeps its tick on that basis, D-161.
- Workaround to remove when draft 13 is answered: Check's enabled state is set from a `Signal.effect` instead of `bindEnabled`, which is refused beside disable on click, `Lookup.checkButton`.
- Clearing a field navigates home only when the answer of that field is on screen, `Lookup.goHomeFrom`; clearing the version beside the ids asks the same ids again without a version, `Lookup.withdrawIdsVersion`.
- The ids field is a single line `TextField`, `Lookup.idsPanel`; splitting on newlines in `LookupInput.splitIds` has not been checked against what a browser keeps of a pasted multi line list.
- The tier badge appears only for the extended tier, reads "Extended" and has the prototype's sentence as hover text, built from published fields, `HomeView.extendedSentence`, D-167, which superseded D-44. A `title`, not a Vaadin tooltip, so a keyboard reader does not reach it; the badge is not focusable either way.
- A card whose release has no summary row shows no state word and no icon, where the reference shows Not assessed yet, `HomeView.card`, D-45. The prototype could not settle it: no card lacked a summary row on the day of the comparison.
- The action line puts Upgrade to X first, then On X or later, then Newest is X, `HomeView.actionLine`, D-46. The prototype did not settle the order: no card on the day of the comparison carried an upgrade.
- A recent row with no impact shows the first sentence of `commentary` instead, `HomeView.firstSentence`; a recent row with no applicability gets the `in_triage` chip, which the glossary defines as the absence of a verdict, `HomeView.recentRow`.
- The pointer box keeps the designer's words, which send the reader to the version field, and does not point at the report tab, D-47, confirmed by the prototype, D-168.
- A tab change under an answer returns to the home page, D-230. The prototype's lookup tip and Enterprise chip are not drawn, D-166 and the data need row.
- Workaround to remove when draft 17 is answered: `@Uses(Lookup.class)` on `HomeView` so that the production bundle includes the lookup, D-79.

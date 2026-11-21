# Feature 03: Home and the lookup

## Overview

The front door: every maintained line at a glance, the recently reviewed advisories, and the lookup that carries all three ways of asking. Covers H1 to H4, and the entry halves of V1, C1, S1.

## Behaviour

### The lookup

- One component, three tabs: by version, by advisory ids, by report. Mounted in the customer shell, state in the session scoped `LookupState` signals, so switching tabs or navigating never loses what was typed.
- The version field cleans its input: `V24.9.20`, `Vaadin 24.9.20` and `24.9.20` are the same question. Its placeholder shows the newest measured head as an example, written fainter than any real value, and is never prefilled, rule 6.8.4 of the source spec.
- The version field is a ComboBox over the measured versions with `PartialMatchMode`, so typing `24.9` offers the releases of that line, and a version we never measured can still be submitted as free text, because the not measured answer belongs to the version screen, not to the field.
- The ids field takes one or more CVE or GHSA ids split on spaces, commas, semicolons or newlines, plus an optional version field beside it. A CVE pasted into the version field jumps to the ids tab with the id carried over.
- The report tab is a drop zone and a file button; its behaviour is epic 06. A version field appears on it only when the uploaded file does not name one.
- Check submits and navigates to the answering route. The button takes `DisableOnClickMode` so a slow answer cannot be asked twice.
- Clearing a field with its clear control or Escape withdraws the question and navigates home. The clear control is visible whenever the field holds something, not only on hover.
- The slash key focuses the lookup from anywhere, through a shortcut with a JS filter that stays quiet while another input has focus.
- Each tab offers its help as a `HelpDialog` from a question mark on the field's label, and the dialog explains the fields, offered full or empty.

### The cards

- One card per followed line, newest first, from `index.lines`: the minor, the support tier badge, the state icon and word, Secure, Fix available, N findings open, or Not assessed yet, the action line, On X or later, Upgrade to X, or Newest is X, and Scanned date.
- A card is one link, to `/version/<newest measured>`. The whole card is clickable and is a real link, openable in a new tab.
- Prerelease lines render only with `?pre`, matching the source page.

### Recently reviewed

- The eight newest verdicts from `index.recent`: id, the first sentence of the impact, the verdict chip in customer words from `labels`, the date. A link, All N advisories, to `/advisories`.
- A static pointer box, scanner flagged Tomcat, Spring or logback, pointing at the report tab, in the designer's words.

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
| HOM-08 | The recent list in the fixture | Opening home | Eight rows, first impact sentences, verdict chips in customer words | browserless | `HomeBrowserlessTest` |

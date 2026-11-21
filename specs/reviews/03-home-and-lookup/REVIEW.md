# Review: epic 03, home and the lookup

Reviewed on 2026-11-21 against the application in development mode on 8080, reading the committed fixtures' shape from the live site, signed out, at 1920 by 1080 and at 390 by 844. Screenshots: `new-home.png` and `reference-home.png` in this folder.

## What could and could not be compared

The reference customer page shows a sign in gate first, `reference-home.png`, and nobody in this pass has an account, P-03. So nothing behind the gate was compared on screen. What the reference does was read from its sources in vaadin/cve-status, `web/ee/screens/home.js`, `web/ee/ui/chrome.js` and `web/ee/ui/help.js`, for behaviour and wording only, and from `docs/requirements.md` section 6. Every comparison below that names the reference is to those sources, not to a rendered page.

## UI verification, per acceptance criterion

All walked in Chrome through Playwright on the running application, with the accessibility tree and DOM reads, not screenshots.

| Criterion | Result |
| --- | --- |
| AC1 switching tabs keeps the version and the ids | Pass. Ids pasted, tabs switched three times, the ids and the version still there |
| AC1 navigating to an answer and back keeps them | Pass. `/version/24.9.20` and back home, the field still holds 24.9.20 |
| AC1 Escape in a full field clears it and navigates home | Pass. On `/version/24.9.20` the first Escape closes the open dropdown, the second empties the field and lands on `/` with Check disabled. That two step is deliberate: the filter does not act while the dropdown is open |
| AC2 `Vaadin 24.9.20` asks about `24.9.20` | Pass. Typed with key presses, Check, the address is `/version/24.9.20`, the heading and the title read "Vaadin 24.9.20" |
| AC2 a CVE pasted into the version field lands on the ids tab | Pass after two fixes found here: the hidden field kept the pasted text and its open dropdown covered the page once shown again. Both fixed, D-41 |
| AC3 state word and icon match the index | Pass for the nine lines of the fixture, read from the DOM |
| AC3 the action line names `upgrade_to` | Not visible on the fixture, where no newest release carries one. Proven by `HomeBrowserlessTest` on a changed index |
| AC3 prereleases only with `?pre` | Pass, `?pre` shows 25.4 first |
| AC4 slash focuses the lookup except while typing elsewhere | Pass. From a heading the slash focuses the version field and no `/` is typed. In a text field inside a modal `<dialog>` added to the page the slash is typed and focus stays |
| AC4 Check disabled on click until the navigation answers | Pass. A mutation observer saw the button go disabled at the click and enabled again with the response |
| Phone width | Pass after one fix: at 390 pixels nothing scrolls sideways, the cards stack, the tabs scroll. The trail of the app bar ran under the measured chip, an epic 01 layout, and now wraps to its own row |
| Console | One 404 for `favicon.ico`, known from epic 01. No client exception |
| `vaadin.csp=warn` log | Nothing logged for the new code. The four filters and the field cleanup are declared, and `CspIT` passes in production mode |

## The support engineer

- The cards answer the first question at a glance and each is a link I can open in a new tab. Good. But eight of nine cards carry the same "Extended" mark, so it says nothing to me unless I hover it, and hover is not something I can paste into a ticket.
- "1 finding open" on 24.7 in red reads as alarming as "25 findings open" on 24.4. The count is there, the weight is the same.
- The verdict chips in Recently reviewed are all the same blue. "affects you" looks exactly like "not used by Vaadin". The words are right, the eye has nothing to go on.
- "Security status" appears twice before the cards: in the trail and as the page heading, and the heading sits under the lookup, pushing the cards down by about 90 pixels.
- Typing a CVE id by hand into the version field does not move it to the CVE tab until I press Enter or Check. Pasting does. I would not know which one I did.
- The Upload tab draws a file button that does nothing. It looks enabled at a glance and is not. I am told epic 06 brings it.
- The help on the version tab promises that an unmeasured version is answered with the nearest measured release. That screen is epic 04, so today the promise points at a heading.

## The pedantic analyst

| Metric | Reference | New | Delta | Verdict |
| --- | --- | --- | --- | --- |
| Facts above the fold for the same question, home | Not measured, behind the gate | 17 of 17 rows and cards above 1080 pixels: nine cards with four facts each, eight advisory rows | Unknown | Not comparable |
| Clicks from arrival to the verdict | Not measured | 0 for a line's state on home. For one version: type, then one click on Check | Unknown | Not comparable |
| Data area as a percentage of the viewport, home | Not measured | About 48: the cards 1920 by 134 and the rows 1920 by 380, of 1920 by 1080 | Unknown | Not comparable |
| Base font size | 16px on the gate | 16px | 0 | Pass |
| Smallest click target | Not measured | The "All 535 advisories" link, 127 by 22, and the question mark, 27 by 24 | Unknown | Small, under 24 pixels high for the link |

## Issues

### Critical

None found.

### Major

1. Verdict chips in Recently reviewed carry no difference between a risk and a dismissal. The `labels` words are right, but `affects you` and `not used by Vaadin` are drawn identically. `.verdict-chip` has a class per applicability and no rule uses it.
2. The page heading "Security status" repeats the trail and sits between the lookup and the cards. It costs a row above the fold and answers nothing. The heading is epic 01's `ViewHeading`, rule 6.1.7 asks for one writer, which this is; whether home needs a visible one is a question for the design.
3. The report tab's disabled upload does not look disabled. D-42.

### Minor

1. The "Extended" mark on eight of nine cards is noise; the tier sentence is only in the hover text.
2. The open count is the same red at 1 and at 25.
3. The question marks are 27 by 24 pixels and the "All advisories" link is 22 pixels high.
4. A typed CVE id moves tabs only on commit, a pasted one at once, D-41. Defensible, and invisible to the reader.
5. The help dialog of the version tab promises the version screen's unmeasured answer before epic 04 exists.
6. The dev tools button of development mode sits over the pointer box in the screenshot. Not in production, noted so nobody files it.

## Deliberate improvements over the reference

- The lookup lives in the session, so a second tab of the same browser opens with the question already in it.
- Ids that do not parse are reported beside the ones that are asked, where the reference drops them silently or refuses the whole list.
- The slash key focuses the lookup, which the reference does not have.
- Check cannot be pressed twice for one question.

## Verdict

Pass, with the three major issues above left for the polish pass and for a person: they are taste and design, not wrong answers.

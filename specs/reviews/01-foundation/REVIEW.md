# Review: Feature 01, the customer shell (US-1.1)

Reviewed on 2026-11-20 against `https://vaadin.github.io/cve-status/`. Screenshots at 1920 by 1080: `new-home.png`, `reference-home.png`, plus `new-home-phone.png` at 390 by 844.

**Limit of this review.** The reference page answers with its sign in gate, so `reference-home.png` shows the gate and not the home screen. The reference header was read from the DOM behind the gate, where it says "Vaadin / My account / Security status / Measured 2026-11-20 Enterprise". Nothing below the header could be compared. The new screenshot was taken before the gutters were widened from 8px to 24px, so it shows the tighter spacing.

## The support engineer

1. The page is empty. There is a heading and nothing to ask a question with. The lookup mount exists but holds nothing until epic 03, so today nobody can answer a ticket from this screen.
2. The date is gone. The reference header always says when the data was measured. The new header says nothing, because no measurement has been read until epic 02 lands. The chip is correct to stay silent, but the reader loses a fact the old page stated on every screen.
3. The trail is shorter. The reference reads Vaadin, My account, Security status. The new one reads Vaadin, Security status. Anyone who used "My account" to get back to their account page has lost that link.
4. The "Enterprise" marker of the reference header has no counterpart.
5. The tab has no icon. The browser asks for `/favicon.ico` and gets a 404, so several open answers are told apart by title only.
6. An unknown address shows an empty tab title. The app bar and the link home are there, but the tab itself says nothing.

## The pedantic analyst

| Metric | Reference | New | Delta | Verdict |
| --- | --- | --- | --- | --- |
| Facts above the fold for the same question | Not measurable, the gate hides the screen. The header alone carries 5: three trail items, the measured date, the plan marker | 2 trail items and the heading, which repeats one of them | At least 3 fewer in the header | Fail |
| Clicks from arrival to the verdict | Not measurable behind the gate | No path to a verdict exists yet | Not comparable | Not applicable until epic 03 |
| Data area as a percentage of the viewport | Not measurable behind the gate | 95 percent, 1031 of 1080px below a 49px app bar, all of it empty | Not comparable | Not applicable |
| Base font size | 16px | 14px | 2px smaller | Fail |
| Smallest click target | 45 by 26px, the Show control of the gate | 61 by 32px, the Vaadin trail link | 6px taller | Pass, though 32px is under the 44px touch guideline |

Heading size, measured for completeness: 34px in the reference, 18px here.

## Issues

### Critical

None for the scope of US-1.1. The empty screen is the scope of epics 02 and 03, not a defect of the shell.

### Major

- Base font is 14px against 16px, and the heading is 18px against 34px. Both come from Aura defaults nobody has tuned yet. Owner: US-1.3.
- No measured date anywhere until epic 02 publishes one. The mechanism is proven by `ShellBrowserlessTest`, the fact is still missing on screen.

### Minor

- No favicon, one 404 in the console on every load.
- The not found view leaves the document title empty.
- "My account" and the "Enterprise" marker are not in the trail. The feature document specifies the shorter trail, so this is recorded as a difference and not changed.
- The trail link is 32px tall.
- At 390px the two trail items fit with no horizontal scroll. The overflow menu was not exercised because no trail is long enough yet.

## Deliberate improvements

- The trail is a real `navigation` landmark with `aria-current="page"` on the leaf, from the Breadcrumbs component.
- The document title, the heading and the trail leaf come from one resolved title, so they cannot disagree.
- An unknown address keeps the app bar and offers the way home.
- No sign in gate in front of public answers, per `00-overview.md`.

## Verdict

Fail as a screen, pass as a shell. The structure the story asked for is in place and verified, but measured without rounding the page states fewer facts than the reference header and uses smaller type. The font findings go to US-1.3, the missing date to epic 02, the missing lookup to epic 03.

---

# Review addendum: the internal shell and sign in (US-1.2)

Reviewed on 2026-11-20. Screenshots at 1200 by 941, the size of the tab that was signed in: `new-login.png`, `new-findings.png`. Not 1920 by 1080, because resizing would have meant a second tab and a second sign in. The reference at `https://vaadin.github.io/cve-status/findings/` sits behind its own gate and was not opened past it, so the comparison is against the gate of the customer page captured earlier in `reference-home.png`.

## The support engineer

1. I now sign in with a username where the old gate asked for my work email. Nobody told me which username.
2. The old gate said "Trouble signing in? Contact your account manager." The new form offers nothing when I am stuck.
3. After signing in the page is a heading and one menu item that leads to where I already am. Explorer content is epic 08, but today the sign in buys nothing.
4. Signed in as the wrong account, the page tells me the address does not exist. It does exist, I am just not allowed. In development mode it even prints class and annotation names at me.
5. The drawer label says "Internal" and the single item says "Findings", which is also the heading and the trail leaf. The same word three times on an empty page.
6. The session expired notice works and keeps me on my view, but for an internal view the reload then sends me to sign in again. Two steps where the notice promised one.

## The pedantic analyst

| Metric | Reference | New | Delta | Verdict |
| --- | --- | --- | --- | --- |
| Facts above the fold for the same question | Gate: title, one instruction, one help line | Login: title only. Findings: trail, heading, one menu item | 2 fewer on the sign in screen | Fail |
| Clicks from arrival to the verdict | Not measurable behind the gate | No verdict exists yet. Arrival to the internal page: 2 fields and 1 click | Not comparable | Not applicable until epic 08 |
| Data area as a percentage of the viewport | Not measurable behind the gate | 78 percent, the content card of about 1098 by 854px in a 1200 by 941 viewport, empty | Not comparable | Not applicable |
| Base font size | 16px | 14px | 2px smaller | Fail |
| Smallest click target | 45 by 26px, Show | 72 by 34px, Sign out. The drawer toggle is 38 by 34px | 8px taller | Pass, both under 44px |

## Issues

### Critical

None.

### Major

- The refusal of a signed in user without the role reads as a missing page. Recorded in `specs/FEEDBACK-PLATFORM.md`.
- No help line on the sign in form, which the reference gate has.

### Minor

- The drawer opens by default at 1200px with one item. It earns its width only when the other three destinations exist.
- "Findings" three times on one screen.
- The drawer lists only routes that exist, so today it has one of the four destinations the feature names. This is deliberate, a link to a route that does not exist would be an invention.

## Deliberate improvements

- A real boundary. The reference gate runs in the browser over bytes already fetched.
- The signed in user lands on the address they asked for, verified in a browser and in `ShellSecurityBrowserlessTest`.
- A way out, Sign out, on every screen for any signed in session, including the account that was refused.

## Verdict

Pass for the boundary, fail for the sign in screen as a replacement of the gate: it tells the reader less than the page it replaces.

---

# Review addendum: theme and dark mode (US-1.3)

Reviewed on 2026-11-20 at 1920 by 1080. `new-home.png` is retaken with the switch and the larger type, `new-home-dark.png` is the same screen after switching.

## The support engineer

1. The switch says "Dark" and nothing else. It does not say that it follows my system until I touch it, or how to go back to following the system once I have. There is no way back short of clearing site data.
2. The switch sits where the measured date will sit. Two small things at the right edge of the bar, one a fact and one a control.
3. The page still has no content, so the only thing dark mode changes today is the background.

## The pedantic analyst

| Metric | Reference | New | Delta | Verdict |
| --- | --- | --- | --- | --- |
| Facts above the fold for the same question | Header: 5 | Trail of 2, the heading, the switch label | Unchanged by this story | Fail, carried over |
| Clicks from arrival to the verdict | Not measurable behind the gate | No verdict exists yet | Not comparable | Not applicable |
| Data area as a percentage of the viewport | Not measurable behind the gate | 95 percent, empty | Not comparable | Not applicable |
| Base font size | 16px | 16px, was 14px | 0 | Pass |
| Smallest click target | 45 by 26px, Show | 86 by 22px, the switch with its label | 4px shorter | Fail |

Heading size: 34px in the reference, 28px here, was 18px.

## Issues

### Critical

None.

### Major

- The switch is 22px tall, under the reference's smallest control and half the 44px touch guideline. The home view is on the responsive list.
- No return to "follow the system" once a choice is made.

### Minor

- The status and severity tokens exist and resolve, but nothing draws with them yet, so their contrast in both schemes is unmeasured. First consumer: epic 03.
- The heading is 6px smaller than the reference.

## Deliberate improvements

- Dark mode is decided before the first frame. `ThemeFirstPaintIT` samples every frame, and it fails when the head script is removed.
- The base font now matches the reference.

## Verdict

Pass for behaviour, with the switch height and the missing way back to the system setting open.


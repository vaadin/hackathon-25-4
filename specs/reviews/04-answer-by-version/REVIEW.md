# Review: answer by version

Screens: `new-version-fix-available.png` (24.9.21, 1920 by 1080), `new-version-unresolved-one.png` (24.7.19, 1920 by 1080), `new-version-phone.png` (24.4.28 with `?boot=1&ruled=1`, 390 by 844), `reference-version.png` (the live page on `#version/24.9.21`).

The reference page shows its sign in gate and nothing behind it, so every comparison with the reference below is against the source page's code and `docs/requirements.md`, read with `gh`, and not against pixels. P-03 holds for this review as it did for the earlier ones.

## The support engineer

- On 24.7.19, the screen with one finding and no fix, the two actions the design draws on the red card, Notify me and Or move to 24.10.14, and the scanned date are below the fold at 1920 by 1080. The card is 825 pixels tall because the finding inside it prints its whole workaround, about a thousand characters, and its whole fix sentence. The reference has the same prose, and rule 6.4.13a of the source says an opened finding is four short fields with the long prose behind a control. Here nothing is shortened. A reader who wants "what do I move to" has to scroll past a paragraph about OpenTelemetry properties to find it.
- The fix columns on the list of 25 findings on 24.4.28 have the same problem: each card is the verdict's full prose, so the list is about 11000 pixels tall. The reference shortens at a sentence boundary.
- "View 24.9.26 release notes" opened a new tab on the first draw, where the reference opens it in the same tab. Changed to the same tab before this review was written.
- The not assessed notice names its eleven ids with no severity, as the reference does, so a reader cannot tell which of the eleven is the High one without opening each.
- The Spring Boot table names at most two packages per row, as the reference does, and says nothing when there are more.
- In the ruled out table, a row whose advisory has no applicability in the catalogue shows an empty reason cell. On the fixture every row has one, so nothing empty is drawn today, but the code allows it.
- Nothing the old page said is missing from the card on the three fixture states, as far as the reference's code says what it draws. The state, the headline, the sentence, the badges, the actions and the scanned date are all there, from the same fields.

## The pedantic analyst

| Metric | Reference | New | Delta | Verdict |
| --- | --- | --- | --- | --- |
| Facts above the fold for 24.9.21 | Not measurable, sign in gate | 20: state, upgrade target, count of 12, maintenance release, same minor, tier, end of free maintenance, release notes, What it fixes, scan date and its age, the 11 unassessed ids, the fixes heading and count, 7 fix rows | Unknown | Not judged |
| Clicks from arrival to the verdict | Not measurable | 0 | Unknown | Not judged |
| Data area as a percentage of the viewport, 1920 by 1080 | Not measurable | 45, the body is capped at 72rem and starts 267 pixels down, under the lookup | Unknown | Low for a 1920 screen: the right 40 percent is empty |
| Base font size | 16px in `tokens.css` | 16px | 0 | Same |
| Smallest click target | Not measurable | 19.5 pixels high, the text links: Hide them, Show them, the advisory ids | Unknown | Under the 24 pixel minimum target of WCAG 2.2 |

## Issues

### Critical

None found.

### Major

1. Long verdict prose is drawn whole, inside the red card and in every finding card, so on 24.7.19 the card's actions fall below the fold. Rule 6.4.13a of the source asks for short fields and the rest behind a control.
2. The data area uses 45 percent of a 1920 wide viewport. The body is capped at 72rem, which keeps lines readable but leaves the right side empty on the widest screens.

### Minor

1. Section links and advisory ids are 19.5 pixels high, under 24.
2. The badges on the card all look the same: Maintenance release, Same minor, Extended maintenance and Free maintenance ended are one style, so the one that asks for money reads like the one that reassures.
3. The not assessed ids carry no severity.
4. The Spring Boot and discontinued tables cut the package list at two without saying so, as the reference does.
5. The phone layout wraps every fix row onto three lines; the scan date sits on its own line under the actions.

## Deliberate improvements

- An owner nobody established, `fix_owner` `unknown`, gets no chip, where the reference prints "Vaadin issue", D-51.
- A red version that is red only because nothing is judged yet is headed with that, instead of "0 vulnerabilities have no fix yet", D-53.
- The "newest release on its line" sentence is said only when it is true, D-54.
- The scroll discipline is proven in a real browser: a toggle leaves the reader where they were, Back closes it, a new version starts at the top, and What it fixes brings the list to the top of the viewport (`ScrollActionsIT`).
- A long word in verdict prose no longer widens the page at phone width: the first draw overflowed by 63 pixels at 390 wide and was fixed before this review.

## Verdict

Pass, with the two major issues left for a person (P-07): shortening verdict prose is a design decision about which sentence a reader sees first, and the source page's rule for it was written for a screen this application does not copy pixel for pixel.

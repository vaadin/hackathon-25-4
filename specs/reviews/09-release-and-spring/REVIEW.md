# Review: the release and Spring Boot pages

Reviewed on 2026-11-22 against the application in development mode on 8080, signed in as `team`, live data of 2026-11-21T10:13:39Z, at 1920 by 1080 in dark mode. Screenshots: `new-release.png`, the release page of 24.9.21; `new-spring.png`, the top of the Spring Boot page with every group folded by script, because the page fully open is 11,909 pixels tall and the first viewport screenshot of it came back blank while the browser was still painting.

The reference pages, `findings/release.html` and `findings/spring.html` of <https://vaadin.github.io/cve-status/findings/>, are behind their sign in gate, P-03. Nothing past the gate was compared: every statement below about the reference comes from `docs/requirements.md` section 6 and from reading the reference sources for behaviour, not from the rendered pages.

## The support engineer

- The ticket question "I am on 24.9.21, what do I do" is answered above the fold: both badges, the one line split of 135 findings, the update sentence naming the first six of the 22 findings 24.9.26 removes, and the Spring Boot sentence naming 3.5.16. The reference answers the same question in the same order.
- The reference narrows the release card by the products the reader chose on the front door and offers the chips again on the release page. This page answers for the whole release, D-111. A customer with only Flow and the starter reads 135 findings where the reference would show fewer. The explorer's selection is not even mentioned on the page.
- The reference marks each finding's verdict collapse, rule 6.4.13: the same verdict on seven rows says so once and points. Here every opened finding shows its own record in full; on 24.9.21 the 42 `configuration` findings repeat long prose.
- The reference keeps "From Vaadin" and "With Spring Boot" as two halves of the card, each with its own folds. Here the Spring Boot findings are one of the explorer's five groups, folded shut by default; the half is still listed, rule 4.5a, but under a group heading, not a half heading.
- The update sentence breaks advisory ids at their hyphen at 1920 wide: "CVE-" at the end of one line, "2026-102278" on the next. A support engineer copying the list gets a broken id.
- The Spring Boot page is long: six releases, the risks open by the explorer's fold rule, 259 risk rows open on arrival. The reference folds every Spring Boot release shut and opens only on demand. Finding 2.7.18 means scrolling past 4.2.0 to 3.2.12.
- "Measured on Vaadin 25.3.5: closes 0 of these 1" is the data said literally, and reads badly.
- The chip "Supply open" and the sentence "Supply open: this line still gets free patches." say the same thing twice on every open release.
- The breadcrumb trail carries Findings above the page and the page carries All series under the heading: two links to the same place, which rule 6.1.6 forbids for the header. Both existed before this epic, the trail since epic 01.

## The pedantic analyst

| Metric | Reference | New | Delta | Verdict |
| --- | --- | --- | --- | --- |
| Facts above the fold for the same question, release 24.9.21 | Not measured, behind the gate | Version, line, scan date, two badges, the five way split, 22 removed with six named, Spring Boot sentence with its target, two files, the two risk rows and nine unjudged rows | n/a | Not comparable |
| Clicks from arrival to the verdict | Not measured | 0: both badges and the advice are on arrival | n/a | Not comparable |
| Data area as a percentage of the viewport | Not measured | Content column 1152 pixels of 1920, 60 percent of the width; the right 40 percent is empty, `max-width: 72rem` | n/a | Not comparable |
| Base font size | Not measured | 16 px on both pages | n/a | Not comparable |
| Smallest click target | Not measured | 20 px for the inline links Read 24.9.26 and What Spring Boot brings, 22 px for the file links; padded to 24 px after the review | n/a | Fixed |

## Issues

### Critical

None found.

### Major

1. No product narrowing on the release page, D-111; the reference has it.
2. The Spring Boot page opens 259 risk rows on arrival and is 11,909 pixels tall; the first paint in the Playwright browser took long enough for a screenshot to come back blank.
3. Advisory ids in the update sentence break at their hyphen.

### Minor

1. "closes 0 of these 1" reads badly.
2. The supply status is said twice, in the chip and in the sentence.
3. The heading says "Release 24.9.21" and the card's first line says "24.9.21 in 24.9" right under it.
4. Two links back to the explorer, the breadcrumb and All series.
5. The lookup's helper text wraps in a 24rem field.
6. Fixed during the review: the inline links of the advice, the file links and the release links of the not measured answer were under 24 pixels.
7. Fixed during the review: the Spring Boot advice said "nothing newer within this major" on 3.4.13 and 3.2.12 and, one line later, named 3.5.16 as a later minor of the same major. It now says no newer patch of this line. The release page said "nothing newer is published" on 3.5.16 where 3.5.18 is sold; it now says "openly".

## Deliberate improvements

- A version under the wrong line in the address is answered under its own and the address corrected, instead of failing to load.
- A version newer than a line's newest measured release is not told to update backwards.
- Every Spring Boot release names its supply status, open included, D-115; the reference is silent on open lines.
- A GHSA id links to GitHub's database, not to an NVD page that does not exist, D-113.

## Verdict

Pass with the major issues above recorded for a person, P-16. Every acceptance criterion of the feature holds on the live data and on the fixtures.

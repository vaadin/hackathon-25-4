# What waits for a person

The implementation pass runs unattended and never stops for something only a person can do. Whatever needs one is moved here and done at the end, in this order. An epic that meets such a thing does the rest of its work, adds a row, and goes on.

## The order at the end

1. Epics 02 to 13, unattended, one after another. Epic 12 and epic 13 no longer wait for the interactive polish.
2. Everything in the table below, with Manolo.
3. The decisions in `DECISIONS.md`, one by one: confirm or revert.
4. The issue drafts in `specs/issues/`, one by one: accept, edit or drop, and only then `post.sh --confirm`.

This reorders the schedule in `specs/00-overview.md`, which puts the interactive polish between epics 11 and 12. The overview is left as written. If the interactive polish changes screens that epic 12 instruments or epic 13 documents, those two are revisited after it.

## The list

| # | From | What needs a person | Why it cannot be done unattended | What was done instead |
| --- | --- | --- | --- | --- |
| P-01 | Overview, the schedule | Polish, step one: the interactive pass with the dev loop running and a browser open, where behaviour and appearance are argued out view by view | It is a conversation by definition | Skipped for now. Epics 12 and 13 run without it |
| P-02 | Epic 01 | Review the visual findings in `specs/reviews/01-foundation/REVIEW.md` and say which to fix: the 22px switch, no way back to the system color scheme, no favicon, no help line on the sign in form | Each is a matter of taste or of scope | Recorded, not changed |
| P-03 | All epics | The visual reviews compared against the live reference pages only as far as their sign in gate. A person with an account can open the reference past the gate | No credentials, and the gate is not to be circumvented | Each review says what it could not compare |
| P-04 | Epic 02 | Say whether the wording written without a source is right: the unreachable sentence, `data.unreachable`, and the five download link labels, `export.*`, in the bundle | The source pages never had an outage state and name the files only by file name, so both are a matter of taste | Written as best guesses, D-28, and criticised in `specs/reviews/02-data-client/REVIEW.md` |
| P-05 | Epic 03 | Say whether the wording written without a source is right: the version tab's help dialog, `lookup.help.version.*`, and the note for ids that do not parse, `lookup.ids.unrecognised` | The reference has no help on the version tab and no wording for a partly parsed list | Written as best guesses, D-43 and D-48 |
| P-06 | Epic 03 | Review the findings in `specs/reviews/03-home-and-lookup/REVIEW.md` and say which to fix: verdict chips that look alike for a risk and a dismissal, the page heading that repeats the trail above the cards, the disabled upload that does not look disabled, the "Extended" mark on eight of nine cards | Each is a matter of design | Recorded, not changed |
| P-07 | Epic 04 | Review the findings in `specs/reviews/04-answer-by-version/REVIEW.md` and say which to fix: verdict prose drawn whole, which pushes the red card's actions below the fold on 24.7.19, the empty right side at 1920 wide, text links under 24 pixels, badges that all look alike | Shortening a verdict means choosing the sentence a reader sees first, which is a design decision, and the others are matters of taste | Recorded, not changed |
| P-08 | Epic 04 | Say whether the wording written without a source is right: the Notify me dialog, `version.notify.*`, the headline of a version red only through unjudged findings, `version.card.triage.title.*`, and the sentence for a version below every followed line, `version.card.unmeasured.oldest` | The reference has no wording for these, or wording about a static site this application is not | Written as best guesses, D-53, D-55, D-57 |
| P-09 | Epic 05 | Say whether the wording written without a source is right: the note above the cards for a version we never measured, `cve.unmeasured.*`, the card for a GHSA id we do not track, `cve.title.unknown.ghsa` and `cve.why.unknown.ghsa`, the Spring Boot sentence for a risk in `theirs_risk`, `cve.why.boot.theirs`, the copy notices, `cve.copy.*`, the build only sentence, `cve.why.build`, the no longer reported words, `cve.assessment.notCurrent` and `verdict.short.not_current`, the table words of the dependency in the box, `cve.box.*`, and `cve.evidence.askedAs` | The source page has no wording for these, or wording that says something false for the case, D-62 and D-65 | Written as best guesses |
| P-10 | Epic 05 | Review the findings in `specs/reviews/05-answer-by-cve/REVIEW.md` and say which to fix: the empty right side at 1920 wide, nine line cards ahead of the details, "Affects you" as Vaadin's assessment beside a fixed headline, two dates on one screen, and whether the copied answer should carry a link to the page | Each is a matter of design, and a link in the copied text is a fact the card does not show, which the epic forbids without a person's say | Recorded, not changed |

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

# The issue pipeline

A finding starts as a row in `specs/FEEDBACK-25.4.md` or `specs/FEEDBACK-PLATFORM.md`. It becomes an issue only through this directory, because a row is a suspicion and a project is a fact: in the 25.3 hackathon five findings died the moment somebody built their reproducer, and every one of those would have wasted a maintainer's morning.

## What a finding needs before it is posted

1. **A draft**, `NN-short-name.md`, starting with two header lines, `REPO:` and `TITLE:`, then the sections Description, Why it matters, and Expected, ending with the line "Found on 25.4.0-beta2". Less prose than the feedback row and one thing the row does not always carry.
2. **The smallest thing that reproduces it**, a minimal project zipped into `projects/`, referenced from the draft by name and commit. A finding whose reproduction is "this application, on this branch, on this screen" is not ready. A documentation draft reproduces by quoting the page and the behaviour, and says so instead.
3. **A search against existing issues** in the target repository, with the query written into the draft. Commenting on an existing issue beats opening a twin, and the draft then becomes `NNb-comment-on-<issue>.md`.
4. **A row in `TO-POST.md`**: repo, kind, whether it reproduces outside this application, which zip.

## Routing

| What | Repository |
| --- | --- |
| Server API, binder, signals, router, security | `vaadin/flow` |
| A component misbehaving in the browser | `vaadin/web-components` |
| A component's Java API, the AI family | `vaadin/flow-components` |
| Browserless testing | `vaadin/browserless-test` |
| Documented but absent, wrong or missing docs | `vaadin/docs`, naming the page |
| BOM, versions, or nobody can tell who owns it | `vaadin/platform` |

## Posting

`post.sh` reads `TO-POST.md`, dry run by default, `--confirm` to send, and refuses a dirty working tree so what is posted is what is committed. After posting, the issue link goes at the end of the feedback row and the `TO-POST.md` row moves to `posted.txt`.

## Lessons carried from 25.3, so they are not relearned

- A layout box is not visibility, and a scripted click is not a click. Measure with `checkVisibility` and a human gesture before claiming a component is broken.
- A passing probe written after forming a theory tests the theory. Ask the capability question naively first.
- Never delete a fixed finding: mark it fixed, the workaround still has to be removed.
- Withdrawn findings keep their draft file with a one line epitaph, because the next person will suspect the same thing.

Implement every story of the epic named in `$ARGUMENTS`, unattended, start to finish.

Nobody is watching and nobody can answer. Never stop to ask. When a choice is needed, take the best guess, act on it, and record it.

## Before anything

1. Read `CLAUDE.md`, `NEXT.md`, `DECISIONS.md`, `specs/00-overview.md`.
2. Read `specs/user-stories/epic-<NN>-*.md`, the feature document it points at, and every numbered spec either of them names.
3. Read `specs/issues/README.md`. It is the procedure for findings.
4. Run `git status` and `./mvnw verify`. Start from a clean tree and a green gate. If either is not so, fix that first and say so in the report.

## For each story, in order

Follow `.claude/commands/implement-story.md`, all five phases, with the documents under `.claude/commands/story/`.

- Query the Vaadin MCP before writing against any 25.4 API, with `vaadin_version: "25.4"` and `ui_language: "java"`. Call `get_java_symbol` before concluding that an API is missing.
- Read the documentation page for a behaviour before calling it a finding.
- The behaviour reference is `docs/requirements.md` section 6 of `vaadin/cve-status` and the live pages. Read them with `gh` or the browser. Never copy code from that repository.
- Start the application with `VAADIN_LAUNCH_BROWSER=false ./mvnw` in the background and stop it when the verification is done. Never leave anything listening on 8080 or 8081. Never run the dev loop daemon and Maven at the same time.
- `./mvnw verify -Pit` runs the application in production mode. Run it whenever the epic adds or touches an `*IT`.
- A story is done only when its gate is green. A red gate is fixed, not reported and left.

## Processes and the browser

- Stop only what you started, and only by a port it listens on: `kill $(lsof -nP -tiTCP:8080 -sTCP:LISTEN)`. Never `lsof -ti :8080 | xargs kill`: without `-sTCP:LISTEN` that also matches the browsers connected to the port, including the person's own.
- After a sign in, Chrome shows its save password bubble, and the first real click on the page closes the bubble instead of reaching the page. If real clicks seem not to arrive in Playwright, that is the cause: click once on an empty area, or open a fresh browser context, then retry. Verify with real clicks; a scripted `element.click()` proves less, and a report must say which one it used.

## The model key

This machine has `OPENAI_API_KEY` in the environment, and the `ai` profile of `pom.xml` activates by itself whenever it is set. The gate must prove `CLAUDE.md` rule 8, no key, so run both final gates with the key removed: `env -u OPENAI_API_KEY -u OPENAI_TOKEN ./mvnw clean verify`, and the same for `-Pit`. Start the application for UI verification the same way unless the story is about the live model.

## Save as you go

A session can be cut at any moment and whatever lives only in your head is lost. Write each decision row, feedback row and `NEEDS-A-PERSON.md` row when it happens, not at the end. After each story, bring `NEXT.md` up to date with what now exists and the traps just learned. Keep `git status` free of tool output such as `.playwright-mcp/`.

## Decisions

Every choice a person would have been asked about goes into `DECISIONS.md` as the next numbered row: what, why, the alternative, and how to revert it. Missing data, ambiguous wording in a spec, a trade between two readings, a scope cut, a dependency added, a spec sentence that could not be met as written. Never renumber and never delete a row.

When the spec cannot be met as written, do the nearest honest thing, record the decision, and do not edit the spec to match.

## What only a person can do

Some things cannot be done unattended: a credential or a key nobody gave, a licence, a look at something behind a sign in, a judgment of taste, an interactive session. Do not stop for them and do not fake them. Do everything around it, add the next numbered row to `NEEDS-A-PERSON.md` saying what is needed, why, and what was done instead, and go on. Those are done at the end, with a person.

## Findings

Each thing that could not be done the obvious way gets its row in `specs/FEEDBACK-25.4.md` or `specs/FEEDBACK-PLATFORM.md`, and then goes through `specs/issues/README.md` before the story is reported done: a duplicate search with `gh`, a draft, and a minimal reproducer that is built and run in a scratch directory outside the repository and zipped as `pom.xml src/main/java src/main/resources`. The Reproduce section states what the run printed. A finding the reproducer contradicts is corrected or withdrawn, and the ledger tables in that README say so.

**Never post.** Do not run `post.sh --confirm`, do not create or comment on any GitHub issue or pull request, do not send anything to Slack. The drafts are reviewed and accepted one by one by a person, later.

## Writing

Everything written is in English: code, documents, commit messages. In prose never use a hyphen or a dash as punctuation, use a comma, a colon or a full stop. In Markdown a paragraph is one line. No document and no commit mentions an AI or an assistant.

## When every story is done

1. `./mvnw clean verify`, and `./mvnw clean verify -Pit` if the epic has an `*IT`. Both green.
2. Tick the checkboxes that are genuinely true, in the epic file and the feature document. `SpecConsistencyTest` makes a fully ticked story a claim the build checks.
3. Rewrite `NEXT.md`: where the work stopped, what is next, what exists now, the traps learned. Keep the traps of earlier epics that still apply.
4. Two commits, never pushed:
   - `Epic NN: <name>`, with the implementation, the tests, and the ticked checkboxes.
   - `Record epic NN: <what>`, with `NEXT.md`, `DECISIONS.md`, the reviews, the feedback files and `specs/issues/`.

   A title, and at most two sentences saying why. No attribution line, no co author line.
5. Confirm `git status` is clean and nothing listens on 8080 or 8081.

## The report

Say, plainly and without rounding in your own favour: what was built per story, the exact test counts of the final gates, every decision row added, every finding and what became of it, and everything left undone or not verified. If something failed and could not be fixed, say that first.

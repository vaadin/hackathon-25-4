# The issue pipeline

A finding starts as a row in `specs/FEEDBACK-25.4.md` or `specs/FEEDBACK-PLATFORM.md`. It becomes an issue only through this directory, because a row is a suspicion and a project is a fact: in the 25.3 hackathon ten findings died the moment somebody built their reproducer, and every one of those would have wasted a maintainer's morning.

Everything here is a file first. Nothing is posted by an agent: the drafts are written, committed and pushed, and `post.sh --confirm` is run by a person.

## Where the issues go

Each issue is opened in the repository that owns the module, and every one of them is also listed in the Platform in Test tracker for the release, which is where somebody looks to see what this release's testing produced.

| Release | Tracker |
| --- | --- |
| 25.4 | <https://github.com/vaadin/platform/issues/9642> |
| 25.3, for reference | <https://github.com/vaadin/platform/issues/9204> |

In the tracker the hackathon issues are a checklist under a `## Hackathon` heading, one `- [ ] <url>` line each. `post.sh` appends the line, and adds the heading the first time.

## When a finding is drafted

Before the story that found it is reported done. A row with no draft at that point needs a line in the Not ready table below saying what it lacks. Drafting late is how findings get lost and how the details that made them reproducible are forgotten.

## The draft

One file per finding, `NN-short-name.md`, copied from `TEMPLATE.md`. Three header lines that are instructions and are never posted, then the body:

```
REPO: vaadin/flow
TITLE: What the issue is called
---
```

The title is written bare. `post.sh` posts it as `Hackathon 25.4: <title>`, which is how the teams tell these apart from customer reports.

The body is as short as it can be and still be acted on by somebody who has never seen this application. Sections, in this order:

| Section | What it holds |
| --- | --- |
| `### Description` | What happens, with the smallest snippet or the exact message. No history of how it was found |
| `### Why it matters` | What it costs somebody: a silent failure, a misleading message, a workaround everyone will rewrite |
| `### Expected` | What should happen instead. Options in order of usefulness when there are several |
| `### Workaround` | What we did. Omitted when there is none |
| `### Reproduce` | The detailed one. Numbered steps, or a table of observed values, with the route to open, what to press, and the exact command or snippet that measures the result. A reader must be able to tell a reproduction from a non reproduction without judgment |
| `### Getting the project` | The zip link, its size, "sources only", and the one command that runs it |
| Last line | `Found on 25.4.0-beta2.` Add the version of any separately versioned module involved |

A documentation draft has no project: its Reproduce section quotes the page and the sentence that is missing or wrong, and Getting the project is dropped.

A draft says what it needs when the reader may not have it: a commercial licence, an API key, Chrome.

## The reproducer

The smallest project that shows the behaviour, zipped into `projects/`. A finding whose reproduction is "this application, on this branch, on this screen" is not ready.

- A `pom.xml`, an `Application`, and one or two classes that do nothing else. Package `com.example`.
- Sources only: no `target`, no `node_modules`, no wrapper, no generated frontend files. Between three and ten kilobytes.
- Its own port, `81NN` for draft `NN`, in `application.properties`, so two reproducers run side by side.
- Runs with `mvn spring-boot:run`, or `mvn test` when the reproduction is a test. A test is better whenever the behaviour can be asserted, because it prints the numbers.
- When the claim is a comparison, two projects one line apart, or one project with two routes.
- One zip can serve several drafts. Name it for the subject, not for one draft.
- Built and run in a scratch directory outside the repository, then zipped: `(cd <scratch>/NN-name && zip -r -X <repo>/specs/issues/projects/NN-name.zip pom.xml src/main/java src/main/resources)`, adding `src/test/java` when the reproduction is a test. Never zip `src` whole: running the project makes Vaadin write a development bundle and generated frontend files into it, five megabytes of them. `.gitignore` here refuses an unzipped copy.

The draft links its zip by full commit SHA, written as the placeholder `COMMIT_SHA`:

```
https://github.com/vaadin/hackathon-25-4/raw/COMMIT_SHA/specs/issues/projects/NN-name.zip
```

A SHA rather than a branch, so the link keeps working when the branch moves, and a zip rather than a directory, because a reader wants one file and one command. `post.sh` substitutes the SHA of `HEAD` when it posts. A GitHub issue cannot carry an attachment through the API, which is why the zip lives in this repository. A screenshot a draft refers to goes beside the zips as a `.png` for the same reason.

**Running the reproducer is part of writing the draft.** The Reproduce section states what the run printed, not what the row claimed. If the reproducer disagrees with the row, the row is wrong: correct it and record the finding under Corrected or Withdrawn below.

## The duplicate search

Before a draft is ready, search the target repository, and the previous hackathon's issues, for the same thing. The query goes into the `TO-POST.md` row. If the issue exists, the draft becomes a comment: name the file `NNb-comment-on-<repo>-<number>.md`, with `REPO:` and a `COMMENT-ON: <number>` line instead of `TITLE:`. Saying "still on 25.4.0-beta2", with a fresh reproducer, on an open issue is worth more than a twin.

```
gh search issues --repo vaadin/flow "<words>" --limit 10
gh search issues --owner vaadin "Hackathon 25.3" "<words>" --limit 10
```

## Routing

The destination follows the artefact, not the symptom.

| What | Repository |
| --- | --- |
| Server API, binder, signals, router, security, the dev loop | `vaadin/flow` |
| A component misbehaving in the browser, theme CSS | `vaadin/web-components` |
| A component's Java API, and the whole AI family, `vaadin-ai-*` | `vaadin/flow-components` |
| Browserless testing | `vaadin/browserless-test` |
| TestBench | `vaadin/testbench` |
| Documented but absent, wrong or missing docs | `vaadin/docs`, naming the page |
| Copilot and the dev tools window | `vaadin/copilot-internal` |
| BOM, versions, or nobody can tell who owns it | `vaadin/platform` |

In 25.3 ten issues were transferred by triage after posting. Six were AI findings filed under `flow` that belong to `flow-components`.

## Posting

`TO-POST.md` is the posting list. `post.sh` reads it.

```
./specs/issues/post.sh            # dry run: prints what it would do and sends nothing
./specs/issues/post.sh --confirm  # creates the issues and the comments
```

It refuses to send while the working tree is dirty, while a zip a draft links is missing from `HEAD`, or while `HEAD` is on no remote branch. Any of the three turns the one useful link in the issue into a 404. For each row whose status is `ready` it strips the header, substitutes the SHA, prefixes the title, creates the issue or the comment, appends the issue to the tracker, and writes the link to `posted.txt`.

After posting: the link goes at the end of the finding's feedback row and into the `TO-POST.md` row, whose status becomes `posted`. A finding with no link is a finding nobody outside this repository has seen.

## Every row accounted for

A feedback row ends in exactly one of these, and the tables below are the ledger.

### Ready or posted

See `TO-POST.md`.

### Not ready

Real here, not yet reduced. Each says what it needs.

| Finding | What it needs |
| --- | --- |
| Aura has no inline tertiary button, `FEEDBACK-PLATFORM.md` | One view with a paragraph ending in a `TERTIARY` button, screenshot on Aura and on Lumo with `LUMO_TERTIARY_INLINE`, and a search for an existing web components issue |
| The production build fails in `eclipse-temurin:21-jdk` because the Node.js that Vaadin downloads needs `libatomic.so.1`, `FEEDBACK-25.4.md` | A Dockerfile over a starter project, run once with and once without `libatomic1`, and the Node.js version where it began |
| A `SideNavItem` with children opens itself whenever it is the current page, `FEEDBACK-PLATFORM.md` | One view with a parent item and a child, the server closing the parent after navigation, and the same on 24.9 to date it |
| Aura pads the drawer of `vaadin-app-layout` by a fraction of a pixel, 6.445, so a small `SvgIcon` inside blurs on a 1x screen, `FEEDBACK-25.4.md` | One app layout with a drawer holding a 32 pixel `SvgIcon`, its x read with `getBoundingClientRect`, and a zoomed screenshot on a 1x screen beside the same icon in a drawer padded 6 pixels |

### Needs no draft

Rows that are records rather than requests, each with the reason.

| Rows | Why no draft |
| --- | --- |
| `FEEDBACK-25.4.md`, CSP directives the components forced open, every row but `form-action` | A record of what the policy had to admit and why, kept because `specs/04-security.md` asks for it. Each loosened directive is what Vaadin's own strict mode also needs, or nothing was loosened, so there is nothing for a team to change |
| `FEEDBACK-25.4.md`, CSP directives, `form-action https://github.com` | The fact belongs on the `NativeForm` documentation page. vaadin/docs#6625 wrote that page and was merged on 2026-11-22 without mentioning `form-action`; a docs issue asking for one sentence there is for the person to decide |
| `FEEDBACK-25.4.md`, Data needs toward cve-status, all eight rows, four from the epics and four from the alignment with the prototype | Not Vaadin findings: fields the published JSON of vaadin/cve-status does not carry. They go to that project's pipeline as additive exports, `specs/02-data-source.md`, not through this directory |
| `REPORT.md`, two observations about the prototype of vaadin/cve-status: on a CycloneDX report it names components by `bom-ref` and counts the file's own findings twice, 55/2/21 against the application's 52/1/18, `specs/reviews/prototype/scan.md`, D-201; its per row "not assessed" sentence leaves out the findings that arrive only through frontend packages, D-224 | Not Vaadin findings and not data needs: defects of the JavaScript prototype the application rebuilds. They belong to vaadin/cve-status, and are recorded for the person, who decides whether that project hears of them |

### Withdrawn

Died when somebody tried to reproduce it. The draft file is kept with a one line epitaph, because the next person will suspect the same thing.

| Finding | What happened |
| --- | --- |
| `19` the Grid context menu example uses the deprecated handler | Fixed before posting: vaadin/docs#6587, merged on 2026-11-22, moved the example, the page text and the upgrade guide to the provider |
| `08` the Content Security Policy page does not say how the strict csp mode behaves | Covered by two open pull requests found at review time, vaadin/docs#6495 and #6687. The beta2 measurement was posted on #6495 instead, `08b`. Lesson: search open pull requests of the docs repository, not only its issues |
| `02` the not found view renders outside the automatic layout | Documented, with the fix we had called a workaround: error views are not routes, add `@ParentLayout`. The documentation was read after the row was written |
| `04` a signed in user without the role sees the not found view | Documented as a deliberate precaution, with a custom access denied view offered for applications that want to say so |
| `06` `VaadinExpiredSessionStrategy` only acts under concurrency control | Stated twice on the page that introduces it |
| A real click on the Sign out button does nothing, while a scripted click signs out | Not the button and not Vaadin. After a form sign in Chrome shows its save password bubble, and the first real click on the page closes the bubble instead of reaching the page: the first click on a bare heading is lost the same way, the second one arrives. Chrome's password manager is switched off in `CspIT` and the click works |
| `05` an unknown address lands on the login page | Not withdrawn as a fact, withdrawn as an issue: it is the open question at the end of the maintainer's answer on flow#25576, so it went there as a comment |
| `07` no way to apply a remembered color scheme before first paint | The same: flow#26391 and flow#26390 already ask for it, so the measurement went there as a comment |
| `NativeForm` on no documentation page, epic 10 | Not drafted: vaadin/docs#6625, "docs: document the NativeForm component", was the pull request doing exactly this, found by `gh search issues --repo vaadin/docs "NativeForm"`, and was merged on 2026-11-22: the component has its page under HTML elements |

### Corrected by its own reproduction

Still real, and saying something different from what the row said.

| Draft | What changed |
| --- | --- |
| `03b-comment-on-flow-25576.md` | The row said an imported sheet is blocked while the declared one is served. The reproducer says the declared one is blocked too, for signed in users as well: ours was served only because it is named `styles.css`, the one name on the public list |
| `10-bootstrap-page-without-security-headers.md` | First seen as "two `CspIT` assertions failed once and not again", then as "the headers are lost in production mode". The reproducer says both modes: the full `-Pit` run is the only one in production mode, and in development what carried the headers was the "dev mode pending" page of the first seconds |
| `20b-comment-on-browserless-test-29.md` | The row said a browserless test cannot reach the component a Grid item details renderer builds. The reproducer says the component is found by a query once the details are open; what is missing is `clickRow` opening them, which browserless-test#29 already asks |
| `07b-comment-on-flow-26391.md` | The row said `ExtendedClientDetails.getColorScheme()` is empty until a scheme is set. It reports what the application set, `LIGHT_DARK` here. Never the system preference, which was the point |

## Lessons carried from 25.3, so they are not relearned

- A layout box is not visibility. Measure with `checkVisibility`.
- A scripted click is not a click, in both directions. Two 25.3 findings died because a synthesised gesture did nothing where a real one worked. The reverse also happens: a real click that does nothing where a scripted one works. Establish which gesture a person performs, and reproduce with that one, before claiming a component is broken.
- A passing probe written after forming a theory tests the theory. Ask the capability question naively first.
- A compile error is about the line it names, not about the API you suspect. "Documented and does not exist" was wrong once and cost a forty line adapter.
- Never delete a fixed finding: mark it fixed, the workaround still has to be removed.
- Several rows with one root make one draft. One row with two causes makes two.
- A browser has user interface of its own. After a sign in Chrome's save password bubble takes the first click. Switch the password manager off in any browser test that signs in.
- A test that passes against one instance and fails against another is telling you the instances differ. `./mvnw verify -Pit` runs the application in production mode, `./mvnw` does not.
- Read the documentation page for the behaviour before writing the row, not when drafting. Three of the first nine 25.4 rows were documented behaviour, and one of them documented our workaround as the intended way.

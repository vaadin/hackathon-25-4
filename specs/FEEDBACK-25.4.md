# Feedback for the Vaadin teams: 25.4

Everything found while building this application on 25.4.0-beta2 that a Vaadin team could act on: an API that is missing, one that is awkward, one that is documented but absent, and every place where we had to write a workaround.

This file is part of the specifications on purpose. If this application is ever rebuilt from these specifications, whoever does it should hit none of this by surprise, and should check whether each entry is still true.

**How to add an entry.** One row per finding, in the table that matches its kind. Say what was expected, what happened, what we did instead, and where in this repository the workaround lives. If it is fixed later, do not delete the row: mark it fixed and in which version, because the code that works around it still has to be cleaned up.

**What happens to these.** Nothing goes straight from here to an issue tracker. The pipeline is `specs/issues/`: a draft per finding, a minimal reproducer per draft, a check against existing issues, and the posting script with its dry run. A row is a suspicion, a project is a fact. When an issue is opened, its link goes at the end of the row. A finding with no link is a finding nobody outside this repository has seen.

Routing, when drafting: an API on the server, a binder, a signal, the router goes to `vaadin/flow`. How a component behaves in the browser goes to `vaadin/web-components`. A Java component API and the AI family go to `vaadin/flow-components`. Browserless testing goes to `vaadin/browserless-test`. Something documented that does not exist goes to `vaadin/docs`, naming the page. A version or BOM problem, or a case where nobody can tell which module owns it, goes to `vaadin/platform`.

## Missing or absent APIs

| Finding | Expected | What happened | What we did instead | Where the workaround lives |
| --- | --- | --- | --- | --- |

## Awkward APIs

| Finding | Expected | What happened | What we did instead | Where the workaround lives |
| --- | --- | --- | --- | --- |

## Bugs routed around

| Finding | Expected | What happened | What we did instead | Where the workaround lives |
| --- | --- | --- | --- | --- |

## Documentation gaps

| Finding | Expected | What happened | What we did instead | Where the workaround lives |
| --- | --- | --- | --- | --- |

## CSP directives the components forced open

Per `specs/04-security.md`, every loosened directive is a fact the teams want, recorded the moment it happens.

| Directive | Forced by | Why |
| --- | --- | --- |

## Data needs toward cve-status

Not Vaadin findings: fields a screen needed that the published JSON does not carry, per `specs/02-data-source.md`. Each becomes an additive export in the cve-status pipeline, never a computation here.

| Screen | Field | Why nothing present covers it |
| --- | --- | --- |
| Explorer front door | The short product codes, `module_codes`, in `data/front.json` | The off only address grammar wants the written down codes, which today exist only inside the 66 MB `data.json` no session can fetch. Until the export lands the explorer addresses products by name, the source spec's own 6.2.6 fallback |

## The SDD starter

Observations about the start.vaadin.com spec driven starter and its templates, gathered while assembling the scaffold. Same discipline: a row, then a reproducer or a stated reason it needs none.

| Finding | Expected | What happened | What we did instead | Where |
| --- | --- | --- | --- | --- |
| The SDD framework's `architecture.md` describes a Hilla React split and JUnit 5 while the v25.4 walking skeleton it ships over is Flow only on JUnit 6 era tooling | The template matches the skeleton it lands on | Two documents disagree about the stack of a freshly generated project | Replaced the framework with this suite, per `specs/09-tooling.md` | Needs re checking against a wizard generated zip before it becomes a draft, because our assembly bypassed the wizard's own replacements |

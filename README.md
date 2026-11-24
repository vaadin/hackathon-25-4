# Security Status 25.4

A rebuild of Vaadin's two security status pages on Vaadin 25.4, built specification first, for the 25.4 hackathon.

The originals live in vaadin/cve-status: two static JavaScript pages on GitHub Pages that compute everything in the reader's browser from the published JSON. One answers customers ("is my Vaadin version safe, and what do I do?"), the other is the security team's findings explorer behind a client side gate. This application answers the same questions from the same JSON as one Vaadin Flow application, with a real sign in, real addresses, server side parsing of scanner reports, and two things the originals do not have: an AI assistant for the team and a diagnostics board.

Nothing is copied from the JavaScript code. It was read for behaviour only.

## How it was built

1. **Specifications first.** One prompt in plan mode produced `specs/`: an overview, the cross cutting documents and thirteen epics with acceptance criteria and named test cases. The brief is `specs/PROMPT.md`. The specs carried one standing order: write down every bug, missing API and workaround as you hit it.
2. **Thirteen epics, unattended.** One agent session per epic, from 20 to 22 November, each closing with its gate green and every decision it took on its own written to `DECISIONS.md`, with how to revert it.
3. **Aligned with the live prototype.** Seven more sessions compared every screen with the live pages on the same data and moved the app to the prototype's layout and wording.
4. **Polished by hand.** With the dev loop running, view by view in the browser and on a phone, through `DECISIONS.md` and `NEEDS-A-PERSON.md` together with the agent.
5. **Findings to issues.** Every row in `specs/FEEDBACK-25.4.md` was reduced to a minimal project, searched for duplicates and posted only after review. The pipeline is `specs/issues/README.md`.

## Running it

```bash
./mvnw                       # the application on 8080, reading the published JSON
./mvnw verify                # unit and browserless tests: no licence, no key, no network
./mvnw verify -Pit           # adds the browser tests
OPENAI_API_KEY=... ./mvnw    # the assistant with a live model, the ai profile turns on by itself
```

Sign in with `team` / `team` for the team's pages or `guest` / `guest` for a user without the role. They are demo accounts and they are in the source on purpose. Without a key the assistant replays recorded answers in development and is not offered in production. `DEMO.md` is the walk through.

## What to look at

| Route | What it shows |
| --- | --- |
| `/` | The customer page: supported versions and the lookup by version, by CVE or by scanner report |
| `/version/24.9.21` | The answer for one version: the verdict card, the upgrade, Spring Boot findings, details |
| `/cve/CVE-2026-45292/24.9.21` | The answer for one advisory on one version, with a copyable answer |
| `/scan` | A scanner report (CycloneDX, Dependency Check, OSV Scanner, bomber) triaged on the server, with CSV and PDF |
| `/advisories` | The advisory catalogue with verdict filters and a per row context menu |
| `/findings` | Team: the findings explorer, series by series, filtered by product |
| `/findings/workbench` | Team: the verdict queue, the form, drafts shared by the team, the handover to GitHub |
| `/diagnostics` | Team: navigation, request and session counts from the service event bus, as a board |
| The chat icon | Team: the assistant, with tools over the same data and a photo from the phone's camera |

## What 25.4 it exercises

Signals end to end (`SharedValueSignal`, effects bound to a component's life, `bindVisible`, `bindChildren`), the AI orchestrator with tools, Grid AI and Form AI, `UploadCapture` for the phone camera, the modular upload (`UploadManager`, `UploadButton`, `UploadDropZone`), the `Download` API and the PDF Viewer, `GridContextMenu.setDynamicContentProvider`, FormLayout side labels, `NativeForm`, the navigation and request events on the service event bus, declared JavaScript with `@JsExpression`, the Dashboard, SideNav and Breadcrumbs, Aura with light and dark, browserless tests on JUnit 6, and the dev loop CLI. On Spring Boot 4 and Java 21.

## Two UIs, one removable

The code is split so the team's half can be deleted if it is not wanted:

```
data/  security/            shared: the Pages client and cache, Spring Security, the CSP
customer/  ui/customer/     the customer's rules and views
team/      ui/team/         the team's rules and views: explorer, workbench, assistant, diagnostics
ui/common/                  what both UIs share: app bar, login, theme, navigation helpers
```

Delete `team/`, `ui/team/` and `styles/team/` and the application still builds and serves the customer pages. `TeamIsRemovableTest` keeps it that way, and `DEVELOPMENT.md` has the steps.

## The issues this build opened

Fourteen issues and six comments on existing ones, each issue with a reproducer project zipped under `specs/issues/projects/`. They carry the `Hackathon 25.4:` prefix and are listed in the platform tracker, https://github.com/vaadin/platform/issues/9642.

**vaadin/flow**

- [#27159](https://github.com/vaadin/flow/issues/27159) Since 25.0 the bootstrap page is served without the headers Spring Security writes
- [#27160](https://github.com/vaadin/flow/issues/27160) A shared signal changed by another thread before a component is attached never reaches its bindings
- [#27172](https://github.com/vaadin/flow/issues/27172) QueryParameters could keep the order the parameters were given in
- [#27173](https://github.com/vaadin/flow/issues/27173) The error callback of the Clipboard API takes a type from an internal package
- [#27187](https://github.com/vaadin/flow/issues/27187) A signal effect runs without the session lock when a RequestEndedEvent listener changes a shared signal

**vaadin/flow-components**

- [#10752](https://github.com/vaadin/flow-components/issues/10752) Button refuses bindEnabled beside disable on click, also in the new UNTIL_RESPONSE mode
- [#10753](https://github.com/vaadin/flow-components/issues/10753) GridAIController cannot narrow a grid the application already has
- [#10754](https://github.com/vaadin/flow-components/issues/10754) A failed AI turn writes a fixed English sentence the application cannot replace
- [#10755](https://github.com/vaadin/flow-components/issues/10755) Breadcrumbs in ROUTER mode cannot show an item that is not a route of the application
- [#10779](https://github.com/vaadin/flow-components/issues/10779) ComboBox.bindValue before setItems drops the bound value, with only a log line

**vaadin/browserless-test**

- [#383](https://github.com/vaadin/browserless-test/issues/383) A file served by Download.onClick cannot be fetched from a test, as an Anchor's can
- [#384](https://github.com/vaadin/browserless-test/issues/384) GridContextMenuTester cannot open a menu that uses the 25.4 dynamic content provider

**vaadin/docs**

- [#6739](https://github.com/vaadin/docs/issues/6739) The @Uses note on the production build page covers reflection only, and an injected component is left out the same way

**vaadin/spec-driven-development-demo**

- [#39](https://github.com/vaadin/spec-driven-development-demo/issues/39) architecture.md describes React views, Spring Security and JUnit 5 that the generated project does not have

**Comments on existing issues**, where 25.4 still shows the behaviour or adds a case: [flow#25576](https://github.com/vaadin/flow/issues/25576#issuecomment-7094873357), [flow#26338](https://github.com/vaadin/flow/issues/26338#issuecomment-7094821340), [flow#26391](https://github.com/vaadin/flow/issues/26391#issuecomment-7094873718), [flow#5473](https://github.com/vaadin/flow/issues/5473#issuecomment-7088108706), [browserless-test#29](https://github.com/vaadin/browserless-test/issues/29#issuecomment-7094696694), [docs#6495](https://github.com/vaadin/docs/pull/6495#issuecomment-7082927999).

Five more findings died on the way: three were already documented, two were already being fixed in open documentation pull requests. They are kept in `specs/issues/README.md` with the reason.

## Where to read more

| File | Contents |
| --- | --- |
| `specs/` | The contract, written before the code. Start at `specs/00-overview.md` |
| `specs/FEEDBACK-25.4.md`, `specs/FEEDBACK-PLATFORM.md` | Every missing API, bug and workaround, in 25.4 and older |
| `DECISIONS.md` | 256 decisions, each with why and how to revert it |
| `REPORT.md` | How the build went, the timeline, the comparison with the originals, what is not done |
| `DEMO.md` | The demo, step by step |
| `DEVELOPMENT.md` | Development notes, the licence, removing the team |

## Hackathon

- Platform version: 25.4.0-beta2
- Slack: #hackathon-25-4
- Documentation: https://vaadin.com/docs/next/

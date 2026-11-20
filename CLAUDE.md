# Security Status 25.4

A rebuild of the two cve-status JavaScript apps as one Vaadin 25.4 application, built specification first. Read `specs/` before writing code. The specifications are the source of truth, not this file and not the existing code.

Start at `NEXT.md` when it exists: where the work stopped, what to do next, and the traps that are already known.

## Stack

| Thing | Version |
| --- | --- |
| Vaadin | 25.4.0-beta2, prereleases repository |
| Spring Boot | 4.2.x |
| Java | 21 |
| Database | H2, only for what the workbench writes. Everything read comes from GitHub Pages JSON |
| Tests | JUnit, `browserless-test-spring`, TestBench only where a browser is required |

## Where things are

```
specs/                 the source of truth, start at 00-overview.md
  features/NN-*.md     behaviour, acceptance criteria and the natural language test cases
  user-stories/        epics with US-N.M stories and tasks
src/main/java/com/vaadin/securitystatus/
  base/                shell, signals helpers, security, error handling
  data/                the Pages client, caching, read models
  status/ explorer/ workbench/ assistant/ diagnostics/
src/main/resources/META-INF/resources/   all CSS and images
```

## Hard rules

1. Never copy code from the JavaScript apps in vaadin/cve-status. Read them, and `docs/requirements.md` section 6 there, for behaviour only.
2. Signals first. Cross component state is a signal, not a listener chain.
3. Views never fetch. `data/` services are the only place that talks to the network, and they answer from the cache.
4. The page never invents a value. A field the data does not carry is omitted, never rendered empty and never filled with wording of ours. This rule comes from the source project and it is the one that matters most.
5. No user visible string literal in Java. Everything goes through the translation bundle.
6. CSS lives in `src/main/resources/META-INF/resources`, imported from one `styles.css`.
7. Subscriptions register in `Component.whenAttached` and release through the returned registration.
8. The default build must boot and pass tests with no commercial licence, no LLM key, and no network beyond the one fixture refresh task.

## Using the Vaadin MCP

Always query the docs server before writing against a 25.4 API. It is a beta and knowledge of 25.3 is not good enough.

- Endpoint: `https://mcp.vaadin.com/docs`
- Always pass `ui_language: "java"` and `vaadin_version: "25.4"`
- This is a Vaadin Flow project. It is not React or Hilla

## Commands

| Command | Result |
| --- | --- |
| `./mvnw` | Runs the application on 8080 |
| `./mvnw verify` | Unit plus browserless tests. This is the gate |
| `./mvnw verify -Pit` | Adds the TestBench integration tests |
| `./mvnw vaadin:install-dev-cli` | Installs the dev loop |
| `.vaadin/vaadin-dev status` `start` `apply` `restart` | The dev loop |

Never run the application through Maven and through the dev loop daemon at the same time.

## Definition of Done

A story is done when all of these hold:

1. `./mvnw verify` is green on the default profile, with no licence, no key and no network.
2. Every acceptance criterion of the story's feature document is checked.
3. Every test case in that document's table exists, in the tier the table names.
4. The application boots and the touched view works, verified with Playwright.
5. No new user visible string literal, and the bundle carries every key the code asks for.

## Record every workaround

Whenever you cannot do something the obvious way, write it down. The point of building a real application on a beta is to find these.

| What you hit | Where it goes |
| --- | --- |
| A 25.4 API that is missing, awkward, or documented but absent | `specs/FEEDBACK-25.4.md` |
| Something that behaves the same on 25.3 and earlier | `specs/FEEDBACK-PLATFORM.md` |
| Something that cannot be done at all | The same file, under missing APIs, saying what was impossible |
| A bug you routed around | The same file, naming the workaround and the file it lives in |

Say what you expected, what happened, what you did instead, and where the workaround lives. Never delete an entry when it gets fixed: mark it fixed, because the workaround still has to be removed.

Writing the row is half of it. Each finding becomes an issue in the repository that owns the thing, through the pipeline in `specs/issues/`: a draft per finding, a minimal reproducer per draft, and a posting script with a dry run. A row is a suspicion, a project is a fact.

## Preview APIs

`specs/CHANGELOG-RISK.md` lists every preview, experimental or beta API in use, with its flag and its fallback. Read it before using a 25.4 component that is not in the stable set, and add to it if you introduce another one.

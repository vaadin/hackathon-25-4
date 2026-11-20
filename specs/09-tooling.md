# 09 Tooling

## Versions

| Thing | Version | Why |
| --- | --- | --- |
| Vaadin | 25.4.0-beta2, pinned in one property | The hackathon target. The prereleases repository is in the pom |
| Spring Boot | 4.2.0-RC2, from the skeleton | What the v25.4 walking skeleton ships against |
| Java | 21 | The skeleton's floor |
| Spring AI | 2.0.0, BOM imported | Behind the `ai` profile only |

## Where the scaffold came from, and what was done to it

The project was assembled the way start.vaadin.com builds its spec driven starter, from the same two public repositories, rather than through the wizard: `vaadin/walking-skeleton` branch `v25.4-flow` is the application skeleton, and `vaadin/spec-driven-development-demo` contributed `spec/`, `DEVELOPMENT.md`, `README.md`, `.mcp.json` and the agent skills. The one thing the wizard adds that this repo skipped is the AI generated spec from a free text description, which this suite replaces. Assembling from source was chosen for reproducibility: the scaffold commit can be re derived, a wizard zip cannot.

What was then changed, each visible in its own commit:

- The package rename to `com.vaadin.securitystatus`, the same replacement the wizard performs.
- The `ai`, `it` and `production` Maven profiles and the Spring AI BOM, ported from the 25.3 hackathon pom.
- The generic SDD skills under `.claude/skills/` were removed, and the `spec/` framework directory is replaced by this suite. Two methodologies in one repository would send an unattended agent down the wrong one, and the 25.3 method is the validated one here. The framework remains in the scaffold commit for judging, and findings about it belong in `specs/FEEDBACK-25.4.md` like any other.

## Maven profiles

| Profile | Adds | Activated by |
| --- | --- | --- |
| none | The gate: unit and browserless tests, no licence, no key, no network | |
| `ai` | Spring AI's OpenAI starter behind `SpringAILLMProvider` | `OPENAI_API_KEY` in the environment, or `-Pai` |
| `it` | Starts the application and runs `*IT` with failsafe on 8081 | `-Pit` |
| `production` | The production frontend build, where the licence is enforced | `-Pproduction` |

The `ai` profile is key activated rather than activeByDefault because Maven turns every activeByDefault profile off the moment any other profile is named, so `-Pit` would silently take the assistant away. Learned in 25.3, kept.

## Feature flags

Preview APIs and their flags are the register in `specs/CHANGELOG-RISK.md`. Flags live in `src/main/resources/vaadin-featureflags.properties`, committed, so a fresh clone runs the same application.

## MCP servers

`.mcp.json` points at the Vaadin docs MCP, `https://mcp.vaadin.com/docs`, and Playwright. Every 25.4 API is queried before use with `ui_language: "java"` and `vaadin_version: "25.4"`, because beta knowledge is not in anyone's training data, and `get_new_apis` is consulted once per epic to catch arrivals the specification missed.

## The fixtures script

`scripts/refresh-fixtures.sh` fetches every endpoint named in `02-data-source.md` from the live site into `src/test/resources/pages/`, prints the `generated_at` it pinned, and is the only way those files change. It runs on demand, never in the build: the gate has no network.

## CI

`.github/workflows/ci.yml`, two jobs. `verify` is the gate, no secrets required, green on a fork. `live` runs after it with `-Pai` and the lifted exclusion when `OPENAI_API_KEY` and `TB_LICENSE` exist, with one rerun, because a live model failure is evidence rather than proof. The pro key is written to `~/.vaadin/proKey` from the `TB_LICENSE` secret in the shape the licence checker reads.

## Copilot and the dev loop

The dev loop is specified in `specs/10-dev-loop.md` and installed in epic 01, because the polish pass depends on it and discovering it broken during polish is too late.

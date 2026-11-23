REPO: vaadin/spec-driven-development-demo
TITLE: architecture.md describes React views, Spring Security and JUnit 5 that the generated project does not have
---
### Description

A project downloaded from start.vaadin.com with "Spec-driven development" ships `spec/architecture.md` unchanged from this repository, and its stack section does not describe the project it lands in:

| `spec/architecture.md` says | The generated project has |
| --- | --- |
| "client-side React for public views", Hilla React views with file based routing under `src/main/frontend/views/` | No Hilla in the dependency tree, no `src/main/frontend` |
| "Do not modify `pom.xml`, `vite.config.ts`, or `architecture.md` without asking" | No `vite.config.ts`, no `package.json` |
| "Testing: JUnit 5, ... Vitest for React views" | `browserless-test-junit6`, which brings JUnit Jupiter 6.1.3; nothing for Vitest |
| Package `com.example.specdriven/` | `com.example.Application` |
| "Spring Security with `VaadinSecurityConfigurer`", `LoginForm` at `/login` | No Spring Security dependency |

Two of the skills follow the same text: `new-use-case` asks whether each view is "public (React/Hilla) or admin (Vaadin Flow)", and `use-case-tests` prescribes Vitest and React Testing Library for the public ones.

### Why it matters

The file tells the agent it is the source of truth for the stack and not to be changed without asking. An agent that follows it either starts the first use case by adding Hilla, a frontend toolchain and Spring Security to a project that was generated as Flow only, or asks the user a question about React views the user never chose. A user who picked the starter to get a Flow application gets a specification for a different one.

### Expected

`spec/architecture.md` describes what the starter generates: Flow views, JUnit 6 through `browserless-test-junit6`, the package the project actually uses. React views and Spring Security, if they are meant as options, are named as additions that need a dependency, not as the current stack. The two skills ask the React question only when Hilla is on the classpath.

### Workaround

The SDD framework was replaced by our own specification suite, so nothing in our project reads this file any more.

### Reproduce

1. On <https://start.vaadin.com>, choose "Spec-driven development", "Vaadin", Vaadin 25.4 (pre), Java 21, Claude Code, the "Expense Management" example, and Download.
2. In the unzipped project, read `spec/architecture.md` sections 1, 2 and 4.
3. `find . -name vite.config.ts -o -name package.json -o -path ./src/main/frontend` prints nothing.
4. `./mvnw -q dependency:tree -DoutputFile=/dev/stdout | grep -iE 'hilla|security|junit-jupiter:'` prints only `org.junit.jupiter:junit-jupiter:jar:6.1.3:test` and `org.mockito:mockito-junit-jupiter:jar:5.24.0:test`.

The same `architecture.md`, byte for byte, came out of a second download with "Custom" and a free text description, where the AI drafts the use cases and the data model, and it is the file on `main` of this repository, whose own `pom.xml` has no Hilla either.

### Getting the project

[`24-sdd-architecture.zip`](https://github.com/vaadin/hackathon-25-4/raw/COMMIT_SHA/specs/issues/projects/24-sdd-architecture.zip), 41 KB, the project exactly as start.vaadin.com generated it on 2026-11-23, without its `.git` folder. Unzip it and:

```
./mvnw dependency:tree
```

Found on start.vaadin.com with Vaadin 25.4.0-beta2.

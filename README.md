# Security Status 25.4

The Vaadin 25.4 hackathon application on branch `manolo`: a from scratch rebuild of the two cve-status JavaScript apps, the customer Security status page and the internal findings explorer, as one Vaadin Flow application.

Built specification first. The specifications live in `specs/`, start at `specs/00-overview.md`, and the brief they were written against is `specs/PROMPT.md`. The agent workflow is `/implement-story US-<epic>.<story>`, defined in `.claude/commands/`.

## Running

```bash
./mvnw              # dev mode on 8080
./mvnw verify       # the gate: unit plus browserless tests, no licence, no key, no network
./mvnw verify -Pit  # adds the browser tier
```

The application reads its data from the JSON that vaadin/cve-status publishes on GitHub Pages, with a server side cache. There is no database of its own beyond what the verdict workbench drafts.

## The other half of the point

Everything missing, awkward or broken found in Vaadin 25.4 while building this goes to `specs/FEEDBACK-25.4.md` and from there, verified and reproduced, to an issue in the repository that owns it. See `specs/issues/README.md`.

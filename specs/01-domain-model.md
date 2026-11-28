# 01 Domain model

## The shape of this domain

This application owns almost no data. Everything it shows is a projection the vaadin/cve-status pipeline computes nightly and publishes as JSON, so the domain here is a read model: records mapped one to one from that JSON, named with its field names, compared and grouped but never edited. The one thing a user writes is a verdict draft in the workbench, and even that leaves the application through GitHub's own editor rather than being published from here.

That shape is a rule, not an accident. The moment this application computes a verdict, a state or an affected range of its own, it can disagree with the published pages, and two answers to "is my version safe" is strictly worse than one. Where a screen needs a value the JSON does not carry, the fix is an additive export in the cve-status pipeline, recorded first as a row in `specs/FEEDBACK-25.4.md` under data needs, never a computation here.

## Conventions

- Read models are Java records in `com.vaadin.securitystatus.data.model`, one record per JSON object shape, field names matching the JSON so a reader can hold the wire format and the code in one head.
- Jackson maps them. Unknown fields are ignored, because the pipeline adds fields without versioning, and a new field must never break this application.
- Every optional field is an `Optional` or a nullable with an absent rendering defined by the screen that shows it. Rule 4 of `00-overview.md`: absent is omitted.
- Versions are a `VersionNumber` value type: parsed once, compared numerically, printed as received. String comparison of versions is a rejected pattern because `24.10` sorts under `24.2` as a string.
- Identifiers keep their case. `CVE-2016-1000027` is an id, not a word.

## The entities, read side

### Line

One maintained minor. From `cases/index.json` `lines[]`.

| Field | Meaning |
| --- | --- |
| `line` | The minor, `24.9` |
| `version` | The newest measured release of it |
| `kind` | `release` or `prerelease` |
| `status`, `reason` | Whether it was scanned, and why not when not |
| `findings`, `by_severity`, `by_ecosystem` | Counts from the newest measurement |
| `scanned_at`, `measured`, `measured_at` | When, and what exactly |
| `support` | Tier, label, the free and subscription boundaries, and the sentence quoting where that came from |

The home card's action line reads the index's `summary` entry for the line's version, whose fields include the bucket id lists and `upgrade_to`; the entry's captured shape is in `02-data-source.md`.

### VersionAnswer

Everything the by version screen needs for one release. From `cases/v/<version>.json`.

| Field | Meaning |
| --- | --- |
| `state` | `secure`, `fix_available`, `unresolved` |
| `risk`, `triage`, `ruled_out` | Finding id lists by bucket: real risks, not judged, judged not applicable |
| `theirs`, `theirs_risk`, `spring_pinned`, `spring_boot`, `spring_boot_declared` | The Spring Boot half |
| `discontinued`, `discontinued_through` | Findings only a discontinued product brings, and through what |
| `from_build` | Findings only the build carries |
| `fixable`, `stuck` | What the newest release of the line clears, and what nothing clears yet |
| `upgrade` | `to`, `clears`, `remains`: the one upgrade sentence |
| `verdicts` | Per advisory: state and the assessments answering it, with what each said by package |
| `assessments` | Per assessment id: applicability, impact, workaround, fix expectation, PRs, handover, depth, affects, when |

### CveAnswer

Everything the by CVE screen needs for one advisory. From `cases/cve/<id>.json`.

| Field | Meaning |
| --- | --- |
| `severity`, `summary`, `aliases`, `first_seen`, `current` | The advisory itself |
| `applicability`, `fix_owner` | The worst verdict and who ships the fix |
| `modules`, `packages`, `origins` | What carries it and where it comes from |
| `coverage` | Per line: status, `fixed_in`, prerelease and enterprise variants, the measured affected range and its count, and what the fix actually was |
| `arrives` | Per line: the package, its version, what it is bundled in, where it was fixed upstream |
| `assessments` | The full verdict prose |
| `on_versions` | Which measured versions report it |

### AdvisoryCatalogEntry

The short form for lists. From `cases/index.json` `advisories` and `recent`.

### ScanIndex

Coordinate to advisories, for triaging an uploaded report. From `cases/scan-index.json`: per purl without version, the advisory ids, and per advisory the applicability, severity, fix owner and origins.

### ExplorerFront, ExplorerVersion, QueueEntry

The explorer's data, from `data/front.json`, `data/<version>.json` and `data/queue.json`. Same mapping conventions. The field lists are in `02-data-source.md` because their exact shape was captured from live fetches and belongs with the endpoint contract.

## The verdict vocabulary

Closed, owned by cve-status, and never extended here: `risk`, `configuration`, `no_risk`, `not_affected`, `false_positive`, `fixed`, `not_current`. The workbench writes only six of these, `not_current` being the pipeline's own, D-120. The absence of a verdict is `in_triage`, which is a fact about Vaadin's work and never presented as a property of the reader's release. Labels for customer words come from `labels` in the index, not from strings of ours, because the same wording written in two places drifts.

## The state rule

A version's state is read from the JSON, never recomputed: `secure` when nothing is open, `fix_available` when the newest release of the line clears everything open, `unresolved` otherwise. Spring Boot, build only and discontinued product findings are outside that count, which is the pipeline's rule and the reason this application must not count for itself.

## The triage model, scanner reports

An uploaded report is parsed into a `ScannerReport`: the detected format, the Vaadin version when the file names one, and one finding per entry with its advisory ids, coordinate and severity, within the bounds of D-257 (32 MB, 10 000 findings, a coordinate of at most 300 characters). Triage joins them against `ScanIndex` and the reader's `VersionAnswer` into three buckets with an owner and an action sentence each:

| Bucket | Meaning |
| --- | --- |
| Already secure | Judged not applicable, or fixed at or below the reader's version |
| Fix available | Open on the reader's version, cleared by the named upgrade |
| Unresolved | Open with nothing to move to yet, or not assessed yet, said as such |

Owner is one of Vaadin, Spring Boot, the library, or yours, derived the way `6.11.14` of the source spec demands: Spring Boot only where Spring Boot can fix it. A coordinate the measurements do not cover is the reader's own and is never called secure.

## The one write model

### VerdictDraft

A JPA entity, H2, session independent so a draft survives a restart. Its fields mirror the verdict YAML that vaadin/cve-status takes in `data/assessments/`, whose reference is that repository's `docs/spec.md`, and whose shape was read from a real assessment file on 2026-11-20. A verdict is named after itself, not after a CVE: one advisory reaching the sbom through several artifacts earns a verdict per artifact, and one verdict can answer a batch of advisories on one artifact.

| Field | YAML key | Constraint |
| --- | --- | --- |
| `verdictId` | `id` | Required for `Ready`. Starts with a letter or digit, then letters, digits, dot, underscore and hyphen. The verdict's own name, which also names the file. The workbench proposes one, `features/10-workbench.md`, and the person edits it, D-124 |
| `purl` | `purl` | Required for `Ready`, `pkg:<type>/<name>` with no version |
| `advisories` | `cves` | Required for `Ready`, one or more `CVE-` or `GHSA-` ids separated by commas or spaces |
| `applicability` | `applicability` | Required for `Ready`, one of the six words the cve-status loader takes: `risk`, `not_affected`, `configuration`, `no_risk`, `false_positive`, `fixed`, D-120 |
| (derived) | `justification` | Not a field: written from the applicability, `not_affected` as `code_not_present`, `configuration` as `requires_configuration`, `no_risk` as `code_not_reachable`, nothing for the others |
| `depth` | `depth` | Optional, one of `cursory`, `reproduced`, `committee`, `automatic` |
| `affects` | `affects` | Optional, `*` or comparison clauses such as `>=24.9.0, <24.9.26`, a version with an optional `-alpha`, `-beta` or `-rc` number |
| `impact`, `workaround` | same | Prose. Impact is required to export a `risk` or `no_risk` verdict, because such a verdict without an impact sentence answers nothing |
| `commentary` | `commentary` | Required for `Ready`, because the cve-status writer refuses a verdict without it, D-223 |
| `fixExpected`, `fixExpectedVersion`, `fixPrs` | `fix_expected`, `fix_expected_version`, `fix_prs` | Optional. The version is a list of `<line>: <version>` promises, each version inside its line; the pull requests are GitHub pull request addresses |
| `evidence`, `supersedes` | same | Optional. `supersedes` names another verdict id. Superseding is a new file pointing at the old one, never an edit in place |
| `assessedBy` | `assessed_by` | Optional. `assessed_at` is the day of the export |
| `pair`, `savedBy`, `updatedAt` | not exported | The identity, one draft per package and advisories pair with the last save winning, then who saved it last and when, D-131 |

Validation groups: `Draft` checks only the formats, `Ready` checks the requireds above, and the YAML export is offered only on `Ready`. A package that carries verdicts and has no kept draft arrives filled from the newest of them by `assessed_at`, among those that speak for the chosen series when a series is chosen and any does, D-278; filling sets the depth back to `cursory`. Two groups because the workbench must save half a thought without arguing about it, and must refuse to export one.

## Not entities on purpose

| Thing | Why not |
| --- | --- |
| Measurement, SBOM, dependency route | They live in the cve-status data branch and arrive pre joined in the JSON. Modelling them here would be a second pipeline |
| User and role beyond Spring Security's | The demo profile's two in memory users, or vaadin.com's single sign on under `cas`, D-258, with the role derived from the email, `04-security.md`. An account system is not this application |
| Advisory text from cve.org or GitHub | Linked, never fetched or stored. The pipeline already curates what is shown |
| A computed affected range | The range arrives measured. Recomputing it is forbidden by the shape rule above |

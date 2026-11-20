# 02 The data source

## Where the data lives

Everything this application shows is published by the vaadin/cve-status pipeline to GitHub Pages at `https://vaadin.github.io/cve-status/`. The JSON is public and carries no authentication, which was verified by fetching every endpoint below without credentials on 2026-11-20. The HTML pages beside it carry a client side sign in, the JSON never did, and the cve-status project records that nothing behind that sign in could not be published in front of it. So: no token, no secret, and nothing here to protect beyond ordinary politeness toward a static host.

The pipeline republishes nightly at 03:17 UTC and on demand. `generated_at` in the index is the freshness fact and the only one: this application never infers freshness from HTTP dates.

## The endpoints

Field lists below were captured from live fetches on 2026-11-20, not from memory. A field this table does not name may still arrive, and the mapper ignores it rather than failing, because the pipeline adds fields without versioning.

| Endpoint | Size that day | Top level fields |
| --- | --- | --- |
| `cases/index.json` | 291 KB | `generated_at`, `labels`, `origin_labels`, `counts`, `line_rule`, `lines[]`, `release_repos`, `spring_boot_latest`, `spring_boot_supply`, `by_line`, `summary`, `advisories`, `fixed_in`, `recent[]` |
| `cases/v/<version>.json` | a few KB each | `version`, `line`, `state`, `raw_state`, `scanned_at`, `findings`, `risk`, `triage`, `ruled_out`, `theirs`, `theirs_risk`, `spring_pinned`, `spring_boot`, `spring_boot_declared`, `discontinued`, `discontinued_through`, `from_build`, `fixable`, `stuck`, `upgrade`, `verdicts`, `assessments` |
| `cases/cve/<id>.json` | a few KB each | `cve`, `severity`, `summary`, `aliases`, `applicability`, `fix_owner`, `modules`, `packages`, `origins`, `first_seen`, `current`, `coverage`, `arrives`, `assessments`, `on_versions`, `vaadin_advisory` |
| `cases/scan-index.json` | 85 KB | One entry per coordinate, a purl without version: its advisory ids, and per advisory the applicability, severity, fix owner and origins |
| `cases/cases.json` | small | `generated_at`, `cases[]` with `id`, `title`, `screen`, `status`, `count`, `example` |
| `data/front.json` | 201 KB | `generated_at`, `lines[]` with `line`, `version`, `kind`, `releases`, `findings`, `scanned_at`, `scanner_files`, `spring_boot`, `spring_boot_declared`, `needs_spring`, `without_spring`, `reason`; plus `module_names` in usage order, `profile` with its `off`, `discontinued`, `fixed` and `inside` groups, `packages`, `history`, `spring_boot_latest`, `spring_boot_supply`, `spring_boot_tried` |
| `data/<version>.json` | varies | `version`, `line`, `state`, `newest`, `scanned_at`, `generated_at`, `counts`, `findings`, `from_spring_boot`, `without_spring`, `spring_boot`, `spring_boot_declared`, `scanner_files`, `workaround` |
| `data/queue.json` | 923 KB | `generated_at`, `heads`, `assessments`, `packages[]` with `package`, `purl`, `ecosystem`, `cves`, `cpes`, `lines`, `closes`, `routes`, `resolved`, `origin`, `scanners`, `sources`, `acknowledged` |
| `data/index.json` | 43 KB | The machine readable per version index, `release_repos` and friends |
| `exports/<version>.csv`, `.vex.json`, `.dependency-check-suppressions.xml`, `.osv-scanner.toml`, `.bomber-ignore.txt` | varies | The per release files the explorer offers for download. Proxied, never parsed |

An id in a path keeps its case and its dots: `cases/v/24.9.21.json`, `cases/cve/CVE-2016-1000027.json`.

Three entry shapes inside the index that screens read directly, captured the same day: a `summary` entry carries `line`, `state`, `scanned_at`, `risk`, `triage`, `ruled_out`, `theirs`, `theirs_risk`, `spring_pinned`, `discontinued`, `from_build` and `upgrade_to`, which is where the home card's action line comes from; a `recent` entry is a verdict with `id`, `purl`, `cves`, `applicability`, `impact`, `workaround`, `commentary`, `depth`, `affects`, `fix_expected`, `fix_expected_version`, `fix_prs`, `handover`, `assessed_at`, `assessed_by`; an `advisories` entry carries `cve`, `severity`, `applicability`, `fix_owner`, `modules`, `packages`, `origins`, `current`, `vaadin_advisory`.

The product registry the explorer filters by arrives in `data/front.json`: `module_names` is the usage order and `profile` says which modules are optional, discontinued, fixed or carried inside another. What front.json does not carry is the short address codes that the full `data.json` holds in `module_codes`, and the full file is 66 MB, which no session fetches. Until an additive export brings the codes, recorded under data needs in `specs/FEEDBACK-25.4.md`, the explorer addresses products by name, which the source spec's own rule 6.2.6 allows as the fallback for a product with no code.

## The client

One class talks HTTP, `PagesClient` in `data`, built on Spring's `RestClient`. Everything else, views included, reads `SecurityDataService`, which answers from the cache. This is hard rule 3 of `00-overview.md`.

### Caching

- In memory, per endpoint, keyed by path. The whole working set is a few MB, so eviction is a non problem and the policy is time based only.
- The index is fresh for 15 minutes, per version and per CVE documents for 6 hours, because they only change when the index does: a new `generated_at` in the index invalidates everything else.
- Conditional requests with `ETag` and `If-None-Match`, which GitHub Pages serves, so a revalidation costs a 304 and no body.
- Single flight: concurrent first requests for one path share one fetch. Vaadin sessions arrive in bursts and twenty sessions must not mean twenty fetches.
- Stale over error: when a refresh fails, the cache keeps answering with what it has and the staleness is surfaced once, in the measured chip, as the data's own `generated_at`. An error page where an answer exists is the wrong trade on a read only site.
- A cold cache that cannot reach Pages is the one real failure. The screen says the data source is unreachable, in one sentence, and nothing pretends.

### The refresh signal

`DataRefreshSignal` holds the newest `generated_at` as a shared signal. A scheduled task revalidates the index every 15 minutes, and when `generated_at` moves, every session's measured chip and home cards update without a reload. This is the application's one live surface and the reason shared signals are claimed by epic 02: the data changes nightly underneath long lived tabs, and the current static pages simply go stale.

## What the machine files are

The per release export links on the explorer are passthroughs: the server streams `exports/<version>.<ext>` from Pages with the right content type and a content disposition, through the 25.4 `Download` API. Proxied rather than linked cross origin so the download works where the explorer works and carries one origin's cookies, nothing else. Never parsed, never rewritten.

## Tests and the network

The gate runs with no network. `src/test/resources/pages/` holds one committed fixture per endpoint shape, taken from the live site by `scripts/refresh-fixtures.sh`, which is committed and is the only way those files change. A fixture is a pinned copy of a real answer, not an offline mode: the application has none, by decision, and a test asserting on fixture content says which `generated_at` it pinned.

## When the JSON falls short

If a screen needs a value these endpoints do not carry, the fix is an additive export in the cve-status pipeline. The need is recorded first in `specs/FEEDBACK-25.4.md` under data needs, with the screen, the field and why nothing present covers it, and the screen ships with the value omitted until the export exists. Inventing the value here is forbidden by rule 4, and computing it here is forbidden by the shape rule in `01-domain-model.md`.

Known candidates at specification time, one: the explorer's short product codes, missing from `data/front.json` as described above, already a row in the feedback file. Every other field the thirteen epics name was found in the captured answers above.

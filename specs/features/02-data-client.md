# Feature 02: The data client

## Overview

The one place that talks to GitHub Pages, the cache in front of it, the read models, and the refresh signal every session watches. The contract it implements is `specs/02-data-source.md`; this document is its behaviour on this side of the wire.

## Behaviour

- `PagesClient` fetches the endpoints of `02-data-source.md` with `RestClient`, sending `If-None-Match` whenever an `ETag` is cached.
- `SecurityDataService` is the only API views and tools see: `index()`, `version(VersionNumber)`, `cve(String)`, `scanIndex()`, `cases()`, `explorerFront()`, `explorerVersion(...)`, `queue()`, each answering from the cache with the TTLs the contract names.
- Single flight per path. Twenty sessions asking for a cold index cause one fetch.
- Stale over error: a failed refresh keeps the last good answer and the `generated_at` chip tells the truth about its age. A cold cache with no network renders the one unreachable sentence.
- `DataRefreshSignal` holds the newest `generated_at` as a shared signal. A scheduled revalidation every 15 minutes moves it when the index moved, and moving it invalidates the per version and per CVE entries.
- Read models per `01-domain-model.md`, Jackson, unknown fields ignored, `VersionNumber` for every version comparison.
- The exports passthrough streams `exports/<version>.<ext>` through the `Download` API with the correct content type and disposition, no buffering of the whole body.

## Edge cases

| Scenario | Behaviour |
| --- | --- |
| Pages answers 404 for a version | `version()` answers empty, the screen renders the not measured state, nothing is cached as an error |
| Pages answers 500 or times out | Stale answer if one exists, the unreachable sentence if not. The failure is logged once, not per session |
| `generated_at` moves backwards | Trusted anyway and surfaced, because the pipeline owns the clock and second guessing it is inventing data |
| Two refresh tasks overlap | Single flight makes the second a spectator |
| A JSON field disappears | The `Optional` is empty and the screen omits, rule 4. The mapper never throws for absence |

## Acceptance criteria

### AC1: One door to the network
- [ ] No class outside `data` imports an HTTP client, enforced by an architecture test

### AC2: The cache behaves
- [ ] A second request inside the TTL causes no fetch
- [ ] A 304 revalidation refreshes the TTL without reparsing
- [ ] Stale answers survive a dead upstream

### AC3: The signal reaches everyone
- [ ] A moved `generated_at` updates the measured chip in two concurrent sessions without a reload

### AC4: The models are honest
- [ ] Absent optional fields map to absent, never to empty strings
- [ ] `24.10` sorts above `24.2`

## Test cases

| Id | Given | When | Then | Tier | Verified by |
| --- | --- | --- | --- | --- | --- |
| DAT-01 | A warm cache inside TTL | A second `index()` call | No HTTP request is made | unit | `PagesCacheTest` |
| DAT-02 | An upstream returning 304 | Revalidating | The cached body is kept and the TTL renewed | unit | `PagesCacheTest` |
| DAT-03 | A dead upstream and a warm cache | Any read | The stale answer, and the chip shows its `generated_at` | unit | `PagesCacheTest` |
| DAT-04 | A cold cache and a dead upstream | Any read | The unreachable sentence, no exception page | browserless | `DataOutageBrowserlessTest` |
| DAT-05 | Two sessions open | The fixture's `generated_at` moves | Both measured chips update | browserless | `RefreshBroadcastBrowserlessTest` |
| DAT-06 | The version fixture with absent workaround fields | Mapping | The record reports absent, not empty | unit | `ReadModelTest` |
| DAT-07 | Versions 24.2, 24.10, 24.9.21 | Sorting | Numeric order, not string order | unit | `VersionNumberTest` |
| DAT-08 | A request for an export file | Downloading | The bytes stream with the upstream content type and a disposition naming the file | testbench | `ExportsProxyIT` |

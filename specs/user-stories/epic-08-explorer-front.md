# Epic 08: Explorer front door

**Goal:** the team's whole map, narrowed to what an application actually uses, remembering what the reader arranged.
**Feature spec:** `specs/features/08-explorer-front.md`
**Dependencies:** epics 01 and 02.

---

### US-8.1: Master and detail

**Tasks:**
- [x] `MasterDetailLayout`, series rows with badges and rings, detail per opened line. Superseded at the prototype alignment: each series opens in place under its row, D-204
- [x] The drawer with search and chips, overlay below 800. Since D-203 and D-204 the drawer is the internal shell's left panel itself

**25.2 APIs:** Master Detail Layout.
**Verified by:** `ExplorerBrowserlessTest`

---

### US-8.2: The product filter

**As a** team member **I want** to turn off the products an application does not use **so that** every number on the screen is about that application.

**Tasks:**
- [x] Chips from the registry, usage order, tri state all, colour rings
- [x] Off only addressing by registry name until the code export lands, unknown names dropped
- [x] Marginal counts, hide only when every route is off, no product never hides. The counts are the prototype's since D-207
- [x] Web storage memory, merged, address wins
- [x] The without Spring switch per line

**Verified by:** `ExplorerFilterBrowserlessTest`

---

### US-8.3: Groups and findings

**Tasks:**
- [x] The five group discipline, open state persisted on stable keys. The groups are the prototype's halves since D-205
- [x] Finding rows with routes, lazy per version load, the 150 cap
- [x] Grid texts through the Grid i18n API

**25.3 APIs:** `GridI18n`.
**Verified by:** `ExplorerBrowserlessTest`

---

### US-8.4: Search and downloads

**Tasks:**
- [x] `?q` filter, full version jumps to the release page
- [x] The six per release files through the proxy

**Verified by:** `ExplorerBrowserlessTest`, `ExportsProxyIT`

---

## Definition of Done

- [x] Every acceptance criterion in `features/08-explorer-front.md` is checked
- [x] The filter rules hold under the fixture's multi product findings

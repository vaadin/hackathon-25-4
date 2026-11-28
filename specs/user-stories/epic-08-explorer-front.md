# Epic 08: Explorer front door

**Goal:** the team's whole map, narrowed to what an application actually uses, remembering what the reader arranged.
**Feature spec:** `specs/features/08-explorer-front.md`
**Dependencies:** epics 01 and 02.

---

### US-8.1: The series and their detail

**Tasks:**
- [x] Series rows with badges and rings, detail per opened line: each series opens in place under its row, one at a time, `?open=<line>`. First built as a `MasterDetailLayout`, which D-204 removed
- [x] The drawer with search and chips, overlay below 800: the drawer is the internal shell's left panel itself since D-203 and D-204, and it is the App Layout that overlays. The measurement of D-160 was of the Master Detail Layout and does not carry over; the App Layout's left panel was seen arriving shut as an overlay at 760 pixels in session five, `specs/reviews/prototype/explorer.md`

**25.2 APIs:** Master Detail Layout, no longer used since D-204.
**Verified by:** `ExplorerBrowserlessTest`

---

### US-8.2: The product filter

**As a** team member **I want** to turn off the products an application does not use **so that** every number on the screen is about that application.

**Tasks:**
- [x] Chips from the profile's catalogue, the fixed product first, the choices, then the discontinued one, a tri state box heading them, colour rings. The order and the box are the prototype's since D-206; the registry's usage order is kept for the ring colours
- [x] Off only addressing by registry name until the code export lands, unknown names dropped
- [x] Chip counts, hide only when every route is off, no product never hides. The counts are no longer marginal: distinct advisories through a chip as if it were on, D-207
- [x] Web storage memory, merged, address wins
- [x] ~~The without Spring switch per line~~: removed by D-266. The Spring Boot starter's chip is the one control for the shape

**Verified by:** `ExplorerFilterBrowserlessTest`

---

### US-8.3: Groups and findings

**Tasks:**
- [x] The group discipline, open state persisted on stable keys: since D-205 two halves, From Vaadin and With Spring Boot, of three groups each, and the build only group after them, in place of the five groups of D-95
- [x] Finding rows with routes, lazy per version load, the 150 cap
- [x] ~~Grid texts through the Grid i18n API~~: removed with the grid by D-264. The findings are disclosures with no header, so there is no grid text to localise

**25.3 APIs:** none since D-264, which removed the grid that used `GridI18n`.
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

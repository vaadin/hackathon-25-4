# Feature 08: Explorer front door

## Overview

The security team's reading: every maintained line, narrowed by the products an application actually uses. The behaviour contract is sections 6.2 and 6.5 to 6.7 of the source requirements. Covers T2 to T6.

## Behaviour

### The layout

- `MasterDetailLayout`: the series list is the master, the opened line's detail the detail pane, which is the 25.2 component doing the job the source page did with one open details row at a time, rule 6.3.4's spirit: choosing replaces, not extends.
- The drawer holds the search and the product chips. Below 800 pixels it overlays, which the component gives for free.

### Search

- One field, filter by advisory, version or dependency, held in `?q`. Typing a full version navigates to the release page, the source shortcut.

### Product chips

- One chip per product from the registry `data/front.json` carries: `module_names` is the usage order, most widely used first, never alphabetical, and `profile` says which are optional, discontinued, fixed or carried inside another. Each chip shows its per line count and a colour ring assigned from one fixed palette by registry order, a presentation detail the builder owns.
- Everything starts on. The address names only what is off, `?off=<name>,<name>`, by product name until the short code export lands, per the data need recorded in `specs/FEEDBACK-25.4.md` and the source spec's 6.2.6 fallback. The selection is remembered through web storage, merged, restored only when the address carries no selection of its own, rules 6.2.4, 6.7.3, 6.7.6.
- The tri state all control reads the chips: all on shows checked, all off unchecked, anything else mixed. Clicking it turns everything on unless everything is already on, in which case it turns the optional products off, which is the useful second state, and a second click restores all on.
- A finding is hidden only when every product it arrives through is off, and a finding with no product recorded is never hidden, rules 6.5.11 and 6.5.12.
- Counts are marginal, recomputed against what else is off, quieter than the names, rules 6.5.8 to 6.5.10.
- Turning the Spring Boot starter off switches the line to the measurement taken without Spring, the source behaviour.

### The series rows

- One row per maintained line: the line, the exact version, scanned date, the Vaadin badge, vulnerable, not assessed yet, or not affected, the Spring Boot badge, a summary sentence, per product rings.
- The opened line shows the groups: a real risk, not assessed yet, and answered closed by default, the with Spring Boot half, the frontend bundle and switched off products notes. The two groups that ask for work start open, the ones that inform start closed, rule 6.6.2. Which groups the reader opened persists, keyed on the heading's stable part, rules 6.7.1 and 6.7.2.
- Each finding row: id, severity badge, through N of M products or only with X or bundled inside X, the coloured route dots. Opening it loads the per version document lazily and shows summary, impact, workaround, fix, routes, advisory link. Groups cap at 150 with the remainder counted.
- Grid texts localise through the Grid i18n API, which is where that 25.3 arrival earns its row.

### Downloads

Per release: JSON, CSV, VEX, dependency check suppressions, osv-scanner toml, bomber ignore, each streamed through the proxy of epic 02 with the `Download` API.

## Edge cases

| Scenario | Behaviour |
| --- | --- |
| A chip that can hide nothing | Not shown, accounted for in the one sentence, rule 6.5.6 |
| An `?off` name matching no product on this data | Dropped from the address, rule 6.2.7 |
| Storage empty or unreadable | Everything on, silently |
| A line measured without Spring only | The Spring toggle explains itself on that line |

## Acceptance criteria

### AC1: The filter is the source filter
- [x] Off only addressing by registry name, unknown names dropped
- [x] Marginal counts move when another chip turns off
- [x] A finding with two products needs both off to hide
- [x] The no product finding never hides

### AC2: Rows and groups
- [x] Badges match the fixture per line
- [x] Group open state persists across a reload, merged
- [x] The 150 cap counts its remainder

### AC3: Search
- [x] `?q` narrows rows and survives navigation
- [x] A full version typed lands on the release page

### AC4: Downloads
- [x] Each file fetches with its content type through the proxy

## Test cases

| Id | Given | When | Then | Tier | Verified by |
| --- | --- | --- | --- | --- | --- |
| EXP-01 | The front fixture | Opening findings as team | One row per line, badges match | browserless | `ExplorerBrowserlessTest` |
| EXP-02 | Two chips off | Reading the address | `?off` names them and nothing else | browserless | `ExplorerBrowserlessTest` |
| EXP-03 | A finding through two products | One off | Still shown; both off, hidden | browserless | `ExplorerFilterBrowserlessTest` |
| EXP-04 | A chip's count | Turning a second chip off | The count changes marginally | browserless | `ExplorerFilterBrowserlessTest` |
| EXP-05 | Groups opened, reload | Reopening the line | The same groups open | browserless | `ExplorerBrowserlessTest` |
| EXP-06 | A version typed in search | Submitting | The release route | browserless | `ExplorerBrowserlessTest` |
| EXP-07 | The six download links | Fetching each | 200 with the expected content type | testbench | `ExportsProxyIT` |
| EXP-08 | Spring starter chip off | Reading the line | The without Spring measurement renders | browserless | `ExplorerFilterBrowserlessTest` |

## Still open

Written from the code at the end of epic 13. Each line names where the code shows it and the decision or draft behind it.

- There is no drawer: the search, the tri state control and the chips sit at the top of the master pane, and what overlays is the detail pane over them, D-94. Measured in epic 13: at a 760 pixel window the detail overlays; the threshold is the layout's own width, 26rem plus 24rem, so with the internal drawer open it also overlays up to a window of about 1180 pixels, D-160.
- `?off` addresses products by registry name; switch to the short codes when `module_codes` lands in `data/front.json`, `ExplorerRules`, data need in `FEEDBACK-25.4.md`.
- A product the profile marks `fixed` has no chip and cannot be turned off; a chip's count is over the opened line, or over every shown line when none is open, not per line, `ExplorerView.drawProducts`, D-96.
- A full version navigates to the release page on Enter or on leaving the field, not as it is typed, `ExplorerView.submit`, D-103.
- Each filter change adds a history entry instead of rewriting the address in place, and a remembered selection reaches the address only after the first paint, `ExplorerView.beforeEnter`, `ProductSelection`, D-98.
- There is no frontend bundle note on the opened line: a bundled finding says "bundled inside X" in its row, and build only findings form an "Only in the build" group, `FindingGrid`, `ExplorerRules.group`, D-95.
- The opened line offers its files only when its record says `scanner_files`, `ExplorerView.detail`, D-109.
- Workaround to remove when browserless-test#29 is fixed, draft 20b: the tests open item details with `setDetailsVisible`, `ExplorerBrowserlessTest`.

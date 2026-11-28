# Epic 03: Home and the lookup

**Goal:** a front door that answers at a glance and a lookup that never loses the question.
**Feature spec:** `specs/features/03-home-and-lookup.md`
**Dependencies:** epics 01 and 02.

---

### US-3.1: The lookup component

**As a** customer **I want** to ask by version, by id or by file in one place **so that** the way in is the same whoever I am.

**Tasks:**
- [x] Three tabs over `LookupState` session signals
- [x] Version cleanup, the CVE jump, the ids splitter
- Removed with D-173, not done: ~~ComboBox with `PartialMatchMode` over measured versions~~. The requirement left feature 03 because neither the prototype nor the application has it, and 25.4.0-beta2 refuses partial match beside custom values, D-40. What stands is a ComboBox over measured versions with free text allowed, D-228
- [x] Check with `DisableOnClickMode`, clear and Escape withdraw to home
- [x] Slash focuses the lookup through a keydown listener with a declared filter
- [x] The help dialogs per tab
- [x] One version for every tab, D-274
- [x] Enter asks as Check does, D-240; ids found inside a pasted address, D-244

**25.x APIs:** `DisableOnClickMode`, declared JavaScript filters (`@JsDefinition`, `@JsExpression`), value change modes; `PartialMatchMode` removed with D-173.
**Verified by:** `LookupBrowserlessTest`

---

### US-3.2: The line cards

**Tasks:**
- [x] One card per line from the index, state words and icons, action line, scanned date
- [x] Whole card a real link, `?pre` for prereleases
- [x] The three home parts under By version only, D-273

**Verified by:** `HomeBrowserlessTest`

---

### US-3.3: Recently reviewed

**Tasks:**
- [x] Eight rows from `recent`, verdict chips in customer words, the advisories link
- [x] The Tomcat, Spring, logback pointer box

**Verified by:** `HomeBrowserlessTest`

---

## Definition of Done

- [x] Every acceptance criterion in `features/03-home-and-lookup.md` is checked
- [x] The lookup state survives every navigation the tests drive

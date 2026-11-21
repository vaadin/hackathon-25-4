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
- [ ] ComboBox with `PartialMatchMode` over measured versions, free text allowed
- [x] Check with `DisableOnClickMode`, clear and Escape withdraw to home
- [x] Slash shortcut with a JS filter
- [x] The help dialogs per tab

**25.x APIs:** `PartialMatchMode`, `DisableOnClickMode`, `ShortcutFilterJs`, value change modes.
**Verified by:** `LookupBrowserlessTest`

---

### US-3.2: The line cards

**Tasks:**
- [x] One card per line from the index, state words and icons, action line, scanned date
- [x] Whole card a real link, `?pre` for prereleases

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

# Epic 03: Home and the lookup

**Goal:** a front door that answers at a glance and a lookup that never loses the question.
**Feature spec:** `specs/features/03-home-and-lookup.md`
**Dependencies:** epics 01 and 02.

---

### US-3.1: The lookup component

**As a** customer **I want** to ask by version, by id or by file in one place **so that** the way in is the same whoever I am.

**Tasks:**
- [ ] Three tabs over `LookupState` session signals
- [ ] Version cleanup, the CVE jump, the ids splitter
- [ ] ComboBox with `PartialMatchMode` over measured versions, free text allowed
- [ ] Check with `DisableOnClickMode`, clear and Escape withdraw to home
- [ ] Slash shortcut with a JS filter
- [ ] The help dialogs per tab

**25.x APIs:** `PartialMatchMode`, `DisableOnClickMode`, `ShortcutFilterJs`, value change modes.
**Verified by:** `LookupBrowserlessTest`

---

### US-3.2: The line cards

**Tasks:**
- [ ] One card per line from the index, state words and icons, action line, scanned date
- [ ] Whole card a real link, `?pre` for prereleases

**Verified by:** `HomeBrowserlessTest`

---

### US-3.3: Recently reviewed

**Tasks:**
- [ ] Eight rows from `recent`, verdict chips in customer words, the advisories link
- [ ] The Tomcat, Spring, logback pointer box

**Verified by:** `HomeBrowserlessTest`

---

## Definition of Done

- [ ] Every acceptance criterion in `features/03-home-and-lookup.md` is checked
- [ ] The lookup state survives every navigation the tests drive

# Feature 06: Scanner report triage

## Overview

An auditor drops the file their scanner produced and takes away a triaged answer. The parsing happens on the server, the file is never persisted, and the screen says so. Covers S1 to S8.

## Behaviour

### Upload

- The report tab's drop zone and file button, built on the modular upload pieces, accepting one JSON file at a time. Replace file swaps it.
- The file is parsed in the request through an in memory `UploadHandler` and discarded: no temp file, no storage, and the help dialog states it, because the source page promised the file never leaves the browser and this application must replace that promise with an equal one it can keep.
- Format detection by shape, not extension: osv-scanner JSON, OWASP Dependency-Check JSON, bomber JSON, CycloneDX including nested components. The help card lists exactly these four and a test holds the list to the parser, rule 6.11.21.
- The Vaadin version is read from the report's own coordinates, `com.vaadin:vaadin`, `vaadin-core`, `vaadin-bom`, `vaadin-platform-sbom`, `vaadin-server`, and the version field appears only when the file names none, with the submit reading Answer for this version.

### Triage

Per `01-domain-model.md`: each reported finding joins `scan-index` and the reader's `VersionAnswer` into already secure, fix available, or unresolved, with an owner, Vaadin, Spring Boot, the library, or yours, and an action sentence. Spring Boot owns only what Spring Boot can fix, rule 6.11.14. A coordinate we do not measure is the reader's own and is never called secure. Entries with no coordinate are skipped and counted, never silently dropped.

### The screen

- The headline: one upgrade clears N of your M open findings, or N findings need attention, from the buckets.
- Three bucket cards with counts and detail sentences, the design's three words, and the unjudged counted under unresolved with the fourth word, not assessed yet, only in rows, rule 6.11.19.
- The open findings table: CVE, component, owner, verdict, action, capped at 200 rows with the remainder counted, and a Switch, show the N secure findings.
- Downloads: the triage CSV, named `vaadin-security-triage-<version>.csv`, through the `Download` API, and the audit report as a PDF, `vaadin-security-audit-<version>.pdf`, composed server side with the same facts, previewed in `PdfViewer` inside a dialog before the reader takes it. The PDF exists because auditors attach documents, and the preview exists because `PdfViewer` is a 25.4 arrival with a real job here.
- Both files carry the whole triage, never the screen's 200 row cap, because the cap is a screen concern and an audit record that silently drops rows is an invention by omission. Columns, in this order in both: cve, component, owner, verdict, action, bucket, and a header row naming the version and the `generated_at` it was answered from. The PDF is the headline, the three bucket counts and that same table, one library choice left to the builder with the constraint that it adds no network fetch at render time.
- The help dialog: how to produce each of the four formats, and what is assessed counted against the file in hand, their coordinate count against our measured count, rule 6.11.18.
- A not measured version answers with the nearest measured release offered, in place.
- The report itself is session bound and the address is not pretended otherwise: `/scan/<version>` opened fresh, or shared, renders the report tab's upload state with the version prefilled and one sentence saying no report is in this session. The address carries the version half of the question, which is all that survives the session, and the use case list's V10 is scoped by this rule.

## Edge cases

| Scenario | Behaviour |
| --- | --- |
| A file that is not JSON or matches no format | Refused in the upload area with the four formats named, nothing navigates |
| A report with zero findings | The headline says so, the buckets render zeros, the CSV still downloads |
| A CycloneDX file with nested components | Components flattened, coordinates found |
| A huge report | Parsed streaming, the table caps at 200 with the remainder counted |
| The same file uploaded twice | Same answer, idempotent, no accumulated state |
| A report naming a version we never measured | The triage answers what the catalogue can and the not measured offer renders above it |

## Acceptance criteria

### AC1: The four formats parse
- [ ] One fixture per format produces the same findings set where the fixtures encode the same scan
- [ ] The help card's format list equals the parser's, by test

### AC2: Triage is honest
- [ ] A measured, ruled out finding lands in already secure with the verdict named
- [ ] An unmeasured coordinate lands with owner yours and is never called secure
- [ ] Skipped entries are counted on screen

### AC3: Files leave correctly
- [ ] The CSV carries one row per finding with the six columns in the stated order, the whole set, named for the version
- [ ] The PDF previews in the viewer and downloads with the right type and name

### AC4: The file is not kept
- [ ] No temp file exists after the request, asserted by the handler's contract test
- [ ] The help dialog says the report is parsed and discarded

## Test cases

| Id | Given | When | Then | Tier | Verified by |
| --- | --- | --- | --- | --- | --- |
| SCN-01 | The osv-scanner fixture | Uploading | Format detected, version read, headline correct | browserless | `ScanViewBrowserlessTest` |
| SCN-02 | The OWASP, bomber and CycloneDX fixtures | Uploading each | Same buckets as SCN-01 where they encode the same scan | unit | `ReportParserTest` |
| SCN-03 | A file naming no version | Uploading | The version field appears, submit reads Answer for this version | browserless | `ScanViewBrowserlessTest` |
| SCN-04 | A non report JSON | Uploading | Refused with the formats named | browserless | `ScanViewBrowserlessTest` |
| SCN-05 | The triage fixture | Reading the table | Owners follow the 6.11.14 rule, actions name who moves | unit | `TriageTest` |
| SCN-06 | A real browser and the fixture file | The whole flow | Upload, headline, buckets, table | testbench | `ScanUploadIT` |
| SCN-07 | The CSV download | Fetching it | 200, text/csv, the filename, M rows | testbench | `TriageDownloadIT` |
| SCN-08 | The PDF | Previewing then fetching | The viewer renders page one, the download is application/pdf | testbench | `TriageDownloadIT` |
| SCN-09 | Entries with no coordinate | Uploading | The skipped count renders | browserless | `ScanViewBrowserlessTest` |

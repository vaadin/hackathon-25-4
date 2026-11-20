# Feature 09: Release and Spring Boot pages

## Overview

One release in full, and what Spring Boot brings, the two deep reads the explorer links into. Covers T7 and T8.

## Behaviour

### The release page

- `/findings/release/{line}/{version}`: the same per line card the explorer shows, for one exact release, with its advice sentences, updating to X removes such and such, the Spring Boot upgrade targets, the downloads, and its own lookup for jumping to another release.
- The way back to the explorer is one control, in the same place at every level, above the content, rule 6.1.6.
- The heading names the version, and the document title follows, rule 6.1.7.

### The Spring Boot page

- `/findings/spring`: findings grouped by the Spring Boot release that produces them, each group naming that release, its supply status, open, commercial only through Broadcom, or ended, what is available now, and NVD links per advisory.
- The groups follow the same fold and cap rules as the explorer.

## Edge cases

| Scenario | Behaviour |
| --- | --- |
| A release address for a version not measured | The not measured sentence with the line's measured releases listed |
| A version that belongs to another line than the address claims | Answered under its real line, the address corrected in place |
| A Spring release with supply ended | The group says so and still lists its findings |

## Acceptance criteria

### AC1: The release page is whole
- [ ] Card, advice, downloads and lookup render for a measured release
- [ ] The not measured answer renders in place for the rest

### AC2: Spring groups tell the supply truth
- [ ] Each group names its release and its supply status from the data
- [ ] NVD links match the advisories

## Test cases

| Id | Given | When | Then | Tier | Verified by |
| --- | --- | --- | --- | --- | --- |
| REL-01 | A measured release | Opening its address | The card and advice match the fixture | browserless | `ReleaseViewBrowserlessTest` |
| REL-02 | An unmeasured version | Opening its address | The in place answer | browserless | `ReleaseViewBrowserlessTest` |
| SPR-01 | The spring fixture | Opening the page | Groups per release with supply wording | browserless | `SpringViewBrowserlessTest` |

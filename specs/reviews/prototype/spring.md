# Prototype comparison: the Spring Boot page

Compared on 2026-11-22, both sides reading the same published data, `generated_at` 2026-11-22T05:30:11Z: the prototype at https://vaadin.github.io/cve-status/findings/spring.html past its client side gate with the internal account the person gave, and this application in development mode on live data, on port 8090, without the model key, signed in as the team. Both at 1920 by 1080. Screenshots: `prototype-spring.png`; `app-spring-before.png` before this change and `app-spring.png` after it.

The page has one state, so both were read on arrival, then with every release opened, and the whole text compared line by line. The prototype's rules were read in `web/findings/spring.js`; no code was copied.

| Prototype | App before | Action |
| --- | --- | --- |
| No left panel, the H1 "What Spring Boot brings" in the bar | The same, since D-203 | None |
| "All series" back link | The same | None |
| The lead: "A Vaadin application is a Spring Boot application, and Spring Boot decides the version of Tomcat, Jackson, the Spring libraries and about forty other things. Those are not Vaadin's to move: ..." | A shorter lead of our own | Transcribe wording |
| One row per Spring Boot release, newest first, each a disclosure shut on arrival | One section per release, always open, with 259 risk rows open on arrival, the finding of P-16 | Move: each release is a disclosure shut on arrival, D-219 |
| Row: "3.5.16", "Vaadin 24.10, 24.9", "Vulnerable", "41 findings arrive with this release, 24 of them a real risk", "free patches ended · 3.5.18 is commercial" | H2 "Spring Boot 3.5.16", "Vaadin 24.10, 24.9", "vulnerable", the chip "Commercial only, through Broadcom", "41 findings arrive with this release, 24 of them judged a real risk." | Transcribe wording: the bare release, the state word, the sentence, the supply note; the note replaces the chip, so the supply status is said once on the row, P-16. The prototype's "1 finding arrive" is its own slip: the application keeps the singular |
| The supply note is silent while a line still gets free patches | The chip "Supply open" | Move: silent, as the prototype; the opened release still says it in a sentence: keep as app extra |
| Opened: the supply lines "Open 3.5.16 what you have", "Commercial 3.5.18 Spring Enterprise, not measured", "measured on Vaadin 25.3.5, ...: closes 0 of these 1", "not measured yet", "the last there was, and nobody sells it now" | A table with a header row, "Open", "Commercial, through Broadcom", "What these lines are on", "Measured on Vaadin ...", "Not measured yet" | Transcribe wording; the header row stays: keep as app extra |
| The supply sentence is absent | "Commercial only: free patches for this line have ended, ..." | Keep as app extra, inside the opened release |
| Advice: "Taking the open release above is the first thing to try, because that is where fixes for these arrive." with " Nothing here has measured it yet, so this page cannot say which of these it closes." when nothing measured it; "There is somewhere to upgrade to: Spring Boot X, the newest patch of that line. ..." when no supply entry; "We know of nothing newer to take within this major that anybody publishes, so until the next release a version property in the application pom moves any one of these."; the later minor; "The property is interpolated by spring-boot-dependencies, ... Both were measured on 25.3.2." | Sentences of our own for the same cases, D-115 | Transcribe wording, D-219 |
| No advice at all where `spring_boot_latest` has no entry is not the prototype's: it says "We know of nothing newer" | No advice, D-115 | Keep as app extra: no entry means nobody could look, rule 4, D-115 confirmed |
| Groups "24 we have judged a real risk" open, "nobody has judged yet" and "we have answered, nothing to do" shut, the count first | "Judged a real risk (24)", "Not assessed yet", "Answered, nothing to do (17)" | Transcribe wording |
| A verdict outside the five ruled out verdicts and not `risk` is not judged | `in_triage` alone is not judged, any other verdict answered | Move: the explorer's rule of D-205, D-219 |
| Finding row: the id linked to NVD, the severity, "we have judged this a real risk", "nobody has judged this", "we have answered this, nothing to do", "gone on X" | The same with "judged a real risk", "nobody has judged this yet", "answered, nothing to do"; a GHSA linked to GitHub, D-113 | Transcribe wording. The GHSA link stays, D-113 |
| The caveat: "On Spring Boot 2.7.18 the Vaadin series listed do not all resolve the same set, ..." | The same in our words | Transcribe wording |
| Footer: "Measured against an application shaped like the starter of its own major, which inherits spring-boot-starter-parent as every Vaadin starter does. ..." | The same in our words | Transcribe wording |

## Data

Everything on this page is in `data/front.json`: `lines`, `spring_boot`, `spring_boot_latest`, `spring_boot_supply`, `spring_boot_tried`. No data need row.

## Verified after the change

In development mode on live data, on port 8090, in an isolated Playwright context signed in as the team, at 1920 by 1080, with real clicks. On arrival the six releases are rows shut, each reading as the prototype's: "3.5.16 Vaadin 24.10, 24.9 Vulnerable 41 findings arrive with this release, 24 of them a real risk free patches ended · 3.5.18 is commercial", "3.2.12 ... free and commercial patches have both ended", nothing on the two lines that still get free patches. 3.5.16 opened by a click: "Open 3.5.16 what you have", "Commercial 3.5.18 Spring Enterprise, not measured", the supply sentence the application keeps, "We know of nothing newer to take within this major that anybody publishes, ...", the property hint, and "24 we have judged a real risk" open with "we have judged this a real risk" on each row, the answered group shut. The caveat names 2.7.18 as the prototype's does.

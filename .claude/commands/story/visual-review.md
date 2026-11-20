# Visual review

Only for stories that change something a user sees. The reference for anything that existed before is the live pages: `https://vaadin.github.io/cve-status/` for the customer screens and `https://vaadin.github.io/cve-status/findings/` for the explorer.

## Setup

- New application on 8080.
- The reference page open in another tab, on the same question: the same version, the same CVE list, the same uploaded report.
- Screenshots at 1920 by 1080 into `specs/reviews/<feature>/`, named `new-<view>.png` and `reference-<view>.png`.

## Two reviewers

**The support engineer.** Answers customer tickets with these pages every day and knows the current ones by heart. Every answer that moved, every fact the old page stated that the new one does not, every extra click to reach the verdict is a complaint. The answer matters more than the looks: if the old page said "fixed in 24.9.28" and the new one makes the reader open a section to learn that, that is a defect.

**The pedantic analyst.** Measures. Fills this table and does not round in anyone's favour.

| Metric | Reference | New | Delta | Verdict |
| --- | --- | --- | --- | --- |
| Facts above the fold for the same question | | | | |
| Clicks from arrival to the verdict | | | | |
| Data area as a percentage of the viewport | | | | |
| Base font size | | | | |
| Smallest click target | | | | |

## How to write it

Assume there are problems and find them. Do not give the design the benefit of the doubt, and do not write that something is probably acceptable. List every difference and let the reader decide. Finding nothing means you did not look.

Output: `specs/reviews/<feature>/REVIEW.md` with the complaints, the metrics table, issues split into critical, major and minor, a short list of differences that are deliberate improvements, and a final verdict of pass or fail.

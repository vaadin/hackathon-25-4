# Prototype comparison: the advisories list

Compared on 2026-11-22, both sides reading the same published data, `generated_at` of that day, 534 advisories in the catalogue: the prototype at https://vaadin.github.io/cve-status/ signed in through its client side gate with the customer account the person gave, asked through `#advisories` and `#advisories?verdict=<word>` for each verdict, and this application in development mode on live data, without the model key, through `/advisories` and `/advisories?verdict=<word>`. Both at 1920 by 1080 and at 390 by 844. Screenshots: `prototype-advisories.png` and `prototype-advisories-390.png` for the prototype, `app-advisories-before.png` and `app-advisories-390-before.png` for the application before this change, `app-advisories.png` and `app-advisories-390.png` after it.

The comparison was written before anything was changed. The Action column is one of: move, add, transcribe wording, keep as app extra, data not published. A row whose two sides already agree says so.

| Prototype | App before | Action |
| --- | --- | --- |
| Trail leaf "All advisories" | The same, the view's title | None, already equal |
| The shell's H1, purpose and lookup | The same, `home.md` | None, already equal |
| H2 "Every advisory we have measured" with the aside "534 in all, newest scan of every line we follow" on the same line | H2 "All advisories", the view's title, and no aside | Transcribe: the prototype's heading and aside, as the answer's H2. The view's title stays the trail leaf and the document title, D-191 |
| Tip: "One row per advisory, with the verdict we reached for it. A verdict is about the advisory, not about your version: open one to see which releases carry it and which do not." | Absent | Transcribe, D-191 |
| Pills: "All 534", then one per verdict present in the order affects you, not assessed yet, used only in specific configurations, used, no risk, not used by Vaadin, scanner false positive, already fixed in a release, not on a current release; the count in lighter type; the active pill tinted | "All 534", then not assessed yet first, affects you second, and no "not on a current release" pill; the count in the same type; the active pill outlined | Move: the prototype's order, the count in its own lighter span, D-192 |
| An advisory in triage that no current release reports counts as "not on a current release": not assessed yet 1, not on a current release 121 | Counted by its raw applicability: not assessed yet 122 | Move: the listed verdict, `in_triage` with `current` false read as `not_current`, as the CVE screen already reads it, D-192. The counts become equal |
| Under a pill, the aside keeps "534 in all" and nothing says how many are shown | "N of 534 advisories shown: <verdict>" under the pills, "534 advisories, worst first" without a filter | Keep as app extra: rule 6.3.11 wants the shown count against the total |
| Rows in one bordered box, worst severity first and then by id, the same order as the application's on every row compared | Grid rows in the same order | None for the order: the prototype orders by severity and id too, without dates. D-81 confirmed, D-193 |
| Row: id in bold, the owner chip, the first package of the advisory, the verdict at the right, a chevron | Grid columns Advisory, Severity, Who fixes it, Package, What it means for you; every package joined with commas and cut with an ellipsis at the column's width | Add the chevron. Keep the severity column and the header row as app extras. Every package stays, one per line and wrapping, never cut: this is the fix for the narrow column of `home.md`, D-194 |
| Owner chip "Vaadin issue" for every advisory whose `fix_owner` is not `upstream`, `unknown` included | No chip for `unknown`, D-51 | Keep: D-180 confirmed on this screen, rule 4, D-193 |
| No impact sentence on a row | None, D-82 | None, already equal. D-82 confirmed: the prototype puts the package where an impact would go, D-193 |
| Verdict: "affects you" in a red chip, "not on a current release" in a grey chip, every other verdict in bold words | Every verdict in the same badge | Move: the risk verdict in the error badge and the others in the plain badge, as the home page's recent rows since D-172, D-194 |
| A `?verdict` naming a verdict no row carries: an empty list and "No advisory carries that verdict today." | Ignored, everything shown, no pill active, D-84 | Keep as app extra: D-84 confirmed, a filter with no pill leaves no way back, D-193 |
| A row opens `#cve/<id>` | A row opens `/cve/<id>` | None, already equal |
| No context menu, no ask box | The `GridContextMenu` with copy, the record and the ask, D-85 to D-87; the Grid AI ask for the team role, D-140 | Keep as app extras, in their places: the menu on the rows, the ask box between the pills and the total line, D-193 |

## Each verdict filter

Every pill was opened on both sides. After the listed verdict of D-192 the counts agree on every pill: affects you 150, not assessed yet 1, used only in specific configurations 114, used, no risk 73, not used by Vaadin 32, scanner false positive 38, already fixed in a release 5, not on a current release 121, all 534. Before it, the application showed not assessed yet 122 and no not on a current release pill.

## At 390 pixels

Neither side scrolls sideways at 390, `scrollWidth` 390 on both. The prototype's rows wrap the package under the id; the application's grid keeps its columns and scrolls inside itself. The application's behaviour at that width is kept.

## Verified after the change

In development mode on live data, in a tab of an isolated Playwright context, with real clicks: the pills read All 534, affects you 150, not assessed yet 1, used only in specific configurations 114, used, no risk 73, not used by Vaadin 32, scanner false positive 38, already fixed in a release 5, not on a current release 121, the prototype's counts in its order. Clicking not on a current release wrote `?verdict=not_current` and "121 of 534 advisories shown: not on a current release"; clicking the CVE-2024-38821 row opened `/cve/CVE-2024-38821`. Long package names wrap inside the column, `org.springframework.security:spring-security-web` included, and a row with several packages lists each on its own line. The risk chip is red, the others plain, and every row ends with the chevron. At 390 by 844 `scrollWidth` is 390. The context menu and the team's ask box were not opened in this session; their tests are unchanged and green.

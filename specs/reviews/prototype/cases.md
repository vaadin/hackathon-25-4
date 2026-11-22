# Prototype comparison: the cases screen

Compared on 2026-11-22, both sides reading the same `cases/cases.json`, `generated_at` of that day: the prototype at https://vaadin.github.io/cve-status/ signed in through its client side gate with the customer account the person gave, through `#cases`, which its router lists as "the case index, which is ours and not the design's", and this application in development mode on live data, without the model key, through `/cases`. Both at 1920 by 1080 and at 390 by 844. Screenshots: `prototype-cases.png` and `prototype-cases-390.png` for the prototype, `app-cases-before.png` and `app-cases-390-before.png` for the application before this change, `app-cases.png` and `app-cases-390.png` after it.

The comparison was written before anything was changed. The Action column is one of: move, add, transcribe wording, keep as app extra, data not published. A row whose two sides already agree says so.

| Prototype | App before | Action |
| --- | --- | --- |
| Reached only by its address, linked from nowhere | The same, unlisted | None, already equal |
| Trail leaf "Every case" | "Cases", the view's title | Transcribe: the title becomes "Every case", D-195 |
| H2 "Every case, with a real example" with the aside "Measured on the payload of 2026-11-22", the catalogue's `generated_at` | H2 "Cases" and no date | Transcribe: the heading and the aside, the date read from the catalogue, D-195 |
| Tip: "This screen is ours rather than the design's. It exists so that a walkthrough can show every state the page has, and so that a state nobody has ever seen is visible as exactly that." | Our lead "Every state the customer screens can show, each with a release or an advisory that shows it today." | Transcribe: the prototype's tip replaces our lead, which said less, D-195 |
| Three groups in the same order: impossible, none today, renderable. Headings with an aside: "What the design asks for and the measurements cannot give" with "5 of them", "Cases that do not occur today" with "The layout exists and nothing reaches it", "Cases you can go and look at" with "31 of them" | The same three groups, the same 5, 3 and 31 cases; headings "Cannot be rendered (5)", "None today (3)", "Renderable (31)" | Transcribe the headings and the asides, D-195 |
| A group with no case is not drawn | "None." under the heading | Keep as app extra: it does not occur on this data, and saying none is the honest form of an empty group |
| Within a group, the most frequent case first, by `count` | The catalogue's order | Move: by count, most first, D-195 |
| Row: the count at the left (a dash sign for a case that cannot be rendered, 0 for none today, the count for the others, 6398 without grouping), the title with the screen word under it, at the right a chip "cannot" or "none today", or a See one link | The title, the screen word, "6,398 today", See one, all on one line | Move: the count first and in its own column without grouping, the screen word under the title, the chips at the right, D-195. A case that cannot be rendered gets no count rather than the dash sign: rule 4 has no placeholder for an absent value, and its catalogue count of 0 would say it occurs nowhere, which is a different claim |
| An impossible case: why it cannot be drawn, then "Would need:" and what it would need. The why comes from the prototype's own code, a table of five sentences; only `fix_eta` carries a `needs` in the catalogue, the other four needs are in that table too | The catalogue's `needs` alone, without a prefix, on `fix_eta` | Transcribe "Would need:" before the published `needs`. The why sentences and the four other needs are not in any published document: data not published, a row in `specs/FEEDBACK-25.4.md`, D-195 |
| See one links to the example's own address | The same addresses on every renderable case, translated to routes, D-89 | None, already equal. D-88 and D-89 confirmed: both sides place 5, 3 and 31 cases the same way on this data |
| "A composed answer to paste into a ticket" listed as impossible | The same, as the catalogue says | None. The application composes that answer, D-63, but the catalogue describes the published data, and rule 4 has the screen say what the data says |

## At 390 pixels

Neither side scrolls sideways at 390, `scrollWidth` 390 on both. The application's rows wrap under the count. The application's behaviour at that width is kept.

## Verified after the change

In development mode on live data, in a tab of an isolated Playwright context: the trail's leaf and the document title read "Every case"; the H2, the aside "Measured on the payload of 2026-11-22", the tip and the three group headings with "5 of them", "The layout exists and nothing reaches it" and "31 of them" read as the prototype's; the renderable rows are ordered most first, which `CasesBrowserlessTest` asserts on the fixture, the live order read from the screenshot only; the five impossible rows carry the cannot chip and no count, `fix_eta` with "Would need:" and its sentence; the none today rows carry 0 and the none today chip. At 390 by 844 `scrollWidth` is 390.

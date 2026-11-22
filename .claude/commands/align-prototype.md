Align the screens named in `$ARGUMENTS` with the live prototype, unattended: compare, reorder, add what the data carries, update the spec, and settle the pending items the prototype answers.

The prototype is the JavaScript application this one rebuilds, published at https://vaadin.github.io/cve-status/ (customer page) and https://vaadin.github.io/cve-status/findings/ (internal pages: `index.html`, `release.html`, `spring.html`, `workbench.html`). Both sit behind a client side gate. The two accounts are given to you in your instructions. They belong to a person: never write them into any file of this repository, any commit message, any issue draft, any review or any report.

Everything in `.claude/commands/implement-epic.md` holds as well: never stop to ask, decisions and findings saved as they happen, findings through `specs/issues/README.md` and never posted, gates with the model key removed, stop only listening processes, real clicks in an isolated browser context, two commits, not pushed, no attribution line.

## The person's two choices

- Keep everything the app has. Nothing is removed from the app because the prototype lacks it: the app's extras stay, placed where they disturb the prototype's order least.
- Reorder the app's screens to the prototype's distribution, and add what the prototype shows and the app lacks when the published data carries it.

## For each screen

1. Same data on both sides. Start the app with `env -u OPENAI_API_KEY -u OPENAI_TOKEN VAADIN_LAUNCH_BROWSER=false ./mvnw` in the background: by default it reads the live published JSON, the same data the prototype reads. Ask both the same question: the same version, the same ids, the same report file, the same release.
2. Write the comparison before changing anything, in `specs/reviews/prototype/<screen>.md`: every element and every datum in the prototype's order, in a table with the columns Prototype, App and Action. An action is one of: move, add, transcribe wording, keep as app extra, data not published. Screenshots of both sides beside it, at 1920 by 1080, and at 390 wide for customer screens, named `prototype-<screen>.png` and `app-<screen>.png`.
3. Implement with Vaadin components and Aura. Match the structure and the answers, not the pixels, as `specs/00-overview.md` says. The prototype's customer page is one centered column of about 912 pixels.
4. Wording: where the prototype shows the same state, its words are transcribed into the bundle, which `specs/03-architecture.md` allows. Only words: hard rule 1 still forbids copying code, CSS or markup.
5. Data the prototype shows and the published JSON does not carry: a data needs row in `specs/FEEDBACK-25.4.md`, nothing invented. Rule 4 still outranks layout.
6. Spec: rewrite the screen's feature document to describe the prototype's layout and contents. A requirement that neither the prototype has nor the app implements is removed, with a decision row naming it. Update `specs/05-theming.md` or `specs/03-architecture.md` when a shell or a route changes.
7. Pending items:
   - A `NEEDS-A-PERSON.md` row the prototype answers is marked resolved in its own text, saying how. It is never deleted.
   - A `DECISIONS.md` row the prototype confirms or contradicts gets a new row that confirms or supersedes it, and the old row is annotated with the new number. It is never deleted.
8. Tests move with the structure. Update the browserless tests and ITs that assert it. `AbsenceSweepBrowserlessTest` and `SpecConsistencyTest` stay green.
9. Before committing, run `grep -rn -i` over the whole repository for both account names, and for the domains they use, and get no match.

## When the screens are done

- Run both gates with the key removed, both green.
- Tick the checkboxes only where true.
- Bring `NEXT.md` up to date.
- Make two commits, not pushed:
  - `Align <screens> with the prototype`
  - `Record the <screens> comparison`
- Leave `git status` clean and nothing listening on 8080 or 8081.

The final report states:

- what moved, what was added, and what stayed as an app extra;
- every decision row added, confirmed or superseded;
- every `NEEDS-A-PERSON.md` row resolved;
- every data gap;
- the exact gate counts;
- anything left undone.

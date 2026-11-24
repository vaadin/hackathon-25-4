# Review: diagnostics, epic 12

Screen: `new-diagnostics.png`, the whole page at 1920 by 1080 in development mode, signed in as the team, right after the application started, with one recorded assistant exchange typed into the drawer. There is no reference: neither JavaScript page has a diagnostics view, so the comparison is with what the team would otherwise do, which is read the server log.

## The support engineer

The view is internal and for the team, so the support engineer meets it only as a member of the team. Read that way:

- It answers "is anything failing right now" in one look: the Failed column of the navigations and the failed count of the requests are both on the first screen.
- Nothing here explains a number. A Failed count of one under `/diagnostics` may be a guest refused by the role check, a 404, or a view that threw: the outcome enum does not separate them and the view keeps no exception, D-152. The team still has to go to the log for the why.
- "2 sessions open now" with one person signed in. The session from before the sign in stays alive until the container times it out, D-154. A reader who does not know that will think somebody else is on.
- The routes are templates, `/findings/release/:line/:version`, in Flow's own syntax. Useful for counting, unfamiliar to somebody who knows the address bar.
- The handler names, `UidlRequestHandler`, `IndexHtmlRequestHandler`, are Flow class names. They are what a developer wants and mean nothing to anybody else.
- The policy tail shows the raw JSON a browser sent, which is exactly what is needed to act on it, and nothing else.
- The assistant tail shows the tool result in full, up to 2000 characters, on one table row. With 50 rows that is a long page.
- Times are UTC everywhere except the browser time, which is in the reader's zone by design. The column headings say UTC.

## The pedantic analyst

| Metric | Reference | New | Delta | Verdict |
| --- | --- | --- | --- | --- |
| Facts above the fold | Nothing, the log | Navigations per route and outcome, request totals, the per minute row, six handler kinds, sessions now, the data's age, seven facts of the reader's browser | n/a | New |
| Clicks from arrival to the numbers | Open a shell, tail the log | 0, the drawer link then nothing | n/a | |
| Clicks to see a change made by another session | Reload the log | 0, verified with push in a second browser context | n/a | Live, as asked |
| Data area as a percentage of the viewport | n/a | About 80 per cent; the gap below the navigations table when the requests column is taller is empty | n/a | |
| Base font size | n/a | 16 pixels, Aura; the tail texts 14 pixels monospace | n/a | |
| Smallest click target | n/a | Nothing on the view is clickable | n/a | |

## Issues

### Critical

- Fixed during the review: in the first production jar, `/diagnostics` opened without its shell, `vaadin-app-layout`, `vaadin-side-nav` and `vaadin-message-list` undefined and a `setItems` TypeError, because the build reused a production bundle that predates the route. A rebuilt bundle renders it whole with no console error. Draft 23b, flow#26338.

### Major

- One reader counts as two sessions after a sign in, D-154. The number is true of the container and misleading to a person.
- The Failed column does not say why. A refused guest and a broken view count the same.

### Minor

- The total row of the navigations has no mean and no longest, so its bottom border stops two columns short. Fixed on 2026-11-24: the mean is weighted by navigations and the longest is the largest of every route.
- The navigations panel leaves an empty block below it when the requests column is taller.
- Handler kinds and route templates are developer names, not words.
- The assistant tail rows can be very tall with a full tool result.
- The viewport follows a resize of the window; the screen size, pixel ratio and time zone are read once when the view opens and do not follow a move to another monitor.

## Deliberate

- No refresh button, as `specs/07-observability.md` asks. Every panel is a signal binding.
- No address, query, header, body, session id or user is recorded from a request, D-153. The query string of a navigation is dropped, verified with `/no-such-route?secret=1`.
- The counters are memory only and say since when.

## Verdict

Pass, with the major issues recorded for a person, P-19.

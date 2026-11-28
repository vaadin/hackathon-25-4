# 05 Theming

## The theme

Aura, as the walking skeleton ships it, with design tokens over component CSS. The Figma design of the customer page remains the structural reference, what sits above what, what is a card, what folds, but this application reproduces its answers and its hierarchy with Vaadin components rather than its pixels. That trade is stated in `00-overview.md`: fighting the components to clone a hand rolled page inverts the point of the exercise.

## Light and dark

- One icon button in the app bar, both halves of the application, that steps through System, Light and Dark at each click, its icon saying which is on: a half circle, a sun, a moon, D-247. Before that a menu, D-239, and a select. System is the default and the way back: choosing it forgets the remembered choice. The same button sits on the sign in card, D-277. Decided by Manolo on 2026-11-23, P-02, replacing the Switch, which was 22 pixels tall and had no way back to the system.
- The default follows the system. A reader who disagrees with their system can say so, and the choice is remembered through the typed `WebStorage` accessors, because a preference query cannot be argued with: that is rule 6.8.6 of the source spec.
- The choice applies before the first paint, or the page paints light and repaints dark, which is worse than no choice at all: rule 6.9.1.

## The vocabulary of color

One palette, every value named once as a custom property on `html` in `styles.css`, each a `light-dark()` pair:

| Tokens | What they are |
| --- | --- |
| `--status-secure`, `--status-fix`, `--status-unresolved` | The three state inks: green, amber, red |
| `--severity-critical`, `--severity-high`, `--severity-moderate`, `--severity-low` | The four severities; moderate is the same as `--status-fix` |
| `--surface`, `--panel`, `--hairline`, `--edge-neutral`, `--ink-soft`, `--ink-neutral`, `--chip-neutral` | The prototype's surfaces, lines and quiet inks |
| `--accent`, `--accent-ink`, `--accent-deep`, `--accent-chip`, `--accent-face`, `--accent-edge` | The accent blue, for marks, words, pills and tinted cards |
| `--secure-face`, `--secure-edge`, `--secure-ink`, `--secure-chip`, and the same four for `unresolved`, plus `--unjudged-chip` and `--neutral-face` | Per state, a card's face and edge, its words and a small pill's tint |
| `--r-card` 15px, `--r-panel` 12px, `--r-row` 10px, `--r-control` 9px | The radii |
| `--shadow-card`, `--shadow-button` | The two shadows |
| `--vaadin-tooltip-*` | Tooltips a shade apart from the page, prose width, D-243 |

`styles/common/state.css` hands the palette to whatever carries a state class, through local variables a component reads: `--face` and `--edge` for a card, `--edge-style`, `--ink` for its words, `--ink-deep` for words a shade deeper, `--mark` for a filled mark and `--chip` for a small pill. `state-secure` is green; `state-unresolved` and `state-in_triage` are red; `state-fix_available`, `state-no_version` and `state-none` are the accent blue, as the prototype draws fix available, D-269, D-272; `state-unmeasured` is neutral with a dashed edge. Every state sets all of them, so a state nested in another inherits nothing. A view sets the state class and never a color. The amber `--status-fix` is kept for the team's badges and the moderate severity.

State words always accompany the color, Secure, Fix available, N findings open, because color alone fails a color blind reader and a printed ticket.

## The customer page

One centered column, at most 60rem with 1.5rem of padding, which is the prototype's 912 pixels of content, D-170. In it, in this order on every customer route: the H1, the purpose in the secondary text color, the lookup in a box on the container background, then the route. Home's line cards take their state class and are drawn by `state.css`, D-268, and in Recently reviewed only the risk verdict takes the error variant of the badge, D-172.

## Layout rules that carry over from the source spec

- A long value is never laid out underneath its own label, and a label column never takes a whole phone's width.
- The page survives 390 pixels, including coordinates and advisory ids that are one unbroken word: `overflow-wrap` on the value cells, horizontal scroll only inside tables that genuinely cannot fold.
- Severity and verdict badges are quieter than the text they qualify. A row reads as words with marks beside them, not as a row of marks.
- Form labels in the workbench sit beside their fields on wide screens, through FormLayout's 25.4 side label support with `LabelTextAlign`, and above them on narrow ones.

## Responsive views

The views listed here are verified at phone width during UI verification, the rest are desktop screens by declaration: HomeView, VersionView, CveView, ScanView, AdvisoriesView. The explorer and workbench are team screens used on laptops, and claiming phone support for a verdict queue nobody will work from a phone is polish spent where nobody stands.

## The internal shell

The internal pages: a left panel about 20rem wide, headed by the app icon and the brand "CVE Status", with the destinations as a side navigation at its top, the advisories list among them, and under them the filter and products of the explorer and of the release page where the route has them, D-237, which supersedes the bar links of D-203; it overlays below the App Layout's own breakpoint; a bar over the content with the drawer toggle, the page's H1 at 18 pixels, the trail, the measured chip, the scheme button, the Assistant button and Sign out; the content; and on the right, when opened, the explorer's legend or the assistant panel, each about 22rem wide; below 800 pixels the assistant panel covers the whole page and closes with its own button, D-255. Section headings inside the explorer, "Series", "Filter by advisory, version or dependency", "Narrow the list to your application", "From Vaadin", "With Spring Boot", are small capitals in the secondary text color, as the prototype draws them. A series row is marked at its start by its state's status color, and the risk and unassessed groups inside it are faintly tinted in the same colors. Hints on rings, chips, dots and badges are Vaadin tooltips, never the browser's own title, D-243. Product rings carry their number and take one of six palette colors by the product's place in the catalogue; a ring with nothing to count is grey. The release page draws the same row, opened, D-216; the Spring Boot page draws each Spring Boot release as such a row, shut, D-219; the workbench draws each package as one, marked by whether a verdict there would close anything, its form in a tinted fold inside the row, D-220. The pages without a left panel, the Spring Boot page, the workbench and diagnostics, take the same 1.5rem of content padding, D-225. All of it is in `styles/team/`: `shell.css`, `explorer.css`, `workbench.css`, `assistant.css` and `diagnostics.css`. The customer's styles are in `styles/customer/` and what both share is in `styles/common/` (`shell.css`, `data.css`, `state.css`). `styles.css` imports all of them, the team's last, so deleting the team deletes those lines, D-254. Outside the overlay the drawer is padded a whole 6 pixels, because Aura pads it from an unrounded 1.5vmin and the app icon blurs on a fraction of a pixel; it goes when vaadin/web-components#13535 is fixed.

## CSS discipline

All CSS in `src/main/resources/META-INF/resources`, imported from one `styles.css`, no `@CssImport`, no themes folder. Component customisation through Aura's published tokens first, part selectors second, and anything that needed a part selector gets a comment naming the component version it was written against, because part names move between betas and that comment is what makes the break findable.

## The app icon and the sign in page

The app icon is `icons/favicon.svg`, a blue shield filling its whole height with the Vaadin mark in white, drawn upright: a curly bracket lying on its side above a narrow V. It is the favicon, it opens the customer app bar, it heads the internal left panel beside the brand, and it sits on the sign in card, always at its own 32 pixels and on whole pixels, because scaled or on a half pixel it blurs on a 1x screen.

The sign in page stands alone, with no app bar and no breadcrumbs, D-276: a white card 380 pixels wide with a light edge, 14 pixel corners and a soft shadow, on a grey ground, dark navy in dark mode, its top placed on a whole pixel near the middle of the window. In it: the icon, "Security status" and the scheme button as a small ring; the heading Sign in; the lead "Use your work account to continue."; Vaadin's `LoginForm` with its own title hidden, the labels Username and Password with no required mark, 44 pixel fields and the button Continue in the accent blue; and the footer "Trouble signing in? Contact your account manager.", D-277. Its styles are the `.login-*` rules in `styles/common/shell.css`.

## The customer bar

From the start: the app icon, the trail Vaadin › My account › the route, the measured chip, the scheme button, for a team member the shield that leads to the team's pages (shown only below 800 pixels, where the side navigation is hidden, D-253), and Sign out as an icon. Every icon button has a Vaadin tooltip and an accessible name, D-248.

# 05 Theming

## The theme

Aura, as the walking skeleton ships it, with design tokens over component CSS. The Figma design of the customer page remains the structural reference, what sits above what, what is a card, what folds, but this application reproduces its answers and its hierarchy with Vaadin components rather than its pixels. That trade is stated in `00-overview.md`: fighting the components to clone a hand rolled page inverts the point of the exercise.

## Light and dark

- A Switch in the app bar, both halves of the application.
- The default follows the system. A reader who disagrees with their system can say so, and the choice is remembered through the typed `WebStorage` accessors, because a preference query cannot be argued with: that is rule 6.8.6 of the source spec.
- The choice applies before the first paint, or the page paints light and repaints dark, which is worse than no switch: rule 6.9.1.

## The vocabulary of color

Three states and four severities, each a token pair defined once in `styles.css` and consumed by name:

| Token | Meaning |
| --- | --- |
| `--status-secure` | Secure, green range |
| `--status-fix` | Fix available, amber range |
| `--status-unresolved` | Unresolved, red range |
| `--severity-critical` to `--severity-low` | The four severity badges |

State words always accompany the color, Secure, Fix available, N findings open, because color alone fails a color blind reader and a printed ticket.

## The customer page

One centered column, at most 60rem with 1.5rem of padding, which is the prototype's 912 pixels of content, D-170. In it, in this order on every customer route: the H1, the purpose in the secondary text color, the lookup in a box on the container background, then the route. Home's line cards are tinted by state from the three status tokens through `color-mix`, and in Recently reviewed only the risk verdict takes the error variant of the badge, D-172.

## Layout rules that carry over from the source spec

- A long value is never laid out underneath its own label, and a label column never takes a whole phone's width.
- The page survives 390 pixels, including coordinates and advisory ids that are one unbroken word: `overflow-wrap` on the value cells, horizontal scroll only inside tables that genuinely cannot fold.
- Severity and verdict badges are quieter than the text they qualify. A row reads as words with marks beside them, not as a row of marks.
- Form labels in the workbench sit beside their fields on wide screens, through FormLayout's 25.4 side label support with `LabelTextAlign`, and above them on narrow ones.

## Responsive views

The views listed here are verified at phone width during UI verification, the rest are desktop screens by declaration: HomeView, VersionView, CveView, ScanView, AdvisoriesView. The explorer and workbench are team screens used on laptops, and claiming phone support for a verdict queue nobody will work from a phone is polish spent where nobody stands.

## The internal shell

The internal pages follow the prototype's distribution, D-203: a left panel about 20rem wide that holds the explorer's filter and products and nothing else, overlaying below the App Layout's own breakpoint; a bar over the content with the drawer toggle, the page's H1 at 18 pixels, the internal destinations as quiet links, the measured chip, the dark switch, the Assistant button and Sign out; the content; and on the right, when opened, the explorer's legend or the assistant panel, each about 22rem wide. Section headings inside the explorer, "Series", "Filter by advisory, version or dependency", "Narrow the list to your application", "From Vaadin", "With Spring Boot", are small capitals in the secondary text color, as the prototype draws them. A series row is marked at its start by its state's status color, and the risk and unassessed groups inside it are faintly tinted in the same colors. Product rings carry their number and take one of twelve palette colors by the product's place in the registry; a ring with nothing to count is grey. All of it is in `styles/explorer.css` and `styles/shell.css`.

## CSS discipline

All CSS in `src/main/resources/META-INF/resources`, imported from one `styles.css`, no `@CssImport`, no themes folder. Component customisation through Aura's published tokens first, part selectors second, and anything that needed a part selector gets a comment naming the component version it was written against, because part names move between betas and that comment is what makes the break findable.

REPO: vaadin/web-components
PREFIX: PiT 25.4:
TITLE: Aura's app layout inset is 1.5vmin unrounded, so the drawer's content sits on fractions of a pixel
---
### Description

Aura sets the app layout inset straight from the viewport, without rounding:

```css
/* packages/aura/src/components/app-layout.css */
--aura-app-layout-inset: 1.5vmin;
```

and pads the drawer from it:

```css
vaadin-app-layout:not([overlay])::part(drawer) {
  padding-inline: max(0px, var(--aura-app-layout-inset) - var(--vaadin-padding-s));
}
```

`1.5vmin` is a whole number of pixels only when the shorter side of the window is a multiple of 200. In a 1728 × 963 window it is 14.445px, so the drawer is padded 6.445px and everything in it starts at x 22.4375 instead of 22. The padding tokens in `size.css` are rounded, `round(var(--aura-base-size) * 0.5 * 1px, 1px)`; this one is not.

This is not specific to 25.4: it measures the same on 25.3.5, and the line is the same in 25.0.0, 25.2.0 and 25.3.5. It was found while testing 25.4.

### Why it matters

On a 1x screen, content placed on a fraction of a pixel is drawn blurred. It shows most on a small vector icon, the brand mark at the top of the drawer, which is where nearly every application puts its logo. Text gets away with it, so the cause is hard to spot: the icon looks pixelated in the drawer and sharp anywhere else.

### Expected

The inset rounded to a whole pixel, as the padding tokens are:

```css
--aura-app-layout-inset: round(1.5vmin, 1px);
```

### Workaround

The drawer's inline padding set to a whole number, with a selector as specific as Aura's: `vaadin-app-layout.my-layout:not([overlay])::part(drawer) { padding-inline: 6px; }`.

### Reproduce

The project has two routes with the same app layout and a 32 pixel `SvgIcon` at the top of the drawer. `/` uses Aura as it ships; `/rounded` sets `--aura-app-layout-inset: round(1.5vmin, 1px)` on the layout and nothing else. Each page measures itself and prints the window size, what 1.5vmin comes to, the drawer's `padding-inline-start` and the icon's `getBoundingClientRect().x`; Measure again repeats it after resizing.

1. Run the project and open `http://localhost:8128/` in a window wider than 800 and taller than 600 pixels, where Aura keeps the inset. Make the height a number that is not a multiple of 200, for example 963.
2. Read the line at the top of the content. Then open `/rounded` and read it again.

In a 1728 × 963 window, Chrome on macOS:

| Route | 1.5vmin | Drawer `padding-inline-start` | Icon x |
| --- | --- | --- | --- |
| `/` | 14.445px | 6.445px | 22.4375 |
| `/rounded` | 14.445px | 6px | 22 |

The issue reproduces when the first row shows a padding and an x that are not whole numbers. On a 1x display, zooming into the two icons with a screenshot tool shows the first one softer at its edges.

`-Dvaadin.version=25.3.5` prints the same two rows, with `vaadin-app-layout` reporting 25.3.5 in the page. The inset has been `1.5vmin` since 25.0; 25.2.0, 25.3.5 and `main` carry the same line.

### Getting the project

[`28-aura-app-layout-inset.zip`](https://github.com/vaadin/hackathon-25-4/raw/COMMIT_SHA/specs/issues/projects/28-aura-app-layout-inset.zip), 4 KB, sources only. Unzip it and:

```
mvn spring-boot:run
```

Found on 25.4.0-beta2, and the same since 25.0.

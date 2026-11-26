# The demo material

The stills in `screenshots/` and the recordings in `video/` that the root `README.md` shows, and the two scripts that make them. Both drive the application the way a presenter does, against the committed fixtures rather than live data, so every number is the same on every run, and neither presses anything that writes: the workbench's handover to GitHub is never submitted.

## Making them again

Serve the fixtures and start the application against them, with the development tools off so they stay out of the frame:

```bash
mkdir -p /tmp/pages && ln -sfn "$PWD/src/test/resources/pages" /tmp/pages/cve-status
(cd /tmp/pages && python3 -m http.server 8082 --bind 127.0.0.1) &
PORT=8090 SECURITYSTATUS_DRAFTS_DIR=$(mktemp -d) VAADIN_LAUNCH_BROWSER=false ./mvnw \
  -Dspring-boot.run.arguments="--securitystatus.pages.base-url=http://localhost:8082/cve-status/ --vaadin.devmode.devTools.enabled=false"
```

Then, with Playwright on the path (`npm i -g playwright`, and a `node_modules/playwright` link beside these scripts if node cannot resolve it):

```bash
node demo/record.mjs customer   # writes video/<hash>.webm
node demo/record.mjs team
node demo/shots.mjs             # writes shots-clean/ and captions.json
```

The recordings come out as WebM, which Keynote and PowerPoint do not read. For a deck:

```bash
ffmpeg -i video/customer.webm -vf scale=1280:-2 -c:v libx264 -crf 30 -pix_fmt yuv420p \
  -movflags +faststart -an video/customer.mp4
```

The stills are shrunk to 1200 pixels and encoded as WebP, which is what keeps sixteen screens under a megabyte in the repository:

```bash
magick shot.png -resize 1200x -strip /tmp/r.png && cwebp -q 82 /tmp/r.png -o screenshots/shot.webp
```

## What `overlay.js` is for

A headless browser records no pointer and writes no narration, so a viewer sees fields fill and panels open with nothing causing it. `overlay.js` is injected into every document the walk opens and draws two things: a cursor that follows the real mouse events Playwright sends, and a caption the script sets per step. It lives on the `documentElement` rather than the body, because Vaadin writes the application into the body when it boots and takes anything already there with it.

## Traps worth knowing

- **Escape in the version field withdraws the question** and goes home, which is what the specification asks for. Press Enter to close the version list, never Escape, or the walk ends up back on the front page.
- **The viewport must not be forced wider than the window.** A forced viewport is pinned for the life of the page, and the capture is then taken on a canvas nobody is looking at.
- **The product chips in the explorer are links**, not checkboxes, because every filter control writes the selection into the address.
- **A live model takes twenty to twenty five seconds a turn.** The team recording is mostly waiting for two of them. Without `OPENAI_API_KEY` the assistant replays recorded answers instantly, which is the faster take and the one that cannot surprise you on stage.

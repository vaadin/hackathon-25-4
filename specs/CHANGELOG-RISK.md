# Preview and beta API register

Everything in this application that depends on an API that is preview, experimental, beta or otherwise likely to move before 25.4 goes final. When the release lands, this is the checklist. Epic 01 spikes every row marked unverified and corrects this table before the other epics start.

| API | Status on 25.4.0-beta2 | Used in | What to recheck at GA |
| --- | --- | --- | --- |
| PDF Viewer | Experimental, has its own feature flag provider and throws without the flag | Epic 06 audit preview | Flag removal, toolbar actions API, zoom API |
| AI components, core and controllers | Feature flag, free core, commercial controllers | Epics 10 and 11 | Flag removal, whether the free and commercial split moved |
| Content Security Policy | Verified on beta2 in US-1.4: the `strict` mode sends its nonce policy in production mode only and on the bootstrap page only, so this application sends its own enforced policy and keeps the mode at `warn`. Verified with push and sign in. Not yet verified against uploads, the AI panel and the PDF viewer | Epic 01, everything after | Whether `strict` becomes usable, which would let `unsafe-inline` and `unsafe-eval` go. Each later epic re checks `CspIT` and the `warn` log |
| Typed JS API, `JsExpression`, `JsSnippet` | New in 25.4. Verified on beta2 in US-4.4, in development and production mode: `Element.ScrollIntoViewJs` through `executeJs(Class)` brings the fixes list and a linked finding into view, and a declared `@JsDefinition` scrolls to the top, with nothing logged by the `warn` mode in a development run | Epics 04 and 05 | Signature stability, return value mapping |
| `Download` API | Verified on beta2 in US-2.4 for `DownloadHandler.fromInputStream` with a file name: the body streams, the disposition is `attachment` with the name, the name is the last segment of the address, and `DownloadResponse.error(404)` reaches the browser as a 404. The servlet container respells a content type that has a charset, `text/csv; charset=utf-8` leaves as `text/csv;charset=utf-8`. `Download.onClick` is not used yet | Epics 02 and 06, through `base/ui/Downloads` | Content disposition behaviour, URL postfix |
| `DisableOnClickMode` | New in 25.4. Verified on beta2 in US-3.1: `UNTIL_RESPONSE` disables the button at the click and enables it with the next response. It refuses `bindEnabled` on the same button, draft 13 | Epic 03, the Check button | Whether `bindEnabled` is accepted beside `UNTIL_RESPONSE`, which would delete the effect in `Lookup.checkButton` |
| Declared DOM event filters, `DomEventFilter` through `JsSnippets` and `setFilterExpression` | New in 25.4. Verified on beta2 in US-3.1, in development and production mode: the filter runs in the browser, only a passing event reaches the server, `preventDefault()` on the registration applies to passing events only. `ShortcutRegistration.ShortcutFilterJs` exists and is internal, draft 12b | Epic 03, the slash key and Escape in `Lookup.KeysJs` | Whether shortcuts gain a public filter, which would move the slash back to the shortcut API |
| `NavigationStartedEvent`, `NavigationEndedEvent` | New in 25.4 | Epic 12 | Outcome enum completeness |
| `NativeForm` | New in 25.4 | Epic 10 | Method and enctype handling of a GET with a long query |
| `UploadCapture` | New in 25.4 | Epic 11 | Whether capture reaches the message input's attachments |
| FormLayout side labels, `LabelTextAlign` | New in 25.4 | Epic 10 | Part names, responsive behaviour |
| Switch | Since 25.3. Verified on beta2 in US-1.3: renders and reports its value with no feature flag set | Epics 01 and 08 | Nothing known |
| Breadcrumbs | Since 25.2. Verified on beta2 in US-1.1: renders and navigates with no feature flag set | Epic 01 | Whether `ROUTER` mode learns to take a fixed leading item, which would delete the manual trail in `CustomerLayout` |
| Master Detail Layout | Since 25.2 | Epic 08 | Overlay breakpoint behaviour |
| `browserless-test-spring` | Versioned independently, managed by the BOM | Every browserless test | Never hand pin |
| Dev loop CLI | Verified on beta2 in US-1.5: `vaadin:install-dev-cli` exists under the `vaadin` prefix. Costs measured in `specs/10-dev-loop.md` | Tooling | Nothing known |

## Rules that follow from this table

1. Every API in this table is used through a thin adapter in `base` where one exists naturally, so a signature change is a one file fix. A view level component like the PDF viewer is used behind its flag with the screen degrading to the plain download when the flag is off.
2. Flags live in the committed `vaadin-featureflags.properties`, so a fresh clone runs the same application.
3. Epic 01's spike turns every "unverified" above into a fact, and an API that turns out absent or broken becomes a `FEEDBACK-25.4.md` row the same day.

## Project risks

| Risk | Mitigation |
| --- | --- |
| Beta APIs move between beta2 and GA | The adapter rule, one pinned `vaadin.version` property |
| GitHub Pages unreachable at the demo | The cache serves stale with the age named. A cold start needs one fetch, so the demo machine warms the cache before the slot |
| The data under the demo changes overnight | The demo script leans on `/cases`, which finds whatever exhibits each state that day |
| The assistant is not deterministic | The default run has no model at all. Cassettes in the gate, one live tagged test |
| Commercial licensing blocks a machine | Development and the whole test suite run with no licence; a production build needs one, deliberately |
| Thirteen epics do not fit the time | Phase 1 is demoable alone, the cut order is in `00-overview.md` |
| The spec and the code drift | `SpecConsistencyTest`, and polish step two exists precisely to re true the documents |

# To post

One row per draft. `post.sh` sends the rows whose status is `ready`, dry run by default. A draft is ready when its reproducer has been run, the zip is committed and the duplicate search is written here.

Status is one of `draft`, `ready`, `posted`, `not ready`, `withdrawn`. After posting, the draft name becomes a link to the issue.

| Draft | Repo | What it says | Kind | Reproducible | Zip | Status | Duplicate search |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `01-breadcrumbs-leading-item.md` | vaadin/flow-components | Breadcrumbs in ROUTER mode cannot take a leading item that is not a route | Request | Yes | `01-breadcrumbs-root.zip` | ready | `Breadcrumbs` in flow-components and web-components: nothing on a fixed item. Nearest is flow-components#9751, icons |
| `03b-comment-on-flow-25576.md` | vaadin/flow | Still on beta2. A declared sheet outside the public list fails too, and the same default hides the not found view | Comment | Yes | `03-security.zip` | ready | `anyRequest` in flow: this is flow#25576 from the 25.3 hackathon, open, answered by a maintainer |
| `07b-comment-on-flow-26391.md` | vaadin/flow | The first frame measured with a remembered dark scheme, and why the value has to be in the bootstrap response | Comment | Yes | `07-color-scheme.zip` | ready | `ColorScheme` in flow: flow#26391 is the store, flow#26390 is the accessor, both open |
| `08-csp-page-strict-mode.md` | vaadin/docs | The Content Security Policy page does not say how the strict csp mode behaves | Docs | Yes, two curl commands | `08-csp.zip` | ready | `CSP` and `Content Security Policy` in docs: nothing. In flow the behaviour is specified in #26149 and #26660, and #26688 is the Spring Security half |
| `10-bootstrap-page-without-security-headers.md` | vaadin/flow | The bootstrap page is served without the headers Spring Security writes | Bug | Yes, two curl commands | `10-security-headers.zip` | ready | `HeaderWriterFilter`, `X-Content-Type-Options`, `X-Frame-Options DENY` in flow: nearest is flow#26688, which is the strict csp mode only |
| `02-notfound-outside-layout.md` | none | The not found view renders outside the automatic layout | none | Yes, and documented | none | withdrawn | `handle-errors` in the documentation |
| `04-role-refusal-reads-as-not-found.md` | none | A signed in user without the role sees the not found view | none | Yes, and documented | none | withdrawn | `access-control/error-views` in the documentation |
| `06-expired-session-strategy.md` | none | VaadinExpiredSessionStrategy only acts under concurrency control | none | Not built, documented | none | withdrawn | `security-configuration` in the documentation |

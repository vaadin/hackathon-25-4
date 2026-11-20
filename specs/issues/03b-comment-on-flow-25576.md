REPO: vaadin/flow
COMMENT-ON: 25576
---
Still the same on 25.4.0-beta2, measured on a fresh project with the setup the documentation shows and nothing added. Two things worth adding to the open questions above.

**A declared sheet outside the public list fails the same way as an imported one.** `@StyleSheet("styles/main.css")` and `@StyleSheet("root.css")` on the application shell are both refused, so a public page of a secured application is unstyled unless its one sheet is named exactly `styles.css`:

| Request | Anonymous | Anonymous, `Sec-Fetch-Dest: style` | Signed in |
| --- | --- | --- | --- |
| `/root.css`, declared | 302 to `/login` | 401 | 403 |
| `/styles/main.css`, declared | 302 to `/login` | 401 | 403 |
| `/styles/parts.css`, imported | 302 to `/login` | 401 | 403 |

The 401 is the fix from this issue doing its job: the console now names the file. The styling page still does not say that a sheet needs a rule.

**The same default hides the not found view.** This is the last open question above, seen from the router's side. An address that matches no route never reaches `RouteNotFoundError`:

| Request | Anonymous | Signed in |
| --- | --- | --- |
| `/missing` | 302 to `/login` | 403, empty body, the browser's own error page |
| `/admin`, a real route the user may not see | 302 to `/login` | 200, the not found view |

So a signed in user is shown the application's not found view for a view that exists and a blank browser error for one that does not, which is the opposite of what the access control page describes. On a public site a mistyped address sends an anonymous reader to a login form.

What we did: `configurer.anyRequest(rule -> rule.permitAll())`, leaving the views to navigation access control. That gives up the deny by default for everything that is not a view, which an application with other endpoints would not want.

### Reproduce

```
curl -s -o /dev/null -w '%{http_code} %{redirect_url}\n' localhost:8103/styles/main.css
curl -s -o /dev/null -w '%{http_code} %{redirect_url}\n' localhost:8103/missing
```

Then sign in as `user` / `user` and open `/missing` and `/admin`.

[`03-security.zip`](https://github.com/vaadin/hackathon-25-4/raw/COMMIT_SHA/specs/issues/projects/03-security.zip), 5 KB, sources only, `mvn spring-boot:run`, port 8103.

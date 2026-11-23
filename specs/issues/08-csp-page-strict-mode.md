Withdrawn, replaced by a comment. The two open pull requests vaadin/docs#6495 and vaadin/docs#6687 document all three points: no header in development mode, `IndexHtmlResponse.getContentSecurityPolicy()` in an `IndexHtmlRequestListener`, and the other responses keeping the Spring Security policy. The measurement went to #6495 as `08b-comment-on-docs-6495.md`, posted.

REPO: vaadin/docs
TITLE: The Content Security Policy page does not say how the strict csp mode behaves
---
### Description

`flow/security/protections/content-security-policy` describes the manual nonce listener in full and mentions the `csp` parameter in its last paragraph, "Checking Compatibility". Three things an application meets the moment it sets `vaadin.csp=strict` are not on the page.

1. **Nothing is sent in development mode.** The header appears only in production mode. The page says so about the manual listener, not about the parameter.
2. **Where the policy is adjusted.** `IndexHtmlResponse.getContentSecurityPolicy()` in an `IndexHtmlRequestListener`, and `getCspNonce()`. `ContentSecurityPolicy` and `CspMode` have javadoc and no mention here. We found the hook by reading `IndexHtmlRequestHandler`.
3. **Which responses carry it.** Only the bootstrap page. A stylesheet, an error page or a Spring MVC endpoint of the same application has no policy unless the application sends one itself.

### Why it matters

"Develop under the policy" is the reason to turn it on early, and in development there is no policy: an application can set `strict`, see nothing break, and meet every violation on the first production build. With no pointer to the hook, the obvious next move is to set the header in a filter, which strict mode then refuses with an exception.

### Expected

A short section on the page for the `csp` parameter: the three modes, that the header is production only and why, the listener hook with a two line example, and that other responses are the application's to cover.

### Workaround

We send our own enforced policy on every response through Spring Security's `headers().contentSecurityPolicy(...)`, built with `ContentSecurityPolicy`, with `'unsafe-inline'` and `'unsafe-eval'` in `script-src`, and keep the parameter at `warn`.

### Reproduce

The project sets `vaadin.csp=strict` and nothing else.

| Run | `curl -s -D - -o /dev/null localhost:8108/` |
| --- | --- |
| `mvn spring-boot:run` | No `Content-Security-Policy` header |
| `mvn package -DskipTests`, then `java -jar target/csp-1.0-SNAPSHOT.jar` | `Content-Security-Policy: default-src 'self'; script-src 'nonce-...' 'strict-dynamic'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self' data:; connect-src 'self'; worker-src 'self'; object-src 'none'; base-uri 'self'` |

### Getting the project

[`08-csp.zip`](https://github.com/vaadin/hackathon-25-4/raw/COMMIT_SHA/specs/issues/projects/08-csp.zip), 2 KB, sources only. Unzip it and:

```
mvn spring-boot:run
```

Found on 25.4.0-beta2, flow 25.4.0-beta1.

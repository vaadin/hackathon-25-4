REPO: vaadin/flow
TITLE: The bootstrap page is served without the headers Spring Security writes
---
### Description

In an application secured with `VaadinSecurityConfigurer`, every response carries Spring Security's headers except the bootstrap page, in development mode and in production mode alike.

```java
http.headers(headers -> headers
        .contentSecurityPolicy(policy -> policy.policyDirectives("frame-ancestors 'none'")));
return http.with(VaadinSecurityConfigurer.vaadin(), vaadin -> { }).build();
```

| Header | `/`, the bootstrap page | Any other response, here `/missing.css` |
| --- | --- | --- |
| `Content-Security-Policy` | absent | `frame-ancestors 'none'` |
| `X-Frame-Options` | `SAMEORIGIN` | `DENY` |
| `X-Content-Type-Options` | absent | `nosniff` |
| `Cache-Control` | `no-cache, no-store` | `no-cache, no-store, max-age=0, must-revalidate` |

### Why it matters

The bootstrap page is the document the policy exists for: it is the one that runs scripts and the one that can be framed. An application that configures a policy the standard Spring way gets it on stylesheets and error pages and not on the page, and nothing says so. `X-Frame-Options: DENY`, Spring Security's default, is quietly `SAMEORIGIN` on that page. A scanner run against any path but `/` reports the application as covered.

This is not the strict csp mode of #26688: no csp mode is set here, and it is every header, not one.

### Expected

Either the bootstrap response keeps the headers the security filter chain writes, or the security page says that it does not and names the hook.

### Workaround

Set the header again from an `IndexHtmlRequestListener`:

```java
event.addIndexHtmlRequestListener(response ->
        response.getVaadinResponse().setHeader("Content-Security-Policy", policy));
```

### Reproduce

1. `mvn spring-boot:run`. The first seconds answer with the "dev mode pending" page, which does carry the headers, so wait until `X-DevModePending` is gone.
2. `curl -s -D - -o /dev/null localhost:8110/` prints the left column.
3. `curl -s -D - -o /dev/null localhost:8110/missing.css` prints the right column.
4. `mvn package -DskipTests`, then `java -jar target/security-headers-1.0-SNAPSHOT.jar`, and the same two commands print the same two columns.

### Getting the project

[`10-security-headers.zip`](https://github.com/vaadin/hackathon-25-4/raw/COMMIT_SHA/specs/issues/projects/10-security-headers.zip), 3 KB, sources only. Unzip it and:

```
mvn spring-boot:run
```

Found on 25.4.0-beta2, flow 25.4.0-beta1.

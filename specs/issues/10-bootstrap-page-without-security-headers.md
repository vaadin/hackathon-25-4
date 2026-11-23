REPO: vaadin/flow
TITLE: Since 25.0 the bootstrap page is served without the headers Spring Security writes
---
### Description

In an application secured with `VaadinSecurityConfigurer`, every response carries Spring Security's headers except the bootstrap page. It worked on 24.9 and stopped on 25.0, and it is the same in development and production mode.

```java
http.headers(headers -> headers
        .contentSecurityPolicy(policy -> policy.policyDirectives("frame-ancestors 'none'")));
return http.with(VaadinSecurityConfigurer.vaadin(), vaadin -> vaadin.anyRequest(rule -> rule.permitAll())).build();
```

Headers of `/`, the bootstrap page, with that one configuration:

| Version | `Content-Security-Policy` | `X-Frame-Options` | `X-Content-Type-Options` |
| --- | --- | --- | --- |
| 24.9.26, Spring Boot 3.5.8 | `frame-ancestors 'none'` | `DENY` | `nosniff` |
| 25.0.13, Spring Boot 4.0.10 | absent | absent | absent |
| 25.3.5 | absent | `SAMEORIGIN` | absent |
| 25.4.0-beta2 | absent | `SAMEORIGIN` | absent |

Every other response of the same application, a stylesheet or a 404, carries all three on every version. The `SAMEORIGIN` on 25.3 and later is Vaadin's own default from #24525, not Spring Security's `DENY`.

### Why it matters

`Content-Security-Policy` and `X-Frame-Options` act only on documents, and the bootstrap page is the one document of a Flow application. So the policy an application configures the standard Spring way reaches the scripts and stylesheets, where it does nothing, and misses the page it exists for. Between 25.0 and 25.2 the page could be framed by any site; since 25.3 by any page of the same origin, against a configured `DENY`.

Nothing fails, so it goes unnoticed: a check with `curl` or the network tab on any path but `/` shows the headers, and in development the "dev mode pending" page of the first seconds carries them too. Only the three headers above were measured; others Spring writes for documents, such as `Referrer-Policy`, are likely affected the same way.

### Expected

The bootstrap response keeps the headers the security filter chain writes, as on 24.9.

### Workaround

Set the header again from an `IndexHtmlRequestListener`:

```java
event.addIndexHtmlRequestListener(response ->
        response.getVaadinResponse().setHeader("Content-Security-Policy", policy));
```

### Reproduce

A Spring Boot application with `vaadin-spring-boot-starter`, `spring-boot-starter-security`, the configuration above as its only `SecurityFilterChain`, and one anonymous route:

```java
@Route("")
@AnonymousAllowed
public class HomeView extends Div {
}
```

1. `mvn spring-boot:run`, and wait until the response no longer has `X-DevModePending`.
2. `curl -s -D - -o /dev/null localhost:8080/` prints the row of the table for that version.
3. `curl -s -D - -o /dev/null localhost:8080/missing.css` prints all three headers.

The same project was run on each version in the table, changing only the Vaadin and Spring Boot versions.

Found on 25.4.0-beta2.

### Getting the project

[`10-security-headers.zip`](https://github.com/vaadin/hackathon-25-4/raw/COMMIT_SHA/specs/issues/projects/10-security-headers.zip), 3 KB, sources only. Unzip it and:

```
mvn spring-boot:run
```

It listens on 8110, so use that port in the two `curl` commands. To compare versions, change `vaadin.version` and the Spring Boot parent in the `pom.xml`.

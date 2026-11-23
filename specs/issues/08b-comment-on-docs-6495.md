REPO: vaadin/docs
COMMENT-ON: 6495
---
Checked this against 25.4.0-beta2 (flow 25.4.0-beta1) in the 25.4 hackathon, and the page describes what happens. A project that sets only `vaadin.csp=strict`:

| Run | `curl -s -D - -o /dev/null localhost:8080/` |
| --- | --- |
| `mvn spring-boot:run` | No `Content-Security-Policy` header |
| `mvn package -DskipTests`, then `java -jar target/*.jar` | `default-src 'self'; script-src 'nonce-...' 'strict-dynamic'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self' data:; connect-src 'self'; worker-src 'self'; object-src 'none'; base-uri 'self'` |

One thing worth one more sentence: the development mode paragraph is the one a reader needs first, because it changes how they work. An application that turns `strict` on to develop under the policy sees nothing break in development and meets every violation on its first production build. Moving that paragraph up, or saying in it that `warn` is the mode to develop with, would save that surprise.

To reproduce: any Spring Boot project on 25.4.0-beta2 with `vaadin.csp=strict` in `application.properties` and one route.

The project: [`08-csp.zip`](https://github.com/vaadin/hackathon-25-4/raw/COMMIT_SHA/specs/issues/projects/08-csp.zip), 3 KB, sources only, `mvn spring-boot:run`. It listens on 8108. For the production row, `mvn package -DskipTests` and `java -jar target/csp-1.0-SNAPSHOT.jar`.

# Development

## Build & Run Commands

```bash
./mvnw                            # Run in dev mode (default goal: spring-boot:run)
./mvnw clean package -Pproduction  # Production build, needs the Vaadin licence (JAR in target/)
./mvnw verify                     # The gate: unit and browserless tests, no licence, key or network
./mvnw verify -Pit                # Adds the browser tests on 8081
./mvnw test -Dtest=ClassName      # Run a single test class
```

The app runs on port 8080 (configurable via `PORT` env var).

## Shipping the customer UI without the team

The team's pages are separable, D-254. Delete these and the application builds and runs with the customer pages alone:

```bash
rm -rf src/main/java/com/vaadin/securitystatus/team \
       src/main/java/com/vaadin/securitystatus/ui/team \
       src/main/resources/META-INF/resources/styles/team
# remove the styles/team imports at the end of styles.css
# remove the team's tests under src/test/java/.../team and .../ui/team
# remove the team's specifications: features and epics 08 to 12, and the rows of
# 01-foundation.md and US-1.2 that ShellSecurityBrowserlessTest verifies
```

No test outside the team's folders names a team class, which `TeamIsRemovableTest` checks. The browser tests walk the team's pages by address and lose their team cases: `AssistantPanelIT` and `ExportsProxyIT` whole, `CspIT` and `DenyByDefaultIT` in part.

## Without a Vaadin licence

The scan screen's audit report preview uses the PDF Viewer, a commercial component. On a machine with no Vaadin licence the build and the tests still pass, and the application boots and works, but in development mode opening "Preview the audit report" shows "No license found" in the viewer and Vaadin's trial dialog over the page. Start the trial or sign in with a licence to see the preview; downloading the audit report works either way. The diagnostics board, a `Dashboard`, is commercial too and behaves the same way. A build without a licence that creates an application bundle shows Vaadin's commercial banner instead, through `commercialWithBanner` in `pom.xml`.

## Docker

To build a Docker image, run:

```bash
docker build -t my-application:latest .
```

If you use commercial components, pass the license key as a build secret:

```bash
docker build --secret id=proKey,src=$HOME/.vaadin/proKey .
```

A host that cannot mount build secrets, such as v-herd, takes build arguments instead: `proKey` carries the `proKey` value of `~/.vaadin/proKey`, and `mavenprofiles=production` leaves the model out, which a container with no `OPENAI_API_KEY` needs to start. A server licence key, `offlinekey`, does not cover `vaadin-pdf-viewer` 25.4.0-beta1 and fails the build. Without a key the build shows Vaadin's commercial banner instead of failing. The container is told how people sign in with `SPRING_PROFILES_ACTIVE`, `demo` or `cas`, and gets `OPENAI_API_KEY` when the image was built with the model. Under `demo` the accounts are `team` / `team` and `guest` / `guest` unless `SECURITYSTATUS_DEMO_TEAM_NAME`, `SECURITYSTATUS_DEMO_TEAM_PASSWORD`, `SECURITYSTATUS_DEMO_GUEST_NAME` and `SECURITYSTATUS_DEMO_GUEST_PASSWORD` name others. The image runs in 512 MB.

```bash
docker build --build-arg mavenprofiles=production -t security-status .
docker run -p 8080:8080 -e SPRING_PROFILES_ACTIVE=demo \
    -e SECURITYSTATUS_DEMO_TEAM_NAME=alice -e SECURITYSTATUS_DEMO_TEAM_PASSWORD=secret security-status
```

# Development

## Build & Run Commands

```bash
./mvnw                            # Run in dev mode (default goal: spring-boot:run)
./mvnw clean package              # Production build (JAR in target/)
./mvnw test                       # Run all tests
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
```

A few tests outside those folders also walk the team's pages and lose their team cases: `AbsenceSweepBrowserlessTest`, `DataOutageBrowserlessTest`, and the integration tests `AssistantPanelIT`, `CspIT`, `DenyByDefaultIT` and `ExportsProxyIT`.

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

A host that passes neither secrets nor environment to the container, such as v-herd, takes build arguments instead: `profile` bakes `SPRING_PROFILES_ACTIVE` into the image, `mavenprofiles=production` leaves the model out, which a container with no `OPENAI_API_KEY` needs to start, and `offlinekey` carries a server licence key. Without a key the build shows Vaadin's commercial banner instead of failing. The image runs in 512 MB.

```bash
docker build --build-arg profile=demo --build-arg mavenprofiles=production .
```

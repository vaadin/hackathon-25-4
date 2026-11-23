# Development

## Build & Run Commands

```bash
./mvnw                            # Run in dev mode (default goal: spring-boot:run)
./mvnw clean package              # Production build (JAR in target/)
./mvnw test                       # Run all tests
./mvnw test -Dtest=ClassName      # Run a single test class
```

The app runs on port 8080 (configurable via `PORT` env var).

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

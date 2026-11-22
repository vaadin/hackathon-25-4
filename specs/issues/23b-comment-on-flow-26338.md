REPO: vaadin/flow
COMMENT-ON: 26338
---
Still so on 25.4.0-beta2, and on 25.3.5, with a simpler trigger than the one in the description: a brand new route that only uses components the bundle already has. Found in the 25.4 hackathon, where a new `/diagnostics` view inside the application's `AppLayout` shell rendered without its shell in production.

The new route's class has no `loadOnDemand` key in the reused bundle at all. Its parent layout is not eager either, so opening the new route directly loads no chunk: `vaadin-app-layout`, `vaadin-side-nav` and every other component of the layout stay undefined, and a connector call on one of them throws, `TypeError: Cannot read properties of undefined (reading 'setItems')` in our application. The build says nothing beyond "A production mode bundle build is not needed".

### Why it matters

Adding a plain view to an existing application is the most ordinary change there is, and it ships a production build whose shell is broken on that view only, while development mode and every older view are fine. With `src/main/bundles/prod.bundle` committed, as the production build page and the generated README recommend, every developer and the CI build reuse the stale bundle.

### Reproduce

The project has a layout, `MainLayout extends AppLayout` with a `SideNav`, and two routes in it: `/a` with a `Button` and `/b` with only a `Paragraph`. `@JsModule("./hello.js")` on the app shell makes the build create an application bundle. The profile `without-b` leaves `BView` out of the compilation, which stands for the state before the route was written.

1. `mvn clean package -Pproduction,without-b`. A bundle is built and written to `src/main/bundles/prod.bundle`.
2. `mvn clean package -Pproduction`. The log says `A production mode bundle build is not needed`.
3. `java -jar target/stale-production-bundle-1.0-SNAPSHOT.jar` and open `http://localhost:8123/b` in Chrome. After three seconds, in the console:

```js
[customElements.get('vaadin-app-layout'), customElements.get('vaadin-side-nav')]
```

On 25.4.0-beta2 it printed `[undefined, undefined]`, and the bundle's `generated-flow-imports-*.js` contains the key of `com.example.AView`, `caba981a…`, and not the key of `com.example.BView`, `7fac8ad7…` (the SHA-256 of the class name). `/a` printed both constructors.

4. `mvn clean package -Pproduction -Dvaadin.force.production.build=true` and open `/b` again: both are defined and the key of `BView` is in the bundle.

Steps 1 to 3 with `-Dvaadin.version=25.3.5`, after deleting `src/main/bundles`, printed the same `[undefined, undefined]`.

### Expected

The validation in the description, with one more case: a route class whose chunk key is not in the bundle's `loadOnDemand` at all also makes the bundle stale. Failing that, a build line saying which routes the reused bundle does not know.

### Workaround

Delete `src/main/bundles/prod.bundle`, or build with `-Dvaadin.force.production.build=true`, after adding a route.

### Getting the project

[`23-stale-production-bundle.zip`](https://github.com/vaadin/hackathon-25-4/raw/COMMIT_SHA/specs/issues/projects/23-stale-production-bundle.zip), 4 KB, sources only. Unzip it and run the steps above; it listens on 8123.

Found on 25.4.0-beta2, and the same on 25.3.5.

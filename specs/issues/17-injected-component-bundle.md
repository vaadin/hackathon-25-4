REPO: vaadin/docs
TITLE: The @Uses note on the production build page covers reflection only, and an injected component is left out the same way
---
### Description

`flow/production/production-build`, under "Lazy Loading (On Demand)", says:

> Any components that are loaded using reflection should be named on the `Route` class using `@Uses` so that they're collected.

The scanner misses more than reflection. A route that receives a component from an injected Spring bean, through an interface, never names the implementation, so nothing the implementation creates is collected either:

```java
public interface Part { Component create(); }

@SpringComponent
class ComboPart implements Part {
    public Component create() { return new ComboBox<String>("Version"); }
}

@Route("")
public class MainView extends Div {
    public MainView(Part part) { add(part.create()); }
}
```

And nothing shows it while the application uses the pre-compiled bundle, which carries every component. It appears the day anything makes the build create an application bundle, a `@JsModule`, an npm package, a commercial component outside the pre-compiled set, and then every component reached that way stops rendering in production only.

### Why it matters

A layout that asks for its parts through interfaces, so a base package does not depend on a feature package, is ordinary Spring design. Here it ran green through three epics and broke in production when a PDF Viewer was added to an unrelated screen: the lookup in the application shell lost its combo box, tabs, text fields and upload, with only an error line in the server log. The note's word "reflection" gave no reason to look at it, and neither does the server's message, which says "Check that you have a reference to the component and that you are not using it only through reflection".

It is not a new case: in vaadin/flow#21118 a maintainer explains it for a component injected as a `Supplier<Settings>`, which the scanner sees only as `Supplier`, and says `@Uses` is required there. The behaviour is understood; the page and the message do not say it.

### Expected

The note says what the scanner follows, references in the bytecode reachable from routes, and names the usual ways a component escapes it: reflection, an injected bean or factory whose class no route names, a component created from configuration. And it says that the pre-compiled bundle hides the gap until an application bundle is built, so the place to check is a production build with one. The server's message could name the same cases.

### Workaround

`@Uses(Lookup.class)` on the root route, the one whose components load eagerly, naming the component the injected bean builds.

### Reproduce

The project is the three classes above plus one `@JsModule` of its own on the application shell, so the build has to create an application bundle.

1. `mvn -Pproduction package -DskipTests`. The build log says `A production mode bundle build is needed`, because of the one `@JsModule`.
2. Read `target/classes/META-INF/VAADIN/config/stats.json`. On 25.4.0-beta2 its `bundleImports` were `['Frontend/generated/jar-resources/ReactRouterOutletElement.tsx', 'Frontend/generated/jar-resources/hello.js', 'react', 'react-router']`: no `@vaadin/combo-box`.
3. Add `@Uses(ComboBox.class)` to `MainView` and build again: `bundleImports` then contains `@vaadin/combo-box/src/vaadin-combo-box.js`.
4. Remove the `@JsModule` instead, and the build says `A production mode bundle build is not needed`: the pre-compiled bundle is used, which the same page says carries every component, so the gap does not show. This step was read from the build log and not opened in a browser.

In the application where it was found, opening the page in the production build logged, for each component the bean builds, `The component class com.vaadin.flow.component.combobox.ComboBox includes '@vaadin/combo-box/src/vaadin-combo-box.js' but this file was not included when creating the production bundle`, and the page drew none of them.

Found on 25.4.0-beta2.

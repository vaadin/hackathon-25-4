REPO: vaadin/flow
TITLE: QueryParameters could keep the order the parameters were given in
---
### Description

`QueryParameters` writes its parameters in hash order, whatever order they were built or parsed in:

```java
QueryParameters.fromString("boot=1&ruled=1&disc=1&open=x").getQueryString();
// ruled=1&disc=1&boot=1&open=x
```

The same happens to a `LinkedHashMap` passed to the constructor, and to a `Location` parsed from an address, so a view that navigates with `UI.navigate(target, routeParameters, queryParameters)` writes the parameters into the browser's address in an order it did not choose. A reader who pasted `?boot=1&ruled=1` sees the address bar change to `?ruled=1&boot=1` on the first navigation inside the view.

### Why it matters

The order carries no meaning for HTTP, so nothing is wrong in the strict sense. But the browser's `URLSearchParams` and Spring's `UriComponentsBuilder` both keep it, and Flow is the one place in the chain that reorders.

An address that carries the whole state of a screen is meant to be read and shared: a support engineer pastes it into a ticket, and the next one compares it with the one they have. Two addresses that mean the same thing and read differently, and a link the application renders with `href` that differs from the address the same click writes, look like two different answers. Nothing in the application can fix the order, because it is lost inside the class.

### Expected

The order the parameters were given in is kept: insertion order for the constructor, and the order of the string for `fromString` and `Location`. A `LinkedHashMap` copy instead of a `HashMap` copy would do it.

### Workaround

None for the address bar. Our own links write the parameters in a fixed order, and tests compare the query as a map.

### Reproduce

Three calls, one line each. On 25.4.0-beta2:

```
fromString("boot=1&ruled=1&disc=1&open=x").getQueryString() = ruled=1&disc=1&boot=1&open=x
new QueryParameters(LinkedHashMap boot, ruled, disc, open).getQueryString() = ruled=1&disc=1&boot=1&open=x
new Location("version/24.9.21?boot=1&ruled=1&disc=1&open=x").getPathWithQueryParameters() = version/24.9.21?ruled=1&disc=1&boot=1&open=x
```

25.3.5 and 24.9.26 print the same three lines.

Found on 25.4.0-beta2, and the same on 25.3.5 and 24.9.26.

### Getting the project

[`14-query-parameters-order.zip`](https://github.com/vaadin/hackathon-25-4/raw/COMMIT_SHA/specs/issues/projects/14-query-parameters-order.zip), 3 KB, sources only. Unzip it and:

```
mvn test
```

It prints the three lines above; `-Dvaadin.version=25.3.5` or `24.9.26` prints the same.

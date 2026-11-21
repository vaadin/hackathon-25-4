REPO: vaadin/browserless-test
TITLE: A file served by Download.onClick cannot be fetched from a test, as an Anchor's can
---
### Description

`AnchorTester.download(OutputStream)` fetches what a link's `DownloadHandler` serves. The 25.4 `Download.onClick(component, handler)` serves the same kind of handler from a click on a button, and `ButtonTester` has nothing that reaches it:

```java
Download.onClick(button, DownloadHandler.fromInputStream(event -> ..., "triage.csv"));

test(anchor).download(out);   // works for a link
test(button).???              // no counterpart
```

The address exists on the server side, but only in an attribute of the button whose name carries a random id, `data-flow-download-<uuid>`, which no page documents.

### Why it matters

The documentation recommends `Download.onClick` for a download started from a button, so an application that follows it loses the browserless test of its export: the bytes, the content type and the file name can only be checked in a real browser, by reading that undocumented attribute or by intercepting the client's download function. A test that clicks the button checks nothing, since the download happens in the browser.

### Expected

In order of usefulness:

1. `ComponentTester.download(OutputStream)`, or a `DownloadTester` wrapping any component, that fetches what a `Download.onClick` registration on it serves, as `AnchorTester.download` does for a link.
2. A documented, stable way to read the address of a `Download.onClick` registration, for a TestBench test to fetch.

### Workaround

The export is tested in a browser only: the test reads the attribute whose name starts with `data-flow-download-` and fetches its value from the page, so nothing is saved to disk.

### Reproduce

`mvn test` runs one test against a view that offers the same handler twice, through an `Anchor` and through `Download.onClick` on a `Button`. It downloads through the link, then lists what the button offers a test. On 25.4.0-beta2 it printed:

```
AnchorTester.download: 45 bytes
ButtonTester methods about downloads: []
Button attributes: [id=button, data-flow-download-dfc235e2-2cbc-4618-a540-15b362888fed=VAADIN/dynamic/resource/0/dfc235e2-2cbc-4618-a540-15b362888fed/triage.csv], properties: []
```

and failed with `the file Download.onClick serves cannot be fetched from a test`.

### Getting the project

[`16-download-onclick-testing.zip`](https://github.com/vaadin/hackathon-25-4/raw/COMMIT_SHA/specs/issues/projects/16-download-onclick-testing.zip), 4 KB, sources only. Unzip it and:

```
mvn test
```

Found on 25.4.0-beta2, browserless-test 25.4.0-beta1.

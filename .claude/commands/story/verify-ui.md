# UI verification

Start the application and walk the story's acceptance criteria as a user would.

1. `./mvnw` in the background, or `.vaadin/vaadin-dev status` if the dev loop is running. Wait for the port to answer.
2. Log in with the account the story needs. Accounts are in `specs/04-security.md`.
3. For each acceptance criterion: navigate, act, and assert with `browser_snapshot`. Use the accessibility tree, not a screenshot, for anything you assert on. Take screenshots only as evidence for the review.
4. Check the console for errors after each step. A client side exception is a failure even when the screen looks right.
5. Check the same flow at phone width when the story touches a view listed as responsive in `05-theming.md`. **Resize in a tab of your own, never in the tab somebody is watching.** `browser_resize` sets the page viewport rather than the window, and it pins it for that page's whole life. Open a second tab, measure there, close it.
6. Measure a responsive breakpoint on the component, not on the window. Read `getBoundingClientRect().width` on the component and divide by 16 to compare it against a step declared in `em`.
7. Before concluding that the platform cannot do something, ask the MCP one deliberately naive question about the capability, in the plainest words available, and only then go back to the specific path. A probe written after forming a theory tests the theory, not the requirement.
8. Verify a download by fetching it, never by clicking it. `fetch(anchor.getAttribute('href'))` from the page gives the status, the content type, the `content-disposition`, the length and the bytes. A real click in an automated browser is a hazard.
9. The behaviour reference is the live pages at `https://vaadin.github.io/cve-status/` and `https://vaadin.github.io/cve-status/findings/`. When a criterion mirrors them, open the same question there and compare the answer, not the pixels.

Report per criterion: pass, fail with what you saw, or not applicable with the reason. Do not write "probably fine". If you did not verify it, say you did not verify it.

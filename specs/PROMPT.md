# The brief

What was asked for, before any of this existed. Kept because a specification is easier to judge when you can see the request it came from.

---

We are running a hackathon on Vaadin 25.4.0-beta2 and I want a real application out of it: a from scratch rebuild of the security status site we already run as two pure JavaScript apps on GitHub Pages. Read these first:

- <https://vaadin.github.io/cve-status/>, the customer "Security status" page. Lookup by version, by CVE id, or by an uploaded scanner report, plus the home cards and the advisories list.
- <https://vaadin.github.io/cve-status/findings/>, the internal explorer. Product chips, one row per maintained line, the release and Spring Boot pages, and the verdict workbench.
- The cve-status repo, `docs/requirements.md` section 6, which is the written spec those two apps were built against.
- The 25.3 hackathon repo, branch `manolo` of <https://github.com/vaadin/hackathon-25-3>. That is the method we are repeating: specs first, one unattended session per epic, and a feedback file that becomes verified issues.
- The projects start.vaadin.com generates with `type=sdd`, as a reference for how Vaadin itself frames spec driven development.

Then write the specifications for rebuilding both apps as one Vaadin application. Same behaviour, same deep links, same answers; the design language of the EE page reproduced with Vaadin, not pixel copied.

Four things I care about, in this order.

**Specifications first.** Everything needed to implement it: the domain, the data source, the architecture, security, theming, testing, tooling. Every feature with its acceptance criteria and its test cases in plain language, and every epic broken into stories. Enough that an agent can finish the application from them without asking me what I meant.

**Maximum use of what is new.** As much of 25.4 (beta2) and 25.3 as the application honestly needs: the AI family, signals, the typed JavaScript API, the Download API, the PDF viewer, navigation events, CSP, Switch, Markdown, the Table family. If a feature has no place here, leave it out and say so.

**The data comes from cve-status.** The rendered JSON the current UIs read is already public and ungated on GitHub Pages; the server fetches it there with a cache. No token, no snapshot, no offline mode. If the app needs something the JSON does not carry, the fix is an additive export in the cve-status pipeline, recorded first as a need.

**The working keys.** Anything missing, awkward or broken in 25.4 or 25.3 goes in a feedback file for the Vaadin teams, and a finding only becomes an issue after it is verified: reproduced in a minimal project, checked against existing issues, filed in the right repo. Finding those is half the point.

**MUST**

- use signals where possible with as much as possible of API on it
- use all new features in 25.3 and especially 25.4 (beta2)
- we have a license, use commercial components when possible
- we have an LLM key, use AI in the views where it is needed
- scaffold the project from the start.vaadin.com spec driven starter (Claude Code flavor) and judge that starter as part of the feedback
- implementation will be performed in one unattended session per epic, after specs are written and validated
- there would be a second interactive session for polishing the UI and behavior with 2 steps
  - code vibing: using dev-loop and implementation only (no run tests)
  - once polish is done, unattended session for updating specs, and adding tests
- the destination is branch `manolo` of this repo

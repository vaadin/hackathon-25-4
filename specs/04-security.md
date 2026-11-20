# 04 Security

## The model

Two audiences, one boundary. The customer screens are public, because the data behind them is public by the source project's own record and a sign in between a support ticket and its answer helps nobody. The internal screens, everything under `/findings` and `/diagnostics`, require authentication with the `SECURITY` role.

This replaces the JavaScript apps' client side gates, deliberately. Those gates ran in the reader's browser over bytes already fetched and their own documentation calls them a deterrent, not a boundary. A server can have the real thing, so it does, and reproducing the deterrent would be a step backwards recorded nowhere. What stays true from the source project: nothing is served behind the sign in that could not be served in front of it, because the JSON on Pages is public anyway. The role protects the team's workflow surfaces, not the data.

## Configuration

`VaadinSecurityConfigurer`, the 25.3 way, with the login view configuration it added. The login success handling added in 25.4 routes a signed in user back to the internal page they asked for, and failure stays on the login view with the component's own error state, nothing custom.

| Account | Password | Role | For |
| --- | --- | --- | --- |
| `team` | `team` | `SECURITY` | The demo and every internal test |
| `guest` | `guest` | none | Proving the refusal |

In memory, two of them, committed. An identity provider is out by decision: the 25.4 OAuth2 and Keycloak additions are real and the configurer has the seams for them, but standing up a provider for a demo proves the provider rather than this application. The decision row in `00-overview.md` names it so the omission is a choice somebody can reverse.

## Action level rules

Read only screens have no action rules. The two actions that exist:

| Action | Rule | Proven by |
| --- | --- | --- |
| Saving a verdict draft | `SECURITY` only, enforced in the service, not the view | `WorkbenchSecurityTest` |
| Opening the GitHub editor handoff | `SECURITY` only, and the YAML travels in the URL to github.com, where GitHub's own authentication takes over | `WorkbenchSecurityTest` |

The assistant runs under the session's authentication: its tools read the same service layer, so an unauthenticated session cannot reach workbench tools because the assistant surface itself is internal.

## Content Security Policy

25.4 ships `ContentSecurityPolicy` support and this application turns it on, in enforcing mode, as epic 01 work. A security page that answers security questions should not itself ship with an open policy, and doing it on a beta is exactly the kind of real world trial the hackathon wants: a Flow application with push, uploads, the AI components and a PDF viewer under one enforced policy. Every directive the components force us to loosen is a row in `specs/FEEDBACK-25.4.md`, because each one is a fact the Vaadin teams want.

Violation reports are logged and surfaced on the diagnostics view. `CspViolationException` paths get a test of their own.

## Headers and sessions

Whatever `VaadinSecurityConfigurer` and Spring Security default to stands, explicitly unconfigured, except the CSP above. `VaadinExpiredSessionStrategy`, new in 25.4, handles the expired session: the user lands back on the view they were on, with the lookup state gone and one notice, rather than a stack trace or a silent new session.

## What a test has to prove

Every rule here has a refusing test: `guest` is refused on each internal route, anonymous is redirected to login, the draft service throws for a caller without the role, and the CSP header is present on a rendered page. Security tests assert the refusal, not the absence of a link.

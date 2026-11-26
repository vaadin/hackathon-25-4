# 04 Security

## The model

Two audiences, one boundary, and since D-258 a sign in in front of both. Every page needs a signed in user; a verified vaadin.com email makes that user the team, and everything under `/findings` and `/diagnostics` needs that role on top.

The customer half was specified public, and was built public, because the data behind it is public by the source project's own record. It is behind the sign in now for a reason that is about where this application is going rather than about the data: the design puts it inside My account on vaadin.com, so the reader arrives already signed in, and a page that will live behind a sign in should be built behind one. The honesty rule survives the change: nothing is served behind it that could not be served in front of it.

This replaces the JavaScript apps' client side gates, deliberately. Those gates ran in the reader's browser over bytes already fetched and their own documentation calls them a deterrent, not a boundary. A server can have the real thing, so it does.

## Configuration

`VaadinSecurityConfigurer`, the 25.3 way, with the login view configuration it added. The login success handling added in 25.4 routes a signed in user back to the page they asked for, and failure stays on the login view with the component's own error state, nothing custom.

Two profiles, and a production start refuses to run without one of them named, which `ProfileGuard` enforces:

| Profile | Who signs in | For |
| --- | --- | --- |
| `demo`, the default | `team` / `team` with the role, `guest` / `guest` without it | Development, the demo and every test. In memory, committed on purpose |
| `cas` | vaadin.com's single sign on at sso.vaadin.com, through `spring-security-cas` and the CAS 3 ticket validator | Production |
| `cas-local` | The same against the staging server | Trying the real flow from a developer machine |

Under `cas`, `TeamRule` reads the released attributes and grants the role to a verified email on the team domain. Single log out is not wired: signing out here signs out of CAS. No real sign in through sso.vaadin.com has been made yet, only against a stubbed ticket validator, and what the production deployment still needs from the vaadin.com web team is P-21 in `NEEDS-A-PERSON.md`: the service path, whether CAS releases `emailAddressVerified`, single log out, and whether the customer pages should later read a subscription.

One open question lives in that rule and is deliberately not settled here: a missing `emailAddressVerified` is currently read as verified, which only matters if CAS does not release the attribute. Until P-21 answers that, the lenient default is the risk the deployment carries.

Five failed sign ins for one name within fifteen minutes lock that name for the rest of the window, through the 25.4 `failureHandler` and `SignInFailures`, D-259. It counts names rather than addresses, because the demo accounts are published and the thing worth slowing down is guessing.

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

## A refused route

A signed in reader without the role who opens an internal address lands on the home page, decided by Manolo on 2026-11-27. Access control refuses the route as not found, and `NotFoundView` forwards to the home page when the address names a registered route, so the reader learns no more about the route than the not found page told them. An address that names no route still shows the not found page, and an anonymous reader is still sent to sign in.

## What a test has to prove

Every rule here has a refusing test: `guest` is refused on each internal route and lands on the home page, anonymous is redirected to login, the draft service throws for a caller without the role, and the CSP header is present on a rendered page. Security tests assert the refusal, not the absence of a link.

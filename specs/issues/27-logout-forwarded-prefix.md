REPO: vaadin/flow
TITLE: AuthenticationContext.logout() sends the browser to the host's root when a proxy serves the application under a path
---
### Description

An application secured with `VaadinSecurityConfigurer` and served by a reverse proxy under a path, here `http://localhost:8128/app/`, signs in fine but signs out to the host's root: `authentication.logout()` from a button opens `http://localhost:8128/`, which is not the application.

The logout success URL is fixed at startup from the servlet context path, `/` here, and during a Vaadin request `UidlRedirectStrategy` hands it to `Page.setLocation` as it is. The login redirect is an HTTP `Location` header, which the proxy moves under its path; the logout location travels inside the UIDL response, where no proxy can reach it. `X-Forwarded-Prefix`, sent by the proxy and enabled with `server.forward-headers-strategy=framework`, is not applied to it either.

### Why it matters

Hosting that serves several applications on one host under a path each, such as Kubernetes ingresses, ends every sign out on a page of the proxy, usually a 404. In development the application is at the root and nothing shows.

### Expected

The logout lands on the application's own root, `http://localhost:8128/app/` or its login view. Either of:

1. The location sent to the page is relative to the application, for example resolved against the page's base, as the router's own links are.
2. The logout success URL is computed per request from the request's context path, which `ForwardedHeaderFilter` sets from `X-Forwarded-Prefix`.

### Workaround

A logout success handler that sends the page to `.`, which the browser resolves against the base Vaadin writes, the application's root:

```java
configurer.loginView(LoginView.class).logoutSuccessHandler((request, response, authentication) -> {
    UI ui = UI.getCurrent();
    if (ui != null) {
        ui.getPage().setLocation(".");
    } else {
        response.sendRedirect(request.getContextPath() + "/");
    }
});
```

### Reproduce

A starter with the documented setup, `vaadin.loginView(LoginView.class)`, an in memory user `user` / `user`, and a view with one button calling `AuthenticationContext.logout()`. In front of it an nginx that serves it at `/app/`, strips the prefix, sends `X-Forwarded-Prefix: /app` and moves the application's redirects under `/app/`.

1. `mvn spring-boot:run`, the application on 8127.
2. In the unzipped directory: `docker run --rm -p 8128:80 -v "$PWD/nginx.conf:/etc/nginx/conf.d/default.conf" nginx:1.27-alpine`.
3. Open `http://localhost:8128/app/`. It redirects to `http://localhost:8128/app/login`.
4. Sign in as `user` / `user`. The view opens at `http://localhost:8128/app/`.
5. Press Sign out.

The browser opened `http://localhost:8128/`, nginx's 404. The same project on 25.3.5, Flow 25.3.7, with Spring Boot 4.1.2, does the same. A reproduction lands anywhere outside `/app/`; with the workaround above it lands on `http://localhost:8128/app/login`.

### Getting the project

[`27-logout-forwarded-prefix.zip`](https://github.com/vaadin/hackathon-25-4/raw/COMMIT_SHA/specs/issues/projects/27-logout-forwarded-prefix.zip), 4 KB, sources only, with the `nginx.conf`. Unzip it and:

```
mvn spring-boot:run
```

Then start nginx as in step 2. It needs Docker.

Found on 25.4.0-beta2.

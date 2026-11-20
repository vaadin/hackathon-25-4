package com.vaadin.securitystatus.base.security;

import org.springframework.stereotype.Component;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;

/**
 * Puts the policy on the bootstrap page itself. In production mode that
 * response leaves without the headers Spring Security writes, so the header
 * writer in {@link SecurityConfig} covers every response except the one that
 * matters most.
 */
@Component
class CspOnBootstrapPage implements VaadinServiceInitListener {

    static final String HEADER = "Content-Security-Policy";

    @Override
    public void serviceInit(ServiceInitEvent event) {
        String policy = CspPolicy.enforced().toHeaderValue();
        event.addIndexHtmlRequestListener(response -> response.getVaadinResponse().setHeader(HEADER, policy));
    }
}

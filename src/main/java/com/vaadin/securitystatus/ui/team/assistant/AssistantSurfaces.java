package com.vaadin.securitystatus.ui.team.assistant;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.securitystatus.data.SecurityDataService;
import com.vaadin.securitystatus.security.Roles;
import com.vaadin.securitystatus.team.assistant.AdvisoryCatalogue;
import com.vaadin.securitystatus.team.assistant.AssistantHistory;
import com.vaadin.securitystatus.team.assistant.AssistantProviders;
import com.vaadin.securitystatus.team.assistant.AssistantTail;
import com.vaadin.securitystatus.team.assistant.AssistantTools;
import com.vaadin.securitystatus.ui.common.AiSurfaces;

/**
 * The assistant's surfaces, as the other screens ask for them. Every surface
 * is internal, {@code specs/06-ai.md}: one asked for in a session without the
 * team role is an empty element, so no model is ever called on behalf of an
 * anonymous reader, D-141.
 */
@Component
class AssistantSurfaces implements AiSurfaces {

    private final AssistantProviders providers;
    private final SecurityDataService data;
    private final AssistantTail tail;
    private final AdvisoryCatalogue catalogue;
    private final ObjectProvider<AssistantHistory> history;
    private final AuthenticationContext authentication;

    AssistantSurfaces(AssistantProviders providers, SecurityDataService data, AssistantTail tail,
            AdvisoryCatalogue catalogue, ObjectProvider<AssistantHistory> history,
            AuthenticationContext authentication) {
        this.providers = providers;
        this.data = data;
        this.tail = tail;
        this.catalogue = catalogue;
        this.history = history;
        this.authentication = authentication;
    }

    @Override
    public com.vaadin.flow.component.Component chatPanel() {
        if (!team()) {
            return nothing();
        }
        return new AssistantPanel(providers, new AssistantTools(data, tail), tail, history.getObject(), user());
    }

    @Override
    public com.vaadin.flow.component.Component advisoryAsk(SerializableConsumer<Optional<Set<String>>> narrow) {
        if (!team()) {
            return nothing();
        }
        return new AdvisoryAsk(providers, catalogue, tail, user(), narrow);
    }

    @Override
    public com.vaadin.flow.component.Component formFill(FormLayout form, Binder<?> binder,
            Collection<HasValue<?, ?>> personOnly) {
        if (!team()) {
            return nothing();
        }
        return new VerdictFill(providers, tail, user(), form, binder, personOnly);
    }

    @Override
    public boolean offered() {
        return providers.offered();
    }

    private boolean team() {
        return authentication.hasRole(Roles.SECURITY) && providers.offered();
    }

    private String user() {
        return authentication.getPrincipalName().orElse("");
    }

    private static Div nothing() {
        var empty = new Div();
        empty.setVisible(false);
        return empty;
    }
}

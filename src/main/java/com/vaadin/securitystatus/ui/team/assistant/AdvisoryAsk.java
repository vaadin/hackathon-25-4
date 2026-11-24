package com.vaadin.securitystatus.ui.team.assistant;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.ai.common.QueryPage;
import com.vaadin.flow.component.ai.grid.AIDataRow;
import com.vaadin.flow.component.ai.grid.GridAIController;
import com.vaadin.flow.component.ai.grid.GridState;
import com.vaadin.flow.component.ai.orchestrator.AIOrchestrator;
import com.vaadin.flow.component.ai.orchestrator.ResponseListener;
import com.vaadin.flow.component.ai.provider.DatabaseProvider;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageInputI18n;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.securitystatus.team.assistant.AdvisoryCatalogue;
import com.vaadin.securitystatus.team.assistant.AssistantProviders;
import com.vaadin.securitystatus.team.assistant.AssistantTail;
import com.vaadin.securitystatus.team.assistant.Prompts;

/**
 * The plain language ask above the advisories list, through the Grid AI
 * controller with query validation on.
 *
 * <p>
 * The controller only drives a {@code Grid<AIDataRow>} of its own, with
 * columns and a data provider it builds from the query, while the list it is
 * meant to narrow is a typed grid with its own columns, menu and order. So the
 * controller drives a grid nobody sees, over {@link AdvisoryCatalogue}, and
 * every query it settles on is run once more to learn which advisories it
 * names; those narrow the real list, D-140. The model writes filters and
 * never sees a row.
 */
final class AdvisoryAsk extends Composite<Div> {

    private final Grid<AIDataRow> unseen = new Grid<>();
    private final GridAIController controller;
    private final Paragraph answer = new Paragraph();
    private final Button showAll = new Button();
    private final AdvisoryCatalogue catalogue;
    private final AssistantTail tail;
    private final String user;
    private final SerializableConsumer<Optional<Set<String>>> narrow;

    AdvisoryAsk(AssistantProviders providers, AdvisoryCatalogue catalogue, AssistantTail tail, String user,
            SerializableConsumer<Optional<Set<String>>> narrow) {
        this.catalogue = catalogue;
        this.tail = tail;
        this.user = user;
        this.narrow = narrow;
        Div box = getContent();
        box.addClassName("advisories-ask");

        var ask = new MessageInput();
        ask.addClassName("advisories-ask-input");
        ask.setI18n(new MessageInputI18n().setMessage(getTranslation("advisories.ask.message"))
                .setSend(getTranslation("advisories.ask.send")));
        answer.addClassName("advisories-ask-answer");
        answer.setVisible(false);
        showAll.setText(getTranslation("advisories.ask.all"));
        showAll.addClassName("advisories-ask-all");
        showAll.addThemeVariants(ButtonVariant.TERTIARY);
        showAll.setVisible(false);
        showAll.addClickListener(click -> showEverything());
        box.add(ask, answer, showAll);

        controller = new GridAIController(unseen, new CatalogueQueries(catalogue));
        controller.addStateChangeListener(this::narrowTo);
        AIOrchestrator.builder(providers.create(getTranslation("advisories.ask.unrecorded")),
                Prompts.text(Prompts.GRID))
                .withInput(ask)
                .withController(controller)
                .withRequestInterceptor(event -> tail.record("grid", user, AssistantTail.Kind.PROMPT,
                        event.getUserMessage()))
                .withResponseListener(this::ended)
                .build();
    }

    /** The grid the controller drives, which nobody sees. */
    Grid<AIDataRow> unseen() {
        return unseen;
    }

    private void narrowTo(GridState state) {
        Set<String> ids = catalogue.ids(state.query());
        tail.record("grid", user, AssistantTail.Kind.TOOL, state.query() + " -> " + ids.size());
        narrow.accept(Optional.of(ids));
        showAll.setVisible(true);
    }

    private void ended(ResponseListener.ResponseEvent event) {
        String words = event.getError().map(error -> getTranslation("advisories.ask.failed"))
                .orElse(event.getResponse().strip());
        tail.record("grid", user,
                event.getError().isPresent() ? AssistantTail.Kind.FAILURE : AssistantTail.Kind.ANSWER,
                event.getError().map(Throwable::toString).orElse(words));
        answer.setText(words);
        answer.setVisible(!words.isEmpty());
    }

    private void showEverything() {
        narrow.accept(Optional.empty());
        showAll.setVisible(false);
        answer.setVisible(false);
    }

    /** The catalogue as the controller's {@link DatabaseProvider}, with the first column checked on every probe. */
    private record CatalogueQueries(AdvisoryCatalogue catalogue) implements DatabaseProvider {

        @Override
        public String getSchema() {
            return catalogue.schema();
        }

        @Override
        public List<Map<String, Object>> executeQuery(String sql) {
            return catalogue.query(sql);
        }

        @Override
        public List<Map<String, Object>> executeQuery(String sql, QueryPage page) {
            catalogue.requireAdvisoryFirst(sql);
            return DatabaseProvider.super.executeQuery(sql, page);
        }
    }
}

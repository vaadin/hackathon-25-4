package com.vaadin.securitystatus.ui.team.assistant;

import java.util.Collection;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.ai.form.FieldMarkerI18n;
import com.vaadin.flow.component.ai.form.FormAIController;
import com.vaadin.flow.component.ai.orchestrator.AIOrchestrator;
import com.vaadin.flow.component.ai.orchestrator.ResponseListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.securitystatus.team.assistant.AssistantHistory;
import com.vaadin.securitystatus.team.assistant.AssistantProviders;
import com.vaadin.securitystatus.team.assistant.AssistantTail;
import com.vaadin.securitystatus.team.assistant.Prompts;

/**
 * The paste box above the workbench's verdict form: prose in, the fields the
 * prose supports filled by the Form AI controller, each marked with its AI
 * badge and revert control. Filling and saving are two acts: this box only
 * fills, the form's own Save button is the only way a draft is kept.
 */
final class VerdictFill extends Composite<Div> {

    private final TextArea paste = new TextArea();
    private final Button fill = new Button();
    private final Paragraph answer = new Paragraph();
    private final FormAIController controller;
    private final AIOrchestrator orchestrator;
    private final AssistantTail tail;
    private final String user;

    VerdictFill(AssistantProviders providers, AssistantTail tail, AssistantHistory history, String user,
            FormLayout form, Binder<?> binder, Collection<HasValue<?, ?>> personOnly) {
        this.tail = tail;
        this.user = user;
        Div box = getContent();
        box.addClassName("verdict-fill");

        paste.setLabel(getTranslation("workbench.fill.label"));
        paste.setHelperText(getTranslation("workbench.fill.help"));
        paste.addClassName("verdict-fill-paste");
        paste.setWidthFull();
        paste.setValueChangeMode(ValueChangeMode.EAGER);
        fill.setText(getTranslation("workbench.fill.run"));
        fill.addClassName("verdict-fill-run");
        fill.setEnabled(false);
        paste.addValueChangeListener(change -> fill.setEnabled(!change.getValue().isBlank()));
        fill.addClickListener(click -> run());
        answer.addClassName("verdict-fill-answer");
        answer.setVisible(false);
        box.add(paste, fill, answer);

        controller = new FormAIController(form, binder);
        personOnly.forEach(controller::ignoreField);
        controller.setFieldMarkerI18n(new FieldMarkerI18n().setMessage(getTranslation("workbench.fill.marker.message"))
                .setRevert(getTranslation("workbench.fill.marker.revert"))
                .setBadgeLabel(getTranslation("workbench.fill.marker.badge"))
                .setBadgeTooltip(getTranslation("workbench.fill.marker.tooltip"))
                .setConfidence(new FieldMarkerI18n.Confidence()
                        .setHigh(getTranslation("workbench.fill.marker.high"))
                        .setMedium(getTranslation("workbench.fill.marker.medium"))
                        .setLow(getTranslation("workbench.fill.marker.low"))));
        orchestrator = AIOrchestrator.builder(providers.create(getTranslation("workbench.fill.unrecorded")),
                Prompts.text(Prompts.FORM))
                .withController(controller)
                .withRequestInterceptor(event -> {
                    if (!history.admit()) {
                        event.reject();
                        answer.setText(getTranslation("assistant.enough"));
                        answer.setVisible(true);
                        return;
                    }
                    tail.record("form", user, AssistantTail.Kind.PROMPT, event.getUserMessage());
                })
                .withResponseListener(this::ended)
                .build();
    }

    private void run() {
        String text = paste.getValue().strip();
        if (!text.isEmpty()) {
            orchestrator.prompt(text);
        }
    }

    private void ended(ResponseListener.ResponseEvent event) {
        String words = event.getError().map(error -> getTranslation("workbench.fill.failed"))
                .orElse(event.getResponse().strip());
        tail.record("form", user,
                event.getError().isPresent() ? AssistantTail.Kind.FAILURE : AssistantTail.Kind.ANSWER,
                event.getError().map(Throwable::toString).orElse(words));
        answer.setText(words);
        answer.setVisible(!words.isEmpty());
    }
}

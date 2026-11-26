package com.vaadin.securitystatus.ui.team.assistant;

import java.util.List;
import java.util.Map;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.ai.common.AIAttachment;
import com.vaadin.flow.component.ai.orchestrator.AIOrchestrator;
import com.vaadin.flow.component.ai.orchestrator.RequestInterceptor;
import com.vaadin.flow.component.ai.orchestrator.ResponseListener;
import com.vaadin.flow.component.ai.provider.ToolCallLimitExceededException;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageInputI18n;
import com.vaadin.flow.component.messages.MessageInputVariant;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.messages.MessageListItem;
import com.vaadin.flow.component.upload.UploadButton;
import com.vaadin.flow.component.upload.UploadCapture;
import com.vaadin.flow.component.upload.UploadFileList;
import com.vaadin.flow.component.upload.UploadManager;
import com.vaadin.securitystatus.team.assistant.AssistantHistory;
import com.vaadin.securitystatus.team.assistant.AssistantProviders;
import com.vaadin.securitystatus.team.assistant.AssistantTail;
import com.vaadin.securitystatus.team.assistant.AssistantTools;
import com.vaadin.securitystatus.team.assistant.Prompts;

/**
 * The chat assistant of the internal drawer: a message list, an input, and an
 * attachment button that offers the camera on a phone through the 25.4
 * {@link UploadCapture}, wired by one {@link AIOrchestrator} to the five tools
 * of {@link AssistantTools}.
 *
 * <p>
 * Every prompt passes the request interceptor, which writes it to the
 * assistant tail for the diagnostics view. A turn that fails says so in the
 * reader's words, and one stopped at the tool call limit says how many calls
 * were made: the orchestrator's own text for both is English and fixed, so it
 * is replaced in the list when the turn ends.
 */
final class AssistantPanel extends Composite<Section> {

    /** The largest picture accepted, five megabytes. */
    static final long MOST_BYTES = 5L * 1024 * 1024;

    private final MessageList messages = new MessageList();
    private final MessageInput input = new MessageInput();
    private final UploadManager uploads = new UploadManager(this);
    private final AssistantTools tools;
    private final AssistantTail tail;
    private final AssistantHistory history;
    private final String user;
    private final AIOrchestrator orchestrator;

    AssistantPanel(AssistantProviders providers, AssistantTools tools, AssistantTail tail, AssistantHistory history,
            String user) {
        this.tools = tools;
        this.tail = tail;
        this.history = history;
        this.user = user;
        Section panel = getContent();
        panel.addClassName("assistant-panel");
        panel.getElement().setAttribute("aria-label", getTranslation("assistant.title"));

        var title = new H2(getTranslation("assistant.title"));
        title.addClassName("assistant-title");
        panel.add(title);
        if (!providers.live()) {
            var mock = new Paragraph(getTranslation("assistant.mock"));
            mock.addClassName("assistant-mock");
            panel.add(mock);
        }

        messages.addClassName("assistant-messages");
        messages.setMarkdown(true);

        input.addClassName("assistant-input");
        input.addThemeVariants(MessageInputVariant.AURA_ICON_BUTTON);
        input.setI18n(new MessageInputI18n().setMessage(getTranslation("assistant.input.message"))
                .setSend(getTranslation("assistant.input.send")));

        uploads.setAcceptedMimeTypes("image/*");
        uploads.setMaxFileSize(MOST_BYTES);
        uploads.setMaxFiles(1);
        // A camera icon at the start of the composer, as phone chat apps draw it.
        var attach = new UploadButton(uploads);
        attach.setIcon(VaadinIcon.CAMERA.create());
        attach.setAriaLabel(getTranslation("assistant.attach"));
        attach.addThemeVariants(ButtonVariant.TERTIARY);
        attach.addClassName("assistant-attach");
        attach.setCapture(UploadCapture.ENVIRONMENT);
        attach.setTooltipText(getTranslation("assistant.attach.tooltip"));
        var files = new UploadFileList(uploads);
        files.addClassName("assistant-files");

        var compose = new Div(files, attach, input);
        compose.addClassName("assistant-compose");
        panel.add(messages, compose);

        orchestrator = AIOrchestrator.builder(providers.create(getTranslation("assistant.unrecorded")),
                Prompts.text(Prompts.ASSISTANT))
                .withMessageList(messages)
                .withInput(input)
                .withFileReceiver(uploads)
                .withController(tools)
                .withUserName(getTranslation("assistant.you"))
                .withAssistantName(getTranslation("assistant.name"))
                .withRequestInterceptor(this::intercept)
                .withResponseListener(this::ended)
                .withHistory(history.messages(), Map.of())
                .build();
    }

    /** The orchestrator, for a programmatic prompt in the tests. */
    AIOrchestrator orchestrator() {
        return orchestrator;
    }

    /** The prompt is written to the tail as it goes out, or refused when the session has asked enough for now. */
    private void intercept(RequestInterceptor.RequestInterceptEvent event) {
        if (!history.admit()) {
            event.reject(getTranslation("assistant.enough"));
            return;
        }
        tools.forUser(user);
        var line = new StringBuilder(event.getUserMessage());
        for (AIAttachment file : event.getAttachments()) {
            line.append(" [").append(file.name()).append(", ").append(file.mimeType()).append(", ")
                    .append(file.data().length).append(" bytes]");
        }
        tail.record("chat", user, AssistantTail.Kind.PROMPT, line.toString());
    }

    private void ended(ResponseListener.ResponseEvent event) {
        event.getError().ifPresentOrElse(error -> {
            boolean limit = error instanceof ToolCallLimitExceededException;
            String sentence = limit ? getTranslation("assistant.gaveUp", tools.callsThisTurn())
                    : getTranslation("assistant.failed");
            tail.record("chat", user, AssistantTail.Kind.FAILURE,
                    error.getClass().getSimpleName() + ": " + error.getMessage());
            List<MessageListItem> items = messages.getItems();
            if (!items.isEmpty()) {
                MessageListItem last = items.getLast();
                last.setText(sentence);
                last.addClassNames(limit ? "assistant-gave-up" : "assistant-failed");
            }
        }, () -> tail.record("chat", user, AssistantTail.Kind.ANSWER, event.getResponse()));
        history.keep(orchestrator.getHistory());
    }
}

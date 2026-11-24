package com.vaadin.securitystatus.ui.team.assistant;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.net.URISyntaxException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.messages.MessageListItem;
import com.vaadin.flow.component.upload.UploadButton;
import com.vaadin.flow.component.upload.UploadCapture;
import com.vaadin.securitystatus.security.Roles;
import com.vaadin.securitystatus.team.assistant.AssistantProviders;
import com.vaadin.securitystatus.team.assistant.AssistantTail.Entry;
import com.vaadin.securitystatus.team.assistant.AssistantTail.Kind;
import com.vaadin.securitystatus.team.assistant.AssistantTail;
import com.vaadin.securitystatus.team.assistant.AssistantTools;
import com.vaadin.securitystatus.team.assistant.CassetteLLMProvider;
import com.vaadin.securitystatus.team.assistant.Prompts;

/**
 * AST-01 to AST-04 and the rest of US-11.1 to US-11.3: the chat panel of the
 * internal drawer against the cassette provider, the five tools over the
 * fixtures, the refusals, the limit sentence, the picture flow, the history
 * kept across shells, the prompts under test as text, and every exchange in
 * the assistant tail.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@WithMockUser(username = "team", roles = Roles.SECURITY)
class AssistantBrowserlessTest extends SpringBrowserlessTest {

    @Autowired
    AssistantTail tail;

    @Autowired
    AssistantProviders providers;

    private long started;

    @BeforeEach
    void openAnInternalScreen() {
        started = tail.newest();
        UI.getCurrent().navigate("findings");
        openPanel();
    }

    /** The panel is on the right of the internal shell, shut until the Assistant button opens it, D-203. */
    private void openPanel() {
        test($(com.vaadin.flow.component.button.Button.class).withClassName("assistant-toggle").single()).click();
    }

    // US-11.1: the mock is the default, and says so

    @Test
    void the_default_build_runs_the_cassette_provider_and_the_banner_says_so() {
        assertThat(providers.live()).isFalse();
        assertThat(providers.create("x")).isInstanceOf(CassetteLLMProvider.class);
        assertThat($(Paragraph.class).withClassName("assistant-mock").single().getText())
                .startsWith("Recorded answers: no model is configured here");
    }

    // AST-01 and AC1

    @Test
    void the_version_question_makes_one_versionStatus_call_and_quotes_state_and_date() {
        ask("Is 24.9.21 safe?");

        List<MessageListItem> messages = messages();
        assertThat(messages).hasSize(2);
        assertThat(messages.get(0).getUserName()).isEqualTo("You");
        assertThat(messages.get(0).getText()).isEqualTo("Is 24.9.21 safe?");
        assertThat(messages.get(1).getUserName()).isEqualTo("Assistant");
        assertThat(messages.get(1).getText()).isEqualTo("The measurements of 24.9.21 say **fix_available**, "
                + "scanned 2026-11-15T03:57:22Z: 1 advisories open as a risk and 11 in triage. "
                + "Upgrading to 24.9.26 clears 12 and leaves 0. Source: `cases/v/24.9.21.json`.");
        assertThat(toolCalls()).containsExactly("versionStatus {\"version\":\"24.9.21\"}");
    }

    // AST-02 and AC2

    @Test
    void an_unmeasured_version_is_refused_naming_the_nearest_measured() {
        ask("Is 24.9.15 safe?");

        assertThat(lastAnswer()).isEqualTo(
                "24.9.15 was never measured. The nearest measured release is 24.9.14; ask about that one for a measured answer.");
        assertThat(tailSince(Kind.REFUSAL)).singleElement().extracting(Entry::text).asString()
                .startsWith("versionStatus {\"version\":\"24.9.15\"} -> Version 24.9.15 was never measured. "
                        + "The nearest measured release is 24.9.14");
    }

    // AST-03 and AC1

    @Test
    void a_question_the_tools_cannot_ground_gets_the_contract_sentence() {
        ask("Who found CVE-2026-89407?");

        assertThat(lastAnswer()).isEqualTo("The measurements do not say.");
        assertThat(toolCalls()).containsExactly("cveStatus {\"id\":\"CVE-2026-89407\"}");
    }

    // AC2: the limit renders as the sentence, never a stack trace

    @Test
    void calls_past_the_tool_limit_end_in_the_gave_up_sentence_with_the_count() {
        ask("Check every release of every line");

        MessageListItem last = messages().getLast();
        assertThat(last.getText())
                .isEqualTo("The assistant gave up after 40 tool calls without an answer. Ask a narrower question.");
        assertThat(last.hasClassName("assistant-gave-up")).isTrue();
        assertThat(tailSince(Kind.FAILURE)).singleElement().extracting(Entry::text).asString()
                .startsWith("ToolCallLimitExceededException: Tool call limit exceeded: versionStatus was called 41 times");
    }

    // AST-04 and AC3

    @Test
    void a_picture_of_a_scanner_line_is_a_question_answered_by_a_cveStatus_call() throws URISyntaxException {
        UploadButton attach = $(UploadButton.class).withClassName("assistant-attach").single();
        assertThat(attach.getCapture()).isEqualTo(UploadCapture.ENVIRONMENT);

        test(attach).upload(fixture("scanner-line.png"));
        ask("What does this line mean for us?");

        assertThat(test(list()).getAttachments(0)).extracting(MessageListItem.Attachment::name)
                .containsExactly("scanner-line.png");
        assertThat(toolCalls()).containsExactly("cveStatus {\"id\":\"CVE-2026-89407\"}");
        assertThat(lastAnswer()).startsWith("The picture shows CVE-2026-89407 on "
                + "com.fasterxml.jackson.core:jackson-core:2.21.5. The measurements give it high, verdict **risk**, "
                + "fix owner vaadin. Source: `cases/cve/CVE-2026-89407.json`.")
                .contains("- 24.9: ");
        assertThat(tailSince(Kind.PROMPT)).singleElement().extracting(Entry::text).asString()
                .isEqualTo("What does this line mean for us? [scanner-line.png, image/png, 9840 bytes]");
    }

    @Test
    void a_picture_with_no_readable_id_says_so_and_asks_for_the_id() throws URISyntaxException {
        test($(UploadButton.class).withClassName("assistant-attach").single()).upload(fixture("no-id.png"));
        ask("And this one?");

        assertThat(lastAnswer()).isEqualTo("I could not read an advisory id in the picture: the identifier column "
                + "is cut off. Which CVE or GHSA id is it?");
        assertThat(toolCalls()).isEmpty();
    }

    // US-11.2: the other tools and their refusals

    @Test
    void an_advisory_on_a_version_and_an_unknown_advisory() {
        ask("What does CVE-2026-89407 mean for 24.9.21?");
        assertThat(lastAnswer()).startsWith("On 24.9.21, CVE-2026-89407 is **");

        ask("Tell me about CVE-1999-0001");
        assertThat(lastAnswer()).isEqualTo("The measurements know no advisory CVE-1999-0001. Check the id; "
                + "a GHSA id is known only when the team tracks it under that id.");
    }

    @Test
    void open_findings_are_paged_and_the_queue_head_is_read() {
        ask("What is open on 24.9.21?");
        assertThat(lastAnswer()).startsWith("Open on 24.9.21, 12 in all, from `cases/v/24.9.21.json`:")
                .contains("- CVE-2026-89407, risk, high");
        assertThat(toolCalls()).containsExactly("openFindings {\"version\":\"24.9.21\"}");

        ask("What should get a verdict next?");
        assertThat(lastAnswer()).startsWith("The top of the verdict queue, from `data/queue.json`:")
                .contains("- com.fasterxml.jackson.core:jackson-core: ");
    }

    @Test
    void the_tools_page_long_lists_and_refuse_out_of_range_asks() {
        var tools = new AssistantTools(dataService(), tail);
        var json = tools.openFindings(tools_json("{\"version\":\"24.9.21\",\"offset\":0,\"limit\":5}"));
        assertThat(json.get("findings").size()).isEqualTo(5);
        assertThat(json.get("next_offset").asInt()).isEqualTo(5);
        assertThat(json.get("total").asInt()).isEqualTo(12);
        var capped = tools.openFindings(tools_json("{\"version\":\"24.9.21\",\"limit\":500}"));
        assertThat(capped.get("findings").size()).isEqualTo(12);

        org.assertj.core.api.Assertions
                .assertThatThrownBy(() -> tools.queueHead(tools_json("{\"n\":500}")))
                .isInstanceOf(com.vaadin.flow.component.ai.provider.ToolException.class)
                .hasMessage("n must be a whole number from 1 to 20, and 500 is not.");
    }

    @Test
    void a_question_no_cassette_recorded_is_answered_with_the_unrecorded_sentence() {
        ask("Write me a poem about jackson");

        assertThat(lastAnswer()).startsWith("There is no recorded answer for that question");
        assertThat(toolCalls()).isEmpty();
    }

    // US-11.2: history kept

    @Test
    void the_conversation_survives_a_new_shell() {
        ask("Is 24.9.21 safe?");
        assertThat(messages()).hasSize(2);

        UI.getCurrent().navigate("");
        UI.getCurrent().navigate("findings/workbench");
        openPanel();

        assertThat(messages()).extracting(MessageListItem::getText).first().isEqualTo("Is 24.9.21 safe?");
        assertThat(messages()).hasSize(2);
    }

    // US-11.1: request interception into the diagnostics tail

    @Test
    void every_exchange_is_in_the_tail_with_who_asked() {
        ask("Is 24.9.21 safe?");
        // The end of a turn is a UI.access task, run by the next component query.
        assertThat(messages()).hasSize(2);

        List<Entry> entries = tail.entries().stream().filter(entry -> entry.number() > started).toList();
        assertThat(entries).extracting(Entry::kind).containsExactly(Kind.PROMPT, Kind.TOOL, Kind.ANSWER);
        assertThat(entries).extracting(Entry::user).containsOnly("team");
        assertThat(entries).extracting(Entry::surface).containsOnly("chat");
        assertThat(entries.getFirst().text()).isEqualTo("Is 24.9.21 safe?");
    }

    // US-11.1: the prompt files under test as text

    @Test
    void every_cassette_was_recorded_against_the_prompt_that_ships() {
        assertThat(CassetteLLMProvider.cassettes()).hasSizeGreaterThanOrEqualTo(14);
        for (var cassette : CassetteLLMProvider.cassettes()) {
            assertThat(cassette.promptSha256())
                    .as("%s was recorded against another %s; record it again or sign it deliberately", cassette.file(),
                            cassette.prompt())
                    .isEqualTo(Prompts.sha256(Prompts.text(cassette.prompt())));
            assertThat(cassette.recorded()).as(cassette.file()).isNotBlank();
        }
    }

    @Test
    void the_prompts_hold_their_contracts() {
        assertThat(Prompts.text(Prompts.ASSISTANT))
                .contains("Answer only from the results of your tools")
                .contains("reply with exactly this sentence and nothing else: The measurements do not say.")
                .contains("The image gives you the question, never the answer.");
        assertThat(Prompts.text(Prompts.FORM)).contains("Fill only the fields the pasted text supports")
                .contains("You never save or submit anything.");
        assertThat(Prompts.text(Prompts.GRID)).contains("You emit only filters, never data.");
        for (String tool : List.of("versionStatus", "cveStatus", "upgradeTarget", "openFindings", "queueHead")) {
            assertThat(Prompts.toolDescription(tool)).as(tool).isNotBlank();
            assertThat(Prompts.toolParameters(tool)).as(tool).startsWith("{\"type\":\"object\"");
        }
    }

    // Helpers

    private void ask(String question) {
        test($(MessageInput.class).withClassName("assistant-input").single()).send(question);
    }

    private MessageList list() {
        return $(MessageList.class).withClassName("assistant-messages").single();
    }

    private List<MessageListItem> messages() {
        return test(list()).getMessages();
    }

    private String lastAnswer() {
        return messages().getLast().getText();
    }

    /** The tool calls since the test started, as the tail recorded them, without their results. */
    private List<String> toolCalls() {
        return tail.entries().stream().filter(entry -> entry.number() > started)
                .filter(entry -> entry.kind() == Kind.TOOL || entry.kind() == Kind.REFUSAL)
                .map(entry -> entry.text().substring(0, entry.text().indexOf(" -> "))).toList();
    }

    private List<Entry> tailSince(Kind kind) {
        return tail.entries().stream().filter(entry -> entry.number() > started)
                .filter(entry -> entry.kind() == kind).toList();
    }

    @Autowired
    com.vaadin.securitystatus.data.SecurityDataService data;

    private com.vaadin.securitystatus.data.SecurityDataService dataService() {
        return data;
    }

    private static tools.jackson.databind.JsonNode tools_json(String json) {
        return tools.jackson.databind.json.JsonMapper.builder().build().readTree(json);
    }

    private static File fixture(String name) throws URISyntaxException {
        return new File(AssistantBrowserlessTest.class.getResource("/assistant/" + name).toURI());
    }
}

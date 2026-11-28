package com.vaadin.securitystatus.team.assistant;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import com.vaadin.flow.component.ai.common.AIAttachment;
import com.vaadin.flow.component.ai.common.ChatMessage;
import com.vaadin.flow.component.ai.provider.LLMProvider;
import com.vaadin.flow.component.ai.provider.ToolCallLimitExceededException;
import com.vaadin.flow.component.ai.provider.ToolException;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import reactor.core.publisher.Flux;

/**
 * The provider the default build runs with: no key, no network, no model. It
 * replays recorded exchanges, cassettes, from {@code cassettes/*.json} on the
 * classpath. A cassette names the system prompt it was recorded against by
 * its SHA-256, and is only ever played to a request carrying that prompt, so
 * a prompt edited without recording again makes its cassettes fall silent,
 * which a test turns into a failure.
 *
 * <p>
 * A cassette is played as the model would act: it calls the tools the
 * request offers, through their {@code execute}, in the recorded order and
 * with the recorded arguments, and then says what it recorded. What it says
 * is a template filled from the tool results of this replay, {@code ${tool.field}},
 * so a replay on today's data quotes today's values; a sentence whose blanks
 * the results cannot fill is not said at all, rule 4. A tool's refusal is
 * {@code ${tool.refused}}.
 *
 * <p>
 * It bounds the tool calls of a turn as the built in providers do, 40 per
 * tool and 150 in all, and ends the turn with the 25.4
 * {@link ToolCallLimitExceededException} past them.
 */
public final class CassetteLLMProvider implements LLMProvider {

    static final int MAX_CALLS_PER_TOOL = 40;
    static final int MAX_TOTAL_CALLS = 150;

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Pattern BLANK = Pattern.compile("\\$\\{([A-Za-z0-9_.]+)}");
    private static final List<Cassette> ALL = load();

    /** One recorded exchange. */
    public record Cassette(String file, String prompt, String promptSha256, String recorded, @Nullable Pattern message,
            @Nullable String attachmentSha256, List<JsonNode> steps) {
    }

    private final String unrecorded;

    /**
     * @param unrecorded
     *            what is said to a question no cassette recorded, in the
     *            reader's language
     */
    CassetteLLMProvider(String unrecorded) {
        this.unrecorded = unrecorded;
    }

    /** Every cassette on the classpath, in file name order. */
    public static List<Cassette> cassettes() {
        return ALL;
    }

    @Override
    public Flux<String> stream(LLMRequest request) {
        return Flux.defer(() -> Flux.fromIterable(chunks(play(request))));
    }

    @Override
    public void setHistory(List<ChatMessage> history, Map<String, List<AIAttachment>> attachmentsByMessageId) {
        // Nothing to remember: a cassette answers one message as recorded,
        // whatever came before it.
    }

    private String play(LLMRequest request) {
        String promptSha = Prompts.sha256(request.systemPrompt());
        for (Cassette cassette : ALL) {
            if (!cassette.promptSha256().equals(promptSha)) {
                continue;
            }
            Optional<Map<String, String>> asked = matches(cassette, request);
            if (asked.isPresent()) {
                return replay(cassette, request, asked.get());
            }
        }
        return unrecorded;
    }

    private static Optional<Map<String, String>> matches(Cassette cassette, LLMRequest request) {
        var values = new HashMap<String, String>();
        if (cassette.attachmentSha256() != null) {
            boolean attached = request.attachments().stream()
                    .anyMatch(file -> cassette.attachmentSha256().equals(sha256(file.data())));
            if (!attached) {
                return Optional.empty();
            }
        }
        if (cassette.message() != null) {
            Matcher matcher = cassette.message().matcher(request.userMessage());
            if (!matcher.find()) {
                return Optional.empty();
            }
            for (String group : cassette.message().namedGroups().keySet()) {
                Optional.ofNullable(matcher.group(group)).ifPresent(value -> values.put(group, value));
            }
        }
        return Optional.of(values);
    }

    private String replay(Cassette cassette, LLMRequest request, Map<String, String> values) {
        Map<String, ToolSpec> tools = new LinkedHashMap<>();
        request.explicitTools().forEach(tool -> tools.put(tool.getName(), tool));
        Map<String, Integer> perTool = new HashMap<>();
        int total = 0;
        var said = new StringBuilder();
        for (JsonNode step : cassette.steps()) {
            if (!applies(step, values)) {
                continue;
            }
            if (step.has("extract")) {
                step.get("extract").properties().forEach(entry -> values.put(entry.getKey(), entry.getValue().asString()));
            } else if (step.has("match")) {
                // Reads named values out of an earlier result, as a model reads a refusal's sentence.
                String in = values.getOrDefault(step.get("match").path("in").asString(), "");
                Pattern pattern = Pattern.compile(step.get("match").path("pattern").asString());
                Matcher found = pattern.matcher(in);
                if (found.find()) {
                    for (String group : pattern.namedGroups().keySet()) {
                        Optional.ofNullable(found.group(group)).ifPresent(value -> values.put(group, value));
                    }
                }
            } else if (step.has("each")) {
                // One sentence per item of a listed result, its fields as blanks.
                String list = step.get("each").asString();
                int count = Integer.parseInt(values.getOrDefault(list + ".count", "0"));
                for (int i = 0; i < count; i++) {
                    var item = new HashMap<>(values);
                    Optional.ofNullable(values.get(list + "." + i)).ifPresent(value -> item.put("item", value));
                    String prefix = list + "." + i + ".";
                    values.forEach((key, value) -> {
                        if (key.startsWith(prefix)) {
                            item.put(key.substring(prefix.length()), value);
                        }
                    });
                    said.append(parts(step.path("say"), item));
                }
            } else if (step.has("call")) {
                String name = step.get("call").asString();
                int times = step.path("times").asInt(1);
                for (int time = 0; time < times; time++) {
                    total++;
                    int count = perTool.merge(name, 1, Integer::sum);
                    if (count > MAX_CALLS_PER_TOOL || total > MAX_TOTAL_CALLS) {
                        throw new ToolCallLimitExceededException(String.format("Tool call limit exceeded: %s was called %d times, %d calls in all", name, count, total));
                    }
                    call(tools, name, fill(step.path("with"), values), values);
                }
            } else if (step.has("say")) {
                said.append(parts(step.get("say"), values));
            }
        }
        return said.toString();
    }

    private void call(Map<String, ToolSpec> tools, String name, JsonNode arguments, Map<String, String> values) {
        ToolSpec tool = tools.get(name);
        if (tool == null) {
            throw new IllegalStateException("The cassette calls " + name + ", which the request does not offer");
        }
        values.keySet().removeIf(key -> key.startsWith(name + "."));
        try {
            String result = tool.execute(arguments);
            try {
                flatten(name, JSON.readTree(result), values);
            } catch (RuntimeException notJson) {
                values.put(name + ".text", result);
            }
        } catch (ToolException refused) {
            values.put(name + ".refused", refused.getMessage());
        }
    }

    /** A step with {@code if} runs only when that value is known, one with {@code unless} only when it is not. */
    private static boolean applies(JsonNode step, Map<String, String> values) {
        String when = step.path("if").asString("");
        String unless = step.path("unless").asString("");
        return (when.isEmpty() || values.containsKey(when)) && (unless.isEmpty() || !values.containsKey(unless));
    }

    private static void flatten(String prefix, JsonNode node, Map<String, String> values) {
        if (node.isObject()) {
            node.properties().forEach(entry -> flatten(prefix + "." + entry.getKey(), entry.getValue(), values));
        } else if (node.isArray()) {
            values.put(prefix + ".count", String.valueOf(node.size()));
            for (int i = 0; i < node.size(); i++) {
                flatten(prefix + "." + i, node.get(i), values);
            }
            if (node.valueStream().allMatch(JsonNode::isValueNode)) {
                values.put(prefix, String.join(", ", node.valueStream().map(JsonNode::asString).toList()));
            }
        } else if (!node.isNull()) {
            values.put(prefix, node.asString());
        }
    }

    private static JsonNode fill(JsonNode template, Map<String, String> values) {
        ObjectNode out = JSON.createObjectNode();
        template.properties().forEach(entry -> {
            JsonNode value = entry.getValue();
            if (value.isString()) {
                fillText(value.asString(), values).ifPresent(text -> out.put(entry.getKey(), text));
            } else {
                out.set(entry.getKey(), value);
            }
        });
        return out;
    }

    /**
     * A sentence, or a list of parts of one: each part is said only when all
     * its blanks can be filled, so an absent field drops its clause, never the
     * whole sentence.
     */
    private static String parts(JsonNode say, Map<String, String> values) {
        if (!say.isArray()) {
            return fillText(say.asString(), values).orElse("");
        }
        var out = new StringBuilder();
        say.valueStream().forEach(part -> fillText(part.asString(), values).ifPresent(out::append));
        return out.toString();
    }

    /** The text with every blank filled, or empty when one cannot be. */
    static Optional<String> fillText(String template, Map<String, String> values) {
        Matcher blank = BLANK.matcher(template);
        var out = new StringBuilder();
        while (blank.find()) {
            String value = values.get(blank.group(1));
            if (value == null) {
                return Optional.empty();
            }
            blank.appendReplacement(out, Matcher.quoteReplacement(value));
        }
        blank.appendTail(out);
        return Optional.of(out.toString());
    }

    /** The answer as a model streams it, a word at a time. */
    private static List<String> chunks(String text) {
        var chunks = new ArrayList<String>();
        Matcher word = Pattern.compile("\\S+\\s*|\\s+").matcher(text);
        while (word.find()) {
            chunks.add(word.group());
        }
        return chunks;
    }

    static String sha256(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static List<Cassette> load() {
        try {
            Resource[] files = new PathMatchingResourcePatternResolver(CassetteLLMProvider.class.getClassLoader())
                    .getResources("classpath*:cassettes/*.json");
            var cassettes = new ArrayList<Cassette>();
            for (Resource file : files) {
                try (InputStream in = file.getInputStream()) {
                    JsonNode root = JSON.readTree(in);
                    String message = root.path("when").path("message").asString("");
                    String attachment = root.path("when").path("attachmentSha256").asString("");
                    cassettes.add(new Cassette(String.valueOf(file.getFilename()), root.path("prompt").asString(),
                            root.path("promptSha256").asString(), root.path("recorded").asString(""),
                            message.isEmpty() ? null : Pattern.compile(message),
                            attachment.isEmpty() ? null : attachment, root.path("steps").valueStream().toList()));
                }
            }
            cassettes.sort(Comparator.comparing(Cassette::file));
            return List.copyOf(cassettes);
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }
}

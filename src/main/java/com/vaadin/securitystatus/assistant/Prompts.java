package com.vaadin.securitystatus.assistant;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.HexFormat;
import java.util.Locale;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Everything a model reads that this application wrote: the system prompts,
 * one file each under {@code prompts/}, the descriptions and parameter schemas
 * of the assistant's tools, and the sentences a tool refuses with. Held as
 * text so a test can pin it, and so a cassette can say which prompt it was
 * recorded against.
 */
final class Prompts {

    /** The chat assistant. */
    static final String ASSISTANT = "assistant.md";
    /** The ask box over the advisories list. */
    static final String GRID = "grid.md";
    /** The paste box of the workbench form. */
    static final String FORM = "form.md";
    /** The schema of the advisories catalogue the grid ask queries. */
    static final String CATALOGUE = "catalogue-schema.md";

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final JsonNode TOOLS = JSON.readTree(text("tools.json"));

    private Prompts() {
    }

    /**
     * The text of one file under {@code prompts/}, trimmed as the orchestrator
     * trims a system prompt before it sends it, so the text hashed here is the
     * text a provider receives.
     */
    static String text(String name) {
        try (InputStream in = Prompts.class.getResourceAsStream("/prompts/" + name)) {
            if (in == null) {
                throw new IllegalStateException("No prompt file " + name);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    /** The SHA-256 of a text, as a cassette names the prompt it was recorded against. */
    static String sha256(String text) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    /** What the model is told a tool does. */
    static String toolDescription(String tool) {
        return TOOLS.path("tools").path(tool).path("description").asString();
    }

    /** The JSON schema of a tool's parameters. */
    static String toolParameters(String tool) {
        return JSON.writeValueAsString(TOOLS.path("tools").path(tool).path("parameters"));
    }

    /** The sentence a tool refuses with, its blanks filled. */
    static String refusal(String key, Object... values) {
        String pattern = TOOLS.path("refusals").path(key).asString();
        if (pattern.isEmpty()) {
            throw new IllegalStateException("No refusal " + key);
        }
        return new MessageFormat(pattern, Locale.ROOT).format(values);
    }
}

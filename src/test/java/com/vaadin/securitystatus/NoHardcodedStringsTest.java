package com.vaadin.securitystatus;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * FND-06. Every string a user can read comes from the translation bundle, so
 * no Java source under the application package holds wording of its own.
 *
 * <p>
 * A literal counts as wording when it has two words separated by a space, or
 * is a single capitalised word. Keys, class names, paths and tokens are
 * neither. What is exempt is exempt by kind, not by file: a line that logs,
 * throws or declares JavaScript. A file whose literals are code in another
 * language is listed below with its reason.
 */
class NoHardcodedStringsTest {

    record Finding(Path file, int line, String literal) {
        @Override
        public String toString() {
            return file + ":" + line + " \"" + literal + "\"";
        }
    }

    private static final Path SOURCES = Path.of("src/main/java/com/vaadin/securitystatus");

    private static final Map<String, String> NOT_WORDING = Map.of("ThemeMemory.java",
            "the first paint script, JavaScript in a text block", "AdvisoryCatalogue.java",
            "SQL statements, read by the database", "TeamNavigation.java",
            "the script that keeps Findings closed, JavaScript in a text block");

    private static final Pattern BLOCK_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
    private static final Pattern TEXT_BLOCK = Pattern.compile("\"\"\"(.*?)\"\"\"", Pattern.DOTALL);
    private static final Pattern LITERAL = Pattern.compile("\"((?:\\\\.|[^\"\\\\])*)\"");
    private static final Pattern TWO_WORDS = Pattern.compile("[\\p{L}]{2,} [\\p{L}]{2,}");
    private static final Pattern ONE_CAPITALISED_WORD = Pattern.compile("\\p{Lu}\\p{Ll}{2,}[.!?:]?");
    private static final Pattern NOT_SHOWN_TO_A_USER = Pattern
            .compile("\\b(LOG|log|logger|LOGGER)\\.|\\bthrow new\\b|Exception\\(|@JsExpression");

    @Test
    void the_application_sources_hold_no_wording_of_their_own() throws IOException {
        assertThat(scan(SOURCES)).isEmpty();
    }

    @Test
    void a_planted_literal_in_a_view_is_found_and_the_file_is_named(@TempDir Path planted) throws IOException {
        Files.writeString(planted.resolve("PlantedView.java"), """
                package planted;

                // "A comment is not wording"
                class PlantedView {
                    PlantedView() {
                        add(new Button("Save changes"));
                        add(new Span("Fixed"));
                        add(new Span(getTranslation("planted.key")), "css-class", "24.9.20");
                        LOG.info("Not shown to anybody");
                    }
                }
                """);

        List<Finding> findings = scan(planted);

        assertThat(findings).extracting(Finding::literal).containsExactly("Save changes", "Fixed");
        assertThat(findings).allMatch(finding -> finding.toString().contains("PlantedView.java:"));
        assertThat(findings.getFirst().line()).isEqualTo(6);
    }

    @Test
    void the_exempt_files_exist_so_the_list_cannot_rot() throws IOException {
        try (var files = Files.walk(SOURCES)) {
            var names = files.map(path -> path.getFileName().toString()).toList();
            assertThat(names).containsAll(NOT_WORDING.keySet());
        }
    }

    static List<Finding> scan(Path root) throws IOException {
        var findings = new ArrayList<Finding>();
        try (var files = Files.walk(root)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).sorted().toList()) {
                if (NOT_WORDING.containsKey(file.getFileName().toString())) {
                    continue;
                }
                String source = blankKeepingLines(BLOCK_COMMENT, Files.readString(file));
                source = TEXT_BLOCK.matcher(source).replaceAll(block -> "\""
                        + block.group(1).replace("\\", "").replace("\"", "'").replace("$", "").replace("\n", " ") + "\""
                        + "\n".repeat((int) block.group().chars().filter(c -> c == '\n').count()));
                String[] lines = source.split("\n", -1);
                for (int i = 0; i < lines.length; i++) {
                    String line = lines[i].replaceFirst("^\\s*//.*", "");
                    if (NOT_SHOWN_TO_A_USER.matcher(line).find()) {
                        continue;
                    }
                    var literals = LITERAL.matcher(line);
                    while (literals.find()) {
                        String literal = literals.group(1);
                        if (TWO_WORDS.matcher(literal).find() || ONE_CAPITALISED_WORD.matcher(literal).matches()) {
                            findings.add(new Finding(file, i + 1, literal));
                        }
                    }
                }
            }
        }
        return findings;
    }

    private static String blankKeepingLines(Pattern pattern, String source) {
        return pattern.matcher(source)
                .replaceAll(match -> "\n".repeat((int) match.group().chars().filter(c -> c == '\n').count()));
    }
}

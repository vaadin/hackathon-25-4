package com.vaadin.securitystatus;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The test case tables of the feature documents are a contract, and a ticked
 * checkbox is a claim. This test holds both to the sources:
 *
 * <ul>
 * <li>every row of every table names a class whose suffix fits its tier, and
 * every browser test is on the closed list of {@code 08-testing.md};</li>
 * <li>a story whose tasks are all ticked has the classes its "Verified by"
 * line names;</li>
 * <li>a feature whose acceptance criteria are all ticked has every class of
 * its table.</li>
 * </ul>
 */
class SpecConsistencyTest {

    private static final Path SPECS = Path.of("specs");
    private static final Path TESTS = Path.of("src/test/java");

    private static final Pattern TABLE_ROW = Pattern
            .compile("^\\|\\s*([A-Z]+-\\d+)\\s*\\|.*\\|\\s*(\\w+)\\s*\\|\\s*`(\\w+)`\\s*\\|\\s*$");
    private static final Pattern TEST_CLASS = Pattern.compile("`(\\w+(?:Test|IT))`");
    private static final Pattern STORY = Pattern.compile("^### (US-[\\d.]+)");

    @Test
    void the_specifications_and_the_tests_agree() throws IOException {
        assertThat(problems(SPECS, TESTS)).isEmpty();
    }

    @Test
    void a_planted_phantom_test_name_in_a_finished_story_is_reported(@TempDir Path planted) throws IOException {
        Path specs = Files.createDirectories(planted.resolve("specs"));
        Path tests = Files.createDirectories(planted.resolve("tests/com/example"));
        Files.createDirectories(specs.resolve("features"));
        Files.createDirectories(specs.resolve("user-stories"));
        Files.writeString(specs.resolve("08-testing.md"), "| `RealIT` | proves |\n");
        Files.writeString(tests.resolve("RealBrowserlessTest.java"), "class RealBrowserlessTest {}");
        Files.writeString(specs.resolve("user-stories/epic-01-x.md"), """
                ### US-1.1: Done and true
                - [x] a task
                **Verified by:** `RealBrowserlessTest`

                ### US-1.2: Done and not true
                - [x] a task
                **Verified by:** `PhantomBrowserlessTest`

                ### US-1.3: Not done yet
                - [ ] a task
                **Verified by:** `LaterBrowserlessTest`
                """);
        Files.writeString(specs.resolve("features/01-x.md"), """
                ## Acceptance criteria
                - [x] all of it

                ## Test cases
                | Id | Given | When | Then | Tier | Verified by |
                | --- | --- | --- | --- | --- | --- |
                | X-01 | a | b | c | browserless | `RealBrowserlessTest` |
                | X-02 | a | b | c | browserless | `GhostBrowserlessTest` |
                | X-03 | a | b | c | unit | `WrongTierBrowserlessTest` |
                | X-04 | a | b | c | testbench | `UnlistedIT` |
                """);

        List<String> problems = problems(specs, planted.resolve("tests"));

        assertThat(problems).anyMatch(problem -> problem.contains("US-1.2") && problem.contains("PhantomBrowserlessTest"));
        assertThat(problems).anyMatch(problem -> problem.contains("X-02") && problem.contains("GhostBrowserlessTest"));
        assertThat(problems).anyMatch(problem -> problem.contains("X-03") && problem.contains("unit"));
        assertThat(problems).anyMatch(problem -> problem.contains("UnlistedIT") && problem.contains("closed list"));
        assertThat(problems).noneMatch(problem -> problem.contains("RealBrowserlessTest"));
        assertThat(problems).noneMatch(problem -> problem.contains("LaterBrowserlessTest"));
    }

    static List<String> problems(Path specs, Path tests) throws IOException {
        Set<String> existing = existingTestClasses(tests);
        String closedList = Files.readString(specs.resolve("08-testing.md"));
        var problems = new ArrayList<String>();

        for (Path feature : markdownIn(specs.resolve("features"))) {
            String text = Files.readString(feature);
            boolean finished = allTicked(section(text, "## Acceptance criteria"));
            for (String line : text.lines().toList()) {
                var row = TABLE_ROW.matcher(line);
                if (!row.matches()) {
                    continue;
                }
                String id = row.group(1), tier = row.group(2), name = row.group(3);
                String where = feature.getFileName() + " " + id + ": ";
                if (!fitsTier(tier, name)) {
                    problems.add(where + "`" + name + "` does not fit the tier " + tier);
                }
                if (name.endsWith("IT") && !closedList.contains("`" + name + "`")) {
                    problems.add(where + "`" + name + "` is not on the closed list of 08-testing.md");
                }
                if (finished && !existing.contains(name)) {
                    problems.add(where + "every criterion is ticked and `" + name + "` does not exist");
                }
            }
        }

        for (Path epic : markdownIn(specs.resolve("user-stories"))) {
            String story = null;
            var body = new StringBuilder();
            for (String line : (Files.readString(epic) + "\n### end").lines().toList()) {
                var heading = STORY.matcher(line);
                if (line.startsWith("### ") || line.startsWith("## ")) {
                    if (story != null && allTicked(body.toString())) {
                        String verifiedBy = body.toString().lines().filter(l -> l.contains("Verified by"))
                                .collect(Collectors.joining(" "));
                        var names = TEST_CLASS.matcher(verifiedBy);
                        while (names.find()) {
                            if (!existing.contains(names.group(1))) {
                                problems.add(epic.getFileName() + " " + story + ": every task is ticked and `"
                                        + names.group(1) + "` does not exist");
                            }
                        }
                    }
                    story = heading.find() ? heading.group(1) : null;
                    body.setLength(0);
                } else {
                    body.append(line).append('\n');
                }
            }
        }
        return problems;
    }

    private static boolean fitsTier(String tier, String name) {
        return switch (tier) {
        case "unit" -> name.endsWith("Test") && !name.endsWith("BrowserlessTest");
        case "browserless" -> name.endsWith("BrowserlessTest");
        case "testbench" -> name.endsWith("IT");
        default -> false;
        };
    }

    /** True when the text has checkboxes and none of them is open. */
    private static boolean allTicked(String text) {
        return text.contains("- [x]") && !text.contains("- [ ]");
    }

    private static String section(String text, String heading) {
        int start = text.indexOf(heading);
        if (start < 0) {
            return "";
        }
        int end = text.indexOf("\n## ", start + heading.length());
        return end < 0 ? text.substring(start) : text.substring(start, end);
    }

    private static List<Path> markdownIn(Path directory) throws IOException {
        try (var files = Files.list(directory)) {
            return files.filter(path -> path.toString().endsWith(".md")).sorted().toList();
        }
    }

    private static Set<String> existingTestClasses(Path tests) throws IOException {
        try (var files = Files.walk(tests)) {
            return files.map(path -> path.getFileName().toString()).filter(name -> name.endsWith(".java"))
                    .map(name -> name.substring(0, name.length() - ".java".length()))
                    .collect(Collectors.toCollection(TreeSet::new));
        }
    }
}

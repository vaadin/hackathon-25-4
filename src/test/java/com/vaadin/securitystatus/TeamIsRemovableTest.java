package com.vaadin.securitystatus;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * D-254. The application is two user interfaces, the customer's and the
 * team's, and the team's can be taken out by deleting its packages,
 * {@code team} and {@code ui.team}, its stylesheets under
 * {@code styles/team} and their imports. That holds only while nothing
 * outside those packages names anything inside them; the customer side
 * reaches the team through {@code ui.common.TeamEntrance} and {@code AiSurfaces}
 * beans, and the team registers its own routes.
 */
class TeamIsRemovableTest {

    private static final Path SOURCES = Path.of("src/main/java/com/vaadin/securitystatus");
    private static final Path TESTS = Path.of("src/test/java/com/vaadin/securitystatus");
    private static final Path STYLES = Path.of("src/main/resources/META-INF/resources/styles");

    @Test
    void nothing_outside_the_team_names_the_team() throws IOException {
        List<String> offenders;
        try (Stream<Path> files = Files.walk(SOURCES)) {
            offenders = files.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.startsWith(SOURCES.resolve("team")) && !path.startsWith(SOURCES.resolve("ui/team")))
                    .filter(path -> read(path).contains("securitystatus.team.")
                            || read(path).contains("securitystatus.ui.team."))
                    .map(Path::toString).toList();
        }
        assertThat(offenders).isEmpty();
    }

    /** The tests go with what they test, so deleting the team deletes its tests and the rest still compile. */
    @Test
    void no_test_outside_the_team_names_the_team() throws IOException {
        List<String> offenders;
        try (Stream<Path> files = Files.walk(TESTS)) {
            offenders = files.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.startsWith(TESTS.resolve("team")) && !path.startsWith(TESTS.resolve("ui/team")))
                    .filter(path -> !path.getFileName().toString().equals("TeamIsRemovableTest.java"))
                    .filter(path -> read(path).contains("securitystatus.team.")
                            || read(path).contains("securitystatus.ui.team."))
                    .map(Path::toString).toList();
        }
        assertThat(offenders).isEmpty();
    }

    @Test
    void every_user_interface_class_lives_under_ui() throws IOException {
        List<String> outside;
        try (Stream<Path> files = Files.walk(SOURCES)) {
            outside = files.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.startsWith(SOURCES.resolve("ui")))
                    .filter(path -> {
                        String text = read(path);
                        return text.contains("@Route(") || text.contains("extends Composite<")
                                || text.contains("extends AppLayout");
                    }).map(Path::toString).toList();
        }
        assertThat(outside).isEmpty();
    }

    @Test
    void the_customer_and_common_styles_do_not_style_the_team() throws IOException {
        List<String> offenders;
        try (Stream<Path> files = Files.walk(STYLES)) {
            offenders = files.filter(path -> path.toString().endsWith(".css"))
                    .filter(path -> !path.startsWith(STYLES.resolve("team")))
                    .filter(path -> read(path).matches("(?s).*\\.(internal-|customer-team|assistant-|explorer-|workbench-|diagnostics-).*"))
                    .map(Path::toString).toList();
        }
        assertThat(offenders).isEmpty();
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException unreadable) {
            throw new IllegalStateException(unreadable);
        }
    }
}

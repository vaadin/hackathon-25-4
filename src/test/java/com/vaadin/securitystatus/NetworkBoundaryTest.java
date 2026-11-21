package com.vaadin.securitystatus;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * One door to the network. No class outside the {@code data} package imports
 * an HTTP client, so no view, layout or tool can fetch anything: all of them
 * are left with {@code SecurityDataService}, which answers from the cache. The
 * client and the cache themselves are package private, which the compiler
 * holds.
 */
class NetworkBoundaryTest {

    private static final Path SOURCES = Path.of("src/main/java/com/vaadin/securitystatus");

    private static final Pattern HTTP_CLIENT = Pattern.compile("^import\\s+(?:static\\s+)?(" + String.join("|",
            "java\\.net\\.http\\.", "java\\.net\\.(?:URL|URLConnection|HttpURLConnection|Socket)\\b",
            "javax\\.net\\.", "org\\.springframework\\.web\\.client\\.",
            "org\\.springframework\\.http\\.client\\.", "org\\.springframework\\.web\\.reactive\\.function\\.client\\.",
            "org\\.springframework\\.web\\.service\\.", "org\\.springframework\\.boot\\.(?:restclient|webclient|http\\.client)\\.",
            "org\\.apache\\.hc\\.", "org\\.apache\\.http\\.", "okhttp3\\.", "reactor\\.netty\\.http\\.client\\.",
            "org\\.eclipse\\.jetty\\.client\\.") + ")[\\w.*]*;", Pattern.MULTILINE);

    @Test
    void no_class_outside_the_data_package_imports_an_http_client() throws IOException {
        assertThat(violations(SOURCES)).isEmpty();
    }

    @Test
    void the_data_package_is_where_the_http_client_is() throws IOException {
        // The control: the scan sees the imports it forbids elsewhere.
        assertThat(importsOf(SOURCES.resolve("data/PagesClient.java"))).isNotEmpty();
    }

    @Test
    void a_planted_fetch_in_a_view_is_found_and_the_file_is_named(@TempDir Path planted) throws IOException {
        Files.createDirectories(planted.resolve("status"));
        Files.createDirectories(planted.resolve("data/model"));
        Files.writeString(planted.resolve("status/PlantedView.java"), """
                package planted.status;

                import java.net.http.HttpClient;
                import org.springframework.web.client.RestClient;
                import com.vaadin.flow.component.html.Div;
                import java.net.URI;

                class PlantedView extends Div {
                }
                """);
        Files.writeString(planted.resolve("data/Allowed.java"), """
                package planted.data;

                import org.springframework.web.client.RestClient;

                class Allowed {
                }
                """);
        Files.writeString(planted.resolve("data/model/NotEvenHere.java"), """
                package planted.data.model;

                import java.net.HttpURLConnection;

                record NotEvenHere() {
                }
                """);

        List<String> violations = violations(planted);

        assertThat(violations).hasSize(3);
        assertThat(violations).filteredOn(violation -> violation.contains("PlantedView.java")).hasSize(2)
                .anyMatch(violation -> violation.contains("java.net.http.HttpClient"))
                .anyMatch(violation -> violation.contains("org.springframework.web.client.RestClient"));
        assertThat(violations).anyMatch(violation -> violation.contains("NotEvenHere.java"));
        assertThat(violations).noneMatch(violation -> violation.contains("Allowed.java"));
    }

    static List<String> violations(Path root) throws IOException {
        Path door = root.resolve("data");
        var violations = new ArrayList<String>();
        try (var files = Files.walk(root)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).sorted().toList()) {
                if (file.getParent().equals(door)) {
                    continue;
                }
                for (String imported : importsOf(file)) {
                    violations.add(root.relativize(file) + " imports " + imported);
                }
            }
        }
        return violations;
    }

    private static List<String> importsOf(Path file) throws IOException {
        var imports = new ArrayList<String>();
        var matcher = HTTP_CLIENT.matcher(Files.readString(file));
        while (matcher.find()) {
            imports.add(matcher.group().replaceFirst("^import\\s+(?:static\\s+)?", "").replace(";", ""));
        }
        return imports;
    }
}

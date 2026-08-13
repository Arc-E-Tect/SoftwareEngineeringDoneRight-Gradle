package com.arc_e_tect.gradle.shadow.report;

import com.arc_e_tect.gradle.detector.core.model.Endpoint;
import com.arc_e_tect.gradle.detector.core.model.HttpVerb;
import com.arc_e_tect.gradle.detector.core.progress.ContractProgressRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ShadowApiReportWriter")
class ShadowApiReportWriterTest {

    private final ShadowApiReportWriter writer = new ShadowApiReportWriter();

    @Test
    @DisplayName("reports that no shadows were found when the list is empty")
    void reportsNoShadowsFound(@TempDir Path tempDir) throws Exception {
        File output = new File(tempDir.toFile(), "report.adoc");

        writer.write(output, 3, List.of(), "1.0.0");

        String content = Files.readString(output.toPath());
        assertThat(content)
                .contains("= Shadow API Report")
                .contains("Scanned 3 endpoint(s)")
                .contains("None found. Every endpoint exposed by the scanned controllers is described");
    }

    @Test
    @DisplayName("lists every shadow grouped by declaring controller")
    void listsShadowsGroupedByController(@TempDir Path tempDir) throws Exception {
        File output = new File(tempDir.toFile(), "report.adoc");
        List<Endpoint> shadows = List.of(
                new Endpoint(HttpVerb.POST, "/api/users/{id}", "com.example.UserController",
                        "createUser(Long)", "UserController.java", 42),
                new Endpoint(HttpVerb.GET, "/api/orders", "com.example.OrderController",
                        "listOrders()", "OrderController.java", 10));

        writer.write(output, 5, shadows, "1.0.0");

        String content = Files.readString(output.toPath());
        assertThat(content)
                .contains("Scanned 5 endpoint(s)")
                .contains("2 of them are not described")
                .contains("=== com.example.UserController")
                .contains("=== com.example.OrderController")
                .contains("/api/users/{id}")
                .contains("createUser(Long)")
                .contains("UserController.java")
                .contains("/api/orders")
                .contains("listOrders()");
    }

    @Test
    @DisplayName("creates the parent directory when it does not yet exist")
    void createsParentDirectory(@TempDir Path tempDir) throws Exception {
        File output = new File(tempDir.toFile(), "nested/dir/report.adoc");

        writer.write(output, 0, List.of(), "1.0.0");

        assertThat(output).exists();
    }

    @Test
    @DisplayName("uses singular phrasing for exactly one shadow")
    void usesSingularPhrasingForOneShadow(@TempDir Path tempDir) throws Exception {
        File output = new File(tempDir.toFile(), "report.adoc");
        List<Endpoint> shadows = List.of(
                new Endpoint(HttpVerb.GET, "/api/orders", "com.example.OrderController",
                        "listOrders()", "OrderController.java", 10));

        writer.write(output, 1, shadows, "1.0.0");

        assertThat(Files.readString(output.toPath())).contains("1 of them is not described");
    }

    @Test
    @DisplayName("includes the given system under test version")
    void includesSystemUnderTestVersion(@TempDir Path tempDir) throws Exception {
        File output = new File(tempDir.toFile(), "report.adoc");

        writer.write(output, 0, List.of(), "v2.3.1");

        assertThat(Files.readString(output.toPath())).contains("System Under Test version: v2.3.1");
    }

    @Test
    @DisplayName("includes the bundled preamble explaining what a shadow API is")
    void includesBundledPreamble(@TempDir Path tempDir) throws Exception {
        File output = new File(tempDir.toFile(), "report.adoc");

        writer.write(output, 0, List.of(), "1.0.0");

        String content = Files.readString(output.toPath());
        assertThat(content)
                .contains("What Is a Shadow API?")
                .contains("Zombie API")
                .contains("owasp.org/API-Security");
    }

    @Test
    @DisplayName("preamble content matches the bundled shadow-api-preamble.adoc resource verbatim")
    void preambleMatchesBundledResource(@TempDir Path tempDir) throws Exception {
        File output = new File(tempDir.toFile(), "report.adoc");

        writer.write(output, 0, List.of(), "1.0.0");

        String content = Files.readString(output.toPath());
        String preamble;
        try (var stream = getClass().getClassLoader()
                .getResourceAsStream(ShadowApiReportWriter.PREAMBLE_RESOURCE)) {
            preamble = new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }

        assertThat(content).contains(preamble);
    }

    @Test
    @DisplayName("omits the Progress Over Time section when contract history is empty")
    void omitsProgressOverTimeSectionWhenHistoryIsEmpty(@TempDir Path tempDir) throws Exception {
        File output = new File(tempDir.toFile(), "report.adoc");

        writer.write(output, 0, List.of(), "1.0.0", Map.of());

        assertThat(Files.readString(output.toPath())).doesNotContain("Progress Over Time");
    }

    @Test
    @DisplayName("adds a Progress Over Time section with a human-friendly Tracked since when contract history is non-empty")
    void addsProgressOverTimeSectionWhenHistoryIsNonEmpty(@TempDir Path tempDir) throws Exception {
        File output = new File(tempDir.toFile(), "report.adoc");
        Map<String, ContractProgressRecord> history = Map.of("fp1", new ContractProgressRecord(
                "fp1", HttpVerb.GET, "/orders/{id}", "com.example.OrderController",
                Instant.parse("2026-01-14T09:02:11Z"), Instant.parse("2026-02-20T11:15:44Z"), null,
                Instant.parse("2026-02-20T11:15:44Z"), null));

        writer.write(output, 0, List.of(), "1.0.0", history);

        String content = Files.readString(output.toPath());
        assertThat(content)
                .contains("== Progress Over Time")
                .contains("2026-01-14 09:02:11 UTC")
                .doesNotContain("2026-01-14T09:02:11Z");
    }
}

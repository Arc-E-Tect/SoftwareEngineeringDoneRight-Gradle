package com.arc_e_tect.gradle.mirage.report;

import com.arc_e_tect.gradle.detector.core.model.HttpVerb;
import com.arc_e_tect.gradle.detector.core.openapi.DescribedEndpoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MirageApiReportWriter")
class MirageApiReportWriterTest {

    private final MirageApiReportWriter writer = new MirageApiReportWriter();

    @Test
    @DisplayName("reports that no mirages were found when the list is empty")
    void reportsNoMiragesFound(@TempDir Path tempDir) throws Exception {
        File output = new File(tempDir.toFile(), "report.adoc");

        writer.write(output, 3, List.of(), "1.0.0");

        String content = Files.readString(output.toPath());
        assertThat(content)
                .contains("= Mirage API Report")
                .contains("Scanned 3 endpoint(s)")
                .contains("None found. Every endpoint described in the OpenAPI documentation is implemented");
    }

    @Test
    @DisplayName("lists every mirage grouped by its primary tag")
    void listsMiragesGroupedByTag(@TempDir Path tempDir) throws Exception {
        File output = new File(tempDir.toFile(), "report.adoc");
        List<DescribedEndpoint> mirages = List.of(
                new DescribedEndpoint(HttpVerb.POST, "/api/users/{id}", "createUser", List.of("Users")),
                new DescribedEndpoint(HttpVerb.GET, "/api/orders", "listOrders", List.of("Orders")));

        writer.write(output, 5, mirages, "1.0.0");

        String content = Files.readString(output.toPath());
        assertThat(content)
                .contains("Scanned 5 endpoint(s)")
                .contains("2 of them are not implemented")
                .contains("=== Users")
                .contains("=== Orders")
                .contains("/api/users/{id}")
                .contains("createUser")
                .contains("/api/orders")
                .contains("listOrders");
    }

    @Test
    @DisplayName("groups an untagged mirage under the untagged heading")
    void groupsUntaggedMirageUnderUntaggedHeading(@TempDir Path tempDir) throws Exception {
        File output = new File(tempDir.toFile(), "report.adoc");
        List<DescribedEndpoint> mirages = List.of(
                new DescribedEndpoint(HttpVerb.GET, "/api/ping", "ping", List.of()));

        writer.write(output, 1, mirages, "1.0.0");

        assertThat(Files.readString(output.toPath())).contains("=== (untagged)");
    }

    @Test
    @DisplayName("treats a blank tag the same as no tag at all")
    void treatsBlankTagAsUntagged(@TempDir Path tempDir) throws Exception {
        File output = new File(tempDir.toFile(), "report.adoc");
        List<DescribedEndpoint> mirages = List.of(
                new DescribedEndpoint(HttpVerb.GET, "/api/ping", "ping", List.of("  ")));

        writer.write(output, 1, mirages, "1.0.0");

        String content = Files.readString(output.toPath());
        assertThat(content).contains("=== (untagged)").doesNotContain("=== " + "  ");
    }

    @Test
    @DisplayName("prints (none) for a blank operationId, same as a missing one")
    void printsNoneForBlankOperationId(@TempDir Path tempDir) throws Exception {
        File output = new File(tempDir.toFile(), "report.adoc");
        List<DescribedEndpoint> mirages = List.of(
                new DescribedEndpoint(HttpVerb.GET, "/api/ping", "  ", List.of()));

        writer.write(output, 1, mirages, "1.0.0");

        assertThat(Files.readString(output.toPath())).contains("| (none)");
    }

    @Test
    @DisplayName("prints (none) for a mirage with no operationId or tags")
    void printsNoneForMissingOperationIdAndTags(@TempDir Path tempDir) throws Exception {
        File output = new File(tempDir.toFile(), "report.adoc");
        List<DescribedEndpoint> mirages = List.of(
                new DescribedEndpoint(HttpVerb.GET, "/api/ping", null, List.of()));

        writer.write(output, 1, mirages, "1.0.0");

        String content = Files.readString(output.toPath());
        assertThat(content).contains("| (none)");
    }

    @Test
    @DisplayName("creates the parent directory when it does not yet exist")
    void createsParentDirectory(@TempDir Path tempDir) throws Exception {
        File output = new File(tempDir.toFile(), "nested/dir/report.adoc");

        writer.write(output, 0, List.of(), "1.0.0");

        assertThat(output).exists();
    }

    @Test
    @DisplayName("uses singular phrasing for exactly one mirage")
    void usesSingularPhrasingForOneMirage(@TempDir Path tempDir) throws Exception {
        File output = new File(tempDir.toFile(), "report.adoc");
        List<DescribedEndpoint> mirages = List.of(
                new DescribedEndpoint(HttpVerb.GET, "/api/orders", "listOrders", List.of()));

        writer.write(output, 1, mirages, "1.0.0");

        assertThat(Files.readString(output.toPath())).contains("1 of them is not implemented");
    }

    @Test
    @DisplayName("includes the given system under test version")
    void includesSystemUnderTestVersion(@TempDir Path tempDir) throws Exception {
        File output = new File(tempDir.toFile(), "report.adoc");

        writer.write(output, 0, List.of(), "v2.3.1");

        assertThat(Files.readString(output.toPath())).contains("System Under Test version: v2.3.1");
    }

    @Test
    @DisplayName("includes the bundled preamble explaining what a mirage API is")
    void includesBundledPreamble(@TempDir Path tempDir) throws Exception {
        File output = new File(tempDir.toFile(), "report.adoc");

        writer.write(output, 0, List.of(), "1.0.0");

        String content = Files.readString(output.toPath());
        assertThat(content)
                .contains("What Is a Mirage API?")
                .contains("Shadow API")
                .contains("owasp.org/API-Security");
    }

    @Test
    @DisplayName("uses WireMock stub wording when scanMocks is true")
    void usesWireMockStubWordingWhenScanningMocks(@TempDir Path tempDir) throws Exception {
        File output = new File(tempDir.toFile(), "report.adoc");
        List<DescribedEndpoint> mirages = List.of(
                new DescribedEndpoint(HttpVerb.GET, "/api/orders", "listOrders", List.of()));

        writer.write(output, 1, mirages, "1.0.0", true);

        assertThat(Files.readString(output.toPath()))
                .contains("1 of them is not backed by any WireMock stub");
    }

    @Test
    @DisplayName("uses WireMock stub wording for the empty case when scanMocks is true")
    void usesWireMockStubWordingForEmptyCaseWhenScanningMocks(@TempDir Path tempDir) throws Exception {
        File output = new File(tempDir.toFile(), "report.adoc");

        writer.write(output, 3, List.of(), "1.0.0", true);

        assertThat(Files.readString(output.toPath()))
                .contains("None found. Every endpoint described in the OpenAPI documentation is backed by a WireMock stub.");
    }

    @Test
    @DisplayName("preamble content matches the bundled mirage-api-preamble.adoc resource verbatim")
    void preambleMatchesBundledResource(@TempDir Path tempDir) throws Exception {
        File output = new File(tempDir.toFile(), "report.adoc");

        writer.write(output, 0, List.of(), "1.0.0");

        String content = Files.readString(output.toPath());
        String preamble;
        try (var stream = getClass().getClassLoader()
                .getResourceAsStream(MirageApiReportWriter.PREAMBLE_RESOURCE)) {
            preamble = new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }

        assertThat(content).contains(preamble);
    }
}

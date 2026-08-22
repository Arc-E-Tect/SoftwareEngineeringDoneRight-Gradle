package com.arc_e_tect.gradle.mirage.report;

import com.arc_e_tect.gradle.detector.core.model.HttpVerb;
import com.arc_e_tect.gradle.detector.core.openapi.DescribedEndpoint;
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
    @DisplayName("omits the WARNING admonition when there are no warnings")
    void omitsWarningAdmonitionWhenThereAreNoWarnings(@TempDir Path tempDir) throws Exception {
        File output = new File(tempDir.toFile(), "report.adoc");

        writer.write(output, 0, List.of(), "1.0.0", List.of(), Map.of());

        assertThat(Files.readString(output.toPath())).doesNotContain("[WARNING]");
    }

    @Test
    @DisplayName("renders every warning as a bullet inside a single WARNING admonition block")
    void rendersWarningsAsAWarningAdmonition(@TempDir Path tempDir) throws Exception {
        File output = new File(tempDir.toFile(), "report.adoc");

        writer.write(output, 0, List.of(), "1.0.0",
                List.of("`rootDocument` is not configured yet.", "Configured `controllerDirs` entry does not exist yet: `/tmp/missing`."),
                Map.of());

        String content = Files.readString(output.toPath());
        assertThat(content)
                .contains("[WARNING]")
                .contains("====")
                .contains("* `rootDocument` is not configured yet.")
                .contains("* Configured `controllerDirs` entry does not exist yet: `/tmp/missing`.");
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
                Instant.parse("2026-01-14T09:02:11Z"), Instant.parse("2026-02-20T11:15:44Z"), null, null,
                Instant.parse("2026-02-20T11:15:44Z"), null));

        writer.write(output, 0, List.of(), "1.0.0", history);

        String content = Files.readString(output.toPath());
        assertThat(content)
                .contains("== Progress Over Time")
                .contains("2026-01-14 09:02:11 UTC")
                .doesNotContain("2026-01-14T09:02:11Z");
    }

    @Test
    @DisplayName("omits the Excluded Mirage APIs section when there are no excluded mirages")
    void omitsExcludedSectionWhenEmpty(@TempDir Path tempDir) throws Exception {
        File output = new File(tempDir.toFile(), "report.adoc");

        writer.write(output, 0, List.of(), List.of(), "1.0.0", List.of(), Map.of());

        assertThat(Files.readString(output.toPath())).doesNotContain("== Excluded Mirage APIs");
    }

    @Test
    @DisplayName("lists an excluded mirage with its stub status under Excluded Mirage APIs")
    void listsExcludedMirageWithStubStatus(@TempDir Path tempDir) throws Exception {
        File output = new File(tempDir.toFile(), "report.adoc");
        DescribedEndpoint health = new DescribedEndpoint(HttpVerb.GET, "/actuator/health", "health", List.of());

        writer.write(output, 1, List.of(), List.of(new ExcludedMirage(health, StubStatus.STUBBED)),
                "1.0.0", List.of(), Map.of());

        String content = Files.readString(output.toPath());
        assertThat(content)
                .contains("== Excluded Mirage APIs")
                .contains("/actuator/health")
                .contains("health")
                .contains("| Yes");
    }

    @Test
    @DisplayName("renders each stub status as its own label")
    void rendersEachStubStatusAsItsOwnLabel(@TempDir Path tempDir) throws Exception {
        File output = new File(tempDir.toFile(), "report.adoc");
        List<ExcludedMirage> excluded = List.of(
                new ExcludedMirage(new DescribedEndpoint(HttpVerb.GET, "/a", "a", List.of()), StubStatus.STUBBED),
                new ExcludedMirage(new DescribedEndpoint(HttpVerb.GET, "/b", "b", List.of()), StubStatus.NOT_STUBBED),
                new ExcludedMirage(new DescribedEndpoint(HttpVerb.GET, "/c", "c", List.of()), StubStatus.NOT_SCANNED));

        writer.write(output, 3, List.of(), excluded, "1.0.0", List.of(), Map.of());

        String content = Files.readString(output.toPath());
        assertThat(content).contains("| Yes").contains("| No").contains("| Not scanned");
    }

    @Test
    @DisplayName("still writes the Excluded Mirage APIs section when the main Mirage APIs list is also non-empty")
    void writesExcludedSectionAlongsideNonEmptyMainList(@TempDir Path tempDir) throws Exception {
        File output = new File(tempDir.toFile(), "report.adoc");
        List<DescribedEndpoint> mirages = List.of(
                new DescribedEndpoint(HttpVerb.POST, "/api/users/{id}", "createUser", List.of("Users")));
        List<ExcludedMirage> excluded = List.of(new ExcludedMirage(
                new DescribedEndpoint(HttpVerb.GET, "/actuator/health", "health", List.of()), StubStatus.NOT_SCANNED));

        writer.write(output, 2, mirages, excluded, "1.0.0", List.of(), Map.of());

        String content = Files.readString(output.toPath());
        assertThat(content)
                .contains("=== Users")
                .contains("createUser")
                .contains("== Excluded Mirage APIs")
                .contains("/actuator/health");
    }
}

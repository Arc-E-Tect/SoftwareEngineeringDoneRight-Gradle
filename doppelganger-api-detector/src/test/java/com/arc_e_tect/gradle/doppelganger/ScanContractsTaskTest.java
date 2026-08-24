package com.arc_e_tect.gradle.doppelganger;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ScanContractsTask")
class ScanContractsTaskTest {

    @TempDir
    Path tempDir;

    private Project project;
    private File controllerDir;
    private File testDir;
    private File reportDir;

    @BeforeEach
    void setUp() throws Exception {
        project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build();

        controllerDir = new File(tempDir.toFile(), "src/main/java/com/example");
        Files.createDirectories(controllerDir.toPath());
        Files.writeString(controllerDir.toPath().resolve("FoobarController.java"), """
                package com.example;

                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.RequestMapping;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                @RequestMapping("/v1/foobars")
                public class FoobarController {

                    @GetMapping
                    public String listFoobars() { return "[]"; }
                }
                """);

        testDir = new File(tempDir.toFile(), "src/test/java/com/example");
        Files.createDirectories(testDir.toPath());

        reportDir = new File(tempDir.toFile(), "build/reports/doppelganger-api-detector");
    }

    @Test
    @DisplayName("the /v1/foobars worked example: 200 covered by 2 tests, 404 covered by 1")
    void worksThroughTheFoobarsExample() throws Exception {
        writeFoobarTests();
        ScanContractsTask task = configuredTask(openApiFixture("foobars.yaml"));
        task.getIncludeResponseCoverage().set(true);

        task.generate();

        String content = Files.readString(new File(reportDir, "contract-coverage.adoc").toPath());
        assertThat(content).contains("== Response Code Coverage");
        assertThat(content).containsSubsequence("| 200", "| 2");
        assertThat(content).containsSubsequence("| 404", "| 1");
    }

    @Test
    @DisplayName("reports the declared response code count and contract test count per endpoint")
    void reportsDeclaredResponseCodeCountAndContractTestCount() throws Exception {
        writeFoobarTests();
        ScanContractsTask task = configuredTask(openApiFixture("foobars.yaml"));

        task.generate();

        String content = Files.readString(new File(reportDir, "contract-coverage.adoc").toPath());
        assertThat(content).contains("GET");
        assertThat(content).contains("/v1/foobars");
        assertThat(content).contains("2 (200, 404)");
        assertThat(content).contains("| 3");
    }

    @Test
    @DisplayName("when includeResponseCoverage is false, no per-response-code breakdown section is written")
    void noBreakdownSectionWhenResponseCoverageDisabled() throws Exception {
        writeFoobarTests();
        ScanContractsTask task = configuredTask(openApiFixture("foobars.yaml"));

        task.generate();

        String content = Files.readString(new File(reportDir, "contract-coverage.adoc").toPath());
        assertThat(content).doesNotContain("== Response Code Coverage");
    }

    @Test
    @DisplayName("does not throw when rootDocument is not configured, and warns instead")
    void doesNotThrowWhenRootDocumentNotConfigured() throws Exception {
        ScanContractsTask task = newTask();
        task.getControllerDirs().from(controllerDir);
        task.getTestDirs().from(testDir);
        task.getReportDir().set(reportDir);
        task.getReportFileName().set("contract-coverage.adoc");
        task.getUseRestDocs().set(true);
        task.getUseOpenApiRequestValidator().set(false);
        task.getUseSpringCloudContract().set(false);
        task.getSystemUnderTestVersion().set("1.0.0");

        task.generate();

        String content = Files.readString(new File(reportDir, "contract-coverage.adoc").toPath());
        assertThat(content)
                .contains("[WARNING]")
                .contains("`rootDocument` is not configured yet")
                .contains("None found");
    }

    @Test
    @DisplayName("does not throw when none of the configured controllerDirs exist")
    void doesNotThrowWhenNoControllerDirsExist() throws Exception {
        File missing = new File(tempDir.toFile(), "src/main/java/does-not-exist");
        ScanContractsTask task = newTask();
        task.getControllerDirs().from(missing);
        task.getTestDirs().from(testDir);
        task.getRootDocument().set(openApiFixture("foobars.yaml"));
        task.getReportDir().set(reportDir);
        task.getReportFileName().set("contract-coverage.adoc");
        task.getUseRestDocs().set(true);
        task.getUseOpenApiRequestValidator().set(false);
        task.getUseSpringCloudContract().set(false);
        task.getSystemUnderTestVersion().set("1.0.0");

        task.generate();

        String content = Files.readString(new File(reportDir, "contract-coverage.adoc").toPath());
        assertThat(content)
                .contains("[WARNING]")
                .contains("Contract scanning was skipped for this run");
    }

    @Test
    @DisplayName("fails eagerly when every verification source is disabled")
    void failsEagerlyWhenEveryVerificationSourceDisabled() throws Exception {
        ScanContractsTask task = configuredTask(openApiFixture("foobars.yaml"));
        task.getUseRestDocs().set(false);

        assertThatThrownBy(task::generate)
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("at least one of useRestDocs");
    }

    @Test
    @DisplayName("fails eagerly when useSpringCloudContract is enabled without contractsDir")
    void failsEagerlyWhenSpringCloudContractEnabledWithoutContractsDir() throws Exception {
        ScanContractsTask task = configuredTask(openApiFixture("foobars.yaml"));
        task.getUseSpringCloudContract().set(true);

        assertThatThrownBy(task::generate)
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("contractsDir must be configured");
    }

    @Test
    @DisplayName("fails eagerly when trackResponseCoverageHistory is enabled without includeResponseCoverage")
    void failsEagerlyWhenTrackHistoryEnabledWithoutIncludeResponseCoverage() throws Exception {
        ScanContractsTask task = configuredTask(openApiFixture("foobars.yaml"));
        task.getTrackResponseCoverageHistory().set(true);
        task.getIncludeResponseCoverage().set(false);

        assertThatThrownBy(task::generate)
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("trackResponseCoverageHistory requires includeResponseCoverage");
    }

    @Test
    @DisplayName("persists response coverage history when tracking and updating are both enabled")
    void persistsResponseCoverageHistoryWhenEnabled() throws Exception {
        writeFoobarTests();
        File historyFile = new File(tempDir.toFile(), "response-coverage-history.ndjson");
        ScanContractsTask task = configuredTask(openApiFixture("foobars.yaml"));
        task.getIncludeResponseCoverage().set(true);
        task.getTrackResponseCoverageHistory().set(true);
        task.getResponseCoverageHistoryFile().set(historyFile);
        task.getUpdateResponseCoverageHistory().set(true);

        task.generate();

        String historyContent = Files.readString(historyFile.toPath());
        assertThat(historyContent).contains("\"responseCode\":\"200\",\"testCount\":2");
        assertThat(historyContent).contains("\"responseCode\":\"404\",\"testCount\":1");
    }

    @Test
    @DisplayName("does not write the history file when updateResponseCoverageHistory is false")
    void doesNotWriteHistoryFileWhenUpdateDisabled() throws Exception {
        writeFoobarTests();
        File historyFile = new File(tempDir.toFile(), "response-coverage-history.ndjson");
        ScanContractsTask task = configuredTask(openApiFixture("foobars.yaml"));
        task.getIncludeResponseCoverage().set(true);
        task.getTrackResponseCoverageHistory().set(true);
        task.getResponseCoverageHistoryFile().set(historyFile);
        task.getUpdateResponseCoverageHistory().set(false);

        task.generate();

        assertThat(historyFile).doesNotExist();
    }

    @Test
    @DisplayName("an excluded endpoint is not reported")
    void excludedEndpointIsNotReported() throws Exception {
        writeFoobarTests();
        ScanContractsTask task = configuredTask(openApiFixture("foobars.yaml"));
        task.getExcludePaths().set(List.of("/v1/foobars"));

        task.generate();

        String content = Files.readString(new File(reportDir, "contract-coverage.adoc").toPath());
        assertThat(content).contains("None found");
    }

    @Test
    @DisplayName("an unrecognised excludeWellKnown name fails the build")
    void unrecognisedExcludeWellKnownNameFailsBuild() throws Exception {
        ScanContractsTask task = configuredTask(openApiFixture("foobars.yaml"));
        task.getExcludeWellKnown().set(List.of("not-a-real-set"));

        assertThatThrownBy(task::generate)
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("not-a-real-set");
    }

    private void writeFoobarTests() throws Exception {
        Files.writeString(testDir.toPath().resolve("FoobarControllerDocTest.java"), """
                package com.example;

                class FoobarControllerDocTest {

                    void listFoobarsOk1() throws Exception {
                        mockMvc.perform(get("/v1/foobars"))
                                .andExpect(status().isOk())
                                .andDo(document("list-foobars-ok-1"));
                    }

                    void listFoobarsOk2() throws Exception {
                        mockMvc.perform(get("/v1/foobars"))
                                .andExpect(status().isOk())
                                .andDo(document("list-foobars-ok-2"));
                    }

                    void listFoobarsNotFound() throws Exception {
                        mockMvc.perform(get("/v1/foobars"))
                                .andExpect(status().isNotFound())
                                .andDo(document("list-foobars-not-found"));
                    }
                }
                """);
    }

    private ScanContractsTask configuredTask(File rootDocument) {
        ScanContractsTask task = newTask();
        task.getControllerDirs().from(controllerDir);
        task.getTestDirs().from(testDir);
        task.getRootDocument().set(rootDocument);
        task.getReportDir().set(reportDir);
        task.getReportFileName().set("contract-coverage.adoc");
        task.getUseRestDocs().set(true);
        task.getUseOpenApiRequestValidator().set(false);
        task.getUseSpringCloudContract().set(false);
        task.getSystemUnderTestVersion().set("1.0.0");
        return task;
    }

    private ScanContractsTask newTask() {
        ScanContractsTask task = project.getTasks().create("scanContractsUnderTest", ScanContractsTask.class);
        task.getIncludeResponseCoverage().set(false);
        task.getTrackResponseCoverageHistory().set(false);
        return task;
    }

    private File openApiFixture(String name) throws Exception {
        File dir = new File(tempDir.toFile(), "openapi");
        Files.createDirectories(dir.toPath());
        File file = new File(dir, name);
        String content = switch (name) {
            case "foobars.yaml" -> """
                    openapi: 3.0.3
                    info:
                      title: Test API
                      version: "1.0"
                    paths:
                      /v1/foobars:
                        get:
                          operationId: listFoobars
                          responses:
                            '200':
                              description: OK
                            '404':
                              description: Not Found
                    """;
            default -> throw new IllegalArgumentException("Unknown fixture: " + name);
        };
        Files.writeString(file.toPath(), content);
        return file;
    }
}

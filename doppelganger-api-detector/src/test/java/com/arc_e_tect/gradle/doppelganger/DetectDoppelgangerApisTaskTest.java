package com.arc_e_tect.gradle.doppelganger;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DetectDoppelgangerApisTask")
class DetectDoppelgangerApisTaskTest {

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
        Files.writeString(controllerDir.toPath().resolve("OrderController.java"), """
                package com.example;

                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.RequestMapping;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                @RequestMapping("/orders")
                public class OrderController {

                    @GetMapping
                    public String listOrders() { return "[]"; }

                    @GetMapping("/{id}")
                    public String getOrder() { return "{}"; }
                }
                """);

        testDir = new File(tempDir.toFile(), "src/test/java/com/example");
        Files.createDirectories(testDir.toPath());
        Files.writeString(testDir.toPath().resolve("OrderControllerDocTest.java"), """
                package com.example;

                class OrderControllerDocTest {

                    void getOrder() throws Exception {
                        mockMvc.perform(get("/orders/{id}", 1))
                                .andDo(document("get-order"));
                    }
                }
                """);

        reportDir = new File(tempDir.toFile(), "build/reports/doppelganger-api-detector");
    }

    @Test
    @DisplayName("does not throw when rootDocument is not configured, and warns instead")
    void doesNotThrowWhenRootDocumentNotConfigured() throws Exception {
        DetectDoppelgangerApisTask task = newTask();
        task.getControllerDirs().from(controllerDir);
        task.getTestDirs().from(testDir);
        task.getReportDir().set(reportDir);
        task.getReportFileName().set("doppelganger-apis.adoc");
        task.getFailOnDoppelganger().set(true);
        task.getUseRestDocs().set(true);
        task.getUseOpenApiRequestValidator().set(false);
        task.getUseSpringCloudContract().set(false);
        task.getSystemUnderTestVersion().set("1.0.0");

        task.generate();

        String content = Files.readString(new File(reportDir, "doppelganger-apis.adoc").toPath());
        assertThat(content)
                .contains("[WARNING]")
                .contains("`rootDocument` is not configured yet")
                .contains("None found.");
    }

    @Test
    @DisplayName("does not throw when the configured rootDocument file does not exist")
    void doesNotThrowWhenRootDocumentFileDoesNotExist() throws Exception {
        File missing = new File(tempDir.toFile(), "openapi/does-not-exist.yaml");
        DetectDoppelgangerApisTask task = newTask();
        task.getControllerDirs().from(controllerDir);
        task.getTestDirs().from(testDir);
        task.getRootDocument().set(missing);
        task.getReportDir().set(reportDir);
        task.getReportFileName().set("doppelganger-apis.adoc");
        task.getFailOnDoppelganger().set(true);
        task.getUseRestDocs().set(true);
        task.getUseOpenApiRequestValidator().set(false);
        task.getUseSpringCloudContract().set(false);
        task.getSystemUnderTestVersion().set("1.0.0");

        task.generate();

        String content = Files.readString(new File(reportDir, "doppelganger-apis.adoc").toPath());
        assertThat(content)
                .contains("[WARNING]")
                .contains("does not exist yet: `" + missing + "`")
                .contains("None found.");
    }

    @Test
    @DisplayName("does not throw when none of the configured controllerDirs exist")
    void doesNotThrowWhenNoControllerDirsExist() throws Exception {
        File missing = new File(tempDir.toFile(), "src/main/java/does-not-exist");
        DetectDoppelgangerApisTask task = newTask();
        task.getControllerDirs().from(missing);
        task.getTestDirs().from(testDir);
        task.getRootDocument().set(openApiFixture("both-endpoints.yaml"));
        task.getReportDir().set(reportDir);
        task.getReportFileName().set("doppelganger-apis.adoc");
        task.getFailOnDoppelganger().set(true);
        task.getUseRestDocs().set(true);
        task.getUseOpenApiRequestValidator().set(false);
        task.getUseSpringCloudContract().set(false);
        task.getSystemUnderTestVersion().set("1.0.0");

        task.generate();

        String content = Files.readString(new File(reportDir, "doppelganger-apis.adoc").toPath());
        assertThat(content)
                .contains("[WARNING]")
                .contains("None of the configured `controllerDirs` exist yet")
                .contains("None found.");
    }

    @Test
    @DisplayName("does not treat an empty controllerDirs as a missing source")
    void emptyControllerDirsIsNotTreatedAsAMissingSource() throws Exception {
        File historyFile = new File(tempDir.toFile(), "contract-history.ndjson");
        DetectDoppelgangerApisTask task = newTask();
        task.getTestDirs().from(testDir);
        task.getRootDocument().set(openApiFixture("both-endpoints.yaml"));
        task.getReportDir().set(reportDir);
        task.getReportFileName().set("doppelganger-apis.adoc");
        task.getFailOnDoppelganger().set(false);
        task.getUseRestDocs().set(true);
        task.getUseOpenApiRequestValidator().set(false);
        task.getUseSpringCloudContract().set(false);
        task.getSystemUnderTestVersion().set("1.0.0");
        task.getTrackContractHistory().set(true);
        task.getContractHistoryFile().set(historyFile);
        task.getUpdateContractHistory().set(true);

        task.generate();

        String content = Files.readString(new File(reportDir, "doppelganger-apis.adoc").toPath());
        assertThat(content).doesNotContain("[WARNING]").contains("None found.");
        assertThat(historyFile).exists();
    }

    @Test
    @DisplayName("warns about one missing testDirs entry but still detects when another exists")
    void warnsAboutOneMissingTestDirButStillDetects() throws Exception {
        File missing = new File(tempDir.toFile(), "src/test/java/does-not-exist");
        DetectDoppelgangerApisTask task = configuredTask(openApiFixture("both-endpoints.yaml"), false);
        task.getTestDirs().from(missing);

        task.generate();

        String content = Files.readString(new File(reportDir, "doppelganger-apis.adoc").toPath());
        assertThat(content)
                .contains("[WARNING]")
                .contains("Configured `testDirs` entry does not exist yet: `" + missing + "`")
                .doesNotContain("None of the configured `testDirs` exist yet")
                .contains("listOrders()")
                .doesNotContain("getOrder()");
    }

    @Test
    @DisplayName("suppresses detection when the only enabled verification source has no usable directory")
    void suppressesDetectionWhenTheOnlyEnabledVerificationSourceHasNoUsableDirectory() throws Exception {
        File missing = new File(tempDir.toFile(), "src/test/java/does-not-exist");
        DetectDoppelgangerApisTask task = newTask();
        task.getControllerDirs().from(controllerDir);
        task.getTestDirs().from(missing);
        task.getRootDocument().set(openApiFixture("both-endpoints.yaml"));
        task.getReportDir().set(reportDir);
        task.getReportFileName().set("doppelganger-apis.adoc");
        task.getFailOnDoppelganger().set(true);
        task.getUseRestDocs().set(true);
        task.getUseOpenApiRequestValidator().set(false);
        task.getUseSpringCloudContract().set(false);
        task.getSystemUnderTestVersion().set("1.0.0");

        task.generate();

        String content = Files.readString(new File(reportDir, "doppelganger-apis.adoc").toPath());
        assertThat(content)
                .contains("[WARNING]")
                .contains("None of the configured `testDirs` exist yet")
                .contains("None found.");
    }

    @Test
    @DisplayName("does not suppress detection when the missing testDirs entry is only the plugin's own default")
    void doesNotSuppressWhenMissingTestDirIsOnlyThePluginsDefault() throws Exception {
        File missing = new File(tempDir.toFile(), "src/testContract/java");
        DetectDoppelgangerApisTask task = newTask();
        task.getControllerDirs().from(controllerDir);
        task.getTestDirs().from(missing);
        task.getTestDirsUserConfigured().set(false);
        task.getRootDocument().set(openApiFixture("both-endpoints.yaml"));
        task.getReportDir().set(reportDir);
        task.getReportFileName().set("doppelganger-apis.adoc");
        task.getFailOnDoppelganger().set(true);
        task.getUseRestDocs().set(true);
        task.getUseOpenApiRequestValidator().set(false);
        task.getUseSpringCloudContract().set(false);
        task.getSystemUnderTestVersion().set("1.0.0");

        assertThatThrownBy(task::generate)
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("found 2 doppelganger API(s)");

        String content = Files.readString(new File(reportDir, "doppelganger-apis.adoc").toPath());
        assertThat(content)
                .doesNotContain("[WARNING]")
                .doesNotContain("None of the configured `testDirs` exist yet")
                .contains("listOrders()")
                .contains("getOrder()");
    }

    @Test
    @DisplayName("does not suppress detection when at least one enabled verification source is usable")
    void doesNotSuppressWhenAtLeastOneEnabledSourceIsUsable() throws Exception {
        File missing = new File(tempDir.toFile(), "src/test/java/does-not-exist");
        File contractsDir = new File(tempDir.toFile(), "src/contractTest/resources/contracts");
        Files.createDirectories(contractsDir.toPath());
        Files.writeString(contractsDir.toPath().resolve("shouldListOrders.yml"), """
                request:
                  method: GET
                  urlPath: /orders
                response:
                  status: 200
                """);

        DetectDoppelgangerApisTask task = newTask();
        task.getControllerDirs().from(controllerDir);
        task.getTestDirs().from(missing);
        task.getContractsDir().set(contractsDir);
        task.getRootDocument().set(openApiFixture("both-endpoints.yaml"));
        task.getReportDir().set(reportDir);
        task.getReportFileName().set("doppelganger-apis.adoc");
        task.getFailOnDoppelganger().set(false);
        task.getUseRestDocs().set(true);
        task.getUseOpenApiRequestValidator().set(false);
        task.getUseSpringCloudContract().set(true);
        task.getSystemUnderTestVersion().set("1.0.0");

        task.generate();

        String content = Files.readString(new File(reportDir, "doppelganger-apis.adoc").toPath());
        assertThat(content)
                .contains("[WARNING]")
                .contains("None of the configured `testDirs` exist yet")
                .contains("getOrder()")
                .doesNotContain("listOrders()");
    }

    @Test
    @DisplayName("warns when a configured contractsDir does not exist")
    void warnsWhenConfiguredContractsDirDoesNotExist() throws Exception {
        File missingContractsDir = new File(tempDir.toFile(), "src/contractTest/resources/does-not-exist");
        DetectDoppelgangerApisTask task = newTask();
        task.getControllerDirs().from(controllerDir);
        task.getTestDirs().from(testDir);
        task.getContractsDir().set(missingContractsDir);
        task.getRootDocument().set(openApiFixture("both-endpoints.yaml"));
        task.getReportDir().set(reportDir);
        task.getReportFileName().set("doppelganger-apis.adoc");
        task.getFailOnDoppelganger().set(false);
        task.getUseRestDocs().set(true);
        task.getUseOpenApiRequestValidator().set(false);
        task.getUseSpringCloudContract().set(true);
        task.getSystemUnderTestVersion().set("1.0.0");

        task.generate();

        String content = Files.readString(new File(reportDir, "doppelganger-apis.adoc").toPath());
        assertThat(content)
                .contains("[WARNING]")
                .contains("Configured `contractsDir` does not exist yet: `" + missingContractsDir + "`");
    }

    @Test
    @DisplayName("throws when useSpringCloudContract is enabled but contractsDir is not configured")
    void throwsWhenSpringCloudContractEnabledButContractsDirNotConfigured() throws Exception {
        DetectDoppelgangerApisTask task = configuredTask(openApiFixture("both-endpoints.yaml"), false);
        task.getUseSpringCloudContract().set(true);

        assertThatThrownBy(task::generate)
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("contractsDir must be configured");

        assertThat(new File(reportDir, "doppelganger-apis.adoc")).doesNotExist();
    }

    @Test
    @DisplayName("throws when every verification source is disabled")
    void throwsWhenEveryVerificationSourceIsDisabled() throws Exception {
        DetectDoppelgangerApisTask task = configuredTask(openApiFixture("both-endpoints.yaml"), false);
        task.getUseRestDocs().set(false);
        task.getUseOpenApiRequestValidator().set(false);
        task.getUseSpringCloudContract().set(false);

        assertThatThrownBy(task::generate)
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("at least one of useRestDocs, useOpenApiRequestValidator, "
                        + "or useSpringCloudContract must be enabled");

        assertThat(new File(reportDir, "doppelganger-apis.adoc")).doesNotExist();
    }

    @Test
    @DisplayName("leaves contract history untouched when this run's input is incomplete")
    void leavesContractHistoryUntouchedWhenInputIsIncomplete() throws Exception {
        File historyFile = new File(tempDir.toFile(), "contract-history.ndjson");
        DetectDoppelgangerApisTask seedTask = configuredTask(openApiFixture("both-endpoints.yaml"), false);
        seedTask.getTrackContractHistory().set(true);
        seedTask.getContractHistoryFile().set(historyFile);
        seedTask.getUpdateContractHistory().set(true);
        seedTask.generate();
        String seededContent = Files.readString(historyFile.toPath());

        DetectDoppelgangerApisTask incompleteTask = project.getTasks()
                .create("detectDoppelgangerApisWithIncompleteInput", DetectDoppelgangerApisTask.class);
        incompleteTask.getControllerDirs().from(new File(tempDir.toFile(), "src/main/java/does-not-exist"));
        incompleteTask.getTestDirs().from(testDir);
        incompleteTask.getRootDocument().set(openApiFixture("both-endpoints.yaml"));
        incompleteTask.getReportDir().set(reportDir);
        incompleteTask.getReportFileName().set("doppelganger-apis.adoc");
        incompleteTask.getFailOnDoppelganger().set(false);
        incompleteTask.getUseRestDocs().set(true);
        incompleteTask.getUseOpenApiRequestValidator().set(false);
        incompleteTask.getUseSpringCloudContract().set(false);
        incompleteTask.getSystemUnderTestVersion().set("1.0.0");
        incompleteTask.getTrackContractHistory().set(true);
        incompleteTask.getContractHistoryFile().set(historyFile);
        incompleteTask.getUpdateContractHistory().set(true);

        incompleteTask.generate();

        assertThat(Files.readString(historyFile.toPath())).isEqualTo(seededContent);
        String reportContent = Files.readString(new File(reportDir, "doppelganger-apis.adoc").toPath());
        assertThat(reportContent).contains("== Progress Over Time").contains("Tracked since");
    }

    @Test
    @DisplayName("writes a report listing the unverified endpoint, but not the RestDocs-verified one")
    void writesReportListingUnverifiedEndpoint() throws Exception {
        DetectDoppelgangerApisTask task = configuredTask(openApiFixture("both-endpoints.yaml"), false);

        task.generate();

        File report = new File(reportDir, "doppelganger-apis.adoc");
        assertThat(report).exists();
        String content = Files.readString(report.toPath());
        assertThat(content)
                .contains("Scanned 2 endpoint(s)")
                .contains("1 of them is not verified")
                .contains("listOrders()")
                .doesNotContain("getOrder()");
    }

    @Test
    @DisplayName("writes the configured system under test version into the report")
    void writesSystemUnderTestVersionIntoReport() throws Exception {
        DetectDoppelgangerApisTask task = configuredTask(openApiFixture("both-endpoints.yaml"), false);
        task.getSystemUnderTestVersion().set("v2.3.1");

        task.generate();

        String content = Files.readString(new File(reportDir, "doppelganger-apis.adoc").toPath());
        assertThat(content).contains("System Under Test version: v2.3.1");
    }

    @Test
    @DisplayName("does not throw when failOnDoppelganger is false, even though doppelgangers were found")
    void doesNotThrowWhenFailOnDoppelgangerIsFalse() throws Exception {
        DetectDoppelgangerApisTask task = configuredTask(openApiFixture("both-endpoints.yaml"), false);

        task.generate();
    }

    @Test
    @DisplayName("throws when failOnDoppelganger is true and doppelgangers were found, but still writes the report")
    void throwsWhenFailOnDoppelgangerIsTrueAndDoppelgangersFound() throws Exception {
        DetectDoppelgangerApisTask task = configuredTask(openApiFixture("both-endpoints.yaml"), true);

        assertThatThrownBy(task::generate)
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("1 doppelganger API(s)");

        assertThat(new File(reportDir, "doppelganger-apis.adoc")).exists();
    }

    @Test
    @DisplayName("does not throw when failOnDoppelganger is true but every declared-and-implemented endpoint is verified")
    void doesNotThrowWhenEveryEndpointVerified() throws Exception {
        DetectDoppelgangerApisTask task = configuredTask(openApiFixture("single-verified-endpoint.yaml"), true);

        task.generate();

        String content = Files.readString(new File(reportDir, "doppelganger-apis.adoc").toPath());
        assertThat(content).contains("None found.");
    }

    @Test
    @DisplayName("persists contract history to contractHistoryFile and reports it when trackContractHistory is true")
    void persistsAndReportsContractHistoryWhenTrackContractHistoryIsTrue() throws Exception {
        File historyFile = new File(tempDir.toFile(), "contract-history.ndjson");
        DetectDoppelgangerApisTask task = configuredTask(openApiFixture("both-endpoints.yaml"), false);
        task.getTrackContractHistory().set(true);
        task.getContractHistoryFile().set(historyFile);
        task.getUpdateContractHistory().set(true);

        task.generate();

        assertThat(historyFile).exists();
        String historyContent = Files.readString(historyFile.toPath());
        assertThat(historyContent)
                .contains("\"declaredAt\"").contains("\"implementedAt\"").contains("\"verifiedAt\"");

        String reportContent = Files.readString(new File(reportDir, "doppelganger-apis.adoc").toPath());
        assertThat(reportContent).contains("== Progress Over Time").contains("Tracked since");
    }

    @Test
    @DisplayName("reads but does not write contractHistoryFile when updateContractHistory is false")
    void doesNotWriteContractHistoryFileWhenUpdateContractHistoryIsFalse() throws Exception {
        File historyFile = new File(tempDir.toFile(), "contract-history.ndjson");
        DetectDoppelgangerApisTask task = configuredTask(openApiFixture("both-endpoints.yaml"), false);
        task.getTrackContractHistory().set(true);
        task.getContractHistoryFile().set(historyFile);
        task.getUpdateContractHistory().set(false);

        task.generate();

        assertThat(historyFile).doesNotExist();
        String reportContent = Files.readString(new File(reportDir, "doppelganger-apis.adoc").toPath());
        assertThat(reportContent).contains("== Progress Over Time");
    }

    @Test
    @DisplayName("contractHistoryFilePath reflects the configured contractHistoryFile's absolute path")
    void contractHistoryFilePathReflectsConfiguredFile() {
        File historyFile = new File(tempDir.toFile(), "contract-history.ndjson");
        DetectDoppelgangerApisTask task = newTask();
        task.getContractHistoryFile().set(historyFile);

        assertThat(task.getContractHistoryFilePath()).isEqualTo(historyFile.getAbsolutePath());
    }

    @Test
    @DisplayName("contractHistoryFilePath is null when contractHistoryFile is unset")
    void contractHistoryFilePathIsNullWhenUnset() {
        DetectDoppelgangerApisTask task = newTask();

        assertThat(task.getContractHistoryFilePath()).isNull();
    }

    @Test
    @DisplayName("getContractHistoryFilePath is annotated with @Input so a changed path invalidates up-to-date state")
    void contractHistoryFilePathIsAnnotatedAsInput() throws NoSuchMethodException {
        var method = DetectDoppelgangerApisTask.class.getMethod("getContractHistoryFilePath");

        assertThat(method.isAnnotationPresent(org.gradle.api.tasks.Input.class)).isTrue();
    }

    @Test
    @DisplayName("runs correctly when a controller source file does not compile")
    void runsCorrectlyAgainstNonCompilingControllerSource() throws Exception {
        Files.writeString(controllerDir.toPath().resolve("BrokenController.java"), "this is not java { {{ }}}}}");

        DetectDoppelgangerApisTask task = configuredTask(openApiFixture("both-endpoints.yaml"), false);

        task.generate();

        String content = Files.readString(new File(reportDir, "doppelganger-apis.adoc").toPath());
        assertThat(content).contains("listOrders()");
    }

    @Test
    @DisplayName("excludePaths moves a matching doppelganger into Excluded Doppelganger APIs and it no longer fails the build")
    void excludePathsMovesMatchingDoppelgangerToExcludedSection() throws Exception {
        DetectDoppelgangerApisTask task = configuredTask(openApiFixture("both-endpoints.yaml"), true);
        task.getExcludePaths().set(List.of("/orders"));

        task.generate();

        String content = Files.readString(new File(reportDir, "doppelganger-apis.adoc").toPath());
        assertThat(content)
                .contains("None found.")
                .contains("== Excluded Doppelganger APIs")
                .contains("listOrders()");
    }

    @Test
    @DisplayName("excludeFiles rules combine across more than one file")
    void excludeFilesCombineAcrossMultipleFiles() throws Exception {
        File exclusionsA = new File(tempDir.toFile(), "exclusions-a.yaml");
        Files.writeString(exclusionsA.toPath(), "exclusions:\n  - \"/nowhere\"\n");
        File exclusionsB = new File(tempDir.toFile(), "exclusions-b.yaml");
        Files.writeString(exclusionsB.toPath(), "exclusions:\n  - \"/orders\"\n");

        DetectDoppelgangerApisTask task = configuredTask(openApiFixture("both-endpoints.yaml"), true);
        task.getExcludeFiles().from(exclusionsA, exclusionsB);

        task.generate();

        String content = Files.readString(new File(reportDir, "doppelganger-apis.adoc").toPath());
        assertThat(content).contains("None found.").contains("== Excluded Doppelganger APIs");
    }

    @Test
    @DisplayName("a missing excludeFiles entry warns but does not fail the build")
    void missingExcludeFilesEntryWarns() throws Exception {
        DetectDoppelgangerApisTask task = configuredTask(openApiFixture("both-endpoints.yaml"), false);
        task.getExcludeFiles().from(new File(tempDir.toFile(), "does-not-exist.yaml"));

        task.generate();

        String content = Files.readString(new File(reportDir, "doppelganger-apis.adoc").toPath());
        assertThat(content).contains("[WARNING]").contains("`excludeFiles` entry does not exist yet");
    }

    @Test
    @DisplayName("excludeWellKnown resolves the bundled spring-boot-actuator set")
    void excludeWellKnownResolvesSpringBootActuator() throws Exception {
        Files.writeString(controllerDir.toPath().resolve("HealthController.java"), """
                package com.example;

                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.RequestMapping;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                @RequestMapping("/actuator/health")
                public class HealthController {

                    @GetMapping
                    public String health() { return ""; }
                }
                """);
        File dir = new File(tempDir.toFile(), "openapi");
        Files.createDirectories(dir.toPath());
        File rootDocument = new File(dir, "actuator.yaml");
        Files.writeString(rootDocument.toPath(), """
                openapi: 3.0.3
                info:
                  title: Test API
                  version: "1.0"
                paths:
                  /actuator/health:
                    get:
                      operationId: health
                      responses:
                        '200':
                          description: OK
                """);

        DetectDoppelgangerApisTask task = configuredTask(rootDocument, true);
        task.getExcludeWellKnown().set(List.of("spring-boot-actuator"));

        task.generate();

        String content = Files.readString(new File(reportDir, "doppelganger-apis.adoc").toPath());
        assertThat(content)
                .contains("None found.")
                .contains("== Excluded Doppelganger APIs")
                .contains("/actuator/health");
    }

    @Test
    @DisplayName("an unrecognised excludeWellKnown name fails the build")
    void unrecognisedExcludeWellKnownNameFailsBuild() throws Exception {
        DetectDoppelgangerApisTask task = configuredTask(openApiFixture("both-endpoints.yaml"), false);
        task.getExcludeWellKnown().set(List.of("not-a-real-set"));

        assertThatThrownBy(task::generate)
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("not-a-real-set");
    }

    @Test
    @DisplayName("an excluded endpoint never reaches the persisted contract history file")
    void excludedEndpointNeverReachesContractHistory() throws Exception {
        File historyFile = new File(tempDir.toFile(), "contract-history.ndjson");
        DetectDoppelgangerApisTask task = configuredTask(openApiFixture("both-endpoints.yaml"), false);
        task.getExcludePaths().set(List.of("/orders"));
        task.getTrackContractHistory().set(true);
        task.getContractHistoryFile().set(historyFile);
        task.getUpdateContractHistory().set(true);

        task.generate();

        String historyContent = Files.readString(historyFile.toPath());
        assertThat(historyContent).doesNotContain("\"path\":\"/orders\"");
    }

    private DetectDoppelgangerApisTask configuredTask(File rootDocument, boolean failOnDoppelganger) {
        DetectDoppelgangerApisTask task = newTask();
        task.getControllerDirs().from(controllerDir);
        task.getTestDirs().from(testDir);
        task.getRootDocument().set(rootDocument);
        task.getReportDir().set(reportDir);
        task.getReportFileName().set("doppelganger-apis.adoc");
        task.getFailOnDoppelganger().set(failOnDoppelganger);
        task.getUseRestDocs().set(true);
        task.getUseOpenApiRequestValidator().set(false);
        task.getUseSpringCloudContract().set(false);
        task.getSystemUnderTestVersion().set("1.0.0");
        return task;
    }

    @Test
    @DisplayName("emits phase banners and at least one progress line while scanning more than one controller file")
    void emitsPhaseBannersAndProgressLineWhileScanningMultipleControllerFiles() throws Exception {
        Files.writeString(controllerDir.toPath().resolve("PaymentController.java"), """
                package com.example;

                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.RequestMapping;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                @RequestMapping("/payments")
                public class PaymentController {

                    @GetMapping
                    public String listPayments() { return "[]"; }
                }
                """);

        RecordingLogger recordingLogger = new RecordingLogger();
        LoggerCapturingDetectDoppelgangerApisTask task = project.getTasks()
                .create("detectDoppelgangerApisWithRecordingLogger", LoggerCapturingDetectDoppelgangerApisTask.class);
        task.recordingLogger = recordingLogger;
        task.getControllerDirs().from(controllerDir);
        task.getTestDirs().from(testDir);
        task.getRootDocument().set(openApiFixture("both-endpoints.yaml"));
        task.getReportDir().set(reportDir);
        task.getReportFileName().set("doppelganger-apis.adoc");
        task.getFailOnDoppelganger().set(false);
        task.getUseRestDocs().set(true);
        task.getUseOpenApiRequestValidator().set(false);
        task.getUseSpringCloudContract().set(false);
        task.getSystemUnderTestVersion().set("1.0.0");
        task.getTrackContractHistory().set(false);

        task.generate();

        assertThat(recordingLogger.lifecycleMessages())
                .anyMatch(message -> message.contains("Scanning @RestController classes"));
        assertThat(recordingLogger.lifecycleMessages())
                .anyMatch(message -> message.contains("Doppelganger API Detector: [1/3]"));
    }

    /**
     * Test-only subclass that substitutes a {@link RecordingLogger} for the framework-provided
     * task logger, since {@link DetectDoppelgangerApisTask#getLogger()} cannot otherwise be
     * observed from a {@link ProjectBuilder}-based test.
     */
    abstract static class LoggerCapturingDetectDoppelgangerApisTask extends DetectDoppelgangerApisTask {

        RecordingLogger recordingLogger;

        @Inject
        public LoggerCapturingDetectDoppelgangerApisTask() {}

        @Override
        public Logger getLogger() {
            return recordingLogger;
        }
    }

    private DetectDoppelgangerApisTask newTask() {
        DetectDoppelgangerApisTask task =
                project.getTasks().create("detectDoppelgangerApisUnderTest", DetectDoppelgangerApisTask.class);
        task.getTrackContractHistory().set(false);
        return task;
    }

    private File openApiFixture(String name) throws Exception {
        File dir = new File(tempDir.toFile(), "openapi");
        Files.createDirectories(dir.toPath());
        File file = new File(dir, name);
        String content = switch (name) {
            case "single-verified-endpoint.yaml" -> """
                    openapi: 3.0.3
                    info:
                      title: Test API
                      version: "1.0"
                    paths:
                      /orders/{id}:
                        get:
                          operationId: getOrder
                          parameters:
                            - name: id
                              in: path
                              required: true
                              schema:
                                type: integer
                          responses:
                            '200':
                              description: OK
                    """;
            case "both-endpoints.yaml" -> """
                    openapi: 3.0.3
                    info:
                      title: Test API
                      version: "1.0"
                    paths:
                      /orders:
                        get:
                          operationId: listOrders
                          responses:
                            '200':
                              description: OK
                      /orders/{id}:
                        get:
                          operationId: getOrder
                          parameters:
                            - name: id
                              in: path
                              required: true
                              schema:
                                type: integer
                          responses:
                            '200':
                              description: OK
                    """;
            default -> throw new IllegalArgumentException("Unknown fixture: " + name);
        };
        Files.writeString(file.toPath(), content);
        return file;
    }
}

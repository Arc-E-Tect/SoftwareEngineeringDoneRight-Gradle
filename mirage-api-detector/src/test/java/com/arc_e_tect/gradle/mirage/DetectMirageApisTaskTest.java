package com.arc_e_tect.gradle.mirage;

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

@DisplayName("DetectMirageApisTask")
class DetectMirageApisTaskTest {

    @TempDir
    Path tempDir;

    private Project project;
    private File controllerDir;
    private File reportDir;

    @BeforeEach
    void setUp() throws Exception {
        project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build();

        controllerDir = new File(tempDir.toFile(), "src/main/java/com/example");
        Files.createDirectories(controllerDir.toPath());
        Files.writeString(controllerDir.toPath().resolve("UserController.java"), """
                package com.example;

                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.RequestMapping;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                @RequestMapping("/users")
                public class UserController {

                    @GetMapping
                    public String listUsers() { return ""; }
                }
                """);

        reportDir = new File(tempDir.toFile(), "build/reports/mirage-api-detector");
    }

    @Test
    @DisplayName("does not throw when rootDocument is not configured, and writes a WARNING instead")
    void doesNotThrowWhenRootDocumentNotConfigured() throws Exception {
        DetectMirageApisTask task = newTask();
        task.getControllerDirs().from(controllerDir);
        task.getReportDir().set(reportDir);
        task.getReportFileName().set("mirage-apis.adoc");
        task.getFailOnMirage().set(true);
        task.getSystemUnderTestVersion().set("1.0.0");

        task.generate();

        String content = Files.readString(new File(reportDir, "mirage-apis.adoc").toPath());
        assertThat(content)
                .contains("[WARNING]")
                .contains("`rootDocument` is not configured yet");
    }

    @Test
    @DisplayName("does not throw when the configured rootDocument file does not exist, and writes a WARNING instead")
    void doesNotThrowWhenRootDocumentFileDoesNotExist() throws Exception {
        DetectMirageApisTask task = newTask();
        task.getControllerDirs().from(controllerDir);
        task.getRootDocument().set(new File(tempDir.toFile(), "openapi/missing.yaml"));
        task.getReportDir().set(reportDir);
        task.getReportFileName().set("mirage-apis.adoc");
        task.getFailOnMirage().set(true);
        task.getSystemUnderTestVersion().set("1.0.0");

        task.generate();

        String content = Files.readString(new File(reportDir, "mirage-apis.adoc").toPath());
        assertThat(content)
                .contains("[WARNING]")
                .contains("does not exist yet")
                .contains("missing.yaml");
    }

    @Test
    @DisplayName("does not throw when a configured controllerDirs entry does not exist, and writes a WARNING instead")
    void doesNotThrowWhenNoControllerDirsExist() throws Exception {
        DetectMirageApisTask task = newTask();
        task.getControllerDirs().from(new File(tempDir.toFile(), "src/main/java/does-not-exist"));
        task.getRootDocument().set(openApiFixture("single-endpoint.yaml"));
        task.getReportDir().set(reportDir);
        task.getReportFileName().set("mirage-apis.adoc");
        task.getFailOnMirage().set(true);
        task.getSystemUnderTestVersion().set("1.0.0");

        task.generate();

        String content = Files.readString(new File(reportDir, "mirage-apis.adoc").toPath());
        assertThat(content)
                .contains("[WARNING]")
                .contains("None of the configured `controllerDirs` exist yet");
    }

    @Test
    @DisplayName("a controllerDirs left entirely unconfigured is a valid stub-only setup, not a warning-worthy gap")
    void emptyControllerDirsIsNotTreatedAsAMissingSource() throws Exception {
        // Deliberately empty controllerDirs (no @RestController scanned at all) plus a stub that
        // matches the declared endpoint. Since stub evidence never counts as real implementation
        // evidence, this endpoint is still a genuine mirage API - the point of this test is only
        // that the empty controllerDirs collection itself produces no [WARNING], unlike a
        // controllerDirs entry that was configured but doesn't exist yet.
        File stubDir = new File(tempDir.toFile(), "src/test/resources/mappings");
        Files.createDirectories(stubDir.toPath());
        Files.writeString(stubDir.toPath().resolve("listUsers.json"), """
                { "request": { "method": "GET", "urlPath": "/users" }, "response": { "status": 200 } }
                """);

        DetectMirageApisTask task = newTask();
        task.getScanMocks().set(true);
        task.getStubDirs().from(stubDir);
        task.getRootDocument().set(openApiFixture("single-endpoint.yaml"));
        task.getReportDir().set(reportDir);
        task.getReportFileName().set("mirage-apis.adoc");
        task.getFailOnMirage().set(false);
        task.getSystemUnderTestVersion().set("1.0.0");

        task.generate();

        String content = Files.readString(new File(reportDir, "mirage-apis.adoc").toPath());
        assertThat(content).doesNotContain("[WARNING]").contains("1 of them is not implemented");
    }

    @Test
    @DisplayName("writes a report listing the described endpoint not implemented by any controller")
    void writesReportListingUnimplementedEndpoint() throws Exception {
        DetectMirageApisTask task = configuredTask(openApiFixture("both-endpoints.yaml"), false);

        task.generate();

        File report = new File(reportDir, "mirage-apis.adoc");
        assertThat(report).exists();
        String content = Files.readString(report.toPath());
        assertThat(content)
                .contains("Scanned 2 endpoint(s)")
                .contains("1 of them is not implemented")
                .contains("/users/{id}")
                .contains("deleteUser");
    }

    @Test
    @DisplayName("writes the configured system under test version into the report")
    void writesSystemUnderTestVersionIntoReport() throws Exception {
        DetectMirageApisTask task = configuredTask(openApiFixture("both-endpoints.yaml"), false);
        task.getSystemUnderTestVersion().set("v2.3.1");

        task.generate();

        String content = Files.readString(new File(reportDir, "mirage-apis.adoc").toPath());
        assertThat(content).contains("System Under Test version: v2.3.1");
    }

    @Test
    @DisplayName("does not throw when failOnMirage is false, even though mirages were found")
    void doesNotThrowWhenFailOnMirageIsFalse() throws Exception {
        DetectMirageApisTask task = configuredTask(openApiFixture("both-endpoints.yaml"), false);

        task.generate();
    }

    @Test
    @DisplayName("throws when failOnMirage is true and mirages were found, but still writes the report")
    void throwsWhenFailOnMirageIsTrueAndMiragesFound() throws Exception {
        DetectMirageApisTask task = configuredTask(openApiFixture("both-endpoints.yaml"), true);

        assertThatThrownBy(task::generate)
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("1 mirage API(s)");

        assertThat(new File(reportDir, "mirage-apis.adoc")).exists();
    }

    @Test
    @DisplayName("does not throw when failOnMirage is true but every described endpoint is implemented")
    void doesNotThrowWhenEveryEndpointImplemented() throws Exception {
        DetectMirageApisTask task = configuredTask(openApiFixture("single-endpoint.yaml"), true);

        task.generate();

        String content = Files.readString(new File(reportDir, "mirage-apis.adoc").toPath());
        assertThat(content).contains("None found.");
    }

    @Test
    @DisplayName("persists contract history to contractHistoryFile and reports it when trackContractHistory is true")
    void persistsAndReportsContractHistoryWhenTrackContractHistoryIsTrue() throws Exception {
        File historyFile = new File(tempDir.toFile(), "contract-history.ndjson");
        DetectMirageApisTask task = configuredTask(openApiFixture("both-endpoints.yaml"), false);
        task.getTrackContractHistory().set(true);
        task.getContractHistoryFile().set(historyFile);
        task.getUpdateContractHistory().set(true);

        task.generate();

        assertThat(historyFile).exists();
        String historyContent = Files.readString(historyFile.toPath());
        assertThat(historyContent).contains("\"declaredAt\"").contains("\"implementedAt\"");

        String reportContent = Files.readString(new File(reportDir, "mirage-apis.adoc").toPath());
        assertThat(reportContent).contains("== Progress Over Time").contains("Tracked since");
    }

    @Test
    @DisplayName("leaves a pre-existing contractHistoryFile untouched but still displays it when input is incomplete")
    void leavesContractHistoryUntouchedWhenInputIsIncomplete() throws Exception {
        File historyFile = new File(tempDir.toFile(), "contract-history.ndjson");
        DetectMirageApisTask seedTask = configuredTask(openApiFixture("both-endpoints.yaml"), false);
        seedTask.getTrackContractHistory().set(true);
        seedTask.getContractHistoryFile().set(historyFile);
        seedTask.getUpdateContractHistory().set(true);
        seedTask.generate();
        String seededContent = Files.readString(historyFile.toPath());

        DetectMirageApisTask task = project.getTasks()
                .create("detectMirageApisWithIncompleteInput", DetectMirageApisTask.class);
        task.getTrackContractHistory().set(true);
        task.getScanMocks().set(false);
        task.getControllerDirs().from(new File(tempDir.toFile(), "src/main/java/does-not-exist"));
        task.getRootDocument().set(openApiFixture("both-endpoints.yaml"));
        task.getReportDir().set(reportDir);
        task.getReportFileName().set("mirage-apis.adoc");
        task.getFailOnMirage().set(false);
        task.getSystemUnderTestVersion().set("1.0.0");
        task.getContractHistoryFile().set(historyFile);
        task.getUpdateContractHistory().set(true);

        task.generate();

        assertThat(Files.readString(historyFile.toPath())).isEqualTo(seededContent);
        String reportContent = Files.readString(new File(reportDir, "mirage-apis.adoc").toPath());
        assertThat(reportContent).contains("== Progress Over Time");
    }

    @Test
    @DisplayName("reads but does not write contractHistoryFile when updateContractHistory is false")
    void doesNotWriteContractHistoryFileWhenUpdateContractHistoryIsFalse() throws Exception {
        File historyFile = new File(tempDir.toFile(), "contract-history.ndjson");
        DetectMirageApisTask task = configuredTask(openApiFixture("both-endpoints.yaml"), false);
        task.getTrackContractHistory().set(true);
        task.getContractHistoryFile().set(historyFile);
        task.getUpdateContractHistory().set(false);

        task.generate();

        assertThat(historyFile).doesNotExist();
        String reportContent = Files.readString(new File(reportDir, "mirage-apis.adoc").toPath());
        assertThat(reportContent).contains("== Progress Over Time");
    }

    @Test
    @DisplayName("contractHistoryFilePath reflects the configured contractHistoryFile's absolute path")
    void contractHistoryFilePathReflectsConfiguredFile() {
        File historyFile = new File(tempDir.toFile(), "contract-history.ndjson");
        DetectMirageApisTask task = newTask();
        task.getContractHistoryFile().set(historyFile);

        assertThat(task.getContractHistoryFilePath()).isEqualTo(historyFile.getAbsolutePath());
    }

    @Test
    @DisplayName("contractHistoryFilePath is null when contractHistoryFile is unset")
    void contractHistoryFilePathIsNullWhenUnset() {
        DetectMirageApisTask task = newTask();

        assertThat(task.getContractHistoryFilePath()).isNull();
    }

    @Test
    @DisplayName("getContractHistoryFilePath is annotated with @Input so a changed path invalidates up-to-date state")
    void contractHistoryFilePathIsAnnotatedAsInput() throws NoSuchMethodException {
        var method = DetectMirageApisTask.class.getMethod("getContractHistoryFilePath");

        assertThat(method.isAnnotationPresent(org.gradle.api.tasks.Input.class)).isTrue();
    }

    @Test
    @DisplayName("still bases mirages on controller implementation, not stub coverage, when scanMocks is true")
    void stillBasesMiragesOnControllerImplementationWhenScanMocksTrue() throws Exception {
        // controllerDir (see setUp) only implements listUsers - deleteUser has no controller match,
        // so it must still be reported as a mirage even though scanMocks is true and a stub exists
        // for listUsers: stub evidence never changes which endpoints are mirage APIs.
        File stubDir = new File(tempDir.toFile(), "src/test/resources/mappings");
        Files.createDirectories(stubDir.toPath());
        Files.writeString(stubDir.toPath().resolve("listUsers.json"), """
                {
                  "request": { "method": "GET", "urlPath": "/users" },
                  "response": { "status": 200 }
                }
                """);

        DetectMirageApisTask task = newTask();
        task.getScanMocks().set(true);
        task.getStubDirs().from(stubDir);
        task.getControllerDirs().from(controllerDir);
        task.getRootDocument().set(openApiFixture("both-endpoints.yaml"));
        task.getReportDir().set(reportDir);
        task.getReportFileName().set("mirage-apis.adoc");
        task.getFailOnMirage().set(false);
        task.getSystemUnderTestVersion().set("1.0.0");

        task.generate();

        String content = Files.readString(new File(reportDir, "mirage-apis.adoc").toPath());
        assertThat(content)
                .contains("1 of them is not implemented by any `@RestController` class")
                .contains("/users/{id}")
                .contains("deleteUser");
    }

    @Test
    @DisplayName("persists both implementedAt and stubbedAt in one run when scanMocks is true and a controller also implements it")
    void persistsBothImplementedAtAndStubbedAtWhenScanMocksIsTrue() throws Exception {
        // controllerDir (see setUp) implements listUsers, and the stub below also covers it - the
        // whole point of scanMocks no longer being exclusive: one run can gather both kinds of
        // evidence for the same endpoint.
        File stubDir = new File(tempDir.toFile(), "src/test/resources/mappings");
        Files.createDirectories(stubDir.toPath());
        Files.writeString(stubDir.toPath().resolve("listUsers.json"), """
                {
                  "request": { "method": "GET", "urlPath": "/users" },
                  "response": { "status": 200 }
                }
                """);
        File historyFile = new File(tempDir.toFile(), "contract-history.ndjson");

        DetectMirageApisTask task = newTask();
        task.getScanMocks().set(true);
        task.getStubDirs().from(stubDir);
        task.getControllerDirs().from(controllerDir);
        task.getRootDocument().set(openApiFixture("single-endpoint.yaml"));
        task.getReportDir().set(reportDir);
        task.getReportFileName().set("mirage-apis.adoc");
        task.getFailOnMirage().set(false);
        task.getSystemUnderTestVersion().set("1.0.0");
        task.getTrackContractHistory().set(true);
        task.getContractHistoryFile().set(historyFile);
        task.getUpdateContractHistory().set(true);

        task.generate();

        String historyContent = Files.readString(historyFile.toPath());
        assertThat(historyContent)
                .doesNotContain("\"implementedAt\":null")
                .doesNotContain("\"stubbedAt\":null");
    }

    @Test
    @DisplayName("persists only stubbedAt when scanMocks is true but no controller implements the endpoint")
    void persistsOnlyStubbedAtWhenNoControllerImplementsIt() throws Exception {
        // controllerDir (see setUp) implements listUsers only - listOrders has no controller
        // match, so its history record must gain stubbedAt but never implementedAt. Both
        // endpoints here are path-parameter-free deliberately: matching a stub's literal path
        // (e.g. "/users/1") against a declared path-variable template (e.g. "/users/{id}") is a
        // separate, unrelated limitation this test isn't meant to exercise.
        File stubDir = new File(tempDir.toFile(), "src/test/resources/mappings");
        Files.createDirectories(stubDir.toPath());
        Files.writeString(stubDir.toPath().resolve("listOrders.json"), """
                {
                  "request": { "method": "GET", "urlPath": "/orders" },
                  "response": { "status": 200 }
                }
                """);
        File historyFile = new File(tempDir.toFile(), "contract-history.ndjson");

        DetectMirageApisTask task = newTask();
        task.getScanMocks().set(true);
        task.getStubDirs().from(stubDir);
        task.getControllerDirs().from(controllerDir);
        task.getRootDocument().set(openApiFixture("users-and-orders.yaml"));
        task.getReportDir().set(reportDir);
        task.getReportFileName().set("mirage-apis.adoc");
        task.getFailOnMirage().set(false);
        task.getSystemUnderTestVersion().set("1.0.0");
        task.getTrackContractHistory().set(true);
        task.getContractHistoryFile().set(historyFile);
        task.getUpdateContractHistory().set(true);

        task.generate();

        List<String> lines = Files.readAllLines(historyFile.toPath()).stream()
                .filter(line -> line.contains("\"path\":\"/orders\""))
                .toList();
        assertThat(lines).hasSize(1);
        assertThat(lines.get(0)).contains("\"implementedAt\":null").doesNotContain("\"stubbedAt\":null");
    }

    @Test
    @DisplayName("throws with migration guidance when contractHistoryFile is in the legacy 9-field format")
    void throwsWithMigrationGuidanceOnLegacyContractHistoryFormat() throws Exception {
        File historyFile = new File(tempDir.toFile(), "contract-history.ndjson");
        String legacyLine = "{\"fingerprint\":\"aaaa000000000000\",\"verb\":\"GET\",\"path\":\"/users\","
                + "\"declaringClass\":null,\"declaredAt\":\"2026-01-01T00:00:00Z\","
                + "\"implementedAt\":null,\"verifiedAt\":null,"
                + "\"lastSeenAt\":\"2026-01-01T00:00:00Z\",\"removedAt\":null}";
        Files.writeString(historyFile.toPath(), legacyLine + "\n");

        DetectMirageApisTask task = configuredTask(openApiFixture("both-endpoints.yaml"), false);
        task.getTrackContractHistory().set(true);
        task.getContractHistoryFile().set(historyFile);

        assertThatThrownBy(task::generate)
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("migrateContractHistory");
    }

    @Test
    @DisplayName("still considers an endpoint implemented by a matching controller when scanMocks is true, even without a matching stub")
    void stillConsidersControllerImplementationWhenScanningMocks() throws Exception {
        File stubDir = new File(tempDir.toFile(), "src/test/resources/mappings");
        Files.createDirectories(stubDir.toPath());

        DetectMirageApisTask task = newTask();
        task.getScanMocks().set(true);
        task.getStubDirs().from(stubDir);
        task.getControllerDirs().from(controllerDir);
        task.getRootDocument().set(openApiFixture("single-endpoint.yaml"));
        task.getReportDir().set(reportDir);
        task.getReportFileName().set("mirage-apis.adoc");
        task.getFailOnMirage().set(false);
        task.getSystemUnderTestVersion().set("1.0.0");

        task.generate();

        String content = Files.readString(new File(reportDir, "mirage-apis.adoc").toPath());
        assertThat(content).contains("None found.");
    }

    @Test
    @DisplayName("strips an explicitly configured basePath from scanned stub paths before matching")
    void stripsExplicitlyConfiguredBasePathFromScannedStubPaths() throws Exception {
        File stubDir = new File(tempDir.toFile(), "src/test/resources/mappings");
        Files.createDirectories(stubDir.toPath());
        Files.writeString(stubDir.toPath().resolve("listUsers.json"), """
                {
                  "request": { "method": "GET", "urlPath": "/crm-service/users" },
                  "response": { "status": 200 }
                }
                """);
        File historyFile = new File(tempDir.toFile(), "contract-history.ndjson");

        DetectMirageApisTask task = newTask();
        task.getScanMocks().set(true);
        task.getStubDirs().from(stubDir);
        task.getBasePath().set("/crm-service");
        task.getRootDocument().set(openApiFixture("single-endpoint.yaml"));
        task.getReportDir().set(reportDir);
        task.getReportFileName().set("mirage-apis.adoc");
        task.getFailOnMirage().set(false);
        task.getSystemUnderTestVersion().set("1.0.0");
        task.getTrackContractHistory().set(true);
        task.getContractHistoryFile().set(historyFile);
        task.getUpdateContractHistory().set(true);

        task.generate();

        // Proof the stub actually matched the declared endpoint after basePath was stripped: mirage
        // status itself no longer depends on stub matching (controllerDirs is unset here, so the
        // report unconditionally lists the endpoint as a mirage), so stubbedAt in history is the
        // signal to check instead.
        String historyContent = Files.readString(historyFile.toPath());
        assertThat(historyContent).doesNotContain("\"stubbedAt\":null");
    }

    @Test
    @DisplayName("falls back to rootDocument's own first servers entry when basePath is not configured")
    void fallsBackToRootDocumentServersUrlWhenBasePathNotConfigured() throws Exception {
        File stubDir = new File(tempDir.toFile(), "src/test/resources/mappings");
        Files.createDirectories(stubDir.toPath());
        Files.writeString(stubDir.toPath().resolve("listUsers.json"), """
                {
                  "request": { "method": "GET", "urlPath": "/crm-service/users" },
                  "response": { "status": 200 }
                }
                """);
        File historyFile = new File(tempDir.toFile(), "contract-history.ndjson");

        DetectMirageApisTask task = newTask();
        task.getScanMocks().set(true);
        task.getStubDirs().from(stubDir);
        task.getRootDocument().set(openApiFixture("single-endpoint-with-servers.yaml"));
        task.getReportDir().set(reportDir);
        task.getReportFileName().set("mirage-apis.adoc");
        task.getFailOnMirage().set(false);
        task.getSystemUnderTestVersion().set("1.0.0");
        task.getTrackContractHistory().set(true);
        task.getContractHistoryFile().set(historyFile);
        task.getUpdateContractHistory().set(true);

        task.generate();

        String historyContent = Files.readString(historyFile.toPath());
        assertThat(historyContent).doesNotContain("\"stubbedAt\":null");
    }

    @Test
    @DisplayName("an explicitly configured basePath takes precedence over rootDocument's servers entry")
    void explicitBasePathTakesPrecedenceOverRootDocumentServersUrl() throws Exception {
        File stubDir = new File(tempDir.toFile(), "src/test/resources/mappings");
        Files.createDirectories(stubDir.toPath());
        // The stub's own request path matches the explicit basePath below, not the (deliberately
        // different) base path rootDocument's own servers entry declares.
        Files.writeString(stubDir.toPath().resolve("listUsers.json"), """
                {
                  "request": { "method": "GET", "urlPath": "/explicit-base/users" },
                  "response": { "status": 200 }
                }
                """);
        File historyFile = new File(tempDir.toFile(), "contract-history.ndjson");

        DetectMirageApisTask task = newTask();
        task.getScanMocks().set(true);
        task.getStubDirs().from(stubDir);
        task.getBasePath().set("/explicit-base");
        task.getRootDocument().set(openApiFixture("single-endpoint-with-servers.yaml"));
        task.getReportDir().set(reportDir);
        task.getReportFileName().set("mirage-apis.adoc");
        task.getFailOnMirage().set(false);
        task.getSystemUnderTestVersion().set("1.0.0");
        task.getTrackContractHistory().set(true);
        task.getContractHistoryFile().set(historyFile);
        task.getUpdateContractHistory().set(true);

        task.generate();

        String historyContent = Files.readString(historyFile.toPath());
        assertThat(historyContent).doesNotContain("\"stubbedAt\":null");
    }

    @Test
    @DisplayName("merges stub evidence into the same contract history record as the declared endpoint once basePath is stripped")
    void mergesStubEvidenceWithDeclaredEndpointAfterStrippingBasePath() throws Exception {
        File stubDir = new File(tempDir.toFile(), "src/test/resources/mappings");
        Files.createDirectories(stubDir.toPath());
        Files.writeString(stubDir.toPath().resolve("listUsers.json"), """
                {
                  "request": { "method": "GET", "urlPath": "/crm-service/users" },
                  "response": { "status": 200 }
                }
                """);
        File historyFile = new File(tempDir.toFile(), "contract-history.ndjson");

        DetectMirageApisTask task = newTask();
        task.getScanMocks().set(true);
        task.getStubDirs().from(stubDir);
        task.getBasePath().set("/crm-service");
        task.getRootDocument().set(openApiFixture("single-endpoint.yaml"));
        task.getReportDir().set(reportDir);
        task.getReportFileName().set("mirage-apis.adoc");
        task.getFailOnMirage().set(false);
        task.getSystemUnderTestVersion().set("1.0.0");
        task.getTrackContractHistory().set(true);
        task.getContractHistoryFile().set(historyFile);
        task.getUpdateContractHistory().set(true);

        task.generate();

        // A single, merged record - not one orphan row with only stubbedAt set alongside a
        // second, separate row with only declaredAt set.
        List<String> lines = Files.readAllLines(historyFile.toPath()).stream()
                .filter(line -> !line.isBlank() && !line.startsWith("{\"schemaVersion\""))
                .toList();
        assertThat(lines).hasSize(1);
        assertThat(lines.get(0)).contains("\"declaredAt\":\"").contains("\"stubbedAt\":\"");
    }

    private DetectMirageApisTask configuredTask(File rootDocument, boolean failOnMirage) {
        DetectMirageApisTask task = newTask();
        task.getControllerDirs().from(controllerDir);
        task.getScanMocks().set(false);
        task.getRootDocument().set(rootDocument);
        task.getReportDir().set(reportDir);
        task.getReportFileName().set("mirage-apis.adoc");
        task.getFailOnMirage().set(failOnMirage);
        task.getSystemUnderTestVersion().set("1.0.0");
        return task;
    }

    @Test
    @DisplayName("emits at least one progress line while scanning more than one controller file")
    void emitsProgressLineWhileScanningMultipleControllerFiles() throws Exception {
        Files.writeString(controllerDir.toPath().resolve("OrderController.java"), """
                package com.example;

                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.RequestMapping;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                @RequestMapping("/orders")
                public class OrderController {

                    @GetMapping
                    public String listOrders() { return ""; }
                }
                """);

        RecordingLogger recordingLogger = new RecordingLogger();
        LoggerCapturingDetectMirageApisTask task = project.getTasks()
                .create("detectMirageApisWithRecordingLogger", LoggerCapturingDetectMirageApisTask.class);
        task.recordingLogger = recordingLogger;
        task.getControllerDirs().from(controllerDir);
        task.getScanMocks().set(false);
        task.getRootDocument().set(openApiFixture("both-endpoints.yaml"));
        task.getReportDir().set(reportDir);
        task.getReportFileName().set("mirage-apis.adoc");
        task.getFailOnMirage().set(false);
        task.getSystemUnderTestVersion().set("1.0.0");
        task.getTrackContractHistory().set(false);

        task.generate();

        assertThat(recordingLogger.lifecycleMessages())
                .anyMatch(message -> message.contains("Scanning @RestController classes"));
    }

    /**
     * Test-only subclass that substitutes a {@link RecordingLogger} for the framework-provided
     * task logger, since {@link DetectMirageApisTask#getLogger()} cannot otherwise be observed
     * from a {@link ProjectBuilder}-based test.
     */
    abstract static class LoggerCapturingDetectMirageApisTask extends DetectMirageApisTask {

        RecordingLogger recordingLogger;

        @Inject
        public LoggerCapturingDetectMirageApisTask() {}

        @Override
        public Logger getLogger() {
            return recordingLogger;
        }
    }

    private DetectMirageApisTask newTask() {
        DetectMirageApisTask task = project.getTasks().create("detectMirageApisUnderTest", DetectMirageApisTask.class);
        task.getTrackContractHistory().set(false);
        task.getScanMocks().set(false);
        return task;
    }

    private File openApiFixture(String name) throws Exception {
        File dir = new File(tempDir.toFile(), "openapi");
        Files.createDirectories(dir.toPath());
        File file = new File(dir, name);
        String content = switch (name) {
            case "single-endpoint.yaml" -> """
                    openapi: 3.0.3
                    info:
                      title: Test API
                      version: "1.0"
                    paths:
                      /users:
                        get:
                          operationId: listUsers
                          responses:
                            '200':
                              description: OK
                    """;
            case "single-endpoint-with-servers.yaml" -> """
                    openapi: 3.0.3
                    info:
                      title: Test API
                      version: "1.0"
                    servers:
                      - url: http://localhost:9011/crm-service
                    paths:
                      /users:
                        get:
                          operationId: listUsers
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
                      /users:
                        get:
                          operationId: listUsers
                          responses:
                            '200':
                              description: OK
                      /users/{id}:
                        delete:
                          operationId: deleteUser
                          parameters:
                            - name: id
                              in: path
                              required: true
                              schema:
                                type: integer
                          responses:
                            '204':
                              description: No Content
                    """;
            case "users-and-orders.yaml" -> """
                    openapi: 3.0.3
                    info:
                      title: Test API
                      version: "1.0"
                    paths:
                      /users:
                        get:
                          operationId: listUsers
                          responses:
                            '200':
                              description: OK
                      /orders:
                        get:
                          operationId: listOrders
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

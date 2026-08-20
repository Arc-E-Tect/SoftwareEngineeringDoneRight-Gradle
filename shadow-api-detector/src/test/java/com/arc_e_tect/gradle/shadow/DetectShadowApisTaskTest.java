package com.arc_e_tect.gradle.shadow;

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
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DetectShadowApisTask")
class DetectShadowApisTaskTest {

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

                import org.springframework.web.bind.annotation.DeleteMapping;
                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.RequestMapping;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                @RequestMapping("/users")
                public class UserController {

                    @GetMapping
                    public String listUsers() { return ""; }

                    @DeleteMapping("/{id}")
                    public String deleteUser() { return ""; }
                }
                """);

        reportDir = new File(tempDir.toFile(), "build/reports/shadow-api-detector");
    }

    @Test
    @DisplayName("does not throw when rootDocument is not configured, and writes a WARNING instead")
    void doesNotThrowWhenRootDocumentNotConfigured() throws Exception {
        DetectShadowApisTask task = newTask();
        task.getControllerDirs().from(controllerDir);
        task.getReportDir().set(reportDir);
        task.getReportFileName().set("shadow-apis.adoc");
        task.getFailOnShadow().set(true);
        task.getSystemUnderTestVersion().set("1.0.0");

        task.generate();

        String content = Files.readString(new File(reportDir, "shadow-apis.adoc").toPath());
        assertThat(content)
                .contains("[WARNING]")
                .contains("`rootDocument` is not configured yet")
                .contains("0 of them are not described");
    }

    @Test
    @DisplayName("does not throw when the configured rootDocument file does not exist, and writes a WARNING instead")
    void doesNotThrowWhenRootDocumentFileDoesNotExist() throws Exception {
        DetectShadowApisTask task = newTask();
        task.getControllerDirs().from(controllerDir);
        task.getRootDocument().set(new File(tempDir.toFile(), "openapi/missing.yaml"));
        task.getReportDir().set(reportDir);
        task.getReportFileName().set("shadow-apis.adoc");
        task.getFailOnShadow().set(true);
        task.getSystemUnderTestVersion().set("1.0.0");

        task.generate();

        String content = Files.readString(new File(reportDir, "shadow-apis.adoc").toPath());
        assertThat(content)
                .contains("[WARNING]")
                .contains("does not exist yet")
                .contains("missing.yaml");
    }

    @Test
    @DisplayName("does not throw when no configured controllerDirs exist, and writes a WARNING instead")
    void doesNotThrowWhenNoControllerDirsExist() throws Exception {
        DetectShadowApisTask task = newTask();
        task.getControllerDirs().from(new File(tempDir.toFile(), "src/main/java/does-not-exist"));
        task.getRootDocument().set(openApiFixture("single-endpoint.yaml"));
        task.getReportDir().set(reportDir);
        task.getReportFileName().set("shadow-apis.adoc");
        task.getFailOnShadow().set(true);
        task.getSystemUnderTestVersion().set("1.0.0");

        task.generate();

        String content = Files.readString(new File(reportDir, "shadow-apis.adoc").toPath());
        assertThat(content)
                .contains("[WARNING]")
                .contains("None of the configured `controllerDirs` exist yet");
    }

    @Test
    @DisplayName("warns about one missing controllerDirs entry but still detects shadows from the entries that do exist")
    void warnsAboutOneMissingControllerDirButStillDetects() throws Exception {
        DetectShadowApisTask task = newTask();
        task.getControllerDirs().from(controllerDir, new File(tempDir.toFile(), "src/main/java/also-missing"));
        task.getRootDocument().set(openApiFixture("single-endpoint.yaml"));
        task.getReportDir().set(reportDir);
        task.getReportFileName().set("shadow-apis.adoc");
        task.getFailOnShadow().set(false);
        task.getSystemUnderTestVersion().set("1.0.0");

        task.generate();

        String content = Files.readString(new File(reportDir, "shadow-apis.adoc").toPath());
        assertThat(content)
                .contains("[WARNING]")
                .contains("Configured `controllerDirs` entry does not exist yet")
                .contains("also-missing")
                .contains("1 of them is not described");
    }

    @Test
    @DisplayName("does not fail even when failOnShadow is true if rootDocument is missing, since nothing was genuinely checked")
    void doesNotFailOnMissingRootDocumentEvenWhenFailOnShadowIsTrue() throws Exception {
        DetectShadowApisTask task = newTask();
        task.getControllerDirs().from(controllerDir);
        task.getReportDir().set(reportDir);
        task.getReportFileName().set("shadow-apis.adoc");
        task.getFailOnShadow().set(true);
        task.getSystemUnderTestVersion().set("1.0.0");

        task.generate();
    }

    @Test
    @DisplayName("leaves a pre-existing contractHistoryFile untouched but still displays it when input is incomplete")
    void leavesContractHistoryUntouchedWhenInputIsIncomplete() throws Exception {
        File historyFile = new File(tempDir.toFile(), "contract-history.ndjson");
        DetectShadowApisTask seedTask = configuredTask(openApiFixture("both-endpoints.yaml"), false);
        seedTask.getTrackContractHistory().set(true);
        seedTask.getContractHistoryFile().set(historyFile);
        seedTask.getUpdateContractHistory().set(true);
        seedTask.generate();
        String seededContent = Files.readString(historyFile.toPath());

        DetectShadowApisTask task = project.getTasks()
                .create("detectShadowApisWithIncompleteInput", DetectShadowApisTask.class);
        task.getTrackContractHistory().set(false);
        task.getProjectDirectory().set(tempDir.toFile());
        task.getControllerDirs().from(new File(tempDir.toFile(), "src/main/java/does-not-exist"));
        task.getRootDocument().set(openApiFixture("both-endpoints.yaml"));
        task.getReportDir().set(reportDir);
        task.getReportFileName().set("shadow-apis.adoc");
        task.getFailOnShadow().set(false);
        task.getSystemUnderTestVersion().set("1.0.0");
        task.getTrackContractHistory().set(true);
        task.getContractHistoryFile().set(historyFile);
        task.getUpdateContractHistory().set(true);

        task.generate();

        assertThat(Files.readString(historyFile.toPath())).isEqualTo(seededContent);
        String reportContent = Files.readString(new File(reportDir, "shadow-apis.adoc").toPath());
        assertThat(reportContent).contains("== Progress Over Time");
    }

    @Test
    @DisplayName("scanForShadows still throws clearly when rootDocument is not configured, unlike the full-project scan")
    void scanForShadowsStillThrowsWhenRootDocumentNotConfigured() {
        DetectShadowApisTask task = newTask();
        task.getControllerDirs().from(controllerDir);
        task.getScanForShadows().set("UserController");

        assertThatThrownBy(task::generate)
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("rootDocument must be configured");
    }

    @Test
    @DisplayName("writes a report listing the endpoint not described in the OpenAPI document")
    void writesReportListingUndescribedEndpoint() throws Exception {
        DetectShadowApisTask task = configuredTask(openApiFixture("single-endpoint.yaml"), false);

        task.generate();

        File report = new File(reportDir, "shadow-apis.adoc");
        assertThat(report).exists();
        String content = Files.readString(report.toPath());
        assertThat(content)
                .contains("Scanned 2 endpoint(s)")
                .contains("1 of them is not described")
                .contains("/users/{id}")
                .contains("com.example.UserController")
                .doesNotContain("listUsers()");
    }

    @Test
    @DisplayName("writes the configured system under test version into the report")
    void writesSystemUnderTestVersionIntoReport() throws Exception {
        DetectShadowApisTask task = configuredTask(openApiFixture("single-endpoint.yaml"), false);
        task.getSystemUnderTestVersion().set("v2.3.1");

        task.generate();

        String content = Files.readString(new File(reportDir, "shadow-apis.adoc").toPath());
        assertThat(content).contains("System Under Test version: v2.3.1");
    }

    @Test
    @DisplayName("does not throw when failOnShadow is false, even though shadows were found")
    void doesNotThrowWhenFailOnShadowIsFalse() throws Exception {
        DetectShadowApisTask task = configuredTask(openApiFixture("single-endpoint.yaml"), false);

        task.generate();
    }

    @Test
    @DisplayName("throws when failOnShadow is true and shadows were found, but still writes the report")
    void throwsWhenFailOnShadowIsTrueAndShadowsFound() throws Exception {
        DetectShadowApisTask task = configuredTask(openApiFixture("single-endpoint.yaml"), true);

        assertThatThrownBy(task::generate)
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("1 shadow API(s)");

        assertThat(new File(reportDir, "shadow-apis.adoc")).exists();
    }

    @Test
    @DisplayName("does not throw when failOnShadow is true but every endpoint is described")
    void doesNotThrowWhenEveryEndpointDescribed() throws Exception {
        DetectShadowApisTask task = configuredTask(openApiFixture("both-endpoints.yaml"), true);

        task.generate();

        String content = Files.readString(new File(reportDir, "shadow-apis.adoc").toPath());
        assertThat(content).contains("None found.");
    }

    @Test
    @DisplayName("persists contract history to contractHistoryFile and reports it when trackContractHistory is true")
    void persistsAndReportsContractHistoryWhenTrackContractHistoryIsTrue() throws Exception {
        File historyFile = new File(tempDir.toFile(), "contract-history.ndjson");
        DetectShadowApisTask task = configuredTask(openApiFixture("both-endpoints.yaml"), false);
        task.getTrackContractHistory().set(true);
        task.getContractHistoryFile().set(historyFile);
        task.getUpdateContractHistory().set(true);

        task.generate();

        assertThat(historyFile).exists();
        String historyContent = Files.readString(historyFile.toPath());
        assertThat(historyContent).contains("\"declaredAt\"").contains("\"implementedAt\"");

        String reportContent = Files.readString(new File(reportDir, "shadow-apis.adoc").toPath());
        assertThat(reportContent).contains("== Progress Over Time").contains("Tracked since");
    }

    @Test
    @DisplayName("reads but does not write contractHistoryFile when updateContractHistory is false")
    void doesNotWriteContractHistoryFileWhenUpdateContractHistoryIsFalse() throws Exception {
        File historyFile = new File(tempDir.toFile(), "contract-history.ndjson");
        DetectShadowApisTask task = configuredTask(openApiFixture("both-endpoints.yaml"), false);
        task.getTrackContractHistory().set(true);
        task.getContractHistoryFile().set(historyFile);
        task.getUpdateContractHistory().set(false);

        task.generate();

        assertThat(historyFile).doesNotExist();
        String reportContent = Files.readString(new File(reportDir, "shadow-apis.adoc").toPath());
        assertThat(reportContent).contains("== Progress Over Time");
    }

    @Test
    @DisplayName("contractHistoryFilePath reflects the configured contractHistoryFile's absolute path")
    void contractHistoryFilePathReflectsConfiguredFile() {
        File historyFile = new File(tempDir.toFile(), "contract-history.ndjson");
        DetectShadowApisTask task = newTask();
        task.getContractHistoryFile().set(historyFile);

        assertThat(task.getContractHistoryFilePath()).isEqualTo(historyFile.getAbsolutePath());
    }

    @Test
    @DisplayName("contractHistoryFilePath is null when contractHistoryFile is unset")
    void contractHistoryFilePathIsNullWhenUnset() {
        DetectShadowApisTask task = newTask();

        assertThat(task.getContractHistoryFilePath()).isNull();
    }

    @Test
    @DisplayName("getContractHistoryFilePath is annotated with @Input so a changed path invalidates up-to-date state")
    void contractHistoryFilePathIsAnnotatedAsInput() throws NoSuchMethodException {
        var method = DetectShadowApisTask.class.getMethod("getContractHistoryFilePath");

        assertThat(method.isAnnotationPresent(org.gradle.api.tasks.Input.class)).isTrue();
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
        LoggerCapturingDetectShadowApisTask task = project.getTasks()
                .create("detectShadowApisWithRecordingLogger", LoggerCapturingDetectShadowApisTask.class);
        task.recordingLogger = recordingLogger;
        task.getControllerDirs().from(controllerDir);
        task.getRootDocument().set(openApiFixture("both-endpoints.yaml"));
        task.getReportDir().set(reportDir);
        task.getReportFileName().set("shadow-apis.adoc");
        task.getFailOnShadow().set(false);
        task.getSystemUnderTestVersion().set("1.0.0");
        task.getTrackContractHistory().set(false);

        task.generate();

        assertThat(recordingLogger.lifecycleMessages())
                .anyMatch(message -> message.contains("Scanning @RestController classes"));
    }

    @Test
    @DisplayName("scanForShadows by bare name finds the controller under controllerDirs and prints shadows to the console")
    void scanForShadowsByNameFindsControllerAndPrintsToConsole() throws Exception {
        RecordingLogger recordingLogger = new RecordingLogger();
        LoggerCapturingDetectShadowApisTask task = project.getTasks()
                .create("scanForShadowsByName", LoggerCapturingDetectShadowApisTask.class);
        task.recordingLogger = recordingLogger;
        task.getControllerDirs().from(controllerDir);
        task.getRootDocument().set(openApiFixture("single-endpoint.yaml"));
        task.getReportDir().set(reportDir);
        task.getReportFileName().set("shadow-apis.adoc");
        task.getFailOnShadow().set(false);
        task.getSystemUnderTestVersion().set("1.0.0");
        task.getScanForShadows().set("UserController");

        task.generate();

        assertThat(recordingLogger.lifecycleMessages())
                .anyMatch(message -> message.contains("/users/{id}") && message.contains("com.example.UserController"))
                .anyMatch(message -> message.contains("scanned 1 controller(s), found 2 endpoint(s), 1 of them shadow API(s)"));
        assertThat(new File(reportDir, "shadow-apis.adoc")).doesNotExist();
    }

    @Test
    @DisplayName("scanForShadows by name scans every same-named controller across different packages")
    void scanForShadowsByNameScansEveryMatchAcrossPackages() throws Exception {
        File otherPackageDir = new File(controllerDir.getParentFile(), "other");
        Files.createDirectories(otherPackageDir.toPath());
        Files.writeString(otherPackageDir.toPath().resolve("UserController.java"), """
                package com.example.other;

                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.RequestMapping;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                @RequestMapping("/other-users")
                public class UserController {

                    @GetMapping
                    public String listOtherUsers() { return ""; }
                }
                """);
        DetectShadowApisTask task = newTask();
        task.getControllerDirs().from(controllerDir.getParentFile());
        task.getRootDocument().set(openApiFixture("single-endpoint.yaml"));
        task.getReportDir().set(reportDir);
        task.getReportFileName().set("shadow-apis.adoc");
        task.getFailOnShadow().set(false);
        task.getSystemUnderTestVersion().set("1.0.0");
        task.getScanForShadows().set("UserController");

        task.generate();
    }

    @Test
    @DisplayName("scanForShadows by an absolute .java path scans exactly that file, regardless of controllerDirs")
    void scanForShadowsByAbsolutePathScansExactFile() throws Exception {
        DetectShadowApisTask task = newTask();
        task.getControllerDirs().from(tempDir.resolve("does-not-exist").toFile());
        task.getRootDocument().set(openApiFixture("single-endpoint.yaml"));
        task.getReportDir().set(reportDir);
        task.getReportFileName().set("shadow-apis.adoc");
        task.getFailOnShadow().set(true);
        task.getSystemUnderTestVersion().set("1.0.0");
        task.getScanForShadows().set(new File(controllerDir, "UserController.java").getAbsolutePath());

        assertThatThrownBy(task::generate)
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("1 shadow API(s)");
    }

    @Test
    @DisplayName("scanForShadows by a relative .java path resolves against the project directory")
    void scanForShadowsByRelativePathResolvesAgainstProjectDirectory() throws Exception {
        DetectShadowApisTask task = newTask();
        task.getControllerDirs().from(controllerDir);
        task.getRootDocument().set(openApiFixture("single-endpoint.yaml"));
        task.getReportDir().set(reportDir);
        task.getReportFileName().set("shadow-apis.adoc");
        task.getFailOnShadow().set(false);
        task.getSystemUnderTestVersion().set("1.0.0");
        Path relative = tempDir.relativize(new File(controllerDir, "UserController.java").toPath());
        task.getScanForShadows().set(relative.toString());

        task.generate();
    }

    @Test
    @DisplayName("scanForShadows fails clearly when the given .java path does not exist")
    void scanForShadowsFailsClearlyWhenPathDoesNotExist() throws Exception {
        DetectShadowApisTask task = newTask();
        task.getControllerDirs().from(controllerDir);
        task.getRootDocument().set(openApiFixture("single-endpoint.yaml"));
        task.getReportDir().set(reportDir);
        task.getReportFileName().set("shadow-apis.adoc");
        task.getFailOnShadow().set(false);
        task.getSystemUnderTestVersion().set("1.0.0");
        task.getScanForShadows().set("/no/such/Controller.java");

        assertThatThrownBy(task::generate)
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("scanForShadows path does not exist");
    }

    @Test
    @DisplayName("scanForShadows fails clearly when no controller matches the given name")
    void scanForShadowsFailsClearlyWhenNameNotFound() throws Exception {
        DetectShadowApisTask task = newTask();
        task.getControllerDirs().from(controllerDir);
        task.getRootDocument().set(openApiFixture("single-endpoint.yaml"));
        task.getReportDir().set(reportDir);
        task.getReportFileName().set("shadow-apis.adoc");
        task.getFailOnShadow().set(false);
        task.getSystemUnderTestVersion().set("1.0.0");
        task.getScanForShadows().set("NoSuchController");

        assertThatThrownBy(task::generate)
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("no controller named 'NoSuchController'");
    }

    @Test
    @DisplayName("scanForShadows does not fail the build when failOnShadow is true but no shadows are found")
    void scanForShadowsDoesNotFailWhenNoShadowsFound() throws Exception {
        DetectShadowApisTask task = newTask();
        task.getControllerDirs().from(controllerDir);
        task.getRootDocument().set(openApiFixture("both-endpoints.yaml"));
        task.getReportDir().set(reportDir);
        task.getReportFileName().set("shadow-apis.adoc");
        task.getFailOnShadow().set(true);
        task.getSystemUnderTestVersion().set("1.0.0");
        task.getScanForShadows().set("UserController");

        task.generate();
    }

    @Test
    @DisplayName("failOnShadowOverride true forces a failure even though failOnShadow is false")
    void failOnShadowOverrideTrueForcesFailure() throws Exception {
        DetectShadowApisTask task = configuredTask(openApiFixture("single-endpoint.yaml"), false);
        task.getFailOnShadowOverride().set(true);

        assertThatThrownBy(task::generate)
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("1 shadow API(s)");
    }

    @Test
    @DisplayName("failOnShadowOverride false prevents a failure even though failOnShadow is true")
    void failOnShadowOverrideFalsePreventsFailure() throws Exception {
        DetectShadowApisTask task = configuredTask(openApiFixture("single-endpoint.yaml"), true);
        task.getFailOnShadowOverride().set(false);

        task.generate();
    }

    @Test
    @DisplayName("updateContractHistoryOverride true writes the history file even though updateContractHistory is false")
    void updateContractHistoryOverrideTrueWritesFile() throws Exception {
        File historyFile = new File(tempDir.toFile(), "contract-history.ndjson");
        DetectShadowApisTask task = configuredTask(openApiFixture("both-endpoints.yaml"), false);
        task.getTrackContractHistory().set(true);
        task.getContractHistoryFile().set(historyFile);
        task.getUpdateContractHistory().set(false);
        task.getUpdateContractHistoryOverride().set(true);

        task.generate();

        assertThat(historyFile).exists();
    }

    @Test
    @DisplayName("updateContractHistoryOverride false skips writing the history file even though updateContractHistory is true")
    void updateContractHistoryOverrideFalseSkipsWrite() throws Exception {
        File historyFile = new File(tempDir.toFile(), "contract-history.ndjson");
        DetectShadowApisTask task = configuredTask(openApiFixture("both-endpoints.yaml"), false);
        task.getTrackContractHistory().set(true);
        task.getContractHistoryFile().set(historyFile);
        task.getUpdateContractHistory().set(true);
        task.getUpdateContractHistoryOverride().set(false);

        task.generate();

        assertThat(historyFile).doesNotExist();
    }

    @Test
    @DisplayName("getScanForShadows, getFailOnShadowOverride, and getUpdateContractHistoryOverride are annotated "
            + "with @Input so a changed CLI value invalidates up-to-date state")
    void newCliOverridePropertiesAreAnnotatedAsInput() throws NoSuchMethodException {
        assertThat(DetectShadowApisTask.class.getMethod("getScanForShadows")
                .isAnnotationPresent(org.gradle.api.tasks.Input.class)).isTrue();
        assertThat(DetectShadowApisTask.class.getMethod("getFailOnShadowOverride")
                .isAnnotationPresent(org.gradle.api.tasks.Input.class)).isTrue();
        assertThat(DetectShadowApisTask.class.getMethod("getUpdateContractHistoryOverride")
                .isAnnotationPresent(org.gradle.api.tasks.Input.class)).isTrue();
    }

    /**
     * Test-only subclass that substitutes a {@link RecordingLogger} for the framework-provided
     * task logger, since {@link DetectShadowApisTask#getLogger()} cannot otherwise be observed
     * from a {@link ProjectBuilder}-based test.
     */
    abstract static class LoggerCapturingDetectShadowApisTask extends DetectShadowApisTask {

        RecordingLogger recordingLogger;

        @Inject
        public LoggerCapturingDetectShadowApisTask() {}

        @Override
        public Logger getLogger() {
            return recordingLogger;
        }
    }

    private DetectShadowApisTask configuredTask(File rootDocument, boolean failOnShadow) {
        DetectShadowApisTask task = newTask();
        task.getControllerDirs().from(controllerDir);
        task.getRootDocument().set(rootDocument);
        task.getReportDir().set(reportDir);
        task.getReportFileName().set("shadow-apis.adoc");
        task.getFailOnShadow().set(failOnShadow);
        task.getSystemUnderTestVersion().set("1.0.0");
        return task;
    }

    private DetectShadowApisTask newTask() {
        DetectShadowApisTask task = project.getTasks().create("detectShadowApisUnderTest", DetectShadowApisTask.class);
        task.getTrackContractHistory().set(false);
        task.getProjectDirectory().set(tempDir.toFile());
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
            default -> throw new IllegalArgumentException("Unknown fixture: " + name);
        };
        Files.writeString(file.toPath(), content);
        return file;
    }
}

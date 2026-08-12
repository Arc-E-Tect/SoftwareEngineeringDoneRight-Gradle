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
    @DisplayName("throws when rootDocument is not configured")
    void throwsWhenRootDocumentNotConfigured() {
        DetectDoppelgangerApisTask task = newTask();
        task.getControllerDirs().from(controllerDir);
        task.getTestDirs().from(testDir);
        task.getReportDir().set(reportDir);
        task.getReportFileName().set("doppelganger-apis.adoc");
        task.getFailOnDoppelganger().set(false);
        task.getUseRestDocs().set(true);
        task.getUseOpenApiRequestValidator().set(false);
        task.getUseSpringCloudContract().set(false);
        task.getSystemUnderTestVersion().set("1.0.0");

        assertThatThrownBy(task::generate)
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("rootDocument must be configured");
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
    @DisplayName("runs correctly when a controller source file does not compile")
    void runsCorrectlyAgainstNonCompilingControllerSource() throws Exception {
        Files.writeString(controllerDir.toPath().resolve("BrokenController.java"), "this is not java { {{ }}}}}");

        DetectDoppelgangerApisTask task = configuredTask(openApiFixture("both-endpoints.yaml"), false);

        task.generate();

        String content = Files.readString(new File(reportDir, "doppelganger-apis.adoc").toPath());
        assertThat(content).contains("listOrders()");
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

        task.generate();

        assertThat(recordingLogger.lifecycleMessages())
                .anyMatch(message -> message.contains("Scanning @RestController classes"));
        assertThat(recordingLogger.lifecycleMessages())
                .anyMatch(message -> message.contains("Doppelganger API Detector: [{}/{}]"));
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
        return project.getTasks().create("detectDoppelgangerApisUnderTest", DetectDoppelgangerApisTask.class);
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

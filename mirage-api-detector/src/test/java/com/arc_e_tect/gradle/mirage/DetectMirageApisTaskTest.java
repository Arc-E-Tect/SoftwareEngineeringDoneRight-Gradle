package com.arc_e_tect.gradle.mirage;

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
    @DisplayName("throws when rootDocument is not configured")
    void throwsWhenRootDocumentNotConfigured() {
        DetectMirageApisTask task = newTask();
        task.getControllerDirs().from(controllerDir);
        task.getReportDir().set(reportDir);
        task.getReportFileName().set("mirage-apis.adoc");
        task.getFailOnMirage().set(false);
        task.getSystemUnderTestVersion().set("1.0.0");

        assertThatThrownBy(task::generate)
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("rootDocument must be configured");
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
    @DisplayName("scans WireMock stubs instead of controllers when scanMocks is true")
    void scansStubsInsteadOfControllersWhenScanMocksTrue() throws Exception {
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
                .contains("1 of them is not backed by any WireMock stub")
                .contains("/users/{id}")
                .contains("deleteUser");
    }

    @Test
    @DisplayName("does not consider an endpoint implemented by a matching controller when scanMocks is true")
    void ignoresControllerImplementationWhenScanningMocks() throws Exception {
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
        assertThat(content).contains("1 of them is not backed by any WireMock stub");
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

    private DetectMirageApisTask newTask() {
        return project.getTasks().create("detectMirageApisUnderTest", DetectMirageApisTask.class);
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

package com.arc_e_tect.gradle.shadow;

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
    @DisplayName("throws when rootDocument is not configured")
    void throwsWhenRootDocumentNotConfigured() {
        DetectShadowApisTask task = newTask();
        task.getControllerDirs().from(controllerDir);
        task.getReportDir().set(reportDir);
        task.getReportFileName().set("shadow-apis.adoc");
        task.getFailOnShadow().set(false);
        task.getSystemUnderTestVersion().set("1.0.0");

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
      @DisplayName("adds a compatibility disclaimer to the report when parsing an OpenAPI 3.2 root")
      void addsCompatibilityDisclaimerForOpenApi32Root() throws Exception {
        DetectShadowApisTask task = configuredTask(openApiFixture("single-endpoint-3-2.yaml"), false);

        task.generate();

        String content = Files.readString(new File(reportDir, "shadow-apis.adoc").toPath());
        assertThat(content).contains("OpenAPI 3.2 compatibility mode was used while parsing the root document.");
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
        return project.getTasks().create("detectShadowApisUnderTest", DetectShadowApisTask.class);
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
            case "single-endpoint-3-2.yaml" -> """
                    openapi: 3.2.0
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
            default -> throw new IllegalArgumentException("Unknown fixture: " + name);
        };
        Files.writeString(file.toPath(), content);
        return file;
    }
}

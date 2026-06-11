package com.arc_e_tect.gradle.architecture;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ArchitectureValidatorIntegration")
class ArchitectureValidatorIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("should fail build when architecture tests violate rules")
    void shouldFailBuildWhenArchitectureViolationsExist() throws IOException {
        Path projectDir = createProjectWithFailingArchitectureTest("fail-on-violation", false);

        BuildResult result = createRunner(projectDir)
                .withArguments("testArchitecture", "--stacktrace")
                .buildAndFail();

        assertThat(result.getOutput()).contains("Architecture validation failed with");
        assertThat(result.getOutput()).contains("failing rule");
    }

    @Test
    @DisplayName("should report violations and pass when ignoreFailures is true")
    void shouldReportViolationsAndPassWhenIgnoreFailuresIsTrue() throws IOException {
        Path projectDir = createProjectWithFailingArchitectureTest("ignore-failures", true);

        BuildResult result = createRunner(projectDir)
                .withArguments("testArchitecture", "--stacktrace")
                .build();

        assertThat(result.task(":testArchitecture").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
        assertThat(result.getOutput()).contains("FAILED");

        Path xmlReportDir = projectDir.resolve("build/reports/architecture-validator/xml");
        assertThat(xmlReportDir).isDirectory();
        String mergedXml = readAllXml(xmlReportDir);
        assertThat(mergedXml).contains("failures=");
        assertThat(mergedXml).doesNotContain("failures=\"0\"");
    }

    private Path createProjectWithFailingArchitectureTest(String projectName, boolean ignoreFailures) throws IOException {
        Path projectDir = tempDir.resolve(projectName);
        Files.createDirectories(projectDir);

        write(projectDir.resolve("settings.gradle"), """
                pluginManagement {
                    repositories {
                        gradlePluginPortal()
                    }
                }

                rootProject.name = '%s'
                """.formatted(projectName));

        write(projectDir.resolve("build.gradle"), """
                plugins {
                    id 'java'
                    id 'com.arc-e-tect.architecture-validator'
                }

                group = 'com.example.archtest'
                version = '0.0.1'

                repositories {
                    mavenCentral()
                }

                architectureValidator {
                    basePackage = 'com.example.archtest'
                    useBuiltInHexagonalRulePack = false
                    ignoreFailures = %s
                }
                """.formatted(ignoreFailures));

        write(projectDir.resolve("src/main/java/com/example/archtest/Dummy.java"), """
                package com.example.archtest;

                public class Dummy {
                }
                """);

        write(projectDir.resolve("src/testArchitecture/java/com/example/archtest/ManualArchitectureTest.java"), """
                package com.example.archtest;

                import org.junit.jupiter.api.Test;
                import static org.junit.jupiter.api.Assertions.fail;

                class ManualArchitectureTest {
                    @Test
                    void shouldFlagArchitectureViolation() {
                        fail("Intentional architecture rule violation for smoke test");
                    }
                }
                """);

        return projectDir;
    }

    private GradleRunner createRunner(Path projectDir) {
        return GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .forwardOutput()
                .withArguments("--no-daemon", "--max-workers=1");
    }

    private String readAllXml(Path xmlReportDir) throws IOException {
        try (Stream<Path> files = Files.walk(xmlReportDir)) {
            return files
                    .filter(path -> path.getFileName().toString().endsWith(".xml"))
                    .map(path -> {
                        try {
                            return Files.readString(path, StandardCharsets.UTF_8);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .reduce("", (left, right) -> left + "\n" + right);
        }
    }

    private void write(Path file, String contents) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, contents, StandardCharsets.UTF_8);
    }
}
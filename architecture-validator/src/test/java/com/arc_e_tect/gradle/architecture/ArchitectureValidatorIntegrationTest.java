package com.arc_e_tect.gradle.architecture;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
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
    @DisplayName("compositeBuildSpringRulePackResolutionShouldExecuteSpringRulePackTests")
    void compositeBuildSpringRulePackResolutionShouldExecuteSpringRulePackTests() throws IOException {
        Path projectDir = tempDir.resolve("spring-composite");
        Files.createDirectories(projectDir);

        String architectureValidatorRoot = new File(".").getCanonicalFile().getAbsolutePath();
        String normalizedRoot = architectureValidatorRoot.replace("\\", "\\\\");

        write(projectDir.resolve("settings.gradle"), """
                pluginManagement {
                    repositories {
                        gradlePluginPortal()
                    }
                }

                includeBuild('%s') {
                    dependencySubstitution {
                        substitute module('com.arc-e-tect:spring-rules') using project(':spring-rules')
                    }
                }

                rootProject.name = 'spring-composite-it'
                """.formatted(normalizedRoot));

        write(projectDir.resolve("build.gradle"), """
                plugins {
                    id 'java'
                    id 'com.arc-e-tect.architecture-validator'
                }

                group = 'com.example.springit'
                version = '0.0.1'

                repositories {
                    mavenCentral()
                }

                dependencies {
                    implementation 'org.springframework:spring-context:6.2.8'
                }

                architectureValidator {
                    basePackage = 'com.example.springit'
                    ignoreFailures = true
                    useSpringRulePack = true
                    springRulePackCoordinate = 'com.arc-e-tect:spring-rules'
                }
                """);

        write(projectDir.resolve("src/main/java/com/example/springit/application/port/in/OrderUseCase.java"), """
                package com.example.springit.application.port.in;

                public interface OrderUseCase {
                    void createOrder(String id);
                }
                """);

        write(projectDir.resolve("src/main/java/com/example/springit/adapter/persistence/OrderRepository.java"), """
                package com.example.springit.adapter.persistence;

                import org.springframework.stereotype.Repository;

                @Repository
                public class OrderRepository {
                    public void save(String id) {
                    }
                }
                """);

        write(projectDir.resolve("src/main/java/com/example/springit/application/service/OrderService.java"), """
                package com.example.springit.application.service;

                import com.example.springit.adapter.persistence.OrderRepository;
                import org.springframework.stereotype.Service;

                @Service
                public class OrderService {
                    private final OrderRepository repository;

                    public OrderService(OrderRepository repository) {
                        this.repository = repository;
                    }

                    public void createOrder(String id) {
                        repository.save(id);
                    }
                }
                """);

        BuildResult result = GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .withArguments("testArchitecture")
                .build();

        assertThat(result.task(":testArchitecture").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
        Path xmlReportDir = projectDir.resolve("build/reports/architecture-validator/xml");
        assertThat(xmlReportDir).isDirectory();
        try (Stream<Path> files = Files.walk(xmlReportDir)) {
            String mergedXml = files
                    .filter(path -> path.getFileName().toString().endsWith(".xml"))
                    .map(path -> {
                        try {
                            return Files.readString(path, StandardCharsets.UTF_8);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .reduce("", (left, right) -> left + "\n" + right);
            assertThat(mergedXml).contains("SpringHexagonalArchitectureTest");
        }
    }

    @Test
    @DisplayName("duplicateRuleHandlingShouldFailGenerationWhenFailOnDuplicateRulesEnabled")
    void duplicateRuleHandlingShouldFailGenerationWhenFailOnDuplicateRulesEnabled() throws IOException {
        Path projectDir = tempDir.resolve("duplicate-rule-it");
        Files.createDirectories(projectDir);

        Path externalRulePack = projectDir.resolve("duplicate-rules-pack");
        Files.createDirectories(externalRulePack.resolve("src/main/java/com/org/ea/duplicate"));

        write(projectDir.resolve("settings.gradle"), """
                pluginManagement {
                    repositories {
                        gradlePluginPortal()
                    }
                }

                includeBuild('duplicate-rules-pack') {
                    dependencySubstitution {
                        substitute module('com.org.ea:duplicate-rules') using project(':')
                    }
                }

                rootProject.name = 'duplicate-rule-it'
                """);

        write(projectDir.resolve("build.gradle"), """
                plugins {
                    id 'java'
                    id 'com.arc-e-tect.architecture-validator'
                }

                group = 'com.example.duplicate'
                version = '0.0.1'

                repositories {
                    mavenCentral()
                }

                dependencies {
                    testArchitectureImplementation 'com.org.ea:duplicate-rules:1.0.0'
                }

                architectureValidator {
                    basePackage = 'com.example.duplicate'
                    ignoreFailures = true
                    failOnDuplicateRules = true
                }
                """);

        write(projectDir.resolve("src/main/java/com/example/duplicate/Dummy.java"), """
                package com.example.duplicate;

                public class Dummy {
                }
                """);

        write(externalRulePack.resolve("settings.gradle"), "rootProject.name = 'duplicate-rules-pack'\n");
        write(externalRulePack.resolve("build.gradle"), """
                plugins {
                    id 'java-library'
                }

                group = 'com.org.ea'
                version = '1.0.0'

                repositories {
                    mavenCentral()
                }

                dependencies {
                    compileOnly 'org.junit.jupiter:junit-jupiter:5.13.4'
                }
                """);

        write(externalRulePack.resolve("src/main/java/com/org/ea/duplicate/HexagonalArchitectureTest.java"), """
                package com.org.ea.duplicate;

                import org.junit.jupiter.api.Test;

                public class HexagonalArchitectureTest {
                    @Test
                    void placeholderShouldPass() {
                    }
                }
                """);

        BuildResult result = GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .withArguments("generateArchitectureTests")
                .buildAndFail();

        assertThat(result.getOutput()).contains("Duplicate architecture rules discovered");
    }

    @Test
    @DisplayName("disablingBuiltInHexagonalRulePackShouldSkipGeneratedHexagonalTest")
    void disablingBuiltInHexagonalRulePackShouldSkipGeneratedHexagonalTest() throws IOException {
        Path projectDir = tempDir.resolve("disable-built-in-it");
        Files.createDirectories(projectDir);

        String architectureValidatorRoot = new File(".").getCanonicalFile().getAbsolutePath();
        String normalizedRoot = architectureValidatorRoot.replace("\\", "\\\\");

        write(projectDir.resolve("settings.gradle"), """
                pluginManagement {
                    repositories {
                        gradlePluginPortal()
                    }
                }

                includeBuild('%s')

                rootProject.name = 'disable-built-in-it'
                """.formatted(normalizedRoot));

        write(projectDir.resolve("build.gradle"), """
                plugins {
                    id 'java'
                    id 'com.arc-e-tect.architecture-validator'
                }

                group = 'com.example.disablebuiltin'
                version = '0.0.1'

                repositories {
                    mavenCentral()
                }

                architectureValidator {
                    basePackage = 'com.example.disablebuiltin'
                    useBuiltInHexagonalRulePack = false
                    ignoreFailures = true
                }
                """);

        write(projectDir.resolve("src/main/java/com/example/disablebuiltin/Dummy.java"), """
                package com.example.disablebuiltin;

                public class Dummy {
                }
                """);

        BuildResult result = GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .withArguments("generateArchitectureTests")
                .build();

        assertThat(result.task(":generateArchitectureTests").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);

        Path generatedHexagonal = projectDir.resolve("build/generated/testArchitecture/java/com/arc_e_tect/gradle/architecture/generated/HexagonalArchitectureTest.java");
        assertThat(generatedHexagonal).doesNotExist();
    }

    private void write(Path file, String contents) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, contents, StandardCharsets.UTF_8);
    }
}
package com.arc_e_tect.gradle.architecture;

import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GenerateArchitectureTestsTask")
class GenerateArchitectureTestsTaskTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("generateShouldWriteHexagonalArchitectureTestFromTemplate")
    void generateShouldWriteHexagonalArchitectureTestFromTemplate() throws Exception {
        GenerateArchitectureTestsTask task = ProjectBuilder.builder()
                .withProjectDir(tempDir.toFile())
                .build()
                .getTasks()
                .create("generateArchitectureTests", GenerateArchitectureTestsTask.class);

        task.getBasePackage().set("com.example.architecture");
        task.getInPorts().set(List.of("..application.port.in.."));
        task.getOutPorts().set(List.of("..application.port.out.."));
        task.getDomainModel().set(List.of("..application.domain.."));
        task.getAdapters().set(List.of("..adapter..", "..adapters.."));
        task.getApplicationServices().set(List.of("..application.service.."));
        task.getCommonPackages().set(List.of("..application.common.."));
        task.getFailOnDuplicateRules().set(false);
        task.getUserTestsDirectory().set(tempDir.resolve("user-tests").toFile());
        task.getOutputDirectory().set(tempDir.resolve("generated").toFile());

        task.generate();

        Path generatedFile = tempDir.resolve("generated/com/arc_e_tect/gradle/architecture/generated/HexagonalArchitectureTest.java");
        assertThat(generatedFile).exists();
        String contents = Files.readString(generatedFile);
        assertThat(contents)
                .contains("com.example.architecture")
                .contains("Ports are contracts, not implementations")
                .contains("Adapters are outer-layer details and must point inward");
    }

    @Test
    @DisplayName("generateShouldCreateExternalRulePackSuiteWhenRulePackTestsExist")
    void generateShouldCreateExternalRulePackSuiteWhenRulePackTestsExist() throws Exception {
        GenerateArchitectureTestsTask task = ProjectBuilder.builder()
                .withProjectDir(tempDir.toFile())
                .build()
                .getTasks()
                .create("generateArchitectureTestsWithExternalRules", GenerateArchitectureTestsTask.class);

        Path fakeRulePackRoot = tempDir.resolve("rule-pack");
        Files.createDirectories(fakeRulePackRoot.resolve("com/example/rules"));
        Files.write(fakeRulePackRoot.resolve("com/example/rules/LayeredRulesTest.class"), new byte[] {0});

        task.getBasePackage().set("com.example.architecture");
        task.getInPorts().set(List.of("..application.port.in.."));
        task.getOutPorts().set(List.of("..application.port.out.."));
        task.getDomainModel().set(List.of("..application.domain.."));
        task.getAdapters().set(List.of("..adapter.."));
        task.getApplicationServices().set(List.of("..application.service.."));
        task.getCommonPackages().set(List.of("..application.common.."));
        task.getFailOnDuplicateRules().set(false);
        task.getUserTestsDirectory().set(tempDir.resolve("user-tests").toFile());
        task.getRulePackClasspath().from(fakeRulePackRoot.toFile());
        task.getOutputDirectory().set(tempDir.resolve("generated").toFile());

        task.generate();

        Path generatedSuite = tempDir.resolve("generated/com/arc_e_tect/gradle/architecture/generated/ExternalRulePackSuite.java");
        assertThat(generatedSuite).exists();
        assertThat(Files.readString(generatedSuite))
                .contains("@SelectPackages")
                .contains("com.example.rules")
                .contains("@IncludeClassNamePatterns({\".*Test\"})");
    }
}
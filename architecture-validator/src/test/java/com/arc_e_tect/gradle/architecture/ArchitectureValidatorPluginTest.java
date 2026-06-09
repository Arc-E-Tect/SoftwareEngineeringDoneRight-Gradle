package com.arc_e_tect.gradle.architecture;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ArchitectureValidatorPlugin")
class ArchitectureValidatorPluginTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("pluginShouldRegisterGenerateArchitectureTestsTask")
    void pluginShouldRegisterGenerateArchitectureTestsTask() {
        Project project = projectWithPlugin();

        assertThat(project.getTasks().findByName(ArchitectureValidatorPlugin.GENERATE_ARCHITECTURE_TESTS_TASK_NAME))
                .isNotNull();
    }

    @Test
    @DisplayName("pluginShouldRegisterArchitectureValidatorExtensionDefaults")
    void pluginShouldRegisterArchitectureValidatorExtensionDefaults() {
        Project project = projectWithPlugin();

        ArchitectureValidatorExtension extension = project.getExtensions().getByType(ArchitectureValidatorExtension.class);

        assertThat(extension.getFailOnViolation().get()).isTrue();
        assertThat(extension.getMaxAllowedViolations().get()).isZero();
        assertThat(extension.getIgnoreFailures().get()).isFalse();
        assertThat(extension.getFailOnDuplicateRules().get()).isFalse();
        assertThat(extension.getUseBuiltInHexagonalRulePack().get()).isTrue();
        assertThat(extension.getUseSpringRulePack().get()).isFalse();
    }

    private Project projectWithPlugin() {
        Project project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build();
        project.getPluginManager().apply(ArchitectureValidatorPlugin.class);
        return project;
    }
}
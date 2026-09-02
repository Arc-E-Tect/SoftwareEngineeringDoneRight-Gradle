package com.arc_e_tect.gradle.jacoco;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JacocoExclusionReportPlugin")
class JacocoExclusionReportPluginTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("registers the updateJacocoExclusionReportDSL task when applied")
    void registersUpdateDslTask() {
        Project project = projectWithPlugin();

        assertThat(project.getTasks().findByName(JacocoExclusionReportPlugin.UPDATE_DSL_TASK_NAME)).isNotNull();
    }

    @Test
    @DisplayName("defaults the updateJacocoExclusionReportDSL task's buildFile to the project's own build file")
    void updateDslTaskDefaultsBuildFileToProjectsOwnBuildFile() {
        Project project = projectWithPlugin();

        UpdateJacocoExclusionReportDslTask task = (UpdateJacocoExclusionReportDslTask)
                project.getTasks().getByName(JacocoExclusionReportPlugin.UPDATE_DSL_TASK_NAME);

        assertThat(task.getBuildFile().get().getAsFile()).isEqualTo(project.getBuildFile());
    }

    private Project projectWithPlugin() {
        Project project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build();
        project.getPluginManager().apply("java");
        project.getPluginManager().apply(JacocoExclusionReportPlugin.class);
        return project;
    }
}

package com.arc_e_tect.gradle.shadow;

import org.gradle.api.Project;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ShadowApiDetectorPlugin")
class ShadowApiDetectorPluginTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("registers the detectShadowApis task when applied")
    void registersDetectShadowApisTask() {
        Project project = projectWithPlugin();

        assertThat(project.getTasks().findByName(ShadowApiDetectorPlugin.TASK_NAME)).isNotNull();
    }

    @Test
    @DisplayName("extension default: failOnShadow is false")
    void extensionDefaultFailOnShadowIsFalse() {
        Project project = projectWithPlugin();

        assertThat(extension(project).getFailOnShadow().get()).isFalse();
    }

    @Test
    @DisplayName("extension default: reportFileName is shadow-apis.adoc")
    void extensionDefaultReportFileName() {
        Project project = projectWithPlugin();

        assertThat(extension(project).getReportFileName().get()).isEqualTo("shadow-apis.adoc");
    }

    @Test
    @DisplayName("extension default: reportDir is build/reports/shadow-api-detector")
    void extensionDefaultReportDir() {
        Project project = projectWithPlugin();

        assertThat(extension(project).getReportDir().get().getAsFile().getPath())
                .endsWith(String.join(File.separator, "build", "reports", "shadow-api-detector"));
    }

    @Test
    @DisplayName("extension default: systemUnderTestVersion is the project's version")
    void extensionDefaultSystemUnderTestVersionIsProjectVersion() {
        Project project = projectWithPlugin();
        project.setVersion("2.5.0");

        assertThat(extension(project).getSystemUnderTestVersion().get()).isEqualTo("2.5.0");
    }

    @Test
    @DisplayName("extension: systemUnderTestVersion can be overridden")
    void extensionSystemUnderTestVersionCanBeOverridden() {
        Project project = projectWithPlugin();
        project.setVersion("2.5.0");

        extension(project).getSystemUnderTestVersion().set("v1.0.0");

        assertThat(extension(project).getSystemUnderTestVersion().get()).isEqualTo("v1.0.0");
    }

    @Test
    @DisplayName("wires the task's systemUnderTestVersion from the extension")
    void wiresTaskSystemUnderTestVersionFromExtension() {
        Project project = projectWithPlugin();
        project.setVersion("2.5.0");

        ((ProjectInternal) project).evaluate();

        DetectShadowApisTask task = (DetectShadowApisTask)
                project.getTasks().getByName(ShadowApiDetectorPlugin.TASK_NAME);
        assertThat(task.getSystemUnderTestVersion().get()).isEqualTo("2.5.0");
    }

    @Test
    @DisplayName("extension default: controllerDirs is empty before evaluation")
    void extensionDefaultControllerDirsEmptyBeforeEvaluation() {
        Project project = projectWithPlugin();

        assertThat(extension(project).getControllerDirs().isEmpty()).isTrue();
    }

    @Test
    @DisplayName("task defaults controllerDirs to src/main/java after evaluation when unset")
    void taskDefaultsControllerDirsAfterEvaluation() {
        Project project = projectWithPlugin();

        ((ProjectInternal) project).evaluate();

        DetectShadowApisTask task = (DetectShadowApisTask)
                project.getTasks().getByName(ShadowApiDetectorPlugin.TASK_NAME);
        assertThat(task.getControllerDirs().getFiles())
                .containsExactly(new File(project.getProjectDir(), "src/main/java"));
    }

    @Test
    @DisplayName("does not override controllerDirs configured explicitly by the user")
    void doesNotOverrideExplicitControllerDirs() {
        Project project = projectWithPlugin();
        File custom = new File(project.getProjectDir(), "src/web/java");
        extension(project).getControllerDirs().from(custom);

        ((ProjectInternal) project).evaluate();

        DetectShadowApisTask task = (DetectShadowApisTask)
                project.getTasks().getByName(ShadowApiDetectorPlugin.TASK_NAME);
        assertThat(task.getControllerDirs().getFiles()).containsExactly(custom);
    }

    @Test
    @DisplayName("openApiDir defaults to the rootDocument's parent directory")
    void openApiDirDefaultsToRootDocumentParent() {
        Project project = projectWithPlugin();
        File rootDocument = new File(tempDir.toFile(), "openapi/openapi.yaml");
        extension(project).getRootDocument().set(rootDocument);

        assertThat(extension(project).getOpenApiDir().get().getAsFile()).isEqualTo(rootDocument.getParentFile());
    }

    @Test
    @DisplayName("does not hook detectShadowApis into the check lifecycle task by default")
    void doesNotHookIntoCheckTaskByDefault() {
        Project project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build();
        project.getPluginManager().apply("java");
        project.getPluginManager().apply(ShadowApiDetectorPlugin.class);

        assertThat(project.getTasks().getByName("check").getTaskDependencies().getDependencies(null))
                .noneMatch(t -> t.getName().equals(ShadowApiDetectorPlugin.TASK_NAME));
    }

    @Test
    @DisplayName("extension default: trackContractHistory is false")
    void extensionDefaultTrackContractHistoryIsFalse() {
        Project project = projectWithPlugin();

        assertThat(extension(project).getTrackContractHistory().get()).isFalse();
    }

    @Test
    @DisplayName("extension default: contractHistoryFile is shadow-api-detector-contract-history.ndjson in the project directory")
    void extensionDefaultContractHistoryFile() {
        Project project = projectWithPlugin();

        assertThat(extension(project).getContractHistoryFile().get().getAsFile())
                .isEqualTo(new File(project.getProjectDir(), "shadow-api-detector-contract-history.ndjson"));
    }

    @Test
    @DisplayName("extension default: updateContractHistory follows trackContractHistory")
    void extensionDefaultUpdateContractHistoryFollowsTrackContractHistory() {
        Project project = projectWithPlugin();

        extension(project).getTrackContractHistory().set(true);

        assertThat(extension(project).getUpdateContractHistory().get()).isTrue();
    }

    @Test
    @DisplayName("wires the task's contract history properties from the extension")
    void wiresTaskContractHistoryPropertiesFromExtension() {
        Project project = projectWithPlugin();
        extension(project).getTrackContractHistory().set(true);

        ((ProjectInternal) project).evaluate();

        DetectShadowApisTask task = (DetectShadowApisTask)
                project.getTasks().getByName(ShadowApiDetectorPlugin.TASK_NAME);
        assertThat(task.getTrackContractHistory().get()).isTrue();
        assertThat(task.getUpdateContractHistory().get()).isTrue();
        assertThat(task.getContractHistoryFile().get().getAsFile())
                .isEqualTo(new File(project.getProjectDir(), "shadow-api-detector-contract-history.ndjson"));
    }

    @Test
    @DisplayName("can be wired into check explicitly via dependsOn")
    void canBeWiredIntoCheckExplicitly() {
        Project project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build();
        project.getPluginManager().apply("java");
        project.getPluginManager().apply(ShadowApiDetectorPlugin.class);

        project.getTasks().named("check").configure(check -> check.dependsOn(ShadowApiDetectorPlugin.TASK_NAME));

        assertThat(project.getTasks().getByName("check").getTaskDependencies().getDependencies(null))
                .anyMatch(t -> t.getName().equals(ShadowApiDetectorPlugin.TASK_NAME));
    }

    @Test
    @DisplayName("registers the updateShadowApiDetectorDSL task when applied")
    void registersUpdateDslTask() {
        Project project = projectWithPlugin();

        assertThat(project.getTasks().findByName(ShadowApiDetectorPlugin.UPDATE_DSL_TASK_NAME)).isNotNull();
    }

    @Test
    @DisplayName("defaults the updateShadowApiDetectorDSL task's buildFile to the project's own build file")
    void updateDslTaskDefaultsBuildFileToProjectsOwnBuildFile() {
        Project project = projectWithPlugin();

        UpdateShadowApiDetectorDslTask task = (UpdateShadowApiDetectorDslTask)
                project.getTasks().getByName(ShadowApiDetectorPlugin.UPDATE_DSL_TASK_NAME);

        assertThat(task.getBuildFile().get().getAsFile()).isEqualTo(project.getBuildFile());
    }

    private Project projectWithPlugin() {
        Project project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build();
        project.getPluginManager().apply("java");
        project.getPluginManager().apply(ShadowApiDetectorPlugin.class);
        return project;
    }

    private ShadowApiDetectorExtension extension(Project project) {
        return project.getExtensions().getByType(ShadowApiDetectorExtension.class);
    }
}

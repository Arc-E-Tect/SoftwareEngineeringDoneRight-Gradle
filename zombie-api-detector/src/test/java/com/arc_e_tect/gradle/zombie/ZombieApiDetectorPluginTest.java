package com.arc_e_tect.gradle.zombie;

import org.gradle.api.Project;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ZombieApiDetectorPlugin")
class ZombieApiDetectorPluginTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("registers the detectZombieApis task when applied")
    void registersDetectZombieApisTask() {
        Project project = projectWithPlugin();

        assertThat(project.getTasks().findByName(ZombieApiDetectorPlugin.TASK_NAME)).isNotNull();
    }

    @Test
    @DisplayName("extension default: failOnZombie is false")
    void extensionDefaultFailOnZombieIsFalse() {
        Project project = projectWithPlugin();

        assertThat(extension(project).getFailOnZombie().get()).isFalse();
    }

    @Test
    @DisplayName("extension default: reportFileName is zombie-apis.adoc")
    void extensionDefaultReportFileName() {
        Project project = projectWithPlugin();

        assertThat(extension(project).getReportFileName().get()).isEqualTo("zombie-apis.adoc");
    }

    @Test
    @DisplayName("extension default: reportDir is build/reports/zombie-api-detector")
    void extensionDefaultReportDir() {
        Project project = projectWithPlugin();

        assertThat(extension(project).getReportDir().get().getAsFile().getPath())
                .endsWith(String.join(File.separator, "build", "reports", "zombie-api-detector"));
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

        DetectZombieApisTask task = (DetectZombieApisTask)
                project.getTasks().getByName(ZombieApiDetectorPlugin.TASK_NAME);
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

        DetectZombieApisTask task = (DetectZombieApisTask)
                project.getTasks().getByName(ZombieApiDetectorPlugin.TASK_NAME);
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
    @DisplayName("hooks detectZombieApis into the check lifecycle task when the java plugin is applied")
    void hooksIntoCheckTask() {
        Project project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build();
        project.getPluginManager().apply("java");
        project.getPluginManager().apply(ZombieApiDetectorPlugin.class);

        assertThat(project.getTasks().getByName("check").getTaskDependencies().getDependencies(null))
                .anyMatch(t -> t.getName().equals(ZombieApiDetectorPlugin.TASK_NAME));
    }

    private Project projectWithPlugin() {
        Project project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build();
        project.getPluginManager().apply("java");
        project.getPluginManager().apply(ZombieApiDetectorPlugin.class);
        return project;
    }

    private ZombieApiDetectorExtension extension(Project project) {
        return project.getExtensions().getByType(ZombieApiDetectorExtension.class);
    }
}

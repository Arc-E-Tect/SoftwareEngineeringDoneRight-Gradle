package com.arc_e_tect.gradle.doppelganger;

import org.gradle.api.Project;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DoppelgangerApiDetectorPlugin")
class DoppelgangerApiDetectorPluginTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("registers the detectDoppelgangerApis task when applied")
    void registersDetectDoppelgangerApisTask() {
        Project project = projectWithPlugin();

        assertThat(project.getTasks().findByName(DoppelgangerApiDetectorPlugin.TASK_NAME)).isNotNull();
    }

    @Test
    @DisplayName("extension default: failOnDoppelganger is false")
    void extensionDefaultFailOnDoppelgangerIsFalse() {
        Project project = projectWithPlugin();

        assertThat(extension(project).getFailOnDoppelganger().get()).isFalse();
    }

    @Test
    @DisplayName("extension default: useRestDocs is true")
    void extensionDefaultUseRestDocsIsTrue() {
        Project project = projectWithPlugin();

        assertThat(extension(project).getUseRestDocs().get()).isTrue();
    }

    @Test
    @DisplayName("extension default: useOpenApiRequestValidator is false")
    void extensionDefaultUseOpenApiRequestValidatorIsFalse() {
        Project project = projectWithPlugin();

        assertThat(extension(project).getUseOpenApiRequestValidator().get()).isFalse();
    }

    @Test
    @DisplayName("extension default: useSpringCloudContract is false")
    void extensionDefaultUseSpringCloudContractIsFalse() {
        Project project = projectWithPlugin();

        assertThat(extension(project).getUseSpringCloudContract().get()).isFalse();
    }

    @Test
    @DisplayName("extension default: reportFileName is doppelganger-apis.adoc")
    void extensionDefaultReportFileName() {
        Project project = projectWithPlugin();

        assertThat(extension(project).getReportFileName().get()).isEqualTo("doppelganger-apis.adoc");
    }

    @Test
    @DisplayName("extension default: reportDir is build/reports/doppelganger-api-detector")
    void extensionDefaultReportDir() {
        Project project = projectWithPlugin();

        assertThat(extension(project).getReportDir().get().getAsFile().getPath())
                .endsWith(String.join(File.separator, "build", "reports", "doppelganger-api-detector"));
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

        DetectDoppelgangerApisTask task = (DetectDoppelgangerApisTask)
                project.getTasks().getByName(DoppelgangerApiDetectorPlugin.TASK_NAME);
        assertThat(task.getSystemUnderTestVersion().get()).isEqualTo("2.5.0");
    }

    @Test
    @DisplayName("extension default: controllerDirs is empty before evaluation")
    void extensionDefaultControllerDirsEmptyBeforeEvaluation() {
        Project project = projectWithPlugin();

        assertThat(extension(project).getControllerDirs().isEmpty()).isTrue();
    }

    @Test
    @DisplayName("extension default: testDirs is empty before evaluation")
    void extensionDefaultTestDirsEmptyBeforeEvaluation() {
        Project project = projectWithPlugin();

        assertThat(extension(project).getTestDirs().isEmpty()).isTrue();
    }

    @Test
    @DisplayName("task defaults controllerDirs to src/main/java after evaluation when unset")
    void taskDefaultsControllerDirsAfterEvaluation() {
        Project project = projectWithPlugin();

        ((ProjectInternal) project).evaluate();

        DetectDoppelgangerApisTask task = (DetectDoppelgangerApisTask)
                project.getTasks().getByName(DoppelgangerApiDetectorPlugin.TASK_NAME);
        assertThat(task.getControllerDirs().getFiles())
                .containsExactly(new File(project.getProjectDir(), "src/main/java"));
    }

    @Test
    @DisplayName("task defaults testDirs to src/test/java after evaluation when unset")
    void taskDefaultsTestDirsAfterEvaluation() {
        Project project = projectWithPlugin();

        ((ProjectInternal) project).evaluate();

        DetectDoppelgangerApisTask task = (DetectDoppelgangerApisTask)
                project.getTasks().getByName(DoppelgangerApiDetectorPlugin.TASK_NAME);
        assertThat(task.getTestDirs().getFiles())
                .containsExactly(new File(project.getProjectDir(), "src/test/java"));
    }

    @Test
    @DisplayName("does not override controllerDirs configured explicitly by the user")
    void doesNotOverrideExplicitControllerDirs() {
        Project project = projectWithPlugin();
        File custom = new File(project.getProjectDir(), "src/web/java");
        extension(project).getControllerDirs().from(custom);

        ((ProjectInternal) project).evaluate();

        DetectDoppelgangerApisTask task = (DetectDoppelgangerApisTask)
                project.getTasks().getByName(DoppelgangerApiDetectorPlugin.TASK_NAME);
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
    @DisplayName("contractsDir defaults to src/test/resources/contracts")
    void contractsDirDefaultsToStandardLocation() {
        Project project = projectWithPlugin();

        assertThat(extension(project).getContractsDir().get().getAsFile())
                .isEqualTo(new File(project.getProjectDir(), "src/test/resources/contracts"));
    }

    @Test
    @DisplayName("does not hook detectDoppelgangerApis into the check lifecycle task by default")
    void doesNotHookIntoCheckTaskByDefault() {
        Project project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build();
        project.getPluginManager().apply("java");
        project.getPluginManager().apply(DoppelgangerApiDetectorPlugin.class);

        assertThat(project.getTasks().getByName("check").getTaskDependencies().getDependencies(null))
                .noneMatch(t -> t.getName().equals(DoppelgangerApiDetectorPlugin.TASK_NAME));
    }

    @Test
    @DisplayName("can be wired into check explicitly via dependsOn")
    void canBeWiredIntoCheckExplicitly() {
        Project project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build();
        project.getPluginManager().apply("java");
        project.getPluginManager().apply(DoppelgangerApiDetectorPlugin.class);

        project.getTasks().named("check").configure(check -> check.dependsOn(DoppelgangerApiDetectorPlugin.TASK_NAME));

        assertThat(project.getTasks().getByName("check").getTaskDependencies().getDependencies(null))
                .anyMatch(t -> t.getName().equals(DoppelgangerApiDetectorPlugin.TASK_NAME));
    }

    private Project projectWithPlugin() {
        Project project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build();
        project.getPluginManager().apply("java");
        project.getPluginManager().apply(DoppelgangerApiDetectorPlugin.class);
        return project;
    }

    private DoppelgangerApiDetectorExtension extension(Project project) {
        return project.getExtensions().getByType(DoppelgangerApiDetectorExtension.class);
    }
}

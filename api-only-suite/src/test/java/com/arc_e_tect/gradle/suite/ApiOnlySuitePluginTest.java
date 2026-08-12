package com.arc_e_tect.gradle.suite;

import com.arc_e_tect.gradle.doppelganger.DetectDoppelgangerApisTask;
import com.arc_e_tect.gradle.doppelganger.DoppelgangerApiDetectorExtension;
import com.arc_e_tect.gradle.doppelganger.DoppelgangerApiDetectorPlugin;
import com.arc_e_tect.gradle.mirage.DetectMirageApisTask;
import com.arc_e_tect.gradle.mirage.MirageApiDetectorExtension;
import com.arc_e_tect.gradle.mirage.MirageApiDetectorPlugin;
import com.arc_e_tect.gradle.shadow.DetectShadowApisTask;
import com.arc_e_tect.gradle.shadow.ShadowApiDetectorExtension;
import com.arc_e_tect.gradle.shadow.ShadowApiDetectorPlugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ApiOnlySuitePlugin")
class ApiOnlySuitePluginTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("registers the detectShadowApis task when applied")
    void registersDetectShadowApisTask() {
        Project project = projectWithPlugin();

        assertThat(project.getTasks().findByName(ShadowApiDetectorPlugin.TASK_NAME)).isNotNull();
    }

    @Test
    @DisplayName("registers the detectMirageApis task when applied")
    void registersDetectMirageApisTask() {
        Project project = projectWithPlugin();

        assertThat(project.getTasks().findByName(MirageApiDetectorPlugin.TASK_NAME)).isNotNull();
    }

    @Test
    @DisplayName("registers the detectDoppelgangerApis task when applied")
    void registersDetectDoppelgangerApisTask() {
        Project project = projectWithPlugin();

        assertThat(project.getTasks().findByName(DoppelgangerApiDetectorPlugin.TASK_NAME)).isNotNull();
    }

    @Test
    @DisplayName("registers the aggregate detectAllApiGaps task when applied")
    void registersDetectAllApiGapsTask() {
        Project project = projectWithPlugin();

        assertThat(project.getTasks().findByName(ApiOnlySuitePlugin.TASK_NAME)).isNotNull();
    }

    @Test
    @DisplayName("registers the apiOnlySuite extension when applied")
    void registersApiOnlySuiteExtension() {
        Project project = projectWithPlugin();

        assertThat(project.getExtensions().findByType(ApiOnlySuiteExtension.class)).isNotNull();
    }

    @Test
    @DisplayName("puts detectAllApiGaps in the verification group")
    void detectAllApiGapsIsInVerificationGroup() {
        Project project = projectWithPlugin();

        Task task = project.getTasks().getByName(ApiOnlySuitePlugin.TASK_NAME);
        assertThat(task.getGroup()).isEqualTo("verification");
    }

    @Test
    @DisplayName("makes detectAllApiGaps depend on all three individual detector tasks")
    void detectAllApiGapsDependsOnAllThreeIndividualTasks() {
        Project project = projectWithPlugin();

        Task task = project.getTasks().getByName(ApiOnlySuitePlugin.TASK_NAME);
        Set<String> dependencyNames = task.getTaskDependencies().getDependencies(null).stream()
                .map(Task::getName)
                .collect(Collectors.toSet());

        assertThat(dependencyNames).containsExactlyInAnyOrder(
                ShadowApiDetectorPlugin.TASK_NAME,
                MirageApiDetectorPlugin.TASK_NAME,
                DoppelgangerApiDetectorPlugin.TASK_NAME);
    }

    @Test
    @DisplayName("does not hook detectAllApiGaps into the check lifecycle task by default")
    void doesNotHookIntoCheckTaskByDefault() {
        Project project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build();
        project.getPluginManager().apply("java");
        project.getPluginManager().apply(ApiOnlySuitePlugin.class);

        assertThat(project.getTasks().getByName("check").getTaskDependencies().getDependencies(null))
                .noneMatch(t -> t.getName().equals(ApiOnlySuitePlugin.TASK_NAME));
    }

    @Test
    @DisplayName("can be wired into check explicitly via dependsOn")
    void canBeWiredIntoCheckExplicitly() {
        Project project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build();
        project.getPluginManager().apply("java");
        project.getPluginManager().apply(ApiOnlySuitePlugin.class);

        project.getTasks().named("check").configure(check -> check.dependsOn(ApiOnlySuitePlugin.TASK_NAME));

        assertThat(project.getTasks().getByName("check").getTaskDependencies().getDependencies(null))
                .anyMatch(t -> t.getName().equals(ApiOnlySuitePlugin.TASK_NAME));
    }

    @Test
    @DisplayName("forwards apiOnlySuite.rootDocument to all three plugins when none set it directly")
    void suiteWorksWithOnlyApiOnlySuiteRootDocumentSet() {
        Project project = projectWithPlugin();
        File rootDocument = new File(tempDir.toFile(), "openapi.yaml");
        suiteExtension(project).getRootDocument().set(rootDocument);

        ((ProjectInternal) project).evaluate();

        assertThat(java.util.List.of(
                shadowTask(project).getRootDocument().getAsFile().get(),
                mirageTask(project).getRootDocument().getAsFile().get(),
                doppelgangerTask(project).getRootDocument().getAsFile().get()))
                .containsOnly(rootDocument);
    }

    @Test
    @DisplayName("shadowApiDetector's own rootDocument wins over apiOnlySuite's rootDocument")
    void perPluginRootDocumentOverridesSuiteRootDocument() {
        Project project = projectWithPlugin();
        File suiteDocument = new File(tempDir.toFile(), "suite-openapi.yaml");
        File shadowDocument = new File(tempDir.toFile(), "shadow-openapi.yaml");
        suiteExtension(project).getRootDocument().set(suiteDocument);
        shadowExtension(project).getRootDocument().set(shadowDocument);

        ((ProjectInternal) project).evaluate();

        assertThat(shadowTask(project).getRootDocument().getAsFile().get()).isEqualTo(shadowDocument);
    }

    @Test
    @DisplayName("mirageApiDetector still forwards apiOnlySuite's rootDocument when only shadowApiDetector overrides it")
    void suiteRootDocumentStillForwardsToOtherPluginsWhenOnlyOneIsOverridden() {
        Project project = projectWithPlugin();
        File suiteDocument = new File(tempDir.toFile(), "suite-openapi.yaml");
        File shadowDocument = new File(tempDir.toFile(), "shadow-openapi.yaml");
        suiteExtension(project).getRootDocument().set(suiteDocument);
        shadowExtension(project).getRootDocument().set(shadowDocument);

        ((ProjectInternal) project).evaluate();

        assertThat(mirageTask(project).getRootDocument().getAsFile().get()).isEqualTo(suiteDocument);
    }

    @Test
    @DisplayName("forwards apiOnlySuite.controllerDirs to shadowApiDetector when not set directly")
    void suiteControllerDirsForwardedWhenNotSetDirectly() {
        Project project = projectWithPlugin();
        File customDir = new File(project.getProjectDir(), "src/shared/java");
        suiteExtension(project).getControllerDirs().from(customDir);
        suiteExtension(project).getRootDocument().set(new File(tempDir.toFile(), "openapi.yaml"));

        ((ProjectInternal) project).evaluate();

        assertThat(shadowTask(project).getControllerDirs().getFiles()).containsExactly(customDir);
    }

    @Test
    @DisplayName("shadowApiDetector's own controllerDirs wins over apiOnlySuite's controllerDirs")
    void perPluginControllerDirsOverridesSuiteControllerDirs() {
        Project project = projectWithPlugin();
        File suiteDir = new File(project.getProjectDir(), "src/shared/java");
        File shadowDir = new File(project.getProjectDir(), "src/shadow-only/java");
        suiteExtension(project).getControllerDirs().from(suiteDir);
        shadowExtension(project).getControllerDirs().from(shadowDir);
        suiteExtension(project).getRootDocument().set(new File(tempDir.toFile(), "openapi.yaml"));

        ((ProjectInternal) project).evaluate();

        assertThat(shadowTask(project).getControllerDirs().getFiles()).containsExactly(shadowDir);
    }

    @Test
    @DisplayName("defaults controllerDirs to src/main/java for a plugin when neither the suite nor the plugin configures it")
    void defaultsControllerDirsWhenNeitherSuiteNorPluginConfiguresIt() {
        Project project = projectWithPlugin();
        suiteExtension(project).getRootDocument().set(new File(tempDir.toFile(), "openapi.yaml"));

        ((ProjectInternal) project).evaluate();

        assertThat(shadowTask(project).getControllerDirs().getFiles())
                .containsExactly(new File(project.getProjectDir(), "src/main/java"));
    }

    private Project projectWithPlugin() {
        Project project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build();
        project.getPluginManager().apply("java");
        project.getPluginManager().apply(ApiOnlySuitePlugin.class);
        return project;
    }

    private ApiOnlySuiteExtension suiteExtension(Project project) {
        return project.getExtensions().getByType(ApiOnlySuiteExtension.class);
    }

    private ShadowApiDetectorExtension shadowExtension(Project project) {
        return project.getExtensions().getByType(ShadowApiDetectorExtension.class);
    }

    private DetectShadowApisTask shadowTask(Project project) {
        return (DetectShadowApisTask) project.getTasks().getByName(ShadowApiDetectorPlugin.TASK_NAME);
    }

    private DetectMirageApisTask mirageTask(Project project) {
        return (DetectMirageApisTask) project.getTasks().getByName(MirageApiDetectorPlugin.TASK_NAME);
    }

    private DetectDoppelgangerApisTask doppelgangerTask(Project project) {
        return (DetectDoppelgangerApisTask) project.getTasks().getByName(DoppelgangerApiDetectorPlugin.TASK_NAME);
    }
}

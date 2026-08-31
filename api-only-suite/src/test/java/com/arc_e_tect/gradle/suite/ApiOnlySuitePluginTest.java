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
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    @DisplayName("makes detectAllApiGaps depend on dedicated non-failing task instances, not the individual detector tasks themselves")
    void detectAllApiGapsDependsOnDedicatedNonFailingTasks() {
        Project project = projectWithPlugin();

        Task task = project.getTasks().getByName(ApiOnlySuitePlugin.TASK_NAME);
        Set<String> dependencyNames = task.getTaskDependencies().getDependencies(null).stream()
                .map(Task::getName)
                .collect(Collectors.toSet());

        assertThat(dependencyNames).containsExactlyInAnyOrder(
                "shadowApiGapsForSuite", "mirageApiGapsForSuite", "doppelgangerApiGapsForSuite");
        assertThat(dependencyNames).doesNotContain(
                ShadowApiDetectorPlugin.TASK_NAME,
                MirageApiDetectorPlugin.TASK_NAME,
                DoppelgangerApiDetectorPlugin.TASK_NAME);
    }

    @Test
    @DisplayName("forces failOnShadow/failOnMirage/failOnDoppelganger to false on detectAllApiGaps's own task instances, "
            + "even when the individual plugins are configured to fail")
    void nonFailingTasksForceFailPropertiesToFalse() {
        Project project = projectWithPlugin();
        suiteExtension(project).getRootDocument().set(new File(tempDir.toFile(), "openapi.yaml"));
        shadowExtension(project).getFailOnShadow().set(true);
        mirageExtension(project).getFailOnMirage().set(true);
        doppelgangerExtension(project).getFailOnDoppelganger().set(true);

        ((ProjectInternal) project).evaluate();

        assertThat(shadowForSuiteTask(project).getFailOnShadow().get()).isFalse();
        assertThat(mirageForSuiteTask(project).getFailOnMirage().get()).isFalse();
        assertThat(doppelgangerForSuiteTask(project).getFailOnDoppelganger().get()).isFalse();
        // The individually-invokable tasks themselves are unaffected - still respect the user's
        // own configured fail-on-gap value.
        assertThat(shadowTask(project).getFailOnShadow().get()).isTrue();
        assertThat(mirageTask(project).getFailOnMirage().get()).isTrue();
        assertThat(doppelgangerTask(project).getFailOnDoppelganger().get()).isTrue();
    }

    @Test
    @DisplayName("wires shadowApiGapsForSuite identically to detectShadowApis otherwise")
    void nonFailingShadowTaskMirrorsPrimaryConfiguration() {
        Project project = projectWithPlugin();
        File rootDocument = new File(tempDir.toFile(), "openapi.yaml");
        suiteExtension(project).getRootDocument().set(rootDocument);
        shadowExtension(project).getExcludePaths().add("/actuator/health");

        ((ProjectInternal) project).evaluate();

        assertThat(shadowForSuiteTask(project).getRootDocument().getAsFile().get())
                .isEqualTo(shadowTask(project).getRootDocument().getAsFile().get());
        assertThat(shadowForSuiteTask(project).getControllerDirs().getFiles())
                .isEqualTo(shadowTask(project).getControllerDirs().getFiles());
        assertThat(shadowForSuiteTask(project).getReportDir().get().getAsFile())
                .isEqualTo(shadowTask(project).getReportDir().get().getAsFile());
        assertThat(shadowForSuiteTask(project).getReportFileName().get())
                .isEqualTo(shadowTask(project).getReportFileName().get());
        assertThat(shadowForSuiteTask(project).getExcludePaths().get())
                .isEqualTo(shadowTask(project).getExcludePaths().get());
    }

    @Test
    @DisplayName("wires mirageApiGapsForSuite identically to detectMirageApis otherwise, including scanMocks/stubDirs/stubSourceDirs/basePath")
    void nonFailingMirageTaskMirrorsPrimaryConfiguration() {
        Project project = projectWithPlugin();
        File rootDocument = new File(tempDir.toFile(), "openapi.yaml");
        File stubDir = new File(project.getProjectDir(), "src/test/resources/mappings");
        File stubSourceDir = new File(project.getProjectDir(), "src/test/java");
        suiteExtension(project).getRootDocument().set(rootDocument);
        mirageExtension(project).getScanMocks().set(true);
        mirageExtension(project).getStubDirs().from(stubDir);
        mirageExtension(project).getStubSourceDirs().from(stubSourceDir);
        mirageExtension(project).getBasePath().set("/user-account");
        mirageExtension(project).getExcludeWellKnown().add("spring-boot-actuator");

        ((ProjectInternal) project).evaluate();

        assertThat(mirageForSuiteTask(project).getRootDocument().getAsFile().get())
                .isEqualTo(mirageTask(project).getRootDocument().getAsFile().get());
        assertThat(mirageForSuiteTask(project).getControllerDirs().getFiles())
                .isEqualTo(mirageTask(project).getControllerDirs().getFiles());
        assertThat(mirageForSuiteTask(project).getScanMocks().get())
                .isEqualTo(mirageTask(project).getScanMocks().get());
        assertThat(mirageForSuiteTask(project).getStubDirs().getFiles())
                .isEqualTo(mirageTask(project).getStubDirs().getFiles());
        assertThat(mirageForSuiteTask(project).getStubSourceDirs().getFiles())
                .isEqualTo(mirageTask(project).getStubSourceDirs().getFiles())
                .containsExactly(stubSourceDir);
        assertThat(mirageForSuiteTask(project).getBasePath().get())
                .isEqualTo(mirageTask(project).getBasePath().get());
        assertThat(mirageForSuiteTask(project).getReportDir().get().getAsFile())
                .isEqualTo(mirageTask(project).getReportDir().get().getAsFile());
        assertThat(mirageForSuiteTask(project).getReportFileName().get())
                .isEqualTo(mirageTask(project).getReportFileName().get());
        assertThat(mirageForSuiteTask(project).getExcludeWellKnown().get())
                .isEqualTo(mirageTask(project).getExcludeWellKnown().get());
    }

    @Test
    @DisplayName("wires doppelgangerApiGapsForSuite identically to detectDoppelgangerApis otherwise, including testDirs/testDirsUserConfigured")
    void nonFailingDoppelgangerTaskMirrorsPrimaryConfiguration() {
        Project project = projectWithPlugin();
        File rootDocument = new File(tempDir.toFile(), "openapi.yaml");
        File testDir = new File(project.getProjectDir(), "src/testContract/java");
        suiteExtension(project).getRootDocument().set(rootDocument);
        doppelgangerExtension(project).getTestDirs().from(testDir);
        doppelgangerExtension(project).getExcludeWellKnown().add("spring-boot-actuator");

        ((ProjectInternal) project).evaluate();

        assertThat(doppelgangerForSuiteTask(project).getRootDocument().getAsFile().get())
                .isEqualTo(doppelgangerTask(project).getRootDocument().getAsFile().get());
        assertThat(doppelgangerForSuiteTask(project).getControllerDirs().getFiles())
                .isEqualTo(doppelgangerTask(project).getControllerDirs().getFiles());
        assertThat(doppelgangerForSuiteTask(project).getTestDirs().getFiles())
                .isEqualTo(doppelgangerTask(project).getTestDirs().getFiles())
                .containsExactly(testDir);
        assertThat(doppelgangerForSuiteTask(project).getTestDirsUserConfigured().get())
                .isEqualTo(doppelgangerTask(project).getTestDirsUserConfigured().get())
                .isTrue();
        assertThat(doppelgangerForSuiteTask(project).getReportDir().get().getAsFile())
                .isEqualTo(doppelgangerTask(project).getReportDir().get().getAsFile());
        assertThat(doppelgangerForSuiteTask(project).getReportFileName().get())
                .isEqualTo(doppelgangerTask(project).getReportFileName().get());
        assertThat(doppelgangerForSuiteTask(project).getExcludeWellKnown().get())
                .isEqualTo(doppelgangerTask(project).getExcludeWellKnown().get());
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

    @Test
    @DisplayName("forwards apiOnlySuite.excludePaths to all three plugins when none set it directly")
    void suiteExcludePathsForwardedWhenNotSetDirectly() {
        Project project = projectWithPlugin();
        suiteExtension(project).getRootDocument().set(new File(tempDir.toFile(), "openapi.yaml"));
        suiteExtension(project).getExcludePaths().add("/actuator/health");

        ((ProjectInternal) project).evaluate();

        assertThat(java.util.List.of(
                shadowTask(project).getExcludePaths().get(),
                mirageTask(project).getExcludePaths().get(),
                doppelgangerTask(project).getExcludePaths().get()))
                .containsOnly(java.util.List.of("/actuator/health"));
    }

    @Test
    @DisplayName("shadowApiDetector's own excludePaths wins over apiOnlySuite's excludePaths")
    void perPluginExcludePathsOverridesSuiteExcludePaths() {
        Project project = projectWithPlugin();
        suiteExtension(project).getRootDocument().set(new File(tempDir.toFile(), "openapi.yaml"));
        suiteExtension(project).getExcludePaths().add("/actuator/health");
        shadowExtension(project).getExcludePaths().add("/actuator/info");

        ((ProjectInternal) project).evaluate();

        assertThat(shadowTask(project).getExcludePaths().get()).containsExactly("/actuator/info");
    }

    @Test
    @DisplayName("forwards apiOnlySuite.excludeFiles to all three plugins when none set it directly")
    void suiteExcludeFilesForwardedWhenNotSetDirectly() {
        Project project = projectWithPlugin();
        File exclusionsFile = new File(tempDir.toFile(), "exclusions.yaml");
        suiteExtension(project).getRootDocument().set(new File(tempDir.toFile(), "openapi.yaml"));
        suiteExtension(project).getExcludeFiles().from(exclusionsFile);

        ((ProjectInternal) project).evaluate();

        assertThat(mirageTask(project).getExcludeFiles().getFiles()).containsExactly(exclusionsFile);
    }

    @Test
    @DisplayName("shadowApiDetector's own excludeFiles wins over apiOnlySuite's excludeFiles")
    void perPluginExcludeFilesOverridesSuiteExcludeFiles() {
        Project project = projectWithPlugin();
        File suiteFile = new File(tempDir.toFile(), "suite-exclusions.yaml");
        File shadowFile = new File(tempDir.toFile(), "shadow-exclusions.yaml");
        suiteExtension(project).getRootDocument().set(new File(tempDir.toFile(), "openapi.yaml"));
        suiteExtension(project).getExcludeFiles().from(suiteFile);
        shadowExtension(project).getExcludeFiles().from(shadowFile);

        ((ProjectInternal) project).evaluate();

        assertThat(shadowTask(project).getExcludeFiles().getFiles()).containsExactly(shadowFile);
    }

    @Test
    @DisplayName("forwards apiOnlySuite.excludeWellKnown to all three plugins when none set it directly")
    void suiteExcludeWellKnownForwardedWhenNotSetDirectly() {
        Project project = projectWithPlugin();
        suiteExtension(project).getRootDocument().set(new File(tempDir.toFile(), "openapi.yaml"));
        suiteExtension(project).getExcludeWellKnown().add("spring-boot-actuator");

        ((ProjectInternal) project).evaluate();

        assertThat(java.util.List.of(
                shadowTask(project).getExcludeWellKnown().get(),
                mirageTask(project).getExcludeWellKnown().get(),
                doppelgangerTask(project).getExcludeWellKnown().get()))
                .containsOnly(java.util.List.of("spring-boot-actuator"));
    }

    @Test
    @DisplayName("failOnDetection defaults to false")
    void failOnDetectionDefaultsToFalse() {
        Project project = projectWithPlugin();

        assertThat(suiteExtension(project).getFailOnDetection().get()).isFalse();
    }

    @Test
    @DisplayName("forwards apiOnlySuite.failOnDetection to all three plugins when none set their own fail-on-gap property")
    void forwardsFailOnDetectionToAllThreePluginsWhenNoneSetItDirectly() {
        Project project = projectWithPlugin();
        suiteExtension(project).getRootDocument().set(new File(tempDir.toFile(), "openapi.yaml"));
        suiteExtension(project).getFailOnDetection().set(true);

        ((ProjectInternal) project).evaluate();

        assertThat(shadowTask(project).getFailOnShadow().get()).isTrue();
        assertThat(mirageTask(project).getFailOnMirage().get()).isTrue();
        assertThat(doppelgangerTask(project).getFailOnDoppelganger().get()).isTrue();
    }

    @Test
    @DisplayName("shadowApiDetector's own failOnShadow wins over apiOnlySuite's failOnDetection")
    void perPluginFailOnShadowOverridesSuiteFailOnDetection() {
        Project project = projectWithPlugin();
        suiteExtension(project).getRootDocument().set(new File(tempDir.toFile(), "openapi.yaml"));
        suiteExtension(project).getFailOnDetection().set(true);
        shadowExtension(project).getFailOnShadow().set(false);

        ((ProjectInternal) project).evaluate();

        assertThat(shadowTask(project).getFailOnShadow().get()).isFalse();
        // Mirage and Doppelganger still pick up apiOnlySuite's failOnDetection, since neither of
        // them overrides its own fail-on-gap property directly.
        assertThat(mirageTask(project).getFailOnMirage().get()).isTrue();
        assertThat(doppelgangerTask(project).getFailOnDoppelganger().get()).isTrue();
    }

    @Test
    @DisplayName("failOnDetection does not affect detectAllApiGaps's own non-failing task instances")
    void failOnDetectionDoesNotAffectTheNonFailingSuiteTasks() {
        Project project = projectWithPlugin();
        suiteExtension(project).getRootDocument().set(new File(tempDir.toFile(), "openapi.yaml"));
        suiteExtension(project).getFailOnDetection().set(true);

        ((ProjectInternal) project).evaluate();

        assertThat(shadowForSuiteTask(project).getFailOnShadow().get()).isFalse();
        assertThat(mirageForSuiteTask(project).getFailOnMirage().get()).isFalse();
        assertThat(doppelgangerForSuiteTask(project).getFailOnDoppelganger().get()).isFalse();
    }

    @Test
    @DisplayName("shadowApiGapsForSuite does not throw on a detected shadow API even when failOnShadow is true, unlike detectShadowApis itself")
    void nonFailingShadowTaskDoesNotThrowWhileThePrimaryTaskDoes() throws Exception {
        Project project = projectWithPlugin();

        File controllerDir = new File(tempDir.toFile(), "src/main/java/com/example");
        Files.createDirectories(controllerDir.toPath());
        Files.writeString(controllerDir.toPath().resolve("OrderController.java"), """
                package com.example;

                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.RequestMapping;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                @RequestMapping("/orders")
                public class OrderController {

                    @GetMapping
                    public String listOrders() { return "[]"; }
                }
                """);

        File openApiDir = new File(tempDir.toFile(), "openapi");
        Files.createDirectories(openApiDir.toPath());
        File rootDocument = new File(openApiDir, "openapi.yaml");
        Files.writeString(rootDocument.toPath(), """
                openapi: 3.0.3
                info:
                  title: Test API
                  version: "1.0"
                paths: {}
                """);

        suiteExtension(project).getRootDocument().set(rootDocument);
        suiteExtension(project).getControllerDirs().from(controllerDir);
        shadowExtension(project).getFailOnShadow().set(true);

        ((ProjectInternal) project).evaluate();

        assertThatThrownBy(() -> shadowTask(project).generate()).isInstanceOf(GradleException.class);
        assertThatCode(() -> shadowForSuiteTask(project).generate()).doesNotThrowAnyException();
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

    private MirageApiDetectorExtension mirageExtension(Project project) {
        return project.getExtensions().getByType(MirageApiDetectorExtension.class);
    }

    private DoppelgangerApiDetectorExtension doppelgangerExtension(Project project) {
        return project.getExtensions().getByType(DoppelgangerApiDetectorExtension.class);
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

    private DetectShadowApisTask shadowForSuiteTask(Project project) {
        return (DetectShadowApisTask) project.getTasks().getByName("shadowApiGapsForSuite");
    }

    private DetectMirageApisTask mirageForSuiteTask(Project project) {
        return (DetectMirageApisTask) project.getTasks().getByName("mirageApiGapsForSuite");
    }

    private DetectDoppelgangerApisTask doppelgangerForSuiteTask(Project project) {
        return (DetectDoppelgangerApisTask) project.getTasks().getByName("doppelgangerApiGapsForSuite");
    }
}

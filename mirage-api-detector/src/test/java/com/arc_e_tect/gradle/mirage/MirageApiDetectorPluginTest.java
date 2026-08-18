package com.arc_e_tect.gradle.mirage;

import org.gradle.api.Project;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MirageApiDetectorPlugin")
class MirageApiDetectorPluginTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("registers the detectMirageApis task when applied")
    void registersDetectMirageApisTask() {
        Project project = projectWithPlugin();

        assertThat(project.getTasks().findByName(MirageApiDetectorPlugin.TASK_NAME)).isNotNull();
    }

    @Test
    @DisplayName("extension default: failOnMirage is false")
    void extensionDefaultFailOnMirageIsFalse() {
        Project project = projectWithPlugin();

        assertThat(extension(project).getFailOnMirage().get()).isFalse();
    }

    @Test
    @DisplayName("extension default: reportFileName is mirage-apis.adoc")
    void extensionDefaultReportFileName() {
        Project project = projectWithPlugin();

        assertThat(extension(project).getReportFileName().get()).isEqualTo("mirage-apis.adoc");
    }

    @Test
    @DisplayName("extension default: reportDir is build/reports/mirage-api-detector")
    void extensionDefaultReportDir() {
        Project project = projectWithPlugin();

        assertThat(extension(project).getReportDir().get().getAsFile().getPath())
                .endsWith(String.join(File.separator, "build", "reports", "mirage-api-detector"));
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

        DetectMirageApisTask task = (DetectMirageApisTask)
                project.getTasks().getByName(MirageApiDetectorPlugin.TASK_NAME);
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

        DetectMirageApisTask task = (DetectMirageApisTask)
                project.getTasks().getByName(MirageApiDetectorPlugin.TASK_NAME);
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

        DetectMirageApisTask task = (DetectMirageApisTask)
                project.getTasks().getByName(MirageApiDetectorPlugin.TASK_NAME);
        assertThat(task.getControllerDirs().getFiles()).containsExactly(custom);
    }

    @Test
    @DisplayName("extension default: scanMocks is false")
    void extensionDefaultScanMocksIsFalse() {
        Project project = projectWithPlugin();

        assertThat(extension(project).getScanMocks().get()).isFalse();
    }

    @Test
    @DisplayName("extension default: stubDirs is empty before evaluation")
    void extensionDefaultStubDirsEmptyBeforeEvaluation() {
        Project project = projectWithPlugin();

        assertThat(extension(project).getStubDirs().isEmpty()).isTrue();
    }

    @Test
    @DisplayName("extension default: basePath is unset")
    void extensionDefaultBasePathIsUnset() {
        Project project = projectWithPlugin();

        assertThat(extension(project).getBasePath().isPresent()).isFalse();
    }

    @Test
    @DisplayName("wires the task's basePath from the extension")
    void wiresTaskBasePathFromExtension() {
        Project project = projectWithPlugin();
        extension(project).getBasePath().set("/crm-service");

        ((ProjectInternal) project).evaluate();

        DetectMirageApisTask task = (DetectMirageApisTask)
                project.getTasks().getByName(MirageApiDetectorPlugin.TASK_NAME);
        assertThat(task.getBasePath().get()).isEqualTo("/crm-service");
    }

    @Test
    @DisplayName("task defaults stubDirs to src/test/resources/mappings when scanMocks is true and unset")
    void taskDefaultsStubDirsAfterEvaluationWhenScanningMocks() {
        Project project = projectWithPlugin();
        extension(project).getScanMocks().set(true);

        ((ProjectInternal) project).evaluate();

        DetectMirageApisTask task = (DetectMirageApisTask)
                project.getTasks().getByName(MirageApiDetectorPlugin.TASK_NAME);
        assertThat(task.getStubDirs().getFiles())
                .containsExactly(new File(project.getProjectDir(), "src/test/resources/mappings"));
    }

    @Test
    @DisplayName("does not default stubDirs when scanMocks is false")
    void doesNotDefaultStubDirsWhenNotScanningMocks() {
        Project project = projectWithPlugin();

        ((ProjectInternal) project).evaluate();

        DetectMirageApisTask task = (DetectMirageApisTask)
                project.getTasks().getByName(MirageApiDetectorPlugin.TASK_NAME);
        assertThat(task.getStubDirs().getFiles()).isEmpty();
    }

    @Test
    @DisplayName("defaults controllerDirs even when scanMocks is true, since controllers are always scanned")
    void defaultsControllerDirsEvenWhenScanningMocks() {
        Project project = projectWithPlugin();
        extension(project).getScanMocks().set(true);

        ((ProjectInternal) project).evaluate();

        DetectMirageApisTask task = (DetectMirageApisTask)
                project.getTasks().getByName(MirageApiDetectorPlugin.TASK_NAME);
        assertThat(task.getControllerDirs().getFiles())
                .containsExactly(new File(project.getProjectDir(), "src/main/java"));
    }

    @Test
    @DisplayName("does not override stubDirs configured explicitly by the user")
    void doesNotOverrideExplicitStubDirs() {
        Project project = projectWithPlugin();
        extension(project).getScanMocks().set(true);
        File custom = new File(project.getProjectDir(), "src/test/resources/stubs");
        extension(project).getStubDirs().from(custom);

        ((ProjectInternal) project).evaluate();

        DetectMirageApisTask task = (DetectMirageApisTask)
                project.getTasks().getByName(MirageApiDetectorPlugin.TASK_NAME);
        assertThat(task.getStubDirs().getFiles()).containsExactly(custom);
    }

    @Test
    @DisplayName("wires the task's scanMocks from the extension")
    void wiresTaskScanMocksFromExtension() {
        Project project = projectWithPlugin();
        extension(project).getScanMocks().set(true);

        ((ProjectInternal) project).evaluate();

        DetectMirageApisTask task = (DetectMirageApisTask)
                project.getTasks().getByName(MirageApiDetectorPlugin.TASK_NAME);
        assertThat(task.getScanMocks().get()).isTrue();
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
    @DisplayName("extension default: trackContractHistory is false")
    void extensionDefaultTrackContractHistoryIsFalse() {
        Project project = projectWithPlugin();

        assertThat(extension(project).getTrackContractHistory().get()).isFalse();
    }

    @Test
    @DisplayName("extension default: contractHistoryFile is mirage-api-detector-contract-history.ndjson in the project directory")
    void extensionDefaultContractHistoryFile() {
        Project project = projectWithPlugin();

        assertThat(extension(project).getContractHistoryFile().get().getAsFile())
                .isEqualTo(new File(project.getProjectDir(), "mirage-api-detector-contract-history.ndjson"));
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

        DetectMirageApisTask task = (DetectMirageApisTask)
                project.getTasks().getByName(MirageApiDetectorPlugin.TASK_NAME);
        assertThat(task.getTrackContractHistory().get()).isTrue();
        assertThat(task.getUpdateContractHistory().get()).isTrue();
        assertThat(task.getContractHistoryFile().get().getAsFile())
                .isEqualTo(new File(project.getProjectDir(), "mirage-api-detector-contract-history.ndjson"));
    }

    @Test
    @DisplayName("does not hook detectMirageApis into the check lifecycle task by default")
    void doesNotHookIntoCheckTaskByDefault() {
        Project project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build();
        project.getPluginManager().apply("java");
        project.getPluginManager().apply(MirageApiDetectorPlugin.class);

        assertThat(project.getTasks().getByName("check").getTaskDependencies().getDependencies(null))
                .noneMatch(t -> t.getName().equals(MirageApiDetectorPlugin.TASK_NAME));
    }

    @Test
    @DisplayName("can be wired into check explicitly via dependsOn")
    void canBeWiredIntoCheckExplicitly() {
        Project project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build();
        project.getPluginManager().apply("java");
        project.getPluginManager().apply(MirageApiDetectorPlugin.class);

        project.getTasks().named("check").configure(check -> check.dependsOn(MirageApiDetectorPlugin.TASK_NAME));

        assertThat(project.getTasks().getByName("check").getTaskDependencies().getDependencies(null))
                .anyMatch(t -> t.getName().equals(MirageApiDetectorPlugin.TASK_NAME));
    }

    @Test
    @DisplayName("registers the migrateContractHistory task when applied")
    void registersMigrateContractHistoryTask() {
        Project project = projectWithPlugin();

        assertThat(project.getTasks().findByName(MirageApiDetectorPlugin.MIGRATE_CONTRACT_HISTORY_TASK_NAME))
                .isNotNull();
    }

    @Test
    @DisplayName("migrateContractHistory defaults both controllerDirs and stubDirs regardless of scanMocks")
    void migrateContractHistoryDefaultsBothDirsRegardlessOfScanMocks() {
        Project project = projectWithPlugin();

        ((ProjectInternal) project).evaluate();

        MigrateContractHistoryTask task = (MigrateContractHistoryTask) project.getTasks()
                .getByName(MirageApiDetectorPlugin.MIGRATE_CONTRACT_HISTORY_TASK_NAME);
        assertThat(task.getControllerDirs().getFiles())
                .containsExactly(new File(project.getProjectDir(), "src/main/java"));
        assertThat(task.getStubDirs().getFiles())
                .containsExactly(new File(project.getProjectDir(), "src/test/resources/mappings"));
    }

    @Test
    @DisplayName("migrateContractHistory does not override controllerDirs/stubDirs configured explicitly by the user")
    void migrateContractHistoryDoesNotOverrideExplicitDirs() {
        Project project = projectWithPlugin();
        File customControllerDir = new File(project.getProjectDir(), "src/web/java");
        File customStubDir = new File(project.getProjectDir(), "src/test/resources/stubs");
        extension(project).getControllerDirs().from(customControllerDir);
        extension(project).getStubDirs().from(customStubDir);

        ((ProjectInternal) project).evaluate();

        MigrateContractHistoryTask task = (MigrateContractHistoryTask) project.getTasks()
                .getByName(MirageApiDetectorPlugin.MIGRATE_CONTRACT_HISTORY_TASK_NAME);
        assertThat(task.getControllerDirs().getFiles()).containsExactly(customControllerDir);
        assertThat(task.getStubDirs().getFiles()).containsExactly(customStubDir);
    }

    @Test
    @DisplayName("wires migrateContractHistory's contractHistoryFile from the extension")
    void wiresMigrateContractHistoryFileFromExtension() {
        Project project = projectWithPlugin();

        ((ProjectInternal) project).evaluate();

        MigrateContractHistoryTask task = (MigrateContractHistoryTask) project.getTasks()
                .getByName(MirageApiDetectorPlugin.MIGRATE_CONTRACT_HISTORY_TASK_NAME);
        assertThat(task.getContractHistoryFile().get().getAsFile())
                .isEqualTo(new File(project.getProjectDir(), "mirage-api-detector-contract-history.ndjson"));
    }

    @Test
    @DisplayName("the -PmirageApiDetector.updateContractHistory property accepts trimmed, case-insensitive booleans")
    void updateContractHistoryPropertyAcceptsTrimmedCaseInsensitiveBooleans() throws Exception {
        Files.writeString(tempDir.resolve("gradle.properties"), "mirageApiDetector.updateContractHistory=  TrUe  \n");
        Project project = projectWithPlugin();
        extension(project).getTrackContractHistory().set(false);
        extension(project).getUpdateContractHistory().set(false);

        ((ProjectInternal) project).evaluate();

        DetectMirageApisTask task = (DetectMirageApisTask)
                project.getTasks().getByName(MirageApiDetectorPlugin.TASK_NAME);
        assertThat(task.getUpdateContractHistory().get()).isTrue();
    }

    @Test
    @DisplayName("an invalid -PmirageApiDetector.updateContractHistory value throws a descriptive GradleException")
    void invalidUpdateContractHistoryPropertyThrowsDescriptiveError() throws Exception {
        Files.writeString(tempDir.resolve("gradle.properties"), "mirageApiDetector.updateContractHistory=maybe\n");
        Project project = projectWithPlugin();

        ((ProjectInternal) project).evaluate();
        DetectMirageApisTask task = (DetectMirageApisTask)
            project.getTasks().getByName(MirageApiDetectorPlugin.TASK_NAME);

        assertThatThrownBy(() -> task.getUpdateContractHistory().get())
            .isInstanceOf(RuntimeException.class)
            .hasRootCauseInstanceOf(org.gradle.api.GradleException.class)
            .hasRootCauseMessage("mirageApiDetector: invalid value 'maybe' for "
                        + "-PmirageApiDetector.updateContractHistory; expected 'true' or 'false'");
    }

    private Project projectWithPlugin() {
        Project project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build();
        project.getPluginManager().apply("java");
        project.getPluginManager().apply(MirageApiDetectorPlugin.class);
        return project;
    }

    private MirageApiDetectorExtension extension(Project project) {
        return project.getExtensions().getByType(MirageApiDetectorExtension.class);
    }
}

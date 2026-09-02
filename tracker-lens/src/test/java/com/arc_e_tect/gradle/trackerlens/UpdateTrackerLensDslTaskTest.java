package com.arc_e_tect.gradle.trackerlens;

import org.gradle.api.GradleException;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UpdateTrackerLensDslTask")
class UpdateTrackerLensDslTaskTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("updateDslShouldFailWhenTheBuildFileDoesNotExist")
    void updateDslShouldFailWhenTheBuildFileDoesNotExist() {
        UpdateTrackerLensDslTask task = newTask();
        task.getBuildFile().set(tempDir.resolve("build.gradle").toFile());

        assertThatThrownBy(task::updateDsl)
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("build file not found");
    }

    @Test
    @DisplayName("updateDslShouldLeaveAMissingBlockAloneWhenGenerateDslIsNotSet")
    void updateDslShouldLeaveAMissingBlockAloneWhenGenerateDslIsNotSet() throws Exception {
        String original = "plugins {\n    id 'com.arc-e-tect.tracker-lens'\n}\n";
        Path buildFile = writeBuildFile(original);
        UpdateTrackerLensDslTask task = newTask();
        task.getBuildFile().set(buildFile.toFile());

        task.updateDsl();

        assertThat(Files.readString(buildFile)).isEqualTo(original);
        assertThat(backupFileFor(buildFile)).doesNotExist();
    }

    @Test
    @DisplayName("updateDslShouldGenerateAFullBlockWhenGenerateDslIsSetAndNoneExists")
    void updateDslShouldGenerateAFullBlockWhenGenerateDslIsSetAndNoneExists() throws Exception {
        Path buildFile = writeBuildFile("plugins {\n    id 'com.arc-e-tect.tracker-lens'\n}\n");
        UpdateTrackerLensDslTask task = newTask();
        task.getBuildFile().set(buildFile.toFile());
        task.applyGenerateDsl(true);

        task.updateDsl();

        String updated = Files.readString(buildFile);
        assertThat(updated).contains("trackerLens {");
        assertThat(updated).contains("dashboardName = \"${project.name} Lens\"");
        assertThat(updated).contains("outputDir = layout.buildDirectory.dir('reports/tracker-lens')");
        assertThat(updated).contains("version = project.version.toString()");
        assertThat(updated).contains("trackers {");
    }

    @Test
    @DisplayName("updateDslShouldAddOnlyMissingScalarPropertiesToAnExistingBlock")
    void updateDslShouldAddOnlyMissingScalarPropertiesToAnExistingBlock() throws Exception {
        Path buildFile = writeBuildFile("trackerLens {\n    dashboardName = \"Checkout Service Lens\"\n}\n");
        UpdateTrackerLensDslTask task = newTask();
        task.getBuildFile().set(buildFile.toFile());

        task.updateDsl();

        String updated = Files.readString(buildFile);
        assertThat(updated).contains("dashboardName = \"Checkout Service Lens\"");
        assertThat(updated).contains("outputDir = layout.buildDirectory.dir('reports/tracker-lens')");
        assertThat(updated).contains("version = project.version.toString()");
        assertThat(updated).doesNotContain("register(");
    }

    @Test
    @DisplayName("updateDslShouldBeANoOpWhenTheBlockIsAlreadyFullyConfigured")
    void updateDslShouldBeANoOpWhenTheBlockIsAlreadyFullyConfigured() throws Exception {
        String original = "trackerLens {\n"
                + "    outputDir = layout.buildDirectory.dir('reports/tracker-lens')\n"
                + "    dashboardName = \"${project.name} Lens\"\n"
                + "    version = project.version.toString()\n"
                + "}\n";
        Path buildFile = writeBuildFile(original);
        UpdateTrackerLensDslTask task = newTask();
        task.getBuildFile().set(buildFile.toFile());

        task.updateDsl();

        assertThat(Files.readString(buildFile)).isEqualTo(original);
        assertThat(backupFileFor(buildFile)).doesNotExist();
    }

    @Test
    @DisplayName("updateDslShouldBackUpTheOriginalFileBeforeWritingChanges")
    void updateDslShouldBackUpTheOriginalFileBeforeWritingChanges() throws Exception {
        String original = "trackerLens {\n    dashboardName = \"Checkout Service Lens\"\n}\n";
        Path buildFile = writeBuildFile(original);
        UpdateTrackerLensDslTask task = newTask();
        task.getBuildFile().set(buildFile.toFile());

        task.updateDsl();

        Path backup = backupFileFor(buildFile);
        assertThat(backup).exists();
        assertThat(Files.readString(backup)).isEqualTo(original);
        // The live file itself was actually changed - otherwise the backup would be redundant.
        assertThat(Files.readString(buildFile)).isNotEqualTo(original);
    }

    @Test
    @DisplayName("updateDslShouldOverwriteAStaleBackupOnEachChangingRun")
    void updateDslShouldOverwriteAStaleBackupOnEachChangingRun() throws Exception {
        Path buildFile = writeBuildFile("trackerLens {\n    dashboardName = \"Checkout Service Lens\"\n}\n");
        UpdateTrackerLensDslTask firstRun = newTask();
        firstRun.getBuildFile().set(buildFile.toFile());
        firstRun.updateDsl();
        String afterFirstRun = Files.readString(buildFile);

        UpdateTrackerLensDslTask secondRun = newTask();
        secondRun.getBuildFile().set(buildFile.toFile());
        secondRun.applyCleanupDsl(true);
        secondRun.updateDsl();

        // The backup after the second run reflects the state right before *that* run, not the
        // very first original - each run's backup is a fallback for undoing just that run.
        assertThat(Files.readString(backupFileFor(buildFile))).isEqualTo(afterFirstRun);
    }

    @Test
    @DisplayName("updateDslWithCleanupDslShouldStripCommentsFromAnExistingBlock")
    void updateDslWithCleanupDslShouldStripCommentsFromAnExistingBlock() throws Exception {
        Path buildFile = writeBuildFile("trackerLens {\n"
                + "    // why this value was chosen\n"
                + "    dashboardName = \"Checkout Service Lens\"\n"
                + "}\n");
        UpdateTrackerLensDslTask task = newTask();
        task.getBuildFile().set(buildFile.toFile());
        task.applyCleanupDsl(true);

        task.updateDsl();

        String updated = Files.readString(buildFile);
        assertThat(updated).doesNotContain("//");
        assertThat(updated).contains("dashboardName = \"Checkout Service Lens\"");
    }

    private Path writeBuildFile(String content) throws Exception {
        Path buildFile = tempDir.resolve("build.gradle");
        Files.writeString(buildFile, content);
        return buildFile;
    }

    private Path backupFileFor(Path buildFile) {
        return buildFile.resolveSibling(buildFile.getFileName() + ".bak");
    }

    private UpdateTrackerLensDslTask newTask() {
        return ProjectBuilder.builder()
                .withProjectDir(tempDir.toFile())
                .build()
                .getTasks()
                .create("updateTrackerLensDSL", UpdateTrackerLensDslTask.class);
    }
}

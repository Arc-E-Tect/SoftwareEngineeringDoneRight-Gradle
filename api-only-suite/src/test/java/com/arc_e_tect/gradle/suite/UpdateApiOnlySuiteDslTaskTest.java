package com.arc_e_tect.gradle.suite;

import org.gradle.api.GradleException;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UpdateApiOnlySuiteDslTask")
class UpdateApiOnlySuiteDslTaskTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("updateDslShouldFailWhenTheBuildFileDoesNotExist")
    void updateDslShouldFailWhenTheBuildFileDoesNotExist() {
        UpdateApiOnlySuiteDslTask task = newTask();
        task.getBuildFile().set(tempDir.resolve("build.gradle").toFile());

        assertThatThrownBy(task::updateDsl)
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("build file not found");
    }

    @Test
    @DisplayName("updateDslShouldLeaveAMissingBlockAloneWhenGenerateDslIsNotSet")
    void updateDslShouldLeaveAMissingBlockAloneWhenGenerateDslIsNotSet() throws Exception {
        String original = "plugins {\n    id 'com.arc-e-tect.api-only-suite'\n}\n";
        Path buildFile = writeBuildFile(original);
        UpdateApiOnlySuiteDslTask task = newTask();
        task.getBuildFile().set(buildFile.toFile());

        task.updateDsl();

        assertThat(Files.readString(buildFile)).isEqualTo(original);
        assertThat(backupFileFor(buildFile)).doesNotExist();
    }

    @Test
    @DisplayName("updateDslShouldGenerateAFullBlockWhenGenerateDslIsSetAndNoneExists")
    void updateDslShouldGenerateAFullBlockWhenGenerateDslIsSetAndNoneExists() throws Exception {
        Path buildFile = writeBuildFile("plugins {\n    id 'com.arc-e-tect.api-only-suite'\n}\n");
        UpdateApiOnlySuiteDslTask task = newTask();
        task.getBuildFile().set(buildFile.toFile());
        task.applyGenerateDsl(true);

        task.updateDsl();

        String updated = Files.readString(buildFile);
        assertThat(updated).contains("apiOnlySuite {");
        assertThat(updated).contains("failOnDetection = false");
        assertThat(updated).contains("excludePaths = []");
        assertThat(updated).contains("excludeWellKnown = []");
    }

    @Test
    @DisplayName("updateDslShouldAddOnlyMissingScalarPropertiesToAnExistingBlock")
    void updateDslShouldAddOnlyMissingScalarPropertiesToAnExistingBlock() throws Exception {
        Path buildFile = writeBuildFile("apiOnlySuite {\n"
                + "    rootDocument = file('src/main/resources/openapi/openapi.yaml')\n"
                + "    failOnDetection = true\n"
                + "}\n");
        UpdateApiOnlySuiteDslTask task = newTask();
        task.getBuildFile().set(buildFile.toFile());

        task.updateDsl();

        String updated = Files.readString(buildFile);
        // Existing value, including the deliberately-non-default one, is never touched.
        assertThat(updated).contains("failOnDetection = true");
        assertThat(updated).contains("excludePaths = []");
        assertThat(updated).contains("excludeWellKnown = []");
    }

    @Test
    @DisplayName("updateDslShouldBeANoOpWhenTheBlockIsAlreadyFullyConfigured")
    void updateDslShouldBeANoOpWhenTheBlockIsAlreadyFullyConfigured() throws Exception {
        String original = "apiOnlySuite {\n"
                + "    failOnDetection = false\n"
                + "    excludePaths = []\n"
                + "    excludeWellKnown = []\n"
                + "}\n";
        Path buildFile = writeBuildFile(original);
        UpdateApiOnlySuiteDslTask task = newTask();
        task.getBuildFile().set(buildFile.toFile());

        task.updateDsl();

        assertThat(Files.readString(buildFile)).isEqualTo(original);
        assertThat(backupFileFor(buildFile)).doesNotExist();
    }

    @Test
    @DisplayName("updateDslShouldBackUpTheOriginalFileBeforeWritingChanges")
    void updateDslShouldBackUpTheOriginalFileBeforeWritingChanges() throws Exception {
        String original = "apiOnlySuite {\n    failOnDetection = true\n}\n";
        Path buildFile = writeBuildFile(original);
        UpdateApiOnlySuiteDslTask task = newTask();
        task.getBuildFile().set(buildFile.toFile());

        task.updateDsl();

        Path backup = backupFileFor(buildFile);
        assertThat(backup).exists();
        assertThat(Files.readString(backup)).isEqualTo(original);
        assertThat(Files.readString(buildFile)).isNotEqualTo(original);
    }

    @Test
    @DisplayName("updateDslWithCleanupDslShouldStripCommentsFromAnExistingBlock")
    void updateDslWithCleanupDslShouldStripCommentsFromAnExistingBlock() throws Exception {
        Path buildFile = writeBuildFile("apiOnlySuite {\n"
                + "    // why this value was chosen\n"
                + "    failOnDetection = true\n"
                + "}\n");
        UpdateApiOnlySuiteDslTask task = newTask();
        task.getBuildFile().set(buildFile.toFile());
        task.applyCleanupDsl(true);

        task.updateDsl();

        String updated = Files.readString(buildFile);
        assertThat(updated).doesNotContain("//");
        assertThat(updated).contains("failOnDetection = true");
    }

    private Path writeBuildFile(String content) throws Exception {
        Path buildFile = tempDir.resolve("build.gradle");
        Files.writeString(buildFile, content);
        return buildFile;
    }

    private Path backupFileFor(Path buildFile) {
        return buildFile.resolveSibling(buildFile.getFileName() + ".bak");
    }

    private UpdateApiOnlySuiteDslTask newTask() {
        return ProjectBuilder.builder()
                .withProjectDir(tempDir.toFile())
                .build()
                .getTasks()
                .create("updateApiOnlySuiteDSL", UpdateApiOnlySuiteDslTask.class);
    }
}

package com.arc_e_tect.gradle.mirage;

import org.gradle.api.GradleException;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UpdateMirageApiDetectorDslTask")
class UpdateMirageApiDetectorDslTaskTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("updateDslShouldFailWhenTheBuildFileDoesNotExist")
    void updateDslShouldFailWhenTheBuildFileDoesNotExist() {
        UpdateMirageApiDetectorDslTask task = newTask();
        task.getBuildFile().set(tempDir.resolve("build.gradle").toFile());

        assertThatThrownBy(task::updateDsl)
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("build file not found");
    }

    @Test
    @DisplayName("updateDslShouldLeaveAMissingBlockAloneWhenGenerateDslIsNotSet")
    void updateDslShouldLeaveAMissingBlockAloneWhenGenerateDslIsNotSet() throws Exception {
        String original = "plugins {\n    id 'com.arc-e-tect.mirage-api-detector'\n}\n";
        Path buildFile = writeBuildFile(original);
        UpdateMirageApiDetectorDslTask task = newTask();
        task.getBuildFile().set(buildFile.toFile());

        task.updateDsl();

        assertThat(Files.readString(buildFile)).isEqualTo(original);
        assertThat(backupFileFor(buildFile)).doesNotExist();
    }

    @Test
    @DisplayName("updateDslShouldGenerateAFullBlockWhenGenerateDslIsSetAndNoneExists")
    void updateDslShouldGenerateAFullBlockWhenGenerateDslIsSetAndNoneExists() throws Exception {
        Path buildFile = writeBuildFile("plugins {\n    id 'com.arc-e-tect.mirage-api-detector'\n}\n");
        UpdateMirageApiDetectorDslTask task = newTask();
        task.getBuildFile().set(buildFile.toFile());
        task.applyGenerateDsl(true);

        task.updateDsl();

        String updated = Files.readString(buildFile);
        assertThat(updated).contains("mirageApiDetector {");
        assertThat(updated).contains("failOnMirage = false");
        assertThat(updated).contains("scanMocks = false");
        assertThat(updated).contains("reportDir = layout.buildDirectory.dir('reports/mirage-api-detector')");
        assertThat(updated).contains("reportFileName = 'mirage-apis.adoc'");
        assertThat(updated).contains("systemUnderTestVersion = project.version.toString()");
        assertThat(updated).contains("openApiDir = layout.dir(rootDocument.map { it.asFile.parentFile })");
        assertThat(updated).contains("contractHistoryFile = file('mirage-api-detector-contract-history.ndjson')");
        assertThat(updated).contains("updateContractHistory = trackContractHistory");
        assertThat(updated).contains("excludePaths = []");
        assertThat(updated).contains("excludeWellKnown = []");
    }

    @Test
    @DisplayName("updateDslShouldAddOnlyMissingScalarPropertiesToAnExistingBlock")
    void updateDslShouldAddOnlyMissingScalarPropertiesToAnExistingBlock() throws Exception {
        Path buildFile = writeBuildFile("mirageApiDetector {\n"
                + "    rootDocument = file('src/main/resources/openapi/openapi.yaml')\n"
                + "    scanMocks = true\n"
                + "}\n");
        UpdateMirageApiDetectorDslTask task = newTask();
        task.getBuildFile().set(buildFile.toFile());

        task.updateDsl();

        String updated = Files.readString(buildFile);
        // Existing values, including the deliberately-non-default one, are never touched.
        assertThat(updated).contains("scanMocks = true");
        assertThat(updated).contains("reportDir = layout.buildDirectory.dir('reports/mirage-api-detector')");
        assertThat(updated).contains("systemUnderTestVersion = project.version.toString()");
    }

    @Test
    @DisplayName("updateDslShouldBeANoOpWhenTheBlockIsAlreadyFullyConfigured")
    void updateDslShouldBeANoOpWhenTheBlockIsAlreadyFullyConfigured() throws Exception {
        String original = "mirageApiDetector {\n"
                + "    rootDocument = file('src/main/resources/openapi/openapi.yaml')\n"
                + "    failOnMirage = false\n"
                + "    scanMocks = false\n"
                + "    reportDir = layout.buildDirectory.dir('reports/mirage-api-detector')\n"
                + "    reportFileName = 'mirage-apis.adoc'\n"
                + "    systemUnderTestVersion = project.version.toString()\n"
                + "    openApiDir = layout.dir(rootDocument.map { it.asFile.parentFile })\n"
                + "    trackContractHistory = false\n"
                + "    contractHistoryFile = file('mirage-api-detector-contract-history.ndjson')\n"
                + "    updateContractHistory = trackContractHistory\n"
                + "    excludePaths = []\n"
                + "    excludeWellKnown = []\n"
                + "}\n";
        Path buildFile = writeBuildFile(original);
        UpdateMirageApiDetectorDslTask task = newTask();
        task.getBuildFile().set(buildFile.toFile());

        task.updateDsl();

        assertThat(Files.readString(buildFile)).isEqualTo(original);
        assertThat(backupFileFor(buildFile)).doesNotExist();
    }

    @Test
    @DisplayName("updateDslShouldBackUpTheOriginalFileBeforeWritingChanges")
    void updateDslShouldBackUpTheOriginalFileBeforeWritingChanges() throws Exception {
        String original = "mirageApiDetector {\n    scanMocks = true\n}\n";
        Path buildFile = writeBuildFile(original);
        UpdateMirageApiDetectorDslTask task = newTask();
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
        Path buildFile = writeBuildFile("mirageApiDetector {\n"
                + "    // why this value was chosen\n"
                + "    scanMocks = true\n"
                + "}\n");
        UpdateMirageApiDetectorDslTask task = newTask();
        task.getBuildFile().set(buildFile.toFile());
        task.applyCleanupDsl(true);

        task.updateDsl();

        String updated = Files.readString(buildFile);
        assertThat(updated).doesNotContain("//");
        assertThat(updated).contains("scanMocks = true");
    }

    private Path writeBuildFile(String content) throws Exception {
        Path buildFile = tempDir.resolve("build.gradle");
        Files.writeString(buildFile, content);
        return buildFile;
    }

    private Path backupFileFor(Path buildFile) {
        return buildFile.resolveSibling(buildFile.getFileName() + ".bak");
    }

    private UpdateMirageApiDetectorDslTask newTask() {
        return ProjectBuilder.builder()
                .withProjectDir(tempDir.toFile())
                .build()
                .getTasks()
                .create("updateMirageApiDetectorDSL", UpdateMirageApiDetectorDslTask.class);
    }
}

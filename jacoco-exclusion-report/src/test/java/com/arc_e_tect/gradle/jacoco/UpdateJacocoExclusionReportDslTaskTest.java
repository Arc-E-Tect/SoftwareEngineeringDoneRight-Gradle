package com.arc_e_tect.gradle.jacoco;

import org.gradle.api.GradleException;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UpdateJacocoExclusionReportDslTask")
class UpdateJacocoExclusionReportDslTaskTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("updateDslShouldFailWhenTheBuildFileDoesNotExist")
    void updateDslShouldFailWhenTheBuildFileDoesNotExist() {
        UpdateJacocoExclusionReportDslTask task = newTask();
        task.getBuildFile().set(tempDir.resolve("build.gradle").toFile());

        assertThatThrownBy(task::updateDsl)
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("build file not found");
    }

    @Test
    @DisplayName("updateDslShouldLeaveAMissingBlockAloneWhenGenerateDslIsNotSet")
    void updateDslShouldLeaveAMissingBlockAloneWhenGenerateDslIsNotSet() throws Exception {
        String original = "plugins {\n    id 'com.arc-e-tect.jacoco-exclusion-report'\n}\n";
        Path buildFile = writeBuildFile(original);
        UpdateJacocoExclusionReportDslTask task = newTask();
        task.getBuildFile().set(buildFile.toFile());

        task.updateDsl();

        assertThat(Files.readString(buildFile)).isEqualTo(original);
        assertThat(backupFileFor(buildFile)).doesNotExist();
    }

    @Test
    @DisplayName("updateDslShouldGenerateAFullBlockWhenGenerateDslIsSetAndNoneExists")
    void updateDslShouldGenerateAFullBlockWhenGenerateDslIsSetAndNoneExists() throws Exception {
        Path buildFile = writeBuildFile("plugins {\n    id 'com.arc-e-tect.jacoco-exclusion-report'\n}\n");
        UpdateJacocoExclusionReportDslTask task = newTask();
        task.getBuildFile().set(buildFile.toFile());
        task.applyGenerateDsl(true);

        task.updateDsl();

        String updated = Files.readString(buildFile);
        assertThat(updated).contains("jacocoExclusionReport {");
        assertThat(updated).contains("annotationName = 'ExcludeFromJacocoGeneratedCodeCoverage'");
        assertThat(updated).contains("reportDir = layout.buildDirectory.dir('reports/jacoco-exclusions')");
        assertThat(updated).contains("includeConfiguredExclusions = true");
        assertThat(updated).contains("includeGeneratedAnnotationExclusions = false");
    }

    @Test
    @DisplayName("updateDslShouldAddOnlyMissingScalarPropertiesToAnExistingBlock")
    void updateDslShouldAddOnlyMissingScalarPropertiesToAnExistingBlock() throws Exception {
        Path buildFile = writeBuildFile("jacocoExclusionReport {\n"
                + "    includeGeneratedAnnotationExclusions = true\n"
                + "}\n");
        UpdateJacocoExclusionReportDslTask task = newTask();
        task.getBuildFile().set(buildFile.toFile());

        task.updateDsl();

        String updated = Files.readString(buildFile);
        // Existing value, including the deliberately-non-default one, is never touched.
        assertThat(updated).contains("includeGeneratedAnnotationExclusions = true");
        assertThat(updated).contains("annotationName = 'ExcludeFromJacocoGeneratedCodeCoverage'");
        assertThat(updated).contains("reportDir = layout.buildDirectory.dir('reports/jacoco-exclusions')");
        assertThat(updated).contains("includeConfiguredExclusions = true");
    }

    @Test
    @DisplayName("updateDslShouldBeANoOpWhenTheBlockIsAlreadyFullyConfigured")
    void updateDslShouldBeANoOpWhenTheBlockIsAlreadyFullyConfigured() throws Exception {
        String original = "jacocoExclusionReport {\n"
                + "    annotationName = 'ExcludeFromJacocoGeneratedCodeCoverage'\n"
                + "    reportDir = layout.buildDirectory.dir('reports/jacoco-exclusions')\n"
                + "    includeConfiguredExclusions = true\n"
                + "    includeGeneratedAnnotationExclusions = false\n"
                + "}\n";
        Path buildFile = writeBuildFile(original);
        UpdateJacocoExclusionReportDslTask task = newTask();
        task.getBuildFile().set(buildFile.toFile());

        task.updateDsl();

        assertThat(Files.readString(buildFile)).isEqualTo(original);
        assertThat(backupFileFor(buildFile)).doesNotExist();
    }

    @Test
    @DisplayName("updateDslShouldBackUpTheOriginalFileBeforeWritingChanges")
    void updateDslShouldBackUpTheOriginalFileBeforeWritingChanges() throws Exception {
        String original = "jacocoExclusionReport {\n    includeGeneratedAnnotationExclusions = true\n}\n";
        Path buildFile = writeBuildFile(original);
        UpdateJacocoExclusionReportDslTask task = newTask();
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
        Path buildFile = writeBuildFile("jacocoExclusionReport {\n"
                + "    // why this value was chosen\n"
                + "    includeGeneratedAnnotationExclusions = true\n"
                + "}\n");
        UpdateJacocoExclusionReportDslTask task = newTask();
        task.getBuildFile().set(buildFile.toFile());
        task.applyCleanupDsl(true);

        task.updateDsl();

        String updated = Files.readString(buildFile);
        assertThat(updated).doesNotContain("//");
        assertThat(updated).contains("includeGeneratedAnnotationExclusions = true");
    }

    private Path writeBuildFile(String content) throws Exception {
        Path buildFile = tempDir.resolve("build.gradle");
        Files.writeString(buildFile, content);
        return buildFile;
    }

    private Path backupFileFor(Path buildFile) {
        return buildFile.resolveSibling(buildFile.getFileName() + ".bak");
    }

    private UpdateJacocoExclusionReportDslTask newTask() {
        return ProjectBuilder.builder()
                .withProjectDir(tempDir.toFile())
                .build()
                .getTasks()
                .create("updateJacocoExclusionReportDSL", UpdateJacocoExclusionReportDslTask.class);
    }
}

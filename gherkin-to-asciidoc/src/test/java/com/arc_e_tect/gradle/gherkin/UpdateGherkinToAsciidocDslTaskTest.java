package com.arc_e_tect.gradle.gherkin;

import org.gradle.api.GradleException;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UpdateGherkinToAsciidocDslTask")
class UpdateGherkinToAsciidocDslTaskTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("updateDslShouldFailWhenTheBuildFileDoesNotExist")
    void updateDslShouldFailWhenTheBuildFileDoesNotExist() {
        UpdateGherkinToAsciidocDslTask task = newTask();
        task.getBuildFile().set(tempDir.resolve("build.gradle").toFile());

        assertThatThrownBy(task::updateDsl)
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("build file not found");
    }

    @Test
    @DisplayName("updateDslShouldLeaveAMissingBlockAloneWhenGenerateDslIsNotSet")
    void updateDslShouldLeaveAMissingBlockAloneWhenGenerateDslIsNotSet() throws Exception {
        String original = "plugins {\n    id 'com.arc-e-tect.gherkin-to-asciidoc'\n}\n";
        Path buildFile = writeBuildFile(original);
        UpdateGherkinToAsciidocDslTask task = newTask();
        task.getBuildFile().set(buildFile.toFile());

        task.updateDsl();

        assertThat(Files.readString(buildFile)).isEqualTo(original);
        assertThat(backupFileFor(buildFile)).doesNotExist();
    }

    @Test
    @DisplayName("updateDslShouldGenerateAFullBlockWhenGenerateDslIsSetAndNoneExists")
    void updateDslShouldGenerateAFullBlockWhenGenerateDslIsSetAndNoneExists() throws Exception {
        Path buildFile = writeBuildFile("plugins {\n    id 'com.arc-e-tect.gherkin-to-asciidoc'\n}\n");
        UpdateGherkinToAsciidocDslTask task = newTask();
        task.getBuildFile().set(buildFile.toFile());
        task.applyGenerateDsl(true);

        task.updateDsl();

        String updated = Files.readString(buildFile);
        assertThat(updated).contains("gherkinToAsciidoc {");
        assertThat(updated).contains("outputDir = layout.buildDirectory.dir('generated-docs')");
        assertThat(updated).contains("snippetDir = layout.buildDirectory.dir('generated-docs/features/snippets')");
        assertThat(updated).contains("progressHistoryFile = layout.projectDirectory.file('gherkin-progress-history.ndjson')");
        assertThat(updated).contains("updateProgressHistory = trackProgressHistory");
    }

    @Test
    @DisplayName("updateDslShouldAddOnlyMissingScalarPropertiesToAnExistingBlock")
    void updateDslShouldAddOnlyMissingScalarPropertiesToAnExistingBlock() throws Exception {
        Path buildFile = writeBuildFile("gherkinToAsciidoc {\n"
                + "    trackProgress = true\n"
                + "}\n");
        UpdateGherkinToAsciidocDslTask task = newTask();
        task.getBuildFile().set(buildFile.toFile());

        task.updateDsl();

        String updated = Files.readString(buildFile);
        // Existing value is never touched.
        assertThat(updated).contains("trackProgress = true");
        assertThat(updated).contains("outputDir = layout.buildDirectory.dir('generated-docs')");
        assertThat(updated).contains("updateProgressHistory = trackProgressHistory");
    }

    @Test
    @DisplayName("updateDslShouldBeANoOpWhenTheBlockIsAlreadyFullyConfigured")
    void updateDslShouldBeANoOpWhenTheBlockIsAlreadyFullyConfigured() throws Exception {
        String original = "gherkinToAsciidoc {\n"
                + "    outputDir = layout.buildDirectory.dir('generated-docs')\n"
                + "    snippetDir = layout.buildDirectory.dir('generated-docs/features/snippets')\n"
                + "    progressHistoryFile = layout.projectDirectory.file('gherkin-progress-history.ndjson')\n"
                + "    updateProgressHistory = trackProgressHistory\n"
                + "}\n";
        Path buildFile = writeBuildFile(original);
        UpdateGherkinToAsciidocDslTask task = newTask();
        task.getBuildFile().set(buildFile.toFile());

        task.updateDsl();

        assertThat(Files.readString(buildFile)).isEqualTo(original);
        assertThat(backupFileFor(buildFile)).doesNotExist();
    }

    @Test
    @DisplayName("updateDslShouldBackUpTheOriginalFileBeforeWritingChanges")
    void updateDslShouldBackUpTheOriginalFileBeforeWritingChanges() throws Exception {
        String original = "gherkinToAsciidoc {\n    trackProgress = true\n}\n";
        Path buildFile = writeBuildFile(original);
        UpdateGherkinToAsciidocDslTask task = newTask();
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
        Path buildFile = writeBuildFile("gherkinToAsciidoc {\n"
                + "    // why this value was chosen\n"
                + "    trackProgress = true\n"
                + "}\n");
        UpdateGherkinToAsciidocDslTask task = newTask();
        task.getBuildFile().set(buildFile.toFile());
        task.applyCleanupDsl(true);

        task.updateDsl();

        String updated = Files.readString(buildFile);
        assertThat(updated).doesNotContain("//");
        assertThat(updated).contains("trackProgress = true");
    }

    private Path writeBuildFile(String content) throws Exception {
        Path buildFile = tempDir.resolve("build.gradle");
        Files.writeString(buildFile, content);
        return buildFile;
    }

    private Path backupFileFor(Path buildFile) {
        return buildFile.resolveSibling(buildFile.getFileName() + ".bak");
    }

    private UpdateGherkinToAsciidocDslTask newTask() {
        return ProjectBuilder.builder()
                .withProjectDir(tempDir.toFile())
                .build()
                .getTasks()
                .create("updateGherkinToAsciidocDSL", UpdateGherkinToAsciidocDslTask.class);
    }
}

package com.arc_e_tect.gradle.trackerlens;

import org.gradle.api.GradleException;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("BootstrapTrackerLensProjectTask")
class BootstrapTrackerLensProjectTaskTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("bootstrapShouldWriteALensPackProjectAndASampleProject")
    void bootstrapShouldWriteALensPackProjectAndASampleProject() {
        BootstrapTrackerLensProjectTask task = newTask();
        File outputDir = tempDir.resolve("bootstrap").toFile();
        task.getOutputDir().set(outputDir);

        task.bootstrap();

        assertThat(outputDir.toPath()).satisfies(root -> {
            assertThat(root.resolve("README.adoc")).exists();
            assertThat(root.resolve("lens-pack/settings.gradle")).exists();
            assertThat(root.resolve("lens-pack/build.gradle")).exists();
            assertThat(root.resolve("lens-pack/README.adoc")).exists();
            assertThat(root.resolve("lens-pack/src/main/resources/META-INF/arc-e-tect/tracker-lens/lenses/my-lens.css")).exists();
            assertThat(root.resolve("sample-project/settings.gradle")).exists();
            assertThat(root.resolve("sample-project/build.gradle")).exists();
            assertThat(root.resolve("sample-project/README.adoc")).exists();
            // No history file is written here: sample-project/build.gradle wires
            // generateTrackerLensDashboard to depend on generateTrackerLensFixture, so the sample
            // project generates its own calibrated, dated-to-today fixture on its first build.
            assertThat(root.resolve("sample-project/src/main/resources/gherkin-progress-history.ndjson")).doesNotExist();
        });
    }

    @Test
    @DisplayName("bootstrapShouldPinTheCurrentPluginVersionInTheSampleProjectBuildFile")
    void bootstrapShouldPinTheCurrentPluginVersionInTheSampleProjectBuildFile() throws Exception {
        BootstrapTrackerLensProjectTask task = newTask();
        File outputDir = tempDir.resolve("bootstrap").toFile();
        task.getOutputDir().set(outputDir);

        task.bootstrap();

        String sampleBuildGradle = Files.readString(outputDir.toPath().resolve("sample-project/build.gradle"));
        assertThat(sampleBuildGradle)
                .contains("id 'com.arc-e-tect.tracker-lens' version '" + PluginMetadata.pluginVersion() + "'");
    }

    @Test
    @DisplayName("bootstrapShouldWriteTheSameBoilerplateLensAsInitTrackerLens")
    void bootstrapShouldWriteTheSameBoilerplateLensAsInitTrackerLens() throws Exception {
        BootstrapTrackerLensProjectTask task = newTask();
        File outputDir = tempDir.resolve("bootstrap").toFile();
        task.getOutputDir().set(outputDir);

        task.bootstrap();

        Path generatedLens = outputDir.toPath()
                .resolve("lens-pack/src/main/resources/META-INF/arc-e-tect/tracker-lens/lenses/my-lens.css");
        try (var expected = getClass().getClassLoader().getResourceAsStream(InitTrackerLensTask.BOILERPLATE_RESOURCE)) {
            assertThat(Files.readAllBytes(generatedLens)).isEqualTo(expected.readAllBytes());
        }
    }

    @Test
    @DisplayName("bootstrapShouldFailWhenOutputDirAlreadyExistsAndIsNotEmpty")
    void bootstrapShouldFailWhenOutputDirAlreadyExistsAndIsNotEmpty() throws Exception {
        BootstrapTrackerLensProjectTask task = newTask();
        Path outputDir = tempDir.resolve("bootstrap");
        Files.createDirectories(outputDir);
        Files.writeString(outputDir.resolve("already-here.txt"), "user work");
        task.getOutputDir().set(outputDir.toFile());

        assertThatThrownBy(task::bootstrap)
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("already exists");
    }

    private BootstrapTrackerLensProjectTask newTask() {
        return ProjectBuilder.builder()
                .withProjectDir(tempDir.toFile())
                .build()
                .getTasks()
                .create("bootstrapTrackerLensProject", BootstrapTrackerLensProjectTask.class);
    }
}

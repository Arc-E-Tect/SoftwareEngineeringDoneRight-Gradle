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

@DisplayName("InitTrackerLensTask")
class InitTrackerLensTaskTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("initShouldWriteTheBundledBoilerplateToTheConfiguredLensFile")
    void initShouldWriteTheBundledBoilerplateToTheConfiguredLensFile() throws Exception {
        InitTrackerLensTask task = newTask();
        Path target = tempDir.resolve("lenses/my-lens.css");
        task.getLensFile().set(target.toFile());

        task.init();

        assertThat(Files.readString(target)).contains("--dashboard-stage-1");
    }

    @Test
    @DisplayName("initShouldCoverEveryContractSelectorWithInlineExplanation")
    void initShouldCoverEveryContractSelectorWithInlineExplanation() throws Exception {
        InitTrackerLensTask task = newTask();
        Path target = tempDir.resolve("lenses/my-lens.css");
        task.getLensFile().set(target.toFile());

        task.init();

        String css = Files.readString(target);
        assertThat(css).contains(".dashboard", ".tracker", ".metric-card", ".projection",
                ".chart", ".stale-items__table", ".lens-switcher");
    }

    @Test
    @DisplayName("initShouldFailWhenTheLensFileAlreadyExists")
    void initShouldFailWhenTheLensFileAlreadyExists() throws Exception {
        InitTrackerLensTask task = newTask();
        Path target = tempDir.resolve("lenses/my-lens.css");
        Files.createDirectories(target.getParent());
        Files.writeString(target, "/* already customized by the user */");
        task.getLensFile().set(target.toFile());

        assertThatThrownBy(task::init)
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("already exists");
        assertThat(Files.readString(target)).isEqualTo("/* already customized by the user */");
    }

    private InitTrackerLensTask newTask() {
        return ProjectBuilder.builder()
                .withProjectDir(tempDir.toFile())
                .build()
                .getTasks()
                .create("initTrackerLens", InitTrackerLensTask.class);
    }
}

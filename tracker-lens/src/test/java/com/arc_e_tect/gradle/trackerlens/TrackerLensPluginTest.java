package com.arc_e_tect.gradle.trackerlens;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TrackerLensPlugin")
class TrackerLensPluginTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("applyShouldRegisterTheTrackerLensExtension")
    void applyShouldRegisterTheTrackerLensExtension() {
        Project project = newProject();

        assertThat(project.getExtensions().findByType(TrackerLensExtension.class)).isNotNull();
    }

    @Test
    @DisplayName("applyShouldRegisterTheGenerateTrackerLensDashboardTask")
    void applyShouldRegisterTheGenerateTrackerLensDashboardTask() {
        Project project = newProject();

        assertThat(project.getTasks().findByName(TrackerLensPlugin.TASK_NAME)).isNotNull();
    }

    @Test
    @DisplayName("applyShouldRegisterTheListTrackerLensStylesTaskWithoutRequiringAnyTracker")
    void applyShouldRegisterTheListTrackerLensStylesTaskWithoutRequiringAnyTracker() {
        // Deliberately does not evaluate the project or register a tracker: listTrackerLensStyles
        // must be usable even when no tracker has been configured yet, since it only touches lens
        // resolution.
        Project project = newProject();

        assertThat(project.getTasks().findByName(TrackerLensPlugin.LIST_STYLES_TASK_NAME)).isNotNull();
    }

    @Test
    @DisplayName("applyShouldRegisterTheLensStyleConfiguration")
    void applyShouldRegisterTheLensStyleConfiguration() {
        Project project = newProject();

        Configuration lensStyle = project.getConfigurations().findByName(TrackerLensExtension.LENS_STYLE_CONFIGURATION_NAME);
        assertThat(lensStyle).isNotNull();
    }

    @Test
    @DisplayName("evaluateShouldFailWhenNoTrackerIsRegistered")
    void evaluateShouldFailWhenNoTrackerIsRegistered() {
        Project project = newProject();

        assertThatThrownBy(() -> ((ProjectInternal) project).evaluate())
                .rootCause()
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("at least one tracker must be registered");
    }

    @Test
    @DisplayName("evaluateShouldSucceedWhenAtLeastOneTrackerIsRegistered")
    void evaluateShouldSucceedWhenAtLeastOneTrackerIsRegistered() {
        Project project = newProject();
        TrackerLensExtension extension = project.getExtensions().getByType(TrackerLensExtension.class);
        extension.trackers(trackers -> trackers.register("bdd-scenarios", registration -> {
            registration.getHistoryFiles().from(tempDir.resolve("history.ndjson").toFile());
            registration.getSource().set(TrackerSourceKind.GHERKIN_SCENARIO);
        }));

        ((ProjectInternal) project).evaluate();

        assertThat(project.getTasks().findByName(TrackerLensPlugin.TASK_NAME)).isNotNull();
    }

    @Test
    @DisplayName("evaluateShouldSucceedWhenTrackerIsRegisteredWithMultipleHistoryFiles")
    void evaluateShouldSucceedWhenTrackerIsRegisteredWithMultipleHistoryFiles() {
        Project project = newProject();
        TrackerLensExtension extension = project.getExtensions().getByType(TrackerLensExtension.class);
        extension.trackers(trackers -> trackers.register("api-contracts", registration -> {
            registration.getHistoryFiles().from(
                    tempDir.resolve("shadow.ndjson").toFile(), tempDir.resolve("mirage.ndjson").toFile());
            registration.getSource().set(TrackerSourceKind.API_CONTRACT);
        }));

        ((ProjectInternal) project).evaluate();

        assertThat(project.getTasks().findByName(TrackerLensPlugin.TASK_NAME)).isNotNull();
    }

    @Test
    @DisplayName("evaluateShouldFailWhenTrackerHasNoHistoryFilesConfigured")
    void evaluateShouldFailWhenTrackerHasNoHistoryFilesConfigured() {
        Project project = newProject();
        TrackerLensExtension extension = project.getExtensions().getByType(TrackerLensExtension.class);
        extension.trackers(trackers -> trackers.register("bdd-scenarios", registration ->
                registration.getSource().set(TrackerSourceKind.GHERKIN_SCENARIO)));

        assertThatThrownBy(() -> ((ProjectInternal) project).evaluate())
                .rootCause()
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("no historyFiles configured");
    }

    private Project newProject() {
        Project project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build();
        project.getPluginManager().apply(TrackerLensPlugin.class);
        return project;
    }
}

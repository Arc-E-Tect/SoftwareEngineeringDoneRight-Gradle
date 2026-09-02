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
    @DisplayName("applyShouldRegisterTheListTrackerLensStylesTaskAndSurviveEvaluationWithoutAnyTracker")
    void applyShouldRegisterTheListTrackerLensStylesTaskAndSurviveEvaluationWithoutAnyTracker() {
        // listTrackerLensStyles must stay usable even when no tracker has been configured yet,
        // since it only touches lens resolution - proven here by evaluating the project (not just
        // checking the task is registered), since project evaluation is exactly what a real build
        // was found to fail at (see GenerateTrackerLensTaskTest for where the requirement moved).
        Project project = newProject();

        ((ProjectInternal) project).evaluate();

        assertThat(project.getTasks().findByName(TrackerLensPlugin.LIST_STYLES_TASK_NAME)).isNotNull();
    }

    @Test
    @DisplayName("applyShouldRegisterTheListTrackerLensTemplatesTaskAndSurviveEvaluationWithoutAnyTracker")
    void applyShouldRegisterTheListTrackerLensTemplatesTaskAndSurviveEvaluationWithoutAnyTracker() {
        // Same reasoning as listTrackerLensStyles above - listTrackerLensTemplates only touches
        // template resolution, so it must stay usable even when no tracker has been configured yet.
        Project project = newProject();

        ((ProjectInternal) project).evaluate();

        assertThat(project.getTasks().findByName(TrackerLensPlugin.LIST_TEMPLATES_TASK_NAME)).isNotNull();
    }

    @Test
    @DisplayName("applyShouldRegisterTheInitTrackerLensTaskAndSurviveEvaluationWithoutAnyTracker")
    void applyShouldRegisterTheInitTrackerLensTaskAndSurviveEvaluationWithoutAnyTracker() {
        Project project = newProject();

        ((ProjectInternal) project).evaluate();

        assertThat(project.getTasks().findByName(TrackerLensPlugin.INIT_LENS_TASK_NAME)).isNotNull();
    }

    @Test
    @DisplayName("applyShouldRegisterTheBootstrapTrackerLensProjectTaskAndSurviveEvaluationWithoutAnyTracker")
    void applyShouldRegisterTheBootstrapTrackerLensProjectTaskAndSurviveEvaluationWithoutAnyTracker() {
        Project project = newProject();

        ((ProjectInternal) project).evaluate();

        assertThat(project.getTasks().findByName(TrackerLensPlugin.BOOTSTRAP_PROJECT_TASK_NAME)).isNotNull();
    }

    @Test
    @DisplayName("applyShouldRegisterTheGenerateTrackerLensFixtureTaskAndSurviveEvaluationWithoutAnyTracker")
    void applyShouldRegisterTheGenerateTrackerLensFixtureTaskAndSurviveEvaluationWithoutAnyTracker() {
        // generateTrackerLensFixture needs no tracker to be registered - it writes history files,
        // it doesn't read them.
        Project project = newProject();

        ((ProjectInternal) project).evaluate();

        assertThat(project.getTasks().findByName(TrackerLensPlugin.GENERATE_FIXTURE_TASK_NAME)).isNotNull();
    }

    @Test
    @DisplayName("applyShouldDefaultFixtureOutputFilesToTheProjectDirectory")
    void applyShouldDefaultFixtureOutputFilesToTheProjectDirectory() {
        Project project = newProject();

        com.arc_e_tect.gradle.trackerlens.fixture.GenerateTrackerLensFixtureTask task =
                (com.arc_e_tect.gradle.trackerlens.fixture.GenerateTrackerLensFixtureTask)
                        project.getTasks().getByName(TrackerLensPlugin.GENERATE_FIXTURE_TASK_NAME);

        assertThat(task.getBddScenarioHistoryFile().get().getAsFile())
                .isEqualTo(new java.io.File(project.getProjectDir(), "gherkin-progress-history.ndjson"));
        assertThat(task.getApiContractHistoryFile().get().getAsFile())
                .isEqualTo(new java.io.File(project.getProjectDir(), "api-contract-progress.ndjson"));
    }

    @Test
    @DisplayName("applyShouldRegisterTheUpdateDslTaskAndSurviveEvaluationWithoutAnyTracker")
    void applyShouldRegisterTheUpdateDslTaskAndSurviveEvaluationWithoutAnyTracker() {
        Project project = newProject();

        ((ProjectInternal) project).evaluate();

        assertThat(project.getTasks().findByName(TrackerLensPlugin.UPDATE_DSL_TASK_NAME)).isNotNull();
    }

    @Test
    @DisplayName("applyShouldDefaultUpdateDslTaskBuildFileToTheProjectsOwnBuildFile")
    void applyShouldDefaultUpdateDslTaskBuildFileToTheProjectsOwnBuildFile() {
        Project project = newProject();

        UpdateTrackerLensDslTask task =
                (UpdateTrackerLensDslTask) project.getTasks().getByName(TrackerLensPlugin.UPDATE_DSL_TASK_NAME);

        assertThat(task.getBuildFile().get().getAsFile()).isEqualTo(project.getBuildFile());
    }

    @Test
    @DisplayName("applyShouldRegisterTheLensStyleConfiguration")
    void applyShouldRegisterTheLensStyleConfiguration() {
        Project project = newProject();

        Configuration lensStyle = project.getConfigurations().findByName(TrackerLensExtension.LENS_STYLE_CONFIGURATION_NAME);
        assertThat(lensStyle).isNotNull();
    }

    @Test
    @DisplayName("evaluateShouldSucceedWhenNoTrackerIsRegistered")
    void evaluateShouldSucceedWhenNoTrackerIsRegistered() {
        // The "at least one tracker" requirement is enforced by GenerateTrackerLensTask itself at
        // task-execution time (see GenerateTrackerLensTaskTest), not during project configuration -
        // configuration must always succeed regardless, so that listTrackerLensStyles,
        // initTrackerLens, and bootstrapTrackerLensProject stay usable in a project that hasn't
        // registered a tracker yet.
        Project project = newProject();

        ((ProjectInternal) project).evaluate();

        assertThat(project.getTasks().findByName(TrackerLensPlugin.TASK_NAME)).isNotNull();
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
    @DisplayName("applyShouldDefaultDashboardNameToProjectNameSuffixedWithLens")
    void applyShouldDefaultDashboardNameToProjectNameSuffixedWithLens() {
        Project project = ProjectBuilder.builder().withName("checkout-service").withProjectDir(tempDir.toFile()).build();
        project.getPluginManager().apply(TrackerLensPlugin.class);
        TrackerLensExtension extension = project.getExtensions().getByType(TrackerLensExtension.class);

        assertThat(extension.getDashboardName().get()).isEqualTo("checkout-service Lens");
    }

    @Test
    @DisplayName("applyShouldDefaultVersionToProjectVersion")
    void applyShouldDefaultVersionToProjectVersion() {
        Project project = newProject();
        project.setVersion("2.3.0");
        TrackerLensExtension extension = project.getExtensions().getByType(TrackerLensExtension.class);

        assertThat(extension.getVersion().get()).isEqualTo("2.3.0");
    }

    @Test
    @DisplayName("evaluateShouldPassDashboardNameAndVersionThroughToTheGenerateTask")
    void evaluateShouldPassDashboardNameAndVersionThroughToTheGenerateTask() {
        Project project = newProject();
        TrackerLensExtension extension = project.getExtensions().getByType(TrackerLensExtension.class);
        extension.getDashboardName().set("Checkout Service Lens");
        extension.getVersion().set("9.9.9");
        extension.trackers(trackers -> trackers.register("bdd-scenarios", registration -> {
            registration.getHistoryFiles().from(tempDir.resolve("history.ndjson").toFile());
            registration.getSource().set(TrackerSourceKind.GHERKIN_SCENARIO);
        }));

        ((ProjectInternal) project).evaluate();

        GenerateTrackerLensTask task =
                (GenerateTrackerLensTask) project.getTasks().getByName(TrackerLensPlugin.TASK_NAME);
        assertThat(task.getDashboardName().get()).isEqualTo("Checkout Service Lens");
        assertThat(task.getVersion().get()).isEqualTo("9.9.9");
    }

    @Test
    @DisplayName("evaluateShouldPassTemplateIdThroughToTheGenerateTask")
    void evaluateShouldPassTemplateIdThroughToTheGenerateTask() {
        Project project = newProject();
        TrackerLensExtension extension = project.getExtensions().getByType(TrackerLensExtension.class);
        extension.getTemplateId().set("venn-diagram-view");
        extension.trackers(trackers -> trackers.register("bdd-scenarios", registration -> {
            registration.getHistoryFiles().from(tempDir.resolve("history.ndjson").toFile());
            registration.getSource().set(TrackerSourceKind.GHERKIN_SCENARIO);
        }));

        ((ProjectInternal) project).evaluate();

        GenerateTrackerLensTask task =
                (GenerateTrackerLensTask) project.getTasks().getByName(TrackerLensPlugin.TASK_NAME);
        assertThat(task.getTemplateId().get()).isEqualTo("venn-diagram-view");
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

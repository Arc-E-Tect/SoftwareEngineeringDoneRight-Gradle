package com.arc_e_tect.gradle.trackerlens;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the plugin through a real Gradle build via TestKit, rather than {@code ProjectBuilder}.
 *
 * <p>{@code ProjectBuilder}-based unit tests (see {@link TrackerLensPluginTest}) are fast and cover
 * most of this plugin's logic, but they do not enforce every runtime restriction a real build does -
 * notably, Gradle's mutation guard, which forbids realizing a {@code NamedDomainObjectContainer}'s
 * still-{@code register()}-deferred elements from within an {@code afterEvaluate} listener callback.
 * A bug of exactly that shape (reading {@code extension.getTrackers()} via {@code .stream()} inside
 * {@code afterEvaluate}) passed every {@code ProjectBuilder} test in this module while still failing
 * every real build. This class exists specifically to catch that class of bug again.</p>
 */
@DisplayName("TrackerLensPlugin (real Gradle build)")
class TrackerLensPluginIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("generateTrackerLensDashboardShouldSucceedWithARealGroovyTrackersRegisterBlock")
    void generateTrackerLensDashboardShouldSucceedWithARealGroovyTrackersRegisterBlock() throws IOException {
        writeSettingsFile();
        writeBuildFile("""
                plugins {
                    id 'com.arc-e-tect.tracker-lens'
                }
                trackerLens {
                    trackers {
                        register("bdd-scenarios") {
                            historyFiles.from(file("does-not-exist.ndjson"))
                            source = com.arc_e_tect.gradle.trackerlens.TrackerSourceKind.GHERKIN_SCENARIO
                        }
                    }
                }
                """);

        BuildResult result = runner("generateTrackerLensDashboard").build();

        assertThat(result.task(":generateTrackerLensDashboard").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
    }

    @Test
    @DisplayName("generateTrackerLensDashboardShouldFailAtTaskExecutionWhenNoTrackerIsRegistered")
    void generateTrackerLensDashboardShouldFailAtTaskExecutionWhenNoTrackerIsRegistered() throws IOException {
        // Regression test for a second bug the first fix's own TestKit tests immediately caught:
        // the "at least one tracker" check must fail only generateTrackerLensDashboard's own
        // execution, not the whole project's configuration - otherwise initTrackerLens and
        // bootstrapTrackerLensProject would be unusable in exactly the project state they're
        // meant for (no tracker registered yet).
        writeSettingsFile();
        writeBuildFile("""
                plugins {
                    id 'com.arc-e-tect.tracker-lens'
                }
                """);

        BuildResult result = runner("generateTrackerLensDashboard").buildAndFail();

        assertThat(result.getOutput()).contains("at least one tracker must be registered");
    }

    @Test
    @DisplayName("initTrackerLensShouldSucceedWithoutAnyTrackerConfigured")
    void initTrackerLensShouldSucceedWithoutAnyTrackerConfigured() throws IOException {
        writeSettingsFile();
        writeBuildFile("""
                plugins {
                    id 'com.arc-e-tect.tracker-lens'
                }
                """);

        BuildResult result = runner("initTrackerLens").build();

        assertThat(result.task(":initTrackerLens").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
    }

    @Test
    @DisplayName("bootstrapTrackerLensProjectShouldSucceedWithoutAnyTrackerConfigured")
    void bootstrapTrackerLensProjectShouldSucceedWithoutAnyTrackerConfigured() throws IOException {
        writeSettingsFile();
        writeBuildFile("""
                plugins {
                    id 'com.arc-e-tect.tracker-lens'
                }
                """);

        BuildResult result = runner("bootstrapTrackerLensProject").build();

        assertThat(result.task(":bootstrapTrackerLensProject").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
        assertThat(tempDir.resolve("tracker-lens-bootstrap/sample-project/build.gradle")).exists();
    }

    @Test
    @DisplayName("generateTrackerLensFixtureShouldNeverReportUpToDateOnASecondUnmodifiedInvocation")
    void generateTrackerLensFixtureShouldNeverReportUpToDateOnASecondUnmodifiedInvocation() throws IOException {
        // asOf defaults to Instant.now() at execution time, invisible to Gradle's own input
        // snapshotting - ProjectBuilder-based tests (see GenerateTrackerLensFixtureTaskTest) call
        // the task's action directly and so can never catch this: only a real, second Gradle
        // invocation against the same project directory exercises up-to-date checking at all. A
        // second, otherwise-unchanged invocation must still actually execute (SUCCESS, not
        // UP_TO_DATE), or the fixture would silently go stale after the first build.
        writeSettingsFile();
        writeBuildFile("""
                plugins {
                    id 'com.arc-e-tect.tracker-lens'
                }
                """);

        runner("generateTrackerLensFixture").build();
        BuildResult second = runner("generateTrackerLensFixture").build();

        assertThat(second.task(":generateTrackerLensFixture").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
    }

    private void writeSettingsFile() throws IOException {
        Files.writeString(tempDir.resolve("settings.gradle"), "rootProject.name = 'tracker-lens-integration-test'\n");
    }

    private void writeBuildFile(String content) throws IOException {
        Files.writeString(tempDir.resolve("build.gradle"), content);
    }

    private GradleRunner runner(String taskName) {
        return GradleRunner.create()
                .withProjectDir(tempDir.toFile())
                .withPluginClasspath()
                .forwardOutput()
                .withArguments(taskName, "--stacktrace");
    }
}

package com.arc_e_tect.gradle.trackerlens;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BootstrapProjectFiles")
class BootstrapProjectFilesTest {

    @Test
    @DisplayName("sampleProjectBuildGradleShouldPinTheGivenPluginVersion")
    void sampleProjectBuildGradleShouldPinTheGivenPluginVersion() {
        String buildGradle = BootstrapProjectFiles.sampleProjectBuildGradle("1.2.3");

        assertThat(buildGradle).contains("id 'com.arc-e-tect.tracker-lens' version '1.2.3'");
    }

    @Test
    @DisplayName("sampleProjectBuildGradleShouldConsumeTheLensPackCoordinateItsOwnReadmeDescribes")
    void sampleProjectBuildGradleShouldConsumeTheLensPackCoordinateItsOwnReadmeDescribes() {
        String buildGradle = BootstrapProjectFiles.sampleProjectBuildGradle("1.2.3");

        assertThat(buildGradle).contains("lensStyle 'com.example:my-tracker-lens-pack:0.1.0'");
    }

    @Test
    @DisplayName("sampleProjectBuildGradleShouldRegisterBothTrackersAgainstTheFixtureGeneratorsDefaultFilenames")
    void sampleProjectBuildGradleShouldRegisterBothTrackersAgainstTheFixtureGeneratorsDefaultFilenames() {
        String buildGradle = BootstrapProjectFiles.sampleProjectBuildGradle("1.2.3");

        assertThat(buildGradle)
                .contains("register('bdd-scenarios')", "historyFiles.from(file('gherkin-progress-history.ndjson'))")
                .contains("register('api-contracts')", "historyFiles.from(file('api-contract-progress.ndjson'))")
                .contains("TrackerSourceKind.GHERKIN_SCENARIO", "TrackerSourceKind.API_CONTRACT");
    }

    @Test
    @DisplayName("sampleProjectBuildGradleShouldMakeGenerateTrackerLensDashboardDependOnGenerateTrackerLensFixture")
    void sampleProjectBuildGradleShouldMakeGenerateTrackerLensDashboardDependOnGenerateTrackerLensFixture() {
        String buildGradle = BootstrapProjectFiles.sampleProjectBuildGradle("1.2.3");

        assertThat(buildGradle)
                .contains("tasks.named('generateTrackerLensDashboard') { dependsOn 'generateTrackerLensFixture' }");
    }

    @Test
    @DisplayName("lensPackBuildGradleShouldDeclareTheArtifactCoordinateTheSampleProjectConsumes")
    void lensPackBuildGradleShouldDeclareTheArtifactCoordinateTheSampleProjectConsumes() {
        String lensPackBuildGradle = BootstrapProjectFiles.lensPackBuildGradle();

        assertThat(lensPackBuildGradle).contains("artifactId = 'my-tracker-lens-pack'", "version = '0.1.0'");
    }
}

package com.arc_e_tect.gradle.trackerlens;

import com.arc_e_tect.gradle.trackerlens.tracker.GherkinScenarioTrackerSource;
import com.arc_e_tect.gradle.trackerlens.tracker.LifecycleRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BootstrapProjectFiles")
class BootstrapProjectFilesTest {

    @TempDir
    Path tempDir;

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
    @DisplayName("sampleHistoryNdjsonShouldBeReadableByGherkinScenarioTrackerSourceAsThreeRecords")
    void sampleHistoryNdjsonShouldBeReadableByGherkinScenarioTrackerSourceAsThreeRecords() throws Exception {
        Instant now = Instant.parse("2026-08-14T00:00:00Z");
        String ndjson = BootstrapProjectFiles.sampleHistoryNdjson(now);
        Path file = tempDir.resolve("sample-history.ndjson");
        Files.writeString(file, ndjson, StandardCharsets.UTF_8);

        List<LifecycleRecord> records = new GherkinScenarioTrackerSource().read(file.toFile());

        assertThat(records).hasSize(3);
    }

    @Test
    @DisplayName("sampleHistoryNdjsonShouldIncludeAtLeastOneFullyImplementedScenario")
    void sampleHistoryNdjsonShouldIncludeAtLeastOneFullyImplementedScenario() throws Exception {
        Instant now = Instant.parse("2026-08-14T00:00:00Z");
        String ndjson = BootstrapProjectFiles.sampleHistoryNdjson(now);
        Path file = tempDir.resolve("sample-history.ndjson");
        Files.writeString(file, ndjson, StandardCharsets.UTF_8);

        List<LifecycleRecord> records = new GherkinScenarioTrackerSource().read(file.toFile());

        assertThat(records).anyMatch(record -> record.hasReached("implemented"));
    }

    @Test
    @DisplayName("lensPackBuildGradleShouldDeclareTheArtifactCoordinateTheSampleProjectConsumes")
    void lensPackBuildGradleShouldDeclareTheArtifactCoordinateTheSampleProjectConsumes() {
        String lensPackBuildGradle = BootstrapProjectFiles.lensPackBuildGradle();

        assertThat(lensPackBuildGradle).contains("artifactId = 'my-tracker-lens-pack'", "version = '0.1.0'");
    }
}

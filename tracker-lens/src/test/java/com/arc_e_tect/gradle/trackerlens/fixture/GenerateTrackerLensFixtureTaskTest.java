package com.arc_e_tect.gradle.trackerlens.fixture;

import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Thin-wrapper coverage only: confirms this task builds a {@link FixtureSpec} from its properties
 * and delegates to {@link TrackerLensFixtureGenerator}, rather than re-testing the generator's own
 * behavior (covered by {@link TrackerLensFixtureGeneratorTest}) through it.
 */
@DisplayName("GenerateTrackerLensFixtureTask")
class GenerateTrackerLensFixtureTaskTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("generateShouldWriteBothHistoryFilesUsingDefaultsWhenNoPropertyIsSet")
    void generateShouldWriteBothHistoryFilesUsingDefaultsWhenNoPropertyIsSet() throws Exception {
        GenerateTrackerLensFixtureTask task = newTask();
        File bddFile = tempDir.resolve("gherkin-progress-history.ndjson").toFile();
        File apiFile = tempDir.resolve("api-contract-progress.ndjson").toFile();
        task.getBddScenarioHistoryFile().set(bddFile);
        task.getApiContractHistoryFile().set(apiFile);

        task.generate();

        assertThat(bddFile).exists();
        assertThat(apiFile).exists();
        FixtureSpec defaults = FixtureSpec.defaults();
        assertThat(countLines(bddFile) - 1).isEqualTo(defaults.bddScenarioCount());
        assertThat(countLines(apiFile) - 1).isEqualTo(defaults.apiContractCount());
    }

    @Test
    @DisplayName("generateShouldPassExplicitPropertiesThroughToTheGeneratedFixture")
    void generateShouldPassExplicitPropertiesThroughToTheGeneratedFixture() throws Exception {
        GenerateTrackerLensFixtureTask task = newTask();
        File bddFile = tempDir.resolve("bdd.ndjson").toFile();
        File apiFile = tempDir.resolve("api.ndjson").toFile();
        task.getBddScenarioHistoryFile().set(bddFile);
        task.getApiContractHistoryFile().set(apiFile);
        task.getAsOf().set("2026-08-16T00:00:00Z");
        task.getHistoryStartDaysAgo().set(60);
        task.getForecastTargetDaysOut().set(30);
        task.getWorkingDaysPerWeek().set(5);
        task.getBddScenarioCount().set(12);
        task.getApiContractCount().set(30);

        task.generate();

        assertThat(countLines(bddFile) - 1).isEqualTo(12);
        assertThat(countLines(apiFile) - 1).isEqualTo(30);
    }

    private GenerateTrackerLensFixtureTask newTask() {
        return ProjectBuilder.builder()
                .withProjectDir(tempDir.toFile())
                .build()
                .getTasks()
                .create("generateTrackerLensFixture", GenerateTrackerLensFixtureTask.class);
    }

    private long countLines(File file) throws Exception {
        return Files.readAllLines(file.toPath()).stream().filter(line -> !line.isBlank()).count();
    }
}

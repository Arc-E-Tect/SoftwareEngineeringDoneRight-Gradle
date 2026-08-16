package com.arc_e_tect.gradle.trackerlens.fixture;

import com.arc_e_tect.gradle.trackerlens.TrackerSourceKind;
import com.arc_e_tect.gradle.trackerlens.projection.Projection;
import com.arc_e_tect.gradle.trackerlens.projection.ProgressProjector;
import com.arc_e_tect.gradle.trackerlens.tracker.LifecycleRecord;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TrackerLensFixtureGenerator")
class TrackerLensFixtureGeneratorTest {

    private static final Instant FIXED_ASOF = Instant.parse("2026-08-16T12:00:00Z");

    @TempDir
    Path tempDir;

    private final TrackerLensFixtureGenerator generator = new TrackerLensFixtureGenerator();

    // ---------------------------------------------------------------------------------------
    // Default coverage
    // ---------------------------------------------------------------------------------------

    @Test
    @DisplayName("defaultSpecShouldProduceAllFiveApiContractCategoriesWithRealFieldConditions")
    void defaultSpecShouldProduceAllFiveApiContractCategoriesWithRealFieldConditions() {
        FixtureResult result = generator.generate(FixtureSpec.defaults().withAsOf(FIXED_ASOF));
        List<ApiContractFixtureRecord> api = result.apiContracts();

        assertThat(api).anyMatch(r -> r.declaredAt() == null && r.implementedAt() != null); // Shadow
        assertThat(api).anyMatch(r -> r.declaredAt() != null && r.implementedAt() == null); // Mirage
        assertThat(api).anyMatch(r -> r.stubbedAt() != null); // Stubbed
        assertThat(api).anyMatch(r -> r.declaredAt() != null && r.implementedAt() != null && r.verifiedAt() == null); // Doppelganger
        assertThat(api).anyMatch(r -> r.declaredAt() != null && r.implementedAt() != null && r.verifiedAt() != null); // Compliant
    }

    @Test
    @DisplayName("defaultSpecShouldProduceACompliantRecordThatIsAlsoStillStubbed")
    void defaultSpecShouldProduceACompliantRecordThatIsAlsoStillStubbed() {
        FixtureResult result = generator.generate(FixtureSpec.defaults().withAsOf(FIXED_ASOF));

        assertThat(result.apiContracts()).anyMatch(r ->
                r.declaredAt() != null && r.implementedAt() != null && r.verifiedAt() != null && r.stubbedAt() != null);
    }

    @Test
    @DisplayName("defaultSpecShouldProduceARemovedRecordOnBothTrackers")
    void defaultSpecShouldProduceARemovedRecordOnBothTrackers() {
        FixtureResult result = generator.generate(FixtureSpec.defaults().withAsOf(FIXED_ASOF));

        assertThat(result.apiContracts()).anyMatch(r -> r.removedAt() != null);
        assertThat(result.bddScenarios()).anyMatch(r -> r.removedAt() != null);
    }

    @Test
    @DisplayName("defaultSpecShouldProduceABddRepresentativeAtEachStage")
    void defaultSpecShouldProduceABddRepresentativeAtEachStage() {
        FixtureResult result = generator.generate(FixtureSpec.defaults().withAsOf(FIXED_ASOF));
        List<BddScenarioFixtureRecord> bdd = result.bddScenarios();

        assertThat(bdd).anyMatch(r -> r.listedAt() != null && r.definedAt() == null && r.implementedAt() == null);
        assertThat(bdd).anyMatch(r -> r.listedAt() != null && r.definedAt() != null && r.implementedAt() == null);
        assertThat(bdd).anyMatch(r -> r.listedAt() != null && r.definedAt() != null && r.implementedAt() != null);
    }

    @Test
    @DisplayName("defaultSpecShouldProduceABddScenarioThatSkippedTheDefinedStage")
    void defaultSpecShouldProduceABddScenarioThatSkippedTheDefinedStage() {
        FixtureResult result = generator.generate(FixtureSpec.defaults().withAsOf(FIXED_ASOF));

        assertThat(result.bddScenarios()).anyMatch(r -> r.definedAt() == null && r.implementedAt() != null);
    }

    @Test
    @DisplayName("defaultSpecShouldProduceTheDefaultRecordCounts")
    void defaultSpecShouldProduceTheDefaultRecordCounts() {
        FixtureResult result = generator.generate(FixtureSpec.defaults().withAsOf(FIXED_ASOF));

        assertThat(result.bddScenarios()).hasSize(8);
        assertThat(result.apiContracts()).hasSize(20);
    }

    @Test
    @DisplayName("scalingApiContractCountShouldSpreadExtraRecordsAcrossEveryCategoryInsteadOfPaddingOne")
    void scalingApiContractCountShouldSpreadExtraRecordsAcrossEveryCategoryInsteadOfPaddingOne() {
        FixtureSpec spec = FixtureSpec.defaults().withAsOf(FIXED_ASOF);
        spec = new FixtureSpec(spec.asOf(), spec.historyStartDaysAgo(), spec.forecastTargetDaysOut(),
                spec.workingDaysPerWeek(), spec.bddScenarioCount(), 40);

        List<ApiContractFixtureRecord> api = generator.generate(spec).apiContracts();

        long shadowCount = api.stream().filter(r -> r.declaredAt() == null && r.implementedAt() != null).count();
        long mirageCount = api.stream().filter(r -> r.declaredAt() != null && r.implementedAt() == null).count();
        long doppelgangerCount = api.stream()
                .filter(r -> r.declaredAt() != null && r.implementedAt() != null && r.verifiedAt() == null).count();
        long compliantCount = api.stream()
                .filter(r -> r.declaredAt() != null && r.implementedAt() != null && r.verifiedAt() != null).count();
        long stubbedCount = api.stream().filter(r -> r.stubbedAt() != null).count();

        assertThat(api).hasSize(40);
        assertThat(shadowCount).isGreaterThan(1);
        assertThat(mirageCount).isGreaterThan(1);
        assertThat(doppelgangerCount).isGreaterThan(1);
        assertThat(compliantCount).isGreaterThan(1);
        assertThat(stubbedCount).isGreaterThan(1);
    }

    // ---------------------------------------------------------------------------------------
    // Determinism
    // ---------------------------------------------------------------------------------------

    @Test
    @DisplayName("fixedAsOfShouldProduceByteIdenticalOutputAcrossRepeatedCalls")
    void fixedAsOfShouldProduceByteIdenticalOutputAcrossRepeatedCalls() throws Exception {
        FixtureSpec spec = FixtureSpec.defaults().withAsOf(FIXED_ASOF);

        FixtureResult first = generator.generate(spec);
        FixtureResult second = generator.generate(spec);

        File bddA = tempDir.resolve("a-bdd.ndjson").toFile();
        File apiA = tempDir.resolve("a-api.ndjson").toFile();
        File bddB = tempDir.resolve("b-bdd.ndjson").toFile();
        File apiB = tempDir.resolve("b-api.ndjson").toFile();
        first.writeTo(bddA, apiA);
        second.writeTo(bddB, apiB);

        assertThat(Files.readAllBytes(bddA.toPath())).isEqualTo(Files.readAllBytes(bddB.toPath()));
        assertThat(Files.readAllBytes(apiA.toPath())).isEqualTo(Files.readAllBytes(apiB.toPath()));
    }

    // ---------------------------------------------------------------------------------------
    // Calibration, verified end-to-end through the real production readers and projector
    // ---------------------------------------------------------------------------------------

    @ParameterizedTest(name = "historyStartDaysAgo={0}, forecastTargetDaysOut={1}")
    @DisplayName("generateShouldCalibrateTheForecastWithinToleranceAcrossSeveralWindows")
    @CsvSource({
            "60, 30",
            "90, 10",
            "30, 45",
            "14, 5",
    })
    void generateShouldCalibrateTheForecastWithinToleranceAcrossSeveralWindows(
            int historyStartDaysAgo, int forecastTargetDaysOut) throws Exception {
        FixtureSpec defaults = FixtureSpec.defaults();
        FixtureSpec spec = new FixtureSpec(FIXED_ASOF, historyStartDaysAgo, forecastTargetDaysOut,
                defaults.workingDaysPerWeek(), defaults.bddScenarioCount(), defaults.apiContractCount());

        FixtureResult result = generator.generate(spec);

        File bddFile = tempDir.resolve("gherkin-" + historyStartDaysAgo + "-" + forecastTargetDaysOut + ".ndjson").toFile();
        File apiFile = tempDir.resolve("api-" + historyStartDaysAgo + "-" + forecastTargetDaysOut + ".ndjson").toFile();
        result.writeTo(bddFile, apiFile);

        assertForecastWithinTolerance(bddFile, TrackerSourceKind.GHERKIN_SCENARIO, forecastTargetDaysOut);
        assertForecastWithinTolerance(apiFile, TrackerSourceKind.API_CONTRACT, forecastTargetDaysOut);
    }

    @ParameterizedTest(name = "driftMinutes={0}")
    @DisplayName("forecastShouldStayWithinToleranceWhenTheConsumingBuildRunsShortlyAfterGeneration")
    @CsvSource({"15", "45", "75", "110"})
    void forecastShouldStayWithinToleranceWhenTheConsumingBuildRunsShortlyAfterGeneration(int driftMinutes) throws Exception {
        // generateTrackerLensFixture and generateTrackerLensDashboard are always two separate
        // Gradle invocations, each calling Instant.now() independently - the dashboard build's own
        // "now" is never exactly asOf, only close to it. A fixture calibrated so tightly that the
        // earliest "done" item sits exactly on ProgressProjector's lookback-window edge would have
        // that edge flip the moment any time at all passes, swinging the rendered forecast wildly;
        // this proves the generator stays calibrated across the realistic gap between the two, not
        // just at the exact instant it was generated.
        FixtureSpec spec = FixtureSpec.defaults().withAsOf(FIXED_ASOF);
        FixtureResult result = generator.generate(spec);

        File bddFile = tempDir.resolve("gherkin-drift-" + driftMinutes + ".ndjson").toFile();
        File apiFile = tempDir.resolve("api-drift-" + driftMinutes + ".ndjson").toFile();
        result.writeTo(bddFile, apiFile);

        Instant consumingBuildNow = FIXED_ASOF.plus(Duration.ofMinutes(driftMinutes));
        assertForecastWithinTolerance(bddFile, TrackerSourceKind.GHERKIN_SCENARIO, consumingBuildNow,
                spec.forecastTargetDaysOut());
        assertForecastWithinTolerance(apiFile, TrackerSourceKind.API_CONTRACT, consumingBuildNow,
                spec.forecastTargetDaysOut());
    }

    @Test
    @DisplayName("generateShouldSucceedForAHistoryWindowJustAboveTheMinimumLookbackFloor")
    void generateShouldSucceedForAHistoryWindowJustAboveTheMinimumLookbackFloor() {
        FixtureSpec defaults = FixtureSpec.defaults();
        FixtureSpec spec = new FixtureSpec(FIXED_ASOF, 10, 3,
                defaults.workingDaysPerWeek(), defaults.bddScenarioCount(), defaults.apiContractCount());

        FixtureResult result = generator.generate(spec);

        assertThat(result.apiContracts()).isNotEmpty();
        assertThat(result.bddScenarios()).isNotEmpty();
    }

    @Test
    @DisplayName("generateShouldFailWithAClearMessageWhenHistoryWindowIsBelowTheMinimumLookbackFloor")
    void generateShouldFailWithAClearMessageWhenHistoryWindowIsBelowTheMinimumLookbackFloor() {
        FixtureSpec defaults = FixtureSpec.defaults();
        FixtureSpec spec = new FixtureSpec(FIXED_ASOF, 5, 3,
                defaults.workingDaysPerWeek(), defaults.bddScenarioCount(), defaults.apiContractCount());

        assertThatThrownBy(() -> generator.generate(spec))
                .isInstanceOf(FixtureCalibrationException.class)
                .hasMessageContaining("historyStartDaysAgo")
                .hasMessageContaining("forecastTargetDaysOut");
    }

    // ---------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------

    private void assertForecastWithinTolerance(File historyFile, TrackerSourceKind kind, int forecastTargetDaysOut) {
        // A small, deliberately non-zero offset from FIXED_ASOF, not FIXED_ASOF itself: the exact
        // generation instant is a razor-thin special case of ProgressProjector's own truncated
        // lookback math (see the calibration loop's ROBUSTNESS_SAMPLE_COUNT Javadoc) that a real
        // consuming build - always a separate, later Instant.now() - essentially never lands on.
        assertForecastWithinTolerance(historyFile, kind, FIXED_ASOF.plus(Duration.ofMinutes(20)), forecastTargetDaysOut);
    }

    private void assertForecastWithinTolerance(
            File historyFile, TrackerSourceKind kind, Instant now, int forecastTargetDaysOut) {
        List<LifecycleRecord> records = kind.createSource().read(historyFile);
        int activeCount = (int) records.stream().filter(r -> r.removedAt() == null).count();

        Optional<Projection> projection =
                new ProgressProjector().project(records, kind.finalStage(), activeCount, now);

        assertThat(projection).isPresent();
        long actualDaysOut = Duration.between(FIXED_ASOF, projection.get().projectedDate()).toDays();
        assertThat(Math.abs(actualDaysOut - forecastTargetDaysOut))
                .as("forecast for %s should land within tolerance of the %s-day target", kind, forecastTargetDaysOut)
                .isLessThanOrEqualTo(2);
    }
}

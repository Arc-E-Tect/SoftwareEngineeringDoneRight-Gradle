package com.arc_e_tect.gradle.trackerlens.fixture;

import java.time.Instant;
import java.util.Objects;

/**
 * Parameters for {@link TrackerLensFixtureGenerator}: how much history to synthesize, how far out
 * the resulting forecast should land, and how many items each of the two trackers should carry.
 *
 * <p>The same spec serves two distinct callers. A prototyping caller (e.g. the
 * {@code generateTrackerLensFixture} Gradle task) leaves {@link #asOf()} at its default of
 * {@link Instant#now()}, so the fixture is always fresh relative to whenever it's regenerated. An
 * automated-test caller sets {@link #asOf()} explicitly (see {@link #withAsOf(Instant)}), so the
 * generator produces byte-identical output on every run regardless of what day it is. Both are the
 * same generator and the same spec - only whether {@link #asOf()} is left at its default or
 * overridden differs.</p>
 *
 * @param asOf                  the instant to generate the fixture as of; defaults to
 *                              {@link Instant#now()} in {@link #defaults()}
 * @param historyStartDaysAgo   how many days of history, ending at {@code asOf}, the generated
 *                              timestamps are spread across
 * @param forecastTargetDaysOut how many calendar days out from {@code asOf} the calibrated
 *                              forecast should land, verified against the real
 *                              {@code ProgressProjector} at generation time
 * @param workingDaysPerWeek    must equal 5 - the number of business days per week
 *                              {@link com.arc_e_tect.gradle.trackerlens.projection.ProgressProjector}
 *                              itself currently, non-configurably,
 *                              assumes; present so a future version of this spec can widen that
 *                              assumption without an incompatible signature change, and validated
 *                              against today's fixed assumption in the meantime so a caller can
 *                              never silently miscalibrate against it
 * @param bddScenarioCount      how many BDD-scenario records to generate; scaled up from the
 *                              default proportionally across every represented stage, not padded
 * @param apiContractCount      how many API-contract records to generate; scaled up from the
 *                              default proportionally across every represented category, not
 *                              padded
 */
public record FixtureSpec(
        Instant asOf,
        int historyStartDaysAgo,
        int forecastTargetDaysOut,
        int workingDaysPerWeek,
        int bddScenarioCount,
        int apiContractCount) {

    /**
     * {@link com.arc_e_tect.gradle.trackerlens.projection.ProgressProjector}'s own, currently
     * non-configurable business-days-per-week assumption.
     */
    static final int REQUIRED_WORKING_DAYS_PER_WEEK = 5;

    private static final int DEFAULT_HISTORY_START_DAYS_AGO = 60;
    private static final int DEFAULT_FORECAST_TARGET_DAYS_OUT = 30;
    private static final int DEFAULT_BDD_SCENARIO_COUNT = 8;
    private static final int DEFAULT_API_CONTRACT_COUNT = 20;

    /** Validates every field. */
    public FixtureSpec {
        Objects.requireNonNull(asOf, "asOf");
        if (historyStartDaysAgo < 1) {
            throw new IllegalArgumentException("trackerLensFixture: historyStartDaysAgo must be at least 1");
        }
        if (forecastTargetDaysOut < 0) {
            throw new IllegalArgumentException("trackerLensFixture: forecastTargetDaysOut must not be negative");
        }
        if (workingDaysPerWeek != REQUIRED_WORKING_DAYS_PER_WEEK) {
            throw new IllegalArgumentException("trackerLensFixture: workingDaysPerWeek must be "
                    + REQUIRED_WORKING_DAYS_PER_WEEK + " to match ProgressProjector's own, currently "
                    + "non-configurable business-days-per-week assumption - a different value here would "
                    + "silently miscalibrate this fixture against the real forecast calculator");
        }
        if (bddScenarioCount < 1) {
            throw new IllegalArgumentException("trackerLensFixture: bddScenarioCount must be at least 1");
        }
        if (apiContractCount < 1) {
            throw new IllegalArgumentException("trackerLensFixture: apiContractCount must be at least 1");
        }
    }

    /**
     * The default spec: {@code asOf} set to the current instant, 60 days of history, a forecast
     * calibrated to land approximately 30 days out, a 5-day working week, 8 BDD scenarios, and 20
     * API contracts - immediately useful coverage, not a thin example (see
     * {@link TrackerLensFixtureGenerator}'s own Javadoc for exactly what's guaranteed).
     *
     * @return the default spec
     */
    public static FixtureSpec defaults() {
        return new FixtureSpec(Instant.now(), DEFAULT_HISTORY_START_DAYS_AGO, DEFAULT_FORECAST_TARGET_DAYS_OUT,
                REQUIRED_WORKING_DAYS_PER_WEEK, DEFAULT_BDD_SCENARIO_COUNT, DEFAULT_API_CONTRACT_COUNT);
    }

    /**
     * Returns a copy of this spec with {@link #asOf()} replaced, every other field unchanged - the
     * usual way an automated test pins this spec to a fixed instant for byte-identical output,
     * starting from {@link #defaults()}.
     *
     * @param newAsOf the instant to generate the fixture as of
     * @return a new spec, otherwise identical to this one
     */
    public FixtureSpec withAsOf(Instant newAsOf) {
        return new FixtureSpec(newAsOf, historyStartDaysAgo, forecastTargetDaysOut, workingDaysPerWeek,
                bddScenarioCount, apiContractCount);
    }
}

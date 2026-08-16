package com.arc_e_tect.gradle.trackerlens.fixture;

/**
 * Thrown by {@link TrackerLensFixtureGenerator} when no candidate dataset it tried, verified
 * against the real
 * {@link com.arc_e_tect.gradle.trackerlens.projection.ProgressProjector}, produced a forecast
 * within tolerance of the requested {@link FixtureSpec#forecastTargetDaysOut()}.
 *
 * <p>This is a deliberate failure, not an accident of the search loop: a
 * {@code forecastTargetDaysOut} too aggressive for the given
 * {@link FixtureSpec#historyStartDaysAgo()} (e.g. one landing inside
 * {@code ProgressProjector}'s own minimum-lookback floor, below which it refuses to forecast at
 * all) can make every achievable velocity land wide of the target - silently returning the
 * closest-but-still-wrong fixture would be worse than failing loudly here.</p>
 */
public class FixtureCalibrationException extends RuntimeException {

    /**
     * Creates a new {@code FixtureCalibrationException}.
     *
     * @param message a clear description of what was requested and why it couldn't be met
     */
    public FixtureCalibrationException(String message) {
        super(message);
    }
}

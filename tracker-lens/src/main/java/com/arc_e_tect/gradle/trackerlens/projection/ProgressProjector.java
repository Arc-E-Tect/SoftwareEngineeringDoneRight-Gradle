package com.arc_e_tect.gradle.trackerlens.projection;

import com.arc_e_tect.gradle.trackerlens.tracker.LifecycleRecord;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Extrapolates when a tracker will reach its total item count, from the historical rate at which
 * items have reached its final stage.
 *
 * <p>The lookback window adapts to how much history is actually available, capped at 90 days, with
 * a 7-day floor below which no projection is produced at all: a fast-moving team can generate a lot
 * of signal quickly, and a low-confidence estimate with a visible disclaimer is more useful than
 * months of silence. {@link Confidence} reflects how much of that window was actually available and
 * is meant to drive free-text disclaimer wording in the dashboard - it is never part of the CSS
 * contract.</p>
 */
public class ProgressProjector {

    private static final int MAX_LOOKBACK_DAYS = 90;
    private static final int MIN_LOOKBACK_DAYS = 7;
    private static final int MEDIUM_CONFIDENCE_THRESHOLD_DAYS = 30;

    /** Creates a new {@code ProgressProjector}. */
    public ProgressProjector() {}

    /**
     * Projects when {@code records} will collectively reach {@code totalCount} items at
     * {@code finalStage}.
     *
     * @param records    the tracker's current records
     * @param finalStage the stage that marks an item as complete
     * @param totalCount total number of items the tracker is tracking
     * @param now        the instant to project from
     * @return the projection, or {@link java.util.Optional#empty()} when fewer than 7 days of
     *         lookback history is available, or when the resulting velocity is zero or negative
     */
    public Optional<Projection> project(
            List<LifecycleRecord> records, String finalStage, int totalCount, Instant now) {
        Objects.requireNonNull(records, "records");
        Objects.requireNonNull(finalStage, "finalStage");
        Objects.requireNonNull(now, "now");

        List<Instant> reachedTimestamps = records.stream()
                .map(record -> record.stageReachedAt().get(finalStage))
                .filter(Objects::nonNull)
                .toList();
        if (reachedTimestamps.isEmpty()) {
            return Optional.empty();
        }

        Instant earliest = reachedTimestamps.stream().min(Comparator.naturalOrder()).orElseThrow();
        long daysSinceEarliest = Duration.between(earliest, now).toDays();
        long lookbackDays = Math.min(MAX_LOOKBACK_DAYS, daysSinceEarliest);
        if (lookbackDays < MIN_LOOKBACK_DAYS) {
            return Optional.empty();
        }

        Instant windowStart = now.minus(Duration.ofDays(lookbackDays));
        long reachedWithinWindow = reachedTimestamps.stream()
                .filter(timestamp -> !timestamp.isBefore(windowStart))
                .count();
        double velocityPerDay = (double) reachedWithinWindow / lookbackDays;
        if (velocityPerDay <= 0) {
            return Optional.empty();
        }

        int currentCount = reachedTimestamps.size();
        double remaining = Math.max(0, totalCount - currentCount);
        double daysToCompletion = remaining / velocityPerDay;
        Instant projectedDate = now.plusMillis(Math.round(daysToCompletion * Duration.ofDays(1).toMillis()));

        Confidence confidence = lookbackDays >= MAX_LOOKBACK_DAYS ? Confidence.HIGH
                : lookbackDays >= MEDIUM_CONFIDENCE_THRESHOLD_DAYS ? Confidence.MEDIUM
                : Confidence.LOW;

        return Optional.of(new Projection(projectedDate, currentCount, totalCount, velocityPerDay, confidence));
    }
}

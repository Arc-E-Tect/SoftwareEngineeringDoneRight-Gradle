package com.arc_e_tect.gradle.trackerlens.projection;

import java.time.Instant;

/**
 * An extrapolated completion estimate for a tracker, computed by {@link ProgressProjector}.
 *
 * @param projectedDate  estimated date the tracker reaches {@code totalCount}
 * @param currentCount   number of items that have reached the tracker's final stage so far
 * @param totalCount     total number of items the tracker is tracking
 * @param velocityPerDay items reaching the final stage per day, averaged over the lookback window
 * @param confidence     how much history this estimate is based on
 */
public record Projection(
        Instant projectedDate, int currentCount, int totalCount,
        double velocityPerDay, Confidence confidence) {
}

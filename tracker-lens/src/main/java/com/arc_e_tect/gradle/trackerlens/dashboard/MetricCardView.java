package com.arc_e_tect.gradle.trackerlens.dashboard;

/**
 * Rendered data for one {@code .metric-card} in a tracker section.
 *
 * @param stage   the stage name (e.g. {@code listed})
 * @param count   number of currently-active items that have reached this stage
 * @param percent {@code count} as a percentage of the tracker's total active item count, rounded
 *                to the nearest whole number, {@code 0} when the tracker has no active items
 */
public record MetricCardView(String stage, int count, int percent) {
}

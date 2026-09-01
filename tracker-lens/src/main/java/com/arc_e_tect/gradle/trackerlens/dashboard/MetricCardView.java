package com.arc_e_tect.gradle.trackerlens.dashboard;

/**
 * Rendered data for one {@code .metric-card} in a tracker section.
 *
 * @param stage      the stage name (e.g. {@code listed}), or a synthesized label for a card that
 *                    is not one of the tracker's own canonical stages (e.g. {@code removed})
 * @param count      number of items this card counts
 * @param totalCount the denominator {@code count} is a share of - the tracker's own active item
 *                    count for every per-stage card, but a card is free to carry a different one
 *                    (the {@code removed} card divides by every item ever seen, active or not,
 *                    since a removed item was never part of the tracker's own active total)
 * @param percent    {@code count} as a percentage of {@code totalCount}, rounded to the nearest
 *                    whole number, {@code 0} when {@code totalCount} is {@code 0}
 */
public record MetricCardView(String stage, int count, int totalCount, int percent) {
}

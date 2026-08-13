package com.arc_e_tect.gradle.trackerlens.projection;

/**
 * How much history a {@link Projection} is based on, driving the disclaimer text shown next to it
 * in the dashboard. This tier is rendered text only - it is never part of the CSS contract, so a
 * lens may style it any way it likes without breaking anything structural.
 */
public enum Confidence {

    /** Fewer than 30 days of lookback history. */
    LOW,

    /** Between 30 and 89 days of lookback history. */
    MEDIUM,

    /** The full 90-day lookback cap has been reached. */
    HIGH
}

package com.arc_e_tect.gradle.trackerlens.contract;

import static com.arc_e_tect.gradle.trackerlens.contract.Cardinality.SAME_COUNT_AS;
import static com.arc_e_tect.gradle.trackerlens.contract.Cardinality.Fixed.AT_LEAST_ONE;
import static com.arc_e_tect.gradle.trackerlens.contract.Cardinality.Fixed.AT_LEAST_ONE_PER_TRACKER;
import static com.arc_e_tect.gradle.trackerlens.contract.Cardinality.Fixed.AT_MOST_ONE;
import static com.arc_e_tect.gradle.trackerlens.contract.Cardinality.Fixed.AT_MOST_ONE_PER_TRACKER;
import static com.arc_e_tect.gradle.trackerlens.contract.Cardinality.Fixed.EXACTLY_ONE;
import static com.arc_e_tect.gradle.trackerlens.contract.Cardinality.Fixed.SAME_COUNT_AS_TRACKERS;

/**
 * The tracker-lens dashboard content contract: the single source of truth for which structural
 * elements a generated {@code dashboard.html} must contain, so a lens's CSS has stable selectors to
 * style against.
 *
 * <p>Every rule here governs a <strong>data-bound</strong> element - one rendered from a
 * {@code LifecycleRecord} or a {@code Projection}, such as a metric's value, a stale item's name, or
 * a chart's series. Its class names, {@code data-*} attributes, and position are the contract and
 * must not change casually across versions. Surrounding text - headings, captions, labels - is
 * never part of this contract and is free to reword at any time: no rule here, and no lens's CSS,
 * should ever select on literal text.</p>
 *
 * <p>{@link LensContractValidator} enforces every rule below against a generated dashboard, and
 * {@code DASHBOARD-THEMING.adoc} is tested for consistency against this enum, so the two can never
 * drift apart.</p>
 */
public enum ContractRule {

    /** The single root element wrapping the entire dashboard. */
    DASHBOARD_ROOT(".dashboard", EXACTLY_ONE),

    /** One section per registered tracker. */
    TRACKER_SECTION(".tracker[data-tracker]", AT_LEAST_ONE),

    /** One metric card per stage, within each tracker section. */
    METRIC_CARD(".metric-card[data-stage]", AT_LEAST_ONE_PER_TRACKER),

    /** Every metric card also carries the {@code --percent} custom-property styling hook. */
    METRIC_CARD_PERCENT_HOOK(".metric-card[style*='--percent']", SAME_COUNT_AS(METRIC_CARD)),

    /** At most one projection block per tracker section (absent when no projection is available). */
    PROJECTION_BLOCK(".projection[data-status]", AT_MOST_ONE_PER_TRACKER),

    /** One chart canvas per tracker section. */
    CHART_CANVAS(".chart canvas", SAME_COUNT_AS_TRACKERS),

    /** At most one stale-items table for the whole dashboard. */
    STALE_TABLE(".stale-items__table", AT_MOST_ONE),

    /** At most one lens switcher for the whole dashboard. */
    LENS_SWITCHER(".lens-switcher[data-lens-count]", AT_MOST_ONE);

    private final String selector;
    private final Cardinality cardinality;

    ContractRule(String selector, Cardinality cardinality) {
        this.selector = selector;
        this.cardinality = cardinality;
    }

    /**
     * The CSS selector that must match according to {@link #cardinality()}.
     *
     * @return the CSS selector
     */
    public String selector() {
        return selector;
    }

    /**
     * How many times {@link #selector()} is allowed or required to match.
     *
     * @return the cardinality
     */
    public Cardinality cardinality() {
        return cardinality;
    }
}

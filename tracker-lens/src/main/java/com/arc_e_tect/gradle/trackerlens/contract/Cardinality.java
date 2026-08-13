package com.arc_e_tect.gradle.trackerlens.contract;

/**
 * How many times a {@link ContractRule}'s selector is allowed or required to match in a generated
 * dashboard.
 *
 * <p>{@link Fixed} covers whole-document counts ({@link Fixed#EXACTLY_ONE},
 * {@link Fixed#AT_LEAST_ONE}, {@link Fixed#AT_MOST_ONE}), per-tracker counts
 * ({@link Fixed#AT_LEAST_ONE_PER_TRACKER}, {@link Fixed#AT_MOST_ONE_PER_TRACKER}), and the
 * whole-document count matching the number of discovered tracker sections
 * ({@link Fixed#SAME_COUNT_AS_TRACKERS}). {@link #SAME_COUNT_AS(ContractRule)} covers a
 * whole-document count matching another rule's whole-document count, e.g. every metric card also
 * carrying the {@code --percent} styling hook.</p>
 */
public sealed interface Cardinality permits Cardinality.Fixed, Cardinality.SameCountAsRule {

    /** The fixed (non-rule-relative) cardinality kinds. */
    enum Fixed implements Cardinality {
        /** Exactly one match in the whole document. */
        EXACTLY_ONE,
        /** At least one match in the whole document. */
        AT_LEAST_ONE,
        /** At most one match in the whole document. */
        AT_MOST_ONE,
        /** At least one match within each tracker section. */
        AT_LEAST_ONE_PER_TRACKER,
        /** At most one match within each tracker section. */
        AT_MOST_ONE_PER_TRACKER,
        /** The whole-document match count equals the number of discovered tracker sections. */
        SAME_COUNT_AS_TRACKERS
    }

    /**
     * The whole-document match count equals {@code rule}'s whole-document match count.
     *
     * @param rule the rule to compare against
     */
    record SameCountAsRule(ContractRule rule) implements Cardinality {
    }

    /**
     * Creates a {@link SameCountAsRule} cardinality relative to {@code rule}.
     *
     * @param rule the rule to compare against
     * @return the cardinality
     */
    static Cardinality SAME_COUNT_AS(ContractRule rule) {
        return new SameCountAsRule(rule);
    }
}

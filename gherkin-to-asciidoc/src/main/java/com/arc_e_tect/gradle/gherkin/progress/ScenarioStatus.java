package com.arc_e_tect.gradle.gherkin.progress;

/**
 * Implementation progress of a single scenario, as tracked when
 * {@code trackProgress} is enabled.
 */
public enum ScenarioStatus {

    /** The scenario has a title only; it has no {@code Given}/{@code When}/{@code Then} steps yet. */
    LISTED,

    /** The scenario has steps, but at least one step has no matching glue code. */
    DEFINED,

    /** The scenario has steps and every step has matching glue code. */
    IMPLEMENTED
}

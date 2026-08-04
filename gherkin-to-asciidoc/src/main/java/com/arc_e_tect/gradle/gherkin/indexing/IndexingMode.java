package com.arc_e_tect.gradle.gherkin.indexing;

/**
 * Controls whether {@code Feature}/{@code Scenario} titles are numbered directly in the source
 * {@code .feature} files, as configured via the {@code indexing} DSL property.
 */
public enum IndexingMode {

    /** Nothing is numbered. Any numbering left over from a previous run is removed. */
    OFF,

    /**
     * Every feature is numbered, in the order its feature file is processed in - see
     * {@code GherkinToAsciidocExtension#getIndexing()} for exactly what that order is - e.g.
     * {@code Feature: 1 - User authentication}.
     */
    FEATURE,

    /**
     * Every scenario is numbered, continuously across all feature files, in the same file
     * processing order as {@link #FEATURE}, e.g. {@code Scenario: 1 - User logs in}.
     */
    SCENARIO,

    /**
     * Both features and scenarios are numbered. Scenarios are numbered per feature as
     * {@code <featureNumber>.<scenarioNumber>}, e.g. {@code Scenario: 1.1 - User logs in}
     * within {@code Feature: 1 - User authentication}.
     */
    ALL
}

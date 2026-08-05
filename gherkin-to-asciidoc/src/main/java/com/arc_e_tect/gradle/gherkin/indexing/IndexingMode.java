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
    ALL,

    /**
     * Indexing is skipped entirely: the source {@code .feature} files are left completely
     * untouched, not even to strip numbering left over from a previous run - unlike {@link #OFF},
     * which does strip it. Intended to be set via the {@code -PgherkinToAsciidoc.indexing=ci}
     * command-line override (see {@code GherkinToAsciidocExtension#getIndexing()}) so a CI
     * pipeline never mutates source files, regardless of the {@code indexing} value configured in
     * the build script - but it can also be configured directly like any other value.
     */
    CI
}

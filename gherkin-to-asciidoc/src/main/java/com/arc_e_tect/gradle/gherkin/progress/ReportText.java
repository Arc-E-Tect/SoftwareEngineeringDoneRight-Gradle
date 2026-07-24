package com.arc_e_tect.gradle.gherkin.progress;

/** Shared explanatory text used by both {@link ProgressReportWriter} and {@link ReportTemplateRenderer}. */
public final class ReportText {

    private ReportText() {}

    /** One-sentence description of what the generated document contains. */
    public static final String INTRO =
            "This document lists every `Scenario` and `Scenario Outline` found under the configured feature "
            + "file directories, classified by how far each one is toward being automated.";

    /** Explanation of the {@code listed} status. */
    public static final String LISTED_BLURB =
            "Scenarios with a title only. No `Given`/`When`/`Then` steps have been written for them yet.";

    /** Explanation of the {@code defined} status. */
    public static final String DEFINED_BLURB =
            "Scenarios with steps written, but at least one step has no matching glue code yet.";

    /** Explanation of the {@code implemented} status. */
    public static final String IMPLEMENTED_BLURB =
            "Scenarios whose every step has matching glue code.";
}

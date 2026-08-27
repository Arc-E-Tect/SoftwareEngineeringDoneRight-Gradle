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

    /**
     * Explanation of why a scenario becomes removed - i.e. its {@code removedAt} is set - written
     * right before the {@code Progress Over Time} table's own {@code Removed (no longer seen)} row,
     * since a bare count invites the question of what it actually means.
     */
    public static final String REMOVED_BLURB =
            "A scenario becomes removed when a run no longer finds it in any feature file at all - its "
            + "record is never deleted, only marked, so its earlier progress stays intact. This is "
            + "expected, ongoing churn, not necessarily a mistake. Feature files are often written "
            + "ahead of time to scope an upcoming release, and it's common for scenarios to be taken "
            + "out of scope - postponed to a later release - and for others to be taken back into "
            + "scope at a later stage. A scenario can also become removed because it was considered "
            + "obsolete, turned out to duplicate another scenario after all, or was rewritten to fix "
            + "an error in its own definition - which then reappears as a *new* scenario rather than a "
            + "continuation of the old one, since the two no longer share the same title/feature "
            + "fingerprint. If a scenario with the same fingerprint reappears in a later run, its "
            + "`removedAt` is cleared automatically and its progress resumes from where it left off.";
}

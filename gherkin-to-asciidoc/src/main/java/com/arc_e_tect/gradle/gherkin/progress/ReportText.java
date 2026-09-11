package com.arc_e_tect.gradle.gherkin.progress;

import java.util.List;

/** Shared explanatory text used by both {@link ProgressReportWriter} and {@link ReportTemplateRenderer}. */
public final class ReportText {

    private ReportText() {}

    /** One-sentence description of what the generated document contains. */
    public static final String INTRO =
            "This document lists every `Scenario` found under the configured feature file directories, plus "
            + "one entry per `Examples` row of every `Scenario Outline`, classified by how far each one is "
            + "toward being automated.";

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

    /** Title of the admonition {@link #reinterpretedOutlinesNotice(List)} renders. */
    private static final String REINTERPRETED_OUTLINES_TITLE =
            "`Scenario Outline`s are now reported one scenario per `Examples` row";

    /**
     * Renders the notice that this run has started reporting one scenario per {@code Examples} row
     * for the given outlines - see {@link ReinterpretedOutlines} for when that happens, and why it
     * happens at most once per outline.
     *
     * @param outlines the outlines re-interpreted on this run
     * @return the AsciiDoc admonition block to place in the report, ending in a newline; the empty
     *         string when {@code outlines} is empty, so a caller can emit it unconditionally
     */
    public static String reinterpretedOutlinesNotice(List<ReinterpretedOutlines.Outline> outlines) {
        if (outlines.isEmpty()) {
            return "";
        }
        StringBuilder notice = new StringBuilder();
        notice.append("[IMPORTANT]\n")
                .append(".").append(REINTERPRETED_OUTLINES_TITLE).append("\n")
                .append("====\n")
                .append("This report has started reading a `Scenario Outline` the way Cucumber runs it: as one "
                        + "scenario per `Examples` row, rather than as a single scenario. The counts below went "
                        + "up accordingly, without a single new scenario having been written.\n")
                .append("\n")
                .append(outlines.size() == 1
                        ? "One outline changed interpretation on this run:\n"
                        : outlines.size() + " outlines changed interpretation on this run:\n")
                .append("\n");
        for (ReinterpretedOutlines.Outline outline : outlines) {
            notice.append("* `").append(outline.title())
                    .append("` (in `").append(outline.featureTitle()).append("`) - now ")
                    .append(outline.scenarioCount()).append(outline.scenarioCount() == 1
                            ? " scenario\n" : " scenarios\n");
        }
        notice.append("\n")
                .append("Each of those outlines had one entry in the progress history, keyed on the outline's own "
                        + "title. That entry has been closed - counted under `Removed (no longer seen)` in "
                        + "*Progress Over Time* below, with everything it had already recorded kept intact - "
                        + "and every one of the outline's "
                        + "`Examples` rows now has an entry of its own, tracked from this run onward. Nothing in "
                        + "the feature files changed; only how they are read did.\n")
                .append("\n")
                .append("This notice appears only on the run that detects the change.\n")
                .append("====\n");
        return notice.toString();
    }
}

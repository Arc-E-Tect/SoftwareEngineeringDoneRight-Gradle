package com.arc_e_tect.gradle.gherkin.progress;

import java.io.File;
import java.util.Map;

/**
 * Configuration for {@link ProgressReportWriter}.
 *
 * @param groupByFeature whether to group scenarios by their enclosing {@code Feature}, within
 *                       each status, instead of a flat list
 * @param snippetDir     directory to write the {@code listed.adoc}/{@code defined.adoc}/
 *                       {@code implemented.adoc} snippet files to
 * @param template       a Mustache template file used to render the report so that it includes
 *                       the generated snippets rather than embedding their content verbatim;
 *                       {@code null} to use the built-in default report layout
 * @param systemUnderTestVersion version of the system under test that the reported scenarios exercise,
 *                       printed near the top of the report
 * @param history        the persisted scenario progress history, keyed by fingerprint, to render a
 *                       {@code == Progress Over Time} section from; {@code null} or empty when
 *                       {@code trackProgressHistory} is disabled, in which case no such section is
 *                       rendered
 */
public record ProgressReportOptions(
        boolean groupByFeature, File snippetDir, File template, String systemUnderTestVersion,
        Map<String, ScenarioProgressRecord> history) {

    /**
     * Creates options with progress history tracking disabled ({@link #history()} empty).
     *
     * @param groupByFeature whether to group scenarios by their enclosing {@code Feature}
     * @param snippetDir     directory to write the report snippet files to
     * @param template       optional Mustache template file, or {@code null}
     * @param systemUnderTestVersion version of the system under test that the reported scenarios exercise
     */
    public ProgressReportOptions(
            boolean groupByFeature, File snippetDir, File template, String systemUnderTestVersion) {
        this(groupByFeature, snippetDir, template, systemUnderTestVersion, Map.of());
    }

    /** Defensively copies {@code history} into an immutable map, defaulting a {@code null} to empty. */
    public ProgressReportOptions {
        history = history == null ? Map.of() : Map.copyOf(history);
    }
}

package com.arc_e_tect.gradle.gherkin.progress;

import java.io.File;

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
 */
public record ProgressReportOptions(boolean groupByFeature, File snippetDir, File template) {
}

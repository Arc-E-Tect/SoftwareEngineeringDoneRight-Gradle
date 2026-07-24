package com.arc_e_tect.gradle.gherkin.snippet;

import java.io.File;
import java.util.List;

/**
 * The snippet file(s) written for one {@link com.arc_e_tect.gradle.gherkin.progress.ScenarioStatus}.
 *
 * <p>Exactly one of {@link #features()} or {@link #flatFile()} is populated: {@code features} when
 * the status has at least one scenario and scenarios are grouped by feature; {@code flatFile}
 * otherwise (grouping disabled, or the status has no scenarios at all).</p>
 *
 * @param features the per-feature snippet files, in feature order; empty when {@link #flatFile()} is used
 * @param flatFile the single flat snippet file for this status; {@code null} when {@link #features()} is used
 */
public record StatusSnippets(List<FeatureSnippet> features, File flatFile) {
}

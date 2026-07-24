package com.arc_e_tect.gradle.gherkin.progress;

import com.arc_e_tect.gradle.gherkin.parser.ScenarioInfo;

import java.util.List;

/**
 * The scenarios classified under one {@link ScenarioStatus}, along with the display text and
 * summary figures used to render that status's heading, blurb, and summary table row.
 *
 * @param status     the status these scenarios were classified as
 * @param label      the display label, e.g. {@code "Listed"}
 * @param blurb      a one-sentence explanation of what {@code status} means
 * @param scenarios  the scenarios classified as {@code status}
 * @param count      {@code scenarios.size()}
 * @param percentage {@code count} as a percentage of the total scenario count, formatted to one
 *                   decimal place, e.g. {@code "33.3"}
 */
public record StatusSummary(
        ScenarioStatus status, String label, String blurb, List<ScenarioInfo> scenarios, int count, String percentage) {
}

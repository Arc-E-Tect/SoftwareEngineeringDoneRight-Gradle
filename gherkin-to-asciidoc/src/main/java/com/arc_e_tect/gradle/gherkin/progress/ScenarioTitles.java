package com.arc_e_tect.gradle.gherkin.progress;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Strips the incidental formatting {@code FeatureIndexer} adds to {@code Feature}/{@code Scenario}
 * titles - the {@code Scenario:}/{@code Scenario Outline:} keyword prefix, and any existing numeric
 * index prefix ({@code FeatureIndexer}'s own {@code "N - "} / {@code "N.M - "} format) - so a
 * caller that needs a title's stable, human-meaningful identity never sees either.
 *
 * <p>Shared by {@link ScenarioFingerprint} (which additionally lowercases and trims the result
 * before hashing, for a case- and whitespace-insensitive identity) and
 * {@link ProgressHistoryUpdater} (which persists the result as-is, case preserved, into
 * {@code scenarioName}/{@code featureTitle}) - both need the exact same stripping, since a scenario
 * or feature getting renumbered (e.g. because an earlier one was inserted or removed) must never
 * look, to either of them, like a different scenario or a changed title. A feature title never
 * carries the keyword prefix in the first place - {@code FeatureParser} reads it straight from the
 * Gherkin document's own parsed {@code Feature} name - so only {@link #withoutIndex(String)}
 * applies to it; {@link #withoutKeyword(String)} is scenario-title-specific.</p>
 */
final class ScenarioTitles {

    private static final Pattern KEYWORD_PREFIX = Pattern.compile("^(?:Scenario Outline|Scenario):\\s*(.*)$");
    private static final Pattern INDEX_PREFIX = Pattern.compile("^\\d+(?:\\.\\d+)? - (.*)$");

    private ScenarioTitles() {}

    /**
     * Strips a leading {@code Scenario:}/{@code Scenario Outline:} keyword, if present.
     *
     * @param scenarioTitle a scenario's formatted title, e.g. {@code "Scenario: User logs in"}
     * @return {@code scenarioTitle} with the keyword prefix removed, or {@code scenarioTitle}
     *         unchanged if it has none
     */
    static String withoutKeyword(String scenarioTitle) {
        return strip(KEYWORD_PREFIX, scenarioTitle);
    }

    /**
     * Strips a leading {@code FeatureIndexer}-written numeric index prefix, if present - either the
     * single-integer {@code "N - "} form ({@link com.arc_e_tect.gradle.gherkin.indexing.IndexingMode#FEATURE}
     * or {@link com.arc_e_tect.gradle.gherkin.indexing.IndexingMode#SCENARIO}) or the
     * {@code "N.M - "} feature-and-scenario form ({@link com.arc_e_tect.gradle.gherkin.indexing.IndexingMode#ALL}).
     *
     * @param name a {@code Feature} or {@code Scenario} name, with any keyword prefix already
     *             removed
     * @return {@code name} with the index prefix removed, or {@code name} unchanged if it has none
     */
    static String withoutIndex(String name) {
        return strip(INDEX_PREFIX, name);
    }

    private static String strip(Pattern pattern, String value) {
        Matcher matcher = pattern.matcher(value);
        return matcher.matches() ? matcher.group(1) : value;
    }
}

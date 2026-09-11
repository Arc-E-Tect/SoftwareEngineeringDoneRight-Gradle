package com.arc_e_tect.gradle.gherkin.parser;

import java.util.List;

/**
 * Immutable description of a single scenario parsed from a {@code .feature} file - for a
 * {@code Scenario Outline}, one of these per {@code Examples} row rather than one per outline, with
 * that row's values already substituted into both {@code title} and {@code steps} (see
 * {@link FeatureParser}).
 *
 * @param featureTitle the name of the enclosing {@code Feature}, e.g. {@code "User authentication"};
 *                      used to group scenarios by feature in the generated report
 * @param title        the formatted scenario title, e.g. {@code "Scenario: User logs in"}, or
 *                      {@code "Scenario Outline: User logs in as alice"} for one row of an outline
 * @param steps        the text of each {@code Given}/{@code When}/{@code Then}/{@code And}/{@code But}
 *                      step in document order, with the keyword stripped and, for an outline row,
 *                      the row's {@code Examples} values substituted for its {@code <placeholder>}s;
 *                      empty when the scenario has no steps
 * @param outlineTitle  for one {@code Examples} row of a {@code Scenario Outline}, the title that
 *                      outline as a whole would carry - the same formatting as {@code title}, but
 *                      with the outline's own name left unsubstituted, e.g.
 *                      {@code "Scenario Outline: User logs in as <username>"}; {@code null} for
 *                      every scenario that isn't an expanded row. This is the title the scenario
 *                      was reported under before outlines were expanded, which is what lets
 *                      {@link com.arc_e_tect.gradle.gherkin.progress.ReinterpretedOutlines}
 *                      recognise a progress history record that this expansion, rather than an
 *                      edit to the feature files, has superseded
 */
public record ScenarioInfo(String featureTitle, String title, List<String> steps, String outlineTitle) {

    /**
     * Creates a scenario that isn't an expanded {@code Examples} row, leaving
     * {@link #outlineTitle()} {@code null}.
     *
     * @param featureTitle the name of the enclosing {@code Feature}
     * @param title        the formatted scenario title
     * @param steps        the text of each step, in document order
     */
    public ScenarioInfo(String featureTitle, String title, List<String> steps) {
        this(featureTitle, title, steps, null);
    }

    /** Defensively copies {@code steps} into an immutable list. */
    public ScenarioInfo {
        steps = List.copyOf(steps);
    }
}

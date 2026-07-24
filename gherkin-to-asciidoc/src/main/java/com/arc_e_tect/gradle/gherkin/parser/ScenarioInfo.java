package com.arc_e_tect.gradle.gherkin.parser;

import java.util.List;

/**
 * Immutable description of a single scenario parsed from a {@code .feature} file.
 *
 * @param featureTitle the name of the enclosing {@code Feature}, e.g. {@code "User authentication"};
 *                      used to group scenarios by feature in the generated report
 * @param title        the formatted scenario title, e.g. {@code "Scenario: User logs in"}
 * @param steps        the raw text of each {@code Given}/{@code When}/{@code Then}/{@code And}/{@code But}
 *                      step in document order, with the keyword stripped; empty when the scenario
 *                      has no steps
 */
public record ScenarioInfo(String featureTitle, String title, List<String> steps) {

    /** Defensively copies {@code steps} into an immutable list. */
    public ScenarioInfo {
        steps = List.copyOf(steps);
    }
}

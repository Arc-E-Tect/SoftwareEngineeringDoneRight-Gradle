package com.arc_e_tect.gradle.gherkin.progress;

import com.arc_e_tect.gradle.gherkin.parser.ScenarioInfo;
import io.cucumber.cucumberexpressions.Expression;

import java.util.List;

/**
 * Classifies a {@link ScenarioInfo} as {@link ScenarioStatus#LISTED},
 * {@link ScenarioStatus#DEFINED}, or {@link ScenarioStatus#IMPLEMENTED} based on
 * whether it has steps, and whether every step has matching glue code.
 */
public class ScenarioClassifier {

    /** Creates a new {@code ScenarioClassifier}. */
    public ScenarioClassifier() {}

    /**
     * Classifies the given scenario.
     *
     * @param scenario   the scenario to classify
     * @param glueCode   step definition patterns found in the configured glue code directory
     * @return {@link ScenarioStatus#LISTED} when the scenario has no steps,
     *         {@link ScenarioStatus#IMPLEMENTED} when every step matches at least one
     *         glue code pattern, otherwise {@link ScenarioStatus#DEFINED}
     */
    public ScenarioStatus classify(ScenarioInfo scenario, List<Expression> glueCode) {
        if (scenario.steps().isEmpty()) {
            return ScenarioStatus.LISTED;
        }
        boolean allStepsImplemented = scenario.steps().stream()
                .allMatch(step -> hasMatchingGlueCode(step, glueCode));
        return allStepsImplemented ? ScenarioStatus.IMPLEMENTED : ScenarioStatus.DEFINED;
    }

    private boolean hasMatchingGlueCode(String stepText, List<Expression> glueCode) {
        return glueCode.stream().anyMatch(expression -> expression.match(stepText).isPresent());
    }
}

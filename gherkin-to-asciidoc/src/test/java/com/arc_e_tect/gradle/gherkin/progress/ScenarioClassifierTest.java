package com.arc_e_tect.gradle.gherkin.progress;

import com.arc_e_tect.gradle.gherkin.parser.ScenarioInfo;
import io.cucumber.cucumberexpressions.Expression;
import io.cucumber.cucumberexpressions.ExpressionFactory;
import io.cucumber.cucumberexpressions.ParameterTypeRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ScenarioClassifier")
class ScenarioClassifierTest {

    private final ScenarioClassifier classifier = new ScenarioClassifier();
    private final ExpressionFactory expressionFactory =
            new ExpressionFactory(new ParameterTypeRegistry(Locale.ENGLISH));

    @Test
    @DisplayName("classifies a scenario with no steps as LISTED")
    void classifiesScenarioWithNoStepsAsListed() {
        ScenarioInfo scenario = new ScenarioInfo("Scenario: Bare title", List.of());

        ScenarioStatus status = classifier.classify(scenario, List.of());

        assertThat(status).isEqualTo(ScenarioStatus.LISTED);
    }

    @Test
    @DisplayName("classifies a scenario with steps but no matching glue code as DEFINED")
    void classifiesScenarioWithUnmatchedStepsAsDefined() {
        ScenarioInfo scenario = new ScenarioInfo(
                "Scenario: Not implemented yet",
                List.of("the login page is open"));

        ScenarioStatus status = classifier.classify(scenario, List.of());

        assertThat(status).isEqualTo(ScenarioStatus.DEFINED);
    }

    @Test
    @DisplayName("classifies a scenario as DEFINED when only some steps have glue code")
    void classifiesScenarioAsDefinedWhenPartiallyImplemented() {
        ScenarioInfo scenario = new ScenarioInfo(
                "Scenario: Partially implemented",
                List.of("the login page is open", "the user submits credentials"));
        List<Expression> glueCode = List.of(expression("the login page is open"));

        ScenarioStatus status = classifier.classify(scenario, glueCode);

        assertThat(status).isEqualTo(ScenarioStatus.DEFINED);
    }

    @Test
    @DisplayName("classifies a scenario as IMPLEMENTED when every step has matching glue code")
    void classifiesScenarioAsImplementedWhenAllStepsMatch() {
        ScenarioInfo scenario = new ScenarioInfo(
                "Scenario: Fully implemented",
                List.of("the login page is open", "the user submits \"alice\" and \"secret\""));
        List<Expression> glueCode = List.of(
                expression("the login page is open"),
                expression("the user submits {string} and {string}"));

        ScenarioStatus status = classifier.classify(scenario, glueCode);

        assertThat(status).isEqualTo(ScenarioStatus.IMPLEMENTED);
    }

    @Test
    @DisplayName("classifies a scenario as DEFINED when a step's text doesn't fit any glue code parameter type")
    void classifiesScenarioAsDefinedWhenStepDoesNotFitParameterType() {
        ScenarioInfo scenario = new ScenarioInfo(
                "Scenario: Typed mismatch",
                List.of("I have many cukes"));
        List<Expression> glueCode = List.of(expression("I have {int} cukes"));

        ScenarioStatus status = classifier.classify(scenario, glueCode);

        assertThat(status).isEqualTo(ScenarioStatus.DEFINED);
    }

    private Expression expression(String pattern) {
        return expressionFactory.createExpression(pattern);
    }
}

package com.arc_e_tect.gradle.gherkin.progress;

import com.arc_e_tect.gradle.gherkin.parser.ScenarioInfo;
import io.cucumber.cucumberexpressions.Expression;
import io.cucumber.cucumberexpressions.ExpressionFactory;
import io.cucumber.cucumberexpressions.ParameterTypeRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ProgressReportWriter")
class ProgressReportWriterTest {

    private final ProgressReportWriter writer = new ProgressReportWriter();
    private final ExpressionFactory expressionFactory =
            new ExpressionFactory(new ParameterTypeRegistry(Locale.ENGLISH));

    @Test
    @DisplayName("includes an intro paragraph and a status legend explaining what each status means")
    void includesIntroAndStatusLegend(@TempDir Path tempDir) throws IOException {
        ScenarioInfo listed = new ScenarioInfo("Authentication", "Scenario: Only a title", List.of());

        File outputFile = tempDir.resolve("features.adoc").toFile();
        writer.write(outputFile, List.of(listed), List.of());

        String content = Files.readString(outputFile.toPath(), StandardCharsets.UTF_8);
        assertThat(content)
                .contains("This document lists every `Scenario` and `Scenario Outline`")
                .contains("Every scenario is classified as exactly one of:")
                .contains("| Status | Meaning")
                .contains("No `Given`/`When`/`Then` steps have been written for them yet.")
                .contains("at least one step has no matching glue code yet.")
                .contains("Scenarios whose every step has matching glue code.");
    }

    @Test
    @DisplayName("reports scenarios under Listed, Defined and Implemented headings")
    void reportsScenariosUnderEachHeading(@TempDir Path tempDir) throws IOException {
        ScenarioInfo listed = new ScenarioInfo("Authentication", "Scenario: Only a title", List.of());
        ScenarioInfo defined = new ScenarioInfo(
                "Authentication", "Scenario: Has steps, no glue", List.of("an unimplemented step"));
        ScenarioInfo implemented = new ScenarioInfo(
                "Authentication", "Scenario: Fully wired up", List.of("an implemented step"));
        List<Expression> glueCode = List.of(expression("an implemented step"));

        File outputFile = tempDir.resolve("features.adoc").toFile();
        writer.write(outputFile, List.of(listed, defined, implemented), glueCode);

        String content = Files.readString(outputFile.toPath(), StandardCharsets.UTF_8);
        assertThat(content).contains("== Listed", "* Scenario: Only a title");
        assertThat(content).contains("== Defined", "* Scenario: Has steps, no glue");
        assertThat(content).contains("== Implemented", "* Scenario: Fully wired up");
    }

    @Test
    @DisplayName("groups scenarios under their feature within each status section")
    void groupsScenariosByFeatureWithinEachStatus(@TempDir Path tempDir) throws IOException {
        ScenarioInfo authImplemented = new ScenarioInfo(
                "Authentication", "Scenario: User logs in", List.of("has glue"));
        ScenarioInfo billingImplemented = new ScenarioInfo(
                "Billing", "Scenario: User pays an invoice", List.of("has glue"));
        List<Expression> glueCode = List.of(expression("has glue"));

        File outputFile = tempDir.resolve("features.adoc").toFile();
        writer.write(outputFile, List.of(authImplemented, billingImplemented), glueCode);

        String content = Files.readString(outputFile.toPath(), StandardCharsets.UTF_8);
        assertThat(content)
                .contains("== Implemented")
                .contains("=== Authentication")
                .contains("=== Billing");

        // The Authentication feature heading appears before its scenario, which appears
        // before the Billing feature heading.
        int authHeadingIndex = content.indexOf("=== Authentication");
        int authScenarioIndex = content.indexOf("* Scenario: User logs in");
        int billingHeadingIndex = content.indexOf("=== Billing");
        int billingScenarioIndex = content.indexOf("* Scenario: User pays an invoice");
        assertThat(authHeadingIndex).isLessThan(authScenarioIndex);
        assertThat(authScenarioIndex).isLessThan(billingHeadingIndex);
        assertThat(billingHeadingIndex).isLessThan(billingScenarioIndex);
    }

    @Test
    @DisplayName("groups multiple scenarios from the same feature under one feature heading")
    void groupsMultipleScenariosFromSameFeatureTogether(@TempDir Path tempDir) throws IOException {
        ScenarioInfo first = new ScenarioInfo("Authentication", "Scenario: First", List.of());
        ScenarioInfo second = new ScenarioInfo("Authentication", "Scenario: Second", List.of());

        File outputFile = tempDir.resolve("features.adoc").toFile();
        writer.write(outputFile, List.of(first, second), List.of());

        String content = Files.readString(outputFile.toPath(), StandardCharsets.UTF_8);
        assertThat(content.split("=== Authentication", -1)).hasSize(2);
        assertThat(content).contains("* Scenario: First", "* Scenario: Second");
    }

    @Test
    @DisplayName("summary table counts and percentages add up to 100%")
    void summaryTableCountsAndPercentagesAddUpTo100Percent(@TempDir Path tempDir) throws IOException {
        List<ScenarioInfo> scenarios = List.of(
                new ScenarioInfo("Feature", "Scenario: L1", List.of()),
                new ScenarioInfo("Feature", "Scenario: L2", List.of()),
                new ScenarioInfo("Feature", "Scenario: D1", List.of("no glue for this")),
                new ScenarioInfo("Feature", "Scenario: I1", List.of("has glue")),
                new ScenarioInfo("Feature", "Scenario: I2", List.of("has glue")),
                new ScenarioInfo("Feature", "Scenario: I3", List.of("has glue")));
        List<Expression> glueCode = List.of(expression("has glue"));

        File outputFile = tempDir.resolve("features.adoc").toFile();
        writer.write(outputFile, scenarios, glueCode);

        String content = Files.readString(outputFile.toPath(), StandardCharsets.UTF_8);
        assertThat(content)
                .contains("| Listed", "| 2", "| 33.3%")
                .contains("| Defined", "| 1", "| 16.7%")
                .contains("| Implemented", "| 3", "| 50.0%");
    }

    @Test
    @DisplayName("prints a placeholder message when there are no scenarios")
    void printsPlaceholderWhenNoScenarios(@TempDir Path tempDir) throws IOException {
        File outputFile = tempDir.resolve("features.adoc").toFile();
        writer.write(outputFile, List.of(), List.of());

        String content = Files.readString(outputFile.toPath(), StandardCharsets.UTF_8);
        assertThat(content).contains("No scenarios found.");
        assertThat(content).doesNotContain("Progress Summary");
    }

    @Test
    @DisplayName("prints a None placeholder for a heading with no scenarios, without a feature heading")
    void printsNonePlaceholderForEmptyHeading(@TempDir Path tempDir) throws IOException {
        ScenarioInfo implemented = new ScenarioInfo(
                "Authentication", "Scenario: Fully wired up", List.of("an implemented step"));
        List<Expression> glueCode = List.of(expression("an implemented step"));

        File outputFile = tempDir.resolve("features.adoc").toFile();
        writer.write(outputFile, List.of(implemented), glueCode);

        String content = Files.readString(outputFile.toPath(), StandardCharsets.UTF_8);
        assertThat(content).contains("== Listed", "_None._");
        // No feature sub-heading is printed for a status with no scenarios.
        String listedSection = content.substring(content.indexOf("== Listed"), content.indexOf("== Defined"));
        assertThat(listedSection).doesNotContain("===");
    }

    @Test
    @DisplayName("throws a GradleException when the output file cannot be written")
    void throwsWhenOutputFileCannotBeWritten(@TempDir Path tempDir) {
        File directoryAsFile = tempDir.toFile();

        assertThatThrownBy(() -> writer.write(directoryAsFile, List.of(), List.of()))
                .isInstanceOf(org.gradle.api.GradleException.class);
    }

    private Expression expression(String pattern) {
        return expressionFactory.createExpression(pattern);
    }
}

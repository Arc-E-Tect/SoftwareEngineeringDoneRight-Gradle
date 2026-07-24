package com.arc_e_tect.gradle.gherkin.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FeatureParser")
class FeatureParserTest {

    private FeatureParser parser;

    @BeforeEach
    void setUp() {
        parser = new FeatureParser();
    }

    @Test
    @DisplayName("parses Scenario and Scenario Outline titles from a feature file")
    void parsesScenarioAndScenarioOutlineTitles() throws Exception {
        File featureFile = fixtureFile("fixtures/login.feature");

        List<ScenarioInfo> scenarios = parser.parse(featureFile);

        assertThat(scenarios)
                .extracting(ScenarioInfo::title)
                .containsExactly(
                        "Scenario: User logs in successfully",
                        "Scenario Outline: User logs in with different credentials");
    }

    @Test
    @DisplayName("populates the enclosing Feature's title on every scenario")
    void populatesFeatureTitleOnEveryScenario() throws Exception {
        File featureFile = fixtureFile("fixtures/login.feature");

        List<ScenarioInfo> scenarios = parser.parse(featureFile);

        assertThat(scenarios)
                .extracting(ScenarioInfo::featureTitle)
                .containsOnly("User Authentication");
    }

    @Test
    @DisplayName("populates the enclosing Feature's title for scenarios nested inside a Rule")
    void populatesFeatureTitleForScenariosInsideRule() throws Exception {
        File featureFile = fixtureFile("fixtures/rules.feature");

        List<ScenarioInfo> scenarios = parser.parse(featureFile);

        assertThat(scenarios)
                .extracting(ScenarioInfo::featureTitle)
                .containsOnly("Rule-Based Scenarios");
    }

    @Test
    @DisplayName("extracts the Given-When-Then steps for each scenario")
    void extractsStepsForEachScenario() throws Exception {
        File featureFile = fixtureFile("fixtures/login.feature");

        List<ScenarioInfo> scenarios = parser.parse(featureFile);

        assertThat(scenarios.get(0).steps()).isNotEmpty();
    }

    @Test
    @DisplayName("returns a scenario with no steps when the scenario has none")
    void returnsEmptyStepsForStepLessScenario(@org.junit.jupiter.api.io.TempDir java.nio.file.Path tempDir) throws Exception {
        File featureFile = tempDir.resolve("stepless.feature").toFile();
        java.nio.file.Files.writeString(featureFile.toPath(),
                "Feature: Stepless\n\n  Scenario: Not yet fleshed out\n");

        List<ScenarioInfo> scenarios = parser.parse(featureFile);

        assertThat(scenarios).hasSize(1);
        assertThat(scenarios.get(0).steps()).isEmpty();
    }

    @Test
    @DisplayName("returns empty list for a feature file with no scenarios")
    void returnsEmptyListForFeatureWithNoScenarios() throws Exception {
        File featureFile = fixtureFile("fixtures/empty.feature");

        List<ScenarioInfo> scenarios = parser.parse(featureFile);

        assertThat(scenarios).isEmpty();
    }

    @Test
    @DisplayName("parses scenarios nested inside a Rule")
    void parsesScenariosInsideRule() throws Exception {
        File featureFile = fixtureFile("fixtures/rules.feature");

        List<ScenarioInfo> scenarios = parser.parse(featureFile);

        assertThat(scenarios)
                .extracting(ScenarioInfo::title)
                .containsExactly(
                        "Scenario: Premium user views protected page",
                        "Scenario Outline: Premium user accesses different content types");
    }

    @Test
    @DisplayName("returns empty list for a non-existent file")
    void returnsEmptyListForNonExistentFile() {
        File featureFile = new File("/does/not/exist.feature");

        List<ScenarioInfo> scenarios = parser.parse(featureFile);

        assertThat(scenarios).isEmpty();
    }

    @Test
    @DisplayName("returns empty list for a malformed feature file")
    void returnsEmptyListForMalformedFile(@org.junit.jupiter.api.io.TempDir java.nio.file.Path tempDir) throws Exception {
        File malformed = tempDir.resolve("broken.feature").toFile();
        java.nio.file.Files.writeString(malformed.toPath(), "this is not valid gherkin @@@ !!!");

        List<ScenarioInfo> scenarios = parser.parse(malformed);

        assertThat(scenarios).isEmpty();
    }

    @Test
    @DisplayName("returns unmodifiable list")
    void returnsUnmodifiableList() throws Exception {
        File featureFile = fixtureFile("fixtures/login.feature");

        List<ScenarioInfo> scenarios = parser.parse(featureFile);

        assertThat(scenarios).isUnmodifiable();
    }

    private File fixtureFile(String resourcePath) throws Exception {
        URL resource = getClass().getClassLoader().getResource(resourcePath);
        if (resource == null) {
            throw new IllegalArgumentException("Fixture not found on classpath: " + resourcePath);
        }
        return new File(resource.toURI());
    }
}

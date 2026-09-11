package com.arc_e_tect.gradle.gherkin.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
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
                        "Scenario Outline: User logs in with different credentials"
                                + " [username: admin, result: success]",
                        "Scenario Outline: User logs in with different credentials"
                                + " [username: guest, result: failure]");
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
    @DisplayName("extracts scenario steps regardless of using Given, And, But, or * keywords")
    void parserShouldExtractStepsWhenScenarioUsesGivenAndButAndAsteriskKeywords(
            @org.junit.jupiter.api.io.TempDir Path tempDir) throws Exception {
        File featureFile = tempDir.resolve("keywords.feature").toFile();
        Files.writeString(featureFile.toPath(), """
                Feature: Keyword support

                  Scenario: Rich keyword coverage
                    Given a customer exists
                    And the customer is active
                    But the customer is not premium
                    * the customer has a valid account number
                """);

        List<ScenarioInfo> scenarios = parser.parse(featureFile);

        assertThat(scenarios).hasSize(1);
        assertThat(scenarios.get(0).steps()).containsExactly(
                "a customer exists",
                "the customer is active",
                "the customer is not premium",
                "the customer has a valid account number");
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
                        "Scenario Outline: Premium user accesses different content types"
                                + " [content_type: articles]",
                        "Scenario Outline: Premium user accesses different content types"
                                + " [content_type: videos]");
    }

    @Test
    @DisplayName("expands a Scenario Outline into one scenario per Examples row")
    void expandsScenarioOutlineIntoOneScenarioPerExamplesRow(@TempDir Path tempDir) throws Exception {
        File featureFile = writeFeature(tempDir, """
                Feature: User authentication

                  Scenario Outline: User logs in as <username>
                    Given the login page is open
                    When the user submits "<username>" and "<password>"
                    Then the result is "<outcome>"

                    Examples:
                      | username | password | outcome |
                      | alice    | secret   | success |
                      | bob      | wrong    | failure |
                """);

        List<ScenarioInfo> scenarios = parser.parse(featureFile);

        assertThat(scenarios)
                .extracting(ScenarioInfo::title)
                .containsExactly(
                        "Scenario Outline: User logs in as alice",
                        "Scenario Outline: User logs in as bob");
    }

    @Test
    @DisplayName("substitutes each Examples row's values into that row's steps")
    void substitutesExampleValuesIntoSteps(@TempDir Path tempDir) throws Exception {
        File featureFile = writeFeature(tempDir, """
                Feature: User authentication

                  Scenario Outline: User logs in as <username>
                    Given the login page is open
                    When the user submits "<username>" and "<password>"
                    Then the result is "<outcome>"

                    Examples:
                      | username | password | outcome |
                      | alice    | secret   | success |
                      | bob      | wrong    | failure |
                """);

        List<ScenarioInfo> scenarios = parser.parse(featureFile);

        assertThat(scenarios.get(0).steps()).containsExactly(
                "the login page is open",
                "the user submits \"alice\" and \"secret\"",
                "the result is \"success\"");
        assertThat(scenarios.get(1).steps()).containsExactly(
                "the login page is open",
                "the user submits \"bob\" and \"wrong\"",
                "the result is \"failure\"");
    }

    @Test
    @DisplayName("appends each row's column values when the outline's name has no placeholders")
    void appendsColumnValuesWhenOutlineNameHasNoPlaceholders(@TempDir Path tempDir) throws Exception {
        File featureFile = writeFeature(tempDir, """
                Feature: User authentication

                  Scenario Outline: User logs in
                    Given the login page is open
                    When the user submits "<username>"

                    Examples:
                      | username | outcome |
                      | alice    | success |
                      | bob      | failure |
                """);

        List<ScenarioInfo> scenarios = parser.parse(featureFile);

        assertThat(scenarios)
                .extracting(ScenarioInfo::title)
                .containsExactly(
                        "Scenario Outline: User logs in [username: alice, outcome: success]",
                        "Scenario Outline: User logs in [username: bob, outcome: failure]");
    }

    @Test
    @DisplayName("appends each row's column values when the name's placeholders don't vary between rows")
    void appendsColumnValuesWhenNamePlaceholdersDoNotVaryBetweenRows(@TempDir Path tempDir) throws Exception {
        File featureFile = writeFeature(tempDir, """
                Feature: User authentication

                  Scenario Outline: User logs in as <role>
                    Given the login page is open
                    When the user submits "<username>"

                    Examples:
                      | role  | username |
                      | admin | alice    |
                      | admin | bob      |
                """);

        List<ScenarioInfo> scenarios = parser.parse(featureFile);

        assertThat(scenarios)
                .extracting(ScenarioInfo::title)
                .containsExactly(
                        "Scenario Outline: User logs in as admin [role: admin, username: alice]",
                        "Scenario Outline: User logs in as admin [role: admin, username: bob]");
    }

    @Test
    @DisplayName("appends each row's position when two rows hold identical values")
    void appendsRowPositionWhenTwoRowsHoldIdenticalValues(@TempDir Path tempDir) throws Exception {
        File featureFile = writeFeature(tempDir, """
                Feature: User authentication

                  Scenario Outline: User logs in
                    Given the login page is open
                    When the user submits "<username>"

                    Examples:
                      | username |
                      | alice    |
                      | alice    |
                """);

        List<ScenarioInfo> scenarios = parser.parse(featureFile);

        assertThat(scenarios)
                .extracting(ScenarioInfo::title)
                .containsExactly(
                        "Scenario Outline: User logs in [example 1]",
                        "Scenario Outline: User logs in [example 2]");
    }

    @Test
    @DisplayName("expands every Examples block of an outline, in document order")
    void expandsEveryExamplesBlockOfAnOutline(@TempDir Path tempDir) throws Exception {
        File featureFile = writeFeature(tempDir, """
                Feature: User authentication

                  Scenario Outline: User logs in as <username>
                    Given the login page is open

                    Examples: Accepted
                      | username |
                      | alice    |

                    Examples: Rejected
                      | username |
                      | bob      |
                      | carol    |
                """);

        List<ScenarioInfo> scenarios = parser.parse(featureFile);

        assertThat(scenarios)
                .extracting(ScenarioInfo::title)
                .containsExactly(
                        "Scenario Outline: User logs in as alice",
                        "Scenario Outline: User logs in as bob",
                        "Scenario Outline: User logs in as carol");
    }

    @Test
    @DisplayName("keeps an outline with no Examples rows as a single scenario with unsubstituted steps")
    void keepsOutlineWithoutExamplesRowsAsSingleScenario(@TempDir Path tempDir) throws Exception {
        File featureFile = writeFeature(tempDir, """
                Feature: User authentication

                  Scenario Outline: User logs in as <username>
                    Given the login page is open
                    When the user submits "<username>"

                    Examples:
                      | username |
                """);

        List<ScenarioInfo> scenarios = parser.parse(featureFile);

        assertThat(scenarios).hasSize(1);
        assertThat(scenarios.get(0).title()).isEqualTo("Scenario Outline: User logs in as <username>");
        assertThat(scenarios.get(0).steps()).containsExactly(
                "the login page is open",
                "the user submits \"<username>\"");
    }

    @Test
    @DisplayName("leaves a placeholder naming no Examples column exactly as written")
    void leavesPlaceholderNamingNoExamplesColumnAsWritten(@TempDir Path tempDir) throws Exception {
        File featureFile = writeFeature(tempDir, """
                Feature: User authentication

                  Scenario Outline: User logs in as <username>
                    Given the login page is open
                    When the user submits "<username>" and "<password>"

                    Examples:
                      | username |
                      | alice    |
                """);

        List<ScenarioInfo> scenarios = parser.parse(featureFile);

        assertThat(scenarios).hasSize(1);
        assertThat(scenarios.get(0).steps()).containsExactly(
                "the login page is open",
                "the user submits \"alice\" and \"<password>\"");
    }

    @Test
    @DisplayName("populates the enclosing Feature's title on every expanded Examples row")
    void populatesFeatureTitleOnEveryExpandedRow(@TempDir Path tempDir) throws Exception {
        File featureFile = writeFeature(tempDir, """
                Feature: User authentication

                  Scenario Outline: User logs in as <username>
                    Given the login page is open

                    Examples:
                      | username |
                      | alice    |
                      | bob      |
                """);

        List<ScenarioInfo> scenarios = parser.parse(featureFile);

        assertThat(scenarios).hasSize(2);
        assertThat(scenarios)
                .extracting(ScenarioInfo::featureTitle)
                .containsOnly("User authentication");
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

    private File writeFeature(Path tempDir, String content) throws Exception {
        File featureFile = tempDir.resolve("outline.feature").toFile();
        Files.writeString(featureFile.toPath(), content);
        return featureFile;
    }

    private File fixtureFile(String resourcePath) throws Exception {
        URL resource = getClass().getClassLoader().getResource(resourcePath);
        if (resource == null) {
            throw new IllegalArgumentException("Fixture not found on classpath: " + resourcePath);
        }
        return new File(resource.toURI());
    }
}

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

        List<String> titles = parser.parse(featureFile);

        assertThat(titles)
                .hasSize(2)
                .containsExactly(
                        "Scenario: User logs in successfully",
                        "Scenario Outline: User logs in with different credentials");
    }

    @Test
    @DisplayName("returns empty list for a feature file with no scenarios")
    void returnsEmptyListForFeatureWithNoScenarios() throws Exception {
        File featureFile = fixtureFile("fixtures/empty.feature");

        List<String> titles = parser.parse(featureFile);

        assertThat(titles).isEmpty();
    }

    @Test
    @DisplayName("parses scenarios nested inside a Rule")
    void parsesScenariosInsideRule() throws Exception {
        File featureFile = fixtureFile("fixtures/rules.feature");

        List<String> titles = parser.parse(featureFile);

        assertThat(titles)
                .hasSize(2)
                .containsExactly(
                        "Scenario: Premium user views protected page",
                        "Scenario Outline: Premium user accesses different content types");
    }

    @Test
    @DisplayName("returns empty list for a non-existent file")
    void returnsEmptyListForNonExistentFile() {
        File featureFile = new File("/does/not/exist.feature");

        List<String> titles = parser.parse(featureFile);

        assertThat(titles).isEmpty();
    }

    @Test
    @DisplayName("returns empty list for a malformed feature file")
    void returnsEmptyListForMalformedFile(@org.junit.jupiter.api.io.TempDir java.nio.file.Path tempDir) throws Exception {
        File malformed = tempDir.resolve("broken.feature").toFile();
        java.nio.file.Files.writeString(malformed.toPath(), "this is not valid gherkin @@@ !!!");

        List<String> titles = parser.parse(malformed);

        assertThat(titles).isEmpty();
    }

    @Test
    @DisplayName("returns unmodifiable list")
    void returnsUnmodifiableList() throws Exception {
        File featureFile = fixtureFile("fixtures/login.feature");

        List<String> titles = parser.parse(featureFile);

        assertThat(titles).isUnmodifiable();
    }

    private File fixtureFile(String resourcePath) throws Exception {
        URL resource = getClass().getClassLoader().getResource(resourcePath);
        if (resource == null) {
            throw new IllegalArgumentException("Fixture not found on classpath: " + resourcePath);
        }
        return new File(resource.toURI());
    }
}

package com.arc_e_tect.gradle.gherkin.indexing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FeatureIndexer")
class FeatureIndexerTest {

    @TempDir
    Path tempDir;

    private final FeatureIndexer indexer = new FeatureIndexer();

    @Test
    @DisplayName("mode OFF leaves feature and scenario titles untouched")
    void offModeLeavesTitlesUntouched() throws IOException {
        File file = writeFeature("authentication.feature",
                "Feature: User authentication\n\n  Scenario: User logs in\n    Given a user\n");

        indexer.reindex(List.of(file), IndexingMode.OFF);

        assertThat(content(file))
                .contains("Feature: User authentication")
                .contains("Scenario: User logs in");
    }

    @Test
    @DisplayName("mode FEATURE numbers features in the order given, leaves scenarios untouched")
    void featureModeNumbersFeaturesInGivenOrder() throws IOException {
        File auth = writeFeature("authentication.feature",
                "Feature: User authentication\n\n  Scenario: User logs in\n    Given a user\n");
        File invoice = writeFeature("invoice.feature",
                "Feature: Invoice payment\n\n  Scenario: User pays an invoice\n    Given an invoice\n");

        indexer.reindex(List.of(auth, invoice), IndexingMode.FEATURE);

        assertThat(content(auth))
                .contains("Feature: 1 - User authentication")
                .contains("Scenario: User logs in");
        assertThat(content(invoice))
                .contains("Feature: 2 - Invoice payment")
                .contains("Scenario: User pays an invoice");
    }

    @Test
    @DisplayName("mode SCENARIO numbers scenarios continuously across files, leaves features untouched")
    void scenarioModeNumbersScenariosContinuously() throws IOException {
        File auth = writeFeature("authentication.feature", """
                Feature: User authentication

                  Scenario: User logs in
                    Given a user

                  Scenario: User resets password
                    Given a user
                """);
        File invoice = writeFeature("invoice.feature",
                "Feature: Invoice payment\n\n  Scenario: User pays an invoice\n    Given an invoice\n");

        indexer.reindex(List.of(auth, invoice), IndexingMode.SCENARIO);

        assertThat(content(auth))
                .contains("Feature: User authentication")
                .contains("Scenario: 1 - User logs in")
                .contains("Scenario: 2 - User resets password");
        assertThat(content(invoice))
                .contains("Feature: Invoice payment")
                .contains("Scenario: 3 - User pays an invoice");
    }

    @Test
    @DisplayName("mode ALL numbers features and numbers scenarios per feature as featureNumber.scenarioNumber")
    void allModeNumbersFeaturesAndScenariosPerFeature() throws IOException {
        File auth = writeFeature("authentication.feature", """
                Feature: User authentication

                  Scenario: User logs in
                    Given a user

                  Scenario: User resets password
                    Given a user
                """);
        File invoice = writeFeature("invoice.feature",
                "Feature: Invoice payment\n\n  Scenario: User pays an invoice\n    Given an invoice\n");

        indexer.reindex(List.of(auth, invoice), IndexingMode.ALL);

        assertThat(content(auth))
                .contains("Feature: 1 - User authentication")
                .contains("Scenario: 1.1 - User logs in")
                .contains("Scenario: 1.2 - User resets password");
        assertThat(content(invoice))
                .contains("Feature: 2 - Invoice payment")
                .contains("Scenario: 2.1 - User pays an invoice");
    }

    @Test
    @DisplayName("numbers files in the order given, not re-sorted alphabetically")
    void numbersFilesInGivenOrderNotAlphabetically() throws IOException {
        File zFile = writeFeature("z.feature",
                "Feature: Z Feature\n\n  Scenario: Z scenario\n    Given z\n");
        File aFile = writeFeature("a.feature",
                "Feature: A Feature\n\n  Scenario: A scenario\n    Given a\n");

        // Given in z-then-a order: the caller (not the indexer) is responsible for ordering.
        indexer.reindex(List.of(zFile, aFile), IndexingMode.FEATURE);

        assertThat(content(zFile)).contains("Feature: 1 - Z Feature");
        assertThat(content(aFile)).contains("Feature: 2 - A Feature");
    }

    @Test
    @DisplayName("numbers Scenario Outline the same as Scenario")
    void numbersScenarioOutline() throws IOException {
        File file = writeFeature("outline.feature", """
                Feature: Sample

                  Scenario Outline: User logs in with <role>
                    Given a "<role>" user

                    Examples:
                      | role  |
                      | admin |
                """);

        indexer.reindex(List.of(file), IndexingMode.ALL);

        assertThat(content(file)).contains("Scenario Outline: 1.1 - User logs in with <role>");
    }

    @Test
    @DisplayName("numbers scenarios nested inside a Rule block, preserving their indentation")
    void numbersScenariosInsideRule() throws IOException {
        File file = writeFeature("rules.feature", """
                Feature: Rule-Based Scenarios

                  Rule: Registered users can access premium content

                    Scenario: Premium user views protected page
                      Given a premium user
                """);

        indexer.reindex(List.of(file), IndexingMode.SCENARIO);

        assertThat(content(file)).contains("    Scenario: 1 - Premium user views protected page");
    }

    @Test
    @DisplayName("switching from ALL to OFF removes all numbering")
    void switchingToOffRemovesNumbering() throws IOException {
        File auth = writeFeature("authentication.feature",
                "Feature: User authentication\n\n  Scenario: User logs in\n    Given a user\n");
        File invoice = writeFeature("invoice.feature",
                "Feature: Invoice payment\n\n  Scenario: User pays an invoice\n    Given an invoice\n");
        indexer.reindex(List.of(auth, invoice), IndexingMode.ALL);

        indexer.reindex(List.of(auth, invoice), IndexingMode.OFF);

        assertThat(content(auth))
                .contains("Feature: User authentication")
                .contains("Scenario: User logs in")
                .doesNotContain("1 -")
                .doesNotContain("1.1 -");
        assertThat(content(invoice))
                .contains("Feature: Invoice payment")
                .contains("Scenario: User pays an invoice");
    }

    @Test
    @DisplayName("switching from SCENARIO to FEATURE removes scenario numbers and adds feature numbers")
    void switchingModesReplacesNumbering() throws IOException {
        File auth = writeFeature("authentication.feature",
                "Feature: User authentication\n\n  Scenario: User logs in\n    Given a user\n");
        indexer.reindex(List.of(auth), IndexingMode.SCENARIO);
        assertThat(content(auth)).contains("Scenario: 1 - User logs in");

        indexer.reindex(List.of(auth), IndexingMode.FEATURE);

        assertThat(content(auth))
                .contains("Feature: 1 - User authentication")
                .contains("Scenario: User logs in")
                .doesNotContain("Scenario: 1 -");
    }

    @Test
    @DisplayName("re-running the same mode is idempotent and does not change file content")
    void reindexingWithSameModeIsIdempotent() throws IOException {
        File auth = writeFeature("authentication.feature",
                "Feature: User authentication\n\n  Scenario: User logs in\n    Given a user\n");
        indexer.reindex(List.of(auth), IndexingMode.ALL);
        String firstPass = content(auth);

        indexer.reindex(List.of(auth), IndexingMode.ALL);

        assertThat(content(auth)).isEqualTo(firstPass);
    }

    @Test
    @DisplayName("does not renumber unrelated lines that merely contain the word Scenario")
    void doesNotTouchUnrelatedLines() throws IOException {
        File file = writeFeature("sample.feature", """
                Feature: Sample

                  Scenario: User logs in
                    Given a user with role "Scenario: not a keyword"
                """);

        indexer.reindex(List.of(file), IndexingMode.SCENARIO);

        assertThat(content(file))
                .contains("Scenario: 1 - User logs in")
                .contains("Given a user with role \"Scenario: not a keyword\"");
    }

    private File writeFeature(String name, String content) throws IOException {
        File file = tempDir.resolve(name).toFile();
        Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
        return file;
    }

    private String content(File file) throws IOException {
        return Files.readString(file.toPath(), StandardCharsets.UTF_8);
    }
}

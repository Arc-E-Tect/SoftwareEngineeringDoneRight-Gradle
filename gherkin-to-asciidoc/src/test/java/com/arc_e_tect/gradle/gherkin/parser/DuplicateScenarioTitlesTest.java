package com.arc_e_tect.gradle.gherkin.parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DuplicateScenarioTitles")
class DuplicateScenarioTitlesTest {

    private final DuplicateScenarioTitles duplicates = new DuplicateScenarioTitles();

    @Test
    @DisplayName("finds nothing when every scenario title is unique")
    void findsNothingWhenEveryTitleIsUnique() {
        List<DuplicateScenarioTitles.ScenarioOccurrence> occurrences = List.of(
                occurrence("Scenario: User logs in", "login.feature"),
                occurrence("Scenario: User logs out", "login.feature"));

        assertThat(duplicates.find(occurrences)).isEmpty();
    }

    @Test
    @DisplayName("finds a duplicate title shared across two different feature files")
    void findsDuplicateTitleAcrossDifferentFiles() {
        List<DuplicateScenarioTitles.ScenarioOccurrence> occurrences = List.of(
                occurrence("Scenario: User logs in", "login.feature"),
                occurrence("Scenario: User logs in", "auth.feature"));

        List<DuplicateScenarioTitles.Duplicate> found = duplicates.find(occurrences);

        assertThat(found).hasSize(1);
        assertThat(found.get(0).title()).isEqualTo("Scenario: User logs in");
        assertThat(found.get(0).occurrences()).hasSize(2);
        assertThat(found.get(0).occurrences().stream().map(o -> o.featureFile().getName()))
                .containsExactly("login.feature", "auth.feature");
    }

    @Test
    @DisplayName("finds a duplicate title shared twice within the same feature file")
    void findsDuplicateTitleWithinTheSameFile() {
        List<DuplicateScenarioTitles.ScenarioOccurrence> occurrences = List.of(
                occurrence("Scenario: User logs in", "login.feature"),
                occurrence("Scenario: User logs in", "login.feature"));

        assertThat(duplicates.find(occurrences)).hasSize(1);
    }

    @Test
    @DisplayName("finds every duplicate title, not just the first")
    void findsEveryDuplicateTitle() {
        List<DuplicateScenarioTitles.ScenarioOccurrence> occurrences = List.of(
                occurrence("Scenario: User logs in", "login.feature"),
                occurrence("Scenario: User logs in", "auth.feature"),
                occurrence("Scenario: User logs out", "login.feature"),
                occurrence("Scenario: User logs out", "auth.feature"),
                occurrence("Scenario: Password reset", "login.feature"));

        assertThat(duplicates.find(occurrences))
                .extracting(DuplicateScenarioTitles.Duplicate::title)
                .containsExactly("Scenario: User logs in", "Scenario: User logs out");
    }

    @Test
    @DisplayName("treats titles as colliding under the same normalization ScenarioFingerprint uses, ignoring index prefixes")
    void treatsIndexedAndUnindexedTitlesAsColliding() {
        List<DuplicateScenarioTitles.ScenarioOccurrence> occurrences = List.of(
                occurrence("Scenario: 1 - User logs in", "login.feature"),
                occurrence("Scenario: User logs in", "auth.feature"));

        assertThat(duplicates.find(occurrences)).hasSize(1);
    }

    private DuplicateScenarioTitles.ScenarioOccurrence occurrence(String title, String featureFileName) {
        return new DuplicateScenarioTitles.ScenarioOccurrence(
                new ScenarioInfo("Some feature", title, List.of()), new File(featureFileName));
    }
}

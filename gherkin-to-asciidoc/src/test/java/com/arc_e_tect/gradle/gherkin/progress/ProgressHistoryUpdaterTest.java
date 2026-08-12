package com.arc_e_tect.gradle.gherkin.progress;

import com.arc_e_tect.gradle.gherkin.parser.ScenarioInfo;
import io.cucumber.cucumberexpressions.Expression;
import io.cucumber.cucumberexpressions.ExpressionFactory;
import io.cucumber.cucumberexpressions.ParameterTypeRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProgressHistoryUpdater")
class ProgressHistoryUpdaterTest {

    private final ProgressHistoryUpdater updater = new ProgressHistoryUpdater();
    private final ScenarioFingerprint fingerprinter = new ScenarioFingerprint();
    private final ExpressionFactory expressionFactory =
            new ExpressionFactory(new ParameterTypeRegistry(Locale.ENGLISH));

    @Test
    @DisplayName("a brand new scenario that is already implemented on its first run gets only implementedAt set")
    void newScenarioStartingMidwayGetsOnlyItsCurrentStageStamped() {
        ScenarioInfo scenario = new ScenarioInfo(
                "Authentication", "Scenario: User logs in", List.of("an implemented step"));
        List<Expression> glueCode = List.of(expression("an implemented step"));
        Instant now = Instant.parse("2026-01-01T00:00:00Z");

        Map<String, ScenarioProgressRecord> updated = updater.update(Map.of(), List.of(scenario), glueCode, now);

        String fingerprint = fingerprinter.fingerprint(scenario.title());
        assertThat(updated.get(fingerprint).listedAt()).isNull();
    }

    @Test
    @DisplayName("a brand new scenario that is already implemented on its first run gets no definedAt either")
    void newScenarioStartingMidwayGetsNoIntermediateStageStamped() {
        ScenarioInfo scenario = new ScenarioInfo(
                "Authentication", "Scenario: User logs in", List.of("an implemented step"));
        List<Expression> glueCode = List.of(expression("an implemented step"));
        Instant now = Instant.parse("2026-01-01T00:00:00Z");

        Map<String, ScenarioProgressRecord> updated = updater.update(Map.of(), List.of(scenario), glueCode, now);

        String fingerprint = fingerprinter.fingerprint(scenario.title());
        assertThat(updated.get(fingerprint).definedAt()).isNull();
    }

    @Test
    @DisplayName("a brand new scenario that is already implemented on its first run gets implementedAt stamped with now")
    void newScenarioStartingMidwayGetsImplementedAtStampedWithNow() {
        ScenarioInfo scenario = new ScenarioInfo(
                "Authentication", "Scenario: User logs in", List.of("an implemented step"));
        List<Expression> glueCode = List.of(expression("an implemented step"));
        Instant now = Instant.parse("2026-01-01T00:00:00Z");

        Map<String, ScenarioProgressRecord> updated = updater.update(Map.of(), List.of(scenario), glueCode, now);

        String fingerprint = fingerprinter.fingerprint(scenario.title());
        assertThat(updated.get(fingerprint).implementedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("a scenario that regresses from implemented back to defined keeps its original implementedAt")
    void regressingScenarioKeepsItsOriginalImplementedAt() {
        String scenarioTitle = "Scenario: User logs in";
        String fingerprint = fingerprinter.fingerprint(scenarioTitle);
        Instant originalImplementedAt = Instant.parse("2026-01-01T00:00:00Z");
        ScenarioProgressRecord existing = new ScenarioProgressRecord(
                fingerprint, "User logs in", "Authentication",
                null, null, originalImplementedAt, originalImplementedAt, null);
        // Glue code no longer matches, so the scenario is now classified as DEFINED, not IMPLEMENTED.
        ScenarioInfo scenario = new ScenarioInfo("Authentication", scenarioTitle, List.of("an unimplemented step"));
        Instant now = Instant.parse("2026-03-01T00:00:00Z");

        Map<String, ScenarioProgressRecord> updated =
                updater.update(Map.of(fingerprint, existing), List.of(scenario), List.of(), now);

        assertThat(updated.get(fingerprint).implementedAt()).isEqualTo(originalImplementedAt);
    }

    @Test
    @DisplayName("a scenario that regresses from implemented back to defined gets definedAt stamped for the first time")
    void regressingScenarioGetsDefinedAtStampedForTheFirstTime() {
        String scenarioTitle = "Scenario: User logs in";
        String fingerprint = fingerprinter.fingerprint(scenarioTitle);
        Instant originalImplementedAt = Instant.parse("2026-01-01T00:00:00Z");
        ScenarioProgressRecord existing = new ScenarioProgressRecord(
                fingerprint, "User logs in", "Authentication",
                null, null, originalImplementedAt, originalImplementedAt, null);
        ScenarioInfo scenario = new ScenarioInfo("Authentication", scenarioTitle, List.of("an unimplemented step"));
        Instant now = Instant.parse("2026-03-01T00:00:00Z");

        Map<String, ScenarioProgressRecord> updated =
                updater.update(Map.of(fingerprint, existing), List.of(scenario), List.of(), now);

        assertThat(updated.get(fingerprint).definedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("a scenario absent from the current run keeps its record and gets removedAt stamped")
    void absentScenarioGetsRemovedAtStamped() {
        String fingerprint = "aaaa000000000000";
        Instant lastSeenAt = Instant.parse("2026-01-01T00:00:00Z");
        ScenarioProgressRecord existing = new ScenarioProgressRecord(
                fingerprint, "User logs in", "Authentication", lastSeenAt, null, null, lastSeenAt, null);
        Instant now = Instant.parse("2026-02-01T00:00:00Z");

        Map<String, ScenarioProgressRecord> updated =
                updater.update(Map.of(fingerprint, existing), List.of(), List.of(), now);

        assertThat(updated.get(fingerprint).removedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("a removed-then-reappeared scenario has its removedAt cleared")
    void removedThenReappearedScenarioHasRemovedAtCleared() {
        String scenarioTitle = "Scenario: User logs in";
        String fingerprint = fingerprinter.fingerprint(scenarioTitle);
        Instant removedAt = Instant.parse("2026-02-01T00:00:00Z");
        ScenarioProgressRecord existing = new ScenarioProgressRecord(
                fingerprint, "User logs in", "Authentication",
                Instant.parse("2026-01-01T00:00:00Z"), null, null,
                Instant.parse("2026-01-01T00:00:00Z"), removedAt);
        ScenarioInfo scenario = new ScenarioInfo("Authentication", scenarioTitle, List.of());
        Instant now = Instant.parse("2026-03-01T00:00:00Z");

        Map<String, ScenarioProgressRecord> updated =
                updater.update(Map.of(fingerprint, existing), List.of(scenario), List.of(), now);

        assertThat(updated.get(fingerprint).removedAt()).isNull();
    }

    @Test
    @DisplayName("a removed-then-reappeared scenario keeps its original listedAt")
    void removedThenReappearedScenarioKeepsOriginalListedAt() {
        String scenarioTitle = "Scenario: User logs in";
        String fingerprint = fingerprinter.fingerprint(scenarioTitle);
        Instant originalListedAt = Instant.parse("2026-01-01T00:00:00Z");
        ScenarioProgressRecord existing = new ScenarioProgressRecord(
                fingerprint, "User logs in", "Authentication",
                originalListedAt, null, null, originalListedAt, Instant.parse("2026-02-01T00:00:00Z"));
        ScenarioInfo scenario = new ScenarioInfo("Authentication", scenarioTitle, List.of());
        Instant now = Instant.parse("2026-03-01T00:00:00Z");

        Map<String, ScenarioProgressRecord> updated =
                updater.update(Map.of(fingerprint, existing), List.of(scenario), List.of(), now);

        assertThat(updated.get(fingerprint).listedAt()).isEqualTo(originalListedAt);
    }

    @Test
    @DisplayName("a record already marked removed is not re-stamped with a later removedAt")
    void alreadyRemovedRecordKeepsItsOriginalRemovedAt() {
        String fingerprint = "aaaa000000000000";
        Instant originalRemovedAt = Instant.parse("2026-02-01T00:00:00Z");
        ScenarioProgressRecord existing = new ScenarioProgressRecord(
                fingerprint, "User logs in", "Authentication",
                Instant.parse("2026-01-01T00:00:00Z"), null, null,
                Instant.parse("2026-01-01T00:00:00Z"), originalRemovedAt);
        Instant now = Instant.parse("2026-04-01T00:00:00Z");

        Map<String, ScenarioProgressRecord> updated =
                updater.update(Map.of(fingerprint, existing), List.of(), List.of(), now);

        assertThat(updated.get(fingerprint).removedAt()).isEqualTo(originalRemovedAt);
    }

    @Test
    @DisplayName("refreshes featureTitle to the current run's value even though it isn't part of the key")
    void refreshesFeatureTitleToCurrentRunValue() {
        String scenarioTitle = "Scenario: User logs in";
        String fingerprint = fingerprinter.fingerprint(scenarioTitle);
        ScenarioProgressRecord existing = new ScenarioProgressRecord(
                fingerprint, "User logs in", "Authentication",
                Instant.parse("2026-01-01T00:00:00Z"), null, null,
                Instant.parse("2026-01-01T00:00:00Z"), null);
        ScenarioInfo scenario = new ScenarioInfo("Sign-in", scenarioTitle, List.of());
        Instant now = Instant.parse("2026-03-01T00:00:00Z");

        Map<String, ScenarioProgressRecord> updated =
                updater.update(Map.of(fingerprint, existing), List.of(scenario), List.of(), now);

        assertThat(updated.get(fingerprint).featureTitle()).isEqualTo("Sign-in");
    }

    @Test
    @DisplayName("refreshes lastSeenAt to now for every scenario present in the current run")
    void refreshesLastSeenAtToNow() {
        ScenarioInfo scenario = new ScenarioInfo("Authentication", "Scenario: User logs in", List.of());
        Instant now = Instant.parse("2026-05-01T00:00:00Z");

        Map<String, ScenarioProgressRecord> updated = updater.update(Map.of(), List.of(scenario), List.of(), now);

        String fingerprint = fingerprinter.fingerprint(scenario.title());
        assertThat(updated.get(fingerprint).lastSeenAt()).isEqualTo(now);
    }

    private Expression expression(String pattern) {
        return expressionFactory.createExpression(pattern);
    }
}

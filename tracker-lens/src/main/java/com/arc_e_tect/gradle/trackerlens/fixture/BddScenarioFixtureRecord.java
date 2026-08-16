package com.arc_e_tect.gradle.trackerlens.fixture;

import java.time.Instant;

/**
 * One generated row of the BDD-scenario progress history, in
 * {@link com.arc_e_tect.gradle.trackerlens.tracker.GherkinScenarioTrackerSource}'s own field shape
 * - {@code fingerprint}, {@code scenarioName}, {@code featureTitle}, {@code listedAt},
 * {@code definedAt}, {@code implementedAt}, {@code lastSeenAt}, {@code removedAt}.
 *
 * @param fingerprint  stable identifier, unique within the generated fixture
 * @param scenarioName the scenario's display name
 * @param featureTitle the enclosing feature's title
 * @param listedAt     when the scenario was first discovered, or {@code null}
 * @param definedAt    when the scenario first gained a Gherkin definition, or {@code null} - may
 *                      be unset even when {@code implementedAt} is set, when a scenario was first
 *                      observed already implemented
 * @param implementedAt when a step-definition implementation was first detected, or {@code null}
 * @param lastSeenAt   when this scenario was last observed present
 * @param removedAt    when this scenario was first observed missing, or {@code null} while still
 *                      present
 */
public record BddScenarioFixtureRecord(
        String fingerprint,
        String scenarioName,
        String featureTitle,
        Instant listedAt,
        Instant definedAt,
        Instant implementedAt,
        Instant lastSeenAt,
        Instant removedAt) {
}

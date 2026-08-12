package com.arc_e_tect.gradle.gherkin.progress;

import java.time.Instant;

/**
 * Persisted, per-scenario history of when a scenario first reached each {@link ScenarioStatus}
 * stage, keyed by {@link ScenarioFingerprint#fingerprint(String)} rather than by feature file
 * location - so the history survives a scenario being moved between feature files.
 *
 * @param fingerprint   the stable identifier computed by {@link ScenarioFingerprint}
 * @param scenarioName  the scenario's current display name; not part of the key, refreshed on
 *                      every run so a renamed-in-place scenario still shows its latest title
 * @param featureTitle  the scenario's current enclosing feature title; not part of the key,
 *                      refreshed on every run so a scenario moved to a different feature shows
 *                      that feature's title
 * @param listedAt      when the scenario was first observed as {@link ScenarioStatus#LISTED},
 *                      or {@code null} if it never has been
 * @param definedAt     when the scenario was first observed as {@link ScenarioStatus#DEFINED},
 *                      or {@code null} if it never has been
 * @param implementedAt when the scenario was first observed as {@link ScenarioStatus#IMPLEMENTED},
 *                      or {@code null} if it never has been
 * @param lastSeenAt    when the scenario was last present in a run, or {@code null} for a record
 *                      that has never actually been seen (should not occur in practice)
 * @param removedAt     when the scenario was first observed missing from a run, or {@code null}
 *                      while it's still present in the current scan
 */
public record ScenarioProgressRecord(
        String fingerprint,
        String scenarioName,
        String featureTitle,
        Instant listedAt,
        Instant definedAt,
        Instant implementedAt,
        Instant lastSeenAt,
        Instant removedAt) {
}

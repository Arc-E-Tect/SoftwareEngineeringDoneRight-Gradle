package com.arc_e_tect.gradle.gherkin.parser;

import com.arc_e_tect.gradle.gherkin.progress.ScenarioFingerprint;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Finds scenarios whose title collides under {@link ScenarioFingerprint} - the identity used to key
 * persisted progress history - regardless of whether the colliding scenarios were parsed from the
 * same {@code .feature} file or different ones.
 *
 * <p>{@link ScenarioFingerprint} deliberately hashes a scenario's title alone, so that a scenario
 * keeps its history when moved between feature files. That same title-only identity means two
 * distinct scenarios that merely happen to share a title are indistinguishable to it: within a
 * single run, whichever is processed last silently overwrites the other's entry in the persisted
 * history map, and across runs a stage timestamp genuinely earned by one can permanently leak onto
 * the other, since history timestamps are never cleared once set. Rejecting the collision outright,
 * before either the report or the history file is written, is simpler and safer than trying to
 * disambiguate the two scenarios after the fact.</p>
 */
public final class DuplicateScenarioTitles {

    private final ScenarioFingerprint fingerprinter = new ScenarioFingerprint();

    /** Creates a new {@code DuplicateScenarioTitles}. */
    public DuplicateScenarioTitles() {}

    /**
     * Finds every group of two or more {@code occurrences} whose scenario title collides under
     * {@link ScenarioFingerprint}.
     *
     * @param occurrences every scenario parsed this run, paired with the feature file it came from
     * @return one {@link Duplicate} per colliding title, in the order each title was first
     *         encountered; empty when no titles collide
     */
    public List<Duplicate> find(List<ScenarioOccurrence> occurrences) {
        Map<String, List<ScenarioOccurrence>> byFingerprint = new LinkedHashMap<>();
        for (ScenarioOccurrence occurrence : occurrences) {
            String fingerprint = fingerprinter.fingerprint(occurrence.scenario().title());
            byFingerprint.computeIfAbsent(fingerprint, key -> new ArrayList<>()).add(occurrence);
        }

        List<Duplicate> duplicates = new ArrayList<>();
        for (List<ScenarioOccurrence> group : byFingerprint.values()) {
            if (group.size() > 1) {
                duplicates.add(new Duplicate(group.get(0).scenario().title(), List.copyOf(group)));
            }
        }
        return duplicates;
    }

    /**
     * A single scenario as parsed from a particular feature file.
     *
     * @param scenario    the parsed scenario
     * @param featureFile the {@code .feature} file it was parsed from
     */
    public record ScenarioOccurrence(ScenarioInfo scenario, File featureFile) {}

    /**
     * A scenario title shared by two or more {@link ScenarioOccurrence}s.
     *
     * @param title       the shared scenario title
     * @param occurrences every occurrence of {@code title}, in document order; always has at least
     *                    two elements
     */
    public record Duplicate(String title, List<ScenarioOccurrence> occurrences) {}
}

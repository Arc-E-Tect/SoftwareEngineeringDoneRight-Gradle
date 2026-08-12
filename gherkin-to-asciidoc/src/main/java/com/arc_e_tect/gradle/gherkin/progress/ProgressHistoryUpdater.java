package com.arc_e_tect.gradle.gherkin.progress;

import com.arc_e_tect.gradle.gherkin.parser.ScenarioInfo;
import io.cucumber.cucumberexpressions.Expression;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Produces the next {@link ScenarioProgressRecord} map to persist, from the previously persisted
 * map and the current run's scenarios.
 *
 * <p>Every current-run scenario keeps (or starts) a record keyed by
 * {@link ScenarioFingerprint#fingerprint(String)}: whichever of {@code listedAt}/{@code definedAt}/
 * {@code implementedAt} matches the scenario's {@link ScenarioClassifier current status} is stamped
 * with {@code now} the first time that status is observed, and is never overwritten afterwards -
 * so a scenario that jumps straight from {@code listed} to {@code implemented} never gains a
 * {@code definedAt}. Every previously persisted record whose fingerprint is no longer present in
 * the current run is kept, unchanged, with {@code removedAt} stamped the first time it goes
 * missing. Records are never deleted.</p>
 */
public class ProgressHistoryUpdater {

    private static final Pattern KEYWORD_PREFIX = Pattern.compile("^(?:Scenario Outline|Scenario):\\s*(.*)$");

    private final ScenarioFingerprint fingerprinter = new ScenarioFingerprint();
    private final ScenarioClassifier classifier = new ScenarioClassifier();

    /** Creates a new {@code ProgressHistoryUpdater}. */
    public ProgressHistoryUpdater() {}

    /**
     * Computes the updated history map to persist.
     *
     * @param previous  the previously persisted history, keyed by fingerprint; empty on a first run
     * @param scenarios the current run's scenarios
     * @param glueCode  the current run's step definition patterns, used to classify each scenario
     * @param now       the instant to stamp newly-reached stages and newly-observed removals with
     * @return the updated history map, keyed by fingerprint
     */
    public Map<String, ScenarioProgressRecord> update(
            Map<String, ScenarioProgressRecord> previous, List<ScenarioInfo> scenarios,
            List<Expression> glueCode, Instant now) {
        Map<String, ScenarioProgressRecord> updated = new LinkedHashMap<>();

        for (ScenarioInfo scenario : scenarios) {
            String fingerprint = fingerprinter.fingerprint(scenario.title());
            updated.put(fingerprint, advance(fingerprint, previous.get(fingerprint), scenario, glueCode, now));
        }

        for (Map.Entry<String, ScenarioProgressRecord> entry : previous.entrySet()) {
            updated.putIfAbsent(entry.getKey(), markRemoved(entry.getValue(), now));
        }

        return updated;
    }

    private ScenarioProgressRecord advance(
            String fingerprint, ScenarioProgressRecord existing, ScenarioInfo scenario,
            List<Expression> glueCode, Instant now) {
        Instant listedAt = existing != null ? existing.listedAt() : null;
        Instant definedAt = existing != null ? existing.definedAt() : null;
        Instant implementedAt = existing != null ? existing.implementedAt() : null;

        switch (classifier.classify(scenario, glueCode)) {
            case LISTED -> listedAt = listedAt != null ? listedAt : now;
            case DEFINED -> definedAt = definedAt != null ? definedAt : now;
            case IMPLEMENTED -> implementedAt = implementedAt != null ? implementedAt : now;
        }

        return new ScenarioProgressRecord(
                fingerprint, displayName(scenario.title()), scenario.featureTitle(),
                listedAt, definedAt, implementedAt, now, null);
    }

    private ScenarioProgressRecord markRemoved(ScenarioProgressRecord record, Instant now) {
        if (record.removedAt() != null) {
            return record;
        }
        return new ScenarioProgressRecord(
                record.fingerprint(), record.scenarioName(), record.featureTitle(),
                record.listedAt(), record.definedAt(), record.implementedAt(),
                record.lastSeenAt(), now);
    }

    private String displayName(String scenarioTitle) {
        Matcher matcher = KEYWORD_PREFIX.matcher(scenarioTitle);
        return matcher.matches() ? matcher.group(1) : scenarioTitle;
    }
}

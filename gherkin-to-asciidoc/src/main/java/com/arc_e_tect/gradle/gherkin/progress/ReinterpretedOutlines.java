package com.arc_e_tect.gradle.gherkin.progress;

import com.arc_e_tect.gradle.gherkin.parser.ScenarioInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Finds the {@code Scenario Outline}s whose persisted progress history this run's expansion of
 * outlines - one scenario per {@code Examples} row, rather than one per outline (see
 * {@link com.arc_e_tect.gradle.gherkin.parser.FeatureParser}) - has just superseded.
 *
 * <p>An outline that used to be reported once carried one history record, keyed on the outline's own
 * title. Once its rows are reported individually, that title is no longer any scenario's title, so
 * its record stops being seen and {@link ProgressHistoryUpdater} marks it removed, while each row
 * starts a record of its own. Nothing about the feature files changed - only how they're read - so
 * the report says so rather than leaving a reader to conclude that a scenario was deleted and
 * several new ones written.</p>
 *
 * <p>An outline is only reported here on the single run that actually re-interprets it. All three
 * of the following must hold, which they can't again once that run has persisted its history:</p>
 *
 * <ul>
 *   <li>the outline's own title has a record in the previously persisted history - so an outline
 *       first written after this behaviour shipped is never mentioned;</li>
 *   <li>that record is not already marked removed - so the run after the re-interpreting one,
 *       which still finds the same record sitting in the history file, stays quiet;</li>
 *   <li>no scenario in the current run still carries that title - so an outline whose rows happen
 *       to be reported under the outline's own title anyway (a single row, with no
 *       {@code <placeholder>} in the outline's name) is correctly left alone, its record still
 *       being advanced exactly as before.</li>
 * </ul>
 *
 * <p>Scope: like every other use of {@link ScenarioFingerprint}, the caller passes one project's
 * scenarios and that project's own persisted history at a time.</p>
 */
public final class ReinterpretedOutlines {

    private final ScenarioFingerprint fingerprinter = new ScenarioFingerprint();

    /** Creates a new {@code ReinterpretedOutlines}. */
    public ReinterpretedOutlines() {}

    /**
     * Finds every outline this run has re-interpreted, in the order each was first encountered.
     *
     * @param previous  the history as loaded from disk, keyed by fingerprint, before this run's
     *                  own scenarios are applied to it
     * @param scenarios this run's scenarios, outlines already expanded
     * @return one {@link Outline} per re-interpreted outline; empty when this run re-interprets
     *         none, which is every run but the first after the behaviour was introduced
     */
    public List<Outline> find(Map<String, ScenarioProgressRecord> previous, List<ScenarioInfo> scenarios) {
        Set<String> currentFingerprints = new HashSet<>();
        Map<String, List<ScenarioInfo>> rowsByOutlineTitle = new LinkedHashMap<>();
        for (ScenarioInfo scenario : scenarios) {
            currentFingerprints.add(fingerprinter.fingerprint(scenario.title()));
            if (scenario.outlineTitle() != null) {
                rowsByOutlineTitle
                        .computeIfAbsent(scenario.outlineTitle(), title -> new ArrayList<>())
                        .add(scenario);
            }
        }

        List<Outline> reinterpreted = new ArrayList<>();
        for (Map.Entry<String, List<ScenarioInfo>> entry : rowsByOutlineTitle.entrySet()) {
            String fingerprint = fingerprinter.fingerprint(entry.getKey());
            ScenarioProgressRecord record = previous.get(fingerprint);
            if (record == null || record.removedAt() != null || currentFingerprints.contains(fingerprint)) {
                continue;
            }
            List<ScenarioInfo> rows = entry.getValue();
            reinterpreted.add(new Outline(entry.getKey(), rows.get(0).featureTitle(), rows.size()));
        }
        return reinterpreted;
    }

    /**
     * A single {@code Scenario Outline} that this run has started reporting one scenario per
     * {@code Examples} row.
     *
     * @param title        the outline's own title, unsubstituted, e.g.
     *                     {@code "Scenario Outline: User logs in as <username>"} - the title its now
     *                     superseded history record is keyed on
     * @param featureTitle the name of the {@code Feature} the outline lives under
     * @param scenarioCount how many scenarios the outline is now reported as - one per
     *                     {@code Examples} row; always at least one
     */
    public record Outline(String title, String featureTitle, int scenarioCount) {}
}

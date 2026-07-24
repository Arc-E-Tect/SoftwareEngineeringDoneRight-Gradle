package com.arc_e_tect.gradle.gherkin.parser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Groups {@link ScenarioInfo} instances by the {@code Feature} they belong to. */
public final class ScenarioGrouping {

    private ScenarioGrouping() {}

    /**
     * Groups {@code scenarios} by {@link ScenarioInfo#featureTitle()}, preserving the order in
     * which each feature title was first encountered.
     *
     * @param scenarios the scenarios to group
     * @return an ordered map from feature title to the scenarios belonging to that feature,
     *         in document order
     */
    public static Map<String, List<ScenarioInfo>> byFeatureTitle(List<ScenarioInfo> scenarios) {
        Map<String, List<ScenarioInfo>> grouped = new LinkedHashMap<>();
        for (ScenarioInfo scenario : scenarios) {
            grouped.computeIfAbsent(scenario.featureTitle(), title -> new ArrayList<>()).add(scenario);
        }
        return grouped;
    }
}

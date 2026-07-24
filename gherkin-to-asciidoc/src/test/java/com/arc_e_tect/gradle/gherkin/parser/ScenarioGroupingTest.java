package com.arc_e_tect.gradle.gherkin.parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ScenarioGrouping")
class ScenarioGroupingTest {

    @Test
    @DisplayName("groups scenarios under their feature title")
    void groupsScenariosUnderFeatureTitle() {
        ScenarioInfo a = new ScenarioInfo("User authentication", "Scenario: A", List.of());
        ScenarioInfo b = new ScenarioInfo("User authentication", "Scenario: B", List.of());
        ScenarioInfo c = new ScenarioInfo("Invoice payment", "Scenario: C", List.of());

        Map<String, List<ScenarioInfo>> grouped = ScenarioGrouping.byFeatureTitle(List.of(a, b, c));

        assertThat(grouped.keySet()).containsExactly("User authentication", "Invoice payment");
        assertThat(grouped.get("User authentication")).containsExactly(a, b);
        assertThat(grouped.get("Invoice payment")).containsExactly(c);
    }

    @Test
    @DisplayName("preserves the order in which each feature title was first encountered")
    void preservesFirstEncounteredOrder() {
        ScenarioInfo first = new ScenarioInfo("Billing", "Scenario: First", List.of());
        ScenarioInfo second = new ScenarioInfo("Auth", "Scenario: Second", List.of());
        ScenarioInfo third = new ScenarioInfo("Billing", "Scenario: Third", List.of());

        Map<String, List<ScenarioInfo>> grouped = ScenarioGrouping.byFeatureTitle(List.of(first, second, third));

        assertThat(grouped.keySet()).containsExactly("Billing", "Auth");
        assertThat(grouped.get("Billing")).containsExactly(first, third);
    }

    @Test
    @DisplayName("returns an empty map for an empty input list")
    void returnsEmptyMapForEmptyInput() {
        Map<String, List<ScenarioInfo>> grouped = ScenarioGrouping.byFeatureTitle(List.of());

        assertThat(grouped).isEmpty();
    }
}

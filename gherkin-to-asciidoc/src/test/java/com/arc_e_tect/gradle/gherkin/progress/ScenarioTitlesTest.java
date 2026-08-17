package com.arc_e_tect.gradle.gherkin.progress;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ScenarioTitles")
class ScenarioTitlesTest {

    @Test
    @DisplayName("withoutKeyword() strips the Scenario: prefix")
    void withoutKeywordStripsScenarioPrefix() {
        assertThat(ScenarioTitles.withoutKeyword("Scenario: User logs in")).isEqualTo("User logs in");
    }

    @Test
    @DisplayName("withoutKeyword() strips the Scenario Outline: prefix")
    void withoutKeywordStripsScenarioOutlinePrefix() {
        assertThat(ScenarioTitles.withoutKeyword("Scenario Outline: User logs in")).isEqualTo("User logs in");
    }

    @Test
    @DisplayName("withoutKeyword() leaves a title with no keyword prefix unchanged")
    void withoutKeywordLeavesUnprefixedTitleUnchanged() {
        assertThat(ScenarioTitles.withoutKeyword("User logs in")).isEqualTo("User logs in");
    }

    @Test
    @DisplayName("withoutIndex() strips a single-number index prefix")
    void withoutIndexStripsSingleNumberPrefix() {
        assertThat(ScenarioTitles.withoutIndex("3 - User logs in")).isEqualTo("User logs in");
    }

    @Test
    @DisplayName("withoutIndex() strips a feature.scenario index prefix")
    void withoutIndexStripsFeatureScenarioPrefix() {
        assertThat(ScenarioTitles.withoutIndex("1.2 - User logs in")).isEqualTo("User logs in");
    }

    @Test
    @DisplayName("withoutIndex() leaves a title with no index prefix unchanged")
    void withoutIndexLeavesUnprefixedTitleUnchanged() {
        assertThat(ScenarioTitles.withoutIndex("User logs in")).isEqualTo("User logs in");
    }

    @Test
    @DisplayName("withoutIndex() does not mistake a number that's part of the real name for an index")
    void withoutIndexDoesNotMistakeEmbeddedNumberForIndex() {
        // No " - " separator right after the number, so this must be left alone.
        assertThat(ScenarioTitles.withoutIndex("2026 audit report")).isEqualTo("2026 audit report");
    }

    @Test
    @DisplayName("withoutKeyword() and withoutIndex() compose to strip both prefixes together")
    void keywordAndIndexStrippingCompose() {
        String cleaned = ScenarioTitles.withoutIndex(ScenarioTitles.withoutKeyword("Scenario: 1.2 - User logs in"));

        assertThat(cleaned).isEqualTo("User logs in");
    }
}

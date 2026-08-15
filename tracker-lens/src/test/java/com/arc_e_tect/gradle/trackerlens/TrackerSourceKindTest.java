package com.arc_e_tect.gradle.trackerlens;

import com.arc_e_tect.gradle.trackerlens.tracker.ApiContractTrackerSource;
import com.arc_e_tect.gradle.trackerlens.tracker.GherkinScenarioTrackerSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TrackerSourceKind")
class TrackerSourceKindTest {

    @Test
    @DisplayName("gherkinScenarioShouldExposeStagesInCanonicalOrder")
    void gherkinScenarioShouldExposeStagesInCanonicalOrder() {
        assertThat(TrackerSourceKind.GHERKIN_SCENARIO.stages()).containsExactly("listed", "defined", "implemented");
    }

    @Test
    @DisplayName("gherkinScenarioFinalStageShouldBeImplemented")
    void gherkinScenarioFinalStageShouldBeImplemented() {
        assertThat(TrackerSourceKind.GHERKIN_SCENARIO.finalStage()).isEqualTo("implemented");
    }

    @Test
    @DisplayName("gherkinScenarioShouldCreateMatchingSourceType")
    void gherkinScenarioShouldCreateMatchingSourceType() {
        assertThat(TrackerSourceKind.GHERKIN_SCENARIO.createSource()).isInstanceOf(GherkinScenarioTrackerSource.class);
    }

    @Test
    @DisplayName("apiContractShouldExposeStagesInCanonicalOrder")
    void apiContractShouldExposeStagesInCanonicalOrder() {
        assertThat(TrackerSourceKind.API_CONTRACT.stages())
                .containsExactly("declared", "implemented", "stubbed", "verified");
    }

    @Test
    @DisplayName("apiContractFinalStageShouldBeVerified")
    void apiContractFinalStageShouldBeVerified() {
        assertThat(TrackerSourceKind.API_CONTRACT.finalStage()).isEqualTo("verified");
    }

    @Test
    @DisplayName("apiContractShouldCreateMatchingSourceType")
    void apiContractShouldCreateMatchingSourceType() {
        assertThat(TrackerSourceKind.API_CONTRACT.createSource()).isInstanceOf(ApiContractTrackerSource.class);
    }
}

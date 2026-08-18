package com.arc_e_tect.gradle.trackerlens.fixture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FixtureSpec")
class FixtureSpecTest {

    @Test
    @DisplayName("defaults creates a valid spec with expected baseline values")
    void defaultsCreatesExpectedBaselineValues() {
        FixtureSpec spec = FixtureSpec.defaults();

        assertThat(spec.historyStartDaysAgo()).isEqualTo(60);
        assertThat(spec.forecastTargetDaysOut()).isEqualTo(30);
        assertThat(spec.workingDaysPerWeek()).isEqualTo(5);
        assertThat(spec.bddScenarioCount()).isEqualTo(8);
        assertThat(spec.apiContractCount()).isEqualTo(20);
        assertThat(spec.asOf()).isNotNull();
    }

    @Test
    @DisplayName("withAsOf returns a copy with only asOf changed")
    void withAsOfReturnsCopyWithOnlyAsOfChanged() {
        FixtureSpec original = FixtureSpec.defaults();
        Instant fixed = Instant.parse("2026-01-01T00:00:00Z");

        FixtureSpec updated = original.withAsOf(fixed);

        assertThat(updated.asOf()).isEqualTo(fixed);
        assertThat(updated.historyStartDaysAgo()).isEqualTo(original.historyStartDaysAgo());
        assertThat(updated.forecastTargetDaysOut()).isEqualTo(original.forecastTargetDaysOut());
        assertThat(updated.workingDaysPerWeek()).isEqualTo(original.workingDaysPerWeek());
        assertThat(updated.bddScenarioCount()).isEqualTo(original.bddScenarioCount());
        assertThat(updated.apiContractCount()).isEqualTo(original.apiContractCount());
    }

    @Test
    @DisplayName("constructor rejects null asOf")
    void constructorRejectsNullAsOf() {
        assertThatThrownBy(() -> new FixtureSpec(null, 60, 30, 5, 8, 20))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("asOf");
    }

    @Test
    @DisplayName("constructor rejects historyStartDaysAgo below one")
    void constructorRejectsHistoryStartDaysAgoBelowOne() {
        assertThatThrownBy(() -> new FixtureSpec(Instant.now(), 0, 30, 5, 8, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("historyStartDaysAgo must be at least 1");
    }

    @Test
    @DisplayName("constructor rejects negative forecastTargetDaysOut")
    void constructorRejectsNegativeForecastTargetDaysOut() {
        assertThatThrownBy(() -> new FixtureSpec(Instant.now(), 60, -1, 5, 8, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("forecastTargetDaysOut must not be negative");
    }

    @Test
    @DisplayName("constructor rejects workingDaysPerWeek values that differ from required projector assumption")
    void constructorRejectsUnexpectedWorkingDaysPerWeek() {
        assertThatThrownBy(() -> new FixtureSpec(Instant.now(), 60, 30, 4, 8, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("workingDaysPerWeek must be 5");
    }

    @Test
    @DisplayName("constructor rejects bddScenarioCount below one")
    void constructorRejectsBddScenarioCountBelowOne() {
        assertThatThrownBy(() -> new FixtureSpec(Instant.now(), 60, 30, 5, 0, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bddScenarioCount must be at least 1");
    }

    @Test
    @DisplayName("constructor rejects apiContractCount below one")
    void constructorRejectsApiContractCountBelowOne() {
        assertThatThrownBy(() -> new FixtureSpec(Instant.now(), 60, 30, 5, 8, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("apiContractCount must be at least 1");
    }
}
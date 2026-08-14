package com.arc_e_tect.gradle.trackerlens.tracker;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LifecycleRecord")
class LifecycleRecordTest {

    private static final Instant NOW = Instant.parse("2026-08-13T00:00:00Z");
    private static final List<String> STAGES = List.of("listed", "defined", "implemented");

    @Test
    @DisplayName("latestStageShouldReturnTheFurthestReachedStageWhenEveryEarlierStageWasAlsoReached")
    void latestStageShouldReturnTheFurthestReachedStageWhenEveryEarlierStageWasAlsoReached() {
        LifecycleRecord record = new LifecycleRecord("1", "a", null,
                Map.of("listed", NOW, "defined", NOW), NOW, null);

        assertThat(record.latestStage(STAGES)).contains("defined");
    }

    @Test
    @DisplayName("latestStageShouldReturnTheFurthestReachedStageEvenWhenAnEarlierStageWasSkipped")
    void latestStageShouldReturnTheFurthestReachedStageEvenWhenAnEarlierStageWasSkipped() {
        // Mirrors gherkin-to-asciidoc's ProgressHistoryUpdater: a scenario first observed already
        // implemented never gains a "defined" entry, so stageReachedAt can have a later stage
        // present while an earlier one is entirely absent - latestStage must still report the
        // furthest one reached, not be thrown off by the gap.
        LifecycleRecord record = new LifecycleRecord("1", "a", null,
                Map.of("listed", NOW, "implemented", NOW), NOW, null);

        assertThat(record.latestStage(STAGES)).contains("implemented");
    }

    @Test
    @DisplayName("latestStageShouldReturnEmptyWhenNoCanonicalStageHasBeenReached")
    void latestStageShouldReturnEmptyWhenNoCanonicalStageHasBeenReached() {
        LifecycleRecord record = new LifecycleRecord("1", "a", null, Map.of(), NOW, null);

        assertThat(record.latestStage(STAGES)).isEqualTo(Optional.empty());
    }

    @Test
    @DisplayName("hasReachedShouldBeTrueOnlyForStagesPresentInStageReachedAt")
    void hasReachedShouldBeTrueOnlyForStagesPresentInStageReachedAt() {
        LifecycleRecord record = new LifecycleRecord("1", "a", null, Map.of("listed", NOW), NOW, null);

        assertThat(record.hasReached("listed")).isTrue();
        assertThat(record.hasReached("implemented")).isFalse();
    }
}

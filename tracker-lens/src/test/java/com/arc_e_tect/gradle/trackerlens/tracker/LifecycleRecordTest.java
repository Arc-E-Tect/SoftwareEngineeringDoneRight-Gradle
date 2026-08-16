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

    @Test
    @DisplayName("latestStageAsOfShouldIgnoreStagesReachedAfterTheGivenInstant")
    void latestStageAsOfShouldIgnoreStagesReachedAfterTheGivenInstant() {
        Instant definedAt = NOW.plusSeconds(60);
        Instant implementedAt = NOW.plusSeconds(120);
        LifecycleRecord record = new LifecycleRecord("1", "a", null,
                Map.of("listed", NOW, "defined", definedAt, "implemented", implementedAt), implementedAt, null);

        assertThat(record.latestStageAsOf(STAGES, NOW)).contains("listed");
        assertThat(record.latestStageAsOf(STAGES, definedAt)).contains("defined");
        assertThat(record.latestStageAsOf(STAGES, implementedAt)).contains("implemented");
    }

    @Test
    @DisplayName("latestStageAsOfShouldIncludeAStageReachedExactlyAtTheGivenInstant")
    void latestStageAsOfShouldIncludeAStageReachedExactlyAtTheGivenInstant() {
        LifecycleRecord record = new LifecycleRecord("1", "a", null, Map.of("listed", NOW), NOW, null);

        assertThat(record.latestStageAsOf(STAGES, NOW)).contains("listed");
    }

    @Test
    @DisplayName("latestStageAsOfShouldReturnEmptyWhenEvaluatedBeforeTheItemWasFirstObserved")
    void latestStageAsOfShouldReturnEmptyWhenEvaluatedBeforeTheItemWasFirstObserved() {
        LifecycleRecord record = new LifecycleRecord("1", "a", null, Map.of("listed", NOW), NOW, null);

        assertThat(record.latestStageAsOf(STAGES, NOW.minusSeconds(1))).isEmpty();
    }

    @Test
    @DisplayName("latestStageAsOfShouldMatchLatestStageWhenAnEarlierStageWasSkippedRegardlessOfEvaluationInstant")
    void latestStageAsOfShouldMatchLatestStageWhenAnEarlierStageWasSkippedRegardlessOfEvaluationInstant() {
        // The exact edge case that a naive cumulative-count subtraction between two dates would get
        // wrong: this item's furthest-reached-in-list-order stage ("implemented") isn't the
        // highest-index key it holds by coincidence - "defined" was skipped entirely. latestStageAsOf,
        // evaluated at or after implementedAt, must resolve this the same way latestStage() already
        // does for "now".
        LifecycleRecord record = new LifecycleRecord("1", "a", null,
                Map.of("listed", NOW, "implemented", NOW.plusSeconds(60)), NOW.plusSeconds(60), null);

        assertThat(record.latestStageAsOf(STAGES, NOW.plusSeconds(60))).isEqualTo(record.latestStage(STAGES));
        assertThat(record.latestStageAsOf(STAGES, NOW.plusSeconds(60))).contains("implemented");
    }

    @Test
    @DisplayName("isActiveAsOfShouldBeTrueWhenNeverRemoved")
    void isActiveAsOfShouldBeTrueWhenNeverRemoved() {
        LifecycleRecord record = new LifecycleRecord("1", "a", null, Map.of("listed", NOW), NOW, null);

        assertThat(record.isActiveAsOf(NOW.plusSeconds(1000))).isTrue();
    }

    @Test
    @DisplayName("isActiveAsOfShouldBeFalseOnceEvaluatedAtOrAfterRemoval")
    void isActiveAsOfShouldBeFalseOnceEvaluatedAtOrAfterRemoval() {
        Instant removedAt = NOW.plusSeconds(60);
        LifecycleRecord record = new LifecycleRecord("1", "a", null, Map.of("listed", NOW), NOW, removedAt);

        assertThat(record.isActiveAsOf(removedAt.minusSeconds(1))).isTrue();
        assertThat(record.isActiveAsOf(removedAt)).isFalse();
        assertThat(record.isActiveAsOf(removedAt.plusSeconds(1))).isFalse();
    }
}

package com.arc_e_tect.gradle.trackerlens.dashboard;

import com.arc_e_tect.gradle.trackerlens.tracker.LifecycleRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TrackerViewFactory")
class TrackerViewFactoryTest {

    private static final Instant NOW = Instant.parse("2026-08-13T00:00:00Z");
    private static final List<String> STAGES = List.of("listed", "defined", "implemented");

    private final TrackerViewFactory factory = new TrackerViewFactory();

    @Test
    @DisplayName("buildShouldComputeCumulativeMetricCountAndPercentPerStageWhenStagesAreIndependent")
    void buildShouldComputeCumulativeMetricCountAndPercentPerStageWhenStagesAreIndependent() {
        List<LifecycleRecord> records = List.of(
                new LifecycleRecord("1", "a", null, Map.of("listed", NOW), NOW, null),
                new LifecycleRecord("2", "b", null, Map.of("listed", NOW, "defined", NOW), NOW, null));

        TrackerView view = factory.build("t", STAGES, records, Optional.empty(), NOW, false);

        assertThat(view.metrics()).extracting(MetricCardView::stage, MetricCardView::count, MetricCardView::percent)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("listed", 2, 100),
                        org.assertj.core.groups.Tuple.tuple("defined", 1, 50),
                        org.assertj.core.groups.Tuple.tuple("implemented", 0, 0));
    }

    @Test
    @DisplayName("buildShouldComputeCurrentStageMetricCountAndPercentPerStageWhenStagesFormADependencyChain")
    void buildShouldComputeCurrentStageMetricCountAndPercentPerStageWhenStagesFormADependencyChain() {
        List<LifecycleRecord> records = List.of(
                new LifecycleRecord("1", "a", null, Map.of("listed", NOW), NOW, null),
                new LifecycleRecord("2", "b", null, Map.of("listed", NOW, "defined", NOW), NOW, null));

        TrackerView view = factory.build("t", STAGES, records, Optional.empty(), NOW, true);

        assertThat(view.metrics()).extracting(MetricCardView::stage, MetricCardView::count, MetricCardView::percent)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("listed", 1, 50),
                        org.assertj.core.groups.Tuple.tuple("defined", 1, 50),
                        org.assertj.core.groups.Tuple.tuple("implemented", 0, 0));
    }

    @Test
    @DisplayName("buildShouldExcludeRemovedItemsFromTotalCount")
    void buildShouldExcludeRemovedItemsFromTotalCount() {
        List<LifecycleRecord> records = List.of(
                new LifecycleRecord("1", "a", null, Map.of("listed", NOW), NOW, null),
                new LifecycleRecord("2", "b", null, Map.of("listed", NOW), NOW, NOW));

        TrackerView view = factory.build("t", STAGES, records, Optional.empty(), NOW, false);

        assertThat(view.totalCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("buildShouldMarkActiveItemNotReachingFinalStageAsStaleAfterThreshold")
    void buildShouldMarkActiveItemNotReachingFinalStageAsStaleAfterThreshold() {
        Instant longAgo = NOW.minus(Duration.ofDays(30));
        List<LifecycleRecord> records = List.of(
                new LifecycleRecord("1", "stale-one", null, Map.of("listed", longAgo), longAgo, null));

        TrackerView view = factory.build("t", STAGES, records, Optional.empty(), NOW, false);

        assertThat(view.staleItems()).extracting(LifecycleRecord::id).containsExactly("1");
    }

    @Test
    @DisplayName("buildShouldNotMarkRecentlyActiveItemAsStale")
    void buildShouldNotMarkRecentlyActiveItemAsStale() {
        List<LifecycleRecord> records = List.of(
                new LifecycleRecord("1", "fresh", null, Map.of("listed", NOW), NOW, null));

        TrackerView view = factory.build("t", STAGES, records, Optional.empty(), NOW, false);

        assertThat(view.staleItems()).isEmpty();
    }

    @Test
    @DisplayName("buildShouldProduceThirtyChartDatesEndingToday")
    void buildShouldProduceThirtyChartDatesEndingToday() {
        TrackerView view = factory.build("t", STAGES, List.of(), Optional.empty(), NOW, false);

        assertThat(view.chartDates()).hasSize(30);
    }

    @Test
    @DisplayName("buildShouldBucketEachActiveItemUnderItsFurthestReachedStageExactlyOnce")
    void buildShouldBucketEachActiveItemUnderItsFurthestReachedStageExactlyOnce() {
        List<LifecycleRecord> records = List.of(
                new LifecycleRecord("1", "listed-only", null, Map.of("listed", NOW), NOW, null),
                new LifecycleRecord("2", "defined-only", null,
                        Map.of("listed", NOW, "defined", NOW), NOW, null),
                new LifecycleRecord("3", "implemented", null,
                        Map.of("listed", NOW, "defined", NOW, "implemented", NOW), NOW, null));

        TrackerView view = factory.build("t", STAGES, records, Optional.empty(), NOW, false);

        assertThat(view.stageBreakdown()).containsExactly(
                Map.entry("listed", 1), Map.entry("defined", 1), Map.entry("implemented", 1));
    }

    @Test
    @DisplayName("buildShouldBucketAnItemThatSkippedAnIntermediateStageUnderItsFurthestReachedStage")
    void buildShouldBucketAnItemThatSkippedAnIntermediateStageUnderItsFurthestReachedStage() {
        // A scenario gherkin-to-asciidoc first observes already-implemented never gains a
        // "defined" entry (see ProgressHistoryUpdater's own javadoc) - stageBreakdown must still
        // count it under "implemented", not "listed", even though "defined" is absent from its
        // stageReachedAt map entirely. This is exactly the case the naive
        // cumulativeCount(stage) - cumulativeCount(nextStage) subtraction a template might
        // otherwise try gets wrong: it would attribute this item to "listed" instead.
        List<LifecycleRecord> records = List.of(
                new LifecycleRecord("1", "skipped-defined", null,
                        Map.of("listed", NOW, "implemented", NOW), NOW, null));

        TrackerView view = factory.build("t", STAGES, records, Optional.empty(), NOW, false);

        assertThat(view.stageBreakdown()).containsExactly(
                Map.entry("listed", 0), Map.entry("defined", 0), Map.entry("implemented", 1));
    }

    @Test
    @DisplayName("buildShouldExcludeRemovedItemsFromStageBreakdown")
    void buildShouldExcludeRemovedItemsFromStageBreakdown() {
        List<LifecycleRecord> records = List.of(
                new LifecycleRecord("1", "removed", null, Map.of("listed", NOW), NOW, NOW));

        TrackerView view = factory.build("t", STAGES, records, Optional.empty(), NOW, false);

        assertThat(view.stageBreakdown()).containsExactly(
                Map.entry("listed", 0), Map.entry("defined", 0), Map.entry("implemented", 0));
    }
}

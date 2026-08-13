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
    @DisplayName("buildShouldComputeMetricCountAndPercentPerStage")
    void buildShouldComputeMetricCountAndPercentPerStage() {
        List<LifecycleRecord> records = List.of(
                new LifecycleRecord("1", "a", null, Map.of("listed", NOW), NOW, null),
                new LifecycleRecord("2", "b", null, Map.of("listed", NOW, "defined", NOW), NOW, null));

        TrackerView view = factory.build("t", STAGES, records, Optional.empty(), NOW);

        assertThat(view.metrics()).extracting(MetricCardView::stage, MetricCardView::count, MetricCardView::percent)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("listed", 2, 100),
                        org.assertj.core.groups.Tuple.tuple("defined", 1, 50),
                        org.assertj.core.groups.Tuple.tuple("implemented", 0, 0));
    }

    @Test
    @DisplayName("buildShouldExcludeRemovedItemsFromTotalCount")
    void buildShouldExcludeRemovedItemsFromTotalCount() {
        List<LifecycleRecord> records = List.of(
                new LifecycleRecord("1", "a", null, Map.of("listed", NOW), NOW, null),
                new LifecycleRecord("2", "b", null, Map.of("listed", NOW), NOW, NOW));

        TrackerView view = factory.build("t", STAGES, records, Optional.empty(), NOW);

        assertThat(view.totalCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("buildShouldMarkActiveItemNotReachingFinalStageAsStaleAfterThreshold")
    void buildShouldMarkActiveItemNotReachingFinalStageAsStaleAfterThreshold() {
        Instant longAgo = NOW.minus(Duration.ofDays(30));
        List<LifecycleRecord> records = List.of(
                new LifecycleRecord("1", "stale-one", null, Map.of("listed", longAgo), longAgo, null));

        TrackerView view = factory.build("t", STAGES, records, Optional.empty(), NOW);

        assertThat(view.staleItems()).extracting(LifecycleRecord::id).containsExactly("1");
    }

    @Test
    @DisplayName("buildShouldNotMarkRecentlyActiveItemAsStale")
    void buildShouldNotMarkRecentlyActiveItemAsStale() {
        List<LifecycleRecord> records = List.of(
                new LifecycleRecord("1", "fresh", null, Map.of("listed", NOW), NOW, null));

        TrackerView view = factory.build("t", STAGES, records, Optional.empty(), NOW);

        assertThat(view.staleItems()).isEmpty();
    }

    @Test
    @DisplayName("buildShouldProduceThirtyChartDatesEndingToday")
    void buildShouldProduceThirtyChartDatesEndingToday() {
        TrackerView view = factory.build("t", STAGES, List.of(), Optional.empty(), NOW);

        assertThat(view.chartDates()).hasSize(30);
    }
}

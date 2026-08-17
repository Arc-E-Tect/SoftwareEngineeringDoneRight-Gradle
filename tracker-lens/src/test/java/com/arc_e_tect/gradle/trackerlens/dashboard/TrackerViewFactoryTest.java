package com.arc_e_tect.gradle.trackerlens.dashboard;

import com.arc_e_tect.gradle.trackerlens.tracker.LifecycleRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
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
    @DisplayName("buildShouldProduceThirtyChartDatesEndingTodayWhenThereAreNoRecordsAtAll")
    void buildShouldProduceThirtyChartDatesEndingTodayWhenThereAreNoRecordsAtAll() {
        TrackerView view = factory.build("t", STAGES, List.of(), Optional.empty(), NOW, false);

        LocalDate today = LocalDate.ofInstant(NOW, ZoneOffset.UTC);
        assertThat(view.chartDates()).hasSize(30);
        assertThat(view.chartDates().get(view.chartDates().size() - 1)).isEqualTo(today);
    }

    @Test
    @DisplayName("buildShouldStartTheChartOnTheEarliestTimestampAcrossAllRecords")
    void buildShouldStartTheChartOnTheEarliestTimestampAcrossAllRecords() {
        Instant earliest = NOW.minus(Duration.ofDays(40));
        Instant later = NOW.minus(Duration.ofDays(10));
        List<LifecycleRecord> records = List.of(
                new LifecycleRecord("1", "later", null, Map.of("listed", later), NOW, null),
                new LifecycleRecord("2", "earliest", null, Map.of("listed", earliest), NOW, null));

        TrackerView view = factory.build("t", STAGES, records, Optional.empty(), NOW, false);

        assertThat(view.chartDates().get(0)).isEqualTo(LocalDate.ofInstant(earliest, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("buildShouldExtendTheChartSevenDaysPastTheLatestTimestampWhenTheSpanIsAlreadyThirtyDaysOrMore")
    void buildShouldExtendTheChartSevenDaysPastTheLatestTimestampWhenTheSpanIsAlreadyThirtyDaysOrMore() {
        Instant earliest = NOW.minus(Duration.ofDays(40));
        Instant latestData = NOW.minus(Duration.ofDays(5));
        List<LifecycleRecord> records = List.of(
                new LifecycleRecord("1", "a", null, Map.of("listed", earliest), NOW, null),
                new LifecycleRecord("2", "b", null, Map.of("listed", latestData), NOW, null));

        TrackerView view = factory.build("t", STAGES, records, Optional.empty(), NOW, false);

        LocalDate expectedStart = LocalDate.ofInstant(earliest, ZoneOffset.UTC);
        LocalDate expectedEnd = LocalDate.ofInstant(latestData, ZoneOffset.UTC).plusDays(7);
        assertThat(view.chartDates().get(0)).isEqualTo(expectedStart);
        assertThat(view.chartDates().get(view.chartDates().size() - 1)).isEqualTo(expectedEnd);
    }

    @Test
    @DisplayName("buildShouldExtendTheChartToThirtyDaysWhenTheDataSpanPlusPaddingWouldOtherwiseBeShorter")
    void buildShouldExtendTheChartToThirtyDaysWhenTheDataSpanPlusPaddingWouldOtherwiseBeShorter() {
        Instant onlyDataPoint = NOW.minus(Duration.ofDays(2));
        List<LifecycleRecord> records = List.of(
                new LifecycleRecord("1", "a", null, Map.of("listed", onlyDataPoint), NOW, null));

        TrackerView view = factory.build("t", STAGES, records, Optional.empty(), NOW, false);

        LocalDate expectedStart = LocalDate.ofInstant(onlyDataPoint, ZoneOffset.UTC);
        assertThat(view.chartDates()).hasSize(30);
        assertThat(view.chartDates().get(0)).isEqualTo(expectedStart);
        assertThat(view.chartDates().get(29)).isEqualTo(expectedStart.plusDays(29));
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

    @Test
    @DisplayName("buildShouldProduceOneBreakdownByDateEntryPerChartDate")
    void buildShouldProduceOneBreakdownByDateEntryPerChartDate() {
        TrackerView view = factory.build("t", STAGES, List.of(), Optional.empty(), NOW, false);

        assertThat(view.breakdownByDate()).hasSize(view.chartDates().size());
    }

    @Test
    @DisplayName("buildShouldMakeTheMostRecentBreakdownByDateEntryMatchStageBreakdown")
    void buildShouldMakeTheMostRecentBreakdownByDateEntryMatchStageBreakdown() {
        List<LifecycleRecord> records = List.of(
                new LifecycleRecord("1", "listed-only", null, Map.of("listed", NOW), NOW, null),
                new LifecycleRecord("2", "implemented", null,
                        Map.of("listed", NOW, "defined", NOW, "implemented", NOW), NOW, null));

        TrackerView view = factory.build("t", STAGES, records, Optional.empty(), NOW, false);

        assertThat(view.breakdownByDate().get(view.breakdownByDate().size() - 1))
                .isEqualTo(view.stageBreakdown());
    }

    @Test
    @DisplayName("buildShouldReflectAnItemsHistoricalStatusAtEachBreakdownByDateEntry")
    void buildShouldReflectAnItemsHistoricalStatusAtEachBreakdownByDateEntry() {
        LocalDate today = LocalDate.ofInstant(NOW, ZoneOffset.UTC);
        LocalDate fiveDaysAgo = today.minusDays(5);
        LocalDate twoDaysAgo = today.minusDays(2);
        Instant listedAt = fiveDaysAgo.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant implementedAt = twoDaysAgo.atStartOfDay(ZoneOffset.UTC).toInstant();
        List<LifecycleRecord> records = List.of(
                new LifecycleRecord("1", "skipped-defined", null,
                        Map.of("listed", listedAt, "implemented", implementedAt), NOW, null));

        TrackerView view = factory.build("t", STAGES, records, Optional.empty(), NOW, false);

        // No "day before it was ever listed" case here: the chart window now starts exactly on
        // the tracker's earliest data point (this item's own listedAt, fiveDaysAgo), so there is
        // no earlier day left to check - see buildShouldStartTheChartOnTheEarliestTimestampAcrossAllRecords.
        int afterListedBeforeImplementedIndex = view.chartDates().indexOf(twoDaysAgo.minusDays(1));
        int afterImplementedIndex = view.chartDates().indexOf(twoDaysAgo);

        assertThat(view.breakdownByDate().get(afterListedBeforeImplementedIndex))
                .containsExactly(Map.entry("listed", 1), Map.entry("defined", 0), Map.entry("implemented", 0));
        // The exact edge case a naive series-subtraction alternative gets wrong: "defined" was
        // skipped entirely, so this item must bucket under "implemented", never under "defined".
        assertThat(view.breakdownByDate().get(afterImplementedIndex))
                .containsExactly(Map.entry("listed", 0), Map.entry("defined", 0), Map.entry("implemented", 1));
    }

    @Test
    @DisplayName("buildShouldExcludeAnItemFromBreakdownByDateEntriesBeforeItWasRemoved")
    void buildShouldExcludeAnItemFromBreakdownByDateEntriesBeforeItWasRemoved() {
        LocalDate today = LocalDate.ofInstant(NOW, ZoneOffset.UTC);
        LocalDate fiveDaysAgo = today.minusDays(5);
        LocalDate twoDaysAgo = today.minusDays(2);
        Instant listedAt = fiveDaysAgo.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant removedAt = twoDaysAgo.atStartOfDay(ZoneOffset.UTC).toInstant();
        List<LifecycleRecord> records = List.of(
                new LifecycleRecord("1", "later-removed", null, Map.of("listed", listedAt), NOW, removedAt));

        TrackerView view = factory.build("t", STAGES, records, Optional.empty(), NOW, false);

        int beforeRemovalIndex = view.chartDates().indexOf(twoDaysAgo.minusDays(1));
        int afterRemovalIndex = view.chartDates().indexOf(twoDaysAgo);

        assertThat(view.breakdownByDate().get(beforeRemovalIndex).get("listed")).isEqualTo(1);
        assertThat(view.breakdownByDate().get(afterRemovalIndex).get("listed")).isEqualTo(0);
    }
}

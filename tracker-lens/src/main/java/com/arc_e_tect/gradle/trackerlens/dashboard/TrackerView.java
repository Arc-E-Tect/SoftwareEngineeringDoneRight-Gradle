package com.arc_e_tect.gradle.trackerlens.dashboard;

import com.arc_e_tect.gradle.trackerlens.projection.Projection;
import com.arc_e_tect.gradle.trackerlens.tracker.LifecycleRecord;
import com.arc_e_tect.gradle.trackerlens.tracker.ResponseCoverageCell;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Rendered data for one {@code .tracker} section of the dashboard.
 *
 * @param id         the tracker's registered id
 * @param stages     the tracker's canonical stage names, in order
 * @param metrics    one {@link MetricCardView} per stage, in {@code stages} order
 * @param totalCount number of currently-active (non-removed) items in this tracker
 * @param projection the completion projection, when at least 7 days of history is available
 * @param chartDates the dates plotted on this tracker's chart, ascending
 * @param chartSeries cumulative reached-count per stage, aligned index-for-index with {@code chartDates}
 * @param staleItems  active items that have not reached the final stage and have seen no recent activity
 * @param stageBreakdown number of active items whose {@link LifecycleRecord#latestStage(List)} is
 *                       each stage, keyed and ordered exactly like {@code stages} - unlike
 *                       {@code metrics}' cumulative "reached at least this stage" counts, these are
 *                       mutually exclusive and always sum to {@code totalCount}, since every active
 *                       item has exactly one furthest-reached stage
 * @param breakdownByDate {@code stageBreakdown}, recomputed as of each date in {@code chartDates}
 *                        (same index order) instead of "now" - each entry sums to however many
 *                        items were active as of that date, which may differ from {@code totalCount}
 * @param seriesByGroup  {@code chartSeries}, partitioned by {@link LifecycleRecord#group()}: group
 *                       name to per-stage cumulative counts, each aligned index-for-index with
 *                       {@code chartDates} exactly like {@code chartSeries} itself. Empty when no
 *                       record in this tracker carries a non-null group.
 * @param matrix         the current-state response-coverage snapshot, when this tracker's source is
 *                       {@code RESPONSE_COVERAGE} - empty for every other kind. Not derived from
 *                       {@code stageReachedAt}/{@code chartSeries} the way every other field here is;
 *                       see {@link ResponseCoverageCell}.
 */
public record TrackerView(
        String id,
        List<String> stages,
        List<MetricCardView> metrics,
        int totalCount,
        Optional<Projection> projection,
        List<LocalDate> chartDates,
        Map<String, List<Integer>> chartSeries,
        List<LifecycleRecord> staleItems,
        Map<String, Integer> stageBreakdown,
        List<Map<String, Integer>> breakdownByDate,
        Map<String, Map<String, List<Integer>>> seriesByGroup,
        List<ResponseCoverageCell> matrix) {

    /**
     * Creates a {@code TrackerView} with no group-partitioned series and no coverage matrix -
     * equivalent to the canonical constructor with {@code Map.of()} and {@code List.of()} for
     * those two fields.
     *
     * @param id         the tracker's registered id
     * @param stages     the tracker's canonical stage names, in order
     * @param metrics    one {@link MetricCardView} per stage, in {@code stages} order
     * @param totalCount number of currently-active (non-removed) items in this tracker
     * @param projection the completion projection, when at least 7 days of history is available
     * @param chartDates the dates plotted on this tracker's chart, ascending
     * @param chartSeries cumulative reached-count per stage, aligned index-for-index with {@code chartDates}
     * @param staleItems  active items that have not reached the final stage and have seen no recent activity
     * @param stageBreakdown number of active items whose furthest reached stage is each stage
     * @param breakdownByDate {@code stageBreakdown}, recomputed as of each date in {@code chartDates}
     */
    public TrackerView(
            String id, List<String> stages, List<MetricCardView> metrics, int totalCount,
            Optional<Projection> projection, List<LocalDate> chartDates, Map<String, List<Integer>> chartSeries,
            List<LifecycleRecord> staleItems, Map<String, Integer> stageBreakdown,
            List<Map<String, Integer>> breakdownByDate) {
        this(id, stages, metrics, totalCount, projection, chartDates, chartSeries, staleItems, stageBreakdown,
                breakdownByDate, Map.of(), List.of());
    }

    /**
     * Returns a copy of this view with {@link #matrix()} replaced - the coverage matrix is
     * attached separately from this view's own milestone-based construction, since it comes from a
     * raw re-read of the source file rather than from {@link LifecycleRecord}s.
     *
     * @param matrix the coverage matrix to attach
     * @return a copy of this view with {@code matrix} replaced
     */
    public TrackerView withMatrix(List<ResponseCoverageCell> matrix) {
        return new TrackerView(id, stages, metrics, totalCount, projection, chartDates, chartSeries, staleItems,
                stageBreakdown, breakdownByDate, seriesByGroup, matrix);
    }
}

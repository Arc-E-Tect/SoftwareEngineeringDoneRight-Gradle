package com.arc_e_tect.gradle.trackerlens.dashboard;

import com.arc_e_tect.gradle.trackerlens.projection.Projection;
import com.arc_e_tect.gradle.trackerlens.tracker.LifecycleRecord;

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
        Map<String, Integer> stageBreakdown) {
}

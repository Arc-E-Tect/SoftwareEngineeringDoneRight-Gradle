package com.arc_e_tect.gradle.trackerlens.dashboard;

import com.arc_e_tect.gradle.trackerlens.projection.Projection;
import com.arc_e_tect.gradle.trackerlens.tracker.LifecycleRecord;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Builds a {@link TrackerView} from a tracker's raw records, ready for
 * {@link DashboardHtmlWriter} to render.
 */
public class TrackerViewFactory {

    private static final int CHART_WINDOW_DAYS = 30;
    private static final Duration STALE_THRESHOLD = Duration.ofDays(14);

    /** Creates a new {@code TrackerViewFactory}. */
    public TrackerViewFactory() {}

    /**
     * Builds the view for one tracker.
     *
     * @param trackerId                   the tracker's registered id
     * @param stages                      the tracker's canonical stage names, in order
     * @param records                     the tracker's records
     * @param projection                  the tracker's completion projection, if available
     * @param now                         the instant to render as of
     * @param stagesFormADependencyChain  whether an item can only reach a later stage by having
     *                                    already passed through every earlier one, e.g. Gherkin
     *                                    scenarios ({@code true}). When {@code true}, metric card
     *                                    counts are each item's single current stage (mutually
     *                                    exclusive, summing to the total). When {@code false},
     *                                    e.g. API contracts, stages are independent conditions an
     *                                    item may reach in any combination, so metric card counts
     *                                    stay cumulative "reached at least this stage" counts.
     * @return the built view
     */
    public TrackerView build(String trackerId, List<String> stages, List<LifecycleRecord> records,
            Optional<Projection> projection, Instant now, boolean stagesFormADependencyChain) {
        List<LifecycleRecord> active = records.stream().filter(record -> record.removedAt() == null).toList();
        int totalCount = active.size();

        Map<String, Integer> stageBreakdown = computeStageBreakdown(records, stages, now);

        List<MetricCardView> metrics = new ArrayList<>();
        for (String stage : stages) {
            long count = stagesFormADependencyChain
                    ? stageBreakdown.get(stage)
                    : active.stream().filter(record -> record.hasReached(stage)).count();
            int percent = totalCount == 0 ? 0 : Math.round(count * 100f / totalCount);
            metrics.add(new MetricCardView(stage, (int) count, percent));
        }

        List<LocalDate> chartDates = chartDates(now);
        Map<String, List<Integer>> chartSeries = new LinkedHashMap<>();
        for (String stage : stages) {
            List<Integer> counts = new ArrayList<>();
            for (LocalDate date : chartDates) {
                counts.add(cumulativeCountAt(records, stage, date));
            }
            chartSeries.put(stage, counts);
        }

        List<Map<String, Integer>> breakdownByDate = new ArrayList<>();
        for (LocalDate date : chartDates) {
            breakdownByDate.add(computeStageBreakdown(records, stages, endOfDate(date)));
        }

        String finalStage = stages.get(stages.size() - 1);
        List<LifecycleRecord> staleItems = active.stream()
                .filter(record -> !record.hasReached(finalStage))
                .filter(record -> mostRecentActivity(record).isBefore(now.minus(STALE_THRESHOLD)))
                .toList();

        return new TrackerView(
                trackerId, stages, metrics, totalCount, projection, chartDates, chartSeries, staleItems,
                stageBreakdown, breakdownByDate);
    }

    /**
     * The number of active items whose {@link LifecycleRecord#latestStageAsOf(List, Instant)} is
     * each of {@code stages}, as of {@code asOf} - mutually exclusive and summing to the number of
     * items active as of {@code asOf}, exactly like {@link TrackerView#stageBreakdown()} does for
     * "now" (indeed, calling this with {@code asOf = now} reproduces {@code stageBreakdown} exactly).
     *
     * <p>Evaluated against each record's own full history rather than by comparing {@code
     * chartSeries}' cumulative per-stage counts between dates - the latter looks equivalent but
     * silently mis-attributes any item that skipped an intermediate stage (see
     * {@link LifecycleRecord#latestStage(List)}'s own javadoc) to that skipped stage instead of the
     * one it actually, furthest reached.</p>
     */
    private Map<String, Integer> computeStageBreakdown(List<LifecycleRecord> records, List<String> stages,
            Instant asOf) {
        Map<String, Integer> breakdown = new LinkedHashMap<>();
        for (String stage : stages) {
            breakdown.put(stage, 0);
        }
        for (LifecycleRecord record : records) {
            if (!record.isActiveAsOf(asOf)) {
                continue;
            }
            record.latestStageAsOf(stages, asOf).ifPresent(stage -> breakdown.merge(stage, 1, Integer::sum));
        }
        return breakdown;
    }

    /** The last instant of {@code date}, UTC - the inclusive "as of" bound for a chart date. */
    private Instant endOfDate(LocalDate date) {
        return date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().minusNanos(1);
    }

    private List<LocalDate> chartDates(Instant now) {
        LocalDate today = LocalDate.ofInstant(now, ZoneOffset.UTC);
        List<LocalDate> dates = new ArrayList<>();
        for (int i = CHART_WINDOW_DAYS - 1; i >= 0; i--) {
            dates.add(today.minusDays(i));
        }
        return dates;
    }

    private int cumulativeCountAt(List<LifecycleRecord> records, String stage, LocalDate date) {
        Instant endOfDay = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        return (int) records.stream()
                .map(record -> record.stageReachedAt().get(stage))
                .filter(Objects::nonNull)
                .filter(reachedAt -> reachedAt.isBefore(endOfDay))
                .count();
    }

    private Instant mostRecentActivity(LifecycleRecord record) {
        Instant latest = record.lastSeenAt() != null ? record.lastSeenAt() : Instant.EPOCH;
        for (Instant reachedAt : record.stageReachedAt().values()) {
            if (reachedAt.isAfter(latest)) {
                latest = reachedAt;
            }
        }
        return latest;
    }
}

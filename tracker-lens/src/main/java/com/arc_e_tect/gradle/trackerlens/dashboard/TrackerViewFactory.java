package com.arc_e_tect.gradle.trackerlens.dashboard;

import com.arc_e_tect.gradle.trackerlens.projection.Projection;
import com.arc_e_tect.gradle.trackerlens.tracker.LifecycleRecord;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
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

    private static final int MINIMUM_CHART_WINDOW_DAYS = 30;
    private static final int CHART_TRAILING_PADDING_DAYS = 7;
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

        List<LocalDate> chartDates = chartDates(records, now);
        Optional<LocalDate> lastDataDate = lastDataDate(records);

        Map<String, List<Integer>> chartSeries = new LinkedHashMap<>();
        for (String stage : stages) {
            List<Integer> counts = new ArrayList<>();
            for (LocalDate date : chartDates) {
                counts.add(hasDataOn(date, lastDataDate) ? cumulativeCountAt(records, stage, date) : 0);
            }
            chartSeries.put(stage, counts);
        }

        List<Map<String, Integer>> breakdownByDate = new ArrayList<>();
        for (LocalDate date : chartDates) {
            breakdownByDate.add(hasDataOn(date, lastDataDate)
                    ? computeStageBreakdown(records, stages, endOfDate(date))
                    : zeroBreakdown(stages));
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

    /**
     * The chart's date range: starting on the earliest timestamp actually present in
     * {@code records} (not a fixed lookback from {@code now}), so a tracker whose history only
     * goes back a few days never shows weeks of misleading flat-zero lead-in. The end extends 7
     * days past the latest timestamp present, for a little trailing breathing room - or further,
     * whenever that would otherwise make for a window shorter than
     * {@value #MINIMUM_CHART_WINDOW_DAYS} days, so a tightly-clustered tracker (e.g. one whose
     * entire history was recorded in a single run) still gets a chart wide enough to be readable
     * rather than a near-single-point line.
     *
     * <p>This padding can land after the latest real data point - even after {@code now} itself,
     * for a tracker whose history is very recent - purely to give the chart a readable width.
     * {@link #build} never plots a real value on a padding date, though: see {@link #hasDataOn}.</p>
     *
     * <p>When {@code records} has no timestamps at all yet, falls back to the previous fixed
     * window - the last {@value #MINIMUM_CHART_WINDOW_DAYS} days ending {@code now} - since
     * there is no data to anchor a start date to.</p>
     */
    private List<LocalDate> chartDates(List<LifecycleRecord> records, Instant now) {
        LocalDate today = LocalDate.ofInstant(now, ZoneOffset.UTC);
        List<Instant> timestamps = allTimestamps(records);
        if (timestamps.isEmpty()) {
            return datesBetween(today.minusDays(MINIMUM_CHART_WINDOW_DAYS - 1), today);
        }

        LocalDate start = LocalDate.ofInstant(Collections.min(timestamps), ZoneOffset.UTC);
        LocalDate lastDataDate = LocalDate.ofInstant(Collections.max(timestamps), ZoneOffset.UTC);
        LocalDate naturalEnd = lastDataDate.plusDays(CHART_TRAILING_PADDING_DAYS);
        LocalDate minimumEnd = start.plusDays(MINIMUM_CHART_WINDOW_DAYS - 1);
        LocalDate end = naturalEnd.isBefore(minimumEnd) ? minimumEnd : naturalEnd;
        return datesBetween(start, end);
    }

    /** The latest timestamp actually present in {@code records}, as a date - empty when there is none. */
    private Optional<LocalDate> lastDataDate(List<LifecycleRecord> records) {
        List<Instant> timestamps = allTimestamps(records);
        return timestamps.isEmpty()
                ? Optional.empty()
                : Optional.of(LocalDate.ofInstant(Collections.max(timestamps), ZoneOffset.UTC));
    }

    /**
     * Whether {@code date} falls on or before the tracker's last real data point - {@code false}
     * for a chart date that only exists as {@link #chartDates} padding (including one after
     * {@code now}, for a tracker whose history is very recent). {@link #build} plots {@code 0}
     * rather than a computed value on any date this returns {@code false} for, since there is
     * nothing in {@code records} to compute one from - and, absent a forecasting feature, {@code
     * 0} is a more honest chart value than silently repeating the last real one forever.
     */
    private boolean hasDataOn(LocalDate date, Optional<LocalDate> lastDataDate) {
        return lastDataDate.isPresent() && !date.isAfter(lastDataDate.get());
    }

    /** A stage-to-zero map, in {@code stages} order - the padding-date value {@link #hasDataOn} gates to. */
    private Map<String, Integer> zeroBreakdown(List<String> stages) {
        Map<String, Integer> breakdown = new LinkedHashMap<>();
        for (String stage : stages) {
            breakdown.put(stage, 0);
        }
        return breakdown;
    }

    /** Every {@link LifecycleRecord#stageReachedAt()} value and non-null {@code removedAt} across {@code records}. */
    private List<Instant> allTimestamps(List<LifecycleRecord> records) {
        List<Instant> timestamps = new ArrayList<>();
        for (LifecycleRecord record : records) {
            timestamps.addAll(record.stageReachedAt().values());
            if (record.removedAt() != null) {
                timestamps.add(record.removedAt());
            }
        }
        return timestamps;
    }

    private List<LocalDate> datesBetween(LocalDate start, LocalDate end) {
        List<LocalDate> dates = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            dates.add(date);
        }
        return dates;
    }

    /**
     * The number of records that had reached {@code stage} by {@code date} and were still active
     * as of that same date - i.e. not yet {@link LifecycleRecord#removedAt() removed} - using the
     * same {@link #endOfDate(LocalDate)} bound {@link #computeStageBreakdown} uses for {@code asOf},
     * so this series and {@code totalCount} (itself derived from {@code active}, see {@link #build})
     * never disagree about which records currently count. A record removed before it reached
     * {@code stage} correctly never counted anyway, since its {@code stageReachedAt} entry for a
     * stage reached after removal would date from after {@code removedAt} - it is the opposite case,
     * a record removed <em>after</em> reaching {@code stage}, that {@link LifecycleRecord#isActiveAsOf}
     * is needed to exclude here: without it, a removed record that once reached {@code stage} would
     * be counted forever, letting a later stage's cumulative count exceed {@code totalCount} once
     * enough formerly-tracked items are removed - the count going negative downstream in any
     * template computing an "un-X" gap as {@code totalCount - count}.
     */
    private int cumulativeCountAt(List<LifecycleRecord> records, String stage, LocalDate date) {
        Instant endOfDay = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant asOf = endOfDate(date);
        return (int) records.stream()
                .filter(record -> record.isActiveAsOf(asOf))
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

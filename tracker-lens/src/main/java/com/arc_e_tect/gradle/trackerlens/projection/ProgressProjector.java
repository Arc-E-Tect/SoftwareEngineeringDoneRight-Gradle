package com.arc_e_tect.gradle.trackerlens.projection;

import com.arc_e_tect.gradle.trackerlens.tracker.LifecycleRecord;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Extrapolates when a tracker will reach its total item count, from the historical rate at which
 * items have reached its final stage.
 *
 * <p>The lookback window adapts to how much history is actually available, capped at 90 days, with
 * a 7-day floor below which no projection is produced at all: a fast-moving team can generate a lot
 * of signal quickly, and a low-confidence estimate with a visible disclaimer is more useful than
 * months of silence. {@link Confidence} reflects how much of that window was actually available and
 * is meant to drive free-text disclaimer wording in the dashboard - it is never part of the CSS
 * contract.</p>
 *
 * <p>The projected date itself is workday-aware: {@code daysToCompletion} is treated as a count of
 * business days (Monday-Friday) of remaining work, not calendar days, and is converted to a calendar
 * date by skipping over any Saturdays and Sundays it spans - so a projection of "7 days to go" lands
 * 9 calendar days out, not 7, since it implicitly crosses one weekend. Nobody's plan should read as
 * expecting weekend work.</p>
 */
public class ProgressProjector {

    private static final int MAX_LOOKBACK_DAYS = 90;
    private static final int MIN_LOOKBACK_DAYS = 7;
    private static final int MEDIUM_CONFIDENCE_THRESHOLD_DAYS = 30;
    private static final int BUSINESS_DAYS_PER_WEEK = 5;
    private static final ZoneOffset PROJECTION_ZONE = ZoneOffset.UTC;

    /** Creates a new {@code ProgressProjector}. */
    public ProgressProjector() {}

    /**
     * Projects when {@code records} will collectively reach {@code totalCount} items at
     * {@code finalStage}.
     *
     * @param records    the tracker's current records
     * @param finalStage the stage that marks an item as complete
     * @param totalCount total number of items the tracker is tracking - the caller's own active-only
     *                   count, e.g. {@code TrackerViewFactory}'s {@code totalCount}
     * @param now        the instant to project from
     * @return the projection, or {@link java.util.Optional#empty()} when fewer than 7 days of
     *         lookback history is available, or when the resulting velocity is zero or negative
     */
    public Optional<Projection> project(
            List<LifecycleRecord> records, String finalStage, int totalCount, Instant now) {
        Objects.requireNonNull(records, "records");
        Objects.requireNonNull(finalStage, "finalStage");
        Objects.requireNonNull(now, "now");

        // isActiveAsOf(now), not the raw record, so a record that reached finalStage and was later
        // removed stops counting toward currentCount/velocityPerDay from its removal on - otherwise
        // currentCount, built from every record that ever reached finalStage regardless of current
        // removal, can exceed the caller's own active-only totalCount once enough formerly-tracked
        // items are removed, understating "remaining" (line below) and rendering a currentCount past
        // totalCount verbatim in the default template's own "{{currentCount}} of {{totalCount}}
        // complete" line - the exact bug already fixed for chartSeries in TrackerViewFactory.
        List<Instant> reachedTimestamps = records.stream()
                .filter(record -> record.isActiveAsOf(now))
                .map(record -> record.stageReachedAt().get(finalStage))
                .filter(Objects::nonNull)
                .toList();
        if (reachedTimestamps.isEmpty()) {
            return Optional.empty();
        }

        Instant earliest = reachedTimestamps.stream().min(Comparator.naturalOrder()).orElseThrow();
        long daysSinceEarliest = Duration.between(earliest, now).toDays();
        long lookbackDays = Math.min(MAX_LOOKBACK_DAYS, daysSinceEarliest);
        if (lookbackDays < MIN_LOOKBACK_DAYS) {
            return Optional.empty();
        }

        Instant windowStart = now.minus(Duration.ofDays(lookbackDays));
        long reachedWithinWindow = reachedTimestamps.stream()
                .filter(timestamp -> !timestamp.isBefore(windowStart))
                .count();
        double velocityPerDay = (double) reachedWithinWindow / lookbackDays;
        if (velocityPerDay <= 0) {
            return Optional.empty();
        }

        int currentCount = reachedTimestamps.size();
        double remaining = Math.max(0, totalCount - currentCount);
        double daysToCompletion = remaining / velocityPerDay;
        Instant projectedDate = addBusinessDays(now, daysToCompletion);

        Confidence confidence = lookbackDays >= MAX_LOOKBACK_DAYS ? Confidence.HIGH
                : lookbackDays >= MEDIUM_CONFIDENCE_THRESHOLD_DAYS ? Confidence.MEDIUM
                : Confidence.LOW;

        return Optional.of(new Projection(projectedDate, currentCount, totalCount, velocityPerDay, confidence));
    }

    /**
     * Adds {@code businessDays} business days (Monday-Friday) to {@code start}, skipping over any
     * Saturdays and Sundays spanned along the way.
     *
     * <p>The whole-day part is added in O(1) via a weeks-then-remainder split - {@code weeks} full
     * 7-calendar-day jumps (which never change which day of the week {@code start} lands on) plus a
     * remainder of fewer than {@link #BUSINESS_DAYS_PER_WEEK} single-day steps, each corrected
     * forward past a weekend if it lands on one - rather than a loop bounded by the (potentially
     * large) day count itself. The fractional part is added as a plain calendar-time offset
     * afterward, matching the sub-day precision the previous calendar-days-only calculation had.</p>
     *
     * @param start        the instant to project from
     * @param businessDays the number of business days of remaining work, may be fractional
     * @return {@code start} plus that many business days, expressed as a calendar instant
     */
    private static Instant addBusinessDays(Instant start, double businessDays) {
        long wholeBusinessDays = (long) Math.floor(businessDays);
        double fraction = businessDays - wholeBusinessDays;

        LocalDate startDate = LocalDate.ofInstant(start, PROJECTION_ZONE);
        long weeks = wholeBusinessDays / BUSINESS_DAYS_PER_WEEK;
        long remainder = wholeBusinessDays % BUSINESS_DAYS_PER_WEEK;

        LocalDate date = skipWeekend(startDate.plusWeeks(weeks));
        for (long i = 0; i < remainder; i++) {
            date = skipWeekend(date.plusDays(1));
        }

        Duration timeOfDay = Duration.between(startDate.atStartOfDay(PROJECTION_ZONE).toInstant(), start);
        return date.atStartOfDay(PROJECTION_ZONE).toInstant()
                .plus(timeOfDay)
                .plusMillis(Math.round(fraction * Duration.ofDays(1).toMillis()));
    }

    private static LocalDate skipWeekend(LocalDate date) {
        return switch (date.getDayOfWeek()) {
            case SATURDAY -> date.plusDays(2);
            case SUNDAY -> date.plusDays(1);
            default -> date;
        };
    }
}

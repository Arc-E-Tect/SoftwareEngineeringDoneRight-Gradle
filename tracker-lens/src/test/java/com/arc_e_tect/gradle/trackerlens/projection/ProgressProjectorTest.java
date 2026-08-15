package com.arc_e_tect.gradle.trackerlens.projection;

import com.arc_e_tect.gradle.trackerlens.tracker.LifecycleRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProgressProjector")
class ProgressProjectorTest {

    private static final Instant NOW = Instant.parse("2026-08-13T00:00:00Z");

    private final ProgressProjector projector = new ProgressProjector();

    @Test
    @DisplayName("projectShouldReturnEmptyWhenLookbackIsBelowSevenDayFloor")
    void projectShouldReturnEmptyWhenLookbackIsBelowSevenDayFloor() {
        List<LifecycleRecord> records = recordsReachingStageAt(
                "implemented", NOW.minus(Duration.ofDays(3)), NOW.minus(Duration.ofDays(1)));

        Optional<Projection> projection = projector.project(records, "implemented", 10, NOW);

        assertThat(projection).isEmpty();
    }

    @Test
    @DisplayName("projectShouldReturnEmptyWhenNoRecordEverReachedFinalStage")
    void projectShouldReturnEmptyWhenNoRecordEverReachedFinalStage() {
        List<LifecycleRecord> records = List.of(new LifecycleRecord(
                "1", "label", null, Map.of("listed", NOW.minus(Duration.ofDays(30))), NOW, null));

        Optional<Projection> projection = projector.project(records, "implemented", 10, NOW);

        assertThat(projection).isEmpty();
    }

    @Test
    @DisplayName("projectShouldReturnEmptyWhenVelocityIsZero")
    void projectShouldReturnEmptyWhenVelocityIsZero() {
        // Reached the final stage only outside the lookback window, so velocity within the window is zero.
        List<LifecycleRecord> records = recordsReachingStageAt(
                "implemented", NOW.minus(Duration.ofDays(100)));

        Optional<Projection> projection = projector.project(records, "implemented", 10, NOW);

        assertThat(projection).isEmpty();
    }

    @Test
    @DisplayName("projectShouldReportLowConfidenceUnderThirtyDaysOfLookback")
    void projectShouldReportLowConfidenceUnderThirtyDaysOfLookback() {
        List<LifecycleRecord> records = recordsReachingStageAt(
                "implemented",
                NOW.minus(Duration.ofDays(10)), NOW.minus(Duration.ofDays(5)), NOW.minus(Duration.ofDays(1)));

        Optional<Projection> projection = projector.project(records, "implemented", 10, NOW);

        assertThat(projection).isPresent().get()
                .extracting(Projection::confidence).isEqualTo(Confidence.LOW);
    }

    @Test
    @DisplayName("projectShouldReportMediumConfidenceBetweenThirtyAndEightyNineDaysOfLookback")
    void projectShouldReportMediumConfidenceBetweenThirtyAndEightyNineDaysOfLookback() {
        List<LifecycleRecord> records = recordsReachingStageAt(
                "implemented", NOW.minus(Duration.ofDays(45)), NOW.minus(Duration.ofDays(10)));

        Optional<Projection> projection = projector.project(records, "implemented", 10, NOW);

        assertThat(projection).isPresent().get()
                .extracting(Projection::confidence).isEqualTo(Confidence.MEDIUM);
    }

    @Test
    @DisplayName("projectShouldReportHighConfidenceAtNinetyDayLookbackCap")
    void projectShouldReportHighConfidenceAtNinetyDayLookbackCap() {
        List<LifecycleRecord> records = recordsReachingStageAt(
                "implemented", NOW.minus(Duration.ofDays(200)), NOW.minus(Duration.ofDays(10)));

        Optional<Projection> projection = projector.project(records, "implemented", 10, NOW);

        assertThat(projection).isPresent().get()
                .extracting(Projection::confidence).isEqualTo(Confidence.HIGH);
    }

    @Test
    @DisplayName("projectShouldComputeProjectedDateFromVelocityAndRemainingCount")
    void projectShouldComputeProjectedDateFromVelocityAndRemainingCount() {
        // 2 reached within a 10-day lookback -> velocity 0.2/day; 8 remaining of 10 total -> 40
        // business days out. NOW (2026-08-13) is a Thursday; 40 business days is an exact multiple
        // of 5, so it lands on a Thursday 8 weeks later with no remainder-day weekend correction
        // needed - 56 calendar days, not 40.
        List<LifecycleRecord> records = recordsReachingStageAt(
                "implemented", NOW.minus(Duration.ofDays(10)), NOW.minus(Duration.ofDays(1)));

        Optional<Projection> projection = projector.project(records, "implemented", 10, NOW);

        assertThat(projection).isPresent().get()
                .extracting(Projection::projectedDate).isEqualTo(NOW.plus(Duration.ofDays(56)));
    }

    @Test
    @DisplayName("projectShouldSkipWeekendsWhenConvertingBusinessDaysToACalendarProjectedDate")
    void projectShouldSkipWeekendsWhenConvertingBusinessDaysToACalendarProjectedDate() {
        // A Monday. 7 reached, all exactly at the 7-day lookback boundary -> velocity 7/7 = 1/day;
        // 7 remaining of 14 total -> exactly 7 business days out. From a Monday, 7 business days
        // lands on the Wednesday of the week after next - 9 calendar days out, not 7, since it
        // crosses one full weekend (the plugin's own worked example: "7 days to go" should never
        // read as 7 calendar days when that implicitly assumes weekend work).
        Instant monday = Instant.parse("2026-08-10T00:00:00Z");
        List<LifecycleRecord> records = recordsReachingStageAt("implemented",
                monday.minus(Duration.ofDays(7)), monday.minus(Duration.ofDays(7)), monday.minus(Duration.ofDays(7)),
                monday.minus(Duration.ofDays(7)), monday.minus(Duration.ofDays(7)), monday.minus(Duration.ofDays(7)),
                monday.minus(Duration.ofDays(7)));

        Optional<Projection> projection = projector.project(records, "implemented", 14, monday);

        assertThat(projection).isPresent().get()
                .extracting(Projection::projectedDate).isEqualTo(monday.plus(Duration.ofDays(9)));
    }

    private List<LifecycleRecord> recordsReachingStageAt(String stage, Instant... instants) {
        List<LifecycleRecord> records = new ArrayList<>();
        int i = 0;
        for (Instant instant : instants) {
            records.add(new LifecycleRecord(
                    "id" + i++, "label", null, Map.of(stage, instant), instant, null));
        }
        return records;
    }
}

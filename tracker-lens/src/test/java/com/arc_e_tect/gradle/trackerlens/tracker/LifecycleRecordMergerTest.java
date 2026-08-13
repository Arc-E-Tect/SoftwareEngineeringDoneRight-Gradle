package com.arc_e_tect.gradle.trackerlens.tracker;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LifecycleRecordMerger")
class LifecycleRecordMergerTest {

    private static final Instant NOW = Instant.parse("2026-08-13T00:00:00Z");
    private static final List<String> STAGES = List.of("declared", "implemented", "verified");

    private final LifecycleRecordMerger merger = new LifecycleRecordMerger();

    @Test
    @DisplayName("mergeShouldPassThroughARecordWithNoIdCollision")
    void mergeShouldPassThroughARecordWithNoIdCollision() {
        LifecycleRecord record = new LifecycleRecord(
                "ep1", "GET /orders", null, Map.of("declared", NOW), NOW, null);

        List<LifecycleRecord> merged = merger.merge(STAGES, List.of(record));

        assertThat(merged).containsExactly(record);
    }

    @Test
    @DisplayName("mergeShouldCombineComplementaryStagesFromDifferentFilesIntoOneRecord")
    void mergeShouldCombineComplementaryStagesFromDifferentFilesIntoOneRecord() {
        Instant declared = NOW.minus(Duration.ofDays(10));
        Instant implemented = NOW.minus(Duration.ofDays(5));
        LifecycleRecord fromShadow = new LifecycleRecord(
                "ep1", "GET /orders", null, Map.of("declared", declared), declared, null);
        LifecycleRecord fromMirage = new LifecycleRecord(
                "ep1", "GET /orders", "com.example.OrderController", Map.of("implemented", implemented), implemented, null);

        List<LifecycleRecord> merged = merger.merge(STAGES, List.of(fromShadow, fromMirage));

        assertThat(merged).hasSize(1);
        assertThat(merged.get(0).stageReachedAt()).containsExactly(
                Map.entry("declared", declared), Map.entry("implemented", implemented));
    }

    @Test
    @DisplayName("mergeShouldKeepTheEarliestTimestampWhenTheSameStageIsReportedByMultipleFiles")
    void mergeShouldKeepTheEarliestTimestampWhenTheSameStageIsReportedByMultipleFiles() {
        Instant earlier = NOW.minus(Duration.ofDays(10));
        Instant later = NOW.minus(Duration.ofDays(3));
        LifecycleRecord first = new LifecycleRecord("ep1", "GET /orders", null, Map.of("declared", later), later, null);
        LifecycleRecord second = new LifecycleRecord("ep1", "GET /orders", null, Map.of("declared", earlier), earlier, null);

        List<LifecycleRecord> merged = merger.merge(STAGES, List.of(first, second));

        assertThat(merged.get(0).stageReachedAt().get("declared")).isEqualTo(earlier);
    }

    @Test
    @DisplayName("mergeShouldKeepTheLatestLastSeenAtAcrossFiles")
    void mergeShouldKeepTheLatestLastSeenAtAcrossFiles() {
        Instant earlierSeen = NOW.minus(Duration.ofDays(5));
        Instant laterSeen = NOW.minus(Duration.ofDays(1));
        LifecycleRecord first = new LifecycleRecord("ep1", "GET /orders", null, Map.of("declared", NOW), earlierSeen, null);
        LifecycleRecord second = new LifecycleRecord("ep1", "GET /orders", null, Map.of(), laterSeen, null);

        List<LifecycleRecord> merged = merger.merge(STAGES, List.of(first, second));

        assertThat(merged.get(0).lastSeenAt()).isEqualTo(laterSeen);
    }

    @Test
    @DisplayName("mergeShouldNotMarkItemRemovedWhenOnlySomeSourcesReportItRemoved")
    void mergeShouldNotMarkItemRemovedWhenOnlySomeSourcesReportItRemoved() {
        LifecycleRecord stillActive = new LifecycleRecord("ep1", "GET /orders", null, Map.of("declared", NOW), NOW, null);
        LifecycleRecord reportedRemoved = new LifecycleRecord(
                "ep1", "GET /orders", null, Map.of(), NOW.minus(Duration.ofDays(1)), NOW.minus(Duration.ofDays(1)));

        List<LifecycleRecord> merged = merger.merge(STAGES, List.of(stillActive, reportedRemoved));

        assertThat(merged.get(0).removedAt()).isNull();
    }

    @Test
    @DisplayName("mergeShouldMarkItemRemovedWhenEverySourceReportsItRemoved")
    void mergeShouldMarkItemRemovedWhenEverySourceReportsItRemoved() {
        Instant firstRemoved = NOW.minus(Duration.ofDays(3));
        Instant lastRemoved = NOW.minus(Duration.ofDays(1));
        LifecycleRecord first = new LifecycleRecord("ep1", "GET /orders", null, Map.of(), NOW, firstRemoved);
        LifecycleRecord second = new LifecycleRecord("ep1", "GET /orders", null, Map.of(), NOW, lastRemoved);

        List<LifecycleRecord> merged = merger.merge(STAGES, List.of(first, second));

        assertThat(merged.get(0).removedAt()).isEqualTo(lastRemoved);
    }

    @Test
    @DisplayName("mergeShouldPreferANonBlankGroupOverANullOneFromAnotherSource")
    void mergeShouldPreferANonBlankGroupOverANullOneFromAnotherSource() {
        LifecycleRecord withoutGroup = new LifecycleRecord("ep1", "GET /orders", null, Map.of("declared", NOW), NOW, null);
        LifecycleRecord withGroup = new LifecycleRecord(
                "ep1", "GET /orders", "com.example.OrderController", Map.of("implemented", NOW), NOW, null);

        List<LifecycleRecord> merged = merger.merge(STAGES, List.of(withoutGroup, withGroup));

        assertThat(merged.get(0).group()).isEqualTo("com.example.OrderController");
    }
}

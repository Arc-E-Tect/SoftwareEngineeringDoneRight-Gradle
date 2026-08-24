package com.arc_e_tect.gradle.doppelganger.progress;

import com.arc_e_tect.gradle.detector.core.model.HttpVerb;
import com.arc_e_tect.gradle.detector.core.openapi.DescribedEndpoint;
import com.arc_e_tect.gradle.doppelganger.detect.EndpointResponseCoverage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ResponseCoverageHistoryUpdater")
class ResponseCoverageHistoryUpdaterTest {

    private final ResponseCoverageHistoryUpdater updater = new ResponseCoverageHistoryUpdater();

    @Test
    @DisplayName("a first-seen response code is stamped with firstDeclaredAt, lastSeenAt, and, if covered, firstCoveredAt")
    void firstSeenCoveredCodeIsStampedOnFirstRun() {
        Instant now = Instant.parse("2026-08-24T10:00:00Z");
        EndpointResponseCoverage row = coverage("/orders", HttpVerb.GET, Map.of("200", 1));

        Map<String, ResponseCoverageRecord> updated = updater.update(Map.of(), List.of(row), now);

        ResponseCoverageRecord record = onlyRecord(updated);
        assertThat(record.responseCode()).isEqualTo("200");
        assertThat(record.testCount()).isEqualTo(1);
        assertThat(record.firstDeclaredAt()).isEqualTo(now);
        assertThat(record.firstCoveredAt()).isEqualTo(now);
        assertThat(record.lastSeenAt()).isEqualTo(now);
        assertThat(record.removedAt()).isNull();
    }

    @Test
    @DisplayName("a declared but uncovered response code gets firstDeclaredAt but not firstCoveredAt")
    void declaredButUncoveredCodeHasNoFirstCoveredAt() {
        Instant now = Instant.parse("2026-08-24T10:00:00Z");
        EndpointResponseCoverage row = coverage("/orders", HttpVerb.GET, Map.of("404", 0));

        Map<String, ResponseCoverageRecord> updated = updater.update(Map.of(), List.of(row), now);

        assertThat(onlyRecord(updated).firstCoveredAt()).isNull();
    }

    @Test
    @DisplayName("firstCoveredAt is stamped the first time testCount becomes positive, on a later run")
    void firstCoveredAtStampedWhenCodeBecomesCoveredLater() {
        Instant declared = Instant.parse("2026-08-01T00:00:00Z");
        Instant covered = Instant.parse("2026-08-10T00:00:00Z");
        Map<String, ResponseCoverageRecord> afterFirstRun =
                updater.update(Map.of(), List.of(coverage("/orders", HttpVerb.GET, Map.of("404", 0))), declared);

        Map<String, ResponseCoverageRecord> afterSecondRun = updater.update(
                afterFirstRun, List.of(coverage("/orders", HttpVerb.GET, Map.of("404", 1))), covered);

        ResponseCoverageRecord record = onlyRecord(afterSecondRun);
        assertThat(record.firstDeclaredAt()).isEqualTo(declared);
        assertThat(record.firstCoveredAt()).isEqualTo(covered);
        assertThat(record.testCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("firstDeclaredAt and firstCoveredAt are never overwritten once set")
    void milestonesAreNeverOverwritten() {
        Instant first = Instant.parse("2026-08-01T00:00:00Z");
        Instant second = Instant.parse("2026-08-15T00:00:00Z");
        Map<String, ResponseCoverageRecord> afterFirstRun =
                updater.update(Map.of(), List.of(coverage("/orders", HttpVerb.GET, Map.of("200", 3))), first);

        Map<String, ResponseCoverageRecord> afterSecondRun = updater.update(
                afterFirstRun, List.of(coverage("/orders", HttpVerb.GET, Map.of("200", 5))), second);

        ResponseCoverageRecord record = onlyRecord(afterSecondRun);
        assertThat(record.firstDeclaredAt()).isEqualTo(first);
        assertThat(record.firstCoveredAt()).isEqualTo(first);
        assertThat(record.testCount()).isEqualTo(5);
        assertThat(record.lastSeenAt()).isEqualTo(second);
    }

    @Test
    @DisplayName("a response code missing from a later run is stamped with removedAt, not deleted")
    void missingCodeIsStampedRemovedNotDeleted() {
        Instant first = Instant.parse("2026-08-01T00:00:00Z");
        Instant second = Instant.parse("2026-08-15T00:00:00Z");
        Map<String, ResponseCoverageRecord> afterFirstRun =
                updater.update(Map.of(), List.of(coverage("/orders", HttpVerb.GET, Map.of("200", 1))), first);

        Map<String, ResponseCoverageRecord> afterSecondRun = updater.update(afterFirstRun, List.of(), second);

        ResponseCoverageRecord record = onlyRecord(afterSecondRun);
        assertThat(record.removedAt()).isEqualTo(second);
        assertThat(record.testCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("a removed record's removedAt is reset to null when it reappears")
    void reappearingRecordHasRemovedAtReset() {
        Instant first = Instant.parse("2026-08-01T00:00:00Z");
        Instant removed = Instant.parse("2026-08-10T00:00:00Z");
        Instant reappeared = Instant.parse("2026-08-20T00:00:00Z");
        Map<String, ResponseCoverageRecord> afterFirstRun =
                updater.update(Map.of(), List.of(coverage("/orders", HttpVerb.GET, Map.of("200", 1))), first);
        Map<String, ResponseCoverageRecord> afterRemoval = updater.update(afterFirstRun, List.of(), removed);

        Map<String, ResponseCoverageRecord> afterReappearance = updater.update(
                afterRemoval, List.of(coverage("/orders", HttpVerb.GET, Map.of("200", 2))), reappeared);

        assertThat(onlyRecord(afterReappearance).removedAt()).isNull();
    }

    @Test
    @DisplayName("a removedAt already set is never overwritten by a later run that still doesn't see it")
    void removedAtIsStampedOnlyOnce() {
        Instant first = Instant.parse("2026-08-01T00:00:00Z");
        Instant removed = Instant.parse("2026-08-10T00:00:00Z");
        Instant stillMissing = Instant.parse("2026-08-20T00:00:00Z");
        Map<String, ResponseCoverageRecord> afterFirstRun =
                updater.update(Map.of(), List.of(coverage("/orders", HttpVerb.GET, Map.of("200", 1))), first);
        Map<String, ResponseCoverageRecord> afterRemoval = updater.update(afterFirstRun, List.of(), removed);

        Map<String, ResponseCoverageRecord> stillRemoved = updater.update(afterRemoval, List.of(), stillMissing);

        assertThat(onlyRecord(stillRemoved).removedAt()).isEqualTo(removed);
    }

    @Test
    @DisplayName("each declared response code for an endpoint gets its own record")
    void eachResponseCodeGetsItsOwnRecord() {
        Instant now = Instant.parse("2026-08-24T10:00:00Z");
        EndpointResponseCoverage row = coverage("/v1/foobars", HttpVerb.GET, Map.of("200", 2, "404", 1));

        Map<String, ResponseCoverageRecord> updated = updater.update(Map.of(), List.of(row), now);

        assertThat(updated).hasSize(2);
        assertThat(updated.values())
                .extracting(ResponseCoverageRecord::responseCode, ResponseCoverageRecord::testCount)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("200", 2),
                        org.assertj.core.groups.Tuple.tuple("404", 1));
    }

    private EndpointResponseCoverage coverage(String path, HttpVerb verb, Map<String, Integer> testCountByResponseCode) {
        DescribedEndpoint endpoint = new DescribedEndpoint(
                verb, path, null, List.of(), List.copyOf(testCountByResponseCode.keySet()));
        return new EndpointResponseCoverage(endpoint, testCountByResponseCode.values().stream()
                .mapToInt(Integer::intValue).sum(), 0, testCountByResponseCode);
    }

    private ResponseCoverageRecord onlyRecord(Map<String, ResponseCoverageRecord> records) {
        assertThat(records).hasSize(1);
        return records.values().iterator().next();
    }
}

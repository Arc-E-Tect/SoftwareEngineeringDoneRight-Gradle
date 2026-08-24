package com.arc_e_tect.gradle.doppelganger.progress;

import com.arc_e_tect.gradle.detector.core.progress.EndpointFingerprint;
import com.arc_e_tect.gradle.doppelganger.detect.EndpointResponseCoverage;

import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Produces the next {@link ResponseCoverageRecord} map to persist, from the previously persisted
 * map and this run's {@link EndpointResponseCoverage} rows - the response-coverage counterpart of
 * {@code api-detector-core}'s {@code ContractHistoryUpdater}.
 *
 * <p>Unlike that shared updater, {@link ResponseCoverageRecord#testCount()} is a live gauge:
 * refreshed on every run that observes the (endpoint, response code) pair, not a milestone stamped
 * once. {@link ResponseCoverageRecord#firstDeclaredAt()} and
 * {@link ResponseCoverageRecord#firstCoveredAt()} - the latter set the first time
 * {@code testCount > 0} - are the only fields with that once-only semantics. Every previously
 * persisted record whose fingerprint is not seen this run keeps its last known values with
 * {@code removedAt} stamped the first time it goes missing; a record that reappears has
 * {@code removedAt} reset to {@code null}. Records are never deleted.</p>
 */
public class ResponseCoverageHistoryUpdater {

    private final EndpointFingerprint fingerprinter = new EndpointFingerprint();

    /** Creates a new {@code ResponseCoverageHistoryUpdater}. */
    public ResponseCoverageHistoryUpdater() {}

    /**
     * Computes the updated history map to persist.
     *
     * @param existing   the previously persisted history, keyed by fingerprint; empty on a first
     *                   run
     * @param currentRun this run's response coverage rows, computed with
     *                   {@code includeResponseCoverage} enabled
     * @param now        the instant to stamp newly-reached milestones and newly-observed removals
     *                   with
     * @return the updated history map, keyed by fingerprint
     */
    public Map<String, ResponseCoverageRecord> update(
            Map<String, ResponseCoverageRecord> existing, List<EndpointResponseCoverage> currentRun, Instant now) {
        Map<String, ResponseCoverageRecord> updated = new LinkedHashMap<>(existing);
        Set<String> seen = new HashSet<>();

        for (EndpointResponseCoverage row : currentRun) {
            String endpointFingerprint = fingerprinter.fingerprint(row.endpoint());
            for (Map.Entry<String, Integer> entry : row.testCountByResponseCode().entrySet()) {
                String responseCode = entry.getKey();
                int testCount = entry.getValue();
                String fingerprint = endpointFingerprint + "-" + responseCode;
                seen.add(fingerprint);

                ResponseCoverageRecord record = updated.getOrDefault(fingerprint, blank(fingerprint, responseCode));
                Instant firstDeclaredAt = record.firstDeclaredAt() != null ? record.firstDeclaredAt() : now;
                Instant firstCoveredAt = record.firstCoveredAt() != null ? record.firstCoveredAt()
                        : testCount > 0 ? now : null;

                updated.put(fingerprint, new ResponseCoverageRecord(
                        fingerprint, row.endpoint().verb(), row.endpoint().path(), responseCode, testCount,
                        firstDeclaredAt, firstCoveredAt, now, null));
            }
        }

        for (Map.Entry<String, ResponseCoverageRecord> entry : existing.entrySet()) {
            if (!seen.contains(entry.getKey())) {
                updated.put(entry.getKey(), markRemoved(entry.getValue(), now));
            }
        }

        return updated;
    }

    private ResponseCoverageRecord blank(String fingerprint, String responseCode) {
        return new ResponseCoverageRecord(fingerprint, null, null, responseCode, 0, null, null, null, null);
    }

    private ResponseCoverageRecord markRemoved(ResponseCoverageRecord record, Instant now) {
        if (record.removedAt() != null) {
            return record;
        }
        return new ResponseCoverageRecord(
                record.fingerprint(), record.verb(), record.path(), record.responseCode(), record.testCount(),
                record.firstDeclaredAt(), record.firstCoveredAt(), record.lastSeenAt(), now);
    }
}

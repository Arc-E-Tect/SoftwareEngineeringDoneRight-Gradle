package com.arc_e_tect.gradle.doppelganger.progress;

import com.arc_e_tect.gradle.detector.core.model.HttpVerb;

import java.time.Instant;

/**
 * Persisted, per-endpoint-and-response-code history of contract test coverage, keyed by
 * {@code <endpoint fingerprint>-<response code>} (see {@link ResponseCoverageHistoryUpdater}).
 *
 * <p>Deliberately separate from {@code api-detector-core}'s cross-plugin
 * {@code ContractProgressRecord}: response coverage is a Doppelganger-only concern with a different
 * shape - {@link #testCount()} is a live gauge refreshed every run, not a milestone timestamp
 * stamped once.</p>
 *
 * @param fingerprint    the stable identifier for this endpoint + response code pair
 * @param verb           the endpoint's current HTTP verb; not part of the key, refreshed on every
 *                       run that observes this endpoint
 * @param path           the endpoint's current path template; not part of the key, refreshed on
 *                       every run that observes this endpoint
 * @param responseCode   the response code this record tracks, e.g. {@code "200"}, {@code "404"}
 * @param testCount      the number of contract tests detected to cover this response code as of
 *                       the most recent run that observed it
 * @param firstDeclaredAt when this response code was first observed declared in the OpenAPI
 *                       documentation, or {@code null} if it never has been
 * @param firstCoveredAt when this response code was first observed covered by at least one
 *                       contract test ({@code testCount > 0}), or {@code null} if it never has been
 * @param lastSeenAt     when this response code was last present in a run that could see it, or
 *                       {@code null} for a record that has never actually been seen
 * @param removedAt      when this response code was first observed missing from the endpoint's
 *                       declared response codes, or {@code null} while it's still declared
 */
public record ResponseCoverageRecord(
        String fingerprint,
        HttpVerb verb,
        String path,
        String responseCode,
        int testCount,
        Instant firstDeclaredAt,
        Instant firstCoveredAt,
        Instant lastSeenAt,
        Instant removedAt) {
}

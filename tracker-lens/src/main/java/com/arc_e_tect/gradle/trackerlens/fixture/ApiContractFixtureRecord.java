package com.arc_e_tect.gradle.trackerlens.fixture;

import java.time.Instant;

/**
 * One generated row of the API-contract progress history, in
 * {@link com.arc_e_tect.gradle.trackerlens.tracker.ApiContractTrackerSource}'s own field shape -
 * {@code fingerprint}, {@code verb}, {@code path}, {@code declaringClass}, {@code declaredAt},
 * {@code implementedAt}, {@code stubbedAt}, {@code verifiedAt}, {@code lastSeenAt},
 * {@code removedAt}.
 *
 * @param fingerprint    stable identifier, unique within the generated fixture
 * @param verb           the HTTP verb, e.g. {@code GET}
 * @param path           the endpoint path
 * @param declaringClass the implementing class's fully-qualified name, or {@code null} when
 *                        {@code implementedAt} is {@code null}
 * @param declaredAt     when the endpoint was first declared in the contract, or {@code null}
 * @param implementedAt  when a real implementation was first detected, or {@code null}
 * @param stubbedAt      when a consumer-facing stub first appeared, or {@code null} - independent
 *                        of every other field
 * @param verifiedAt     when a passing test first confirmed the implementation matches the
 *                        contract, or {@code null}
 * @param lastSeenAt     when this endpoint was last observed present
 * @param removedAt      when this endpoint was first observed missing, or {@code null} while still
 *                        present
 */
public record ApiContractFixtureRecord(
        String fingerprint,
        String verb,
        String path,
        String declaringClass,
        Instant declaredAt,
        Instant implementedAt,
        Instant stubbedAt,
        Instant verifiedAt,
        Instant lastSeenAt,
        Instant removedAt) {
}

package com.arc_e_tect.gradle.mirage.report;

import com.arc_e_tect.gradle.detector.core.openapi.DescribedEndpoint;

/**
 * One mirage API excluded from {@code == Mirage APIs}/{@code failOnMirage} by a configured
 * exclusion rule, paired with its current-run {@link StubStatus} for display in the
 * {@code == Excluded Mirage APIs} report section.
 *
 * @param endpoint   the excluded, described-but-unimplemented endpoint
 * @param stubStatus the endpoint's current-run WireMock stub status
 */
public record ExcludedMirage(DescribedEndpoint endpoint, StubStatus stubStatus) {
}

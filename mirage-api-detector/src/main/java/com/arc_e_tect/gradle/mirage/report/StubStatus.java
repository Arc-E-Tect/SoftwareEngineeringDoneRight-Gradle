package com.arc_e_tect.gradle.mirage.report;

/**
 * An excluded mirage's current-run WireMock stub status, shown in the
 * {@code == Excluded Mirage APIs} report section. Always computed fresh from this run's stub
 * scan - never sourced from contract history, since excluded endpoints are deliberately kept out
 * of it.
 */
public enum StubStatus {

    /** A matching WireMock stub mapping was found this run. */
    STUBBED,

    /** {@code scanMocks} was enabled but no matching stub mapping was found this run. */
    NOT_STUBBED,

    /** {@code scanMocks} was disabled, so no stub evidence was gathered this run. */
    NOT_SCANNED
}

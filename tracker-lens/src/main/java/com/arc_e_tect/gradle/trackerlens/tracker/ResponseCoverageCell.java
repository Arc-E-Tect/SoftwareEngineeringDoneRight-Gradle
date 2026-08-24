package com.arc_e_tect.gradle.trackerlens.tracker;

/**
 * One cell of the response-coverage snapshot matrix: a currently-declared (verb, path,
 * responseCode) triple, its current test count, and whether that count is positive.
 *
 * <p>Deliberately not a {@link LifecycleRecord}: {@code testCount} is a live gauge with no
 * preserved history (unlike every timestamp {@link LifecycleRecord} carries), and a matrix needs
 * {@code verb}/{@code path}/{@code responseCode} as three independently pivotable fields rather
 * than fused into one {@code label} string - see {@link ResponseCoverageMatrixReader}.</p>
 *
 * @param verb          the HTTP verb, e.g. {@code "GET"}
 * @param path          the endpoint's path template, e.g. {@code "/v1/foobars"}
 * @param responseCode  the response code, e.g. {@code "200"}
 * @param testCount     the number of contract tests currently detected to cover this response code
 * @param covered       {@code true} when {@code testCount > 0}
 */
public record ResponseCoverageCell(String verb, String path, String responseCode, int testCount, boolean covered) {
}

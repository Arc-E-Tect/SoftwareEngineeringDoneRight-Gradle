package com.arc_e_tect.gradle.doppelganger.detect;

import com.arc_e_tect.gradle.detector.core.openapi.DescribedEndpoint;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Computes, for every endpoint both declared in the OpenAPI documentation and implemented by a
 * {@code @RestController} method, how many contract tests exist for it and - only when requested -
 * how many of those tests cover each of its declared response codes.
 *
 * <p>A verified test's status code is matched against a declared response code by <strong>exact
 * string equality only</strong>: a test detected to assert {@code "404"} counts towards a declared
 * {@code "404"} response, never towards a declared {@code "4XX"} range wildcard or a
 * {@code "default"} response, even though either might, in the OpenAPI document's own semantics,
 * legitimately cover that same test. Resolving that would require interpreting the OpenAPI
 * response-matching rules themselves, not just comparing two strings - the same kind of imprecision
 * {@link com.arc_e_tect.gradle.detector.core.model.PathMatcher#matchesConcrete(String, String)}
 * already documents for path matching.</p>
 */
public class ResponseCoverageAnalyzer {

    /** Creates a new {@code ResponseCoverageAnalyzer}. */
    public ResponseCoverageAnalyzer() {}

    /**
     * Computes the coverage rows for {@code declaredAndImplemented}, considering every declared
     * response code including 5xx ones. Equivalent to
     * {@code analyze(declaredAndImplemented, verifiedTests, includeResponseCoverage, false)}.
     *
     * @param declaredAndImplemented  the candidate endpoints - declared in the OpenAPI
     *                                 documentation and implemented by a {@code @RestController}
     *                                 method - carrying their declared response codes
     * @param verifiedTests            every piece of verification evidence gathered from the
     *                                 enabled {@link ContractVerificationSource}s
     * @param includeResponseCoverage  whether to compute the per-response-code breakdown at all;
     *                                 when {@code false}, {@link EndpointResponseCoverage#testCountByResponseCode()}
     *                                 is empty and {@link EndpointResponseCoverage#untrackedTestCount()}
     *                                 is {@code 0} for every row - the breakdown is not merely
     *                                 hidden, it is never computed
     * @return one {@link EndpointResponseCoverage} per candidate, in {@code declaredAndImplemented}'s
     *         order
     */
    public List<EndpointResponseCoverage> analyze(
            List<DescribedEndpoint> declaredAndImplemented, List<VerifiedContractTest> verifiedTests,
            boolean includeResponseCoverage) {
        return analyze(declaredAndImplemented, verifiedTests, includeResponseCoverage, false);
    }

    /**
     * Computes the coverage rows for {@code declaredAndImplemented}.
     *
     * @param declaredAndImplemented  the candidate endpoints - declared in the OpenAPI
     *                                 documentation and implemented by a {@code @RestController}
     *                                 method - carrying their declared response codes
     * @param verifiedTests            every piece of verification evidence gathered from the
     *                                 enabled {@link ContractVerificationSource}s
     * @param includeResponseCoverage  whether to compute the per-response-code breakdown at all;
     *                                 when {@code false}, {@link EndpointResponseCoverage#testCountByResponseCode()}
     *                                 is empty and {@link EndpointResponseCoverage#untrackedTestCount()}
     *                                 is {@code 0} for every row - the breakdown is not merely
     *                                 hidden, it is never computed
     * @param ignore5xx                whether to exclude 5xx response codes - both an exact code
     *                                 (e.g. {@code "500"}) and the {@code "5XX"} range wildcard -
     *                                 from consideration entirely: neither the row's
     *                                 {@link EndpointResponseCoverage#declaredResponseCodes()} nor
     *                                 its {@link EndpointResponseCoverage#testCountByResponseCode()}
     *                                 include one, and a test detected to assert a 5xx status
     *                                 contributes to neither a specific code's count nor
     *                                 {@link EndpointResponseCoverage#untrackedTestCount()} - though
     *                                 it's still counted in
     *                                 {@link EndpointResponseCoverage#contractTestCount()}
     * @return one {@link EndpointResponseCoverage} per candidate, in {@code declaredAndImplemented}'s
     *         order
     */
    public List<EndpointResponseCoverage> analyze(
            List<DescribedEndpoint> declaredAndImplemented, List<VerifiedContractTest> verifiedTests,
            boolean includeResponseCoverage, boolean ignore5xx) {
        List<EndpointResponseCoverage> results = new ArrayList<>();
        for (DescribedEndpoint candidate : declaredAndImplemented) {
            List<VerifiedContractTest> matching = verifiedTests.stream()
                    .filter(test -> ContractEvidenceMatcher.verifies(test.endpoint(), candidate))
                    .toList();

            DescribedEndpoint effectiveCandidate = ignore5xx ? withoutFiveXxResponseCodes(candidate) : candidate;

            if (!includeResponseCoverage) {
                results.add(new EndpointResponseCoverage(effectiveCandidate, matching.size(), 0, Map.of()));
                continue;
            }

            Map<String, Integer> testCountByResponseCode = new LinkedHashMap<>();
            for (String responseCode : effectiveCandidate.responseCodes()) {
                testCountByResponseCode.put(responseCode, 0);
            }
            int untracked = 0;
            for (VerifiedContractTest test : matching) {
                String statusCode = test.statusCode();
                if (ignore5xx && isFiveXx(statusCode)) {
                    continue;
                }
                if (statusCode != null && testCountByResponseCode.containsKey(statusCode)) {
                    testCountByResponseCode.merge(statusCode, 1, Integer::sum);
                } else {
                    untracked++;
                }
            }
            results.add(new EndpointResponseCoverage(
                    effectiveCandidate, matching.size(), untracked, testCountByResponseCode));
        }
        return results;
    }

    private DescribedEndpoint withoutFiveXxResponseCodes(DescribedEndpoint candidate) {
        List<String> filtered = candidate.responseCodes().stream()
                .filter(code -> !isFiveXx(code))
                .toList();
        return new DescribedEndpoint(
                candidate.verb(), candidate.path(), candidate.operationId(), candidate.tags(), filtered);
    }

    private boolean isFiveXx(String responseCode) {
        if (responseCode == null) {
            return false;
        }
        String normalized = responseCode.trim();
        return normalized.matches("5\\d\\d") || normalized.equalsIgnoreCase("5XX");
    }
}

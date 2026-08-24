package com.arc_e_tect.gradle.doppelganger.detect;

import com.arc_e_tect.gradle.detector.core.openapi.DescribedEndpoint;

import java.util.List;
import java.util.Map;

/**
 * The {@code scanContracts} result for a single endpoint: how many response codes its OpenAPI
 * operation declares, how many contract tests exist for it in total, and - only when
 * {@code includeResponseCoverage} is enabled - how many of those tests cover each declared
 * response code.
 *
 * @param endpoint                  the declared-and-implemented endpoint this row describes,
 *                                   carrying its declared response codes via
 *                                   {@link DescribedEndpoint#responseCodes()}
 * @param contractTestCount         the number of distinct contract tests found for this endpoint,
 *                                   across every enabled verification source, regardless of
 *                                   whether their asserted status code could be determined
 * @param untrackedTestCount        the number of those tests whose asserted status code either
 *                                   could not be determined, or was determined but is not among
 *                                   this endpoint's declared response codes - counted in
 *                                   {@link #contractTestCount()} but not in
 *                                   {@link #testCountByResponseCode()}; always {@code 0} when
 *                                   response coverage was not requested
 * @param testCountByResponseCode   for every response code {@link DescribedEndpoint#responseCodes()}
 *                                   declares, the number of tests detected to assert it - including
 *                                   an entry with value {@code 0} for a declared code no test
 *                                   covers; empty when response coverage was not requested
 */
public record EndpointResponseCoverage(
        DescribedEndpoint endpoint,
        int contractTestCount,
        int untrackedTestCount,
        Map<String, Integer> testCountByResponseCode) {

    /**
     * The declared response codes for this endpoint - a convenience view of
     * {@link DescribedEndpoint#responseCodes()}.
     *
     * @return the declared response codes, sorted, possibly empty
     */
    public List<String> declaredResponseCodes() {
        return endpoint.responseCodes();
    }
}

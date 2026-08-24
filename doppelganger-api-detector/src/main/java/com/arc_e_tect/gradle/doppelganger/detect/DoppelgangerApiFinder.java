package com.arc_e_tect.gradle.doppelganger.detect;

import com.arc_e_tect.gradle.detector.core.model.Endpoint;
import com.arc_e_tect.gradle.detector.core.model.PathMatcher;

import java.util.ArrayList;
import java.util.List;

/**
 * Determines which endpoints are both declared in the OpenAPI documentation and implemented by a
 * {@code @RestController} method, but have no matching verification evidence from any configured
 * {@link ContractVerificationSource} - i.e. which endpoints are "doppelganger APIs".
 *
 * <p>This does <strong>not</strong> delegate to {@code api-detector-core}'s
 * {@code ContractSetOperations.difference(...)}, even though both sides are {@code List<Endpoint>}:
 * {@link com.arc_e_tect.gradle.doppelganger.scan.SpringCloudContractScanner} produces genuinely
 * concrete, resolved paths (e.g. {@code "/items/1"}, taken from a contract's own example request),
 * not templates - unlike the paths {@code ControllerScanner}, {@code OpenApiEndpointCollector},
 * {@link com.arc_e_tect.gradle.doppelganger.scan.RestDocsScanner}, and
 * {@link com.arc_e_tect.gradle.doppelganger.scan.OpenApiRequestValidatorScanner} all produce, which
 * are always templates. {@code ContractSetOperations}' shared matching requires both sides to be
 * templates with aligned placeholders, so it would never recognise a Spring Cloud Contract example
 * for {@code "/items/{id}"} as verifying it. This class instead matches candidates against verified
 * entries directly via {@link PathMatcher#matchesConcrete(String, String)}, which accepts a
 * concrete path on one side and a template on the other - the comparison Prompt 3 anticipated
 * wouldn't always "line up cleanly" through the shared generic operation.</p>
 */
public class DoppelgangerApiFinder {

    /** Creates a new {@code DoppelgangerApiFinder}. */
    public DoppelgangerApiFinder() {}

    /**
     * Returns every endpoint in {@code declaredAndImplemented} that has no matching entry in
     * {@code verified}.
     *
     * @param declaredAndImplemented endpoints both declared in the OpenAPI documentation and
     *                                implemented by a {@code @RestController} method
     * @param verified                endpoints with verification evidence from any enabled
     *                                {@link ContractVerificationSource}
     * @return the endpoints with no verification evidence, in the order they were passed in
     */
    public List<Endpoint> findDoppelgangers(List<Endpoint> declaredAndImplemented, List<Endpoint> verified) {
        List<Endpoint> doppelgangers = new ArrayList<>();
        for (Endpoint candidate : declaredAndImplemented) {
            boolean isVerified = verified.stream()
                    .anyMatch(verifiedEntry -> ContractEvidenceMatcher.verifies(verifiedEntry, candidate));
            if (!isVerified) {
                doppelgangers.add(candidate);
            }
        }
        return doppelgangers;
    }
}

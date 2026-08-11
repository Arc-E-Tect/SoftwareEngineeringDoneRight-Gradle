package com.arc_e_tect.gradle.shadow.detect;

import com.arc_e_tect.gradle.detector.core.detect.ContractSetOperations;
import com.arc_e_tect.gradle.detector.core.model.Endpoint;
import com.arc_e_tect.gradle.detector.core.model.HttpVerb;
import com.arc_e_tect.gradle.detector.core.openapi.DescribedEndpoint;

import java.util.List;

/**
 * Determines which controller {@link Endpoint}s are not described by any {@link DescribedEndpoint}
 * collected from the OpenAPI documentation, i.e. which endpoints are "shadow APIs".
 *
 * <p>Delegates to the shared {@link ContractSetOperations#difference(List, List)} in
 * {@code api-detector-core}.</p>
 */
public class ShadowApiFinder {

    /** Creates a new {@code ShadowApiFinder}. */
    public ShadowApiFinder() {}

    /**
     * Returns every endpoint in {@code endpoints} that has no matching entry in {@code described}.
     *
     * <p>An endpoint whose verb is {@link HttpVerb#ANY} (a Spring {@code @RequestMapping} that
     * does not restrict its HTTP method) is considered described as soon as any verb is
     * documented for a matching path.</p>
     *
     * @param endpoints the endpoints found by scanning {@code @RestController} classes
     * @param described the endpoints described by the OpenAPI documentation
     * @return the endpoints that are not described, in the order they were passed in
     */
    public List<Endpoint> findShadows(List<Endpoint> endpoints, List<DescribedEndpoint> described) {
        return ContractSetOperations.difference(endpoints, described);
    }
}

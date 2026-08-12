package com.arc_e_tect.gradle.mirage.detect;

import com.arc_e_tect.gradle.detector.core.detect.ContractSetOperations;
import com.arc_e_tect.gradle.detector.core.model.Endpoint;
import com.arc_e_tect.gradle.detector.core.model.HttpVerb;
import com.arc_e_tect.gradle.detector.core.openapi.DescribedEndpoint;

import java.util.List;

/**
 * Determines which {@link DescribedEndpoint}s collected from the OpenAPI documentation have no
 * matching controller {@link Endpoint}, i.e. which endpoints are "mirage APIs" - endpoints that
 * are declared but never implemented.
 *
 * <p>Delegates to the shared {@link ContractSetOperations#difference(List, List)} in
 * {@code api-detector-core}.</p>
 */
public class MirageApiFinder {

    /** Creates a new {@code MirageApiFinder}. */
    public MirageApiFinder() {}

    /**
     * Returns every endpoint in {@code described} that has no matching entry in {@code endpoints}.
     *
     * <p>A described endpoint is considered implemented as soon as a scanned controller endpoint
     * matches its path and either matches its verb exactly or carries {@link HttpVerb#ANY} - a
     * Spring {@code @RequestMapping} that does not restrict its HTTP method implements every verb
     * documented for a matching path.</p>
     *
     * @param described the endpoints described by the OpenAPI documentation
     * @param endpoints the endpoints found by scanning {@code @RestController} classes
     * @return the described endpoints that are not implemented, in the order they were passed in
     */
    public List<DescribedEndpoint> findMirages(List<DescribedEndpoint> described, List<Endpoint> endpoints) {
        return ContractSetOperations.difference(described, endpoints);
    }
}

package com.arc_e_tect.gradle.shadow.detect;

import com.arc_e_tect.gradle.shadow.model.Endpoint;
import com.arc_e_tect.gradle.shadow.model.HttpVerb;
import com.arc_e_tect.gradle.shadow.model.PathMatcher;
import com.arc_e_tect.gradle.shadow.openapi.DescribedEndpoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Determines which controller {@link Endpoint}s are not described by any {@link DescribedEndpoint}
 * collected from the OpenAPI documentation, i.e. which endpoints are "shadow APIs".
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
        List<Endpoint> shadows = new ArrayList<>();
        for (Endpoint endpoint : endpoints) {
            boolean isDescribed = described.stream().anyMatch(d ->
                    (endpoint.verb() == HttpVerb.ANY || endpoint.verb() == d.verb())
                            && PathMatcher.matches(endpoint.path(), d.path()));
            if (!isDescribed) {
                shadows.add(endpoint);
            }
        }
        return shadows;
    }
}

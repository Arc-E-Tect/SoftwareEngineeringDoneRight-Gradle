package com.arc_e_tect.gradle.mirage.detect;

import com.arc_e_tect.gradle.mirage.model.Endpoint;
import com.arc_e_tect.gradle.mirage.model.HttpVerb;
import com.arc_e_tect.gradle.mirage.model.PathMatcher;
import com.arc_e_tect.gradle.mirage.openapi.DescribedEndpoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Determines which {@link DescribedEndpoint}s collected from the OpenAPI documentation have no
 * matching controller {@link Endpoint}, i.e. which endpoints are "mirage APIs" - endpoints that
 * are declared but never implemented.
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
        List<DescribedEndpoint> mirages = new ArrayList<>();
        for (DescribedEndpoint d : described) {
            boolean isImplemented = endpoints.stream().anyMatch(endpoint ->
                    (endpoint.verb() == HttpVerb.ANY || endpoint.verb() == d.verb())
                            && PathMatcher.matches(d.path(), endpoint.path()));
            if (!isImplemented) {
                mirages.add(d);
            }
        }
        return mirages;
    }
}

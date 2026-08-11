package com.arc_e_tect.gradle.shadow.detect;

import com.arc_e_tect.gradle.detector.core.model.Endpoint;
import com.arc_e_tect.gradle.detector.core.model.HttpVerb;
import com.arc_e_tect.gradle.detector.core.openapi.DescribedEndpoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ShadowApiFinder")
class ShadowApiFinderTest {

    private final ShadowApiFinder finder = new ShadowApiFinder();

    @Test
    @DisplayName("returns no shadows when every endpoint is described")
    void returnsNoShadowsWhenEveryEndpointIsDescribed() {
        List<Endpoint> endpoints = List.of(endpoint(HttpVerb.GET, "/users"));
        List<DescribedEndpoint> described = List.of(described(HttpVerb.GET, "/users"));

        assertThat(finder.findShadows(endpoints, described)).isEmpty();
    }

    @Test
    @DisplayName("flags an endpoint whose path is not described at all")
    void flagsUndescribedPath() {
        List<Endpoint> endpoints = List.of(endpoint(HttpVerb.GET, "/users"));
        List<DescribedEndpoint> described = List.of(described(HttpVerb.GET, "/accounts"));

        assertThat(finder.findShadows(endpoints, described)).extracting(Endpoint::path).containsExactly("/users");
    }

    @Test
    @DisplayName("flags an endpoint whose path matches but verb does not")
    void flagsMismatchedVerb() {
        List<Endpoint> endpoints = List.of(endpoint(HttpVerb.DELETE, "/users/{id}"));
        List<DescribedEndpoint> described = List.of(described(HttpVerb.GET, "/users/{id}"));

        assertThat(finder.findShadows(endpoints, described)).hasSize(1);
    }

    @Test
    @DisplayName("matches path variables regardless of their name")
    void matchesPathVariablesRegardlessOfName() {
        List<Endpoint> endpoints = List.of(endpoint(HttpVerb.GET, "/users/{id}"));
        List<DescribedEndpoint> described = List.of(described(HttpVerb.GET, "/users/{userId}"));

        assertThat(finder.findShadows(endpoints, described)).isEmpty();
    }

    @Test
    @DisplayName("an ANY-verb endpoint is described as soon as any verb is documented for the path")
    void anyVerbEndpointDescribedByAnyDocumentedVerb() {
        List<Endpoint> endpoints = List.of(endpoint(HttpVerb.ANY, "/users/{id}/summary"));
        List<DescribedEndpoint> described = List.of(described(HttpVerb.POST, "/users/{id}/summary"));

        assertThat(finder.findShadows(endpoints, described)).isEmpty();
    }

    @Test
    @DisplayName("an ANY-verb endpoint is a shadow when no verb is documented for the path")
    void anyVerbEndpointIsShadowWhenPathUndescribed() {
        List<Endpoint> endpoints = List.of(endpoint(HttpVerb.ANY, "/users/{id}/summary"));
        List<DescribedEndpoint> described = List.of(described(HttpVerb.GET, "/other"));

        assertThat(finder.findShadows(endpoints, described)).hasSize(1);
    }

    private static Endpoint endpoint(HttpVerb verb, String path) {
        return new Endpoint(verb, path, "com.example.FooController", "foo()", "FooController.java", 1);
    }

    private static DescribedEndpoint described(HttpVerb verb, String path) {
        return new DescribedEndpoint(verb, path, "op", List.of());
    }
}

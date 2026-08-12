package com.arc_e_tect.gradle.mirage.detect;

import com.arc_e_tect.gradle.detector.core.model.Endpoint;
import com.arc_e_tect.gradle.detector.core.model.HttpVerb;
import com.arc_e_tect.gradle.detector.core.openapi.DescribedEndpoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MirageApiFinder")
class MirageApiFinderTest {

    private final MirageApiFinder finder = new MirageApiFinder();

    @Test
    @DisplayName("returns no mirages when every described endpoint is implemented")
    void returnsNoMiragesWhenEveryEndpointIsImplemented() {
        List<DescribedEndpoint> described = List.of(described(HttpVerb.GET, "/users"));
        List<Endpoint> endpoints = List.of(endpoint(HttpVerb.GET, "/users"));

        assertThat(finder.findMirages(described, endpoints)).isEmpty();
    }

    @Test
    @DisplayName("flags a described endpoint whose path is not implemented at all")
    void flagsUnimplementedPath() {
        List<DescribedEndpoint> described = List.of(described(HttpVerb.GET, "/users"));
        List<Endpoint> endpoints = List.of(endpoint(HttpVerb.GET, "/accounts"));

        assertThat(finder.findMirages(described, endpoints))
                .extracting(DescribedEndpoint::path).containsExactly("/users");
    }

    @Test
    @DisplayName("flags a described endpoint whose path matches but verb does not")
    void flagsMismatchedVerb() {
        List<DescribedEndpoint> described = List.of(described(HttpVerb.DELETE, "/users/{id}"));
        List<Endpoint> endpoints = List.of(endpoint(HttpVerb.GET, "/users/{id}"));

        assertThat(finder.findMirages(described, endpoints)).hasSize(1);
    }

    @Test
    @DisplayName("matches path variables regardless of their name")
    void matchesPathVariablesRegardlessOfName() {
        List<DescribedEndpoint> described = List.of(described(HttpVerb.GET, "/users/{userId}"));
        List<Endpoint> endpoints = List.of(endpoint(HttpVerb.GET, "/users/{id}"));

        assertThat(finder.findMirages(described, endpoints)).isEmpty();
    }

    @Test
    @DisplayName("a described endpoint is implemented by a matching ANY-verb controller endpoint")
    void describedEndpointImplementedByAnyVerbEndpoint() {
        List<DescribedEndpoint> described = List.of(described(HttpVerb.POST, "/users/{id}/summary"));
        List<Endpoint> endpoints = List.of(endpoint(HttpVerb.ANY, "/users/{id}/summary"));

        assertThat(finder.findMirages(described, endpoints)).isEmpty();
    }

    @Test
    @DisplayName("a described endpoint is a mirage when no controller endpoint matches its path")
    void describedEndpointIsMirageWhenPathUnimplemented() {
        List<DescribedEndpoint> described = List.of(described(HttpVerb.GET, "/users/{id}/summary"));
        List<Endpoint> endpoints = List.of(endpoint(HttpVerb.ANY, "/other"));

        assertThat(finder.findMirages(described, endpoints)).hasSize(1);
    }

    private static DescribedEndpoint described(HttpVerb verb, String path) {
        return new DescribedEndpoint(verb, path, "op", List.of());
    }

    private static Endpoint endpoint(HttpVerb verb, String path) {
        return new Endpoint(verb, path, "com.example.FooController", "foo()", "FooController.java", 1);
    }
}

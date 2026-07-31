package com.arc_e_tect.gradle.zombie.detect;

import com.arc_e_tect.gradle.zombie.model.Endpoint;
import com.arc_e_tect.gradle.zombie.model.HttpVerb;
import com.arc_e_tect.gradle.zombie.openapi.DescribedEndpoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ZombieApiFinder")
class ZombieApiFinderTest {

    private final ZombieApiFinder finder = new ZombieApiFinder();

    @Test
    @DisplayName("returns no zombies when every endpoint is described")
    void returnsNoZombiesWhenEveryEndpointIsDescribed() {
        List<Endpoint> endpoints = List.of(endpoint(HttpVerb.GET, "/users"));
        List<DescribedEndpoint> described = List.of(new DescribedEndpoint(HttpVerb.GET, "/users"));

        assertThat(finder.findZombies(endpoints, described)).isEmpty();
    }

    @Test
    @DisplayName("flags an endpoint whose path is not described at all")
    void flagsUndescribedPath() {
        List<Endpoint> endpoints = List.of(endpoint(HttpVerb.GET, "/users"));
        List<DescribedEndpoint> described = List.of(new DescribedEndpoint(HttpVerb.GET, "/accounts"));

        assertThat(finder.findZombies(endpoints, described)).extracting(Endpoint::path).containsExactly("/users");
    }

    @Test
    @DisplayName("flags an endpoint whose path matches but verb does not")
    void flagsMismatchedVerb() {
        List<Endpoint> endpoints = List.of(endpoint(HttpVerb.DELETE, "/users/{id}"));
        List<DescribedEndpoint> described = List.of(new DescribedEndpoint(HttpVerb.GET, "/users/{id}"));

        assertThat(finder.findZombies(endpoints, described)).hasSize(1);
    }

    @Test
    @DisplayName("matches path variables regardless of their name")
    void matchesPathVariablesRegardlessOfName() {
        List<Endpoint> endpoints = List.of(endpoint(HttpVerb.GET, "/users/{id}"));
        List<DescribedEndpoint> described = List.of(new DescribedEndpoint(HttpVerb.GET, "/users/{userId}"));

        assertThat(finder.findZombies(endpoints, described)).isEmpty();
    }

    @Test
    @DisplayName("an ANY-verb endpoint is described as soon as any verb is documented for the path")
    void anyVerbEndpointDescribedByAnyDocumentedVerb() {
        List<Endpoint> endpoints = List.of(endpoint(HttpVerb.ANY, "/users/{id}/summary"));
        List<DescribedEndpoint> described = List.of(new DescribedEndpoint(HttpVerb.POST, "/users/{id}/summary"));

        assertThat(finder.findZombies(endpoints, described)).isEmpty();
    }

    @Test
    @DisplayName("an ANY-verb endpoint is a zombie when no verb is documented for the path")
    void anyVerbEndpointIsZombieWhenPathUndescribed() {
        List<Endpoint> endpoints = List.of(endpoint(HttpVerb.ANY, "/users/{id}/summary"));
        List<DescribedEndpoint> described = List.of(new DescribedEndpoint(HttpVerb.GET, "/other"));

        assertThat(finder.findZombies(endpoints, described)).hasSize(1);
    }

    private static Endpoint endpoint(HttpVerb verb, String path) {
        return new Endpoint(verb, path, "com.example.FooController", "foo()", "FooController.java", 1);
    }
}

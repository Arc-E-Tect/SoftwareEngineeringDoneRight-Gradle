package com.arc_e_tect.gradle.doppelganger.detect;

import com.arc_e_tect.gradle.detector.core.model.Endpoint;
import com.arc_e_tect.gradle.detector.core.model.HttpVerb;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DoppelgangerApiFinder")
class DoppelgangerApiFinderTest {

    private final DoppelgangerApiFinder finder = new DoppelgangerApiFinder();

    @Test
    @DisplayName("returns no doppelgangers when every candidate endpoint is verified")
    void returnsNoDoppelgangersWhenEveryEndpointIsVerified() {
        List<Endpoint> declaredAndImplemented = List.of(endpoint(HttpVerb.GET, "/orders/{id}"));
        List<Endpoint> verified = List.of(endpoint(HttpVerb.GET, "/orders/{id}"));

        assertThat(finder.findDoppelgangers(declaredAndImplemented, verified)).isEmpty();
    }

    @Test
    @DisplayName("flags a candidate endpoint whose path has no verification evidence at all")
    void flagsUnverifiedPath() {
        List<Endpoint> declaredAndImplemented = List.of(endpoint(HttpVerb.GET, "/orders/{id}"));
        List<Endpoint> verified = List.of(endpoint(HttpVerb.GET, "/accounts/{id}"));

        assertThat(finder.findDoppelgangers(declaredAndImplemented, verified))
                .extracting(Endpoint::path).containsExactly("/orders/{id}");
    }

    @Test
    @DisplayName("flags a candidate endpoint whose path matches but verb does not")
    void flagsMismatchedVerb() {
        List<Endpoint> declaredAndImplemented = List.of(endpoint(HttpVerb.DELETE, "/orders/{id}"));
        List<Endpoint> verified = List.of(endpoint(HttpVerb.GET, "/orders/{id}"));

        assertThat(finder.findDoppelgangers(declaredAndImplemented, verified)).hasSize(1);
    }

    @Test
    @DisplayName("matches path variables regardless of their name")
    void matchesPathVariablesRegardlessOfName() {
        List<Endpoint> declaredAndImplemented = List.of(endpoint(HttpVerb.GET, "/orders/{id}"));
        List<Endpoint> verified = List.of(endpoint(HttpVerb.GET, "/orders/{orderId}"));

        assertThat(finder.findDoppelgangers(declaredAndImplemented, verified)).isEmpty();
    }

    @Test
    @DisplayName("an ANY-verb candidate endpoint is verified as soon as any verb has evidence for the path")
    void anyVerbCandidateVerifiedByAnyVerifiedVerb() {
        List<Endpoint> declaredAndImplemented = List.of(endpoint(HttpVerb.ANY, "/orders/{id}/summary"));
        List<Endpoint> verified = List.of(endpoint(HttpVerb.POST, "/orders/{id}/summary"));

        assertThat(finder.findDoppelgangers(declaredAndImplemented, verified)).isEmpty();
    }

    @Test
    @DisplayName("a concrete verified path (e.g. from a Spring Cloud Contract example) verifies a templated candidate")
    void concreteVerifiedPathVerifiesTemplatedCandidate() {
        List<Endpoint> declaredAndImplemented = List.of(endpoint(HttpVerb.GET, "/orders/{id}"));
        List<Endpoint> verified = List.of(endpoint(HttpVerb.GET, "/orders/1"));

        assertThat(finder.findDoppelgangers(declaredAndImplemented, verified)).isEmpty();
    }

    @Test
    @DisplayName("a concrete verified path does not verify a candidate with a different literal segment")
    void concreteVerifiedPathDoesNotVerifyDifferentLiteralCandidate() {
        List<Endpoint> declaredAndImplemented = List.of(endpoint(HttpVerb.GET, "/orders/summary"));
        List<Endpoint> verified = List.of(endpoint(HttpVerb.GET, "/orders/count"));

        assertThat(finder.findDoppelgangers(declaredAndImplemented, verified)).hasSize(1);
    }

    private static Endpoint endpoint(HttpVerb verb, String path) {
        return new Endpoint(verb, path, "com.example.OrderController", "foo()", "OrderController.java", 1);
    }
}

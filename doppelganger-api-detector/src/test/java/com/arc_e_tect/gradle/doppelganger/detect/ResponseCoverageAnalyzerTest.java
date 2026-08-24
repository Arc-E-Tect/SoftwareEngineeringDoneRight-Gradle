package com.arc_e_tect.gradle.doppelganger.detect;

import com.arc_e_tect.gradle.detector.core.model.Endpoint;
import com.arc_e_tect.gradle.detector.core.model.HttpVerb;
import com.arc_e_tect.gradle.detector.core.openapi.DescribedEndpoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ResponseCoverageAnalyzer")
class ResponseCoverageAnalyzerTest {

    private final ResponseCoverageAnalyzer analyzer = new ResponseCoverageAnalyzer();

    @Test
    @DisplayName("the /v1/foobars worked example: 200 covered by 2 tests, 404 covered by 1")
    void worksThroughTheFoobarsExample() {
        DescribedEndpoint foobars = new DescribedEndpoint(
                HttpVerb.GET, "/v1/foobars", "listFoobars", List.of(), List.of("200", "404"));
        List<VerifiedContractTest> tests = List.of(
                verified(HttpVerb.GET, "/v1/foobars", "200"),
                verified(HttpVerb.GET, "/v1/foobars", "200"),
                verified(HttpVerb.GET, "/v1/foobars", "404"));

        List<EndpointResponseCoverage> results = analyzer.analyze(List.of(foobars), tests, true);

        assertThat(results).hasSize(1);
        EndpointResponseCoverage row = results.get(0);
        assertThat(row.contractTestCount()).isEqualTo(3);
        assertThat(row.untrackedTestCount()).isZero();
        assertThat(row.testCountByResponseCode()).containsExactly(
                Map.entry("200", 2), Map.entry("404", 1));
    }

    @Test
    @DisplayName("a declared response code with no covering test is reported with count 0")
    void reportsZeroForUncoveredDeclaredCode() {
        DescribedEndpoint endpoint = new DescribedEndpoint(
                HttpVerb.GET, "/orders", null, List.of(), List.of("200", "500"));
        List<VerifiedContractTest> tests = List.of(verified(HttpVerb.GET, "/orders", "200"));

        List<EndpointResponseCoverage> results = analyzer.analyze(List.of(endpoint), tests, true);

        assertThat(results.get(0).testCountByResponseCode()).containsExactly(
                Map.entry("200", 1), Map.entry("500", 0));
    }

    @Test
    @DisplayName("when includeResponseCoverage is false, the breakdown is empty and never computed")
    void skipsBreakdownWhenResponseCoverageNotRequested() {
        DescribedEndpoint endpoint = new DescribedEndpoint(
                HttpVerb.GET, "/orders", null, List.of(), List.of("200", "404"));
        List<VerifiedContractTest> tests = List.of(
                verified(HttpVerb.GET, "/orders", "200"),
                verified(HttpVerb.GET, "/orders", "404"));

        List<EndpointResponseCoverage> results = analyzer.analyze(List.of(endpoint), tests, false);

        EndpointResponseCoverage row = results.get(0);
        assertThat(row.contractTestCount()).isEqualTo(2);
        assertThat(row.untrackedTestCount()).isZero();
        assertThat(row.testCountByResponseCode()).isEmpty();
    }

    @Test
    @DisplayName("a test whose status code cannot be determined counts towards the total but not any response code")
    void countsUndetectedStatusAsUntracked() {
        DescribedEndpoint endpoint = new DescribedEndpoint(
                HttpVerb.GET, "/orders", null, List.of(), List.of("200"));
        List<VerifiedContractTest> tests = List.of(
                verified(HttpVerb.GET, "/orders", "200"),
                verified(HttpVerb.GET, "/orders", null));

        List<EndpointResponseCoverage> results = analyzer.analyze(List.of(endpoint), tests, true);

        EndpointResponseCoverage row = results.get(0);
        assertThat(row.contractTestCount()).isEqualTo(2);
        assertThat(row.untrackedTestCount()).isEqualTo(1);
        assertThat(row.testCountByResponseCode()).containsExactly(Map.entry("200", 1));
    }

    @Test
    @DisplayName("a detected status code not among the declared response codes counts as untracked")
    void countsUndeclaredDetectedStatusAsUntracked() {
        DescribedEndpoint endpoint = new DescribedEndpoint(
                HttpVerb.GET, "/orders", null, List.of(), List.of("200"));
        List<VerifiedContractTest> tests = List.of(verified(HttpVerb.GET, "/orders", "500"));

        List<EndpointResponseCoverage> results = analyzer.analyze(List.of(endpoint), tests, true);

        EndpointResponseCoverage row = results.get(0);
        assertThat(row.contractTestCount()).isEqualTo(1);
        assertThat(row.untrackedTestCount()).isEqualTo(1);
        assertThat(row.testCountByResponseCode()).containsExactly(Map.entry("200", 0));
    }

    @Test
    @DisplayName("a verified test for a different endpoint does not contribute to this endpoint's counts")
    void ignoresUnrelatedVerifiedTests() {
        DescribedEndpoint endpoint = new DescribedEndpoint(
                HttpVerb.GET, "/orders", null, List.of(), List.of("200"));
        List<VerifiedContractTest> tests = List.of(verified(HttpVerb.POST, "/orders", "201"));

        List<EndpointResponseCoverage> results = analyzer.analyze(List.of(endpoint), tests, true);

        assertThat(results.get(0).contractTestCount()).isZero();
    }

    @Test
    @DisplayName("computes independent rows for multiple candidate endpoints")
    void computesIndependentRowsForMultipleEndpoints() {
        DescribedEndpoint getOrders = new DescribedEndpoint(
                HttpVerb.GET, "/orders", null, List.of(), List.of("200"));
        DescribedEndpoint postOrders = new DescribedEndpoint(
                HttpVerb.POST, "/orders", null, List.of(), List.of("201"));
        List<VerifiedContractTest> tests = List.of(
                verified(HttpVerb.GET, "/orders", "200"),
                verified(HttpVerb.POST, "/orders", "201"),
                verified(HttpVerb.POST, "/orders", "201"));

        List<EndpointResponseCoverage> results =
                analyzer.analyze(List.of(getOrders, postOrders), tests, true);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).contractTestCount()).isEqualTo(1);
        assertThat(results.get(1).contractTestCount()).isEqualTo(2);
    }

    private VerifiedContractTest verified(HttpVerb verb, String path, String statusCode) {
        return new VerifiedContractTest(new Endpoint(verb, path, "TestClass", "test()", "Test.java", 1), statusCode);
    }
}

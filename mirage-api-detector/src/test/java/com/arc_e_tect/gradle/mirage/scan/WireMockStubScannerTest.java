package com.arc_e_tect.gradle.mirage.scan;

import com.arc_e_tect.gradle.detector.core.model.Endpoint;
import com.arc_e_tect.gradle.detector.core.model.HttpVerb;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@DisplayName("WireMockStubScanner")
class WireMockStubScannerTest {

    private final WireMockStubScanner scanner = new WireMockStubScanner();

    @Test
    @DisplayName("reads method and urlPath from a stub mapping")
    void readsMethodAndUrlPathFromStub() throws Exception {
        List<Endpoint> endpoints = scanner.scan(fixtureDir());

        assertThat(endpoints)
                .filteredOn(e -> e.methodSignature().equals("shouldReturnOrder"))
                .extracting(Endpoint::verb, Endpoint::path)
                .containsExactly(tuple(HttpVerb.GET, "/orders/1"));
    }

    @Test
    @DisplayName("reads method and url from a stub mapping")
    void readsMethodAndUrlFromStub() throws Exception {
        List<Endpoint> endpoints = scanner.scan(fixtureDir());

        assertThat(endpoints)
                .filteredOn(e -> e.methodSignature().equals("shouldCreateOrder"))
                .extracting(Endpoint::verb, Endpoint::path)
                .containsExactly(tuple(HttpVerb.POST, "/orders"));
    }

    @Test
    @DisplayName("recognises the urlPattern field name, rewriting a regex id segment into a {id} placeholder")
    void recognisesUrlPatternFieldName() throws Exception {
        List<Endpoint> endpoints = scanner.scan(fixtureDir());

        assertThat(endpoints)
                .filteredOn(e -> e.methodSignature().equals("usesUrlPattern"))
                .extracting(Endpoint::verb, Endpoint::path)
                .containsExactly(tuple(HttpVerb.PUT, "/orders/{id}"));
    }

    @Test
    @DisplayName("rewrites every non-literal segment of a urlPathPattern into its own {id} placeholder")
    void rewritesEveryNonLiteralSegmentOfUrlPathPattern() throws Exception {
        List<Endpoint> endpoints = scanner.scan(fixtureDir());

        assertThat(endpoints)
                .filteredOn(e -> e.methodSignature().equals("usesUrlPathPatternMultiSegment"))
                .extracting(Endpoint::verb, Endpoint::path)
                .containsExactly(tuple(HttpVerb.PATCH, "/orders/{id}/items/{id}"));
    }

    @Test
    @DisplayName("leaves a urlPathPattern with no regex metacharacters unchanged")
    void leavesLiteralUrlPathPatternUnchanged() throws Exception {
        List<Endpoint> endpoints = scanner.scan(fixtureDir());

        assertThat(endpoints)
                .filteredOn(e -> e.methodSignature().equals("usesLiteralUrlPathPattern"))
                .extracting(Endpoint::verb, Endpoint::path)
                .containsExactly(tuple(HttpVerb.GET, "/orders/summary"));
    }

    @Test
    @DisplayName("skips a stub file missing a url/urlPath entry")
    void skipsStubMissingUrl() throws Exception {
        List<Endpoint> endpoints = scanner.scan(fixtureDir());

        assertThat(endpoints).noneMatch(e -> e.methodSignature().equals("incomplete"));
    }

    @Test
    @DisplayName("groups a stub by its subdirectory relative to the mappings root")
    void groupsStubBySubdirectory() throws Exception {
        List<Endpoint> endpoints = scanner.scan(fixtureDir());

        assertThat(endpoints)
                .filteredOn(e -> e.methodSignature().equals("shouldReturnOrder"))
                .extracting(Endpoint::declaringClass)
                .containsExactly("orders");
    }

    @Test
    @DisplayName("groups a stub directly under the mappings root as (mappings)")
    void groupsRootLevelStubAsMappings() throws Exception {
        List<Endpoint> endpoints = scanner.scan(fixtureDir());

        assertThat(endpoints)
                .filteredOn(e -> e.methodSignature().equals("shouldDeleteOrder"))
                .extracting(Endpoint::declaringClass)
                .containsExactly("(mappings)");
    }

    @Test
    @DisplayName("returns an empty list when the directory does not exist")
    void returnsEmptyListForMissingDirectory(@TempDir Path tempDir) throws Exception {
        File missing = new File(tempDir.toFile(), "does-not-exist");

        assertThat(scanner.scan(missing)).isEmpty();
    }

    private static File fixtureDir() {
        URL url = WireMockStubScannerTest.class.getClassLoader().getResource("fixtures/mappings");
        if (url == null) {
            throw new IllegalStateException("Fixture directory not found on classpath: fixtures/mappings");
        }
        return new File(url.getFile());
    }
}

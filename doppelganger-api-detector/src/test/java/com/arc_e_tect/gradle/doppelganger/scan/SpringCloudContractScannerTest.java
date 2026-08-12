package com.arc_e_tect.gradle.doppelganger.scan;

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

@DisplayName("SpringCloudContractScanner")
class SpringCloudContractScannerTest {

    private final SpringCloudContractScanner scanner = new SpringCloudContractScanner();

    @Test
    @DisplayName("reads method and url from a Groovy contract")
    void readsMethodAndUrlFromGroovyContract() throws Exception {
        List<Endpoint> endpoints = scanner.scan(fixtureDir());

        assertThat(endpoints)
                .filteredOn(e -> e.methodSignature().equals("shouldReturnOrder"))
                .extracting(Endpoint::verb, Endpoint::path)
                .containsExactly(tuple(HttpVerb.GET, "/orders/1"));
    }

    @Test
    @DisplayName("reads method(...) and urlPath(...) written in call-argument style")
    void readsMethodAndUrlPathInCallArgumentStyle() throws Exception {
        List<Endpoint> endpoints = scanner.scan(fixtureDir());

        assertThat(endpoints)
                .filteredOn(e -> e.methodSignature().equals("shouldCreateOrder"))
                .extracting(Endpoint::verb, Endpoint::path)
                .containsExactly(tuple(HttpVerb.POST, "/orders"));
    }

    @Test
    @DisplayName("reads method and urlPath from a YAML contract")
    void readsMethodAndUrlPathFromYamlContract() throws Exception {
        List<Endpoint> endpoints = scanner.scan(fixtureDir());

        assertThat(endpoints)
                .filteredOn(e -> e.methodSignature().equals("shouldDeleteOrder"))
                .extracting(Endpoint::verb, Endpoint::path)
                .containsExactly(tuple(HttpVerb.DELETE, "/orders/1"));
    }

    @Test
    @DisplayName("skips a contract file missing a url/urlPath entry")
    void skipsContractMissingUrl() throws Exception {
        List<Endpoint> endpoints = scanner.scan(fixtureDir());

        assertThat(endpoints).noneMatch(e -> e.methodSignature().equals("incomplete"));
    }

    @Test
    @DisplayName("groups a contract by its subdirectory relative to the contracts root")
    void groupsContractBySubdirectory() throws Exception {
        List<Endpoint> endpoints = scanner.scan(fixtureDir());

        assertThat(endpoints)
                .filteredOn(e -> e.methodSignature().equals("shouldReturnOrder"))
                .extracting(Endpoint::declaringClass)
                .containsExactly("orders");
    }

    @Test
    @DisplayName("groups a contract directly under the contracts root as (contracts)")
    void groupsRootLevelContractAsContracts() throws Exception {
        List<Endpoint> endpoints = scanner.scan(fixtureDir());

        assertThat(endpoints)
                .filteredOn(e -> e.methodSignature().equals("shouldDeleteOrder"))
                .extracting(Endpoint::declaringClass)
                .containsExactly("(contracts)");
    }

    @Test
    @DisplayName("returns an empty list when the directory does not exist")
    void returnsEmptyListForMissingDirectory(@TempDir Path tempDir) throws Exception {
        File missing = new File(tempDir.toFile(), "does-not-exist");

        assertThat(scanner.scan(missing)).isEmpty();
    }

    private static File fixtureDir() {
        URL url = SpringCloudContractScannerTest.class.getClassLoader().getResource("fixtures/contracts");
        if (url == null) {
            throw new IllegalStateException("Fixture directory not found on classpath: fixtures/contracts");
        }
        return new File(url.getFile());
    }
}

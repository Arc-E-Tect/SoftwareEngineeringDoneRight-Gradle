package com.arc_e_tect.gradle.mirage.scan;

import com.arc_e_tect.gradle.detector.core.model.Endpoint;
import com.arc_e_tect.gradle.detector.core.model.HttpVerb;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@DisplayName("WireMockJavaDslScanner")
class WireMockJavaDslScannerTest {

    private final WireMockJavaDslScanner scanner = new WireMockJavaDslScanner();

    @Test
    @DisplayName("recognises a stubFor(get(urlEqualTo(...))) call")
    void recognisesGetWithUrlEqualTo() throws Exception {
        List<Endpoint> endpoints = scanner.scan(fixtureDir());

        assertThat(endpoints)
                .filteredOn(e -> e.methodSignature().equals("getOrder()"))
                .extracting(Endpoint::verb, Endpoint::path)
                .containsExactly(tuple(HttpVerb.GET, "/orders/1"));
    }

    @Test
    @DisplayName("recognises a stubFor(post(urlPathEqualTo(...))) call")
    void recognisesPostWithUrlPathEqualTo() throws Exception {
        List<Endpoint> endpoints = scanner.scan(fixtureDir());

        assertThat(endpoints)
                .filteredOn(e -> e.methodSignature().equals("createOrder()"))
                .extracting(Endpoint::verb, Endpoint::path)
                .containsExactly(tuple(HttpVerb.POST, "/orders"));
    }

    @Test
    @DisplayName("recognises the urlPathMatching matcher")
    void recognisesUrlPathMatching() throws Exception {
        List<Endpoint> endpoints = scanner.scan(fixtureDir());

        assertThat(endpoints)
                .filteredOn(e -> e.methodSignature().equals("usesUrlPathMatching()"))
                .extracting(Endpoint::verb, Endpoint::path)
                .containsExactly(tuple(HttpVerb.PUT, "/orders/[0-9]+"));
    }

    @Test
    @DisplayName("recognises the urlPathTemplate matcher")
    void recognisesUrlPathTemplate() throws Exception {
        List<Endpoint> endpoints = scanner.scan(fixtureDir());

        assertThat(endpoints)
                .filteredOn(e -> e.methodSignature().equals("usesUrlPathTemplate()"))
                .extracting(Endpoint::verb, Endpoint::path)
                .containsExactly(tuple(HttpVerb.DELETE, "/orders/{id}"));
    }

    @Test
    @DisplayName("walks through builder calls chained between the verb call and willReturn(...)")
    void walksThroughChainedBuilderCalls() throws Exception {
        List<Endpoint> endpoints = scanner.scan(fixtureDir());

        assertThat(endpoints)
                .filteredOn(e -> e.methodSignature().equals("chainsMultipleBuilderCallsBeforeWillReturn()"))
                .extracting(Endpoint::verb, Endpoint::path)
                .containsExactly(tuple(HttpVerb.GET, "/orders/priority"));
    }

    @Test
    @DisplayName("recognises the head, options, and trace verb builders")
    void recognisesHeadOptionsAndTraceVerbs() throws Exception {
        List<Endpoint> endpoints = scanner.scan(fixtureDir());

        assertThat(endpoints)
                .filteredOn(e -> e.methodSignature().equals("usesHeadVerb()"))
                .extracting(Endpoint::verb)
                .containsExactly(HttpVerb.HEAD);
        assertThat(endpoints)
                .filteredOn(e -> e.methodSignature().equals("usesOptionsVerb()"))
                .extracting(Endpoint::verb)
                .containsExactly(HttpVerb.OPTIONS);
        assertThat(endpoints)
                .filteredOn(e -> e.methodSignature().equals("usesTraceVerb()"))
                .extracting(Endpoint::verb)
                .containsExactly(HttpVerb.TRACE);
    }

    @Test
    @DisplayName("recognises an instance-scoped wireMockServer.stubFor(...) call")
    void recognisesInstanceScopedStubFor() throws Exception {
        List<Endpoint> endpoints = scanner.scan(fixtureDir());

        assertThat(endpoints)
                .filteredOn(e -> e.methodSignature().equals("usesInstanceScopedStubFor()"))
                .extracting(Endpoint::verb, Endpoint::path)
                .containsExactly(tuple(HttpVerb.GET, "/orders/instance"));
    }

    @Test
    @DisplayName("skips a stub whose URL matcher is anyUrl()")
    void skipsAnyUrlMatcher() throws Exception {
        List<Endpoint> endpoints = scanner.scan(fixtureDir());

        assertThat(endpoints).noneMatch(e -> e.methodSignature().equals("skipsAnyUrlMatcher()"));
    }

    @Test
    @DisplayName("skips a stub whose URL matcher argument is not a string literal")
    void skipsDynamicPath() throws Exception {
        List<Endpoint> endpoints = scanner.scan(fixtureDir());

        assertThat(endpoints).noneMatch(e -> e.methodSignature().equals("skipsDynamicPath()"));
    }

    @Test
    @DisplayName("skips a stubFor(...) call whose builder was assembled in a local variable")
    void skipsBuilderAssembledInVariable() throws Exception {
        List<Endpoint> endpoints = scanner.scan(fixtureDir());

        assertThat(endpoints).noneMatch(e -> e.methodSignature().equals("skipsBuilderAssembledInVariable()"));
    }

    @Test
    @DisplayName("skips a stubFor() call with no arguments")
    void skipsStubForWithNoArguments() throws Exception {
        List<Endpoint> endpoints = scanner.scan(fixtureDir());

        assertThat(endpoints).noneMatch(e -> e.methodSignature().equals("skipsUnrelatedStubForCall()"));
    }

    @Test
    @DisplayName("sets the declaring class, source file, and a positive line number")
    void setsDeclaringClassSourceFileAndLineNumber() throws Exception {
        List<Endpoint> endpoints = scanner.scan(fixtureDir());

        assertThat(endpoints)
                .filteredOn(e -> e.methodSignature().equals("getOrder()"))
                .allSatisfy(e -> {
                    assertThat(e.declaringClass()).isEqualTo("com.example.fixture.WireMockJavaDslScannerFixture");
                    assertThat(e.sourceFile()).isEqualTo("WireMockJavaDslScannerFixture.java");
                    assertThat(e.lineNumber()).isPositive();
                });
    }

    @Test
    @DisplayName("returns an empty list when the directory does not exist")
    void returnsEmptyListForMissingDirectory(@TempDir Path tempDir) throws Exception {
        File missing = new File(tempDir.toFile(), "does-not-exist");

        assertThat(scanner.scan(missing)).isEmpty();
    }

    @Test
    @DisplayName("returns an empty list when a source file is not valid Java")
    void returnsEmptyListForUnparsableFile(@TempDir Path tempDir) throws Exception {
        File garbage = new File(tempDir.toFile(), "NotJava.java");
        Files.writeString(garbage.toPath(), "this is not java { {{ }}}}}");

        assertThat(scanner.scan(tempDir.toFile())).isEmpty();
    }

    private static File fixtureDir() {
        URL url = WireMockJavaDslScannerTest.class.getClassLoader().getResource("fixtures/wiremock-dsl");
        if (url == null) {
            throw new IllegalStateException("Fixture directory not found on classpath: fixtures/wiremock-dsl");
        }
        return new File(url.getFile());
    }
}

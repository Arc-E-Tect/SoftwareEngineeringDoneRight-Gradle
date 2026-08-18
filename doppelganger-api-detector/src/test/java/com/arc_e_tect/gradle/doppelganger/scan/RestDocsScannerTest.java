package com.arc_e_tect.gradle.doppelganger.scan;

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

@DisplayName("RestDocsScanner")
class RestDocsScannerTest {

    private final RestDocsScanner scanner = new RestDocsScanner();

    @Test
    @DisplayName("recognises a documented mockMvc.perform(get(...)) call")
    void recognisesDocumentedGet() throws Exception {
        List<Endpoint> endpoints = scanner.scan(fixtureDir());

        assertThat(endpoints)
                .filteredOn(e -> e.methodSignature().startsWith("getOrder"))
                .extracting(Endpoint::verb, Endpoint::path)
                .containsExactly(tuple(HttpVerb.GET, "/orders/{id}"));
    }

    @Test
    @DisplayName("recognises a documented mockMvc.perform(post(...)) call")
    void recognisesDocumentedPost() throws Exception {
        List<Endpoint> endpoints = scanner.scan(fixtureDir());

        assertThat(endpoints)
                .filteredOn(e -> e.methodSignature().startsWith("createOrder"))
                .extracting(Endpoint::verb, Endpoint::path)
                .containsExactly(tuple(HttpVerb.POST, "/orders"));
    }

    @Test
    @DisplayName("ignores a perform(...) call with no andDo(document(...)) in the same method")
    void ignoresUndocumentedPerform() throws Exception {
        List<Endpoint> endpoints = scanner.scan(fixtureDir());

        assertThat(endpoints).noneMatch(e -> e.methodSignature().startsWith("listOrdersUndocumented"));
    }

    @Test
    @DisplayName("ignores a documented method whose path argument is not a string literal")
    void ignoresDynamicPath() throws Exception {
        List<Endpoint> endpoints = scanner.scan(fixtureDir());

        assertThat(endpoints).noneMatch(e -> e.methodSignature().startsWith("deleteOrderDynamicPath"));
    }

    @Test
    @DisplayName("ignores a get(...) call that is not an argument to perform(...)")
    void ignoresGetCallOutsidePerform() throws Exception {
        List<Endpoint> endpoints = scanner.scan(fixtureDir());

        assertThat(endpoints).noneMatch(e -> e.methodSignature().startsWith("getCallOutsidePerform"));
    }

    @Test
    @DisplayName("sets the declaring class, source file, and a positive line number")
    void setsDeclaringClassSourceFileAndLineNumber() throws Exception {
        List<Endpoint> endpoints = scanner.scan(fixtureDir());

        assertThat(endpoints)
                .filteredOn(e -> e.methodSignature().startsWith("getOrder"))
                .allSatisfy(e -> {
                    assertThat(e.declaringClass()).isEqualTo("com.example.fixture.RestDocsScannerFixture");
                    assertThat(e.sourceFile()).isEqualTo("RestDocsScannerFixture.java");
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

    @Test
    @DisplayName("recognises a documented REST Assured when().get(...) call")
    void recognisesDocumentedRestAssuredGet() throws Exception {
        List<Endpoint> endpoints = scanner.scan(fixtureDir());

        assertThat(endpoints)
                .filteredOn(e -> e.methodSignature().startsWith("getItemRestAssured"))
                .extracting(Endpoint::verb, Endpoint::path)
                .containsExactly(tuple(HttpVerb.GET, "/items/{id}"));
    }

    @Test
    @DisplayName("recognises a documented REST Assured when().post(...) call")
    void recognisesDocumentedRestAssuredPost() throws Exception {
        List<Endpoint> endpoints = scanner.scan(fixtureDir());

        assertThat(endpoints)
                .filteredOn(e -> e.methodSignature().startsWith("createItemRestAssured"))
                .extracting(Endpoint::verb, Endpoint::path)
                .containsExactly(tuple(HttpVerb.POST, "/items"));
    }

    @Test
    @DisplayName("ignores a REST Assured when().get(...) call with no filter(document(...)) in the same method")
    void ignoresUndocumentedRestAssured() throws Exception {
        List<Endpoint> endpoints = scanner.scan(fixtureDir());

        assertThat(endpoints).noneMatch(e -> e.methodSignature().startsWith("listItemsUndocumentedRestAssured"));
    }

    @Test
    @DisplayName("ignores a documented REST Assured method whose path argument is not a string literal")
    void ignoresRestAssuredDynamicPath() throws Exception {
        List<Endpoint> endpoints = scanner.scan(fixtureDir());

        assertThat(endpoints).noneMatch(e -> e.methodSignature().startsWith("deleteItemDynamicPathRestAssured"));
    }

    @Test
    @DisplayName("ignores a get(...) call that is not scoped on when(...)")
    void ignoresGetCallOutsideWhen() throws Exception {
        List<Endpoint> endpoints = scanner.scan(fixtureDir());

        assertThat(endpoints).noneMatch(e -> e.methodSignature().startsWith("getCallOutsideWhenRestAssured"));
    }

    @Test
    @DisplayName("strips a configured base path from a captured REST Assured path")
    void stripsConfiguredBasePath() throws Exception {
        RestDocsScanner prefixedScanner = new RestDocsScanner("/crm-service");

        List<Endpoint> endpoints = prefixedScanner.scan(fixtureDir());

        assertThat(endpoints)
                .filteredOn(e -> e.methodSignature().startsWith("getItemWithServerBasePathRestAssured"))
                .extracting(Endpoint::path)
                .containsExactly("/items/{id}");
    }

    @Test
    @DisplayName("leaves a captured path unchanged when it does not start with the configured base path")
    void leavesPathUnchangedWhenBasePathDoesNotMatch() throws Exception {
        RestDocsScanner prefixedScanner = new RestDocsScanner("/other-service");

        List<Endpoint> endpoints = prefixedScanner.scan(fixtureDir());

        assertThat(endpoints)
                .filteredOn(e -> e.methodSignature().startsWith("getItemWithServerBasePathRestAssured"))
                .extracting(Endpoint::path)
                .containsExactly("/crm-service/items/{id}");
    }

    @Test
    @DisplayName("leaves an unprefixed captured path unchanged when a base path is configured")
    void leavesUnprefixedPathUnchangedWhenBasePathConfigured() throws Exception {
        RestDocsScanner prefixedScanner = new RestDocsScanner("/crm-service");

        List<Endpoint> endpoints = prefixedScanner.scan(fixtureDir());

        assertThat(endpoints)
                .filteredOn(e -> e.methodSignature().startsWith("getItemRestAssured"))
                .extracting(Endpoint::path)
                .containsExactly("/items/{id}");
    }

    @Test
    @DisplayName("recognises a documented WebTestClient get(...) call (new coverage)")
    void recognisesDocumentedWebTestClientGetNewCoverage() throws Exception {
        List<Endpoint> endpoints = scanner.scan(fixtureDir());

        assertThat(endpoints)
                .filteredOn(e -> e.methodSignature().startsWith("getItemWebTestClient"))
                .extracting(Endpoint::verb, Endpoint::path)
                .containsExactly(tuple(HttpVerb.GET, "/items/{id}"));
    }

    @Test
    @DisplayName("recognises a documented WebTestClient post(...) call (new coverage)")
    void recognisesDocumentedWebTestClientPostNewCoverage() throws Exception {
        List<Endpoint> endpoints = scanner.scan(fixtureDir());

        assertThat(endpoints)
                .filteredOn(e -> e.methodSignature().startsWith("createItemWebTestClient"))
                .extracting(Endpoint::verb, Endpoint::path)
                .containsExactly(tuple(HttpVerb.POST, "/items"));
    }

    @Test
    @DisplayName("ignores a WebTestClient call with no consumeWith(document(...)) (new coverage)")
    void ignoresUndocumentedWebTestClientNewCoverage() throws Exception {
        List<Endpoint> endpoints = scanner.scan(fixtureDir());

        assertThat(endpoints).noneMatch(e -> e.methodSignature().startsWith("listItemsUndocumentedWebTestClient"));
    }

    @Test
    @DisplayName("ignores a WebTestClient call whose path argument is not a string literal (new coverage)")
    void ignoresDynamicPathWebTestClientNewCoverage() throws Exception {
        List<Endpoint> endpoints = scanner.scan(fixtureDir());

        assertThat(endpoints).noneMatch(e -> e.methodSignature().startsWith("deleteItemDynamicPathWebTestClient"));
    }

    private static File fixtureDir() {
        URL url = RestDocsScannerTest.class.getClassLoader().getResource("fixtures/restdocs");
        if (url == null) {
            throw new IllegalStateException("Fixture directory not found on classpath: fixtures/restdocs");
        }
        return new File(url.getFile());
    }

    private static org.assertj.core.groups.Tuple tuple(Object... values) {
        return org.assertj.core.groups.Tuple.tuple(values);
    }
}

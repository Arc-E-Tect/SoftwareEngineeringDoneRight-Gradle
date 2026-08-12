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

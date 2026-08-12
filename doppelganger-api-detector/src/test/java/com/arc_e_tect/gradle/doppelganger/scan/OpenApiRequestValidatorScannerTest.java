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

@DisplayName("OpenApiRequestValidatorScanner")
class OpenApiRequestValidatorScannerTest {

    private final OpenApiRequestValidatorScanner scanner = new OpenApiRequestValidatorScanner();

    @Test
    @DisplayName("recognises a REST Assured request validated via .filter(new OpenApiValidationFilter(...))")
    void recognisesFilterStyleValidation() throws Exception {
        List<Endpoint> endpoints = scanner.scan(fixtureDir());

        assertThat(endpoints)
                .filteredOn(e -> e.methodSignature().startsWith("getOrderWithFilter"))
                .extracting(Endpoint::verb, Endpoint::path)
                .containsExactly(tuple(HttpVerb.GET, "/orders/{id}"));
    }

    @Test
    @DisplayName("recognises a REST Assured request validated via direct validateRequest/validateResponse calls")
    void recognisesDirectValidationCalls() throws Exception {
        List<Endpoint> endpoints = scanner.scan(fixtureDir());

        assertThat(endpoints)
                .filteredOn(e -> e.methodSignature().startsWith("createOrderWithDirectValidation"))
                .extracting(Endpoint::verb, Endpoint::path)
                .containsExactly(tuple(HttpVerb.POST, "/orders"));
    }

    @Test
    @DisplayName("ignores a REST Assured request with no validation call in the same method")
    void ignoresUnvalidatedRequest() throws Exception {
        List<Endpoint> endpoints = scanner.scan(fixtureDir());

        assertThat(endpoints).noneMatch(e -> e.methodSignature().startsWith("listOrdersWithoutValidation"));
    }

    @Test
    @DisplayName("ignores a validation call with no given()/when() REST Assured request in the same method")
    void ignoresValidationWithoutRestAssuredStyleRequest() throws Exception {
        List<Endpoint> endpoints = scanner.scan(fixtureDir());

        assertThat(endpoints).noneMatch(e -> e.methodSignature().startsWith("deleteOrderNotRestAssuredStyle"));
    }

    @Test
    @DisplayName("sets the declaring class, source file, and a positive line number")
    void setsDeclaringClassSourceFileAndLineNumber() throws Exception {
        List<Endpoint> endpoints = scanner.scan(fixtureDir());

        assertThat(endpoints)
                .filteredOn(e -> e.methodSignature().startsWith("getOrderWithFilter"))
                .allSatisfy(e -> {
                    assertThat(e.declaringClass())
                            .isEqualTo("com.example.fixture.OpenApiRequestValidatorScannerFixture");
                    assertThat(e.sourceFile()).isEqualTo("OpenApiRequestValidatorScannerFixture.java");
                    assertThat(e.lineNumber()).isPositive();
                });
    }

    @Test
    @DisplayName("returns an empty list when the directory does not exist")
    void returnsEmptyListForMissingDirectory(@TempDir Path tempDir) throws Exception {
        File missing = new File(tempDir.toFile(), "does-not-exist");

        assertThat(scanner.scan(missing)).isEmpty();
    }

    private static File fixtureDir() {
        URL url = OpenApiRequestValidatorScannerTest.class.getClassLoader().getResource("fixtures/openapivalidator");
        if (url == null) {
            throw new IllegalStateException("Fixture directory not found on classpath: fixtures/openapivalidator");
        }
        return new File(url.getFile());
    }
}

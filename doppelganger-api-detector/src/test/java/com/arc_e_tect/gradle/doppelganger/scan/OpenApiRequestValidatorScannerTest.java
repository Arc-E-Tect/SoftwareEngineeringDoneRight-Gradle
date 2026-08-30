package com.arc_e_tect.gradle.doppelganger.scan;

import com.arc_e_tect.gradle.detector.core.model.Endpoint;
import com.arc_e_tect.gradle.detector.core.model.HttpVerb;
import com.arc_e_tect.gradle.detector.core.scan.PropertyResolutionContext;
import com.arc_e_tect.gradle.doppelganger.detect.VerifiedContractTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    @DisplayName("resolves a path argument that references a literal-initialized field constant")
    void resolvesFieldConstantPathArgument() throws Exception {
        List<Endpoint> endpoints = scanner.scan(fixtureDir());

        assertThat(endpoints)
                .filteredOn(e -> e.methodSignature().startsWith("listItemsWithConstantPath"))
                .extracting(Endpoint::verb, Endpoint::path)
                .containsExactly(tuple(HttpVerb.GET, "/items"));
    }

    @Test
    @DisplayName("still ignores a path argument computed by a method call")
    void ignoresMethodCallPathArgument() throws Exception {
        List<Endpoint> endpoints = scanner.scan(fixtureDir());

        assertThat(endpoints).noneMatch(e -> e.methodSignature().startsWith("listItemsWithDynamicPath"));
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

    @Test
    @DisplayName("scanWithStatusCodes() detects the REST Assured .then().statusCode(...) assertion")
    void scanWithStatusCodesDetectsStatusCode() throws Exception {
        List<VerifiedContractTest> tests = scanner.scanWithStatusCodes(fixtureDir());

        assertThat(tests)
                .filteredOn(t -> t.endpoint().methodSignature().startsWith("getOrderWithFilter"))
                .extracting(VerifiedContractTest::statusCode)
                .containsExactly("200");
    }

    @Test
    @DisplayName("scanWithStatusCodes() reports null when a validated request asserts no status")
    void scanWithStatusCodesReportsNullWhenNoStatusAsserted() throws Exception {
        List<VerifiedContractTest> tests = scanner.scanWithStatusCodes(fixtureDir());

        assertThat(tests)
                .filteredOn(t -> t.endpoint().methodSignature().startsWith("createOrderWithDirectValidation"))
                .extracting(VerifiedContractTest::statusCode)
                .containsExactly((String) null);
    }

    @Test
    @DisplayName("ignores a helper-method call when no propertyResolutionContext is configured")
    void ignoresHelperMethodCallWithoutContext() throws Exception {
        List<Endpoint> endpoints = scanner.scan(fixtureDir());

        assertThat(endpoints).noneMatch(e -> e.methodSignature().startsWith("getUserByUsernamePropertyHelperMethod"));
    }

    @Test
    @DisplayName("ignores an @Value-annotated field path argument when no propertyResolutionContext is configured")
    void ignoresValueAnnotatedFieldWithoutContext() throws Exception {
        List<Endpoint> endpoints = scanner.scan(fixtureDir());

        assertThat(endpoints).noneMatch(e -> e.methodSignature().startsWith("getUserByUsernameValueAnnotation"));
    }

    @Test
    @DisplayName("resolves a configured helper-method call's argument against a propertyResolutionContext")
    void resolvesHelperMethodCallWithContext() throws Exception {
        PropertyResolutionContext context = PropertyResolutionContext.of(
                Map.of("users.by-username", "/v1/users/{username}"), Set.of("ApiEndpoints.path"));
        OpenApiRequestValidatorScanner scannerWithContext = new OpenApiRequestValidatorScanner(context);

        List<Endpoint> endpoints = scannerWithContext.scan(fixtureDir());

        assertThat(endpoints)
                .filteredOn(e -> e.methodSignature().startsWith("getUserByUsernamePropertyHelperMethod"))
                .extracting(Endpoint::verb, Endpoint::path)
                .containsExactly(tuple(HttpVerb.GET, "/v1/users/{username}"));
    }

    @Test
    @DisplayName("resolves an @Value-annotated field path argument against a propertyResolutionContext")
    void resolvesValueAnnotatedFieldWithContext() throws Exception {
        PropertyResolutionContext context = PropertyResolutionContext.of(
                Map.of("users.by-username", "/v1/users/{username}"), Set.of());
        OpenApiRequestValidatorScanner scannerWithContext = new OpenApiRequestValidatorScanner(context);

        List<Endpoint> endpoints = scannerWithContext.scan(fixtureDir());

        assertThat(endpoints)
                .filteredOn(e -> e.methodSignature().startsWith("getUserByUsernameValueAnnotation"))
                .extracting(Endpoint::verb, Endpoint::path)
                .containsExactly(tuple(HttpVerb.GET, "/v1/users/{username}"));
    }

    private static File fixtureDir() {
        URL url = OpenApiRequestValidatorScannerTest.class.getClassLoader().getResource("fixtures/openapivalidator");
        if (url == null) {
            throw new IllegalStateException("Fixture directory not found on classpath: fixtures/openapivalidator");
        }
        return new File(url.getFile());
    }
}

package com.arc_e_tect.gradle.jacoco.scan;

import com.arc_e_tect.gradle.jacoco.model.ExcludedElement;
import com.arc_e_tect.gradle.jacoco.model.ExcludedElement.ElementType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link AnnotationScanner}.
 *
 * <p>Uses fixture {@code .java} source files placed under
 * {@code src/test/resources/fixtures/} so JavaParser can parse them as text —
 * the annotation class does not need to be on the test classpath.</p>
 */
@DisplayName("AnnotationScanner")
class AnnotationScannerTest {

    private static final String ANNOTATION = "ExcludeFromJacocoGeneratedCodeCoverage";

    private AnnotationScanner scanner;

    @BeforeEach
    void setUp() {
        scanner = new AnnotationScanner(ANNOTATION);
    }

    // ── Positive fixture: AnnotatedFixture.java ───────────────────────────────

    @Test
    @DisplayName("finds all annotated elements in the fixture")
    void findsAllAnnotatedElements() throws Exception {
        List<ExcludedElement> elements = scanner.scan(fixture("AnnotatedFixture.java"));

        assertThat(elements)
                .as("total excluded elements")
                .hasSize(4);
    }

    @Test
    @DisplayName("detects class-level annotation")
    void detectsClassLevelAnnotation() throws Exception {
        List<ExcludedElement> elements = scanner.scan(fixture("AnnotatedFixture.java"));

        List<ExcludedElement> classes = byType(elements, ElementType.CLASS);
        assertThat(classes).hasSize(1);

        ExcludedElement cls = classes.get(0);
        assertThat(cls.getClassName()).isEqualTo("AnnotatedFixture");
        assertThat(cls.getPackageName()).isEqualTo("com.example.fixture");
        assertThat(cls.getMember()).isEmpty();
        assertThat(cls.getLineNumber()).isPositive();
    }

    @Test
    @DisplayName("detects constructor annotation with parameter types in signature")
    void detectsConstructorAnnotation() throws Exception {
        List<ExcludedElement> elements = scanner.scan(fixture("AnnotatedFixture.java"));

        List<ExcludedElement> ctors = byType(elements, ElementType.CONSTRUCTOR);
        assertThat(ctors).hasSize(1);

        ExcludedElement ctor = ctors.get(0);
        assertThat(ctor.getClassName()).isEqualTo("AnnotatedFixture");
        assertThat(ctor.getMember()).isEqualTo("AnnotatedFixture(String)");
        assertThat(ctor.getLineNumber()).isPositive();
    }

    @Test
    @DisplayName("detects method annotation with full parameter signature")
    void detectsMethodAnnotation() throws Exception {
        List<ExcludedElement> elements = scanner.scan(fixture("AnnotatedFixture.java"));

        List<ExcludedElement> methods = byType(elements, ElementType.METHOD);
        assertThat(methods).hasSize(1);

        ExcludedElement method = methods.get(0);
        assertThat(method.getClassName()).isEqualTo("AnnotatedFixture");
        assertThat(method.getMember()).isEqualTo("excludedMethod(int, String)");
        assertThat(method.getLineNumber()).isPositive();
    }

    @Test
    @DisplayName("detects field annotation")
    void detectsFieldAnnotation() throws Exception {
        List<ExcludedElement> elements = scanner.scan(fixture("AnnotatedFixture.java"));

        List<ExcludedElement> fields = byType(elements, ElementType.FIELD);
        assertThat(fields).hasSize(1);

        ExcludedElement field = fields.get(0);
        assertThat(field.getMember()).isEqualTo("excludedField");
    }

    @Test
    @DisplayName("does NOT include non-annotated methods or constructors")
    void doesNotIncludeNonAnnotatedMembers() throws Exception {
        List<ExcludedElement> elements = scanner.scan(fixture("AnnotatedFixture.java"));

        Map<String, List<ExcludedElement>> byMember = elements.stream()
                .collect(Collectors.groupingBy(ExcludedElement::getMember));

        assertThat(byMember).doesNotContainKey("notExcluded()");
        // The no-arg constructor is NOT annotated — should not appear
        assertThat(byType(elements, ElementType.CONSTRUCTOR)).hasSize(1);
    }

    @Test
    @DisplayName("sets the source file name on every element")
    void setsSourceFileName() throws Exception {
        List<ExcludedElement> elements = scanner.scan(fixture("AnnotatedFixture.java"));

        assertThat(elements)
                .extracting(ExcludedElement::getSourceFile)
                .containsOnly("AnnotatedFixture.java");
    }

    // ── Negative fixture: UnannotatedFixture.java ─────────────────────────────

    @Test
    @DisplayName("returns empty list for a class with no exclusion annotations")
    void returnsEmptyForUnannotatedFile() throws Exception {
        List<ExcludedElement> elements = scanner.scan(fixture("UnannotatedFixture.java"));

        assertThat(elements).isEmpty();
    }

    // ── FQCN helper ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("getFqcn() builds fully-qualified class name correctly")
    void buildsFqcn() throws Exception {
        List<ExcludedElement> elements = scanner.scan(fixture("AnnotatedFixture.java"));

        assertThat(elements)
                .extracting(ExcludedElement::getFqcn)
                .containsOnly("com.example.fixture.AnnotatedFixture");
    }

    // ── Justification extraction ──────────────────────────────────────────────

    @Test
    @DisplayName("extracts justification from class-level annotation")
    void extractsClassJustification() throws Exception {
        List<ExcludedElement> elements = scanner.scan(fixture("AnnotatedFixture.java"));
        ExcludedElement cls = byType(elements, ElementType.CLASS).get(0);
        assertThat(cls.getJustification()).isEqualTo("Framework-managed class, not unit-testable");
    }

    @Test
    @DisplayName("extracts justification from constructor annotation")
    void extractsConstructorJustification() throws Exception {
        List<ExcludedElement> elements = scanner.scan(fixture("AnnotatedFixture.java"));
        ExcludedElement ctor = byType(elements, ElementType.CONSTRUCTOR).get(0);
        assertThat(ctor.getJustification()).isEqualTo("Boilerplate constructor");
    }

    @Test
    @DisplayName("extracts justification from method annotation")
    void extractsMethodJustification() throws Exception {
        List<ExcludedElement> elements = scanner.scan(fixture("AnnotatedFixture.java"));
        ExcludedElement method = byType(elements, ElementType.METHOD).get(0);
        assertThat(method.getJustification()).isEqualTo("Delegation method only");
    }

    @Test
    @DisplayName("returns empty justification when annotation is a plain marker")
    void returnsEmptyJustificationForMarkerAnnotation() throws Exception {
        List<ExcludedElement> elements = scanner.scan(fixture("AnnotatedFixture.java"));
        ExcludedElement field = byType(elements, ElementType.FIELD).get(0);
        assertThat(field.getJustification()).isEmpty();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static File fixture(String name) {
        URL url = AnnotationScannerTest.class.getClassLoader()
                .getResource("fixtures/" + name);
        if (url == null) {
            throw new IllegalStateException("Fixture not found on classpath: fixtures/" + name);
        }
        return new File(url.getFile());
    }

    private static List<ExcludedElement> byType(List<ExcludedElement> all, ElementType type) {
        return all.stream().filter(e -> e.getType() == type).collect(Collectors.toList());
    }
}

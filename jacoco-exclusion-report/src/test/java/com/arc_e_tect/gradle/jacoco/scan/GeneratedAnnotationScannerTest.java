package com.arc_e_tect.gradle.jacoco.scan;

import com.arc_e_tect.gradle.jacoco.model.ExcludedElement;
import com.arc_e_tect.gradle.jacoco.model.ExcludedElement.ElementType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link GeneratedAnnotationScanner}.
 *
 * <p>Unlike {@link AnnotationScannerTest}, these fixtures must be compiled:
 * the scanner reads bytecode, so a marker annotation whose simple name is
 * {@code Generated} (mirroring {@code lombok.Generated}) needs to actually be
 * present in a {@code .class} file's constant pool for the scanner to find it.</p>
 */
@DisplayName("GeneratedAnnotationScanner")
class GeneratedAnnotationScannerTest {

    @TempDir
    static Path tempDir;

    private static Path classesDir;

    private final GeneratedAnnotationScanner scanner = new GeneratedAnnotationScanner();

    @BeforeAll
    static void compileFixtures() throws IOException {
        Path sourcesDir = tempDir.resolve("sources");
        Files.createDirectories(sourcesDir);
        classesDir = tempDir.resolve("classes");
        Files.createDirectories(classesDir);

        Path markerSource = sourcesDir.resolve("Generated.java");
        Files.writeString(markerSource, """
                package fixtures.generated;

                import java.lang.annotation.ElementType;
                import java.lang.annotation.Retention;
                import java.lang.annotation.RetentionPolicy;
                import java.lang.annotation.Target;

                @Retention(RetentionPolicy.CLASS)
                @Target({ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.FIELD})
                public @interface Generated {
                }
                """);

        Path fixtureSource = sourcesDir.resolve("LombokLikeFixture.java");
        Files.writeString(fixtureSource, """
                package fixtures.generated;

                public class LombokLikeFixture {

                    @Generated
                    private String name;

                    public LombokLikeFixture() {
                    }

                    @Generated
                    public LombokLikeFixture(String name) {
                        this.name = name;
                    }

                    @Generated
                    public String getName() {
                        return name;
                    }

                    public String regularMethod() {
                        return "not generated";
                    }

                    @Generated
                    public static class GeneratedNestedClass {
                    }
                }
                """);

        Path plainSource = sourcesDir.resolve("PlainFixture.java");
        Files.writeString(plainSource, """
                package fixtures.generated;

                public class PlainFixture {
                    public String hello() {
                        return "hello";
                    }
                }
                """);

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        int result = compiler.run(null, null, null,
                "-d", classesDir.toString(),
                markerSource.toString(), fixtureSource.toString(), plainSource.toString());
        assertThat(result).as("fixture compilation exit code").isZero();
    }

    // ── LombokLikeFixture.class ─────────────────────────────────────────────

    @Test
    @DisplayName("finds every member carrying the Generated-named annotation")
    void findsGeneratedMembers() throws Exception {
        List<ExcludedElement> elements = scanner.scan(classFile("LombokLikeFixture"));

        assertThat(elements).hasSize(3);
    }

    @Test
    @DisplayName("detects an annotated constructor with parameter types in the signature")
    void detectsConstructor() throws Exception {
        List<ExcludedElement> elements = scanner.scan(classFile("LombokLikeFixture"));

        List<ExcludedElement> ctors = byType(elements, ElementType.CONSTRUCTOR);
        assertThat(ctors).hasSize(1);
        assertThat(ctors.get(0).getMember()).isEqualTo("LombokLikeFixture(String)");
        assertThat(ctors.get(0).getPackageName()).isEqualTo("fixtures.generated");
        assertThat(ctors.get(0).getJustification()).isEqualTo("@fixtures.generated.Generated");
    }

    @Test
    @DisplayName("detects an annotated method")
    void detectsMethod() throws Exception {
        List<ExcludedElement> elements = scanner.scan(classFile("LombokLikeFixture"));

        List<ExcludedElement> methods = byType(elements, ElementType.METHOD);
        assertThat(methods).hasSize(1);
        assertThat(methods.get(0).getMember()).isEqualTo("getName()");
    }

    @Test
    @DisplayName("detects an annotated field")
    void detectsField() throws Exception {
        List<ExcludedElement> elements = scanner.scan(classFile("LombokLikeFixture"));

        List<ExcludedElement> fields = byType(elements, ElementType.FIELD);
        assertThat(fields).hasSize(1);
        assertThat(fields.get(0).getMember()).isEqualTo("name");
    }

    @Test
    @DisplayName("does NOT include the unannotated no-arg constructor or regularMethod()")
    void excludesUnannotatedMembers() throws Exception {
        List<ExcludedElement> elements = scanner.scan(classFile("LombokLikeFixture"));

        assertThat(elements)
                .extracting(ExcludedElement::getMember)
                .doesNotContain("LombokLikeFixture()", "regularMethod()");
    }

    @Test
    @DisplayName("sets the source file name from the class's SourceFile attribute")
    void setsSourceFileName() throws Exception {
        List<ExcludedElement> elements = scanner.scan(classFile("LombokLikeFixture"));

        assertThat(elements)
                .extracting(ExcludedElement::getSourceFile)
                .containsOnly("LombokLikeFixture.java");
    }

    // ── GeneratedNestedClass.class ──────────────────────────────────────────

    @Test
    @DisplayName("detects a class-level Generated-named annotation on a nested class")
    void detectsClassLevelAnnotation() throws Exception {
        List<ExcludedElement> elements = scanner.scan(classFile("LombokLikeFixture$GeneratedNestedClass"));

        assertThat(elements).hasSize(1);
        ExcludedElement cls = elements.get(0);
        assertThat(cls.getType()).isEqualTo(ElementType.CLASS);
        assertThat(cls.getClassName()).isEqualTo("LombokLikeFixture.GeneratedNestedClass");
        assertThat(cls.getMember()).isEmpty();
    }

    // ── PlainFixture.class ───────────────────────────────────────────────────

    @Test
    @DisplayName("returns empty list for a class with no Generated-named annotations")
    void returnsEmptyForPlainClass() throws Exception {
        List<ExcludedElement> elements = scanner.scan(classFile("PlainFixture"));

        assertThat(elements).isEmpty();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static File classFile(String binaryName) {
        return classesDir.resolve("fixtures/generated/" + binaryName + ".class").toFile();
    }

    private static List<ExcludedElement> byType(List<ExcludedElement> all, ElementType type) {
        return all.stream().filter(e -> e.getType() == type).collect(Collectors.toList());
    }
}

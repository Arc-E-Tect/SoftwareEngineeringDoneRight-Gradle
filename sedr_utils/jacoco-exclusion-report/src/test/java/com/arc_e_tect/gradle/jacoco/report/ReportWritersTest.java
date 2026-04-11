package com.arc_e_tect.gradle.jacoco.report;

import com.arc_e_tect.gradle.jacoco.model.ExcludedElement;
import com.arc_e_tect.gradle.jacoco.model.ExcludedElement.ElementType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link HtmlReportWriter} and {@link XmlReportWriter}.
 *
 * <p>Verifies that both writers produce files in the expected locations and
 * that the file contents contain the key structural elements — not an exact
 * string match, which would be fragile.</p>
 */
@DisplayName("Report writers")
class ReportWritersTest {

    private static final String ANNOTATION = "ExcludeFromJacocoGeneratedCodeCoverage";

    private static final List<ExcludedElement> SAMPLE_ELEMENTS = List.of(
            new ExcludedElement(ElementType.CLASS,       "com.example", "SampleService", "",                          10, "SampleService.java"),
            new ExcludedElement(ElementType.CONSTRUCTOR, "com.example", "SampleService", "SampleService(String)",    15, "SampleService.java"),
            new ExcludedElement(ElementType.METHOD,      "com.example", "SampleService", "process(int, String)",     22, "SampleService.java"),
            new ExcludedElement(ElementType.FIELD,       "com.example", "SampleService", "cachedResult",             12, "SampleService.java")
    );

    // ── HtmlReportWriter ──────────────────────────────────────────────────────

    @Test
    @DisplayName("creates index.html in the output directory")
    void htmlWriterCreatesFile(@TempDir File tmpDir) throws Exception {
        new HtmlReportWriter().write(SAMPLE_ELEMENTS, ANNOTATION, tmpDir);

        assertThat(new File(tmpDir, "index.html")).exists().isNotEmpty();
    }

    @Test
    @DisplayName("HTML contains annotation name in header")
    void htmlContainsAnnotationName(@TempDir File tmpDir) throws Exception {
        new HtmlReportWriter().write(SAMPLE_ELEMENTS, ANNOTATION, tmpDir);

        String html = Files.readString(new File(tmpDir, "index.html").toPath());
        assertThat(html).contains(ANNOTATION);
    }

    @Test
    @DisplayName("HTML lists all four elements by type badge")
    void htmlListsAllElements(@TempDir File tmpDir) throws Exception {
        new HtmlReportWriter().write(SAMPLE_ELEMENTS, ANNOTATION, tmpDir);

        String html = Files.readString(new File(tmpDir, "index.html").toPath());
        assertThat(html)
                .contains("badge-class")
                .contains("badge-constructor")
                .contains("badge-method")
                .contains("badge-field");
    }

    @Test
    @DisplayName("HTML contains class name and method signature")
    void htmlContainsClassAndMember(@TempDir File tmpDir) throws Exception {
        new HtmlReportWriter().write(SAMPLE_ELEMENTS, ANNOTATION, tmpDir);

        String html = Files.readString(new File(tmpDir, "index.html").toPath());
        assertThat(html)
                .contains("SampleService")
                .contains("process(int, String)");
    }

    @Test
    @DisplayName("HTML handles an empty element list gracefully")
    void htmlHandlesEmptyList(@TempDir File tmpDir) throws Exception {
        new HtmlReportWriter().write(List.of(), ANNOTATION, tmpDir);

        String html = Files.readString(new File(tmpDir, "index.html").toPath());
        assertThat(html).contains("No exclusions found");
    }

    // ── XmlReportWriter ───────────────────────────────────────────────────────

    @Test
    @DisplayName("creates jacoco-exclusions.xml in the output directory")
    void xmlWriterCreatesFile(@TempDir File tmpDir) throws Exception {
        new XmlReportWriter().write(SAMPLE_ELEMENTS, ANNOTATION, tmpDir);

        assertThat(new File(tmpDir, "jacoco-exclusions.xml")).exists().isNotEmpty();
    }

    @Test
    @DisplayName("XML contains Surefire testsuites root element")
    void xmlHasTestSuitesRoot(@TempDir File tmpDir) throws Exception {
        new XmlReportWriter().write(SAMPLE_ELEMENTS, ANNOTATION, tmpDir);

        String xml = Files.readString(new File(tmpDir, "jacoco-exclusions.xml").toPath());
        assertThat(xml)
                .contains("<testsuites")
                .contains("</testsuites>");
    }

    @Test
    @DisplayName("XML has a testcase for each element")
    void xmlHasOneTestcasePerElement(@TempDir File tmpDir) throws Exception {
        new XmlReportWriter().write(SAMPLE_ELEMENTS, ANNOTATION, tmpDir);

        String xml = Files.readString(new File(tmpDir, "jacoco-exclusions.xml").toPath());
        long count = xml.lines().filter(l -> l.trim().startsWith("<testcase")).count();
        assertThat(count).isEqualTo(SAMPLE_ELEMENTS.size());
    }

    @Test
    @DisplayName("XML testcase names contain element type and member")
    void xmlTestcaseNamesContainTypeAndMember(@TempDir File tmpDir) throws Exception {
        new XmlReportWriter().write(SAMPLE_ELEMENTS, ANNOTATION, tmpDir);

        String xml = Files.readString(new File(tmpDir, "jacoco-exclusions.xml").toPath());
        assertThat(xml)
                .contains("[class]")
                .contains("[constructor]")
                .contains("[method]")
                .contains("[field]");
    }

    @Test
    @DisplayName("XML uses FQCN as classname attribute")
    void xmlUsesFqcnAsClassname(@TempDir File tmpDir) throws Exception {
        new XmlReportWriter().write(SAMPLE_ELEMENTS, ANNOTATION, tmpDir);

        String xml = Files.readString(new File(tmpDir, "jacoco-exclusions.xml").toPath());
        assertThat(xml).contains("classname=\"com.example.SampleService\"");
    }

    @Test
    @DisplayName("XML reports zero failures")
    void xmlHasZeroFailures(@TempDir File tmpDir) throws Exception {
        new XmlReportWriter().write(SAMPLE_ELEMENTS, ANNOTATION, tmpDir);

        String xml = Files.readString(new File(tmpDir, "jacoco-exclusions.xml").toPath());
        assertThat(xml).contains("failures=\"0\"");
    }

    // ── Justification rendering ───────────────────────────────────────────────

    @Test
    @DisplayName("HTML contains Justification column header")
    void htmlContainsJustificationColumnHeader(@TempDir File tmpDir) throws Exception {
        new HtmlReportWriter().write(SAMPLE_ELEMENTS, ANNOTATION, tmpDir);

        String html = Files.readString(new File(tmpDir, "index.html").toPath());
        assertThat(html).contains("<th>Justification</th>");
    }

    @Test
    @DisplayName("HTML renders justification text when present")
    void htmlRendersJustificationText(@TempDir File tmpDir) throws Exception {
        List<ExcludedElement> elements = List.of(
                new ExcludedElement(ElementType.METHOD, "com.example", "FooService",
                        "doWork()", 5, "FooService.java", "Delegation method only"));
        new HtmlReportWriter().write(elements, ANNOTATION, tmpDir);

        String html = Files.readString(new File(tmpDir, "index.html").toPath());
        assertThat(html).contains("Delegation method only");
    }

    @Test
    @DisplayName("XML contains justification attribute when justification is present")
    void xmlContainsJustificationAttribute(@TempDir File tmpDir) throws Exception {
        List<ExcludedElement> elements = List.of(
                new ExcludedElement(ElementType.CLASS, "com.example", "FooService",
                        "", 1, "FooService.java", "Framework-managed class"));
        new XmlReportWriter().write(elements, ANNOTATION, tmpDir);

        String xml = Files.readString(new File(tmpDir, "jacoco-exclusions.xml").toPath());
        assertThat(xml).contains("justification=\"Framework-managed class\"");
    }

    @Test
    @DisplayName("XML omits justification attribute when justification is empty")
    void xmlOmitsJustificationAttributeWhenEmpty(@TempDir File tmpDir) throws Exception {
        new XmlReportWriter().write(SAMPLE_ELEMENTS, ANNOTATION, tmpDir);

        String xml = Files.readString(new File(tmpDir, "jacoco-exclusions.xml").toPath());
        assertThat(xml).doesNotContain("justification=");
    }
}

package com.arc_e_tect.gradle.gherkin.progress;

import com.arc_e_tect.gradle.gherkin.snippet.FeatureSnippet;
import com.arc_e_tect.gradle.gherkin.snippet.StatusSnippets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ReportTemplateRenderer")
class ReportTemplateRendererTest {

    private final ReportTemplateRenderer renderer = new ReportTemplateRenderer();

    @Test
    @DisplayName("renders a flat include per status when scenarios are not grouped by feature")
    void rendersFlatIncludePerStatus(@TempDir Path tempDir) throws IOException {
        File outputFile = tempDir.resolve("out/features.adoc").toFile();
        outputFile.getParentFile().mkdirs();
        File snippetDir = tempDir.resolve("out/snippets").toFile();
        File listedSnippet = new File(snippetDir, "listed.adoc");

        Map<ScenarioStatus, StatusSnippets> snippets = new EnumMap<>(ScenarioStatus.class);
        snippets.put(ScenarioStatus.LISTED, new StatusSnippets(List.of(), listedSnippet));
        snippets.put(ScenarioStatus.DEFINED, new StatusSnippets(List.of(), new File(snippetDir, "defined.adoc")));
        snippets.put(ScenarioStatus.IMPLEMENTED,
                new StatusSnippets(List.of(), new File(snippetDir, "implemented.adoc")));

        List<StatusSummary> summaries = List.of(
                new StatusSummary(ScenarioStatus.LISTED, "Listed", "blurb-listed", List.of(), 1, "33.3"),
                new StatusSummary(ScenarioStatus.DEFINED, "Defined", "blurb-defined", List.of(), 1, "33.3"),
                new StatusSummary(ScenarioStatus.IMPLEMENTED, "Implemented", "blurb-implemented", List.of(), 1, "33.4"));

        File template = writeTemplate(tempDir,
                "{{#sections}}== {{{status}}}\n{{{blurb}}}\n"
                + "{{^features}}include::{{{snippet}}}[]\n{{/features}}"
                + "{{#features}}=== {{{title}}}\ninclude::{{{snippet}}}[]\n{{/features}}\n{{/sections}}");

        renderer.render(outputFile, template, "1.0.0", summaries, snippets);

        String content = Files.readString(outputFile.toPath(), StandardCharsets.UTF_8);
        assertThat(content)
                .contains("== Listed", "blurb-listed", "include::snippets/listed.adoc[]")
                .contains("== Defined", "blurb-defined", "include::snippets/defined.adoc[]")
                .contains("== Implemented", "blurb-implemented", "include::snippets/implemented.adoc[]")
                .doesNotContain("===");
    }

    @Test
    @DisplayName("renders a nested per-feature include when scenarios are grouped by feature")
    void rendersNestedIncludePerFeature(@TempDir Path tempDir) throws IOException {
        File outputFile = tempDir.resolve("out/features.adoc").toFile();
        outputFile.getParentFile().mkdirs();
        File snippetDir = tempDir.resolve("out/snippets").toFile();
        File authSnippet = new File(new File(snippetDir, "authentication"), "implemented.adoc");
        File billingSnippet = new File(new File(snippetDir, "billing"), "implemented.adoc");

        Map<ScenarioStatus, StatusSnippets> snippets = new EnumMap<>(ScenarioStatus.class);
        snippets.put(ScenarioStatus.LISTED, new StatusSnippets(List.of(), new File(snippetDir, "listed.adoc")));
        snippets.put(ScenarioStatus.DEFINED, new StatusSnippets(List.of(), new File(snippetDir, "defined.adoc")));
        snippets.put(ScenarioStatus.IMPLEMENTED, new StatusSnippets(
                List.of(new FeatureSnippet("Authentication", authSnippet),
                        new FeatureSnippet("Billing", billingSnippet)),
                null));

        List<StatusSummary> summaries = List.of(
                new StatusSummary(ScenarioStatus.LISTED, "Listed", "blurb-listed", List.of(), 0, "0.0"),
                new StatusSummary(ScenarioStatus.DEFINED, "Defined", "blurb-defined", List.of(), 0, "0.0"),
                new StatusSummary(ScenarioStatus.IMPLEMENTED, "Implemented", "blurb-implemented", List.of(), 2, "100.0"));

        File template = writeTemplate(tempDir,
                "{{#sections}}== {{{status}}}\n"
                + "{{^features}}include::{{{snippet}}}[]\n{{/features}}"
                + "{{#features}}=== {{{title}}}\ninclude::{{{snippet}}}[]\n{{/features}}\n{{/sections}}");

        renderer.render(outputFile, template, "1.0.0", summaries, snippets);

        String content = Files.readString(outputFile.toPath(), StandardCharsets.UTF_8);
        assertThat(content)
                .contains("== Implemented")
                .contains("=== Authentication", "include::snippets/authentication/implemented.adoc[]")
                .contains("=== Billing", "include::snippets/billing/implemented.adoc[]");
    }

    @Test
    @DisplayName("uses summary counts and percentages in the context")
    void usesSummaryCountsAndPercentagesInContext(@TempDir Path tempDir) throws IOException {
        File outputFile = tempDir.resolve("features.adoc").toFile();
        Map<ScenarioStatus, StatusSnippets> snippets = emptySnippets(tempDir);
        List<StatusSummary> summaries = List.of(
                new StatusSummary(ScenarioStatus.LISTED, "Listed", "b", List.of(), 2, "40.0"),
                new StatusSummary(ScenarioStatus.DEFINED, "Defined", "b", List.of(), 1, "20.0"),
                new StatusSummary(ScenarioStatus.IMPLEMENTED, "Implemented", "b", List.of(), 2, "40.0"));

        File template = writeTemplate(tempDir,
                "{{#summary}}{{{status}}}={{count}} ({{percentage}}%)\n{{/summary}}");

        renderer.render(outputFile, template, "1.0.0", summaries, snippets);

        String content = Files.readString(outputFile.toPath(), StandardCharsets.UTF_8);
        assertThat(content)
                .contains("Listed=2 (40.0%)")
                .contains("Defined=1 (20.0%)")
                .contains("Implemented=2 (40.0%)");
    }

    @Test
    @DisplayName("exposes the system-under-test version in the context")
    void exposesSystemUnderTestVersionInContext(@TempDir Path tempDir) throws IOException {
        File outputFile = tempDir.resolve("features.adoc").toFile();
        Map<ScenarioStatus, StatusSnippets> snippets = emptySnippets(tempDir);
        List<StatusSummary> summaries = List.of(
                new StatusSummary(ScenarioStatus.LISTED, "Listed", "b", List.of(), 0, "0.0"),
                new StatusSummary(ScenarioStatus.DEFINED, "Defined", "b", List.of(), 0, "0.0"),
                new StatusSummary(ScenarioStatus.IMPLEMENTED, "Implemented", "b", List.of(), 0, "0.0"));

        File template = writeTemplate(tempDir, "System Under Test version: {{{systemUnderTestVersion}}}");

        renderer.render(outputFile, template, "v2.3.1", summaries, snippets);

        String content = Files.readString(outputFile.toPath(), StandardCharsets.UTF_8);
        assertThat(content).contains("System Under Test version: v2.3.1");
    }

    @Test
    @DisplayName("does not HTML-escape triple-mustache substitutions")
    void doesNotEscapeTripleMustacheSubstitutions(@TempDir Path tempDir) throws IOException {
        File outputFile = tempDir.resolve("features.adoc").toFile();
        File snippetDir = tempDir.resolve("snippets").toFile();
        Map<ScenarioStatus, StatusSnippets> snippets = new EnumMap<>(ScenarioStatus.class);
        for (ScenarioStatus status : ScenarioStatus.values()) {
            snippets.put(status, new StatusSnippets(List.of(),
                    new File(snippetDir, status.name().toLowerCase(java.util.Locale.ROOT) + ".adoc")));
        }
        List<StatusSummary> summaries = List.of(
                new StatusSummary(ScenarioStatus.LISTED, "R&D <Listed>", "b", List.of(), 0, "0.0"),
                new StatusSummary(ScenarioStatus.DEFINED, "Defined", "b", List.of(), 0, "0.0"),
                new StatusSummary(ScenarioStatus.IMPLEMENTED, "Implemented", "b", List.of(), 0, "0.0"));

        File template = writeTemplate(tempDir, "{{#legend}}{{{status}}}\n{{/legend}}");

        renderer.render(outputFile, template, "1.0.0", summaries, snippets);

        String content = Files.readString(outputFile.toPath(), StandardCharsets.UTF_8);
        assertThat(content).contains("R&D <Listed>");
    }

    @Test
    @DisplayName("throws a GradleException when the template file does not exist")
    void throwsWhenTemplateFileDoesNotExist(@TempDir Path tempDir) {
        File outputFile = tempDir.resolve("features.adoc").toFile();
        File missingTemplate = tempDir.resolve("missing.mustache").toFile();
        List<StatusSummary> summaries = List.of(
                new StatusSummary(ScenarioStatus.LISTED, "Listed", "b", List.of(), 0, "0.0"),
                new StatusSummary(ScenarioStatus.DEFINED, "Defined", "b", List.of(), 0, "0.0"),
                new StatusSummary(ScenarioStatus.IMPLEMENTED, "Implemented", "b", List.of(), 0, "0.0"));

        assertThatThrownBy(() -> renderer.render(outputFile, missingTemplate, "1.0.0", summaries, emptySnippets(tempDir)))
                .isInstanceOf(org.gradle.api.GradleException.class);
    }

    private Map<ScenarioStatus, StatusSnippets> emptySnippets(Path tempDir) {
        File snippetDir = tempDir.resolve("snippets").toFile();
        Map<ScenarioStatus, StatusSnippets> snippets = new EnumMap<>(ScenarioStatus.class);
        for (ScenarioStatus status : ScenarioStatus.values()) {
            snippets.put(status, new StatusSnippets(List.of(),
                    new File(snippetDir, status.name().toLowerCase(java.util.Locale.ROOT) + ".adoc")));
        }
        return snippets;
    }

    private File writeTemplate(Path tempDir, String content) throws IOException {
        File file = Files.createTempFile(tempDir, "template", ".mustache").toFile();
        Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
        return file;
    }
}

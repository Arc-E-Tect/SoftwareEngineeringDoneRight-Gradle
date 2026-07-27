package com.arc_e_tect.gradle.gherkin.progress;

import com.arc_e_tect.gradle.gherkin.snippet.FeatureSnippet;
import com.arc_e_tect.gradle.gherkin.snippet.StatusSnippets;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheException;
import org.gradle.api.GradleException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Renders a progress report from a user-supplied Mustache template, so that the generated
 * AsciiDoc file references the snippets written by {@link com.arc_e_tect.gradle.gherkin.snippet.SnippetWriter}
 * via {@code include::} directives instead of embedding their content verbatim.
 */
public class ReportTemplateRenderer {

    /** Creates a new {@code ReportTemplateRenderer}. */
    public ReportTemplateRenderer() {}

    /**
     * Renders {@code template} to {@code outputFile}, using {@code summaries} and {@code snippets}
     * to build the Mustache context. Snippet paths in the context are relative to
     * {@code outputFile}'s directory, so plain {@code include::<path>[]} directives resolve correctly.
     *
     * @param outputFile the AsciiDoc file to write
     * @param template   the Mustache template file to render
     * @param systemUnderTestVersion version of the system under test that the reported scenarios exercise
     * @param summaries  the classified scenarios and summary figures for each status, in display order
     * @param snippets   the snippet file(s) written for each status
     */
    public void render(
            File outputFile,
            File template,
            String systemUnderTestVersion,
            List<StatusSummary> summaries,
            Map<ScenarioStatus, StatusSnippets> snippets) {
        Map<String, Object> context = buildContext(systemUnderTestVersion, summaries, snippets, outputFile.getParentFile());

        try (Reader templateReader = new FileReader(template, StandardCharsets.UTF_8);
             PrintWriter writer = new PrintWriter(outputFile, StandardCharsets.UTF_8)) {
            Mustache mustache = new DefaultMustacheFactory().compile(templateReader, template.getName());
            mustache.execute(writer, context).flush();
        } catch (IOException e) {
            throw new GradleException("gherkinToAsciidoc: failed to render template '" + template + "': "
                    + e.getMessage(), e);
        } catch (MustacheException e) {
            throw new GradleException("gherkinToAsciidoc: failed to render template '" + template + "': "
                    + e.getMessage(), e);
        }
    }

    private Map<String, Object> buildContext(
            String systemUnderTestVersion, List<StatusSummary> summaries,
            Map<ScenarioStatus, StatusSnippets> snippets, File outputDir) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("systemUnderTestVersion", systemUnderTestVersion);
        context.put("intro", ReportText.INTRO);
        context.put("legend", summaries.stream()
                .map(s -> Map.of("status", s.label(), "meaning", s.blurb()))
                .collect(Collectors.toList()));
        context.put("summary", summaries.stream()
                .map(s -> Map.of("status", s.label(), "count", s.count(), "percentage", s.percentage()))
                .collect(Collectors.toList()));
        context.put("sections", summaries.stream()
                .map(s -> sectionContext(s, snippets.get(s.status()), outputDir))
                .collect(Collectors.toList()));
        return context;
    }

    private Map<String, Object> sectionContext(StatusSummary summary, StatusSnippets snippets, File outputDir) {
        Map<String, Object> section = new LinkedHashMap<>();
        section.put("status", summary.label());
        section.put("blurb", summary.blurb());
        if (snippets.features().isEmpty()) {
            section.put("features", List.of());
            section.put("snippet", relativePath(outputDir, snippets.flatFile()));
        } else {
            section.put("snippet", "");
            section.put("features", snippets.features().stream()
                    .map(f -> featureContext(f, outputDir))
                    .collect(Collectors.toList()));
        }
        return section;
    }

    private Map<String, Object> featureContext(FeatureSnippet feature, File outputDir) {
        return Map.of(
                "title", feature.featureTitle(),
                "snippet", relativePath(outputDir, feature.file()));
    }

    private String relativePath(File baseDir, File target) {
        return baseDir.toPath().relativize(target.toPath()).toString().replace(File.separatorChar, '/');
    }
}

package com.arc_e_tect.gradle.gherkin.progress;

import com.arc_e_tect.gradle.gherkin.parser.ScenarioGrouping;
import com.arc_e_tect.gradle.gherkin.parser.ScenarioInfo;
import com.arc_e_tect.gradle.gherkin.snippet.SnippetWriter;
import com.arc_e_tect.gradle.gherkin.snippet.StatusSnippets;
import io.cucumber.cucumberexpressions.Expression;
import org.gradle.api.GradleException;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Writes an AsciiDoc progress report that breaks scenarios down by
 * {@link ScenarioStatus} and summarises the breakdown as a table of counts and percentages.
 *
 * <p>Every call also writes the {@code listed.adoc}/{@code defined.adoc}/{@code implemented.adoc}
 * snippet files (see {@link SnippetWriter}), regardless of whether a template is configured. When
 * {@link ProgressReportOptions#template()} is set, the report itself is rendered from that Mustache
 * template via {@link ReportTemplateRenderer} - referencing the snippets via {@code include::}
 * directives - instead of embedding scenario titles verbatim.</p>
 */
public class ProgressReportWriter {

    private final ScenarioClassifier classifier = new ScenarioClassifier();
    private final SnippetWriter snippetWriter = new SnippetWriter();
    private final ReportTemplateRenderer templateRenderer = new ReportTemplateRenderer();

    /** Creates a new {@code ProgressReportWriter}. */
    public ProgressReportWriter() {}

    /**
     * Classifies every scenario, writes the snippet files, and writes the progress report to
     * {@code outputFile}.
     *
     * @param outputFile the AsciiDoc file to write
     * @param scenarios  the scenarios to report on
     * @param glueCode   step definition patterns found in the configured glue code directories
     * @param options    grouping, snippet, and template configuration
     */
    public void write(File outputFile, List<ScenarioInfo> scenarios, List<Expression> glueCode,
            ProgressReportOptions options) {
        if (scenarios.isEmpty()) {
            writeEmptyReport(outputFile, options.systemUnderTestVersion());
            return;
        }

        List<StatusSummary> summaries = buildSummaries(classify(scenarios, glueCode), scenarios.size());

        Map<ScenarioStatus, StatusSnippets> snippets = new EnumMap<>(ScenarioStatus.class);
        for (StatusSummary summary : summaries) {
            snippets.put(summary.status(), snippetWriter.writeStatus(
                    options.snippetDir(), summary.status(), summary.scenarios(), options.groupByFeature()));
        }

        if (options.template() != null) {
            templateRenderer.render(
                    outputFile, options.template(), options.systemUnderTestVersion(), summaries, snippets);
        } else {
            writeDefaultReport(outputFile, summaries, options.groupByFeature(), options.systemUnderTestVersion());
        }
    }

    private Map<ScenarioStatus, List<ScenarioInfo>> classify(List<ScenarioInfo> scenarios, List<Expression> glueCode) {
        Map<ScenarioStatus, List<ScenarioInfo>> byStatus = new EnumMap<>(ScenarioStatus.class);
        for (ScenarioStatus status : ScenarioStatus.values()) {
            byStatus.put(status, new ArrayList<>());
        }
        for (ScenarioInfo scenario : scenarios) {
            byStatus.get(classifier.classify(scenario, glueCode)).add(scenario);
        }
        return byStatus;
    }

    private List<StatusSummary> buildSummaries(Map<ScenarioStatus, List<ScenarioInfo>> byStatus, int total) {
        List<ScenarioInfo> listed = byStatus.get(ScenarioStatus.LISTED);
        List<ScenarioInfo> defined = byStatus.get(ScenarioStatus.DEFINED);
        List<ScenarioInfo> implemented = byStatus.get(ScenarioStatus.IMPLEMENTED);

        BigDecimal listedPct = percentage(listed.size(), total);
        BigDecimal definedPct = percentage(defined.size(), total);
        // The implemented percentage is derived from the other two so that the three
        // percentages always add up to exactly 100%, even after rounding.
        BigDecimal implementedPct = BigDecimal.valueOf(100)
                .subtract(listedPct)
                .subtract(definedPct)
                .setScale(1, RoundingMode.HALF_UP);

        return List.of(
                new StatusSummary(ScenarioStatus.LISTED, "Listed", ReportText.LISTED_BLURB,
                        listed, listed.size(), listedPct.toPlainString()),
                new StatusSummary(ScenarioStatus.DEFINED, "Defined", ReportText.DEFINED_BLURB,
                        defined, defined.size(), definedPct.toPlainString()),
                new StatusSummary(ScenarioStatus.IMPLEMENTED, "Implemented", ReportText.IMPLEMENTED_BLURB,
                        implemented, implemented.size(), implementedPct.toPlainString()));
    }

    private BigDecimal percentage(int count, int total) {
        return BigDecimal.valueOf(count)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
    }

    private void writeEmptyReport(File outputFile, String systemUnderTestVersion) {
        try (PrintWriter writer = new PrintWriter(outputFile, StandardCharsets.UTF_8)) {
            writer.println("= Feature Scenarios");
            writer.println(":toc:");
            writer.println(":toclevels: 2");
            writer.println();
            writer.println("System Under Test version: " + systemUnderTestVersion);
            writer.println();
            writer.println(ReportText.INTRO);
            writer.println();
            writer.println("No scenarios found.");
        } catch (IOException e) {
            throw new GradleException("gherkinToAsciidoc: failed to write AsciiDoc file: " + outputFile, e);
        }
    }

    private void writeDefaultReport(
            File outputFile, List<StatusSummary> summaries, boolean groupByFeature, String systemUnderTestVersion) {
        try (PrintWriter writer = new PrintWriter(outputFile, StandardCharsets.UTF_8)) {
            writer.println("= Feature Scenarios");
            writer.println(":toc:");
            writer.println(":toclevels: 2");
            writer.println();
            writer.println("System Under Test version: " + systemUnderTestVersion);
            writer.println();
            writer.println(ReportText.INTRO);
            writer.println();

            writeStatusLegend(writer, summaries);

            writer.println("== Progress Summary");
            writer.println();
            writeSummaryTable(writer, summaries);
            writer.println();

            for (StatusSummary summary : summaries) {
                writeSection(writer, summary, groupByFeature);
            }
        } catch (IOException e) {
            throw new GradleException("gherkinToAsciidoc: failed to write AsciiDoc file: " + outputFile, e);
        }
    }

    private void writeStatusLegend(PrintWriter writer, List<StatusSummary> summaries) {
        writer.println("Every scenario is classified as exactly one of:");
        writer.println();
        writer.println("[cols=\"1,3\",options=\"header\"]");
        writer.println("|===");
        writer.println("| Status | Meaning");
        writer.println();
        for (StatusSummary summary : summaries) {
            writer.println("| " + summary.label());
            writer.println("| " + summary.blurb());
            writer.println();
        }
        writer.println("|===");
        writer.println();
    }

    private void writeSummaryTable(PrintWriter writer, List<StatusSummary> summaries) {
        writer.println("[cols=\"1,1,1\",options=\"header\"]");
        writer.println("|===");
        writer.println("| Status | Count | Percentage");
        writer.println();
        for (StatusSummary summary : summaries) {
            writer.println("| " + summary.label());
            writer.println("| " + summary.count());
            writer.println("| " + summary.percentage() + "%");
            writer.println();
        }
        writer.println("|===");
    }

    private void writeSection(PrintWriter writer, StatusSummary summary, boolean groupByFeature) {
        writer.println("== " + summary.label());
        writer.println();
        writer.println(summary.blurb());
        writer.println();
        List<ScenarioInfo> scenarios = summary.scenarios();
        if (scenarios.isEmpty()) {
            writer.println("_None._");
            writer.println();
            return;
        }
        if (groupByFeature) {
            for (Map.Entry<String, List<ScenarioInfo>> entry : ScenarioGrouping.byFeatureTitle(scenarios).entrySet()) {
                writer.println("=== " + entry.getKey());
                writer.println();
                for (ScenarioInfo scenario : entry.getValue()) {
                    writer.println("* " + scenario.title());
                }
                writer.println();
            }
        } else {
            for (ScenarioInfo scenario : scenarios) {
                writer.println("* " + scenario.title());
            }
            writer.println();
        }
    }
}

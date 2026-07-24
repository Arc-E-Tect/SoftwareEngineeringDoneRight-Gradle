package com.arc_e_tect.gradle.gherkin.progress;

import com.arc_e_tect.gradle.gherkin.parser.ScenarioInfo;
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
 * {@link ScenarioStatus} and summarises the breakdown as a table of counts
 * and percentages.
 */
public class ProgressReportWriter {

    private static final String LISTED_BLURB =
            "Scenarios with a title only. No `Given`/`When`/`Then` steps have been written for them yet.";
    private static final String DEFINED_BLURB =
            "Scenarios with steps written, but at least one step has no matching glue code yet.";
    private static final String IMPLEMENTED_BLURB =
            "Scenarios whose every step has matching glue code.";

    private final ScenarioClassifier classifier = new ScenarioClassifier();

    /** Creates a new {@code ProgressReportWriter}. */
    public ProgressReportWriter() {}

    /**
     * Classifies every scenario and writes the progress report to {@code outputFile}.
     *
     * @param outputFile the AsciiDoc file to write
     * @param scenarios  the scenarios to report on
     * @param glueCode   step definition patterns found in the configured glue code directories
     */
    public void write(File outputFile, List<ScenarioInfo> scenarios, List<Expression> glueCode) {
        Map<ScenarioStatus, List<String>> titlesByStatus = new EnumMap<>(ScenarioStatus.class);
        for (ScenarioStatus status : ScenarioStatus.values()) {
            titlesByStatus.put(status, new ArrayList<>());
        }
        for (ScenarioInfo scenario : scenarios) {
            ScenarioStatus status = classifier.classify(scenario, glueCode);
            titlesByStatus.get(status).add(scenario.title());
        }

        try (PrintWriter writer = new PrintWriter(outputFile, StandardCharsets.UTF_8)) {
            writer.println("= Feature Scenarios");
            writer.println();
            writer.println("This document lists every `Scenario` and `Scenario Outline` found under the "
                    + "configured feature file directories, classified by how far each one is toward being "
                    + "automated.");
            writer.println();

            if (scenarios.isEmpty()) {
                writer.println("No scenarios found.");
                return;
            }

            writeStatusLegend(writer);

            writer.println("== Progress Summary");
            writer.println();
            writeSummaryTable(writer, titlesByStatus, scenarios.size());
            writer.println();

            writeSection(writer, "Listed", LISTED_BLURB, titlesByStatus.get(ScenarioStatus.LISTED));
            writeSection(writer, "Defined", DEFINED_BLURB, titlesByStatus.get(ScenarioStatus.DEFINED));
            writeSection(writer, "Implemented", IMPLEMENTED_BLURB, titlesByStatus.get(ScenarioStatus.IMPLEMENTED));
        } catch (IOException e) {
            throw new GradleException("gherkinToAsciidoc: failed to write AsciiDoc file: " + outputFile, e);
        }
    }

    private void writeStatusLegend(PrintWriter writer) {
        writer.println("Every scenario is classified as exactly one of:");
        writer.println();
        writer.println("[cols=\"1,3\",options=\"header\"]");
        writer.println("|===");
        writer.println("| Status | Meaning");
        writer.println();
        writer.println("| Listed");
        writer.println("| " + LISTED_BLURB);
        writer.println();
        writer.println("| Defined");
        writer.println("| " + DEFINED_BLURB);
        writer.println();
        writer.println("| Implemented");
        writer.println("| " + IMPLEMENTED_BLURB);
        writer.println("|===");
        writer.println();
    }

    private void writeSummaryTable(PrintWriter writer, Map<ScenarioStatus, List<String>> titlesByStatus, int total) {
        int listed = titlesByStatus.get(ScenarioStatus.LISTED).size();
        int defined = titlesByStatus.get(ScenarioStatus.DEFINED).size();
        int implemented = titlesByStatus.get(ScenarioStatus.IMPLEMENTED).size();

        BigDecimal listedPct = percentage(listed, total);
        BigDecimal definedPct = percentage(defined, total);
        // The implemented percentage is derived from the other two so that the three
        // percentages always add up to exactly 100%, even after rounding.
        BigDecimal implementedPct = BigDecimal.valueOf(100)
                .subtract(listedPct)
                .subtract(definedPct)
                .setScale(1, RoundingMode.HALF_UP);

        writer.println("[cols=\"1,1,1\",options=\"header\"]");
        writer.println("|===");
        writer.println("| Status | Count | Percentage");
        writer.println();
        writeRow(writer, "Listed", listed, listedPct);
        writeRow(writer, "Defined", defined, definedPct);
        writeRow(writer, "Implemented", implemented, implementedPct);
        writer.println("|===");
    }

    private void writeRow(PrintWriter writer, String label, int count, BigDecimal percentage) {
        writer.println("| " + label);
        writer.println("| " + count);
        writer.println("| " + percentage.toPlainString() + "%");
        writer.println();
    }

    private BigDecimal percentage(int count, int total) {
        return BigDecimal.valueOf(count)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
    }

    private void writeSection(PrintWriter writer, String heading, String blurb, List<String> titles) {
        writer.println("== " + heading);
        writer.println();
        writer.println(blurb);
        writer.println();
        if (titles.isEmpty()) {
            writer.println("_None._");
        } else {
            for (String title : titles) {
                writer.println("* " + title);
            }
        }
        writer.println();
    }
}

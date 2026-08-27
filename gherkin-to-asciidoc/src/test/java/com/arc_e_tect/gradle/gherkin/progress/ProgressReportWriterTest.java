package com.arc_e_tect.gradle.gherkin.progress;

import com.arc_e_tect.gradle.gherkin.parser.ScenarioInfo;
import io.cucumber.cucumberexpressions.Expression;
import io.cucumber.cucumberexpressions.ExpressionFactory;
import io.cucumber.cucumberexpressions.ParameterTypeRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ProgressReportWriter")
class ProgressReportWriterTest {

    private final ProgressReportWriter writer = new ProgressReportWriter();
    private final ExpressionFactory expressionFactory =
            new ExpressionFactory(new ParameterTypeRegistry(Locale.ENGLISH));

    @Test
    @DisplayName("includes an intro paragraph and a status legend explaining what each status means")
    void includesIntroAndStatusLegend(@TempDir Path tempDir) throws IOException {
        ScenarioInfo listed = new ScenarioInfo("Authentication", "Scenario: Only a title", List.of());

        File outputFile = tempDir.resolve("features.adoc").toFile();
        writer.write(outputFile, List.of(listed), List.of(), grouped(tempDir));

        String content = Files.readString(outputFile.toPath(), StandardCharsets.UTF_8);
        assertThat(content)
                .contains("System Under Test version: 1.0.0")
                .contains("This document lists every `Scenario` and `Scenario Outline`")
                .contains("Every scenario is classified as exactly one of:")
                .contains("| Status | Meaning")
                .contains("No `Given`/`When`/`Then` steps have been written for them yet.")
                .contains("at least one step has no matching glue code yet.")
                .contains("Scenarios whose every step has matching glue code.");
    }

    @Test
    @DisplayName("reports scenarios under Listed, Defined and Implemented headings")
    void reportsScenariosUnderEachHeading(@TempDir Path tempDir) throws IOException {
        ScenarioInfo listed = new ScenarioInfo("Authentication", "Scenario: Only a title", List.of());
        ScenarioInfo defined = new ScenarioInfo(
                "Authentication", "Scenario: Has steps, no glue", List.of("an unimplemented step"));
        ScenarioInfo implemented = new ScenarioInfo(
                "Authentication", "Scenario: Fully wired up", List.of("an implemented step"));
        List<Expression> glueCode = List.of(expression("an implemented step"));

        File outputFile = tempDir.resolve("features.adoc").toFile();
        writer.write(outputFile, List.of(listed, defined, implemented), glueCode, grouped(tempDir));

        String content = Files.readString(outputFile.toPath(), StandardCharsets.UTF_8);
        assertThat(content).contains("== Listed", "* Scenario: Only a title");
        assertThat(content).contains("== Defined", "* Scenario: Has steps, no glue");
        assertThat(content).contains("== Implemented", "* Scenario: Fully wired up");
    }

    @Test
    @DisplayName("groups scenarios under their feature within each status section when groupByFeature is true")
    void groupsScenariosByFeatureWithinEachStatus(@TempDir Path tempDir) throws IOException {
        ScenarioInfo authImplemented = new ScenarioInfo(
                "Authentication", "Scenario: User logs in", List.of("has glue"));
        ScenarioInfo billingImplemented = new ScenarioInfo(
                "Billing", "Scenario: User pays an invoice", List.of("has glue"));
        List<Expression> glueCode = List.of(expression("has glue"));

        File outputFile = tempDir.resolve("features.adoc").toFile();
        writer.write(outputFile, List.of(authImplemented, billingImplemented), glueCode, grouped(tempDir));

        String content = Files.readString(outputFile.toPath(), StandardCharsets.UTF_8);
        assertThat(content)
                .contains("== Implemented")
                .contains("=== Authentication")
                .contains("=== Billing");

        int authHeadingIndex = content.indexOf("=== Authentication");
        int authScenarioIndex = content.indexOf("* Scenario: User logs in");
        int billingHeadingIndex = content.indexOf("=== Billing");
        int billingScenarioIndex = content.indexOf("* Scenario: User pays an invoice");
        assertThat(authHeadingIndex).isLessThan(authScenarioIndex);
        assertThat(authScenarioIndex).isLessThan(billingHeadingIndex);
        assertThat(billingHeadingIndex).isLessThan(billingScenarioIndex);
    }

    @Test
    @DisplayName("keeps a flat list per status, with no feature headings, when groupByFeature is false")
    void keepsFlatListWhenGroupByFeatureIsFalse(@TempDir Path tempDir) throws IOException {
        ScenarioInfo authImplemented = new ScenarioInfo(
                "Authentication", "Scenario: User logs in", List.of("has glue"));
        ScenarioInfo billingImplemented = new ScenarioInfo(
                "Billing", "Scenario: User pays an invoice", List.of("has glue"));
        List<Expression> glueCode = List.of(expression("has glue"));

        File outputFile = tempDir.resolve("features.adoc").toFile();
        writer.write(outputFile, List.of(authImplemented, billingImplemented), glueCode, flat(tempDir));

        String content = Files.readString(outputFile.toPath(), StandardCharsets.UTF_8);
        assertThat(content)
                .contains("== Implemented", "* Scenario: User logs in", "* Scenario: User pays an invoice")
                .doesNotContain("=== Authentication")
                .doesNotContain("=== Billing");
    }

    @Test
    @DisplayName("groups multiple scenarios from the same feature under one feature heading")
    void groupsMultipleScenariosFromSameFeatureTogether(@TempDir Path tempDir) throws IOException {
        ScenarioInfo first = new ScenarioInfo("Authentication", "Scenario: First", List.of());
        ScenarioInfo second = new ScenarioInfo("Authentication", "Scenario: Second", List.of());

        File outputFile = tempDir.resolve("features.adoc").toFile();
        writer.write(outputFile, List.of(first, second), List.of(), grouped(tempDir));

        String content = Files.readString(outputFile.toPath(), StandardCharsets.UTF_8);
        assertThat(content.split("=== Authentication", -1)).hasSize(2);
        assertThat(content).contains("* Scenario: First", "* Scenario: Second");
    }

    @Test
    @DisplayName("summary table counts and percentages add up to 100%")
    void summaryTableCountsAndPercentagesAddUpTo100Percent(@TempDir Path tempDir) throws IOException {
        List<ScenarioInfo> scenarios = List.of(
                new ScenarioInfo("Feature", "Scenario: L1", List.of()),
                new ScenarioInfo("Feature", "Scenario: L2", List.of()),
                new ScenarioInfo("Feature", "Scenario: D1", List.of("no glue for this")),
                new ScenarioInfo("Feature", "Scenario: I1", List.of("has glue")),
                new ScenarioInfo("Feature", "Scenario: I2", List.of("has glue")),
                new ScenarioInfo("Feature", "Scenario: I3", List.of("has glue")));
        List<Expression> glueCode = List.of(expression("has glue"));

        File outputFile = tempDir.resolve("features.adoc").toFile();
        writer.write(outputFile, scenarios, glueCode, grouped(tempDir));

        String content = Files.readString(outputFile.toPath(), StandardCharsets.UTF_8);
        assertThat(content)
                .contains("| Listed", "| 2", "| 33.3%")
                .contains("| Defined", "| 1", "| 16.7%")
                .contains("| Implemented", "| 3", "| 50.0%");
    }

    @Test
    @DisplayName("prints a placeholder message when there are no scenarios")
    void printsPlaceholderWhenNoScenarios(@TempDir Path tempDir) throws IOException {
        File outputFile = tempDir.resolve("features.adoc").toFile();
        writer.write(outputFile, List.of(), List.of(), grouped(tempDir));

        String content = Files.readString(outputFile.toPath(), StandardCharsets.UTF_8);
        assertThat(content).contains("System Under Test version: 1.0.0");
        assertThat(content).contains("No scenarios found.");
        assertThat(content).doesNotContain("Progress Summary");
    }

    @Test
    @DisplayName("declares a table of contents right after the title, with or without scenarios")
    void declaresTableOfContents(@TempDir Path tempDir) throws IOException {
        File emptyOutputFile = tempDir.resolve("empty.adoc").toFile();
        writer.write(emptyOutputFile, List.of(), List.of(), grouped(tempDir));
        List<String> emptyLines = Files.readAllLines(emptyOutputFile.toPath(), StandardCharsets.UTF_8);
        assertThat(emptyLines).containsSubsequence("= Feature Scenarios", ":toc:", ":toclevels: 2", "");

        ScenarioInfo implemented = new ScenarioInfo(
                "Authentication", "Scenario: Fully wired up", List.of("an implemented step"));
        List<Expression> glueCode = List.of(expression("an implemented step"));
        File outputFile = tempDir.resolve("features.adoc").toFile();
        writer.write(outputFile, List.of(implemented), glueCode, grouped(tempDir));
        List<String> lines = Files.readAllLines(outputFile.toPath(), StandardCharsets.UTF_8);
        assertThat(lines).containsSubsequence("= Feature Scenarios", ":toc:", ":toclevels: 2", "");
    }

    @Test
    @DisplayName("prints a None placeholder for a heading with no scenarios, without a feature heading")
    void printsNonePlaceholderForEmptyHeading(@TempDir Path tempDir) throws IOException {
        ScenarioInfo implemented = new ScenarioInfo(
                "Authentication", "Scenario: Fully wired up", List.of("an implemented step"));
        List<Expression> glueCode = List.of(expression("an implemented step"));

        File outputFile = tempDir.resolve("features.adoc").toFile();
        writer.write(outputFile, List.of(implemented), glueCode, grouped(tempDir));

        String content = Files.readString(outputFile.toPath(), StandardCharsets.UTF_8);
        assertThat(content).contains("== Listed", "_None._");
        String listedSection = content.substring(content.indexOf("== Listed"), content.indexOf("== Defined"));
        assertThat(listedSection).doesNotContain("===");
    }

    @Test
    @DisplayName("always writes the listed/defined/implemented snippet files, even without a template")
    void alwaysWritesSnippetFiles(@TempDir Path tempDir) throws IOException {
        File snippetDir = tempDir.resolve("snippets").toFile();
        ScenarioInfo implemented = new ScenarioInfo(
                "Authentication", "Scenario: Fully wired up", List.of("an implemented step"));
        List<Expression> glueCode = List.of(expression("an implemented step"));

        File outputFile = tempDir.resolve("features.adoc").toFile();
        writer.write(outputFile, List.of(implemented), glueCode,
                new ProgressReportOptions(true, snippetDir, null, "1.0.0"));

        File listedFile = new File(snippetDir, "listed.adoc");
        File definedFile = new File(snippetDir, "defined.adoc");
        File implementedFile = new File(new File(snippetDir, "authentication"), "implemented.adoc");
        assertThat(listedFile).exists();
        assertThat(definedFile).exists();
        assertThat(implementedFile).exists();
        assertThat(Files.readString(listedFile.toPath(), StandardCharsets.UTF_8).trim()).isEqualTo("_None._");
        assertThat(Files.readString(implementedFile.toPath(), StandardCharsets.UTF_8))
                .contains("* Scenario: Fully wired up");
    }

    @Test
    @DisplayName("renders the report from a template instead of embedding scenario titles, when a template is set")
    void rendersReportFromTemplateWhenConfigured(@TempDir Path tempDir) throws IOException {
        File templateFile = tempDir.resolve("report.mustache").toFile();
        Files.writeString(templateFile.toPath(),
                "TEMPLATE OUTPUT\n{{#sections}}{{{status}}}: {{{snippet}}}\n{{/sections}}");
        ScenarioInfo implemented = new ScenarioInfo(
                "Authentication", "Scenario: Fully wired up", List.of("an implemented step"));
        List<Expression> glueCode = List.of(expression("an implemented step"));

        File outputFile = tempDir.resolve("features.adoc").toFile();
        writer.write(outputFile, List.of(implemented), glueCode,
                new ProgressReportOptions(false, tempDir.resolve("snippets").toFile(), templateFile, "1.0.0"));

        String content = Files.readString(outputFile.toPath(), StandardCharsets.UTF_8);
        assertThat(content).contains("TEMPLATE OUTPUT");
        assertThat(content).doesNotContain("Progress Summary");
        assertThat(content).contains("snippets/listed.adoc");
    }

    @Test
    @DisplayName("omits the Progress Over Time section when history is empty")
    void omitsProgressOverTimeSectionWhenHistoryIsEmpty(@TempDir Path tempDir) throws IOException {
        ScenarioInfo implemented = new ScenarioInfo(
                "Authentication", "Scenario: Fully wired up", List.of("an implemented step"));
        List<Expression> glueCode = List.of(expression("an implemented step"));

        File outputFile = tempDir.resolve("features.adoc").toFile();
        writer.write(outputFile, List.of(implemented), glueCode, grouped(tempDir));

        String content = Files.readString(outputFile.toPath(), StandardCharsets.UTF_8);
        assertThat(content).doesNotContain("Progress Over Time");
    }

    @Test
    @DisplayName("adds a Progress Over Time section after Progress Summary when history is non-empty")
    void addsProgressOverTimeSectionAfterProgressSummaryWhenHistoryIsNonEmpty(@TempDir Path tempDir)
            throws IOException {
        ScenarioInfo implemented = new ScenarioInfo(
                "Authentication", "Scenario: Fully wired up", List.of("an implemented step"));
        List<Expression> glueCode = List.of(expression("an implemented step"));
        Map<String, ScenarioProgressRecord> history = Map.of("fp1", new ScenarioProgressRecord(
                "fp1", "Fully wired up", "Authentication",
                Instant.parse("2026-01-01T00:00:00Z"), null, Instant.parse("2026-01-05T00:00:00Z"),
                Instant.parse("2026-01-05T00:00:00Z"), null));

        File outputFile = tempDir.resolve("features.adoc").toFile();
        writer.write(outputFile, List.of(implemented), glueCode,
                new ProgressReportOptions(true, tempDir.resolve("snippets").toFile(), null, "1.0.0", history));

        String content = Files.readString(outputFile.toPath(), StandardCharsets.UTF_8);
        assertThat(content.indexOf("Progress Summary")).isLessThan(content.indexOf("Progress Over Time"));
    }

    @Test
    @DisplayName("reports the earliest non-null timestamp across all records as Tracked since")
    void reportsEarliestTimestampAsTrackedSince(@TempDir Path tempDir) throws IOException {
        ScenarioInfo implemented = new ScenarioInfo(
                "Authentication", "Scenario: Fully wired up", List.of("an implemented step"));
        List<Expression> glueCode = List.of(expression("an implemented step"));
        Map<String, ScenarioProgressRecord> history = Map.of("fp1", new ScenarioProgressRecord(
                "fp1", "Fully wired up", "Authentication",
                Instant.parse("2026-01-14T09:02:11Z"), null, Instant.parse("2026-02-20T11:15:44Z"),
                Instant.parse("2026-02-20T11:15:44Z"), null));

        File outputFile = tempDir.resolve("features.adoc").toFile();
        writer.write(outputFile, List.of(implemented), glueCode,
                new ProgressReportOptions(true, tempDir.resolve("snippets").toFile(), null, "1.0.0", history));

        String content = Files.readString(outputFile.toPath(), StandardCharsets.UTF_8);
        assertThat(content).contains("2026-01-14 09:02:11 UTC");
    }

    @Test
    @DisplayName("counts records implemented within the last 7 days")
    void countsRecordsImplementedWithinLast7Days(@TempDir Path tempDir) throws IOException {
        ScenarioInfo implemented = new ScenarioInfo(
                "Authentication", "Scenario: Fully wired up", List.of("an implemented step"));
        List<Expression> glueCode = List.of(expression("an implemented step"));
        Instant recentlyImplemented = Instant.now().minus(Duration.ofDays(2));
        Instant longAgoImplemented = Instant.now().minus(Duration.ofDays(40));
        Map<String, ScenarioProgressRecord> history = Map.of(
                "fp1", new ScenarioProgressRecord(
                        "fp1", "A", "Authentication", recentlyImplemented, null, recentlyImplemented,
                        recentlyImplemented, null),
                "fp2", new ScenarioProgressRecord(
                        "fp2", "B", "Authentication", longAgoImplemented, null, longAgoImplemented,
                        longAgoImplemented, null));

        File outputFile = tempDir.resolve("features.adoc").toFile();
        writer.write(outputFile, List.of(implemented), glueCode,
                new ProgressReportOptions(true, tempDir.resolve("snippets").toFile(), null, "1.0.0", history));

        String content = Files.readString(outputFile.toPath(), StandardCharsets.UTF_8);
        assertThat(content).contains("| Implemented in the last 7 days" + System.lineSeparator() + "| 1");
    }

    @Test
    @DisplayName("counts records listed and defined within the last 7 and 30 days")
    void countsRecordsListedAndDefinedWithinLast7And30Days(@TempDir Path tempDir) throws IOException {
        ScenarioInfo implemented = new ScenarioInfo(
                "Authentication", "Scenario: Fully wired up", List.of("an implemented step"));
        List<Expression> glueCode = List.of(expression("an implemented step"));
        Instant recentlyListed = Instant.now().minus(Duration.ofDays(2));
        Instant recentlyDefined = Instant.now().minus(Duration.ofDays(10));
        Instant longAgo = Instant.now().minus(Duration.ofDays(40));
        Map<String, ScenarioProgressRecord> history = Map.of(
                "fp1", new ScenarioProgressRecord(
                        "fp1", "A", "Authentication", recentlyListed, recentlyDefined, null,
                        recentlyListed, null),
                "fp2", new ScenarioProgressRecord(
                        "fp2", "B", "Authentication", longAgo, longAgo, null,
                        longAgo, null));

        File outputFile = tempDir.resolve("features.adoc").toFile();
        writer.write(outputFile, List.of(implemented), glueCode,
                new ProgressReportOptions(true, tempDir.resolve("snippets").toFile(), null, "1.0.0", history));

        String content = Files.readString(outputFile.toPath(), StandardCharsets.UTF_8);
        assertThat(content)
                .contains("| Listed in the last 7 days" + System.lineSeparator() + "| 1")
                .contains("| Listed in the last 30 days" + System.lineSeparator() + "| 1")
                .contains("| Defined in the last 7 days" + System.lineSeparator() + "| 0")
                .contains("| Defined in the last 30 days" + System.lineSeparator() + "| 1");
    }

    @Test
    @DisplayName("explains why a scenario becomes removed, right before the Removed row")
    void explainsWhyAScenarioBecomesRemoved(@TempDir Path tempDir) throws IOException {
        ScenarioInfo implemented = new ScenarioInfo(
                "Authentication", "Scenario: Fully wired up", List.of("an implemented step"));
        List<Expression> glueCode = List.of(expression("an implemented step"));
        Map<String, ScenarioProgressRecord> history = Map.of("fp1", new ScenarioProgressRecord(
                "fp1", "Fully wired up", "Authentication",
                Instant.parse("2026-01-01T00:00:00Z"), null, Instant.parse("2026-01-05T00:00:00Z"),
                Instant.parse("2026-01-05T00:00:00Z"), null));

        File outputFile = tempDir.resolve("features.adoc").toFile();
        writer.write(outputFile, List.of(implemented), glueCode,
                new ProgressReportOptions(true, tempDir.resolve("snippets").toFile(), null, "1.0.0", history));

        String content = Files.readString(outputFile.toPath(), StandardCharsets.UTF_8);
        int progressOverTimeIndex = content.indexOf("Progress Over Time");
        int blurbIndex = content.indexOf(ReportText.REMOVED_BLURB);
        int removedRowIndex = content.indexOf("Removed (no longer seen)");
        assertThat(blurbIndex).isGreaterThan(progressOverTimeIndex);
        assertThat(removedRowIndex).isGreaterThan(blurbIndex);
        assertThat(content).contains("taken out of scope").contains("duplicate another scenario");
    }

    @Test
    @DisplayName("counts records with a non-null removedAt as removed")
    void countsRecordsWithNonNullRemovedAtAsRemoved(@TempDir Path tempDir) throws IOException {
        ScenarioInfo implemented = new ScenarioInfo(
                "Authentication", "Scenario: Fully wired up", List.of("an implemented step"));
        List<Expression> glueCode = List.of(expression("an implemented step"));
        Map<String, ScenarioProgressRecord> history = Map.of(
                "fp1", new ScenarioProgressRecord(
                        "fp1", "A", "Authentication", Instant.parse("2026-01-01T00:00:00Z"), null, null,
                        Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-02-01T00:00:00Z")),
                "fp2", new ScenarioProgressRecord(
                        "fp2", "B", "Authentication", Instant.parse("2026-01-01T00:00:00Z"), null, null,
                        Instant.parse("2026-01-01T00:00:00Z"), null));

        File outputFile = tempDir.resolve("features.adoc").toFile();
        writer.write(outputFile, List.of(implemented), glueCode,
                new ProgressReportOptions(true, tempDir.resolve("snippets").toFile(), null, "1.0.0", history));

        String content = Files.readString(outputFile.toPath(), StandardCharsets.UTF_8);
        assertThat(content).contains("| Removed (no longer seen)" + System.lineSeparator() + "| 1");
    }

    @Test
    @DisplayName("throws a GradleException when the output file cannot be written")
    void throwsWhenOutputFileCannotBeWritten(@TempDir Path tempDir) {
        File directoryAsFile = tempDir.toFile();

        assertThatThrownBy(() -> writer.write(directoryAsFile, List.of(), List.of(), grouped(tempDir)))
                .isInstanceOf(org.gradle.api.GradleException.class);
    }

    private Expression expression(String pattern) {
        return expressionFactory.createExpression(pattern);
    }

    private ProgressReportOptions grouped(Path tempDir) {
        return new ProgressReportOptions(true, tempDir.resolve("snippets").toFile(), null, "1.0.0");
    }

    private ProgressReportOptions flat(Path tempDir) {
        return new ProgressReportOptions(false, tempDir.resolve("snippets").toFile(), null, "1.0.0");
    }
}

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
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the two reference Mustache templates documented in the plugin README
 * ({@code templates/grouped.mustache} and {@code templates/flat.mustache}) reproduce, via
 * {@code include::} directives, exactly the same headings, blurbs, summary table, and per-status
 * scenario listings as the built-in default report (no template configured).
 */
@DisplayName("Default-equivalent reference templates")
class DefaultEquivalentTemplatesTest {

    private static final Pattern INCLUDE = Pattern.compile("include::(.+)\\[]");

    private final ProgressReportWriter writer = new ProgressReportWriter();
    private final ExpressionFactory expressionFactory =
            new ExpressionFactory(new ParameterTypeRegistry(Locale.ENGLISH));

    @Test
    @DisplayName("grouped.mustache reproduces the default grouped report")
    void groupedTemplateReproducesDefaultReport(@TempDir Path tempDir) throws IOException, URISyntaxException {
        List<ScenarioInfo> scenarios = List.of(
                new ScenarioInfo("Authentication", "Scenario: Only a title", List.of()),
                new ScenarioInfo("Authentication", "Scenario: Has steps, no glue", List.of("an unimplemented step")),
                new ScenarioInfo("Authentication", "Scenario: Fully wired up", List.of("an implemented step")),
                new ScenarioInfo("Billing", "Scenario: Pays an invoice", List.of("an implemented step")));
        List<Expression> glueCode = List.of(expression("an implemented step"));

        assertTemplateReproducesDefault(tempDir, scenarios, glueCode, true, "templates/grouped.mustache");
    }

    @Test
    @DisplayName("flat.mustache reproduces the default flat (non-grouped) report")
    void flatTemplateReproducesDefaultReport(@TempDir Path tempDir) throws IOException, URISyntaxException {
        List<ScenarioInfo> scenarios = List.of(
                new ScenarioInfo("Authentication", "Scenario: Only a title", List.of()),
                new ScenarioInfo("Authentication", "Scenario: Has steps, no glue", List.of("an unimplemented step")),
                new ScenarioInfo("Billing", "Scenario: Pays an invoice", List.of("an implemented step")));
        List<Expression> glueCode = List.of(expression("an implemented step"));

        assertTemplateReproducesDefault(tempDir, scenarios, glueCode, false, "templates/flat.mustache");
    }

    private void assertTemplateReproducesDefault(
            Path tempDir, List<ScenarioInfo> scenarios, List<Expression> glueCode, boolean groupByFeature,
            String templateResource) throws IOException, URISyntaxException {
        File defaultOutput = tempDir.resolve("default.adoc").toFile();
        File defaultSnippets = tempDir.resolve("default-snippets").toFile();
        writer.write(defaultOutput, scenarios, glueCode,
                new ProgressReportOptions(groupByFeature, defaultSnippets, null, "1.0.0"));
        String defaultContent = Files.readString(defaultOutput.toPath(), StandardCharsets.UTF_8);

        File templatedOutput = tempDir.resolve("templated.adoc").toFile();
        File templatedSnippets = tempDir.resolve("templated-snippets").toFile();
        File template = fixtureFile(templateResource);
        writer.write(templatedOutput, scenarios, glueCode,
                new ProgressReportOptions(groupByFeature, templatedSnippets, template, "1.0.0"));
        String templatedContent = Files.readString(templatedOutput.toPath(), StandardCharsets.UTF_8);

        // The legend and summary table are embedded verbatim by both, so they must be byte-identical.
        assertThat(extractBetween(templatedContent, "Every scenario is classified", "== Progress Summary"))
                .isEqualTo(extractBetween(defaultContent, "Every scenario is classified", "== Progress Summary"));
        assertThat(extractBetween(templatedContent, "== Progress Summary", "== Listed"))
                .isEqualTo(extractBetween(defaultContent, "== Progress Summary", "== Listed"));

        // Resolving every include:: directive (as an AsciiDoc processor would) must reproduce
        // exactly the same document as the default (no-template) report.
        String resolved = resolveIncludes(templatedContent, templatedOutput.getParentFile());
        assertThat(resolved).isEqualTo(defaultContent);

        assertThat(defaultContent).contains("* Scenario:");
        assertThat(templatedContent).doesNotContain("* Scenario:").contains("include::");
    }

    private String resolveIncludes(String content, File baseDir) throws IOException {
        StringBuilder result = new StringBuilder();
        for (String line : content.lines().toList()) {
            Matcher matcher = INCLUDE.matcher(line.trim());
            if (matcher.matches()) {
                File snippetFile = new File(baseDir, matcher.group(1));
                result.append(Files.readString(snippetFile.toPath(), StandardCharsets.UTF_8));
            } else {
                result.append(line).append("\n");
            }
        }
        return result.toString();
    }

    private String extractBetween(String content, String start, String end) {
        int startIndex = content.indexOf(start);
        int endIndex = content.indexOf(end, startIndex);
        return content.substring(startIndex, endIndex);
    }

    private Expression expression(String pattern) {
        return expressionFactory.createExpression(pattern);
    }

    private File fixtureFile(String resourcePath) throws URISyntaxException {
        URL resource = getClass().getClassLoader().getResource(resourcePath);
        if (resource == null) {
            throw new IllegalArgumentException("Fixture not found on classpath: " + resourcePath);
        }
        return new File(resource.toURI());
    }
}

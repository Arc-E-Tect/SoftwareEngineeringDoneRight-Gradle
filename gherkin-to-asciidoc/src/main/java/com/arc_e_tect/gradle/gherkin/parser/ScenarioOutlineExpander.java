package com.arc_e_tect.gradle.gherkin.parser;

import io.cucumber.messages.types.Examples;
import io.cucumber.messages.types.Scenario;
import io.cucumber.messages.types.Step;
import io.cucumber.messages.types.TableCell;
import io.cucumber.messages.types.TableRow;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Expands a {@code Scenario Outline} into one {@link ScenarioInfo} per {@code Examples} row - the
 * same thing a {@code Scenario Outline} is to Cucumber itself, which runs one test case per row
 * rather than one per outline.
 *
 * <p>Every {@code <placeholder>} in the row's steps is replaced with that row's value for the
 * matching {@code Examples} column, so each expanded scenario carries the concrete steps that row
 * actually runs. That's what {@link com.arc_e_tect.gradle.gherkin.progress.ScenarioClassifier}
 * needs to classify an outline honestly: a step still reading {@code the user submits "<username>"}
 * matches no Cucumber expression that the real, substituted step matches, so before expansion an
 * outline whose glue code was fully written could never be reported as {@code implemented}.</p>
 *
 * <p>A row's title is its outline's name with the same substitution applied - matching how Cucumber
 * names the test case it reports for that row, so the two reports line up name-for-name. Two rows
 * of one outline can still end up with the same name that way (the outline's name may use no
 * placeholder at all, or not the ones that vary between rows), which
 * {@link com.arc_e_tect.gradle.gherkin.progress.ScenarioFingerprint} - identifying a scenario by
 * its title alone - could not tell apart. So the first of these three forms that gives every row of
 * the outline a distinct name is used for all of that outline's rows:</p>
 *
 * <ol>
 *   <li>the substituted name, e.g. {@code User logs in as alice};</li>
 *   <li>the substituted name plus the row's own column/value pairs, e.g.
 *       {@code User logs in [username: alice, outcome: success]};</li>
 *   <li>the substituted name plus the row's 1-based position among the outline's rows, e.g.
 *       {@code User logs in [example 2]} - reached only when two rows hold literally the same
 *       values, where the pairs in form 2 would be identical too.</li>
 * </ol>
 *
 * <p>Every expanded row also carries the outline's own unsubstituted title in
 * {@link ScenarioInfo#outlineTitle()} - the title the row used to be reported under, before outlines
 * were expanded - so that
 * {@link com.arc_e_tect.gradle.gherkin.progress.ReinterpretedOutlines} can tell a progress history
 * record superseded by this expansion apart from one whose scenario was genuinely edited away.</p>
 *
 * <p>An outline with no {@code Examples} rows at all - no {@code Examples} block yet, or one with
 * only a header - expands to a single {@code ScenarioInfo} carrying the outline's own name and its
 * unsubstituted steps, exactly as a plain {@code Scenario} would. Expanding it to nothing would
 * drop a scenario that's merely still being authored out of the report entirely, and with it
 * whatever progress history it has already accrued.</p>
 */
final class ScenarioOutlineExpander {

    private static final Pattern PLACEHOLDER = Pattern.compile("<([^<>]+)>");

    private ScenarioOutlineExpander() {}

    /**
     * Expands {@code scenario} into one {@link ScenarioInfo} per {@code Examples} row.
     *
     * @param featureTitle the name of the enclosing {@code Feature}, copied onto every returned
     *                     {@code ScenarioInfo}
     * @param scenario     the scenario to expand; a plain {@code Scenario} (one with no
     *                     {@code Examples}) yields exactly one {@code ScenarioInfo}, unchanged
     * @return the expanded scenarios, in {@code Examples} row order; never empty
     */
    static List<ScenarioInfo> expand(String featureTitle, Scenario scenario) {
        List<Map<String, String>> rows = exampleRows(scenario);
        if (rows.isEmpty()) {
            return List.of(toScenarioInfo(featureTitle, scenario, scenario.getName(), Map.of(), null));
        }

        String outlineTitle = title(scenario, scenario.getName());
        List<String> names = distinctNames(scenario.getName(), rows);
        List<ScenarioInfo> expanded = new ArrayList<>(rows.size());
        for (int row = 0; row < rows.size(); row++) {
            expanded.add(toScenarioInfo(featureTitle, scenario, names.get(row), rows.get(row), outlineTitle));
        }
        return expanded;
    }

    /**
     * Names every row of one outline, using the first of the three forms documented on this class
     * that leaves no two rows sharing a name.
     */
    private static List<String> distinctNames(String outlineName, List<Map<String, String>> rows) {
        List<String> substituted = new ArrayList<>(rows.size());
        for (Map<String, String> row : rows) {
            substituted.add(substitute(outlineName, row));
        }
        if (allDistinct(substituted)) {
            return substituted;
        }

        List<String> withValues = qualify(substituted, row -> valueSuffix(rows.get(row)));
        if (allDistinct(withValues)) {
            return withValues;
        }
        return qualify(substituted, row -> " [example " + (row + 1) + "]");
    }

    private static List<String> qualify(List<String> names, IntFunction<String> suffix) {
        List<String> qualified = new ArrayList<>(names.size());
        for (int row = 0; row < names.size(); row++) {
            qualified.add(names.get(row) + suffix.apply(row));
        }
        return qualified;
    }

    private static boolean allDistinct(List<String> names) {
        Set<String> seen = new HashSet<>(names);
        return seen.size() == names.size();
    }

    private static String valueSuffix(Map<String, String> row) {
        StringBuilder suffix = new StringBuilder(" [");
        String separator = "";
        for (Map.Entry<String, String> cell : row.entrySet()) {
            suffix.append(separator).append(cell.getKey()).append(": ").append(cell.getValue());
            separator = ", ";
        }
        return suffix.append("]").toString();
    }

    /**
     * Collects every {@code Examples} row of {@code scenario}, across all of its {@code Examples}
     * blocks, in document order - each row as an ordered map from its block's column name to that
     * row's value for the column. A block with no header, or a row with fewer cells than its
     * header has columns, contributes only the cells it does have; extra cells beyond the header's
     * columns are ignored, having no column name to be addressed by.
     */
    private static List<Map<String, String>> exampleRows(Scenario scenario) {
        List<Map<String, String>> rows = new ArrayList<>();
        for (Examples examples : scenario.getExamples()) {
            List<String> columns = examples.getTableHeader()
                    .map(ScenarioOutlineExpander::cellValues)
                    .orElse(List.of());
            for (TableRow tableRow : examples.getTableBody()) {
                List<String> values = cellValues(tableRow);
                Map<String, String> row = new LinkedHashMap<>();
                for (int column = 0; column < Math.min(columns.size(), values.size()); column++) {
                    row.put(columns.get(column), values.get(column));
                }
                rows.add(row);
            }
        }
        return rows;
    }

    private static List<String> cellValues(TableRow row) {
        return row.getCells().stream().map(TableCell::getValue).toList();
    }

    private static ScenarioInfo toScenarioInfo(
            String featureTitle, Scenario scenario, String name, Map<String, String> row, String outlineTitle) {
        List<String> steps = new ArrayList<>(scenario.getSteps().size());
        for (Step step : scenario.getSteps()) {
            steps.add(substitute(step.getText(), row));
        }
        return new ScenarioInfo(featureTitle, title(scenario, name), steps, outlineTitle);
    }

    private static String title(Scenario scenario, String name) {
        return scenario.getKeyword().trim() + ": " + name;
    }

    /**
     * Replaces every {@code <column>} in {@code text} with {@code row}'s value for that column, in
     * a single left-to-right pass - so a value that itself looks like a placeholder is never
     * substituted again. A {@code <placeholder>} naming a column the row doesn't have is left
     * exactly as written, the same as Cucumber leaves it.
     */
    private static String substitute(String text, Map<String, String> row) {
        if (row.isEmpty()) {
            return text;
        }
        Matcher matcher = PLACEHOLDER.matcher(text);
        StringBuilder substituted = new StringBuilder();
        while (matcher.find()) {
            String value = row.get(matcher.group(1));
            matcher.appendReplacement(
                    substituted, Matcher.quoteReplacement(value != null ? value : matcher.group()));
        }
        matcher.appendTail(substituted);
        return substituted.toString();
    }
}

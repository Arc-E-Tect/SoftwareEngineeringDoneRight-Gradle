package com.arc_e_tect.gradle.gherkin.parser;

import io.cucumber.gherkin.GherkinParser;
import io.cucumber.messages.types.Envelope;
import io.cucumber.messages.types.Feature;
import io.cucumber.messages.types.FeatureChild;
import io.cucumber.messages.types.GherkinDocument;
import io.cucumber.messages.types.Rule;
import io.cucumber.messages.types.RuleChild;
import io.cucumber.messages.types.Scenario;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * Parses a single Gherkin {@code .feature} file and extracts all scenario titles.
 *
 * <p>Titles are returned in document order as {@code "keyword: name"} strings
 * (e.g. {@code "Scenario: User logs in"}).  Scenarios nested inside {@code Rule}
 * blocks are also included.</p>
 *
 * <p>If the file cannot be read or parsed, the error is logged as a warning and
 * an empty list is returned — the task never fails due to a single bad file.</p>
 */
public class FeatureParser {

    private static final Logger LOGGER = Logging.getLogger(FeatureParser.class);

    private final GherkinParser parser;

    /**
     * Creates a new {@code FeatureParser} with a pre-configured {@link GherkinParser}.
     * Source envelopes and pickles are excluded from parsing to minimise memory usage.
     */
    public FeatureParser() {
        this.parser = GherkinParser.builder()
                .includeSource(false)
                .includePickles(false)
                .build();
    }

    /**
     * Parses the given {@code .feature} file and returns all scenario titles.
     *
     * @param featureFile the Gherkin feature file to parse; must not be {@code null}
     * @return unmodifiable list of scenario titles in document order; empty if the file
     *         cannot be read, is empty, or contains no scenarios
     */
    public List<String> parse(File featureFile) {
        List<String> titles = new ArrayList<>();
        try (Stream<Envelope> envelopes = parser.parse(featureFile.toPath())) {
            envelopes
                    .filter(e -> e.getGherkinDocument().isPresent())
                    .findFirst()
                    .flatMap(Envelope::getGherkinDocument)
                    .flatMap(GherkinDocument::getFeature)
                    .ifPresent(feature -> extractScenarios(feature, titles));
        } catch (IOException e) {
            LOGGER.warn("Could not read feature file '{}': {}", featureFile, e.getMessage());
        } catch (Exception e) {
            LOGGER.warn("Could not parse feature file '{}': {}", featureFile, e.getMessage());
        }
        return Collections.unmodifiableList(titles);
    }

    private void extractScenarios(Feature feature, List<String> titles) {
        for (FeatureChild child : feature.getChildren()) {
            child.getScenario().ifPresent(s -> titles.add(formatTitle(s)));
            child.getRule().ifPresent(rule -> extractFromRule(rule, titles));
        }
    }

    private void extractFromRule(Rule rule, List<String> titles) {
        for (RuleChild ruleChild : rule.getChildren()) {
            ruleChild.getScenario().ifPresent(s -> titles.add(formatTitle(s)));
        }
    }

    private String formatTitle(Scenario scenario) {
        return scenario.getKeyword().trim() + ": " + scenario.getName();
    }
}

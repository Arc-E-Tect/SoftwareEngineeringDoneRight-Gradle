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

public class FeatureParser {

    private static final Logger LOGGER = Logging.getLogger(FeatureParser.class);

    private final GherkinParser parser;

    public FeatureParser() {
        this.parser = GherkinParser.builder()
                .includeSource(false)
                .includePickles(false)
                .build();
    }

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

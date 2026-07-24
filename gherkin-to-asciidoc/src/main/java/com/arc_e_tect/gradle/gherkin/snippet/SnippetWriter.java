package com.arc_e_tect.gradle.gherkin.snippet;

import com.arc_e_tect.gradle.gherkin.parser.ScenarioGrouping;
import com.arc_e_tect.gradle.gherkin.parser.ScenarioInfo;
import com.arc_e_tect.gradle.gherkin.progress.ScenarioStatus;
import org.gradle.api.GradleException;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Writes the {@code listed.adoc}/{@code defined.adoc}/{@code implemented.adoc} snippet files that
 * back a progress report's {@code include::} directives.
 */
public class SnippetWriter {

    /** Creates a new {@code SnippetWriter}. */
    public SnippetWriter() {}

    /**
     * Writes the snippet file(s) for a single status.
     *
     * <p>When {@code groupByFeature} is {@code true} and {@code scenarios} is non-empty, one file is
     * written per feature, under {@code <snippetDir>/<camelCaseFeatureTitle>/<status>.adoc}. Otherwise
     * a single flat file is written at {@code <snippetDir>/<status>.adoc}, containing a bullet per
     * scenario, or {@code _None._} when {@code scenarios} is empty.</p>
     *
     * @param snippetDir     the configured snippet output directory
     * @param status         the status these scenarios were classified as
     * @param scenarios      the scenarios classified as {@code status}
     * @param groupByFeature whether to split the snippet by feature
     * @return a description of the snippet file(s) written
     */
    public StatusSnippets writeStatus(
            File snippetDir, ScenarioStatus status, List<ScenarioInfo> scenarios, boolean groupByFeature) {
        String fileName = status.name().toLowerCase(Locale.ROOT) + ".adoc";

        if (groupByFeature && !scenarios.isEmpty()) {
            List<FeatureSnippet> features = new ArrayList<>();
            for (Map.Entry<String, List<ScenarioInfo>> entry : ScenarioGrouping.byFeatureTitle(scenarios).entrySet()) {
                String directoryName = FeatureNameFormatter.toDirectoryName(entry.getKey());
                File file = new File(new File(snippetDir, directoryName), fileName);
                writeLines(file, entry.getValue());
                features.add(new FeatureSnippet(entry.getKey(), file));
            }
            return new StatusSnippets(features, null);
        }

        File file = new File(snippetDir, fileName);
        writeLines(file, scenarios);
        return new StatusSnippets(List.of(), file);
    }

    private void writeLines(File file, List<ScenarioInfo> scenarios) {
        File parent = file.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new GradleException("gherkinToAsciidoc: could not create snippet directory: " + parent);
        }
        try (PrintWriter writer = new PrintWriter(file, StandardCharsets.UTF_8)) {
            if (scenarios.isEmpty()) {
                writer.println("_None._");
            } else {
                for (ScenarioInfo scenario : scenarios) {
                    writer.println("* " + scenario.title());
                }
            }
        } catch (IOException e) {
            throw new GradleException("gherkinToAsciidoc: failed to write snippet file: " + file, e);
        }
    }
}

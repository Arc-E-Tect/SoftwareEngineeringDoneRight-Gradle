package com.arc_e_tect.gradle.gherkin.indexing;

import org.gradle.api.GradleException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Numbers {@code Feature}/{@code Scenario} titles directly in the source {@code .feature} files
 * according to a configured {@link IndexingMode}.
 *
 * <p>Every run first strips any numbering left over from a previous run (recognised by the
 * {@code <number> - } prefix this class itself adds), then - unless the mode is
 * {@link IndexingMode#OFF} - applies fresh numbering. This makes the operation idempotent and
 * makes switching between modes (including back to {@code OFF}) simply undo the previous
 * numbering rather than requiring any state to be tracked between runs.</p>
 *
 * <p>Never called with {@link IndexingMode#CI}: the caller skips invoking this class entirely for
 * that mode, since {@code CI} means the feature files must be left completely untouched, not even
 * to strip prior numbering the way {@link IndexingMode#OFF} does.</p>
 */
public class FeatureIndexer {

    private static final Pattern KEYWORD_LINE =
            Pattern.compile("^(\\s*)(Feature|Scenario Outline|Scenario):(\\s*)(.*)$");
    private static final Pattern EXISTING_INDEX = Pattern.compile("^\\d+(?:\\.\\d+)? - (.*)$");

    /** Creates a new {@code FeatureIndexer}. */
    public FeatureIndexer() {}

    /**
     * Rewrites every file in {@code featureFiles} in place: strips any {@code Feature}/
     * {@code Scenario} numbering added by a previous run, then applies numbering per
     * {@code mode}. Files are numbered in the order they appear in {@code featureFiles} - the
     * caller is responsible for ordering that list the way numbers should be assigned. A file is
     * only rewritten on disk when its content actually changes.
     *
     * @param featureFiles the feature files collected for this run, in the order to number them in
     * @param mode         the indexing mode to apply
     */
    public void reindex(List<File> featureFiles, IndexingMode mode) {
        int featureNumber = 0;
        int scenarioNumber = 0;
        for (File featureFile : featureFiles) {
            featureNumber++;
            int scenarioInFeature = 0;
            List<String> lines = readLines(featureFile);
            List<String> rewritten = new ArrayList<>(lines.size());
            boolean changed = false;

            for (String line : lines) {
                Matcher matcher = KEYWORD_LINE.matcher(line);
                if (!matcher.matches()) {
                    rewritten.add(line);
                    continue;
                }

                String indent = matcher.group(1);
                String keyword = matcher.group(2);
                String gap = matcher.group(3);
                String name = stripExistingIndex(matcher.group(4));

                String newName;
                if ("Feature".equals(keyword)) {
                    newName = (mode == IndexingMode.FEATURE || mode == IndexingMode.ALL)
                            ? featureNumber + " - " + name
                            : name;
                } else if (mode == IndexingMode.SCENARIO) {
                    scenarioNumber++;
                    newName = scenarioNumber + " - " + name;
                } else if (mode == IndexingMode.ALL) {
                    scenarioInFeature++;
                    newName = featureNumber + "." + scenarioInFeature + " - " + name;
                } else {
                    newName = name;
                }

                String newLine = indent + keyword + ":" + gap + newName;
                changed |= !newLine.equals(line);
                rewritten.add(newLine);
            }

            if (changed) {
                writeLines(featureFile, rewritten);
            }
        }
    }

    private String stripExistingIndex(String name) {
        Matcher matcher = EXISTING_INDEX.matcher(name);
        return matcher.matches() ? matcher.group(1) : name;
    }

    private List<String> readLines(File file) {
        try {
            return Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new GradleException("gherkinToAsciidoc: could not read feature file: " + file, e);
        }
    }

    private void writeLines(File file, List<String> lines) {
        try {
            Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new GradleException("gherkinToAsciidoc: could not update feature file: " + file, e);
        }
    }
}

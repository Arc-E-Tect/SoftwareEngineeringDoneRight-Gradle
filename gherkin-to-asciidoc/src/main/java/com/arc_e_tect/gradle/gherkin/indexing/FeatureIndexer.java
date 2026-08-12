package com.arc_e_tect.gradle.gherkin.indexing;

import org.gradle.api.GradleException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Numbers {@code Feature}/{@code Scenario} titles directly in the source {@code .feature} files
 * according to a configured {@link IndexingMode}.
 *
 * <p>A line whose existing number already matches the format {@code mode} would itself produce -
 * a single integer for a {@code Feature}, or for a {@code Scenario}/{@code Scenario Outline}
 * either a single integer ({@link IndexingMode#SCENARIO}) or {@code <featureNumber>.<n>}
 * ({@link IndexingMode#ALL}, matched against that line's own feature's resolved number) - is left
 * completely untouched: its number is "pinned". Every other numbered line (wrong format, or a
 * leftover from a different mode, or {@link IndexingMode#OFF} itself never expecting a number at
 * all) is stripped and, if {@code mode} numbers lines of that kind, assigned a fresh number one
 * past the highest pinned number seen so far (or from 1, if none are pinned yet) - so a newly
 * added file that happens to sort alphabetically before already-numbered files never bumps their
 * numbers, and fresh numbers always read as a continuation of the existing sequence rather than
 * backfilling a gap earlier in it. Passing {@code forceRewrite = true} instead ignores existing
 * numbers entirely and renumbers everything from scratch, exactly as if every file were being
 * numbered for the first time.</p>
 *
 * <p>Never called with {@link IndexingMode#CI}: the caller skips invoking this class entirely for
 * that mode, since {@code CI} means the feature files must be left completely untouched, not even
 * to strip prior numbering the way {@link IndexingMode#OFF} does.</p>
 */
public class FeatureIndexer {

    private static final Pattern KEYWORD_LINE =
            Pattern.compile("^(\\s*)(Feature|Scenario Outline|Scenario):(\\s*)(.*)$");
    private static final Pattern EXISTING_INDEX = Pattern.compile("^\\d+(?:\\.\\d+)? - (.*)$");
    private static final Pattern SINGLE_INDEX = Pattern.compile("^(\\d+) - (.*)$");

    /** Creates a new {@code FeatureIndexer}. */
    public FeatureIndexer() {}

    /**
     * Rewrites every file in {@code featureFiles} in place per {@code mode} and
     * {@code forceRewrite} - see the class documentation for exactly what changes and what's left
     * alone. Files are numbered in the order they appear in {@code featureFiles} - the caller is
     * responsible for ordering that list the way numbers should be assigned. A file is only
     * rewritten on disk when its content actually changes. Equivalent to
     * {@link #reindex(List, IndexingMode, boolean, Runnable)} with a callback that does nothing.
     *
     * @param featureFiles the feature files collected for this run, in the order to number them in
     * @param mode         the indexing mode to apply
     * @param forceRewrite when {@code true}, ignores existing numbers and renumbers everything from
     *                     scratch; when {@code false}, leaves already-correctly-numbered lines alone
     */
    public void reindex(List<File> featureFiles, IndexingMode mode, boolean forceRewrite) {
        reindex(featureFiles, mode, forceRewrite, () -> { });
    }

    /**
     * Same as {@link #reindex(List, IndexingMode, boolean)}, additionally invoking
     * {@code onFileReindexed} once for every file in {@code featureFiles} as it finishes being
     * reindexed, so a caller can drive a progress indicator during what would otherwise be a
     * single opaque, potentially long-running call.
     *
     * @param featureFiles    the feature files collected for this run, in the order to number them in
     * @param mode            the indexing mode to apply
     * @param forceRewrite    when {@code true}, ignores existing numbers and renumbers everything
     *                        from scratch; when {@code false}, leaves already-correctly-numbered
     *                        lines alone
     * @param onFileReindexed invoked once per file in {@code featureFiles}, in order; never
     *                        {@code null}
     */
    public void reindex(List<File> featureFiles, IndexingMode mode, boolean forceRewrite, Runnable onFileReindexed) {
        List<ParsedFile> parsedFiles = new ArrayList<>();
        for (File featureFile : featureFiles) {
            parsedFiles.add(parse(featureFile));
        }

        boolean numberFeatures = mode == IndexingMode.FEATURE || mode == IndexingMode.ALL;
        if (numberFeatures) {
            List<LineMatch> allFeatures = new ArrayList<>();
            for (ParsedFile parsedFile : parsedFiles) {
                allFeatures.addAll(parsedFile.featureMatches);
            }
            resolveSequential(allFeatures, forceRewrite, SINGLE_INDEX);
        }

        if (mode == IndexingMode.SCENARIO) {
            List<LineMatch> allScenarios = new ArrayList<>();
            for (ParsedFile parsedFile : parsedFiles) {
                allScenarios.addAll(parsedFile.scenarioMatches);
            }
            resolveSequential(allScenarios, forceRewrite, SINGLE_INDEX);
        } else if (mode == IndexingMode.ALL) {
            for (ParsedFile parsedFile : parsedFiles) {
                if (parsedFile.featureMatches.isEmpty()) {
                    continue;
                }
                Integer featureNumber = parsedFile.featureMatches.get(0).number;
                Pattern perFeatureIndex = Pattern.compile("^" + featureNumber + "\\.(\\d+) - (.*)$");
                resolveSequential(parsedFile.scenarioMatches, forceRewrite, perFeatureIndex);
            }
        }

        for (ParsedFile parsedFile : parsedFiles) {
            applyAndWrite(parsedFile, mode);
            onFileReindexed.run();
        }
    }

    /**
     * Determines the number for every entry in {@code matches}: entries whose {@link
     * LineMatch#rawName} already matches {@code pinPattern} (and {@code forceRewrite} is
     * {@code false}) keep that number ("pinned"); every other entry is assigned a fresh number,
     * counting up from one past the highest pinned number (or from 1, if none are pinned), in list
     * order. Fresh numbers are never lower than an already-pinned one, so they read as a
     * continuation of the existing sequence rather than backfilling a gap earlier in it.
     */
    private void resolveSequential(List<LineMatch> matches, boolean forceRewrite, Pattern pinPattern) {
        Set<Integer> taken = new HashSet<>();
        if (!forceRewrite) {
            for (LineMatch match : matches) {
                Matcher matcher = pinPattern.matcher(match.rawName);
                if (matcher.matches()) {
                    match.number = Integer.parseInt(matcher.group(1));
                    taken.add(match.number);
                }
            }
        }
        int next = taken.isEmpty() ? 1 : Collections.max(taken) + 1;
        for (LineMatch match : matches) {
            if (match.number != null) {
                continue;
            }
            while (taken.contains(next)) {
                next++;
            }
            match.number = next;
            taken.add(next);
            next++;
        }
    }

    private ParsedFile parse(File file) {
        List<String> lines = readLines(file);
        ParsedFile parsedFile = new ParsedFile(file, new ArrayList<>(lines));
        for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
            Matcher matcher = KEYWORD_LINE.matcher(lines.get(lineIndex));
            if (!matcher.matches()) {
                continue;
            }
            LineMatch match = new LineMatch(
                    lineIndex, matcher.group(1), matcher.group(2), matcher.group(3), matcher.group(4));
            if ("Feature".equals(match.keyword)) {
                parsedFile.featureMatches.add(match);
            } else {
                parsedFile.scenarioMatches.add(match);
            }
        }
        return parsedFile;
    }

    private void applyAndWrite(ParsedFile parsedFile, IndexingMode mode) {
        boolean changed = false;
        for (LineMatch match : parsedFile.featureMatches) {
            String cleanName = stripExistingIndex(match.rawName);
            String newName = match.number != null ? match.number + " - " + cleanName : cleanName;
            changed |= applyLine(parsedFile.lines, match, newName);
        }

        Integer featureNumber = parsedFile.featureMatches.isEmpty()
                ? null : parsedFile.featureMatches.get(0).number;
        for (LineMatch match : parsedFile.scenarioMatches) {
            String cleanName = stripExistingIndex(match.rawName);
            String newName;
            if (mode == IndexingMode.SCENARIO && match.number != null) {
                newName = match.number + " - " + cleanName;
            } else if (mode == IndexingMode.ALL && match.number != null && featureNumber != null) {
                newName = featureNumber + "." + match.number + " - " + cleanName;
            } else {
                newName = cleanName;
            }
            changed |= applyLine(parsedFile.lines, match, newName);
        }

        if (changed) {
            writeLines(parsedFile.file, parsedFile.lines);
        }
    }

    private boolean applyLine(List<String> lines, LineMatch match, String newName) {
        String newLine = match.indent + match.keyword + ":" + match.gap + newName;
        String oldLine = lines.get(match.lineIndex);
        if (newLine.equals(oldLine)) {
            return false;
        }
        lines.set(match.lineIndex, newLine);
        return true;
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

    /** A single file's lines, plus its parsed {@code Feature}/{@code Scenario} keyword lines. */
    private static final class ParsedFile {
        final File file;
        final List<String> lines;
        final List<LineMatch> featureMatches = new ArrayList<>();
        final List<LineMatch> scenarioMatches = new ArrayList<>();

        ParsedFile(File file, List<String> lines) {
            this.file = file;
            this.lines = lines;
        }
    }

    /**
     * A single parsed {@code Feature}/{@code Scenario}/{@code Scenario Outline} line. Deliberately
     * not a record: instances are used as mutable carriers for the resolved {@link #number}, and
     * must never be treated as equal to another instance with coincidentally identical field
     * values (e.g. two different files' first scenario, both unnumbered) - only the default
     * identity-based {@link Object#equals(Object)} is safe here.
     */
    private static final class LineMatch {
        final int lineIndex;
        final String indent;
        final String keyword;
        final String gap;
        final String rawName;
        Integer number;

        LineMatch(int lineIndex, String indent, String keyword, String gap, String rawName) {
            this.lineIndex = lineIndex;
            this.indent = indent;
            this.keyword = keyword;
            this.gap = gap;
            this.rawName = rawName;
        }
    }
}

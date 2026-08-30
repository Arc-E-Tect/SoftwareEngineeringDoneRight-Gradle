package com.arc_e_tect.gradle.gherkin.indexing;

import org.gradle.api.GradleException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
 * <p>If two lines' existing numbers collide - e.g. two features or two scenarios within the same
 * feature were each numbered independently on separate branches and now coexist after a merge -
 * only the first one encountered keeps the pinned number; the other is treated as unnumbered and
 * assigned a fresh one, so the collision is resolved instead of leaving both lines untouched.</p>
 *
 * <p>{@code Feature} numbering (and, for {@link IndexingMode#SCENARIO}, {@code Scenario} numbering
 * too) is additionally scoped by {@code projectDirectories} when given a non-empty list: each
 * feature file is assigned to whichever directory in that list is its nearest enclosing ancestor,
 * and every such group is numbered as its own independent 1-based sequence, completely unaware of
 * every other group's numbers - so, in a multi-project Gradle build, passing every project's own
 * directory numbers each project's features from 1 rather than continuing one build-wide count
 * across all of them. An empty list (the default for the overloads that don't accept one) instead
 * treats {@code featureFiles} as a single group, exactly as if every file belonged to the same
 * project - today's only behaviour, and the one still used for a build-wide consolidated count.
 * {@link IndexingMode#ALL}'s {@code Scenario} numbering is unaffected either way: it's already
 * scoped per {@code Feature} - strictly finer-grained than per-project - by resetting to 1 within
 * every feature regardless of {@code projectDirectories}.</p>
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
        reindex(featureFiles, mode, forceRewrite, List.of(), () -> { });
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
        reindex(featureFiles, mode, forceRewrite, List.of(), onFileReindexed);
    }

    /**
     * Same as {@link #reindex(List, IndexingMode, boolean, Runnable)}, additionally scoping
     * {@code Feature}/cross-file {@code Scenario} numbering to each of {@code projectDirectories} -
     * see the class documentation for exactly how. Passing an empty list is equivalent to
     * {@link #reindex(List, IndexingMode, boolean, Runnable)}.
     *
     * @param featureFiles       the feature files collected for this run, in the order to number
     *                           them in
     * @param mode               the indexing mode to apply
     * @param forceRewrite       when {@code true}, ignores existing numbers and renumbers everything
     *                           from scratch; when {@code false}, leaves already-correctly-numbered
     *                           lines alone
     * @param projectDirectories the directories numbering is independently scoped to, or empty for
     *                           a single build-wide sequence
     * @param onFileReindexed    invoked once per file in {@code featureFiles}, in order; never
     *                           {@code null}
     */
    public void reindex(
            List<File> featureFiles, IndexingMode mode, boolean forceRewrite,
            List<File> projectDirectories, Runnable onFileReindexed) {
        List<ParsedFile> parsedFiles = new ArrayList<>();
        for (File featureFile : featureFiles) {
            parsedFiles.add(parse(featureFile));
        }

        boolean numberFeatures = mode == IndexingMode.FEATURE || mode == IndexingMode.ALL;
        if (numberFeatures) {
            for (List<ParsedFile> group : partitionByProject(parsedFiles, projectDirectories)) {
                List<LineMatch> groupFeatures = new ArrayList<>();
                for (ParsedFile parsedFile : group) {
                    groupFeatures.addAll(parsedFile.featureMatches);
                }
                resolveSequential(groupFeatures, forceRewrite, SINGLE_INDEX);
            }
        }

        if (mode == IndexingMode.SCENARIO) {
            for (List<ParsedFile> group : partitionByProject(parsedFiles, projectDirectories)) {
                List<LineMatch> groupScenarios = new ArrayList<>();
                for (ParsedFile parsedFile : group) {
                    groupScenarios.addAll(parsedFile.scenarioMatches);
                }
                resolveSequential(groupScenarios, forceRewrite, SINGLE_INDEX);
            }
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
     * Groups {@code parsedFiles} by nearest enclosing directory in {@code projectDirectories},
     * preserving each group's relative order of first appearance in {@code parsedFiles} - so
     * numbering a group in that order matches the overall {@code featureFiles} order, exactly as
     * it always has for a single, ungrouped list. A file that isn't under any of
     * {@code projectDirectories} (shouldn't normally happen, since callers are expected to supply
     * every project directory in the build) becomes its own single-file group rather than being
     * silently folded into an unrelated one. An empty {@code projectDirectories} yields a single
     * group containing every file, unchanged from before this method existed.
     */
    private List<List<ParsedFile>> partitionByProject(List<ParsedFile> parsedFiles, List<File> projectDirectories) {
        if (projectDirectories.isEmpty()) {
            return List.of(parsedFiles);
        }
        List<File> byPathLengthDescending = new ArrayList<>(projectDirectories);
        byPathLengthDescending.sort(
                Comparator.comparingInt((File dir) -> dir.getAbsolutePath().length()).reversed());

        Map<File, List<ParsedFile>> groups = new LinkedHashMap<>();
        for (ParsedFile parsedFile : parsedFiles) {
            File owner = owningProjectDirectory(parsedFile.file, byPathLengthDescending);
            groups.computeIfAbsent(owner, key -> new ArrayList<>()).add(parsedFile);
        }
        return new ArrayList<>(groups.values());
    }

    /**
     * The most specific (longest path) entry in {@code byPathLengthDescending} that is an ancestor
     * of {@code featureFile}, or {@code featureFile} itself if none is - see
     * {@link #partitionByProject(List, List)}.
     */
    private File owningProjectDirectory(File featureFile, List<File> byPathLengthDescending) {
        Path filePath = canonicalPath(featureFile);
        for (File candidate : byPathLengthDescending) {
            if (filePath.startsWith(canonicalPath(candidate))) {
                return candidate;
            }
        }
        return featureFile;
    }

    /**
     * {@code file}'s canonical path - symlinks resolved, so e.g. macOS's {@code /tmp} ->
     * {@code /private/tmp} doesn't make a feature file look like it lives outside every candidate
     * project directory just because one side of the comparison went through the symlink and the
     * other didn't. Falls back to the plain absolute, normalized path on the rare I/O failure
     * (e.g. the file was deleted mid-run) rather than letting {@link #owningProjectDirectory} throw.
     */
    private Path canonicalPath(File file) {
        try {
            return file.getCanonicalFile().toPath();
        } catch (IOException e) {
            return file.getAbsoluteFile().toPath().normalize();
        }
    }

    /**
     * Determines the number for every entry in {@code matches}: entries whose {@link
     * LineMatch#rawName} already matches {@code pinPattern} (and {@code forceRewrite} is
     * {@code false}) keep that number ("pinned"); every other entry is assigned a fresh number,
     * counting up from one past the highest pinned number (or from 1, if none are pinned), in list
     * order. Fresh numbers are never lower than an already-pinned one, so they read as a
     * continuation of the existing sequence rather than backfilling a gap earlier in it.
     *
     * <p>If two entries' titles claim the same number - e.g. because two features or scenarios were
     * authored independently on separate branches and merged - only the first one encountered (in
     * list order) keeps it pinned. Every later entry claiming an already-taken number is treated as
     * unpinned instead and falls through to the fresh-number assignment below, so the collision is
     * resolved rather than silently left in both files.</p>
     */
    private void resolveSequential(List<LineMatch> matches, boolean forceRewrite, Pattern pinPattern) {
        Set<Integer> taken = new HashSet<>();
        if (!forceRewrite) {
            for (LineMatch match : matches) {
                Matcher matcher = pinPattern.matcher(match.rawName);
                if (matcher.matches()) {
                    int candidate = Integer.parseInt(matcher.group(1));
                    if (taken.add(candidate)) {
                        match.number = candidate;
                    }
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

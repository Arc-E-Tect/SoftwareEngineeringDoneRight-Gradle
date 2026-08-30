package com.arc_e_tect.gradle.gherkin;

import com.arc_e_tect.gradle.gherkin.console.ScanProgressReporter;
import com.arc_e_tect.gradle.gherkin.glue.GlueCodeScanner;
import com.arc_e_tect.gradle.gherkin.indexing.FeatureIndexer;
import com.arc_e_tect.gradle.gherkin.indexing.IndexingMode;
import com.arc_e_tect.gradle.gherkin.parser.FeatureParser;
import com.arc_e_tect.gradle.gherkin.parser.ScenarioGrouping;
import com.arc_e_tect.gradle.gherkin.parser.ScenarioInfo;
import com.arc_e_tect.gradle.gherkin.progress.ProgressHistoryStore;
import com.arc_e_tect.gradle.gherkin.progress.ProgressHistoryUpdater;
import com.arc_e_tect.gradle.gherkin.progress.ProgressReportOptions;
import com.arc_e_tect.gradle.gherkin.progress.ProgressReportWriter;
import com.arc_e_tect.gradle.gherkin.progress.ScenarioProgressRecord;
import io.cucumber.cucumberexpressions.Expression;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Gradle task that scans {@code .feature} files and writes all scenario titles
 * to a single AsciiDoc file.
 *
 * <p>Caching is intentionally disabled: the output depends entirely on the
 * contents of the feature files and regeneration is cheap.</p>
 *
 * <p>Either {@link #getSourceDirs()} or {@link #getSourceFile()} must be
 * configured, but not both.  When neither is set the task falls back to the
 * default source directory ({@value GherkinToAsciidocExtension#DEFAULT_SOURCE_DIR})
 * relative to the project directory.</p>
 *
 * <p>When {@link #getTrackProgress()} is enabled, every scenario is classified as
 * {@code listed}, {@code defined}, or {@code implemented} by cross-referencing its
 * steps against the step definitions found in {@link #getGlueCodeDirs()}.</p>
 *
 * <p>When {@link #getTrackProgressHistory()} is also enabled, a per-scenario history of when each
 * scenario first reached each of those three statuses is loaded from
 * {@link #getProgressHistoryFile()}, advanced with the current run's scenarios, and - only when
 * {@link #getUpdateProgressHistory()} resolves to {@code true} - written back.</p>
 *
 * <p>{@link #generate()} runs up to three scan-shaped phases - reindexing (only when indexing is
 * active), parsing (always), and glue code scanning (only when {@link #getTrackProgress()} is
 * {@code true}) - each announced with its own {@code LIFECYCLE} banner and reported on
 * periodically via {@link ScanProgressReporter}, using that class's own default throttle (every
 * 50 items or every 2 seconds) unmodified, so a consumer watching the build knows the task is
 * still alive on a large feature tree.</p>
 */
@DisableCachingByDefault(because = "Generated documentation depends on source file content and is cheap to regenerate")
public abstract class GenerateFeatureDocsTask extends DefaultTask {

    /**
     * Source directories containing the {@code .feature} files to process. One or more
     * directories may be configured. Mutually exclusive with {@link #getSourceFile()}.
     *
     * @return mutable file collection of feature file source directories
     */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getSourceDirs();

    /**
     * Optional single {@code .feature} file to process.
     * Mutually exclusive with {@link #getSourceDirs()}.
     *
     * @return mutable file property for a single feature file
     */
    @Optional
    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getSourceFile();

    /**
     * Whether to recursively scan sub-directories of every directory in {@link #getSourceDirs()}.
     *
     * @return mutable boolean property controlling recursive directory scanning
     */
    @Input
    public abstract Property<Boolean> getIncludeSubDirs();

    /**
     * Directory where the generated AsciiDoc file will be written.
     *
     * @return mutable directory property for the output directory
     */
    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    /**
     * Name of the generated AsciiDoc file (without path).
     *
     * @return mutable string property for the output file name
     */
    @Input
    public abstract Property<String> getOutputFileName();

    /**
     * Whether to classify scenarios as {@code listed}, {@code defined}, or
     * {@code implemented} and include a progress summary in the generated AsciiDoc.
     *
     * @return mutable boolean property controlling progress tracking
     */
    @Input
    public abstract Property<Boolean> getTrackProgress();

    /**
     * Directories containing the Cucumber-JVM glue code (step definitions). One or more
     * directories may be configured. Required when {@link #getTrackProgress()} is {@code true}.
     *
     * @return mutable file collection of glue code directories
     */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getGlueCodeDirs();

    /**
     * Whether to group scenarios by their enclosing {@code Feature} instead of a flat list. When
     * {@link #getTrackProgress()} is {@code true}, grouping additionally splits report snippets by
     * feature (see {@link #getSnippetDir()}).
     *
     * @return mutable boolean property controlling grouping by feature
     */
    @Input
    public abstract Property<Boolean> getGroupByFeature();

    /**
     * Directory that report snippets ({@code listed.adoc}/{@code defined.adoc}/{@code implemented.adoc})
     * are written to when {@link #getTrackProgress()} is {@code true}.
     *
     * @return mutable directory property for the snippet output directory
     */
    @OutputDirectory
    public abstract DirectoryProperty getSnippetDir();

    /**
     * Optional Mustache template used to render the report so that it references the snippets via
     * {@code include::} directives instead of embedding their content verbatim. Only consulted
     * when {@link #getTrackProgress()} is {@code true}; ignored otherwise.
     *
     * @return mutable file property for the Mustache template file
     */
    @Optional
    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getTemplate();

    /**
     * Version of the system under test that the reported Gherkin scenarios exercise, printed in the
     * generated document as e.g. {@code System Under Test version: v1.0.0}.
     *
     * @return mutable string property for the system-under-test version
     */
    @Input
    public abstract Property<String> getSystemUnderTestVersion();

    /**
     * Whether - and how - to number {@code Feature}/{@code Scenario} titles directly in the
     * source {@code .feature} files. {@link IndexingMode#OFF} and {@link IndexingMode#CI} are
     * always allowed; {@link IndexingMode#FEATURE}, {@link IndexingMode#SCENARIO}, and
     * {@link IndexingMode#ALL} are only allowed when {@link #getIncludeSubDirs()} is
     * {@code true}, and when {@link #getGroupByFeature()} is {@code false}, only
     * {@link IndexingMode#SCENARIO} of those three is allowed.
     *
     * @return mutable property for the indexing mode
     */
    @Input
    public abstract Property<IndexingMode> getIndexing();

    /**
     * Whether {@link #getIndexing()} renumbers every {@code Feature}/{@code Scenario} from scratch,
     * or only the ones not already correctly numbered for the currently configured
     * {@link IndexingMode}. Has no effect when {@link #getIndexing()} is {@link IndexingMode#OFF}
     * or {@link IndexingMode#CI}.
     *
     * @return mutable boolean property controlling whether existing numbering is preserved
     */
    @Input
    public abstract Property<Boolean> getForceRewrite();

    /**
     * In a multi-project Gradle build, whether {@link #getIndexing()}'s numbering is one continuous
     * sequence spanning every project in the build, or independently scoped to each project - see
     * {@link GherkinToAsciidocExtension#getConsolidatedIndex()}. Has no effect when
     * {@link #getProjectDirectories()} contains at most one directory, since there's then only one
     * project to scope numbering to either way.
     *
     * @return mutable boolean property controlling whether indexing is consolidated build-wide or
     *         scoped per project
     */
    @Input
    public abstract Property<Boolean> getConsolidatedIndex();

    /**
     * Every project's directory in this build, used - only when {@link #getConsolidatedIndex()} is
     * {@code false} - to scope {@link #getIndexing()}'s numbering to whichever of these directories
     * each feature file lives under. Populated by the plugin from
     * {@code project.getRootProject().getAllprojects()}; not meant to be configured directly.
     *
     * @return mutable list property of every project's directory in this build
     */
    @Input
    public abstract ListProperty<File> getProjectDirectories();

    /**
     * Whether to persist, across builds, a per-scenario history of when each scenario first
     * reached {@code listed}, {@code defined}, and {@code implemented} status. Requires
     * {@link #getTrackProgress()} to also be {@code true}.
     *
     * @return mutable boolean property controlling whether scenario progress history is tracked
     */
    @Input
    public abstract Property<Boolean> getTrackProgressHistory();

    /**
     * File that the persisted scenario progress history is read from and, when
     * {@link #getUpdateProgressHistory()} is {@code true}, written back to. Deliberately not
     * declared as an {@code @InputFile}/{@code @OutputFile}: the file legitimately may not exist yet
     * (treated as an empty history, not an error) and is only conditionally written back, so it's
     * read and written directly in {@link #generate()} instead of through Gradle's file-content-based
     * up-to-date checking. Its configured <em>path</em> - as opposed to the file's content - is still
     * tracked as a plain input via {@link #getProgressHistoryFilePath()}, so that renaming or
     * relocating it is itself enough to invalidate this task's up-to-date state.
     *
     * @return mutable file property for the progress history file
     */
    @Internal
    public abstract RegularFileProperty getProgressHistoryFile();

    /**
     * The absolute path of {@link #getProgressHistoryFile()}, tracked as a plain {@code @Input}
     * value - not the file's content, which {@link #getProgressHistoryFile()} itself is deliberately
     * excluded from up-to-date checking for. Without this, renaming or relocating
     * {@code progressHistoryFile} in the build script - with no other configured input having
     * changed - would leave this task {@code UP-TO-DATE} and silently skip writing history to the
     * newly configured location.
     *
     * @return the progress history file's absolute path, or {@code null} if unset
     */
    @Input
    @Optional
    public String getProgressHistoryFilePath() {
        return getProgressHistoryFile().map(file -> file.getAsFile().getAbsolutePath()).getOrNull();
    }

    /**
     * Whether {@link #getProgressHistoryFile()} is written back to disk after being updated with the
     * current run's scenarios. Only consulted when {@link #getTrackProgressHistory()} is
     * {@code true}; the history file is always read regardless.
     *
     * @return mutable boolean property controlling whether the progress history file is written back
     */
    @Input
    public abstract Property<Boolean> getUpdateProgressHistory();

    /**
     * Root directory of the project, used to resolve the default source directory
     * when neither {@link #getSourceDirs()} nor {@link #getSourceFile()} is set.
     *
     * @return mutable directory property for the project root directory
     */
    @Internal
    public abstract DirectoryProperty getProjectDirectory();

    /**
     * Creates a new task instance.
     * Invoked by Gradle's dependency injection infrastructure.
     */
    @Inject
    public GenerateFeatureDocsTask() {
        setGroup("documentation");
        setDescription("Scans .feature files and generates an AsciiDoc file with all scenario titles.");
    }

    /**
     * Task action: collects {@code .feature} files, parses scenarios,
     * and writes them to the configured AsciiDoc output file.
     */
    @TaskAction
    public void generate() {
        boolean sourceDirsSet = !getSourceDirs().isEmpty();
        boolean sourceFileSet = getSourceFile().isPresent();

        if (sourceDirsSet && sourceFileSet) {
            throw new GradleException(
                    "gherkinToAsciidoc: sourceDirs and sourceFile are mutually exclusive. " +
                    "Please configure only one of them.");
        }

        if (sourceFileSet && getIncludeSubDirs().get()) {
            throw new GradleException(
                    "gherkinToAsciidoc: includeSubDirs cannot be used when sourceFile is configured. " +
                    "It can only be used with sourceDirs.");
        }

        boolean trackProgress = getTrackProgress().get();
        if (trackProgress) {
            if (!sourceDirsSet) {
                throw new GradleException(
                        "gherkinToAsciidoc: trackProgress can only be enabled when sourceDirs is configured.");
            }
            if (getGlueCodeDirs().isEmpty()) {
                throw new GradleException(
                        "gherkinToAsciidoc: trackProgress requires glueCodeDirs to be configured.");
            }
        }

        boolean trackProgressHistory = getTrackProgressHistory().get();
        if (trackProgressHistory && !trackProgress) {
            throw new GradleException(
                    "gherkinToAsciidoc: trackProgressHistory requires trackProgress to be enabled.");
        }

        IndexingMode indexing = getIndexing().get();
        boolean groupByFeature = getGroupByFeature().get();
        boolean indexingActive = indexing != IndexingMode.OFF && indexing != IndexingMode.CI;
        if (indexingActive && !getIncludeSubDirs().get()) {
            throw new GradleException(
                    "gherkinToAsciidoc: indexing can only be used when includeSubDirs is true.");
        }
        if (indexingActive && indexing != IndexingMode.SCENARIO && !groupByFeature) {
            throw new GradleException(
                    "gherkinToAsciidoc: when groupByFeature is false, indexing can only be "
                    + "'off', 'ci', or 'scenario'.");
        }

        // trackProgress implies recursive scanning, regardless of includeSubDirs's own value.
        boolean recursive = trackProgress || getIncludeSubDirs().get();

        List<File> featureFiles = collectFeatureFiles(sourceDirsSet, sourceFileSet, recursive);
        // CI skips indexing entirely - the feature files are left completely untouched, not even
        // to strip numbering left over from a previous run, unlike OFF. Only FEATURE/SCENARIO/ALL
        // get their own announced, progress-reported phase - OFF still runs (to strip stale
        // numbering) but silently, since numbering isn't actually happening.
        if (indexing != IndexingMode.CI) {
            List<File> projectBoundaries = getConsolidatedIndex().get() ? List.of() : getProjectDirectories().get();
            if (indexingActive) {
                getLogger().lifecycle("gherkinToAsciidoc: reindexing feature files...");
                ScanProgressReporter indexingProgress = ScanProgressReporter.determinate(
                        getLogger(), "Reindexing feature files", featureFiles.size());
                new FeatureIndexer().reindex(
                        featureFiles, indexing, getForceRewrite().get(), projectBoundaries, indexingProgress::step);
                indexingProgress.complete();
            } else {
                new FeatureIndexer().reindex(featureFiles, indexing, getForceRewrite().get(), projectBoundaries, () -> { });
            }
        }

        getLogger().lifecycle("gherkinToAsciidoc: parsing feature files...");
        List<ScenarioInfo> scenarios = new ArrayList<>();
        FeatureParser featureParser = new FeatureParser();
        ScanProgressReporter parsingProgress = ScanProgressReporter.determinate(
                getLogger(), "Parsing feature files", featureFiles.size());
        for (File featureFile : featureFiles) {
            scenarios.addAll(featureParser.parse(featureFile));
            parsingProgress.step();
        }
        parsingProgress.complete();

        File outDir = getOutputDir().getAsFile().get();
        if (!outDir.exists() && !outDir.mkdirs()) {
            throw new GradleException("gherkinToAsciidoc: could not create output directory: " + outDir);
        }

        File outputFile = new File(outDir, getOutputFileName().get());
        String systemUnderTestVersion = getSystemUnderTestVersion().get();

        if (trackProgress) {
            List<Expression> glueCode = scanGlueCode();
            File template = getTemplate().isPresent() ? getTemplate().getAsFile().get() : null;
            Map<String, ScenarioProgressRecord> history = trackProgressHistory
                    ? updateProgressHistory(scenarios, glueCode) : Map.of();
            ProgressReportOptions options = new ProgressReportOptions(
                    groupByFeature, getSnippetDir().getAsFile().get(), template, systemUnderTestVersion, history);
            new ProgressReportWriter().write(outputFile, scenarios, glueCode, options);
        } else {
            writeAsciidoc(outputFile, scenarios, groupByFeature, systemUnderTestVersion);
        }

        getLogger().lifecycle("Generated {} scenario title(s) to {}", scenarios.size(), outputFile);
    }

    /**
     * Loads the persisted progress history, advances it with the current run's scenarios, and -
     * only when {@link #getUpdateProgressHistory()} resolves to {@code true} - saves it back.
     * The history file is always read regardless of {@link #getUpdateProgressHistory()}, so the
     * generated report reflects the up-to-date-in-memory history even on a run that doesn't
     * persist it.
     */
    private Map<String, ScenarioProgressRecord> updateProgressHistory(
            List<ScenarioInfo> scenarios, List<Expression> glueCode) {
        File historyFile = getProgressHistoryFile().getAsFile().get();
        ProgressHistoryStore store = new ProgressHistoryStore();
        Map<String, ScenarioProgressRecord> previous = store.load(historyFile);
        Map<String, ScenarioProgressRecord> updated =
                new ProgressHistoryUpdater().update(previous, scenarios, glueCode, Instant.now());
        if (getUpdateProgressHistory().get()) {
            store.save(historyFile, updated.values());
        }
        return updated;
    }

    private List<Expression> scanGlueCode() {
        getLogger().lifecycle("gherkinToAsciidoc: scanning glue code...");
        List<Expression> glueCode = new ArrayList<>();
        GlueCodeScanner scanner = new GlueCodeScanner();
        ScanProgressReporter glueCodeProgress = ScanProgressReporter.indeterminate(getLogger(), "Scanning glue code");
        for (File dir : getGlueCodeDirs()) {
            glueCode.addAll(scanner.scan(dir, file -> glueCodeProgress.step()));
        }
        glueCodeProgress.complete();
        return glueCode;
    }

    private List<File> collectFeatureFiles(boolean sourceDirsSet, boolean sourceFileSet, boolean recursive) {
        List<File> files = new ArrayList<>();
        if (sourceFileSet) {
            files.add(getSourceFile().getAsFile().get());
        } else if (sourceDirsSet) {
            // Ordered by path so that processing order (and thus indexing numbers, and the order
            // scenarios appear in the generated report) is deterministic rather than
            // filesystem-dependent, regardless of the order sourceDirs was configured in.
            List<File> dirs = new ArrayList<>(getSourceDirs().getFiles());
            dirs.sort(Comparator.comparing(File::getAbsolutePath));
            for (File dir : dirs) {
                collectFromDir(dir, files, recursive);
            }
        } else {
            File defaultDir = new File(
                    getProjectDirectory().getAsFile().get(), GherkinToAsciidocExtension.DEFAULT_SOURCE_DIR);
            collectFromDir(defaultDir, files, recursive);
        }
        return files;
    }

    /**
     * Collects {@code .feature} files from {@code dir} in pre-order: every {@code .feature} file
     * directly in {@code dir} first (alphabetically by file name), then - when {@code recursive}
     * is {@code true} - every direct sub-directory's own files, recursively, in the same fashion
     * (sub-directories visited alphabetically by name).
     */
    private void collectFromDir(File dir, List<File> files, boolean recursive) {
        if (!dir.isDirectory()) {
            return;
        }
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }

        List<File> featureFilesHere = new ArrayList<>();
        List<File> subDirs = new ArrayList<>();
        for (File child : children) {
            if (child.isFile() && child.getName().endsWith(".feature")) {
                featureFilesHere.add(child);
            } else if (child.isDirectory() && recursive) {
                subDirs.add(child);
            }
        }

        featureFilesHere.sort(Comparator.comparing(File::getName));
        files.addAll(featureFilesHere);

        subDirs.sort(Comparator.comparing(File::getName));
        for (File subDir : subDirs) {
            collectFromDir(subDir, files, true);
        }
    }

    private void writeAsciidoc(
            File outputFile, List<ScenarioInfo> scenarios, boolean groupByFeature, String systemUnderTestVersion) {
        try (PrintWriter writer = new PrintWriter(outputFile, StandardCharsets.UTF_8)) {
            writer.println("= Feature Scenarios");
            writer.println(":toc:");
            writer.println(":toclevels: 2");
            writer.println();
            writer.println("System Under Test version: " + systemUnderTestVersion);
            writer.println();
            writer.println("This document lists every `Scenario` and `Scenario Outline` found under the "
                    + "configured feature file directories.");
            writer.println();
            if (scenarios.isEmpty()) {
                writer.println("No scenarios found.");
                return;
            }
            if (groupByFeature) {
                for (Map.Entry<String, List<ScenarioInfo>> entry
                        : ScenarioGrouping.byFeatureTitle(scenarios).entrySet()) {
                    writer.println("== " + entry.getKey());
                    writer.println();
                    for (ScenarioInfo scenario : entry.getValue()) {
                        writer.println("* " + scenario.title());
                    }
                    writer.println();
                }
            } else {
                for (ScenarioInfo scenario : scenarios) {
                    writer.println("* " + scenario.title());
                }
            }
        } catch (IOException e) {
            throw new GradleException("gherkinToAsciidoc: failed to write AsciiDoc file: " + outputFile, e);
        }
    }
}

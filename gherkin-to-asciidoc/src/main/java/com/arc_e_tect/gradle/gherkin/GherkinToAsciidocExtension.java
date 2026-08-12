package com.arc_e_tect.gradle.gherkin;

import com.arc_e_tect.gradle.gherkin.indexing.IndexingMode;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;

/**
 * DSL extension for the Gherkin-to-AsciiDoc Gradle plugin.
 *
 * <pre>
 * gherkinToAsciidoc {
 *     sourceDirs.from('src/test/resources/features')                                // default
 *     includeSubDirs = true                                                         // default
 *     outputDir      = layout.buildDirectory.dir('generated-docs')                 // default
 *     outputFileName = 'features.adoc'                                             // default
 *     trackProgress  = false                                                        // default
 *     // glueCodeDirs.from('src/test/java/.../steps')                              // required when trackProgress = true
 *     groupByFeature = true                                                         // default; forced to true whenever trackProgress = true
 *     // snippetDir  = layout.buildDirectory.dir('generated-docs/features/snippets') // default
 *     // template    = file('templates/report.mustache')                            // optional
 *     // systemUnderTestVersion = 'v1.0.0'          // optional; default: project.version
 *     indexing       = IndexingMode.OFF                                             // default; requires includeSubDirs = true
 *     forceRewrite   = false                                                        // default; see getForceRewrite()
 *     trackProgressHistory  = false                                                 // default; requires trackProgress = true
 *     // progressHistoryFile = layout.projectDirectory.file('gherkin-progress-history.ndjson') // default
 *     updateProgressHistory = trackProgressHistory                                  // default; see getUpdateProgressHistory()
 * }
 * </pre>
 *
 * <p>{@code indexing}, {@code forceRewrite}, and {@code updateProgressHistory} can each be overridden for
 * the whole build from the command line, e.g. {@code -PgherkinToAsciidoc.indexing=ci} - see
 * {@link #getIndexing()}, {@link #getForceRewrite()}, and {@link #getUpdateProgressHistory()}.</p>
 */
public abstract class GherkinToAsciidocExtension {

    /** For use by the Gradle-generated concrete subclass. */
    public GherkinToAsciidocExtension() {}

    /** Extension DSL block name, i.e. the name used to register the extension with the project. */
    public static final String NAME = "gherkinToAsciidoc";

    /** Default relative path of the source directory containing {@code .feature} files. */
    public static final String DEFAULT_SOURCE_DIR = "src/test/resources/features";

    /** Default name of the generated AsciiDoc output file. */
    public static final String DEFAULT_OUTPUT_FILE_NAME = "features.adoc";

    /** Default relative path of the directory report snippets are written to. */
    public static final String DEFAULT_SNIPPET_DIR = "generated-docs/features/snippets";

    /** Default name of the persisted scenario progress history file. */
    public static final String DEFAULT_PROGRESS_HISTORY_FILE_NAME = "gherkin-progress-history.ndjson";

    /**
     * Name of the Gradle project property that overrides {@link #getIndexing()} from the command
     * line for every project in the build, e.g. {@code -PgherkinToAsciidoc.indexing=ci}. Takes
     * precedence over any project's own configured {@code indexing} value. The value is matched
     * against {@link IndexingMode} enum constant names case-insensitively.
     */
    public static final String INDEXING_OVERRIDE_PROPERTY = "gherkinToAsciidoc.indexing";

    /**
     * Name of the Gradle project property that overrides {@link #getForceRewrite()} from the
     * command line for every project in the build, e.g.
     * {@code -PgherkinToAsciidoc.forceRewrite=true}. Takes precedence over any project's own
     * configured {@code forceRewrite} value. The value is parsed as a boolean.
     */
    public static final String FORCE_REWRITE_OVERRIDE_PROPERTY = "gherkinToAsciidoc.forceRewrite";

    /**
     * Name of the Gradle project property that overrides {@link #getUpdateProgressHistory()} from
     * the command line for every project in the build, e.g.
     * {@code -PgherkinToAsciidoc.updateProgressHistory=true}. Takes precedence over any project's
     * own configured {@code updateProgressHistory} value. The value is parsed as a boolean.
     */
    public static final String UPDATE_PROGRESS_HISTORY_OVERRIDE_PROPERTY = "gherkinToAsciidoc.updateProgressHistory";

    /**
     * Source directories that contain the {@code .feature} files to process. One or more
     * directories may be configured, e.g. via {@code sourceDirs.from(file('a'), file('b'))}.
     * Mutually exclusive with {@link #getSourceFile()}.
     *
     * @return mutable file collection of feature file source directories
     */
    public abstract ConfigurableFileCollection getSourceDirs();

    /**
     * A single {@code .feature} file to process.
     * Mutually exclusive with {@link #getSourceDirs()}.
     *
     * @return mutable file property for a single feature file
     */
    public abstract RegularFileProperty getSourceFile();

    /**
     * Whether to recursively scan sub-directories of every configured directory in
     * {@link #getSourceDirs()}. Defaults to {@code true}. Forced to {@code true} whenever
     * {@link #getTrackProgress()} is {@code true}.
     *
     * @return mutable boolean property controlling recursive directory scanning
     */
    public abstract Property<Boolean> getIncludeSubDirs();

    /**
     * Directory where the generated AsciiDoc file will be written.
     * Defaults to {@code build/generated-docs}.
     *
     * @return mutable directory property for the output directory
     */
    public abstract DirectoryProperty getOutputDir();

    /**
     * Name of the generated AsciiDoc file (without path).
     * Defaults to {@value #DEFAULT_OUTPUT_FILE_NAME}.
     *
     * @return mutable string property for the output file name
     */
    public abstract Property<String> getOutputFileName();

    /**
     * Whether to classify every scenario as {@code listed}, {@code defined}, or
     * {@code implemented} and include a progress summary in the generated AsciiDoc.
     * Defaults to {@code false}.
     *
     * <p>Can only be enabled when {@link #getSourceDirs()} and {@link #getGlueCodeDirs()}
     * are both configured; enabling it also implies {@link #getIncludeSubDirs()}.</p>
     *
     * @return mutable boolean property controlling progress tracking
     */
    public abstract Property<Boolean> getTrackProgress();

    /**
     * Directories containing the Cucumber-JVM glue code (step definitions) used to
     * determine whether a scenario's steps are implemented. One or more directories may be
     * configured, e.g. via {@code glueCodeDirs.from(file('a'), file('b'))}. Required when
     * {@link #getTrackProgress()} is {@code true}; ignored otherwise.
     *
     * @return mutable file collection of glue code directories
     */
    public abstract ConfigurableFileCollection getGlueCodeDirs();

    /**
     * Whether to group scenarios by their enclosing {@code Feature} in the generated AsciiDoc,
     * instead of a flat list. Defaults to {@code true}. Forced to {@code true} whenever
     * {@link #getTrackProgress()} is {@code true}, in which case scenarios are grouped by
     * feature within each of the listed/defined/implemented sections.
     *
     * @return mutable boolean property controlling grouping by feature
     */
    public abstract Property<Boolean> getGroupByFeature();

    /**
     * Directory that the {@code listed.adoc}/{@code defined.adoc}/{@code implemented.adoc} report
     * snippets are written to when {@link #getTrackProgress()} is {@code true}. Defaults to
     * {@code build/generated-docs/features/snippets}.
     *
     * <p>When {@link #getGroupByFeature()} is {@code true}, snippets for a status with at least one
     * scenario are written per feature, under {@code <snippetDir>/<camelCaseFeatureTitle>/<status>.adoc}.</p>
     *
     * @return mutable directory property for the snippet output directory
     */
    public abstract DirectoryProperty getSnippetDir();

    /**
     * Optional Mustache template used to render the generated AsciiDoc file so that it references
     * the report snippets via {@code include::} directives, instead of embedding their content
     * verbatim. Only consulted when {@link #getTrackProgress()} is {@code true}; ignored otherwise.
     *
     * <p>When not set, the generated report looks exactly as it does without this property:
     * scenario titles are embedded directly, with no {@code include::} directives. The snippet
     * files are still written either way.</p>
     *
     * @return mutable file property for the Mustache template file
     */
    public abstract RegularFileProperty getTemplate();

    /**
     * Version of the system under test that the reported Gherkin scenarios exercise, printed in the
     * generated document as e.g. {@code System Under Test version: v1.0.0}. Defaults to the project's
     * own {@code version} (as set in the build file or a properties file); set this property to override
     * that default, e.g. when the scenarios target a different artifact than the one being built.
     *
     * @return mutable string property for the system-under-test version
     */
    public abstract Property<String> getSystemUnderTestVersion();

    /**
     * Whether - and how - to number {@code Feature}/{@code Scenario} titles directly in the
     * source {@code .feature} files. Defaults to {@link IndexingMode#OFF}.
     *
     * <ul>
     *   <li>{@link IndexingMode#OFF} - nothing is numbered (default).</li>
     *   <li>{@link IndexingMode#FEATURE} - every feature is numbered, e.g.
     *       {@code Feature: 1 - User authentication}.</li>
     *   <li>{@link IndexingMode#SCENARIO} - every scenario is numbered continuously across all
     *       feature files, e.g. {@code Scenario: 1 - User logs in}.</li>
     *   <li>{@link IndexingMode#ALL} - both are numbered, scenarios as
     *       {@code <featureNumber>.<scenarioNumber>}, e.g. {@code Scenario: 1.1 - User logs in}.</li>
     *   <li>{@link IndexingMode#CI} - indexing is skipped entirely; unlike {@link IndexingMode#OFF},
     *       the source files aren't even stripped of prior numbering. See
     *       {@value #INDEXING_OVERRIDE_PROPERTY} below.</li>
     * </ul>
     *
     * <p>Feature files are processed in the same order the generated report lists them in: for each
     * source directory (directories themselves ordered alphabetically by path when more than one is
     * configured), that directory's own feature files first - alphabetically by file name - and only
     * then, when {@link #getIncludeSubDirs()} is {@code true}, its sub-directories' files, each
     * sub-directory visited the same way, alphabetically by name. Scenario numbers additionally
     * follow document order within each file. A line whose existing number already reflects the
     * currently configured mode is left untouched, rather than being renumbered to fit that
     * processing order - see {@link #getForceRewrite()} for exactly what "reflects the currently
     * configured mode" means and how to opt out of it.</p>
     *
     * <p>{@link IndexingMode#OFF} and {@link IndexingMode#CI} are always allowed.
     * {@link IndexingMode#FEATURE}, {@link IndexingMode#SCENARIO}, and {@link IndexingMode#ALL} are
     * only allowed when {@link #getIncludeSubDirs()} is {@code true}; when
     * {@link #getGroupByFeature()} is {@code false}, only {@link IndexingMode#SCENARIO} of those
     * three is allowed.</p>
     *
     * <p>The {@value #INDEXING_OVERRIDE_PROPERTY} project property, when set (e.g.
     * {@code -PgherkinToAsciidoc.indexing=ci}), overrides this property for every project in the
     * build regardless of what any project configures here - typically used to force
     * {@link IndexingMode#CI} in a CI pipeline so {@code generateFeatureDocs} never mutates source
     * files there, without having to change the build script itself.</p>
     *
     * @return mutable property for the indexing mode
     */
    public abstract Property<IndexingMode> getIndexing();

    /**
     * Whether {@link #getIndexing()} renumbers every {@code Feature}/{@code Scenario} from scratch
     * (ignoring any existing numbers), or only numbers the ones that aren't already correctly
     * numbered for the currently configured {@link IndexingMode}. Defaults to {@code false}. Has
     * no effect when {@link #getIndexing()} is {@link IndexingMode#OFF} (which always strips every
     * number, regardless) or {@link IndexingMode#CI} (which never touches anything, regardless).
     *
     * <ul>
     *   <li>{@code true} - every {@code Feature}/{@code Scenario} number is recomputed from
     *       scratch, exactly as if none of them had ever been numbered before - the same behaviour
     *       as before this property existed.</li>
     *   <li>{@code false} (default) - a line whose existing number already matches the format
     *       {@link #getIndexing()}'s mode would itself produce is left completely untouched: for a
     *       {@code Feature}, a single integer; for a {@code Scenario}/{@code Scenario Outline},
     *       either a single integer ({@link IndexingMode#SCENARIO}) or
     *       {@code <featureNumber>.<n>} matching that scenario's own feature's number
     *       ({@link IndexingMode#ALL}). Every other numbered line - the wrong format, or a leftover
     *       from a previously configured, different {@code indexing} value - is stripped and
     *       renumbered, same as {@code true}. A newly added feature file that happens to sort
     *       alphabetically before already-numbered files is given the next number not already in
     *       use, rather than bumping every already-numbered file after it.</li>
     * </ul>
     *
     * <p>For example, with {@code indexing = IndexingMode.SCENARIO} and {@code forceRewrite = false},
     * a {@code Scenario} already reading {@code Scenario: 3 - ...} keeps that number. Changing
     * {@code indexing} to {@link IndexingMode#ALL} affords that same scenario a fresh number - its
     * old {@code 3} doesn't match {@code ALL}'s {@code <featureNumber>.<n>} format, so it no longer
     * "reflects" the currently configured mode.</p>
     *
     * <p>The {@value #FORCE_REWRITE_OVERRIDE_PROPERTY} project property, when set (e.g.
     * {@code -PgherkinToAsciidoc.forceRewrite=true}), overrides this property for every project in
     * the build regardless of what any project configures here.</p>
     *
     * @return mutable boolean property controlling whether existing numbering is preserved
     */
    public abstract Property<Boolean> getForceRewrite();

    /**
     * Whether to persist, across builds, a per-scenario history of when each scenario first
     * reached {@code listed}, {@code defined}, and {@code implemented} status - surviving scenarios
     * being moved between feature files, since the history is keyed by a fingerprint of the
     * scenario's name rather than by feature file location. Defaults to {@code false}.
     *
     * <p>Requires {@link #getTrackProgress()} to also be {@code true}; enabling this property while
     * {@code trackProgress} is {@code false} fails {@code generateFeatureDocs} with a descriptive
     * error. The history file configured via {@link #getProgressHistoryFile()} is always read when
     * this property is {@code true}, regardless of {@link #getUpdateProgressHistory()}.</p>
     *
     * <p>The plugin itself has no dependency on git or any other version control system;
     * branch-based control over when to advance the history (e.g. "only on {@code main}") is a CI
     * concern, expressed purely through {@link #getUpdateProgressHistory()} - typically driven from
     * a Gradle property set differently per branch in the CI pipeline itself.</p>
     *
     * @return mutable boolean property controlling whether scenario progress history is tracked
     */
    public abstract Property<Boolean> getTrackProgressHistory();

    /**
     * File that the persisted scenario progress history is read from and, when
     * {@link #getUpdateProgressHistory()} is {@code true}, written back to. Defaults to
     * {@code gherkin-progress-history.ndjson} directly in the project directory - deliberately not
     * under {@code build/}, since this file is meant to be committed to version control so the
     * history survives across checkouts. Only consulted when {@link #getTrackProgressHistory()} is
     * {@code true}.
     *
     * @return mutable file property for the progress history file
     */
    public abstract RegularFileProperty getProgressHistoryFile();

    /**
     * Whether {@link #getProgressHistoryFile()} is written back to disk after being updated with the
     * current run's scenarios. Defaults to the same value as {@link #getTrackProgressHistory()}.
     * Only consulted when {@link #getTrackProgressHistory()} is {@code true}; the history file is
     * always read regardless of this property's value, so a build with this set to {@code false}
     * still reports against the up-to-date-in-memory history, it simply doesn't persist it.
     *
     * <p>Set this to {@code false} for branches/builds that shouldn't advance the committed history
     * (e.g. feature branches or pull request builds), and leave it {@code true} (the default, once
     * {@code trackProgressHistory} is enabled) for the branch(es) that should - typically expressed
     * via the {@value #UPDATE_PROGRESS_HISTORY_OVERRIDE_PROPERTY} project property from the CI
     * pipeline rather than hardcoded in the build script, since the plugin itself has no notion of
     * which branch is currently checked out.</p>
     *
     * <p>The {@value #UPDATE_PROGRESS_HISTORY_OVERRIDE_PROPERTY} project property, when set (e.g.
     * {@code -PgherkinToAsciidoc.updateProgressHistory=true}), overrides this property for every
     * project in the build regardless of what any project configures here.</p>
     *
     * @return mutable boolean property controlling whether the progress history file is written back
     */
    public abstract Property<Boolean> getUpdateProgressHistory();
}

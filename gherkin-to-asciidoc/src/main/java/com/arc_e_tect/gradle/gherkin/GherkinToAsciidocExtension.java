package com.arc_e_tect.gradle.gherkin;

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
 *     includeSubDirs = false                                                        // default
 *     outputDir      = layout.buildDirectory.dir('generated-docs')                 // default
 *     outputFileName = 'features.adoc'                                             // default
 *     trackProgress  = false                                                        // default
 *     // glueCodeDirs.from('src/test/java/.../steps')                              // required when trackProgress = true
 *     groupByFeature = false                                                        // default; forced to true whenever trackProgress = true
 *     // snippetDir  = layout.buildDirectory.dir('generated-docs/features/snippets') // default
 *     // template    = file('templates/report.mustache')                            // optional
 *     // systemUnderTestVersion = 'v1.0.0'          // optional; default: project.version
 * }
 * </pre>
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
     * {@link #getSourceDirs()}. Defaults to {@code false}. Forced to {@code true} whenever
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
     * instead of a flat list. Defaults to {@code false}. Forced to {@code true} whenever
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
}

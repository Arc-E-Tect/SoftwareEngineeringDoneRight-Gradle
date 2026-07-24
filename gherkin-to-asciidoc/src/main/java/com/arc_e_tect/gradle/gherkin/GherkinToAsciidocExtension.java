package com.arc_e_tect.gradle.gherkin;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;

/**
 * DSL extension for the Gherkin-to-AsciiDoc Gradle plugin.
 *
 * <pre>
 * gherkinToAsciidoc {
 *     sourceDir      = layout.projectDirectory.dir('src/test/resources/features')  // default
 *     includeSubDirs = false                                                        // default
 *     outputDir      = layout.buildDirectory.dir('generated-docs')                 // default
 *     outputFileName = 'features.adoc'                                             // default
 *     trackProgress  = false                                                        // default
 *     // glueCodeDir = layout.projectDirectory.dir('src/test/java/.../steps')       // required when trackProgress = true
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

    /**
     * Source directory that contains the {@code .feature} files to process.
     * Mutually exclusive with {@link #getSourceFile()}.
     *
     * @return mutable directory property for the feature file source directory
     */
    public abstract DirectoryProperty getSourceDir();

    /**
     * A single {@code .feature} file to process.
     * Mutually exclusive with {@link #getSourceDir()}.
     *
     * @return mutable file property for a single feature file
     */
    public abstract RegularFileProperty getSourceFile();

    /**
     * Whether to recursively scan sub-directories when {@link #getSourceDir()} is used.
     * Defaults to {@code false}. Forced to {@code true} whenever {@link #getTrackProgress()}
     * is {@code true}.
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
     * <p>Can only be enabled when {@link #getSourceDir()} and {@link #getGlueCodeDir()}
     * are both configured; enabling it also implies {@link #getIncludeSubDirs()}.</p>
     *
     * @return mutable boolean property controlling progress tracking
     */
    public abstract Property<Boolean> getTrackProgress();

    /**
     * Directory containing the Cucumber-JVM glue code (step definitions) used to
     * determine whether a scenario's steps are implemented. Required when
     * {@link #getTrackProgress()} is {@code true}; ignored otherwise.
     *
     * @return mutable directory property for the glue code directory
     */
    public abstract DirectoryProperty getGlueCodeDir();
}

package com.arc_e_tect.gradle.gherkin;

import com.arc_e_tect.gradle.gherkin.glue.GlueCodeScanner;
import com.arc_e_tect.gradle.gherkin.parser.FeatureParser;
import com.arc_e_tect.gradle.gherkin.parser.ScenarioInfo;
import com.arc_e_tect.gradle.gherkin.progress.ProgressReportWriter;
import io.cucumber.cucumberexpressions.Expression;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFile;
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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Gradle task that scans {@code .feature} files and writes all scenario titles
 * to a single AsciiDoc file.
 *
 * <p>Caching is intentionally disabled: the output depends entirely on the
 * contents of the feature files and regeneration is cheap.</p>
 *
 * <p>Either {@link #getSourceDir()} or {@link #getSourceFile()} must be
 * configured, but not both.  When neither is set the task falls back to the
 * default source directory ({@value GherkinToAsciidocExtension#DEFAULT_SOURCE_DIR})
 * relative to the project directory.</p>
 *
 * <p>When {@link #getTrackProgress()} is enabled, every scenario is classified as
 * {@code listed}, {@code defined}, or {@code implemented} by cross-referencing its
 * steps against the step definitions found in {@link #getGlueCodeDir()}.</p>
 */
@DisableCachingByDefault(because = "Generated documentation depends on source file content and is cheap to regenerate")
public abstract class GenerateFeatureDocsTask extends DefaultTask {

    /**
     * Optional source directory containing the {@code .feature} files to process.
     * Mutually exclusive with {@link #getSourceFile()}.
     *
     * @return mutable directory property for the feature file source directory
     */
    @Optional
    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getSourceDir();

    /**
     * Optional single {@code .feature} file to process.
     * Mutually exclusive with {@link #getSourceDir()}.
     *
     * @return mutable file property for a single feature file
     */
    @Optional
    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getSourceFile();

    /**
     * Whether to recursively scan sub-directories when {@link #getSourceDir()} is used.
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
     * Optional directory containing the Cucumber-JVM glue code (step definitions).
     * Required when {@link #getTrackProgress()} is {@code true}.
     *
     * @return mutable directory property for the glue code directory
     */
    @Optional
    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getGlueCodeDir();

    /**
     * Root directory of the project, used to resolve the default source directory
     * when neither {@link #getSourceDir()} nor {@link #getSourceFile()} is set.
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
        boolean sourceDirSet = getSourceDir().isPresent();
        boolean sourceFileSet = getSourceFile().isPresent();

        if (sourceDirSet && sourceFileSet) {
            throw new GradleException(
                    "gherkinToAsciidoc: sourceDir and sourceFile are mutually exclusive. " +
                    "Please configure only one of them.");
        }

        if (sourceFileSet && getIncludeSubDirs().get()) {
            throw new GradleException(
                    "gherkinToAsciidoc: includeSubDirs cannot be used when sourceFile is configured. " +
                    "It can only be used with sourceDir.");
        }

        boolean trackProgress = getTrackProgress().get();
        if (trackProgress) {
            if (!sourceDirSet) {
                throw new GradleException(
                        "gherkinToAsciidoc: trackProgress can only be enabled when sourceDir is configured.");
            }
            if (!getGlueCodeDir().isPresent()) {
                throw new GradleException(
                        "gherkinToAsciidoc: trackProgress requires glueCodeDir to be configured.");
            }
        }

        // trackProgress implies recursive scanning, regardless of includeSubDirs's own value.
        boolean recursive = trackProgress || getIncludeSubDirs().get();

        List<File> featureFiles = collectFeatureFiles(sourceDirSet, sourceFileSet, recursive);

        List<ScenarioInfo> scenarios = new ArrayList<>();
        FeatureParser featureParser = new FeatureParser();
        for (File featureFile : featureFiles) {
            scenarios.addAll(featureParser.parse(featureFile));
        }

        File outDir = getOutputDir().getAsFile().get();
        if (!outDir.exists() && !outDir.mkdirs()) {
            throw new GradleException("gherkinToAsciidoc: could not create output directory: " + outDir);
        }

        File outputFile = new File(outDir, getOutputFileName().get());

        if (trackProgress) {
            List<Expression> glueCode = new GlueCodeScanner().scan(getGlueCodeDir().getAsFile().get());
            new ProgressReportWriter().write(outputFile, scenarios, glueCode);
        } else {
            List<String> titles = scenarios.stream().map(ScenarioInfo::title).collect(Collectors.toList());
            writeAsciidoc(outputFile, titles);
        }

        getLogger().lifecycle("Generated {} scenario title(s) to {}", scenarios.size(), outputFile);
    }

    private List<File> collectFeatureFiles(boolean sourceDirSet, boolean sourceFileSet, boolean recursive) {
        List<File> files = new ArrayList<>();
        if (sourceFileSet) {
            files.add(getSourceFile().getAsFile().get());
        } else {
            File dir = sourceDirSet
                    ? getSourceDir().getAsFile().get()
                    : new File(getProjectDirectory().getAsFile().get(), GherkinToAsciidocExtension.DEFAULT_SOURCE_DIR);
            collectFromDir(dir, files, recursive);
        }
        return files;
    }

    private void collectFromDir(File dir, List<File> files, boolean recursive) {
        if (!dir.isDirectory()) {
            return;
        }
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (child.isFile() && child.getName().endsWith(".feature")) {
                files.add(child);
            } else if (child.isDirectory() && recursive) {
                collectFromDir(child, files, true);
            }
        }
    }

    private void writeAsciidoc(File outputFile, List<String> titles) {
        try (PrintWriter writer = new PrintWriter(outputFile, StandardCharsets.UTF_8)) {
            writer.println("= Feature Scenarios");
            writer.println();
            for (String title : titles) {
                writer.println("* " + title);
            }
        } catch (IOException e) {
            throw new GradleException("gherkinToAsciidoc: failed to write AsciiDoc file: " + outputFile, e);
        }
    }
}

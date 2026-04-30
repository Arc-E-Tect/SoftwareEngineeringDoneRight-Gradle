package com.arc_e_tect.gradle.gherkin;

import com.arc_e_tect.gradle.gherkin.parser.FeatureParser;
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

@DisableCachingByDefault(because = "Generated documentation depends on source file content and is cheap to regenerate")
public abstract class GenerateFeatureDocsTask extends DefaultTask {

    @Optional
    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getSourceDir();

    @Optional
    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getSourceFile();

    @Input
    public abstract Property<Boolean> getIncludeSubDirs();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    @Input
    public abstract Property<String> getOutputFileName();

    @Internal
    public abstract DirectoryProperty getProjectDirectory();

    @Inject
    public GenerateFeatureDocsTask() {
        setGroup("documentation");
        setDescription("Scans .feature files and generates an AsciiDoc file with all scenario titles.");
    }

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

        List<File> featureFiles = collectFeatureFiles(sourceDirSet, sourceFileSet);

        List<String> titles = new ArrayList<>();
        FeatureParser featureParser = new FeatureParser();
        for (File featureFile : featureFiles) {
            titles.addAll(featureParser.parse(featureFile));
        }

        File outDir = getOutputDir().getAsFile().get();
        if (!outDir.exists() && !outDir.mkdirs()) {
            throw new GradleException("gherkinToAsciidoc: could not create output directory: " + outDir);
        }

        File outputFile = new File(outDir, getOutputFileName().get());
        writeAsciidoc(outputFile, titles);
        getLogger().lifecycle("Generated {} scenario title(s) to {}", titles.size(), outputFile);
    }

    private List<File> collectFeatureFiles(boolean sourceDirSet, boolean sourceFileSet) {
        List<File> files = new ArrayList<>();
        if (sourceFileSet) {
            files.add(getSourceFile().getAsFile().get());
        } else {
            File dir = sourceDirSet
                    ? getSourceDir().getAsFile().get()
                    : new File(getProjectDirectory().getAsFile().get(), GherkinToAsciidocExtension.DEFAULT_SOURCE_DIR);
            collectFromDir(dir, files, getIncludeSubDirs().get());
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

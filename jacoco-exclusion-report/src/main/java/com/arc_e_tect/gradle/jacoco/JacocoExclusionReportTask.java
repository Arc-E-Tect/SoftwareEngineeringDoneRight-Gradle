package com.arc_e_tect.gradle.jacoco;

import com.arc_e_tect.gradle.jacoco.model.ExcludedElement;
import com.arc_e_tect.gradle.jacoco.report.HtmlReportWriter;
import com.arc_e_tect.gradle.jacoco.report.XmlReportWriter;
import com.arc_e_tect.gradle.jacoco.scan.AnnotationScanner;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Gradle task that scans Java source files for the exclusion annotation and
 * writes HTML and XML reports to the configured output directory.
 *
 * <p>Registered automatically by {@link JacocoExclusionReportPlugin} under
 * the name {@code jacocoExclusionReport}.</p>
 */
@DisableCachingByDefault(because = "Report depends on source file content and is cheap to regenerate")
public abstract class JacocoExclusionReportTask extends DefaultTask {

    /**
     * Simple (unqualified) name of the annotation to search for.
     *
     * @return mutable property holding the annotation simple name
     */
    @Input
    public abstract Property<String> getAnnotationName();

    /**
     * Java source files to scan.
     *
     * @return mutable file collection of {@code .java} source files
     */
    @InputFiles
    @SkipWhenEmpty
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getSourceFiles();

    /**
     * Directory to write the HTML and XML reports into.
     *
     * @return mutable directory property for the report output location
     */
    @OutputDirectory
    public abstract DirectoryProperty getReportDir();

    /**
     * Creates the task. Instantiated by Gradle infrastructure via {@link javax.inject.Inject}.
     */
    @Inject
    public JacocoExclusionReportTask() {
        setGroup("verification");
        setDescription("Scans sources for @" + JacocoExclusionReportExtension.DEFAULT_ANNOTATION
                       + " and generates HTML + XML exclusion reports.");
    }

    /**
     * Scans all configured source files for the exclusion annotation and writes the
     * HTML and XML reports to {@link #getReportDir()}.
     */
    @TaskAction
    public void generate() {
        AnnotationScanner  scanner     = new AnnotationScanner(getAnnotationName().get());
        HtmlReportWriter   htmlWriter  = new HtmlReportWriter();
        XmlReportWriter    xmlWriter   = new XmlReportWriter();
        List<ExcludedElement> elements = new ArrayList<>();

        for (File entry : getSourceFiles()) {
            // The source collection may contain individual files (from a FileTree)
            // or directories (when set from srcDirs). Handle both.
            if (entry.isDirectory()) {
                try {
                    Files.walk(entry.toPath())
                         .filter(p -> p.toString().endsWith(".java"))
                         .map(java.nio.file.Path::toFile)
                         .forEach(f -> scanFile(scanner, f, elements));
                } catch (IOException e) {
                    throw new GradleException("Failed to walk source directory " + entry, e);
                }
            } else if (entry.getName().endsWith(".java")) {
                scanFile(scanner, entry, elements);
            }
        }

        File outputDir = getReportDir().get().getAsFile();
        try {
            htmlWriter.write(elements, getAnnotationName().get(), outputDir);
            xmlWriter.write(elements,  getAnnotationName().get(), outputDir);
        } catch (IOException e) {
            throw new GradleException("Failed to write exclusion report to " + outputDir, e);
        }

        getLogger().lifecycle(
                "JaCoCo exclusion report: {} element(s) found. Report \u2192 {}",
                elements.size(), outputDir.getAbsolutePath());
    }

    private void scanFile(AnnotationScanner scanner, File file, List<ExcludedElement> elements) {
        try {
            elements.addAll(scanner.scan(file));
        } catch (IOException e) {
            throw new GradleException("Failed to scan " + file, e);
        }
    }
}

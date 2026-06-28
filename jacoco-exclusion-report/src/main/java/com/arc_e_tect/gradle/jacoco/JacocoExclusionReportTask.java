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
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Gradle task that scans Java source files for the exclusion annotation and,
 * optionally, derives exclusions from configured JaCoCo {@code classDirectories}
 * filters.
 *
 * <p>The task writes separate HTML and XML reports for annotation-based
 * exclusions and JaCoCo DSL-based exclusions to the configured output
 * directory.</p>
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
     * Compiled main-class outputs used to determine potential classes that
     * could be present in coverage analysis.
     *
     * @return file collection of compiled class outputs
     */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getMainClassFiles();

    /**
     * Class directories included by configured JaCoCo tasks.
     *
     * @return file collection representing class files/directories included by JaCoCo
     */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getJacocoIncludedClassFiles();

    /**
     * Whether exclusions configured through JaCoCo DSL should also be reported
     * in dedicated DSL report files.
     *
     * @return mutable flag controlling JaCoCo DSL exclusion reporting
     */
    @Input
    public abstract Property<Boolean> getIncludeConfiguredExclusions();

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
    * Scans all configured source files for the exclusion annotation, optionally
    * derives exclusions from configured JaCoCo class-directory filters, and
    * writes the report files to {@link #getReportDir()}.
     */
    @TaskAction
    public void generate() {
        AnnotationScanner  scanner     = new AnnotationScanner(getAnnotationName().get());
        HtmlReportWriter   htmlWriter  = new HtmlReportWriter();
        XmlReportWriter    xmlWriter   = new XmlReportWriter();
        List<ExcludedElement> annotationElements = new ArrayList<>();

        for (File entry : getSourceFiles()) {
            // The source collection may contain individual files (from a FileTree)
            // or directories (when set from srcDirs). Handle both.
            if (entry.isDirectory()) {
                try {
                    Files.walk(entry.toPath())
                         .filter(p -> p.toString().endsWith(".java"))
                         .map(java.nio.file.Path::toFile)
                         .forEach(f -> scanFile(scanner, f, annotationElements));
                } catch (IOException e) {
                    throw new GradleException("Failed to walk source directory " + entry, e);
                }
            } else if (entry.getName().endsWith(".java")) {
                scanFile(scanner, entry, annotationElements);
            }
        }

        List<ExcludedElement> dslElements = getIncludeConfiguredExclusions().getOrElse(true)
                ? collectJacocoDslExcludedClasses()
                : List.of();

        File outputDir = getReportDir().get().getAsFile();
        try {
            htmlWriter.write(annotationElements, "@" + getAnnotationName().get(), "Annotation", outputDir, "index.html");
            xmlWriter.write(annotationElements,  "annotation: @" + getAnnotationName().get(), outputDir, "jacoco-exclusions.xml");

            if (getIncludeConfiguredExclusions().getOrElse(true)) {
                htmlWriter.write(dslElements, "JaCoCo classDirectories excludes", "Source", outputDir, "index-jacoco-dsl.html");
                xmlWriter.write(dslElements, "source: JaCoCo classDirectories excludes", outputDir, "jacoco-dsl-exclusions.xml");
            }
        } catch (IOException e) {
            throw new GradleException("Failed to write exclusion report to " + outputDir, e);
        }

        getLogger().lifecycle(
                "JaCoCo exclusion report: {} annotation element(s), {} DSL element(s). Report \u2192 {}",
                annotationElements.size(), dslElements.size(), outputDir.getAbsolutePath());
    }

    private List<ExcludedElement> collectJacocoDslExcludedClasses() {
        Set<File> allMainClassFiles = collectClassFiles(
                getMainClassFiles().getFiles());
        if (allMainClassFiles.isEmpty()) {
            return List.of();
        }

        Set<File> includedByJacoco = collectClassFiles(getJacocoIncludedClassFiles().getFiles());

        if (includedByJacoco.isEmpty()) {
            return List.of();
        }

        return allMainClassFiles.stream()
                .filter(file -> !includedByJacoco.contains(file))
                .map(this::toDslExcludedElement)
                .collect(Collectors.toList());
    }

    private Set<File> collectClassFiles(Set<File> roots) {
        Set<File> classFiles = new LinkedHashSet<>();
        for (File root : roots) {
            if (!root.exists()) {
                continue;
            }
            try {
                Files.walk(root.toPath())
                        .filter(path -> path.toString().endsWith(".class"))
                        .map(java.nio.file.Path::toFile)
                        .forEach(classFiles::add);
            } catch (IOException e) {
                throw new GradleException("Failed to read compiled classes from " + root, e);
            }
        }
        return classFiles;
    }

    private ExcludedElement toDslExcludedElement(File classFile) {
        String normalized = classFile.getPath().replace('\\', '/');
        int classesMarker = normalized.lastIndexOf("/classes/");
        String relativePath = classesMarker >= 0
                ? normalized.substring(classesMarker + "/classes/".length())
                : classFile.getName();

        int packageStart = relativePath.indexOf('/');
        if (packageStart >= 0 && packageStart + 1 < relativePath.length()) {
            relativePath = relativePath.substring(packageStart + 1);
        }

        int dotClass = relativePath.lastIndexOf(".class");
        String withoutClass = dotClass >= 0 ? relativePath.substring(0, dotClass) : relativePath;
        withoutClass = stripCompilationPathPrefixes(withoutClass);
        String packageName = "";
        String className = withoutClass;
        int slash = withoutClass.lastIndexOf('/');
        if (slash >= 0) {
            packageName = withoutClass.substring(0, slash).replace('/', '.');
            className = withoutClass.substring(slash + 1);
        }

        return new ExcludedElement(
                ExcludedElement.ElementType.CLASS,
                packageName,
                className,
                "",
                0,
                classFile.getName(),
                "Configured via JaCoCo classDirectories excludes");
    }

    private String stripCompilationPathPrefixes(String path) {
        List<String> parts = Arrays.stream(path.split("/"))
                .filter(part -> !part.isBlank())
                .collect(Collectors.toList());

        int start = 0;
        if (start < parts.size() && ("java".equals(parts.get(start))
                || "kotlin".equals(parts.get(start))
                || "groovy".equals(parts.get(start)))) {
            start++;
        }
        if (start < parts.size() && ("main".equals(parts.get(start)) || "test".equals(parts.get(start)))) {
            start++;
        }

        return String.join("/", parts.subList(start, parts.size()));
    }

    private void scanFile(AnnotationScanner scanner, File file, List<ExcludedElement> elements) {
        try {
            elements.addAll(scanner.scan(file));
        } catch (IOException e) {
            throw new GradleException("Failed to scan " + file, e);
        }
    }
}

package com.arc_e_tect.gradle.shadow;

import com.arc_e_tect.gradle.shadow.detect.ShadowApiFinder;
import com.arc_e_tect.gradle.shadow.model.Endpoint;
import com.arc_e_tect.gradle.shadow.openapi.DescribedEndpoint;
import com.arc_e_tect.gradle.shadow.openapi.OpenApiEndpointCollector;
import com.arc_e_tect.gradle.shadow.report.ShadowApiReportWriter;
import com.arc_e_tect.gradle.shadow.scan.ControllerScanner;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Gradle task that scans {@code @RestController} classes, compares the endpoints they expose
 * against the configured OpenAPI documentation, and writes an AsciiDoc report of every endpoint
 * that is not described - the "shadow APIs".
 *
 * <p>Registered automatically by {@link ShadowApiDetectorPlugin} under the name
 * {@code detectShadowApis}.</p>
 */
@DisableCachingByDefault(because = "Report depends on source and OpenAPI document content and is cheap to regenerate")
public abstract class DetectShadowApisTask extends DefaultTask {

    /**
     * Directories to search recursively for {@code @RestController} classes.
     *
     * @return mutable file collection of controller source directories
     */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getControllerDirs();

    /**
     * The root OpenAPI document describing the API.
     *
     * @return mutable file property for the root OpenAPI document
     */
    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getRootDocument();

    /**
     * Directory where OpenAPI descriptions are stored, tracked so that changes to any document
     * reachable from {@link #getRootDocument()} invalidate the task's cached result.
     *
     * @return mutable directory property for the OpenAPI description directory
     */
    @Optional
    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getOpenApiDir();

    /**
     * Whether the build should fail when shadow APIs are found.
     *
     * @return mutable boolean property controlling whether the build fails on shadow APIs
     */
    @Input
    public abstract Property<Boolean> getFailOnShadow();

    /**
     * Directory the AsciiDoc report is written to.
     *
     * @return mutable directory property for the report output directory
     */
    @OutputDirectory
    public abstract DirectoryProperty getReportDir();

    /**
     * Name of the generated AsciiDoc report file (without path).
     *
     * @return mutable string property for the report file name
     */
    @Input
    public abstract Property<String> getReportFileName();

    /**
     * Version of the system under test whose {@code @RestController} classes were scanned, printed
     * in the generated report as e.g. {@code System Under Test version: v1.0.0}.
     *
     * @return mutable string property for the system-under-test version
     */
    @Input
    public abstract Property<String> getSystemUnderTestVersion();

    /**
     * Creates the task. Instantiated by Gradle infrastructure via {@link javax.inject.Inject}.
     */
    @Inject
    public DetectShadowApisTask() {
        setGroup("verification");
        setDescription("Scans @RestController classes and reports endpoints not described in the OpenAPI documentation.");
    }

    /**
     * Task action: scans the configured controller directories, loads the configured OpenAPI
     * documentation, writes the shadow API report, and - when {@link #getFailOnShadow()} is
     * {@code true} - fails the build if any shadow API was found.
     */
    @TaskAction
    public void generate() {
        if (!getRootDocument().isPresent()) {
            throw new GradleException("shadowApiDetector: rootDocument must be configured - "
                    + "it is the required root OpenAPI document.");
        }

        ControllerScanner scanner = new ControllerScanner();
        List<Endpoint> endpoints = new ArrayList<>();
        for (File dir : getControllerDirs()) {
            for (File javaFile : collectJavaFiles(dir)) {
                scanFile(scanner, javaFile, endpoints);
            }
        }

        File rootDocument = getRootDocument().getAsFile().get();
        List<DescribedEndpoint> described = new OpenApiEndpointCollector().collect(rootDocument);

        List<Endpoint> shadows = new ShadowApiFinder().findShadows(endpoints, described);

        File outputDir = getReportDir().getAsFile().get();
        File outputFile = new File(outputDir, getReportFileName().get());
        try {
            new ShadowApiReportWriter().write(
                    outputFile, endpoints.size(), shadows, getSystemUnderTestVersion().get());
        } catch (IOException e) {
            throw new GradleException("shadowApiDetector: failed to write report to " + outputFile, e);
        }

        getLogger().lifecycle("Shadow API Detector: scanned {} endpoint(s), found {} shadow API(s). Report → {}",
                endpoints.size(), shadows.size(), outputFile);

        if (!shadows.isEmpty() && getFailOnShadow().get()) {
            throw new GradleException("shadowApiDetector: found " + shadows.size()
                    + " shadow API(s) not described in the OpenAPI documentation. See " + outputFile);
        }
    }

    private List<File> collectJavaFiles(File dir) {
        List<File> files = new ArrayList<>();
        collectJavaFiles(dir, files);
        return files;
    }

    private void collectJavaFiles(File dir, List<File> files) {
        if (!dir.isDirectory()) {
            return;
        }
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (child.isFile() && child.getName().endsWith(".java")) {
                files.add(child);
            } else if (child.isDirectory()) {
                collectJavaFiles(child, files);
            }
        }
    }

    private void scanFile(ControllerScanner scanner, File file, List<Endpoint> endpoints) {
        try {
            endpoints.addAll(scanner.scan(file));
        } catch (IOException e) {
            throw new GradleException("shadowApiDetector: failed to scan " + file, e);
        }
    }
}

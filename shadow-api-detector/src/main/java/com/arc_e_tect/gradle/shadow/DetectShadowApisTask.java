package com.arc_e_tect.gradle.shadow;

import com.arc_e_tect.gradle.detector.core.console.ScanProgressReporter;
import com.arc_e_tect.gradle.detector.core.model.Endpoint;
import com.arc_e_tect.gradle.detector.core.openapi.DescribedEndpoint;
import com.arc_e_tect.gradle.detector.core.openapi.OpenApiEndpointCollector;
import com.arc_e_tect.gradle.detector.core.progress.ContractHistoryStore;
import com.arc_e_tect.gradle.detector.core.progress.ContractHistoryUpdater;
import com.arc_e_tect.gradle.detector.core.progress.ContractProgressRecord;
import com.arc_e_tect.gradle.detector.core.scan.ControllerScanner;
import com.arc_e_tect.gradle.shadow.detect.ShadowApiFinder;
import com.arc_e_tect.gradle.shadow.report.ShadowApiReportWriter;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
     * Whether to persist, across builds, a history of when each endpoint first reached each stage
     * of its contract lifecycle - declared, implemented, verified.
     *
     * @return mutable boolean property controlling whether contract progress history is tracked
     */
    @Input
    public abstract Property<Boolean> getTrackContractHistory();

    /**
     * File that the persisted contract progress history is read from and, when
     * {@link #getUpdateContractHistory()} is {@code true}, written back to. Deliberately not
     * declared as an {@code @InputFile}/{@code @OutputFile}: the file legitimately may not exist yet
     * (treated as an empty history, not an error) and is only conditionally written back, so it's
     * read and written directly in {@link #generate()} instead of through Gradle's up-to-date
     * checking.
     *
     * @return mutable file property for the contract history file
     */
    @Internal
    public abstract RegularFileProperty getContractHistoryFile();

    /**
     * Whether {@link #getContractHistoryFile()} is written back to disk after being updated with the
     * current run's endpoints. Only consulted when {@link #getTrackContractHistory()} is
     * {@code true}; the history file is always read regardless.
     *
     * @return mutable boolean property controlling whether the contract history file is written back
     */
    @Input
    public abstract Property<Boolean> getUpdateContractHistory();

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

        List<File> controllerFiles = new ArrayList<>();
        for (File dir : getControllerDirs()) {
            controllerFiles.addAll(collectJavaFiles(dir));
        }

        ControllerScanner scanner = new ControllerScanner();
        List<Endpoint> endpoints = new ArrayList<>();
        ScanProgressReporter controllerScanProgress = ScanProgressReporter.determinate(
                getLogger(), "Scanning @RestController classes", controllerFiles.size());
        for (File javaFile : controllerFiles) {
            scanFile(scanner, javaFile, endpoints);
            controllerScanProgress.step();
        }
        controllerScanProgress.complete();

        File rootDocument = getRootDocument().getAsFile().get();
        ScanProgressReporter openApiProgress =
                ScanProgressReporter.indeterminate(getLogger(), "Resolving OpenAPI documents");
        List<DescribedEndpoint> described = new OpenApiEndpointCollector()
                .collect(rootDocument, file -> openApiProgress.step());
        openApiProgress.complete();

        List<Endpoint> shadows = new ShadowApiFinder().findShadows(endpoints, described);

        Map<String, ContractProgressRecord> contractHistory = getTrackContractHistory().get()
                ? updateContractHistory(endpoints, described) : Map.of();

        File outputDir = getReportDir().getAsFile().get();
        File outputFile = new File(outputDir, getReportFileName().get());
        try {
            new ShadowApiReportWriter().write(
                    outputFile, endpoints.size(), shadows, getSystemUnderTestVersion().get(), contractHistory);
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

    /**
     * Loads the persisted contract progress history, advances it with the current run's implemented
     * and declared endpoints (Shadow API Detector never has verification evidence to offer), and -
     * only when {@link #getUpdateContractHistory()} resolves to {@code true} - saves it back. The
     * history file is always read regardless of {@link #getUpdateContractHistory()}, so the
     * generated report reflects the up-to-date-in-memory history even on a run that doesn't persist
     * it.
     */
    private Map<String, ContractProgressRecord> updateContractHistory(
            List<Endpoint> implementedNow, List<DescribedEndpoint> declaredNow) {
        File historyFile = getContractHistoryFile().getAsFile().get();
        ContractHistoryStore store = new ContractHistoryStore();
        Map<String, ContractProgressRecord> previous = store.load(historyFile);
        Map<String, ContractProgressRecord> updated =
                new ContractHistoryUpdater().update(previous, implementedNow, declaredNow, null, Instant.now());
        if (getUpdateContractHistory().get()) {
            store.save(historyFile, updated.values());
        }
        return updated;
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

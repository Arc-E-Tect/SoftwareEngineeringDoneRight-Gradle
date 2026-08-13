package com.arc_e_tect.gradle.mirage;

import com.arc_e_tect.gradle.detector.core.console.ScanProgressReporter;
import com.arc_e_tect.gradle.detector.core.model.Endpoint;
import com.arc_e_tect.gradle.detector.core.openapi.DescribedEndpoint;
import com.arc_e_tect.gradle.detector.core.openapi.OpenApiEndpointCollector;
import com.arc_e_tect.gradle.detector.core.progress.ContractHistoryStore;
import com.arc_e_tect.gradle.detector.core.progress.ContractHistoryUpdater;
import com.arc_e_tect.gradle.detector.core.progress.ContractProgressRecord;
import com.arc_e_tect.gradle.detector.core.scan.ControllerScanner;
import com.arc_e_tect.gradle.mirage.detect.MirageApiFinder;
import com.arc_e_tect.gradle.mirage.report.MirageApiReportWriter;
import com.arc_e_tect.gradle.mirage.scan.WireMockStubScanner;
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
 * Gradle task that parses the configured OpenAPI documentation, compares the operations it
 * describes against a set of implemented endpoints, and writes an AsciiDoc report of every
 * operation that has no match - the "mirage APIs". The implemented-endpoint set is either the
 * endpoints exposed by scanned {@code @RestController} classes (the default), or, when
 * {@link #getScanMocks()} is {@code true}, the requests stubbed by WireMock mapping files.
 *
 * <p>Registered automatically by {@link MirageApiDetectorPlugin} under the name
 * {@code detectMirageApis}.</p>
 */
@DisableCachingByDefault(because = "Report depends on source and OpenAPI document content and is cheap to regenerate")
public abstract class DetectMirageApisTask extends DefaultTask {

    /**
     * Directories to search recursively for {@code @RestController} classes. Not scanned when
     * {@link #getScanMocks()} is {@code true}.
     *
     * @return mutable file collection of controller source directories
     */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getControllerDirs();

    /**
     * Whether to determine implemented endpoints from WireMock stub mapping files under
     * {@link #getStubDirs()} instead of scanning {@code @RestController} classes.
     *
     * @return mutable boolean property controlling whether stub-based scanning is used
     */
    @Input
    public abstract Property<Boolean> getScanMocks();

    /**
     * Directories to search recursively for WireMock stub mapping files, used to determine
     * implemented endpoints when {@link #getScanMocks()} is {@code true}. Not scanned otherwise.
     *
     * @return mutable file collection of WireMock stub directories
     */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getStubDirs();

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
     * Whether the build should fail when mirage APIs are found.
     *
     * @return mutable boolean property controlling whether the build fails on mirage APIs
     */
    @Input
    public abstract Property<Boolean> getFailOnMirage();

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
     * read and written directly in {@link #generate()} instead of through Gradle's file-content-based
     * up-to-date checking. Its configured <em>path</em> - as opposed to the file's content - is still
     * tracked as a plain input via {@link #getContractHistoryFilePath()}, so that renaming or
     * relocating it is itself enough to invalidate this task's up-to-date state.
     *
     * @return mutable file property for the contract history file
     */
    @Internal
    public abstract RegularFileProperty getContractHistoryFile();

    /**
     * The absolute path of {@link #getContractHistoryFile()}, tracked as a plain {@code @Input}
     * value - not the file's content, which {@link #getContractHistoryFile()} itself is deliberately
     * excluded from up-to-date checking for. Without this, renaming or relocating
     * {@code contractHistoryFile} in the build script - with no other configured input having
     * changed - would leave this task {@code UP-TO-DATE} and silently skip writing history to the
     * newly configured location.
     *
     * @return the contract history file's absolute path, or {@code null} if unset
     */
    @Input
    @Optional
    public String getContractHistoryFilePath() {
        return getContractHistoryFile().map(file -> file.getAsFile().getAbsolutePath()).getOrNull();
    }

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
    public DetectMirageApisTask() {
        setGroup("verification");
        setDescription("Scans the OpenAPI documentation and reports operations with no matching @RestController implementation.");
    }

    /**
     * Task action: loads the configured OpenAPI documentation, scans either the configured
     * controller directories or WireMock stub directories (per {@link #getScanMocks()}), writes
     * the mirage API report, and - when {@link #getFailOnMirage()} is {@code true} - fails the
     * build if any mirage API was found.
     */
    @TaskAction
    public void generate() {
        if (!getRootDocument().isPresent()) {
            throw new GradleException("mirageApiDetector: rootDocument must be configured - "
                    + "it is the required root OpenAPI document.");
        }

        boolean scanMocks = getScanMocks().get();
        List<Endpoint> endpoints = scanMocks ? scanStubs() : scanControllers();

        File rootDocument = getRootDocument().getAsFile().get();
        ScanProgressReporter openApiProgress =
                ScanProgressReporter.indeterminate(getLogger(), "Resolving OpenAPI documents");
        List<DescribedEndpoint> described = new OpenApiEndpointCollector()
                .collect(rootDocument, file -> openApiProgress.step());
        openApiProgress.complete();

        List<DescribedEndpoint> mirages = new MirageApiFinder().findMirages(described, endpoints);

        Map<String, ContractProgressRecord> contractHistory = getTrackContractHistory().get()
                ? updateContractHistory(endpoints, described) : Map.of();

        File outputDir = getReportDir().getAsFile().get();
        File outputFile = new File(outputDir, getReportFileName().get());
        try {
            new MirageApiReportWriter().write(outputFile, described.size(), mirages,
                    getSystemUnderTestVersion().get(), scanMocks, contractHistory);
        } catch (IOException e) {
            throw new GradleException("mirageApiDetector: failed to write report to " + outputFile, e);
        }

        getLogger().lifecycle("Mirage API Detector: scanned {} described endpoint(s), found {} mirage API(s). Report → {}",
                described.size(), mirages.size(), outputFile);

        if (!mirages.isEmpty() && getFailOnMirage().get()) {
            String evidenceNoun = scanMocks
                    ? "backed by any WireMock stub"
                    : "implemented by any @RestController class";
            throw new GradleException("mirageApiDetector: found " + mirages.size()
                    + " mirage API(s) described in the OpenAPI documentation but not " + evidenceNoun
                    + ". See " + outputFile);
        }
    }

    /**
     * Loads the persisted contract progress history, advances it with the current run's implemented
     * (controller- or stub-derived) and declared endpoints (Mirage API Detector never has
     * verification evidence to offer), and - only when {@link #getUpdateContractHistory()} resolves
     * to {@code true} - saves it back. The history file is always read regardless of
     * {@link #getUpdateContractHistory()}, so the generated report reflects the up-to-date-in-memory
     * history even on a run that doesn't persist it.
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

    private List<Endpoint> scanControllers() {
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
        return endpoints;
    }

    private List<Endpoint> scanStubs() {
        WireMockStubScanner scanner = new WireMockStubScanner();
        List<Endpoint> endpoints = new ArrayList<>();
        for (File dir : getStubDirs()) {
            try {
                endpoints.addAll(scanner.scan(dir));
            } catch (IOException e) {
                throw new GradleException("mirageApiDetector: failed to scan " + dir, e);
            }
        }
        return endpoints;
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
            throw new GradleException("mirageApiDetector: failed to scan " + file, e);
        }
    }
}

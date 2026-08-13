package com.arc_e_tect.gradle.doppelganger;

import com.arc_e_tect.gradle.detector.core.console.ScanProgressReporter;
import com.arc_e_tect.gradle.detector.core.detect.ContractSetOperations;
import com.arc_e_tect.gradle.detector.core.model.Endpoint;
import com.arc_e_tect.gradle.detector.core.openapi.DescribedEndpoint;
import com.arc_e_tect.gradle.detector.core.openapi.OpenApiEndpointCollector;
import com.arc_e_tect.gradle.detector.core.progress.ContractHistoryStore;
import com.arc_e_tect.gradle.detector.core.progress.ContractHistoryUpdater;
import com.arc_e_tect.gradle.detector.core.progress.ContractProgressRecord;
import com.arc_e_tect.gradle.detector.core.scan.ControllerScanner;
import com.arc_e_tect.gradle.doppelganger.detect.ContractVerificationSource;
import com.arc_e_tect.gradle.doppelganger.detect.DoppelgangerApiFinder;
import com.arc_e_tect.gradle.doppelganger.report.DoppelgangerApiReportWriter;
import com.arc_e_tect.gradle.doppelganger.scan.OpenApiRequestValidatorScanner;
import com.arc_e_tect.gradle.doppelganger.scan.RestDocsScanner;
import com.arc_e_tect.gradle.doppelganger.scan.SpringCloudContractScanner;
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
 * Gradle task that compares endpoints both declared in the configured OpenAPI documentation and
 * implemented by scanned {@code @RestController} classes against verification evidence collected
 * from the enabled {@link ContractVerificationSource}s, and writes an AsciiDoc report of every
 * endpoint with no such evidence - the "doppelganger APIs".
 *
 * <p>Registered automatically by {@link DoppelgangerApiDetectorPlugin} under the name
 * {@code detectDoppelgangerApis}.</p>
 */
@DisableCachingByDefault(because = "Report depends on source, test, contract, and OpenAPI document content and is cheap to regenerate")
public abstract class DetectDoppelgangerApisTask extends DefaultTask {

    /**
     * Directories to search recursively for {@code @RestController} classes.
     *
     * @return mutable file collection of controller source directories
     */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getControllerDirs();

    /**
     * Directories to search recursively for test classes, scanned by the Spring RestDocs and
     * OpenAPI request validator verification sources when enabled.
     *
     * @return mutable file collection of test source directories
     */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getTestDirs();

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
     * Directory searched for Spring Cloud Contract DSL files when
     * {@link #getUseSpringCloudContract()} is {@code true}.
     *
     * <p>Validated as {@code @InputFiles} rather than {@code @InputDirectory}: most projects have
     * no {@code contracts} directory at all, and unlike {@code @InputDirectory}, {@code @InputFiles}
     * does not require the configured directory to actually exist.</p>
     *
     * @return mutable directory property for the Spring Cloud Contract directory
     */
    @Optional
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getContractsDir();

    /**
     * Whether to treat Spring RestDocs test methods as verification evidence.
     *
     * @return mutable boolean property controlling whether the Spring RestDocs source is enabled
     */
    @Input
    public abstract Property<Boolean> getUseRestDocs();

    /**
     * Whether to treat Atlassian OpenAPI request validator usage as verification evidence.
     *
     * @return mutable boolean property controlling whether the OpenAPI request validator source
     *         is enabled
     */
    @Input
    public abstract Property<Boolean> getUseOpenApiRequestValidator();

    /**
     * Whether to treat Spring Cloud Contract DSL files as verification evidence.
     *
     * @return mutable boolean property controlling whether the Spring Cloud Contract source is
     *         enabled
     */
    @Input
    public abstract Property<Boolean> getUseSpringCloudContract();

    /**
     * Whether the build should fail when doppelganger APIs are found.
     *
     * @return mutable boolean property controlling whether the build fails on doppelganger APIs
     */
    @Input
    public abstract Property<Boolean> getFailOnDoppelganger();

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
    public DetectDoppelgangerApisTask() {
        setGroup("verification");
        setDescription("Scans OpenAPI documentation, @RestController implementations, and test-level "
                + "verification evidence, and reports endpoints that are declared and implemented but never "
                + "verified against their contract.");
    }

    /**
     * Task action: scans the configured controller directories and OpenAPI documentation to find
     * endpoints both declared and implemented, scans the enabled verification sources, writes the
     * doppelganger API report, and - when {@link #getFailOnDoppelganger()} is {@code true} - fails
     * the build if any doppelganger API was found.
     */
    @TaskAction
    public void generate() {
        if (!getRootDocument().isPresent()) {
            throw new GradleException("doppelgangerApiDetector: rootDocument must be configured - "
                    + "it is the required root OpenAPI document.");
        }

        int totalPhases = countTotalPhases();
        int phase = 0;

        phase = announcePhase(phase, totalPhases, "Scanning @RestController classes...");
        List<File> controllerFiles = new ArrayList<>();
        for (File dir : getControllerDirs()) {
            controllerFiles.addAll(collectJavaFiles(dir));
        }
        ControllerScanner controllerScanner = new ControllerScanner();
        List<Endpoint> implemented = new ArrayList<>();
        ScanProgressReporter controllerScanProgress = ScanProgressReporter.determinate(
                getLogger(), "Scanning @RestController classes", controllerFiles.size());
        for (File javaFile : controllerFiles) {
            try {
                implemented.addAll(controllerScanner.scan(javaFile));
            } catch (IOException e) {
                throw new GradleException("doppelgangerApiDetector: failed to scan " + javaFile, e);
            }
            controllerScanProgress.step();
        }
        controllerScanProgress.complete();

        phase = announcePhase(phase, totalPhases, "Collecting OpenAPI endpoints...");
        File rootDocument = getRootDocument().getAsFile().get();
        ScanProgressReporter openApiProgress =
                ScanProgressReporter.indeterminate(getLogger(), "Resolving OpenAPI documents");
        List<DescribedEndpoint> described = new OpenApiEndpointCollector()
                .collect(rootDocument, file -> openApiProgress.step());
        openApiProgress.complete();

        List<Endpoint> declaredAndImplemented = ContractSetOperations.intersection(implemented, described);

        List<Endpoint> verified = collectVerifiedEndpoints(phase, totalPhases);

        List<Endpoint> doppelgangers = new DoppelgangerApiFinder()
                .findDoppelgangers(declaredAndImplemented, verified);

        Map<String, ContractProgressRecord> contractHistory = getTrackContractHistory().get()
                ? updateContractHistory(implemented, described, verified) : Map.of();

        File outputDir = getReportDir().getAsFile().get();
        File outputFile = new File(outputDir, getReportFileName().get());
        try {
            new DoppelgangerApiReportWriter().write(outputFile, declaredAndImplemented.size(), doppelgangers,
                    getSystemUnderTestVersion().get(), contractHistory);
        } catch (IOException e) {
            throw new GradleException("doppelgangerApiDetector: failed to write report to " + outputFile, e);
        }

        getLogger().lifecycle(
                "Doppelganger API Detector: scanned {} declared-and-implemented endpoint(s), found {} "
                        + "doppelganger API(s). Report → {}",
                declaredAndImplemented.size(), doppelgangers.size(), outputFile);

        if (!doppelgangers.isEmpty() && getFailOnDoppelganger().get()) {
            throw new GradleException("doppelgangerApiDetector: found " + doppelgangers.size()
                    + " doppelganger API(s) declared and implemented but not verified by any configured "
                    + "contract verification source. See " + outputFile);
        }
    }

    /**
     * Loads the persisted contract progress history, advances it with the current run's implemented,
     * declared, and verified endpoints - Doppelganger API Detector is the only one of the three
     * plugins that always has verification evidence to offer, even when every source is disabled
     * (an empty list, distinct from offering no evidence at all) - and, only when
     * {@link #getUpdateContractHistory()} resolves to {@code true}, saves it back. The history file
     * is always read regardless of {@link #getUpdateContractHistory()}, so the generated report
     * reflects the up-to-date-in-memory history even on a run that doesn't persist it.
     */
    private Map<String, ContractProgressRecord> updateContractHistory(
            List<Endpoint> implementedNow, List<DescribedEndpoint> declaredNow, List<Endpoint> verifiedNow) {
        File historyFile = getContractHistoryFile().getAsFile().get();
        ContractHistoryStore store = new ContractHistoryStore();
        Map<String, ContractProgressRecord> previous = store.load(historyFile);
        Map<String, ContractProgressRecord> updated = new ContractHistoryUpdater()
                .update(previous, implementedNow, declaredNow, verifiedNow, Instant.now());
        if (getUpdateContractHistory().get()) {
            store.save(historyFile, updated.values());
        }
        return updated;
    }

    private List<Endpoint> collectVerifiedEndpoints(int phase, int totalPhases) {
        List<Endpoint> verified = new ArrayList<>();
        try {
            if (getUseRestDocs().get()) {
                phase = announcePhase(phase, totalPhases, "Scanning Spring RestDocs verification evidence...");
                verified.addAll(scanTestDirs(new RestDocsScanner()));
            }
            if (getUseOpenApiRequestValidator().get()) {
                phase = announcePhase(phase, totalPhases,
                        "Scanning OpenAPI request validator verification evidence...");
                verified.addAll(scanTestDirs(new OpenApiRequestValidatorScanner()));
            }
            if (getUseSpringCloudContract().get() && getContractsDir().isPresent()) {
                announcePhase(phase, totalPhases, "Scanning Spring Cloud Contract verification evidence...");
                File contractsDir = getContractsDir().getAsFile().get();
                verified.addAll(new SpringCloudContractScanner().scan(contractsDir));
            }
        } catch (IOException e) {
            throw new GradleException("doppelgangerApiDetector: failed to scan verification evidence", e);
        }
        return verified;
    }

    /**
     * Total number of phases {@link #generate()} will announce, computed from which verification
     * sources are actually enabled and configured so the banner's {@code [n/total]} count matches
     * the work that will really run.
     *
     * @return the total phase count for this invocation
     */
    private int countTotalPhases() {
        int total = 2;
        if (getUseRestDocs().get()) {
            total++;
        }
        if (getUseOpenApiRequestValidator().get()) {
            total++;
        }
        if (getUseSpringCloudContract().get() && getContractsDir().isPresent()) {
            total++;
        }
        return total;
    }

    /**
     * Emits a one-line {@code LIFECYCLE} phase banner, e.g.
     * {@code "Doppelganger API Detector: [2/5] Collecting OpenAPI endpoints..."}, and returns the
     * incremented phase number.
     *
     * @param phase       the previous phase number (0 before the first phase)
     * @param totalPhases total number of phases, from {@link #countTotalPhases()}
     * @param phaseLabel  human-readable description of the phase that is starting
     * @return {@code phase + 1}
     */
    private int announcePhase(int phase, int totalPhases, String phaseLabel) {
        int nextPhase = phase + 1;
        getLogger().lifecycle("Doppelganger API Detector: [{}/{}] {}", nextPhase, totalPhases, phaseLabel);
        return nextPhase;
    }

    private List<Endpoint> scanTestDirs(ContractVerificationSource source) throws IOException {
        List<Endpoint> results = new ArrayList<>();
        for (File testDir : getTestDirs()) {
            results.addAll(source.scan(testDir));
        }
        return results;
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
}

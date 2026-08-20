package com.arc_e_tect.gradle.mirage;

import com.arc_e_tect.gradle.detector.core.console.ScanProgressReporter;
import com.arc_e_tect.gradle.detector.core.model.Endpoint;
import com.arc_e_tect.gradle.detector.core.model.PathTemplates;
import com.arc_e_tect.gradle.detector.core.openapi.DescribedEndpoint;
import com.arc_e_tect.gradle.detector.core.openapi.OpenApiEndpointCollector;
import com.arc_e_tect.gradle.detector.core.progress.ContractHistoryStore;
import com.arc_e_tect.gradle.detector.core.progress.ContractHistoryUpdater;
import com.arc_e_tect.gradle.detector.core.progress.ContractProgressRecord;
import com.arc_e_tect.gradle.detector.core.progress.LegacyContractHistoryFormatException;
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
 * describes against the endpoints exposed by scanned {@code @RestController} classes, and writes
 * an AsciiDoc report of every operation that has no match - the "mirage APIs". When
 * {@link #getScanMocks()} is {@code true}, WireMock stub mapping files are additionally scanned
 * for stub evidence, recorded into contract history/the report alongside the real implementation
 * evidence above - but never counted as implementation evidence itself, so it never changes which
 * endpoints are reported as mirage APIs.
 *
 * <p>Registered automatically by {@link MirageApiDetectorPlugin} under the name
 * {@code detectMirageApis}.</p>
 */
@DisableCachingByDefault(because = "Report depends on source and OpenAPI document content and is cheap to regenerate")
public abstract class DetectMirageApisTask extends DefaultTask {

    /**
     * Directories to search recursively for {@code @RestController} classes. Always scanned,
     * regardless of {@link #getScanMocks()}: real implementation evidence is what determines
     * which endpoints are reported as mirage APIs.
     *
     * @return mutable file collection of controller source directories
     */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getControllerDirs();

    /**
     * Whether to additionally scan WireMock stub mapping files under {@link #getStubDirs()} for
     * stub evidence, recorded into contract history/the report alongside
     * {@link #getControllerDirs()}'s real implementation evidence. Stub evidence never counts as
     * implementation evidence itself - it never changes which endpoints are reported as mirage
     * APIs, only what {@code stubbedAt} the contract history/report show for them.
     *
     * @return mutable boolean property controlling whether stub scanning is additionally performed
     */
    @Input
    public abstract Property<Boolean> getScanMocks();

    /**
     * Directories to search recursively for WireMock stub mapping files, scanned for stub
     * evidence when {@link #getScanMocks()} is {@code true}. Not scanned otherwise.
     *
     * @return mutable file collection of WireMock stub directories
     */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getStubDirs();

    /**
     * The base path to strip from every path found under {@link #getStubDirs()} before comparing
     * it against the OpenAPI documentation, used only when {@link #getScanMocks()} is
     * {@code true}. When unset, falls back to {@link #getRootDocument()}'s own first
     * {@code servers} entry's {@code url} at task-execution time - see
     * {@link MirageApiDetectorExtension#getBasePath()} for the full explanation.
     *
     * @return mutable string property for the base path to strip from scanned stub paths
     */
    @Input
    @Optional
    public abstract Property<String> getBasePath();

    /**
     * The root OpenAPI document describing the API.
     *
     * <p>Validated as {@code @InputFiles} rather than {@code @InputFile}, and {@code @Optional}:
     * unlike {@code @InputFile}, neither requires a value to be present nor the configured file to
     * actually exist. A team bootstrapping a build script for a project whose OpenAPI documentation
     * doesn't exist yet - or hasn't been configured yet - should get a report with a
     * {@code WARNING} admonition explaining that from {@link #generate()}, not an opaque Gradle
     * input-validation failure before the task action ever runs.</p>
     *
     * @return mutable file property for the root OpenAPI document
     */
    @Optional
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getRootDocument();

    /**
     * Directory where OpenAPI descriptions are stored, tracked so that changes to any document
     * reachable from {@link #getRootDocument()} invalidate the task's cached result.
     *
     * <p>Validated as {@code @InputFiles} rather than {@code @InputDirectory} for the same reason as
     * {@link #getRootDocument()}: it defaults to that file's own parent directory, which does not
     * exist either when the file itself does not.</p>
     *
     * @return mutable directory property for the OpenAPI description directory
     */
    @Optional
    @InputFiles
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
     * Task action: loads the configured OpenAPI documentation, scans the configured controller
     * directories - and, when {@link #getScanMocks()} is {@code true}, additionally the configured
     * WireMock stub directories - and writes the mirage API report.
     *
     * <p>A missing {@link #getRootDocument()} or empty {@link #getControllerDirs()} - e.g. a build
     * script bootstrapped for a project whose OpenAPI documentation or {@code @RestController}
     * classes don't exist yet - is <em>not</em> a build failure: it is recorded as a
     * {@code WARNING} admonition in the generated report instead, and mirage API detection (and,
     * deliberately, contract history advancement - see {@link #loadContractHistoryForDisplay()}) is
     * skipped for this run rather than computed from incomplete input and risking a false positive.
     * A missing {@link #getStubDirs()} entry (only consulted when {@link #getScanMocks()} is
     * {@code true}) only warns - it never suppresses detection, since stub evidence never
     * determines which endpoints are reported as mirage APIs.</p>
     *
     * <p>The build still fails when a mirage API is genuinely found and
     * {@link #getFailOnMirage()} is {@code true} - the one failure condition this task ever raises
     * on its own initiative, per this plugin's design: fail only on a real finding the DSL asked to
     * fail on, never on merely incomplete input.</p>
     */
    @TaskAction
    public void generate() {
        List<String> warnings = new ArrayList<>();

        List<File> missingControllerDirs = new ArrayList<>();
        List<File> controllerFiles = new ArrayList<>();
        boolean anyControllerDirExists = false;
        for (File dir : getControllerDirs()) {
            if (dir.isDirectory()) {
                anyControllerDirExists = true;
                controllerFiles.addAll(collectJavaFiles(dir));
            } else {
                missingControllerDirs.add(dir);
            }
        }
        // Deliberately distinct from "controllerDirs has zero entries at all", which is a valid,
        // complete input (nothing to scan by design - e.g. a stub-only setup that only configures
        // stubDirs) rather than a bootstrapping gap, and must not silently skip detection below.
        boolean controllerSourceMissing = !missingControllerDirs.isEmpty() && !anyControllerDirExists;
        if (!missingControllerDirs.isEmpty()) {
            if (anyControllerDirExists) {
                for (File dir : missingControllerDirs) {
                    warnings.add("Configured `controllerDirs` entry does not exist yet: `" + dir + "`.");
                }
            } else {
                warnings.add("None of the configured `controllerDirs` exist yet. Mirage API detection was "
                        + "skipped for this run - once at least one exists, re-run this task to check it.");
            }
        }

        ControllerScanner controllerScanner = new ControllerScanner();
        List<Endpoint> controllerEndpoints = new ArrayList<>();
        ScanProgressReporter controllerScanProgress = ScanProgressReporter.determinate(
                getLogger(), "Scanning @RestController classes", controllerFiles.size());
        for (File javaFile : controllerFiles) {
            scanFile(controllerScanner, javaFile, controllerEndpoints);
            controllerScanProgress.step();
        }
        controllerScanProgress.complete();

        OpenApiEndpointCollector openApiCollector = new OpenApiEndpointCollector();
        boolean openApiAvailable = isRootDocumentAvailable();
        File rootDocument = openApiAvailable ? getRootDocument().getAsFile().get() : null;

        boolean scanMocks = getScanMocks().get();
        List<Endpoint> stubEndpoints = scanMocks ? scanStubs(openApiCollector, rootDocument, warnings) : null;

        List<DescribedEndpoint> described;
        if (openApiAvailable) {
            ScanProgressReporter openApiProgress =
                    ScanProgressReporter.indeterminate(getLogger(), "Resolving OpenAPI documents");
            described = openApiCollector.collect(rootDocument, file -> openApiProgress.step());
            openApiProgress.complete();
        } else {
            described = List.of();
            warnings.add(describeMissingRootDocument());
        }

        boolean inputComplete = openApiAvailable && !controllerSourceMissing;
        List<DescribedEndpoint> mirages = inputComplete
                ? new MirageApiFinder().findMirages(described, controllerEndpoints) : List.of();

        Map<String, ContractProgressRecord> contractHistory = !getTrackContractHistory().get() ? Map.of()
                : inputComplete ? updateContractHistory(controllerEndpoints, stubEndpoints, described)
                        : loadContractHistoryForDisplay();

        File outputDir = getReportDir().getAsFile().get();
        File outputFile = new File(outputDir, getReportFileName().get());
        try {
            new MirageApiReportWriter().write(outputFile, described.size(), mirages,
                    getSystemUnderTestVersion().get(), warnings, contractHistory);
        } catch (IOException e) {
            throw new GradleException("mirageApiDetector: failed to write report to " + outputFile, e);
        }

        getLogger().lifecycle("Mirage API Detector: scanned {} described endpoint(s), found {} mirage API(s). Report → {}",
                described.size(), mirages.size(), outputFile);

        if (!mirages.isEmpty() && getFailOnMirage().get()) {
            throw new GradleException("mirageApiDetector: found " + mirages.size()
                    + " mirage API(s) described in the OpenAPI documentation but not implemented by any "
                    + "@RestController class. See " + outputFile);
        }
    }

    /**
     * Whether {@link #getRootDocument()} is both configured and points to a file that actually
     * exists - the precondition for OpenAPI-based comparison being meaningful at all.
     */
    private boolean isRootDocumentAvailable() {
        return getRootDocument().isPresent() && getRootDocument().getAsFile().get().isFile();
    }

    /**
     * The {@code WARNING} admonition line explaining why mirage API detection was skipped -
     * distinguishing "never configured" from "configured, but the file doesn't exist yet", since the
     * former needs no path in the message and the latter does.
     */
    private String describeMissingRootDocument() {
        if (!getRootDocument().isPresent()) {
            return "`rootDocument` is not configured yet. Mirage API detection was skipped for this run - "
                    + "configure it once your OpenAPI documentation exists.";
        }
        return "The configured `rootDocument` does not exist yet: `" + getRootDocument().getAsFile().get()
                + "`. Mirage API detection was skipped for this run - once the file exists, re-run this task "
                + "to check it.";
    }

    /**
     * Loads {@link #getContractHistoryFile()} as-is, for display in the report's
     * {@code == Progress Over Time} section, without advancing or saving it. Used in place of
     * {@link #updateContractHistory(List, List, List)} whenever this run's input is incomplete (see
     * {@link #generate()}): advancing history from a partial view - e.g. an empty
     * {@code implementedNow} purely because {@link #getControllerDirs()} doesn't exist yet, not
     * because every previously-implemented endpoint was actually removed - would misrepresent every
     * endpoint this run couldn't see as newly removed. The persisted file itself is left untouched
     * either way; only the report's display of it is affected.
     */
    private Map<String, ContractProgressRecord> loadContractHistoryForDisplay() {
        File historyFile = getContractHistoryFile().getAsFile().get();
        try {
            return new ContractHistoryStore().load(historyFile);
        } catch (LegacyContractHistoryFormatException e) {
            throw new GradleException("mirageApiDetector: " + historyFile + " is in the old 9-field "
                    + "contract history format (missing 'stubbedAt'). Run the 'migrateContractHistory' task "
                    + "to upgrade it in place, or point contractHistoryFile at a new location to start a "
                    + "fresh history.", e);
        }
    }

    /**
     * Loads the persisted contract progress history, advances it with the current run's declared,
     * implemented (controller-derived), and - when {@link #getScanMocks()} is {@code true} -
     * stubbed (WireMock-derived) endpoints (Mirage API Detector never has verification evidence to
     * offer), and - only when {@link #getUpdateContractHistory()} resolves to {@code true} - saves
     * it back. The history file is always read regardless of {@link #getUpdateContractHistory()},
     * so the generated report reflects the up-to-date-in-memory history even on a run that doesn't
     * persist it.
     *
     * @param implementedNow the current run's scanned {@code @RestController} matches
     * @param stubbedNow     the current run's scanned WireMock stub matches, or {@code null} when
     *                       {@link #getScanMocks()} is {@code false} - stub evidence was not
     *                       gathered this run at all, as opposed to an empty list, which means it
     *                       was gathered and none was found
     * @param declaredNow    the current run's declared endpoints, from the OpenAPI documentation
     */
    private Map<String, ContractProgressRecord> updateContractHistory(
            List<Endpoint> implementedNow, List<Endpoint> stubbedNow, List<DescribedEndpoint> declaredNow) {
        File historyFile = getContractHistoryFile().getAsFile().get();
        ContractHistoryStore store = new ContractHistoryStore();
        Map<String, ContractProgressRecord> previous;
        try {
            previous = store.load(historyFile);
        } catch (LegacyContractHistoryFormatException e) {
            throw new GradleException("mirageApiDetector: " + historyFile + " is in the old 9-field "
                    + "contract history format (missing 'stubbedAt'). Run the 'migrateContractHistory' task "
                    + "to upgrade it in place, or point contractHistoryFile at a new location to start a "
                    + "fresh history.", e);
        }
        Map<String, ContractProgressRecord> updated = new ContractHistoryUpdater()
                .update(previous, implementedNow, declaredNow, null, stubbedNow, Instant.now());
        if (getUpdateContractHistory().get()) {
            store.save(historyFile, updated.values());
        }
        return updated;
    }

    private List<Endpoint> scanStubs(OpenApiEndpointCollector openApiCollector, File rootDocument, List<String> warnings) {
        WireMockStubScanner scanner = new WireMockStubScanner();
        List<Endpoint> endpoints = new ArrayList<>();
        List<File> missingStubDirs = new ArrayList<>();
        boolean anyStubDirExists = false;
        for (File dir : getStubDirs()) {
            if (dir.isDirectory()) {
                anyStubDirExists = true;
            } else {
                missingStubDirs.add(dir);
            }
            try {
                endpoints.addAll(scanner.scan(dir));
            } catch (IOException e) {
                throw new GradleException("mirageApiDetector: failed to scan " + dir, e);
            }
        }
        if (!missingStubDirs.isEmpty()) {
            if (anyStubDirExists) {
                for (File dir : missingStubDirs) {
                    warnings.add("Configured `stubDirs` entry does not exist yet: `" + dir + "`.");
                }
            } else {
                warnings.add("None of the configured `stubDirs` exist yet, so no WireMock stub evidence "
                        + "could be gathered for this run.");
            }
        }

        String basePath = resolveBasePath(openApiCollector, rootDocument);
        if (basePath == null) {
            return endpoints;
        }
        List<Endpoint> stripped = new ArrayList<>(endpoints.size());
        for (Endpoint endpoint : endpoints) {
            stripped.add(new Endpoint(endpoint.verb(), PathTemplates.stripBasePath(endpoint.path(), basePath),
                    endpoint.declaringClass(), endpoint.methodSignature(), endpoint.sourceFile(), endpoint.lineNumber()));
        }
        return stripped;
    }

    /**
     * The base path to strip from every stub-scanned path: {@link #getBasePath()} when explicitly
     * configured, otherwise the first {@code servers} entry's {@code url} declared by
     * {@code rootDocument} - see {@link MirageApiDetectorExtension#getBasePath()}.
     * {@code rootDocument} may be {@code null} when the OpenAPI source is unavailable this run (see
     * {@link #isRootDocumentAvailable()}), in which case only the explicit {@link #getBasePath()}
     * property is consulted.
     *
     * @return the resolved base path, or {@code null} when neither source yields one
     */
    private String resolveBasePath(OpenApiEndpointCollector openApiCollector, File rootDocument) {
        if (getBasePath().isPresent() && !getBasePath().get().isBlank()) {
            return getBasePath().get();
        }
        if (rootDocument == null) {
            return null;
        }
        return openApiCollector.firstServerBasePath(rootDocument).orElse(null);
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

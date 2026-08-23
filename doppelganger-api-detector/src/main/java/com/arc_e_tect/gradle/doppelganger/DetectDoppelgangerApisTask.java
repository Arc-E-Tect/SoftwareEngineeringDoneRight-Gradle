package com.arc_e_tect.gradle.doppelganger;

import com.arc_e_tect.gradle.detector.core.console.ScanProgressReporter;
import com.arc_e_tect.gradle.detector.core.detect.ContractSetOperations;
import com.arc_e_tect.gradle.detector.core.exclude.ExclusionFilter;
import com.arc_e_tect.gradle.detector.core.exclude.ExclusionRule;
import com.arc_e_tect.gradle.detector.core.exclude.ExclusionRuleFile;
import com.arc_e_tect.gradle.detector.core.exclude.WellKnownExclusionSets;
import com.arc_e_tect.gradle.detector.core.model.Endpoint;
import com.arc_e_tect.gradle.detector.core.openapi.DescribedEndpoint;
import com.arc_e_tect.gradle.detector.core.openapi.OpenApiEndpointCollector;
import com.arc_e_tect.gradle.detector.core.progress.ContractHistoryStore;
import com.arc_e_tect.gradle.detector.core.progress.ContractHistoryUpdater;
import com.arc_e_tect.gradle.detector.core.progress.ContractProgressRecord;
import com.arc_e_tect.gradle.detector.core.progress.LegacyContractHistoryFormatException;
import com.arc_e_tect.gradle.detector.core.scan.ControllerScanner;
import com.arc_e_tect.gradle.doppelganger.detect.ContractVerificationSource;
import com.arc_e_tect.gradle.doppelganger.detect.DoppelgangerApiFinder;
import com.arc_e_tect.gradle.doppelganger.report.DoppelgangerApiReportWriter;
import com.arc_e_tect.gradle.doppelganger.scan.OpenApiRequestValidatorScanner;
import com.arc_e_tect.gradle.doppelganger.scan.OpenApiServerBasePath;
import com.arc_e_tect.gradle.doppelganger.scan.RestDocsScanner;
import com.arc_e_tect.gradle.doppelganger.scan.SpringCloudContractScanner;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
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
     * Whether {@link #getTestDirs()} holds directories the user actually configured, as opposed to
     * only {@link DoppelgangerApiDetectorExtension#DEFAULT_TEST_DIR} applied by
     * {@link DoppelgangerApiDetectorPlugin} because the user configured none. Set by the plugin;
     * defaults to {@code true} for tasks created without it (e.g. directly in a test), which keeps
     * the conservative, pre-existing behavior of treating a missing {@link #getTestDirs()} entry as
     * a bootstrapping gap.
     *
     * <p>This distinction matters because a missing {@code testDirs} entry means two very different
     * things depending on where it came from. A user-configured path that doesn't exist yet is
     * genuinely a bootstrapping gap - the team is going to add that test directory once the
     * corresponding tests are written - and {@link #generate()} correctly suppresses detection for
     * it, the same as a missing {@link #getContractsDir()}. The plugin's own default, by contrast,
     * is applied whether or not the project will ever have a {@code src/testContract/java} - a
     * project that deliberately has <em>no</em> Spring RestDocs or OpenAPI request validator
     * evidence at all looks, on disk, identical to one that simply hasn't been bootstrapped yet.
     * Suppressing detection in that case defeats the entire plugin for exactly the projects it
     * exists to catch: one with zero verification evidence would never be flagged, because the
     * absence of evidence is misread as "not built yet" rather than "genuinely absent".</p>
     *
     * @return mutable boolean property, {@code true} when {@link #getTestDirs()} reflects the
     *         user's own configuration rather than only the plugin's default
     */
    @Input
    public abstract Property<Boolean> getTestDirsUserConfigured();

    /**
     * The root OpenAPI document describing the API.
     *
     * <p>Validated as {@code @InputFiles} rather than {@code @InputFile}, and {@code @Optional}:
     * unlike {@code @InputFile}, neither requires a value to be present nor the configured file to
     * actually exist. A team bootstrapping a build script for a project whose OpenAPI documentation
     * doesn't exist yet - or hasn't been configured yet - should get a report with a
     * {@code WARNING} admonition explaining that from {@link #generate()}, not an opaque Gradle
     * input-validation failure before the task action ever runs; see
     * {@link #describeMissingRootDocument()}.</p>
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
     * Exclusion rule strings - see {@link DoppelgangerApiDetectorExtension#getExcludePaths()}.
     *
     * @return mutable list property of exclusion rule strings
     */
    @Input
    public abstract ListProperty<String> getExcludePaths();

    /**
     * External exclusion rule files - see
     * {@link DoppelgangerApiDetectorExtension#getExcludeFiles()}.
     *
     * @return mutable file collection of exclusion rule files
     */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getExcludeFiles();

    /**
     * Bundled well-known exclusion set names - see
     * {@link DoppelgangerApiDetectorExtension#getExcludeWellKnown()}.
     *
     * @return mutable list property of well-known exclusion set names
     */
    @Input
    public abstract ListProperty<String> getExcludeWellKnown();

    /**
     * Creates the task. Instantiated by Gradle infrastructure via {@link javax.inject.Inject}.
     */
    @Inject
    public DetectDoppelgangerApisTask() {
        setGroup("verification");
        setDescription("Scans OpenAPI documentation, @RestController implementations, and test-level "
                + "verification evidence, and reports endpoints that are declared and implemented but never "
                + "verified against their contract.");
        getTestDirsUserConfigured().convention(true);
    }

    /**
     * Task action: scans the configured controller directories and OpenAPI documentation to find
     * endpoints both declared and implemented, scans the enabled verification sources, writes the
     * doppelganger API report, and - when {@link #getFailOnDoppelganger()} is {@code true} - fails
     * the build if any doppelganger API was found.
     *
     * <p>A missing {@link #getRootDocument()}, empty {@link #getControllerDirs()}, or - for an
     * enabled verification source - a user-configured {@link #getTestDirs()}/{@link #getContractsDir()}
     * it depends on with none of its currently-enabled sources able to gather any evidence at all - e.g.
     * a build script bootstrapped for a project whose OpenAPI documentation, controllers, or test
     * evidence don't exist yet - is <em>not</em> a build failure: it is recorded as a {@code WARNING}
     * admonition in the generated report instead, and doppelganger API detection (and, deliberately,
     * contract history advancement - see {@link #loadContractHistoryForDisplay()}) is skipped for
     * this run rather than computed from incomplete input and risking a false positive. A missing
     * directory for a source that is <em>not</em> enabled, {@link #getContractsDir()} left unset
     * entirely while {@link #getUseSpringCloudContract()} is enabled, or {@link #getTestDirs()}
     * missing only because it was never configured by the user (see
     * {@link #getTestDirsUserConfigured()}) is a deliberate, complete configuration rather than a
     * gap, and neither warns nor suppresses detection.</p>
     *
     * <p>The build still fails when a doppelganger API is genuinely found and
     * {@link #getFailOnDoppelganger()} is {@code true} - the one failure condition this task ever
     * raises on its own initiative, per this plugin's design: fail only on a real finding the DSL
     * asked to fail on, never on merely incomplete input.</p>
     *
     * <p>Two DSL configurations are rejected eagerly, before anything is scanned, regardless of
     * {@link #getFailOnDoppelganger()} and regardless of what currently exists on disk - unlike
     * every gap above, these are not bootstrapping gaps that resolve themselves as a project fills
     * in, but property combinations that can never yield a meaningful result no matter what: every
     * {@link #getUseRestDocs()}/{@link #getUseOpenApiRequestValidator()}/
     * {@link #getUseSpringCloudContract()} source disabled at once, and
     * {@link #getUseSpringCloudContract()} enabled with {@link #getContractsDir()} left
     * unconfigured.</p>
     */
    @TaskAction
    public void generate() {
        boolean useRestDocs = getUseRestDocs().get();
        boolean useOpenApiRequestValidator = getUseOpenApiRequestValidator().get();
        boolean useSpringCloudContract = getUseSpringCloudContract().get();
        if (!useRestDocs && !useOpenApiRequestValidator && !useSpringCloudContract) {
            throw new GradleException("doppelgangerApiDetector: at least one of useRestDocs, "
                    + "useOpenApiRequestValidator, or useSpringCloudContract must be enabled - with all "
                    + "three disabled, no endpoint could ever be verified, so every declared-and-implemented "
                    + "endpoint would always be reported as a doppelganger API regardless of test coverage.");
        }
        if (useSpringCloudContract && !getContractsDir().isPresent()) {
            throw new GradleException("doppelgangerApiDetector: contractsDir must be configured when "
                    + "useSpringCloudContract is enabled - it has no default location.");
        }

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
        // complete input (nothing to scan by design) rather than a bootstrapping gap, and must not
        // silently skip detection below.
        boolean controllerSourceMissing = !missingControllerDirs.isEmpty() && !anyControllerDirExists;
        if (!missingControllerDirs.isEmpty()) {
            if (anyControllerDirExists) {
                for (File dir : missingControllerDirs) {
                    warnings.add("Configured `controllerDirs` entry does not exist yet: `" + dir + "`.");
                }
            } else {
                warnings.add("None of the configured `controllerDirs` exist yet. Doppelganger API detection "
                        + "was skipped for this run - once at least one exists, re-run this task to check it.");
            }
        }

        int totalPhases = countTotalPhases();
        int phase = 0;

        phase = announcePhase(phase, totalPhases, "Scanning @RestController classes...");
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
        boolean openApiAvailable = isRootDocumentAvailable();
        File rootDocument = openApiAvailable ? getRootDocument().getAsFile().get() : null;
        List<DescribedEndpoint> described;
        if (openApiAvailable) {
            ScanProgressReporter openApiProgress =
                    ScanProgressReporter.indeterminate(getLogger(), "Resolving OpenAPI documents");
            described = new OpenApiEndpointCollector().collect(rootDocument, file -> openApiProgress.step());
            openApiProgress.complete();
        } else {
            described = List.of();
            warnings.add(describeMissingRootDocument());
        }

        List<Endpoint> declaredAndImplemented = ContractSetOperations.intersection(implemented, described);

        VerificationScan verificationScan = collectVerifiedEndpoints(phase, totalPhases, rootDocument, warnings);
        List<Endpoint> verified = verificationScan.endpoints();

        boolean inputComplete =
                openApiAvailable && !controllerSourceMissing && !verificationScan.verificationInputMissing();
        List<Endpoint> doppelgangers = inputComplete
                ? new DoppelgangerApiFinder().findDoppelgangers(declaredAndImplemented, verified) : List.of();

        List<ExclusionRule> exclusionRules = resolveExclusionRules(warnings);
        List<Endpoint> reportableDoppelgangers = ExclusionFilter.excludeMatching(doppelgangers, exclusionRules);
        List<Endpoint> excludedDoppelgangers = ExclusionFilter.onlyMatching(doppelgangers, exclusionRules);

        Map<String, ContractProgressRecord> contractHistory = !getTrackContractHistory().get() ? Map.of()
                : inputComplete ? updateContractHistory(
                        ExclusionFilter.excludeMatching(implemented, exclusionRules),
                        ExclusionFilter.excludeMatching(described, exclusionRules),
                        ExclusionFilter.excludeMatching(verified, exclusionRules))
                        : loadContractHistoryForDisplay();

        File outputDir = getReportDir().getAsFile().get();
        File outputFile = new File(outputDir, getReportFileName().get());
        try {
            new DoppelgangerApiReportWriter().write(outputFile, declaredAndImplemented.size(),
                    reportableDoppelgangers, excludedDoppelgangers, getSystemUnderTestVersion().get(),
                    warnings, contractHistory);
        } catch (IOException e) {
            throw new GradleException("doppelgangerApiDetector: failed to write report to " + outputFile, e);
        }

        getLogger().lifecycle(
                "Doppelganger API Detector: scanned {} declared-and-implemented endpoint(s), found {} "
                        + "doppelganger API(s) ({} excluded). Report → {}",
                declaredAndImplemented.size(), reportableDoppelgangers.size(), excludedDoppelgangers.size(), outputFile);

        if (!reportableDoppelgangers.isEmpty() && getFailOnDoppelganger().get()) {
            throw new GradleException("doppelgangerApiDetector: found " + reportableDoppelgangers.size()
                    + " doppelganger API(s) declared and implemented but not verified by any configured "
                    + "contract verification source. See " + outputFile);
        }
    }

    /**
     * Resolves every configured exclusion rule - {@link #getExcludePaths()},
     * {@link #getExcludeFiles()}, and {@link #getExcludeWellKnown()} - into one combined list. A
     * missing {@code excludeFiles} entry only warns, the same way a missing {@code controllerDirs}
     * or {@code testDirs} entry does; a malformed rule string or an unrecognised well-known set
     * name fails the build outright, since those are build-script/file mistakes, not a
     * "not built yet" bootstrapping gap.
     */
    private List<ExclusionRule> resolveExclusionRules(List<String> warnings) {
        List<ExclusionRule> rules = new ArrayList<>();
        for (String entry : getExcludePaths().get()) {
            try {
                rules.add(ExclusionRule.parse(entry));
            } catch (IllegalArgumentException e) {
                throw new GradleException(
                        "doppelgangerApiDetector: invalid `excludePaths` entry: " + e.getMessage(), e);
            }
        }
        for (File file : getExcludeFiles()) {
            if (!file.isFile()) {
                warnings.add("Configured `excludeFiles` entry does not exist yet: `" + file + "`.");
                continue;
            }
            try {
                rules.addAll(ExclusionRuleFile.load(file));
            } catch (IOException e) {
                throw new GradleException("doppelgangerApiDetector: failed to read excludeFiles entry " + file, e);
            } catch (IllegalArgumentException e) {
                throw new GradleException("doppelgangerApiDetector: " + e.getMessage(), e);
            }
        }
        for (String name : getExcludeWellKnown().get()) {
            try {
                rules.addAll(WellKnownExclusionSets.resolve(name));
            } catch (IllegalArgumentException e) {
                throw new GradleException("doppelgangerApiDetector: " + e.getMessage(), e);
            }
        }
        return rules;
    }

    /**
     * Whether {@link #getRootDocument()} is both configured and points to a file that actually
     * exists - the precondition for OpenAPI-based comparison being meaningful at all.
     */
    private boolean isRootDocumentAvailable() {
        return getRootDocument().isPresent() && getRootDocument().getAsFile().get().isFile();
    }

    /**
     * The {@code WARNING} admonition line explaining why doppelganger API detection was skipped -
     * distinguishing "never configured" from "configured, but the file doesn't exist yet", since the
     * former needs no path in the message and the latter does.
     */
    private String describeMissingRootDocument() {
        if (!getRootDocument().isPresent()) {
            return "`rootDocument` is not configured yet. Doppelganger API detection was skipped for this "
                    + "run - configure it once your OpenAPI documentation exists.";
        }
        return "The configured `rootDocument` does not exist yet: `" + getRootDocument().getAsFile().get()
                + "`. Doppelganger API detection was skipped for this run - once the file exists, re-run this "
                + "task to check it.";
    }

    /**
     * Loads {@link #getContractHistoryFile()} as-is, for display in the report's
     * {@code == Progress Over Time} section, without advancing or saving it. Used in place of
     * {@link #updateContractHistory(List, List, List)} whenever this run's input is incomplete (see
     * {@link #generate()}): advancing history from a partial view - e.g. an empty {@code implementedNow}
     * purely because {@link #getControllerDirs()} doesn't exist yet, not because every
     * previously-implemented endpoint was actually removed - would misrepresent every endpoint this
     * run couldn't see as newly removed. The persisted file itself is left untouched either way; only
     * the report's display of it is affected.
     */
    private Map<String, ContractProgressRecord> loadContractHistoryForDisplay() {
        File historyFile = getContractHistoryFile().getAsFile().get();
        try {
            return new ContractHistoryStore().load(historyFile);
        } catch (LegacyContractHistoryFormatException e) {
            throw new GradleException("doppelgangerApiDetector: " + historyFile + " is in the old 9-field "
                    + "contract history format (missing 'stubbedAt'). Apply the mirage-api-detector plugin "
                    + "and run its 'migrateContractHistory' task against this file to upgrade it in place, "
                    + "or point contractHistoryFile at a new location to start a fresh history.", e);
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
        Map<String, ContractProgressRecord> previous;
        try {
            previous = store.load(historyFile);
        } catch (LegacyContractHistoryFormatException e) {
            throw new GradleException("doppelgangerApiDetector: " + historyFile + " is in the old 9-field "
                    + "contract history format (missing 'stubbedAt'). Apply the mirage-api-detector plugin "
                    + "and run its 'migrateContractHistory' task against this file to upgrade it in place, "
                    + "or point contractHistoryFile at a new location to start a fresh history.", e);
        }
        Map<String, ContractProgressRecord> updated = new ContractHistoryUpdater()
                .update(previous, implementedNow, declaredNow, verifiedNow, null, Instant.now());
        if (getUpdateContractHistory().get()) {
            store.save(historyFile, updated.values());
        }
        return updated;
    }

    /**
     * The endpoints found by every enabled verification source, together with whether at least one
     * currently-enabled source was unable to gather any evidence at all because the directory it
     * depends on doesn't exist - the signal {@link #generate()} uses to decide whether to suppress
     * doppelganger computation for this run.
     *
     * @param endpoints                the endpoints found by every enabled verification source,
     *                                  combined
     * @param verificationInputMissing whether at least one enabled source could gather no evidence
     *                                  because its required directory is missing
     */
    private record VerificationScan(List<Endpoint> endpoints, boolean verificationInputMissing) {}

    /**
     * Scans every enabled verification source, collecting a {@code WARNING} for each configured
     * {@link #getTestDirs()}/{@link #getContractsDir()} entry that doesn't exist yet, and computing
     * whether every currently-enabled source was left with no usable directory to scan at all.
     *
     * <p>{@link #getTestDirs()} is shared by the Spring RestDocs and OpenAPI request validator
     * sources: a missing entry is only reported, and only counts towards suppression, when at least
     * one of those two sources is actually enabled - and, per {@link #getTestDirsUserConfigured()},
     * only when {@link #getTestDirs()} reflects the user's own configuration rather than only the
     * plugin's unconfigured default. Both sources are still scanned - and announced - even when
     * every {@code testDirs} entry is missing, since a directory that simply contains no matching
     * evidence is indistinguishable from one that doesn't exist yet, at the scanner level.
     * Spring Cloud Contract's phase, by contrast, is only announced and scanned when
     * {@link #getContractsDir()} is both configured and exists - most projects have no
     * {@code contracts} directory at all, and {@link #getContractsDir()} being left entirely unset
     * while {@link #getUseSpringCloudContract()} is enabled is treated as a deliberate choice to
     * offer no Spring Cloud Contract evidence, not a bootstrapping gap, so it neither warns nor
     * counts towards suppression.</p>
     *
     * <p>Detection is only suppressed when <em>every</em> currently-enabled source has no usable
     * directory - a single enabled source with real evidence available is enough for the run to be
     * considered complete, even if another enabled source's directory is missing (that source simply
     * contributes no evidence this run, same as if it had found none).</p>
     */
    private VerificationScan collectVerifiedEndpoints(
            int phase, int totalPhases, File rootDocument, List<String> warnings) {
        boolean useRestDocs = getUseRestDocs().get();
        boolean useOpenApiRequestValidator = getUseOpenApiRequestValidator().get();
        boolean useSpringCloudContract = getUseSpringCloudContract().get();

        boolean testDirsNeeded = useRestDocs || useOpenApiRequestValidator;
        boolean testDirsUserConfigured = getTestDirsUserConfigured().get();
        List<File> missingTestDirs = new ArrayList<>();
        boolean anyTestDirExists = false;
        if (testDirsNeeded) {
            for (File dir : getTestDirs()) {
                if (dir.isDirectory()) {
                    anyTestDirExists = true;
                } else {
                    missingTestDirs.add(dir);
                }
            }
            // A missing entry only warns, and only counts towards suppression below, when testDirs
            // reflects the user's own configuration - see getTestDirsUserConfigured(). The plugin's
            // own unconfigured default missing is a project with no such evidence by design, not a
            // bootstrapping gap, and must not silently suppress detection.
            if (testDirsUserConfigured && !missingTestDirs.isEmpty()) {
                if (anyTestDirExists) {
                    for (File dir : missingTestDirs) {
                        warnings.add("Configured `testDirs` entry does not exist yet: `" + dir + "`.");
                    }
                } else {
                    warnings.add("None of the configured `testDirs` exist yet, so no Spring RestDocs or "
                            + "OpenAPI request validator verification evidence could be gathered for this run.");
                }
            }
        }
        boolean testDirsSourceMissing =
                testDirsUserConfigured && testDirsNeeded && !missingTestDirs.isEmpty() && !anyTestDirExists;

        boolean contractsDirConfigured = getContractsDir().isPresent();
        File contractsDir = contractsDirConfigured ? getContractsDir().getAsFile().get() : null;
        boolean contractsDirExists = contractsDirConfigured && contractsDir.isDirectory();
        boolean contractsDirSourceMissing = useSpringCloudContract && contractsDirConfigured && !contractsDirExists;
        if (contractsDirSourceMissing) {
            warnings.add("Configured `contractsDir` does not exist yet: `" + contractsDir + "`.");
        }

        List<Endpoint> verified = new ArrayList<>();
        try {
            if (useRestDocs) {
                phase = announcePhase(phase, totalPhases, "Scanning Spring RestDocs verification evidence...");
                String serverBasePath = rootDocument == null ? "" : OpenApiServerBasePath.resolve(rootDocument);
                verified.addAll(scanTestDirs(new RestDocsScanner(serverBasePath)));
            }
            if (useOpenApiRequestValidator) {
                phase = announcePhase(phase, totalPhases,
                        "Scanning OpenAPI request validator verification evidence...");
                verified.addAll(scanTestDirs(new OpenApiRequestValidatorScanner()));
            }
            if (useSpringCloudContract && contractsDirExists) {
                announcePhase(phase, totalPhases, "Scanning Spring Cloud Contract verification evidence...");
                verified.addAll(new SpringCloudContractScanner().scan(contractsDir));
            }
        } catch (IOException e) {
            throw new GradleException("doppelgangerApiDetector: failed to scan verification evidence", e);
        }

        boolean anySourceEnabled = useRestDocs || useOpenApiRequestValidator || useSpringCloudContract;
        boolean anyEnabledSourceUsable = (useRestDocs && !testDirsSourceMissing)
                || (useOpenApiRequestValidator && !testDirsSourceMissing)
                || (useSpringCloudContract && !contractsDirSourceMissing);
        boolean verificationInputMissing = anySourceEnabled && !anyEnabledSourceUsable;

        return new VerificationScan(verified, verificationInputMissing);
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
        if (getUseSpringCloudContract().get() && getContractsDir().isPresent()
                && getContractsDir().getAsFile().get().isDirectory()) {
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

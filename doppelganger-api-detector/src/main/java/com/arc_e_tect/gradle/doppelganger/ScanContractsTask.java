package com.arc_e_tect.gradle.doppelganger;

import com.arc_e_tect.gradle.detector.core.console.DetectorStageReporter;
import com.arc_e_tect.gradle.detector.core.console.ScanProgressReporter;
import com.arc_e_tect.gradle.detector.core.detect.ContractSetOperations;
import com.arc_e_tect.gradle.detector.core.exclude.ExclusionFilter;
import com.arc_e_tect.gradle.detector.core.exclude.ExclusionRule;
import com.arc_e_tect.gradle.detector.core.model.Endpoint;
import com.arc_e_tect.gradle.detector.core.openapi.DescribedEndpoint;
import com.arc_e_tect.gradle.detector.core.openapi.OpenApiEndpointCollector;
import com.arc_e_tect.gradle.detector.core.scan.PropertyResolutionContext;
import com.arc_e_tect.gradle.doppelganger.detect.ContractVerificationSource;
import com.arc_e_tect.gradle.doppelganger.detect.EndpointResponseCoverage;
import com.arc_e_tect.gradle.doppelganger.detect.ResponseCoverageAnalyzer;
import com.arc_e_tect.gradle.doppelganger.detect.VerifiedContractTest;
import com.arc_e_tect.gradle.doppelganger.progress.ResponseCoverageHistoryStore;
import com.arc_e_tect.gradle.doppelganger.progress.ResponseCoverageHistoryUpdater;
import com.arc_e_tect.gradle.doppelganger.progress.ResponseCoverageRecord;
import com.arc_e_tect.gradle.doppelganger.report.ScanContractsReportWriter;
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
 * Gradle task that, for every endpoint both declared in the configured OpenAPI documentation and
 * implemented by a {@code @RestController} method, reports how many response codes its operation
 * declares and how many contract tests exist for it - and, when
 * {@link DoppelgangerApiDetectorExtension#getIncludeResponseCoverage()} is enabled, how many of
 * those tests cover each declared response code.
 *
 * <p>Unlike {@link DetectDoppelgangerApisTask}, this task never fails the build on its own
 * initiative - it is purely a reporting task, answering "how well is this API covered", not "is
 * this API compliant".</p>
 *
 * <p>Registered automatically by {@link DoppelgangerApiDetectorPlugin} under the name
 * {@code scanContracts}.</p>
 */
@DisableCachingByDefault(because = "Report depends on source, test, contract, and OpenAPI document content and is cheap to regenerate")
public abstract class ScanContractsTask extends DefaultTask {

    /**
     * Directories to search recursively for {@code @RestController} classes.
     *
     * @return mutable file collection of controller source directories
     */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getControllerDirs();

    /**
     * Directories to search recursively for test classes.
     *
     * @return mutable file collection of test source directories
     */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getTestDirs();

    /**
     * See {@link DetectDoppelgangerApisTask#getTestDirsUserConfigured()}.
     *
     * @return mutable boolean property, {@code true} when {@link #getTestDirs()} reflects the
     *         user's own configuration rather than only the plugin's default
     */
    @Input
    public abstract Property<Boolean> getTestDirsUserConfigured();

    /**
     * The root OpenAPI document describing the API.
     *
     * @return mutable file property for the root OpenAPI document
     */
    @Optional
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getRootDocument();

    /**
     * Directory where OpenAPI descriptions are stored.
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
     * Whether to additionally compute, for every declared response code, how many contract tests
     * cover it. Defaults to {@code false}: the breakdown is not merely hidden when disabled, it is
     * never computed.
     *
     * @return mutable boolean property controlling whether response coverage is computed
     */
    @Input
    public abstract Property<Boolean> getIncludeResponseCoverage();

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
     * Version of the system under test whose {@code @RestController} classes were scanned.
     *
     * @return mutable string property for the system-under-test version
     */
    @Input
    public abstract Property<String> getSystemUnderTestVersion();

    /**
     * Whether to persist, across builds, a history of response code coverage. Only meaningful
     * together with {@link #getIncludeResponseCoverage()} - see {@link #generate()}'s eager
     * validation of that combination.
     *
     * @return mutable boolean property controlling whether response coverage history is tracked
     */
    @Input
    public abstract Property<Boolean> getTrackResponseCoverageHistory();

    /**
     * File that the persisted response coverage history is read from and, when
     * {@link #getUpdateResponseCoverageHistory()} is {@code true}, written back to. See
     * {@link DetectDoppelgangerApisTask#getContractHistoryFile()} for why this is {@code @Internal}
     * rather than tracked through Gradle's file-content-based up-to-date checking.
     *
     * @return mutable file property for the response coverage history file
     */
    @Internal
    public abstract RegularFileProperty getResponseCoverageHistoryFile();

    /**
     * The absolute path of {@link #getResponseCoverageHistoryFile()}, tracked as a plain
     * {@code @Input} value - see {@link DetectDoppelgangerApisTask#getContractHistoryFilePath()}.
     *
     * @return the response coverage history file's absolute path, or {@code null} if unset
     */
    @Input
    @Optional
    public String getResponseCoverageHistoryFilePath() {
        return getResponseCoverageHistoryFile().map(file -> file.getAsFile().getAbsolutePath()).getOrNull();
    }

    /**
     * Whether {@link #getResponseCoverageHistoryFile()} is written back to disk after being updated
     * with the current run's coverage. Only consulted when {@link #getTrackResponseCoverageHistory()}
     * is {@code true}; the history file is always read regardless.
     *
     * @return mutable boolean property controlling whether the response coverage history file is
     *         written back
     */
    @Input
    public abstract Property<Boolean> getUpdateResponseCoverageHistory();

    /**
     * Exclusion rule strings - see {@link DoppelgangerApiDetectorExtension#getExcludePaths()}.
     *
     * @return mutable list property of exclusion rule strings
     */
    @Input
    public abstract ListProperty<String> getExcludePaths();

    /**
     * External exclusion rule files - see {@link DoppelgangerApiDetectorExtension#getExcludeFiles()}.
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
     * Property files used to resolve indirectly-referenced request paths in contract tests - see
     * {@link DoppelgangerApiDetectorExtension#getPropertyFiles()}.
     *
     * @return mutable file collection of {@code .properties}/{@code .yml}/{@code .yaml} files
     */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getPropertyFiles();

    /**
     * Helper-method conventions used to resolve indirectly-referenced request paths in contract
     * tests - see {@link DoppelgangerApiDetectorExtension#getPathResolverHelperMethods()}.
     *
     * @return mutable list property of {@code "ClassName.methodName"} conventions
     */
    @Input
    public abstract ListProperty<String> getPathResolverHelperMethods();

    /** Creates the task. Instantiated by Gradle infrastructure via {@link javax.inject.Inject}. */
    @Inject
    public ScanContractsTask() {
        setGroup("verification");
        setDescription("Scans OpenAPI documentation, @RestController implementations, and contract test "
                + "evidence, and reports the declared response codes and contract test count for every "
                + "declared-and-implemented endpoint - and, when includeResponseCoverage is enabled, how many "
                + "tests cover each declared response code.");
        getTestDirsUserConfigured().convention(true);
    }

    /**
     * Task action: scans the same candidate endpoints {@link DetectDoppelgangerApisTask} does, but
     * reports response-code and contract-test coverage rather than a pass/fail verdict. Never fails
     * the build on its own initiative. Bootstrapping-gap handling (a missing {@link #getRootDocument()},
     * empty {@link #getControllerDirs()}, etc.) follows the same "warn, don't fail" philosophy as
     * {@link DetectDoppelgangerApisTask#generate()} - see that method's javadoc for the full rationale.
     *
     * <p>Two DSL configurations are rejected eagerly: every verification source disabled at once (no
     * test evidence could ever be gathered), {@link #getUseSpringCloudContract()} enabled with
     * {@link #getContractsDir()} unconfigured, and - specific to this task -
     * {@link #getTrackResponseCoverageHistory()} enabled while {@link #getIncludeResponseCoverage()}
     * is disabled, since there would be no per-response-code data to persist.</p>
     */
    @TaskAction
    public void generate() {
        boolean useRestDocs = getUseRestDocs().get();
        boolean useOpenApiRequestValidator = getUseOpenApiRequestValidator().get();
        boolean useSpringCloudContract = getUseSpringCloudContract().get();
        if (!useRestDocs && !useOpenApiRequestValidator && !useSpringCloudContract) {
            throw new GradleException("doppelgangerApiDetector: at least one of useRestDocs, "
                    + "useOpenApiRequestValidator, or useSpringCloudContract must be enabled - with all "
                    + "three disabled, no contract test evidence could ever be gathered.");
        }
        if (useSpringCloudContract && !getContractsDir().isPresent()) {
            throw new GradleException("doppelgangerApiDetector: contractsDir must be configured when "
                    + "useSpringCloudContract is enabled - it has no default location.");
        }
        boolean includeResponseCoverage = getIncludeResponseCoverage().get();
        if (getTrackResponseCoverageHistory().get() && !includeResponseCoverage) {
            throw new GradleException("doppelgangerApiDetector: trackResponseCoverageHistory requires "
                    + "includeResponseCoverage to be enabled - with it disabled, no per-response-code "
                    + "coverage is computed for there to be a history of.");
        }

        List<String> warnings = new ArrayList<>();

        ContractScanSupport.DirectoryScanResult controllerScan =
                ContractScanSupport.scanJavaSourceDirs(getControllerDirs());
        boolean controllerSourceMissing = controllerScan.allConfiguredDirsMissing();
        if (!controllerScan.missingDirs().isEmpty()) {
            if (controllerScan.anyDirExists()) {
                for (File dir : controllerScan.missingDirs()) {
                    warnings.add("Configured `controllerDirs` entry does not exist yet: `" + dir + "`.");
                }
            } else {
                warnings.add("None of the configured `controllerDirs` exist yet. Contract scanning was "
                        + "skipped for this run - once at least one exists, re-run this task to check it.");
            }
        }

        DetectorStageReporter stages = new DetectorStageReporter(getLogger(), "Contract Scan", countTotalPhases());

        stages.stage("Scanning @RestController classes");
        List<Endpoint> implemented = ContractScanSupport.scanControllerFiles(controllerScan.javaFiles(), getLogger());

        stages.stage("Collecting OpenAPI endpoints");
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

        // Note the argument order relative to DetectDoppelgangerApisTask: the DescribedEndpoint side
        // must be preserved here, since it's the one carrying responseCodes().
        List<DescribedEndpoint> declaredAndImplemented = ContractSetOperations.intersection(described, implemented);

        VerificationTestScan verificationScan = collectVerifiedTests(stages, rootDocument, warnings);

        boolean inputComplete =
                openApiAvailable && !controllerSourceMissing && !verificationScan.verificationInputMissing();

        List<ExclusionRule> exclusionRules = ContractScanSupport.resolveExclusionRules(
                getExcludePaths(), getExcludeFiles(), getExcludeWellKnown(), warnings);
        List<DescribedEndpoint> candidates =
                ExclusionFilter.excludeMatching(declaredAndImplemented, exclusionRules);

        List<EndpointResponseCoverage> coverage = inputComplete
                ? new ResponseCoverageAnalyzer().analyze(candidates, verificationScan.tests(), includeResponseCoverage)
                : List.of();

        Map<String, ResponseCoverageRecord> history = !getTrackResponseCoverageHistory().get() ? Map.of()
                : inputComplete ? updateResponseCoverageHistory(coverage) : loadResponseCoverageHistoryForDisplay();

        File outputDir = getReportDir().getAsFile().get();
        File outputFile = new File(outputDir, getReportFileName().get());
        try {
            new ScanContractsReportWriter().write(
                    outputFile, coverage, includeResponseCoverage, getSystemUnderTestVersion().get(), warnings,
                    history);
        } catch (IOException e) {
            throw new GradleException("doppelgangerApiDetector: failed to write report to " + outputFile, e);
        }

        getLogger().lifecycle(
                "Contract Scan: scanned {} declared-and-implemented endpoint(s). Report → {}",
                coverage.size(), outputFile);
    }

    private boolean isRootDocumentAvailable() {
        return getRootDocument().isPresent() && getRootDocument().getAsFile().get().isFile();
    }

    private String describeMissingRootDocument() {
        if (!getRootDocument().isPresent()) {
            return "`rootDocument` is not configured yet. Contract scanning was skipped for this run - "
                    + "configure it once your OpenAPI documentation exists.";
        }
        return "The configured `rootDocument` does not exist yet: `" + getRootDocument().getAsFile().get()
                + "`. Contract scanning was skipped for this run - once the file exists, re-run this task to "
                + "check it.";
    }

    private Map<String, ResponseCoverageRecord> loadResponseCoverageHistoryForDisplay() {
        File historyFile = getResponseCoverageHistoryFile().getAsFile().get();
        return new ResponseCoverageHistoryStore().load(historyFile);
    }

    private Map<String, ResponseCoverageRecord> updateResponseCoverageHistory(List<EndpointResponseCoverage> currentRun) {
        File historyFile = getResponseCoverageHistoryFile().getAsFile().get();
        ResponseCoverageHistoryStore store = new ResponseCoverageHistoryStore();
        Map<String, ResponseCoverageRecord> previous = store.load(historyFile);
        Map<String, ResponseCoverageRecord> updated =
                new ResponseCoverageHistoryUpdater().update(previous, currentRun, Instant.now());
        if (getUpdateResponseCoverageHistory().get()) {
            store.save(historyFile, updated.values());
        }
        return updated;
    }

    /**
     * The verification evidence found by every enabled source, together with whether at least one
     * currently-enabled source was unable to gather any evidence at all - the same
     * "was every enabled source usable" signal {@link DetectDoppelgangerApisTask#generate()} computes
     * for itself, just carrying {@link VerifiedContractTest} instead of a plain {@link Endpoint}.
     */
    private record VerificationTestScan(List<VerifiedContractTest> tests, boolean verificationInputMissing) {}

    private VerificationTestScan collectVerifiedTests(
            DetectorStageReporter stages, File rootDocument, List<String> warnings) {
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

        List<VerifiedContractTest> tests = new ArrayList<>();
        PropertyResolutionContext propertyResolutionContext =
                PropertyResolutionContextFactory.create(getPropertyFiles(), getPathResolverHelperMethods());
        try {
            if (useRestDocs) {
                stages.stage("Scanning Spring RestDocs verification evidence");
                String serverBasePath = rootDocument == null ? "" : OpenApiServerBasePath.resolve(rootDocument);
                List<VerifiedContractTest> restDocsTests =
                        scanTestDirsWithStatusCodes(new RestDocsScanner(serverBasePath, propertyResolutionContext));
                tests.addAll(restDocsTests);
                getLogger().lifecycle(
                        "Scanning Spring RestDocs verification evidence: done, {} test(s) found", restDocsTests.size());
            }
            if (useOpenApiRequestValidator) {
                stages.stage("Scanning OpenAPI request validator verification evidence");
                List<VerifiedContractTest> requestValidatorTests =
                        scanTestDirsWithStatusCodes(new OpenApiRequestValidatorScanner(propertyResolutionContext));
                tests.addAll(requestValidatorTests);
                getLogger().lifecycle("Scanning OpenAPI request validator verification evidence: done, {} test(s) found",
                        requestValidatorTests.size());
            }
            if (useSpringCloudContract && contractsDirExists) {
                stages.stage("Scanning Spring Cloud Contract verification evidence");
                List<VerifiedContractTest> contractTests = new SpringCloudContractScanner().scanWithStatusCodes(contractsDir);
                tests.addAll(contractTests);
                getLogger().lifecycle("Scanning Spring Cloud Contract verification evidence: done, {} test(s) found",
                        contractTests.size());
            }
        } catch (IOException e) {
            throw new GradleException("doppelgangerApiDetector: failed to scan verification evidence", e);
        }

        boolean anySourceEnabled = useRestDocs || useOpenApiRequestValidator || useSpringCloudContract;
        boolean anyEnabledSourceUsable = (useRestDocs && !testDirsSourceMissing)
                || (useOpenApiRequestValidator && !testDirsSourceMissing)
                || (useSpringCloudContract && !contractsDirSourceMissing);
        boolean verificationInputMissing = anySourceEnabled && !anyEnabledSourceUsable;

        return new VerificationTestScan(tests, verificationInputMissing);
    }

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

    private List<VerifiedContractTest> scanTestDirsWithStatusCodes(ContractVerificationSource source) throws IOException {
        List<VerifiedContractTest> results = new ArrayList<>();
        for (File testDir : getTestDirs()) {
            results.addAll(source.scanWithStatusCodes(testDir));
        }
        return results;
    }
}

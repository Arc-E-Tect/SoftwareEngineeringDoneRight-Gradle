package com.arc_e_tect.gradle.shadow;

import com.arc_e_tect.gradle.detector.core.console.ScanProgressReporter;
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
import com.arc_e_tect.gradle.shadow.detect.ShadowApiFinder;
import com.arc_e_tect.gradle.shadow.report.ShadowApiReportWriter;
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
import org.gradle.api.tasks.options.Option;
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
     * Single-run override of {@link #getUpdateContractHistory()}, settable only from the command
     * line via {@code --updateContractHistory}/{@code --no-updateContractHistory} - never wired from
     * the DSL, and unset by default. When present, takes precedence over
     * {@link #getUpdateContractHistory()} for this run only, without changing the build script -
     * typically used from CI to advance the committed history only on the branch(es) whose pipeline
     * should, since the plugin itself has no notion of which branch is currently checked out.
     *
     * <p>Tracked as {@code @Input}, not {@code @Internal}: without that, passing a different value
     * on the command line between two otherwise-identical runs would leave the task {@code UP-TO-DATE}
     * and silently skip re-executing with the new override.</p>
     *
     * @return mutable, normally-unset boolean property overriding {@link #getUpdateContractHistory()}
     *         for a single run
     */
    @Input
    @Optional
    public abstract Property<Boolean> getUpdateContractHistoryOverride();

    /**
     * CLI entry point for {@link #getUpdateContractHistoryOverride()}. Not meant to be called
     * directly - Gradle invokes it when {@code --updateContractHistory}/{@code --no-updateContractHistory}
     * is passed on the command line.
     *
     * <p>Deliberately not named {@code setUpdateContractHistoryOverride} - a method matching the
     * {@code getUpdateContractHistoryOverride()}/{@code setUpdateContractHistoryOverride(...)}
     * JavaBean getter/setter naming convention makes Gradle's task class generator treat the pair as
     * a plain {@code boolean} property and reject {@link #getUpdateContractHistoryOverride()} for
     * being abstract, even though it is a perfectly ordinary managed {@code Property<Boolean>}.</p>
     *
     * @param value the value to override {@link #getUpdateContractHistory()} with for this run
     */
    @Option(option = "updateContractHistory",
            description = "Overrides shadowApiDetector.updateContractHistory for this run only.")
    public void applyUpdateContractHistoryOverride(boolean value) {
        getUpdateContractHistoryOverride().set(value);
    }

    /**
     * Single-run override of {@link #getFailOnShadow()}, settable only from the command line via
     * {@code --failOnShadow}/{@code --no-failOnShadow} - never wired from the DSL, and unset by
     * default. When present, takes precedence over {@link #getFailOnShadow()} for this run only,
     * without changing the build script - applies equally to a full-project scan and to a
     * {@link #getScanForShadows()} single-controller scan.
     *
     * <p>Tracked as {@code @Input}, not {@code @Internal} - see
     * {@link #getUpdateContractHistoryOverride()} for why.</p>
     *
     * @return mutable, normally-unset boolean property overriding {@link #getFailOnShadow()} for a
     *         single run
     */
    @Input
    @Optional
    public abstract Property<Boolean> getFailOnShadowOverride();

    /**
     * CLI entry point for {@link #getFailOnShadowOverride()}. Not meant to be called directly -
     * Gradle invokes it when {@code --failOnShadow}/{@code --no-failOnShadow} is passed on the
     * command line. See {@link #applyUpdateContractHistoryOverride(boolean)} for why this is
     * deliberately not named {@code setFailOnShadowOverride}.
     *
     * @param value the value to override {@link #getFailOnShadow()} with for this run
     */
    @Option(option = "failOnShadow", description = "Overrides shadowApiDetector.failOnShadow for this run only.")
    public void applyFailOnShadowOverride(boolean value) {
        getFailOnShadowOverride().set(value);
    }

    /**
     * Exclusion rule strings - see {@link ShadowApiDetectorExtension#getExcludePaths()}.
     *
     * @return mutable list property of exclusion rule strings
     */
    @Input
    public abstract ListProperty<String> getExcludePaths();

    /**
     * External exclusion rule files - see {@link ShadowApiDetectorExtension#getExcludeFiles()}.
     *
     * @return mutable file collection of exclusion rule files
     */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getExcludeFiles();

    /**
     * Bundled well-known exclusion set names - see
     * {@link ShadowApiDetectorExtension#getExcludeWellKnown()}.
     *
     * @return mutable list property of well-known exclusion set names
     */
    @Input
    public abstract ListProperty<String> getExcludeWellKnown();

    /**
     * The name or path of a single {@code @RestController} class to scan for shadow APIs, settable
     * only from the command line via {@code --scanForShadows=<name-or-path>} - never wired from the
     * DSL, and unset by default. When present, {@link #generate()} scans only this controller
     * instead of every file under {@link #getControllerDirs()}, and prints its findings to the
     * console instead of writing {@link #getReportDir()}'s AsciiDoc report - see
     * {@link #scanSingleController(String)}.
     *
     * <p>Two forms are accepted:</p>
     * <ul>
     *   <li>A path ending in {@code .java} - scanned directly, regardless of
     *       {@link #getControllerDirs()}. Fails clearly if the file does not exist.</li>
     *   <li>A bare class name (e.g. {@code OrderController}) - every {@code <name>.java} file found
     *       anywhere under {@link #getControllerDirs()} is scanned; there may be more than one, e.g.
     *       same-named controllers in different packages, in which case every match is scanned and
     *       their findings combined. Fails clearly if no file named {@code <name>.java} is found.</li>
     * </ul>
     *
     * <p>Tracked as {@code @Input}, not {@code @Internal} - see
     * {@link #getUpdateContractHistoryOverride()} for why.</p>
     *
     * @return mutable, normally-unset string property naming or pathing a single controller to scan
     */
    @Input
    @Optional
    public abstract Property<String> getScanForShadows();

    /**
     * The project directory, used only to resolve a relative {@link #getScanForShadows()} path
     * against - never against this process's own working directory, which need not be the project
     * directory at all (e.g. when Gradle is invoked with {@code --project-dir} from elsewhere) and,
     * unlike the project directory, isn't necessarily stable in a configuration-cache-compatible way.
     * Wired once by {@link ShadowApiDetectorPlugin} at configuration time; not itself part of the
     * public DSL.
     *
     * @return directory property for the project directory
     */
    @Internal
    public abstract DirectoryProperty getProjectDirectory();

    /**
     * CLI entry point for {@link #getScanForShadows()}. Not meant to be called directly - Gradle
     * invokes it when {@code --scanForShadows=<value>} is passed on the command line. See
     * {@link #applyUpdateContractHistoryOverride(boolean)} for why this is deliberately not named
     * {@code setScanForShadows}.
     *
     * @param value the controller name or {@code .java} path to scan
     */
    @Option(option = "scanForShadows",
            description = "Scans a single @RestController class (by name, or by a path ending in .java) for "
                    + "shadow APIs, printing findings to the console instead of writing the AsciiDoc report.")
    public void applyScanForShadows(String value) {
        getScanForShadows().set(value);
    }

    /**
     * Creates the task. Instantiated by Gradle infrastructure via {@link javax.inject.Inject}.
     */
    @Inject
    public DetectShadowApisTask() {
        setGroup("verification");
        setDescription("Scans @RestController classes and reports endpoints not described in the OpenAPI documentation.");
    }

    /**
     * Task action: either scans a single controller named or pathed by {@link #getScanForShadows()}
     * and prints its findings to the console (see {@link #scanSingleController(String)}), or - when
     * that property is unset - scans every {@code @RestController} class under
     * {@link #getControllerDirs()}, loads the configured OpenAPI documentation, and writes the
     * shadow API report.
     *
     * <p>A missing {@link #getRootDocument()} or empty {@link #getControllerDirs()} - e.g. a build
     * script bootstrapped for a project whose OpenAPI documentation or {@code @RestController}
     * classes don't exist yet - is <em>not</em> a build failure in this full-scan mode: it is
     * recorded as a {@code WARNING} admonition in the generated report instead, and shadow API
     * detection (and, deliberately, contract history advancement - see
     * {@link #loadContractHistoryForDisplay()}) is skipped for this run rather than computed from
     * incomplete input and risking a false positive. {@link #scanSingleController(String)} keeps its
     * own, deliberately stricter behavior: a controller explicitly named on the command line that
     * cannot be found is a real usage error, not a bootstrapping gap.</p>
     *
     * <p>The build still fails when a shadow API is genuinely found and the effective
     * {@link #getFailOnShadow()} (accounting for {@link #getFailOnShadowOverride()}) is {@code true}
     * - the one failure condition this task ever raises on its own initiative, per this plugin's
     * design: fail only on a real finding the DSL asked to fail on, never on merely incomplete
     * input.</p>
     */
    @TaskAction
    public void generate() {
        if (getScanForShadows().isPresent()) {
            if (!getRootDocument().isPresent()) {
                throw new GradleException("shadowApiDetector: rootDocument must be configured - "
                        + "it is the required root OpenAPI document.");
            }
            scanSingleController(getScanForShadows().get());
            return;
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
                warnings.add("None of the configured `controllerDirs` exist yet. Shadow API detection was "
                        + "skipped for this run - once at least one exists, re-run this task to check it.");
            }
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

        boolean openApiAvailable = isRootDocumentAvailable();
        List<DescribedEndpoint> described;
        if (openApiAvailable) {
            File rootDocument = getRootDocument().getAsFile().get();
            ScanProgressReporter openApiProgress =
                    ScanProgressReporter.indeterminate(getLogger(), "Resolving OpenAPI documents");
            described = new OpenApiEndpointCollector().collect(rootDocument, file -> openApiProgress.step());
            openApiProgress.complete();
        } else {
            described = List.of();
            warnings.add(describeMissingRootDocument());
        }

        boolean inputComplete = openApiAvailable && !controllerSourceMissing;
        List<Endpoint> shadows = inputComplete ? new ShadowApiFinder().findShadows(endpoints, described) : List.of();

        List<ExclusionRule> exclusionRules = resolveExclusionRules(warnings);
        List<Endpoint> reportableShadows = ExclusionFilter.excludeMatching(shadows, exclusionRules);
        // Every implemented endpoint matching a rule, not just the undocumented subset - see
        // ShadowApiReportWriter#write's excludedImplementations javadoc for why.
        List<Endpoint> excludedImplementations = ExclusionFilter.onlyMatching(endpoints, exclusionRules);

        Map<String, ContractProgressRecord> contractHistory = !getTrackContractHistory().get() ? Map.of()
                : inputComplete ? updateContractHistory(
                        ExclusionFilter.excludeMatching(endpoints, exclusionRules),
                        ExclusionFilter.excludeMatching(described, exclusionRules))
                        : loadContractHistoryForDisplay();

        File outputDir = getReportDir().getAsFile().get();
        File outputFile = new File(outputDir, getReportFileName().get());
        try {
            new ShadowApiReportWriter().write(outputFile, endpoints.size(), reportableShadows,
                    excludedImplementations, getSystemUnderTestVersion().get(), warnings, contractHistory);
        } catch (IOException e) {
            throw new GradleException("shadowApiDetector: failed to write report to " + outputFile, e);
        }

        getLogger().lifecycle("Shadow API Detector: scanned {} endpoint(s), found {} shadow API(s) "
                        + "({} excluded implementations). Report → {}",
                endpoints.size(), reportableShadows.size(), excludedImplementations.size(), outputFile);

        if (!reportableShadows.isEmpty() && effectiveFailOnShadow()) {
            throw new GradleException("shadowApiDetector: found " + reportableShadows.size()
                    + " shadow API(s) not described in the OpenAPI documentation. See " + outputFile);
        }
    }

    /**
     * Resolves every configured exclusion rule - {@link #getExcludePaths()},
     * {@link #getExcludeFiles()}, and {@link #getExcludeWellKnown()} - into one combined list. A
     * missing {@code excludeFiles} entry only warns, the same way a missing {@code controllerDirs}
     * entry does; a malformed rule string or an unrecognised well-known set name fails the build
     * outright, since those are build-script/file mistakes, not a "not built yet" bootstrapping gap.
     */
    private List<ExclusionRule> resolveExclusionRules(List<String> warnings) {
        List<ExclusionRule> rules = new ArrayList<>();
        for (String entry : getExcludePaths().get()) {
            try {
                rules.add(ExclusionRule.parse(entry));
            } catch (IllegalArgumentException e) {
                throw new GradleException("shadowApiDetector: invalid `excludePaths` entry: " + e.getMessage(), e);
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
                throw new GradleException("shadowApiDetector: failed to read excludeFiles entry " + file, e);
            } catch (IllegalArgumentException e) {
                throw new GradleException("shadowApiDetector: " + e.getMessage(), e);
            }
        }
        for (String name : getExcludeWellKnown().get()) {
            try {
                rules.addAll(WellKnownExclusionSets.resolve(name));
            } catch (IllegalArgumentException e) {
                throw new GradleException("shadowApiDetector: " + e.getMessage(), e);
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
     * The {@code WARNING} admonition line explaining why OpenAPI-based comparison was skipped -
     * distinguishing "never configured" from "configured, but the file doesn't exist yet", since the
     * former needs no path in the message and the latter does.
     */
    private String describeMissingRootDocument() {
        if (!getRootDocument().isPresent()) {
            return "`rootDocument` is not configured yet. Shadow API detection was skipped for this run - "
                    + "configure it once your OpenAPI documentation exists.";
        }
        return "The configured `rootDocument` does not exist yet: `" + getRootDocument().getAsFile().get()
                + "`. Shadow API detection was skipped for this run - once the file exists, re-run this task "
                + "to check it.";
    }

    /**
     * Loads {@link #getContractHistoryFile()} as-is, for display in the report's
     * {@code == Progress Over Time} section, without advancing or saving it. Used in place of
     * {@link #updateContractHistory(List, List)} whenever this run's input is incomplete (see
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
            throw new GradleException("shadowApiDetector: " + historyFile + " is in the old 9-field "
                    + "contract history format (missing 'stubbedAt'). Apply the mirage-api-detector plugin "
                    + "and run its 'migrateContractHistory' task against this file to upgrade it in place, "
                    + "or point contractHistoryFile at a new location to start a fresh history.", e);
        }
    }

    /**
     * Scans only the controller(s) named or pathed by {@code scanForShadows} - see
     * {@link #getScanForShadows()} for the two accepted forms - compares their endpoints against the
     * configured OpenAPI documentation, and prints every shadow API found to the console, followed by
     * a summary line of how many controllers were scanned, how many endpoints were found, and how
     * many of those are shadow APIs. No AsciiDoc report is written and contract history is not
     * touched, regardless of {@link #getTrackContractHistory()} - both are scoped to a full-project
     * view this single-controller scan deliberately doesn't have, so persisting either from a partial
     * run would misrepresent every endpoint this run didn't see as newly removed. Still fails the
     * build, exactly like a full scan, when a shadow API is found and the effective
     * {@link #getFailOnShadow()} is {@code true}.
     */
    private void scanSingleController(String target) {
        List<File> controllerFiles = resolveScanForShadowsTargets(target);

        ControllerScanner scanner = new ControllerScanner();
        List<Endpoint> endpoints = new ArrayList<>();
        for (File controllerFile : controllerFiles) {
            scanFile(scanner, controllerFile, endpoints);
        }

        File rootDocument = getRootDocument().getAsFile().get();
        List<DescribedEndpoint> described = new OpenApiEndpointCollector().collect(rootDocument, file -> {});

        List<Endpoint> shadows = new ShadowApiFinder().findShadows(endpoints, described);

        if (shadows.isEmpty()) {
            getLogger().lifecycle("Shadow API Detector: no shadow APIs found.");
        } else {
            getLogger().lifecycle("Shadow API Detector: shadow API(s) found:");
            for (Endpoint shadow : shadows) {
                getLogger().lifecycle("  {} {} ({})", shadow.verb(), shadow.path(), shadow.declaringClass());
            }
        }
        getLogger().lifecycle(
                "Shadow API Detector: scanned {} controller(s), found {} endpoint(s), {} of them shadow API(s).",
                controllerFiles.size(), endpoints.size(), shadows.size());

        if (!shadows.isEmpty() && effectiveFailOnShadow()) {
            throw new GradleException("shadowApiDetector: found " + shadows.size()
                    + " shadow API(s) in the scanned controller(s).");
        }
    }

    /**
     * Resolves {@code scanForShadows} to the controller file(s) it names - a path ending in
     * {@code .java} is used as-is (regardless of {@link #getControllerDirs()}), resolved against
     * {@link #getProjectDirectory()} when relative, otherwise every {@code <target>.java} file found
     * anywhere under {@link #getControllerDirs()} is returned.
     *
     * @throws GradleException if a given path does not exist, or a given name matches no file
     */
    private List<File> resolveScanForShadowsTargets(String target) {
        if (target.endsWith(".java")) {
            File targetFile = new File(target);
            File controllerFile = targetFile.isAbsolute()
                    ? targetFile
                    : new File(getProjectDirectory().getAsFile().get(), target);
            if (!controllerFile.isFile()) {
                throw new GradleException("shadowApiDetector: scanForShadows path does not exist: " + controllerFile);
            }
            return List.of(controllerFile);
        }

        String fileName = target + ".java";
        List<File> matches = new ArrayList<>();
        for (File dir : getControllerDirs()) {
            collectMatchingJavaFiles(dir, fileName, matches);
        }
        if (matches.isEmpty()) {
            throw new GradleException("shadowApiDetector: no controller named '" + target
                    + "' found under the configured controllerDirs.");
        }
        return matches;
    }

    private void collectMatchingJavaFiles(File dir, String fileName, List<File> matches) {
        if (!dir.isDirectory()) {
            return;
        }
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (child.isFile() && child.getName().equals(fileName)) {
                matches.add(child);
            } else if (child.isDirectory()) {
                collectMatchingJavaFiles(child, fileName, matches);
            }
        }
    }

    /**
     * {@link #getFailOnShadow()}, unless {@link #getFailOnShadowOverride()} is present, in which case
     * the override takes precedence.
     */
    private boolean effectiveFailOnShadow() {
        return getFailOnShadowOverride().getOrElse(getFailOnShadow().get());
    }

    /**
     * {@link #getUpdateContractHistory()}, unless {@link #getUpdateContractHistoryOverride()} is
     * present, in which case the override takes precedence.
     */
    private boolean effectiveUpdateContractHistory() {
        return getUpdateContractHistoryOverride().getOrElse(getUpdateContractHistory().get());
    }

    /**
     * Loads the persisted contract progress history, advances it with the current run's implemented
     * and declared endpoints (Shadow API Detector never has verification evidence to offer), and -
     * only when the effective {@link #getUpdateContractHistory()} (see
     * {@link #effectiveUpdateContractHistory()}) resolves to {@code true} - saves it back. The
     * history file is always read regardless, so the generated report reflects the up-to-date-in-memory
     * history even on a run that doesn't persist it.
     */
    private Map<String, ContractProgressRecord> updateContractHistory(
            List<Endpoint> implementedNow, List<DescribedEndpoint> declaredNow) {
        File historyFile = getContractHistoryFile().getAsFile().get();
        ContractHistoryStore store = new ContractHistoryStore();
        Map<String, ContractProgressRecord> previous;
        try {
            previous = store.load(historyFile);
        } catch (LegacyContractHistoryFormatException e) {
            throw new GradleException("shadowApiDetector: " + historyFile + " is in the old 9-field "
                    + "contract history format (missing 'stubbedAt'). Apply the mirage-api-detector plugin "
                    + "and run its 'migrateContractHistory' task against this file to upgrade it in place, "
                    + "or point contractHistoryFile at a new location to start a fresh history.", e);
        }
        Map<String, ContractProgressRecord> updated = new ContractHistoryUpdater()
                .update(previous, implementedNow, declaredNow, null, null, Instant.now());
        if (effectiveUpdateContractHistory()) {
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

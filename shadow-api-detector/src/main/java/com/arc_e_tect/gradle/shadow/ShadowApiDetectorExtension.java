package com.arc_e_tect.gradle.shadow;

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

/**
 * DSL extension for the Shadow API Detector Gradle plugin.
 *
 * <pre>
 * shadowApiDetector {
 *     controllerDirs.from('src/main/java')                                    // default
 *     rootDocument   = file('src/main/resources/openapi/openapi.yaml')       // required
 *     // openApiDir  = rootDocument.get().asFile.parentFile                  // default
 *     failOnShadow   = false                                                  // default
 *     reportDir      = layout.buildDirectory.dir('reports/shadow-api-detector') // default
 *     reportFileName = 'shadow-apis.adoc'                                     // default
 *     // systemUnderTestVersion = 'v1.0.0'          // optional; default: project.version
 *     trackContractHistory  = false                                           // default
 *     // contractHistoryFile = file('shadow-api-detector-contract-history.ndjson') // default
 *     updateContractHistory = trackContractHistory                            // default; see getUpdateContractHistory()
 *
 *     // excludePaths.add('/actuator/health')                                 // default: empty
 *     // excludeFiles.from('shadow-exclusions.yaml')                          // default: empty
 *     // excludeWellKnown.add('spring-boot-actuator')                        // default: empty
 * }
 * </pre>
 *
 * <p>{@code updateContractHistory} and {@code failOnShadow} can each be overridden for a single run
 * from the command line, e.g. {@code ./gradlew detectShadowApis --updateContractHistory} /
 * {@code --no-updateContractHistory} and {@code --failOnShadow} / {@code --no-failOnShadow} - see
 * {@link DetectShadowApisTask#getUpdateContractHistoryOverride()} and
 * {@link DetectShadowApisTask#getFailOnShadowOverride()}. {@code detectShadowApis} also accepts
 * {@code --scanForShadows=<name-or-path>} to scan a single {@code @RestController} class instead of
 * the whole project - see {@link DetectShadowApisTask#getScanForShadows()}.</p>
 */
public abstract class ShadowApiDetectorExtension {

    /** For use by the Gradle-generated concrete subclass. */
    public ShadowApiDetectorExtension() {}

    /** Extension DSL block name, i.e. the name used to register the extension with the project. */
    public static final String NAME = "shadowApiDetector";

    /** Default relative path of the directory searched for {@code @RestController} classes. */
    public static final String DEFAULT_CONTROLLER_DIR = "src/main/java";

    /** Default name of the generated AsciiDoc report file. */
    public static final String DEFAULT_REPORT_FILE_NAME = "shadow-apis.adoc";

    /** Default name of the persisted contract progress history file. */
    public static final String DEFAULT_CONTRACT_HISTORY_FILE_NAME = "shadow-api-detector-contract-history.ndjson";

    /**
     * Directories to search recursively for {@code @RestController} classes. One or more
     * directories may be configured. Defaults to {@value #DEFAULT_CONTROLLER_DIR}.
     *
     * @return mutable file collection of controller source directories
     */
    public abstract ConfigurableFileCollection getControllerDirs();

    /**
     * The root OpenAPI document describing the API. Required: every other OpenAPI document is
     * expected to be reachable from this one via {@code $ref} links relative to it.
     *
     * @return mutable file property for the root OpenAPI document
     */
    public abstract RegularFileProperty getRootDocument();

    /**
     * Directory where OpenAPI descriptions are stored. Used only to determine which files the
     * task should track as inputs for up-to-date checks; every document actually consulted is
     * discovered by following the {@code $ref} links reachable from {@link #getRootDocument()}.
     * Defaults to the root document's own parent directory.
     *
     * @return mutable directory property for the OpenAPI description directory
     */
    public abstract DirectoryProperty getOpenApiDir();

    /**
     * Whether the build should fail when shadow APIs are found. The report is written either way.
     * Defaults to {@code false}.
     *
     * @return mutable boolean property controlling whether the build fails on shadow APIs
     */
    public abstract Property<Boolean> getFailOnShadow();

    /**
     * Directory the AsciiDoc report is written to. Defaults to
     * {@code build/reports/shadow-api-detector}.
     *
     * @return mutable directory property for the report output directory
     */
    public abstract DirectoryProperty getReportDir();

    /**
     * Name of the generated AsciiDoc report file (without path).
     * Defaults to {@value #DEFAULT_REPORT_FILE_NAME}.
     *
     * @return mutable string property for the report file name
     */
    public abstract Property<String> getReportFileName();

    /**
     * Version of the system under test whose {@code @RestController} classes are scanned, printed
     * in the generated report as e.g. {@code System Under Test version: v1.0.0}. Defaults to the
     * project's own {@code version} (as set in the build file or a properties file); set this
     * property to override that default, e.g. when the controllers scanned belong to a different
     * artifact than the one being built.
     *
     * @return mutable string property for the system-under-test version
     */
    public abstract Property<String> getSystemUnderTestVersion();

    /**
     * Whether to persist, across builds, a history of when each endpoint first reached each stage
     * of its contract lifecycle - declared, implemented, verified - keyed by a fingerprint of its
     * HTTP verb and path so the history is shared correctly with Mirage and Doppelganger API
     * Detector when they're pointed at the same {@link #getContractHistoryFile()}. Defaults to
     * {@code false}.
     *
     * <p>The history file configured via {@link #getContractHistoryFile()} is always read when this
     * property is {@code true}, regardless of {@link #getUpdateContractHistory()}.</p>
     *
     * @return mutable boolean property controlling whether contract progress history is tracked
     */
    public abstract Property<Boolean> getTrackContractHistory();

    /**
     * File that the persisted contract progress history is read from and, when
     * {@link #getUpdateContractHistory()} is {@code true}, written back to. Defaults to
     * {@value #DEFAULT_CONTRACT_HISTORY_FILE_NAME} directly in the project directory - deliberately
     * not under {@code build/}, since this file is meant to be committed to version control so the
     * history survives across checkouts. Only consulted when {@link #getTrackContractHistory()} is
     * {@code true}.
     *
     * @return mutable file property for the contract history file
     */
    public abstract RegularFileProperty getContractHistoryFile();

    /**
     * Whether {@link #getContractHistoryFile()} is written back to disk after being updated with the
     * current run's endpoints. Defaults to the same value as {@link #getTrackContractHistory()}.
     * Only consulted when {@link #getTrackContractHistory()} is {@code true}; the history file is
     * always read regardless of this property's value, so a build with this set to {@code false}
     * still reports against the up-to-date-in-memory history, it simply doesn't persist it.
     *
     * <p>Overridable for a single run via {@code detectShadowApis}'s own
     * {@code --updateContractHistory}/{@code --no-updateContractHistory} command-line option - see
     * {@link DetectShadowApisTask#getUpdateContractHistoryOverride()} - typically used from CI to
     * advance the committed history only on the branch(es) whose pipeline should, since the plugin
     * itself has no notion of which branch is currently checked out.</p>
     *
     * @return mutable boolean property controlling whether the contract history file is written back
     */
    public abstract Property<Boolean> getUpdateContractHistory();

    /**
     * Exclusion rule strings, parsed by
     * {@link com.arc_e_tect.gradle.detector.core.exclude.ExclusionRule#parse(String)} - e.g.
     * {@code "/actuator/health"} (any verb) or {@code "GET /actuator/**"} (verb-restricted,
     * Ant-style {@code *}/{@code **} wildcards). Unlike Mirage/Doppelganger API Detector, a match
     * here is a signal in the *opposite* direction: it means a real {@code @RestController}
     * implementation exists at a path declared excluded (e.g. framework-provided, no real
     * implementation expected there) - reported under {@code == Excluded Implementations} instead
     * of {@code == Shadow APIs}, regardless of whether that implementation is also undocumented.
     * It never fails {@link #getFailOnShadow()} and never reaches
     * {@link #getContractHistoryFile()}. Defaults to empty.
     *
     * @return mutable list property of exclusion rule strings
     */
    public abstract ListProperty<String> getExcludePaths();

    /**
     * One or more YAML files of exclusion rules, in the same format bundled well-known sets use:
     * <pre>
     * exclusions:
     *   - "/actuator/health"
     *   - "GET /actuator/**"
     * </pre>
     * Rules from every configured file are combined with {@link #getExcludePaths()} and
     * {@link #getExcludeWellKnown()}. Defaults to empty.
     *
     * @return mutable file collection of exclusion rule files
     */
    public abstract ConfigurableFileCollection getExcludeFiles();

    /**
     * Names of bundled, well-known exclusion sets to apply - e.g.
     * {@value com.arc_e_tect.gradle.detector.core.exclude.WellKnownExclusionSets#SPRING_BOOT_ACTUATOR}
     * for Spring Boot Actuator's management endpoints. Combined with {@link #getExcludePaths()}
     * and {@link #getExcludeFiles()}. An unrecognised name fails the build. Defaults to empty.
     *
     * @return mutable list property of well-known exclusion set names
     */
    public abstract ListProperty<String> getExcludeWellKnown();
}

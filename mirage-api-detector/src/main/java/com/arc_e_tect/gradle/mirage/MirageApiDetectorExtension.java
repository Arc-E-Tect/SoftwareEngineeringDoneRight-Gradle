package com.arc_e_tect.gradle.mirage;

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

/**
 * DSL extension for the Mirage API Detector Gradle plugin.
 *
 * <pre>
 * mirageApiDetector {
 *     controllerDirs.from('src/main/java')                                    // default
 *     rootDocument   = file('src/main/resources/openapi/openapi.yaml')       // required
 *     // openApiDir  = rootDocument.get().asFile.parentFile                  // default
 *     scanMocks      = false                                                  // default
 *     // stubDirs.from('src/test/resources/mappings')                       // default; used only when scanMocks = true
 *     // basePath     = '/crm-service'   // optional; used only when scanMocks = true - see getBasePath()
 *     failOnMirage   = false                                                  // default
 *     reportDir      = layout.buildDirectory.dir('reports/mirage-api-detector') // default
 *     reportFileName = 'mirage-apis.adoc'                                     // default
 *     // systemUnderTestVersion = 'v1.0.0'          // optional; default: project.version
 *     trackContractHistory  = false                                           // default
 *     // contractHistoryFile = file('mirage-api-detector-contract-history.ndjson') // default
 *     updateContractHistory = trackContractHistory                            // default; see getUpdateContractHistory()
 *
 *     // excludePaths.add('/actuator/health')                                 // default: empty
 *     // excludeFiles.from('mirage-exclusions.yaml')                          // default: empty
 *     // excludeWellKnown.add('spring-boot-actuator')                        // default: empty
 * }
 * </pre>
 *
 * <p>{@code updateContractHistory} can be overridden for the whole build from the command line,
 * e.g. {@code -PmirageApiDetector.updateContractHistory=true} - see
 * {@link #getUpdateContractHistory()}.</p>
 */
public abstract class MirageApiDetectorExtension {

    /** For use by the Gradle-generated concrete subclass. */
    public MirageApiDetectorExtension() {}

    /** Extension DSL block name, i.e. the name used to register the extension with the project. */
    public static final String NAME = "mirageApiDetector";

    /** Default relative path of the directory searched for {@code @RestController} classes. */
    public static final String DEFAULT_CONTROLLER_DIR = "src/main/java";

    /** Default relative path of the directory searched for WireMock stub mapping files. */
    public static final String DEFAULT_STUB_DIR = "src/test/resources/mappings";

    /** Default name of the generated AsciiDoc report file. */
    public static final String DEFAULT_REPORT_FILE_NAME = "mirage-apis.adoc";

    /** Default name of the persisted contract progress history file. */
    public static final String DEFAULT_CONTRACT_HISTORY_FILE_NAME = "mirage-api-detector-contract-history.ndjson";

    /**
     * Name of the Gradle project property that overrides {@link #getUpdateContractHistory()} from
     * the command line for every project in the build, e.g.
     * {@code -PmirageApiDetector.updateContractHistory=true}. Takes precedence over any project's
     * own configured {@code updateContractHistory} value. The value is parsed as a boolean.
     */
    public static final String UPDATE_CONTRACT_HISTORY_OVERRIDE_PROPERTY = "mirageApiDetector.updateContractHistory";

    /**
     * Directories to search recursively for {@code @RestController} classes, used to determine
     * which OpenAPI operations are implemented - and thus which are reported as mirage APIs.
     * Always scanned, regardless of {@link #getScanMocks()}. One or more directories may be
     * configured. Defaults to {@value #DEFAULT_CONTROLLER_DIR}.
     *
     * @return mutable file collection of controller source directories
     */
    public abstract ConfigurableFileCollection getControllerDirs();

    /**
     * Whether to additionally scan WireMock stub mapping files under {@link #getStubDirs()} for
     * stub evidence, alongside {@link #getControllerDirs()}'s real implementation evidence. Stub
     * evidence is recorded into contract history/the report (as {@code stubbedAt}) but never
     * counts as implementation evidence itself - it never changes which endpoints are reported as
     * mirage APIs. Defaults to {@code false}.
     *
     * @return mutable boolean property controlling whether stub scanning is additionally performed
     */
    public abstract Property<Boolean> getScanMocks();

    /**
     * Directories to search recursively for WireMock stub mapping files ({@code *.json}), scanned
     * for stub evidence when {@link #getScanMocks()} is {@code true}. One or more directories may
     * be configured. Defaults to {@value #DEFAULT_STUB_DIR}.
     *
     * @return mutable file collection of WireMock stub directories
     */
    public abstract ConfigurableFileCollection getStubDirs();

    /**
     * The base path to strip from every path found under {@link #getStubDirs()} before comparing
     * it against the OpenAPI documentation, used only when {@link #getScanMocks()} is
     * {@code true}. A WireMock stub mapping records the full request path a client actually
     * sends - including whatever deployment-time context path the server runs under, e.g.
     * {@code /crm-service} - while an OpenAPI-declared path never includes one. Left unconfigured
     * (the default), this is instead read automatically from {@link #getRootDocument()}'s own
     * first {@code servers} entry's {@code url}, e.g. {@code http://localhost:9011/crm-service}
     * yields {@code /crm-service} - set this explicitly only when the document either declares no
     * {@code servers} entry or declares the wrong one for this purpose.
     *
     * @return mutable string property for the base path to strip from scanned stub paths
     */
    public abstract Property<String> getBasePath();

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
     * Whether the build should fail when mirage APIs are found. The report is written either way.
     * Defaults to {@code false}.
     *
     * @return mutable boolean property controlling whether the build fails on mirage APIs
     */
    public abstract Property<Boolean> getFailOnMirage();

    /**
     * Directory the AsciiDoc report is written to. Defaults to
     * {@code build/reports/mirage-api-detector}.
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
     * HTTP verb and path so the history is shared correctly with Shadow and Doppelganger API
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
     * <p>The {@value #UPDATE_CONTRACT_HISTORY_OVERRIDE_PROPERTY} project property, when set (e.g.
     * {@code -PmirageApiDetector.updateContractHistory=true}), overrides this property for every
     * project in the build regardless of what any project configures here - typically driven from a
     * Gradle property set differently per branch in the CI pipeline, since the plugin itself has no
     * notion of which branch is currently checked out.</p>
     *
     * @return mutable boolean property controlling whether the contract history file is written back
     */
    public abstract Property<Boolean> getUpdateContractHistory();

    /**
     * Exclusion rule strings, parsed by
     * {@link com.arc_e_tect.gradle.detector.core.exclude.ExclusionRule#parse(String)} - e.g.
     * {@code "/actuator/health"} (any verb) or {@code "GET /actuator/**"} (verb-restricted,
     * Ant-style {@code *}/{@code **} wildcards). A described endpoint matching any configured
     * rule - from here, {@link #getExcludeFiles()}, or {@link #getExcludeWellKnown()} - is still a
     * mirage API in fact (declared, unimplemented), but is reported under
     * {@code == Excluded Mirage APIs} instead of {@code == Mirage APIs}: it never fails
     * {@link #getFailOnMirage()} and never reaches {@link #getContractHistoryFile()}. Defaults to
     * empty.
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
     * {@link #getExcludeWellKnown()}. Lets a team check in its own reusable exclusion sets (e.g.
     * an org-wide file shared across projects) alongside per-project ones. Defaults to empty.
     *
     * @return mutable file collection of exclusion rule files
     */
    public abstract ConfigurableFileCollection getExcludeFiles();

    /**
     * Names of bundled, well-known exclusion sets to apply - e.g.
     * {@value com.arc_e_tect.gradle.detector.core.exclude.WellKnownExclusionSets#SPRING_BOOT_ACTUATOR}
     * for Spring Boot Actuator's management endpoints, which are provided by the framework's own
     * auto-configuration rather than a hand-written {@code @RestController} and so are
     * structurally invisible to the controller scan even when documented and fully functional.
     * Combined with {@link #getExcludePaths()} and {@link #getExcludeFiles()}. An unrecognised
     * name fails the build. Defaults to empty.
     *
     * @return mutable list property of well-known exclusion set names
     */
    public abstract ListProperty<String> getExcludeWellKnown();
}

package com.arc_e_tect.gradle.doppelganger;

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

/**
 * DSL extension for the Doppelganger API Detector Gradle plugin.
 *
 * <pre>
 * doppelgangerApiDetector {
 *     controllerDirs.from('src/main/java')                                    // default
 *     testDirs.from('src/testContract/java')                                  // default
 *     rootDocument   = file('src/main/resources/openapi/openapi.yaml')       // required
 *     // openApiDir  = rootDocument.get().asFile.parentFile                  // default
 *     // contractsDir = file('src/testContract/resources/contracts')        // required if useSpringCloudContract = true
 *     useRestDocs                 = true                                      // default
 *     useOpenApiRequestValidator  = false                                     // default
 *     useSpringCloudContract      = false                                     // default
 *     failOnDoppelganger = false                                              // default
 *     reportDir      = layout.buildDirectory.dir('reports/doppelganger-api-detector') // default
 *     reportFileName = 'doppelganger-apis.adoc'                               // default
 *     // systemUnderTestVersion = 'v1.0.0'          // optional; default: project.version
 *     trackContractHistory  = false                                           // default
 *     // contractHistoryFile = file('doppelganger-api-detector-contract-history.ndjson') // default
 *     updateContractHistory = trackContractHistory                            // default; see getUpdateContractHistory()
 *
 *     // excludePaths.add('/actuator/health')                                 // default: empty
 *     // excludeFiles.from('doppelganger-exclusions.yaml')                    // default: empty
 *     // excludeWellKnown.add('spring-boot-actuator')                        // default: empty
 *
 *     // Configuration for the separate `scanContracts` task - reuses controllerDirs, testDirs,
 *     // rootDocument, contractsDir, useRestDocs/useOpenApiRequestValidator/useSpringCloudContract,
 *     // and the exclude* properties above.
 *     includeResponseCoverage = false                                         // default
 *     // scanContractsReportFileName = 'contract-coverage.adoc'               // default
 *     trackResponseCoverageHistory = false                                    // default
 *     // responseCoverageHistoryFile = file('doppelganger-api-detector-response-coverage-history.ndjson') // default
 *     updateResponseCoverageHistory = trackResponseCoverageHistory            // default; see getUpdateResponseCoverageHistory()
 * }
 * </pre>
 *
 * <p>{@code updateContractHistory} can be overridden for the whole build from the command line,
 * e.g. {@code -PdoppelgangerApiDetector.updateContractHistory=true} - see
 * {@link #getUpdateContractHistory()}. {@code updateResponseCoverageHistory} has its own,
 * independent override - see {@link #getUpdateResponseCoverageHistory()}.</p>
 */
public abstract class DoppelgangerApiDetectorExtension {

    /** For use by the Gradle-generated concrete subclass. */
    public DoppelgangerApiDetectorExtension() {}

    /** Extension DSL block name, i.e. the name used to register the extension with the project. */
    public static final String NAME = "doppelgangerApiDetector";

    /** Default relative path of the directory searched for {@code @RestController} classes. */
    public static final String DEFAULT_CONTROLLER_DIR = "src/main/java";

    /** Default relative path of the directory searched for test classes. */
    public static final String DEFAULT_TEST_DIR = "src/testContract/java";

    /**
     * Suggested relative path of the directory searched for Spring Cloud Contract DSL files, shown
     * in the DSL example above. Not applied as a convention default - see {@link #getContractsDir()}.
     */
    public static final String DEFAULT_CONTRACTS_DIR = "src/testContract/resources/contracts";

    /** Default name of the generated AsciiDoc report file. */
    public static final String DEFAULT_REPORT_FILE_NAME = "doppelganger-apis.adoc";

    /** Default name of the persisted contract progress history file. */
    public static final String DEFAULT_CONTRACT_HISTORY_FILE_NAME =
            "doppelganger-api-detector-contract-history.ndjson";

    /**
     * Name of the Gradle project property that overrides {@link #getUpdateContractHistory()} from
     * the command line for every project in the build, e.g.
     * {@code -PdoppelgangerApiDetector.updateContractHistory=true}. Takes precedence over any
     * project's own configured {@code updateContractHistory} value. The value is parsed as a
     * boolean.
     */
    public static final String UPDATE_CONTRACT_HISTORY_OVERRIDE_PROPERTY =
            "doppelgangerApiDetector.updateContractHistory";

    /** Default name of the {@code scanContracts} task's generated AsciiDoc report file. */
    public static final String DEFAULT_SCAN_CONTRACTS_REPORT_FILE_NAME = "contract-coverage.adoc";

    /** Default name of the persisted response coverage history file. */
    public static final String DEFAULT_RESPONSE_COVERAGE_HISTORY_FILE_NAME =
            "doppelganger-api-detector-response-coverage-history.ndjson";

    /**
     * Name of the Gradle project property that overrides
     * {@link #getUpdateResponseCoverageHistory()} from the command line for every project in the
     * build, e.g. {@code -PdoppelgangerApiDetector.updateResponseCoverageHistory=true}. Takes
     * precedence over any project's own configured {@code updateResponseCoverageHistory} value.
     * The value is parsed as a boolean.
     */
    public static final String UPDATE_RESPONSE_COVERAGE_HISTORY_OVERRIDE_PROPERTY =
            "doppelgangerApiDetector.updateResponseCoverageHistory";

    /**
     * Directories to search recursively for {@code @RestController} classes. One or more
     * directories may be configured. Defaults to {@value #DEFAULT_CONTROLLER_DIR}.
     *
     * @return mutable file collection of controller source directories
     */
    public abstract ConfigurableFileCollection getControllerDirs();

    /**
     * Directories to search recursively for test classes, scanned by the Spring RestDocs and
     * OpenAPI request validator verification sources when enabled. One or more directories may be
     * configured. Defaults to {@value #DEFAULT_TEST_DIR}.
     *
     * @return mutable file collection of test source directories
     */
    public abstract ConfigurableFileCollection getTestDirs();

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
     * Directory searched for Spring Cloud Contract DSL files ({@code *.groovy} and {@code *.yml}),
     * scanned recursively when {@link #getUseSpringCloudContract()} is {@code true}.
     *
     * <p>Deliberately has no convention default, unlike this plugin's other directory properties:
     * enabling {@link #getUseSpringCloudContract()} without configuring this property is a DSL
     * configuration error, not a bootstrapping gap - {@code detectDoppelgangerApis} fails eagerly in
     * that case, rather than silently falling back to a guessed location. See
     * {@link DetectDoppelgangerApisTask#generate()}.</p>
     *
     * @return mutable directory property for the Spring Cloud Contract directory
     */
    public abstract DirectoryProperty getContractsDir();

    /**
        * Whether to treat Spring RestDocs test methods as verification evidence: either a
        * {@code spring-restdocs-mockmvc} {@code mockMvc.perform(...)} call paired with
        * {@code .andDo(document(...))}, a {@code spring-restdocs-webtestclient}
        * {@code webTestClient.get()/post()/put()/delete()/patch().uri(...).exchange()...}
        * chain paired with {@code .consumeWith(document(...))}, or a
        * {@code spring-restdocs-restassured} {@code .filter(document(...))} paired with a
        * {@code when().get(...)/post(...)/...} call.
     * Paths captured from a running-server style verification (REST Assured) have any leading
     * segment matching {@link #getRootDocument()}'s first {@code servers[].url} path stripped
     * before comparison, since that path is typically a servlet context path neither the OpenAPI
     * documentation nor the {@code @RestController} mapping itself declares. Defaults to
     * {@code true}.
     *
     * @return mutable boolean property controlling whether the Spring RestDocs source is enabled
     */
    public abstract Property<Boolean> getUseRestDocs();

    /**
     * Whether to treat Atlassian OpenAPI request validator usage as verification evidence.
     * Defaults to {@code false}.
     *
     * @return mutable boolean property controlling whether the OpenAPI request validator source
     *         is enabled
     */
    public abstract Property<Boolean> getUseOpenApiRequestValidator();

    /**
     * Whether to treat Spring Cloud Contract DSL files under {@link #getContractsDir()} as
     * verification evidence. Defaults to {@code false}.
     *
     * @return mutable boolean property controlling whether the Spring Cloud Contract source is
     *         enabled
     */
    public abstract Property<Boolean> getUseSpringCloudContract();

    /**
     * Whether the build should fail when doppelganger APIs are found. The report is written
     * either way. Defaults to {@code false}.
     *
     * @return mutable boolean property controlling whether the build fails on doppelganger APIs
     */
    public abstract Property<Boolean> getFailOnDoppelganger();

    /**
     * Directory the AsciiDoc report is written to. Defaults to
     * {@code build/reports/doppelganger-api-detector}.
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
     * HTTP verb and path so the history is shared correctly with Shadow and Mirage API Detector
     * when they're pointed at the same {@link #getContractHistoryFile()}. Defaults to
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
     * {@code -PdoppelgangerApiDetector.updateContractHistory=true}), overrides this property for
     * every project in the build regardless of what any project configures here - typically driven
     * from a Gradle property set differently per branch in the CI pipeline, since the plugin itself
     * has no notion of which branch is currently checked out.</p>
     *
     * @return mutable boolean property controlling whether the contract history file is written back
     */
    public abstract Property<Boolean> getUpdateContractHistory();

    /**
     * Exclusion rule strings, parsed by
     * {@link com.arc_e_tect.gradle.detector.core.exclude.ExclusionRule#parse(String)} - e.g.
     * {@code "/actuator/health"} (any verb) or {@code "GET /actuator/**"} (verb-restricted,
     * Ant-style {@code *}/{@code **} wildcards). A declared-and-implemented, unverified endpoint
     * matching any configured rule - from here, {@link #getExcludeFiles()}, or
     * {@link #getExcludeWellKnown()} - is still a doppelganger API in fact, but is reported under
     * {@code == Excluded Doppelganger APIs} instead of {@code == Doppelganger APIs}: it never
     * fails {@link #getFailOnDoppelganger()} and never reaches {@link #getContractHistoryFile()}.
     * Defaults to empty.
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

    /**
     * Whether the {@code scanContracts} task additionally computes, for every declared response
     * code, how many contract tests cover it. Defaults to {@code false}: the breakdown is not
     * merely hidden when disabled, it is never computed - this is the more expensive of the two
     * statistics {@code scanContracts} can report, since it requires detecting the asserted status
     * code of every matching contract test, not just whether one exists.
     *
     * <p>For example, an endpoint {@code GET /v1/foobars} declaring response codes {@code 200} and
     * {@code 404}, with two contract tests asserting {@code 200} and one asserting {@code 404},
     * reports {@code 200} as covered by 2 test(s) and {@code 404} as covered by 1 test(s) when this
     * is {@code true}.</p>
     *
     * @return mutable boolean property controlling whether response coverage is computed
     */
    public abstract Property<Boolean> getIncludeResponseCoverage();

    /**
     * Name of the {@code scanContracts} task's generated AsciiDoc report file (without path),
     * written to the same {@link #getReportDir()}. Defaults to
     * {@value #DEFAULT_SCAN_CONTRACTS_REPORT_FILE_NAME}.
     *
     * @return mutable string property for the scanContracts report file name
     */
    public abstract Property<String> getScanContractsReportFileName();

    /**
     * Whether to persist, across builds, a history of response code coverage - keyed by endpoint
     * fingerprint and response code, tracking a live test-count gauge rather than milestone
     * timestamps. Defaults to {@code false}. Only meaningful together with
     * {@link #getIncludeResponseCoverage()} - {@code scanContracts} fails eagerly if this is
     * {@code true} while that is {@code false}, since there would be no per-response-code data to
     * persist.
     *
     * @return mutable boolean property controlling whether response coverage history is tracked
     */
    public abstract Property<Boolean> getTrackResponseCoverageHistory();

    /**
     * File that the persisted response coverage history is read from and, when
     * {@link #getUpdateResponseCoverageHistory()} is {@code true}, written back to. Defaults to
     * {@value #DEFAULT_RESPONSE_COVERAGE_HISTORY_FILE_NAME} directly in the project directory -
     * deliberately not under {@code build/}, for the same reason as
     * {@link #getContractHistoryFile()}. Only consulted when
     * {@link #getTrackResponseCoverageHistory()} is {@code true}. Deliberately a separate file from
     * {@link #getContractHistoryFile()}: response coverage is a Doppelganger-only concern with a
     * different record schema, not shared with Shadow or Mirage API Detector.
     *
     * @return mutable file property for the response coverage history file
     */
    public abstract RegularFileProperty getResponseCoverageHistoryFile();

    /**
     * Whether {@link #getResponseCoverageHistoryFile()} is written back to disk after being updated
     * with the current run's coverage. Defaults to the same value as
     * {@link #getTrackResponseCoverageHistory()}. Only consulted when
     * {@link #getTrackResponseCoverageHistory()} is {@code true}; the history file is always read
     * regardless of this property's value.
     *
     * <p>The {@value #UPDATE_RESPONSE_COVERAGE_HISTORY_OVERRIDE_PROPERTY} project property, when
     * set, overrides this property for every project in the build - the same
     * per-branch-CI-pipeline pattern {@link #getUpdateContractHistory()} supports, independently of
     * it.</p>
     *
     * @return mutable boolean property controlling whether the response coverage history file is
     *         written back
     */
    public abstract Property<Boolean> getUpdateResponseCoverageHistory();
}

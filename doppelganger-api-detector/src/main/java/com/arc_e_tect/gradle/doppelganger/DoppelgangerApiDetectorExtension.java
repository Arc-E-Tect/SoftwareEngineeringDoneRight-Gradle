package com.arc_e_tect.gradle.doppelganger;

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;

/**
 * DSL extension for the Doppelganger API Detector Gradle plugin.
 *
 * <pre>
 * doppelgangerApiDetector {
 *     controllerDirs.from('src/main/java')                                    // default
 *     testDirs.from('src/test/java')                                          // default
 *     rootDocument   = file('src/main/resources/openapi/openapi.yaml')       // required
 *     // openApiDir  = rootDocument.get().asFile.parentFile                  // default
 *     // contractsDir = file('src/test/resources/contracts')                // default
 *     useRestDocs                 = true                                      // default
 *     useOpenApiRequestValidator  = true                                      // default
 *     useSpringCloudContract      = true                                      // default
 *     failOnDoppelganger = false                                              // default
 *     reportDir      = layout.buildDirectory.dir('reports/doppelganger-api-detector') // default
 *     reportFileName = 'doppelganger-apis.adoc'                               // default
 *     // systemUnderTestVersion = 'v1.0.0'          // optional; default: project.version
 * }
 * </pre>
 */
public abstract class DoppelgangerApiDetectorExtension {

    /** For use by the Gradle-generated concrete subclass. */
    public DoppelgangerApiDetectorExtension() {}

    /** Extension DSL block name, i.e. the name used to register the extension with the project. */
    public static final String NAME = "doppelgangerApiDetector";

    /** Default relative path of the directory searched for {@code @RestController} classes. */
    public static final String DEFAULT_CONTROLLER_DIR = "src/main/java";

    /** Default relative path of the directory searched for test classes. */
    public static final String DEFAULT_TEST_DIR = "src/test/java";

    /** Default relative path of the directory searched for Spring Cloud Contract DSL files. */
    public static final String DEFAULT_CONTRACTS_DIR = "src/test/resources/contracts";

    /** Default name of the generated AsciiDoc report file. */
    public static final String DEFAULT_REPORT_FILE_NAME = "doppelganger-apis.adoc";

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
     * scanned recursively when {@link #getUseSpringCloudContract()} is {@code true}. Defaults to
     * {@value #DEFAULT_CONTRACTS_DIR}.
     *
     * @return mutable directory property for the Spring Cloud Contract directory
     */
    public abstract DirectoryProperty getContractsDir();

    /**
     * Whether to treat Spring RestDocs test methods (a {@code mockMvc.perform(...)} call paired
     * with {@code .andDo(document(...))}) as verification evidence. Defaults to {@code true}.
     *
     * @return mutable boolean property controlling whether the Spring RestDocs source is enabled
     */
    public abstract Property<Boolean> getUseRestDocs();

    /**
     * Whether to treat Atlassian OpenAPI request validator usage as verification evidence.
     * Defaults to {@code true}.
     *
     * @return mutable boolean property controlling whether the OpenAPI request validator source
     *         is enabled
     */
    public abstract Property<Boolean> getUseOpenApiRequestValidator();

    /**
     * Whether to treat Spring Cloud Contract DSL files under {@link #getContractsDir()} as
     * verification evidence. Defaults to {@code true}.
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
}

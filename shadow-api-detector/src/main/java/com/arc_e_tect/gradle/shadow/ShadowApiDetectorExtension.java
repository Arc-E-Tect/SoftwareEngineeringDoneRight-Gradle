package com.arc_e_tect.gradle.shadow;

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
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
 * }
 * </pre>
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
}

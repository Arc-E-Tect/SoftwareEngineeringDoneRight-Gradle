package com.arc_e_tect.gradle.jacoco;

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;

/**
 * DSL extension for the JaCoCo exclusion report plugin.
 *
 * <pre>
 * jacocoExclusionReport {
 *     annotationName = 'ExcludeFromJacocoGeneratedCodeCoverage'   // default
 *     sourceDirs.from(sourceSets.main.java.srcDirs)                // default
 *     reportDir  = layout.buildDirectory.dir('reports/jacoco-exclusions') // default
 *     includeConfiguredExclusions = true                            // default
 * }
 * </pre>
 */
public abstract class JacocoExclusionReportExtension {

    /** For use by the Gradle-generated concrete subclass. */
    public JacocoExclusionReportExtension() {}

    /** Extension DSL block name, i.e. the name used to register the extension with the project. */
    public static final String NAME          = "jacocoExclusionReport";

    /** Default simple name of the annotation scanned for exclusions. */
    public static final String DEFAULT_ANNOTATION =
            "ExcludeFromJacocoGeneratedCodeCoverage";

    /**
     * Simple (unqualified) name of the exclusion annotation.
     *
     * @return mutable property holding the annotation simple name
     */
    public abstract Property<String> getAnnotationName();

    /**
     * Java source directories to scan.
     *
     * @return mutable file collection of source directories
     */
    public abstract ConfigurableFileCollection getSourceDirs();

    /**
     * Directory where the HTML and XML reports are written.
     *
     * @return mutable directory property for the report output location
     */
    public abstract DirectoryProperty getReportDir();

    /**
     * Whether exclusions configured through JaCoCo task DSL should be reported
     * in a separate report file in addition to annotation-based exclusions.
     *
     * @return mutable flag controlling JaCoCo DSL exclusion reporting
     */
    public abstract Property<Boolean> getIncludeConfiguredExclusions();
}

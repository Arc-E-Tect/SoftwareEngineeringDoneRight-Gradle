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
 *     includeGeneratedAnnotationExclusions = false                  // default (opt-in)
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

    /**
     * Whether to additionally scan compiled classes for members that JaCoCo
     * automatically excludes because they carry an annotation whose simple
     * name is {@code Generated} but that was not written by hand — e.g.
     * Lombok's {@code @lombok.Generated}, which Lombok stamps onto every
     * getter, setter, constructor, and other member it synthesises. These
     * members never appear in the {@code .java} source, so they can only be
     * found by inspecting the compiled {@code .class} files.
     *
     * <p>Disabled by default: it only takes effect when set to {@code true}
     * explicitly in the {@code jacocoExclusionReport} DSL block.</p>
     *
     * @return mutable flag controlling tool-generated {@code @Generated} exclusion reporting
     */
    public abstract Property<Boolean> getIncludeGeneratedAnnotationExclusions();
}

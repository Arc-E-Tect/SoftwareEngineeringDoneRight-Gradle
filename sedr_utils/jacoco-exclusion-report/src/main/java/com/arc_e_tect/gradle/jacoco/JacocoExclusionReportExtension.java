package com.arc_e_tect.gradle.jacoco;

import com.arc_e_tect.book.sedr.jacoco.marker.ExcludeFromJacocoGeneratedCodeCoverage;
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
 * }
 * </pre>
 */
@ExcludeFromJacocoGeneratedCodeCoverage(justification = "Abstract DSL extension — all concrete methods are Gradle-generated at runtime")
public abstract class JacocoExclusionReportExtension {

    public static final String NAME          = "jacocoExclusionReport";
    public static final String DEFAULT_ANNOTATION =
            "ExcludeFromJacocoGeneratedCodeCoverage";

    /** Simple (unqualified) name of the exclusion annotation. */
    public abstract Property<String> getAnnotationName();

    /** Java source directories to scan. */
    public abstract ConfigurableFileCollection getSourceDirs();

    /** Directory where the HTML and XML reports are written. */
    public abstract DirectoryProperty getReportDir();
}

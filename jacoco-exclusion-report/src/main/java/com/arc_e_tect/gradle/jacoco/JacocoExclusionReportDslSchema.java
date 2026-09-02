package com.arc_e_tect.gradle.jacoco;

import com.arc_e_tect.gradle.dslupdater.DslExtensionSchema;
import com.arc_e_tect.gradle.dslupdater.DslPropertySpec;

import java.util.List;

/**
 * The {@code jacocoExclusionReport {}} block's DSL property schema for
 * {@code updateJacocoExclusionReportDSL} (see {@code com.arc_e_tect.gradle.dslupdater.DslUpdater}).
 *
 * <p>Only lists properties that {@link JacocoExclusionReportPlugin#apply} actually gives a
 * resolved default via {@code convention(...)}. {@code sourceDirs} is left out on purpose - it is
 * a {@code ConfigurableFileCollection}, which has no {@code convention(...)} method at all; its
 * default ({@code sourceSets.main.java.srcDirs}) is set on the extension conditionally inside
 * {@code afterEvaluate} (only when still empty), once the {@code java} plugin's source sets are
 * known - not a single static literal a schema entry could safely represent. Each literal below is
 * the same value that property's own {@code convention(...)} call in
 * {@link JacocoExclusionReportPlugin#apply} resolves to, so that writing it out explicitly is
 * always a no-op.</p>
 */
final class JacocoExclusionReportDslSchema {

    static final DslExtensionSchema SCHEMA = new DslExtensionSchema(JacocoExclusionReportExtension.NAME, List.of(
            DslPropertySpec.scalar("annotationName", "'" + JacocoExclusionReportExtension.DEFAULT_ANNOTATION + "'",
                    "Simple (unqualified) name of the exclusion annotation scanned for."),
            DslPropertySpec.scalar("reportDir", "layout.buildDirectory.dir('reports/jacoco-exclusions')",
                    "Directory the HTML and XML reports are written to."),
            DslPropertySpec.scalar("includeConfiguredExclusions", "true",
                    "Whether exclusions configured through the JaCoCo task DSL are also reported."),
            DslPropertySpec.scalar("includeGeneratedAnnotationExclusions", "false",
                    "Whether class files carrying a tool-written @Generated annotation (e.g. Lombok) are also "
                            + "scanned for exclusions.")
    ));

    private JacocoExclusionReportDslSchema() {
    }
}

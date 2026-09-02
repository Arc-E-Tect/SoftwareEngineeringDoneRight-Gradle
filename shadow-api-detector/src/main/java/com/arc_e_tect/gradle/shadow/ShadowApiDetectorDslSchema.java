package com.arc_e_tect.gradle.shadow;

import com.arc_e_tect.gradle.dslupdater.DslExtensionSchema;
import com.arc_e_tect.gradle.dslupdater.DslPropertySpec;

import java.util.List;

/**
 * The {@code shadowApiDetector {}} block's DSL property schema for {@code updateShadowApiDetectorDSL}
 * (see {@code com.arc_e_tect.gradle.dslupdater.DslUpdater}).
 *
 * <p>Only lists properties that {@link ShadowApiDetectorPlugin#apply} actually gives a resolved
 * default via {@code convention(...)}. {@code rootDocument} is left out on purpose - it is
 * required, with deliberately no default at all. {@code controllerDirs} and {@code excludeFiles}
 * are also left out: {@code controllerDirs}'s default is applied to the task, not the extension,
 * conditionally inside {@code afterEvaluate} (only when still empty), and {@code excludeFiles} has
 * no default beyond "empty" - there's nothing a literal could usefully add for either. Each
 * literal below is the same value that property's own {@code convention(...)} call in
 * {@link ShadowApiDetectorPlugin#apply} resolves to, so that writing it out explicitly is always a
 * no-op.</p>
 */
final class ShadowApiDetectorDslSchema {

    static final DslExtensionSchema SCHEMA = new DslExtensionSchema(ShadowApiDetectorExtension.NAME, List.of(
            DslPropertySpec.scalar("failOnShadow", "false",
                    "Whether the build fails when shadow APIs are found; the report is written either way."),
            DslPropertySpec.scalar("reportDir", "layout.buildDirectory.dir('reports/shadow-api-detector')",
                    "Directory the AsciiDoc report is written to."),
            DslPropertySpec.scalar("reportFileName", "'" + ShadowApiDetectorExtension.DEFAULT_REPORT_FILE_NAME + "'",
                    "Name of the generated AsciiDoc report file, without path."),
            DslPropertySpec.scalar("systemUnderTestVersion", "project.version.toString()",
                    "Version of the scanned system under test, printed in the report."),
            DslPropertySpec.scalar("openApiDir", "layout.dir(rootDocument.map { it.asFile.parentFile })",
                    "Directory of OpenAPI descriptions, derived from rootDocument (which is required and has no "
                            + "default of its own - configure it separately); used only for up-to-date input tracking."),
            DslPropertySpec.scalar("trackContractHistory", "false",
                    "Whether to persist a cross-build history of each endpoint's contract lifecycle stages."),
            DslPropertySpec.scalar("contractHistoryFile",
                    "file('" + ShadowApiDetectorExtension.DEFAULT_CONTRACT_HISTORY_FILE_NAME + "')",
                    "File the contract progress history is read from and written back to."),
            DslPropertySpec.scalar("updateContractHistory", "trackContractHistory",
                    "Whether the contract history file is written back to disk after being updated; tracks "
                            + "trackContractHistory live rather than a fixed value."),
            DslPropertySpec.scalar("excludePaths", "[]",
                    "Exclusion rule strings, e.g. '/actuator/health' or 'GET /actuator/**'."),
            DslPropertySpec.scalar("excludeWellKnown", "[]",
                    "Names of bundled well-known exclusion sets to apply, e.g. 'spring-boot-actuator'.")
    ));

    private ShadowApiDetectorDslSchema() {
    }
}

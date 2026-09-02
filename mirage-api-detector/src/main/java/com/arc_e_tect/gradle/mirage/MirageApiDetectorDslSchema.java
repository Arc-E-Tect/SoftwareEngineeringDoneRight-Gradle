package com.arc_e_tect.gradle.mirage;

import com.arc_e_tect.gradle.dslupdater.DslExtensionSchema;
import com.arc_e_tect.gradle.dslupdater.DslPropertySpec;

import java.util.List;

/**
 * The {@code mirageApiDetector {}} block's DSL property schema for {@code updateMirageApiDetectorDSL}
 * (see {@code com.arc_e_tect.gradle.dslupdater.DslUpdater}).
 *
 * <p>Only lists properties that {@link MirageApiDetectorPlugin#apply} actually gives a resolved
 * default via {@code convention(...)}. {@code rootDocument} is left out on purpose - it is
 * required, with deliberately no default at all. {@code controllerDirs}, {@code stubDirs}, and
 * {@code stubSourceDirs} are also left out: their defaults are applied to the task, not the
 * extension, conditionally inside {@code afterEvaluate} (only when still empty, and for the
 * latter two only when {@code scanMocks} is true). {@code basePath} has no convention at all - it
 * falls back at task time to the root document's own {@code servers[].url} path. {@code excludeFiles}
 * has no default beyond "empty". There's nothing a literal could usefully add for any of these.
 * Each literal below is the same value that property's own {@code convention(...)} call in
 * {@link MirageApiDetectorPlugin#apply} resolves to, so that writing it out explicitly is always a
 * no-op.</p>
 */
final class MirageApiDetectorDslSchema {

    static final DslExtensionSchema SCHEMA = new DslExtensionSchema(MirageApiDetectorExtension.NAME, List.of(
            DslPropertySpec.scalar("failOnMirage", "false",
                    "Whether the build fails when mirage APIs are found; the report is written either way."),
            DslPropertySpec.scalar("scanMocks", "false",
                    "Whether WireMock stub evidence is additionally scanned alongside real implementations."),
            DslPropertySpec.scalar("reportDir", "layout.buildDirectory.dir('reports/mirage-api-detector')",
                    "Directory the AsciiDoc report is written to."),
            DslPropertySpec.scalar("reportFileName", "'" + MirageApiDetectorExtension.DEFAULT_REPORT_FILE_NAME + "'",
                    "Name of the generated AsciiDoc report file, without path."),
            DslPropertySpec.scalar("systemUnderTestVersion", "project.version.toString()",
                    "Version of the scanned system under test, printed in the report."),
            DslPropertySpec.scalar("openApiDir", "layout.dir(rootDocument.map { it.asFile.parentFile })",
                    "Directory of OpenAPI descriptions, derived from rootDocument (which is required and has no "
                            + "default of its own - configure it separately); used only for up-to-date input tracking."),
            DslPropertySpec.scalar("trackContractHistory", "false",
                    "Whether to persist a cross-build history of each endpoint's contract lifecycle stages."),
            DslPropertySpec.scalar("contractHistoryFile",
                    "file('" + MirageApiDetectorExtension.DEFAULT_CONTRACT_HISTORY_FILE_NAME + "')",
                    "File the contract progress history is read from and written back to."),
            DslPropertySpec.scalar("updateContractHistory", "trackContractHistory",
                    "Whether the contract history file is written back to disk after being updated; tracks "
                            + "trackContractHistory live rather than a fixed value."),
            DslPropertySpec.scalar("excludePaths", "[]",
                    "Exclusion rule strings, e.g. '/actuator/health' or 'GET /actuator/**'."),
            DslPropertySpec.scalar("excludeWellKnown", "[]",
                    "Names of bundled well-known exclusion sets to apply, e.g. 'spring-boot-actuator'.")
    ));

    private MirageApiDetectorDslSchema() {
    }
}

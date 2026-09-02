package com.arc_e_tect.gradle.doppelganger;

import com.arc_e_tect.gradle.dslupdater.DslExtensionSchema;
import com.arc_e_tect.gradle.dslupdater.DslPropertySpec;

import java.util.List;

/**
 * The {@code doppelgangerApiDetector {}} block's DSL property schema for
 * {@code updateDoppelgangerApiDetectorDSL} (see {@code com.arc_e_tect.gradle.dslupdater.DslUpdater}).
 *
 * <p>Only lists properties that {@link DoppelgangerApiDetectorPlugin#apply} actually gives a
 * resolved default via {@code convention(...)}. {@code rootDocument} is left out on purpose - it
 * is required, with deliberately no default at all. {@code controllerDirs} and {@code testDirs}
 * are also left out: their defaults are applied to the task, not the extension, conditionally
 * inside {@code afterEvaluate} (only when still empty). {@code contractsDir} deliberately has no
 * convention either - it's required only when {@code useSpringCloudContract} is true, validated
 * eagerly at task time, not defaulted. {@code propertyFiles} and {@code excludeFiles} have no
 * default beyond "empty". There's nothing a literal could usefully add for any of these. Each
 * literal below is the same value that property's own {@code convention(...)} call in
 * {@link DoppelgangerApiDetectorPlugin#apply} resolves to, so that writing it out explicitly is
 * always a no-op.</p>
 */
final class DoppelgangerApiDetectorDslSchema {

    static final DslExtensionSchema SCHEMA =
            new DslExtensionSchema(DoppelgangerApiDetectorExtension.NAME, List.of(
                    DslPropertySpec.scalar("failOnDoppelganger", "false",
                            "Whether the build fails when doppelganger APIs are found; the report is written either way."),
                    DslPropertySpec.scalar("useRestDocs", "true",
                            "Whether Spring RestDocs test methods count as verification evidence."),
                    DslPropertySpec.scalar("useOpenApiRequestValidator", "false",
                            "Whether Atlassian OpenAPI request validator usage counts as verification evidence."),
                    DslPropertySpec.scalar("useSpringCloudContract", "false",
                            "Whether Spring Cloud Contract DSL files count as verification evidence."),
                    DslPropertySpec.scalar("reportDir", "layout.buildDirectory.dir('reports/doppelganger-api-detector')",
                            "Directory the AsciiDoc report is written to."),
                    DslPropertySpec.scalar("reportFileName",
                            "'" + DoppelgangerApiDetectorExtension.DEFAULT_REPORT_FILE_NAME + "'",
                            "Name of the generated AsciiDoc report file, without path."),
                    DslPropertySpec.scalar("systemUnderTestVersion", "project.version.toString()",
                            "Version of the scanned system under test, printed in the report."),
                    DslPropertySpec.scalar("openApiDir", "layout.dir(rootDocument.map { it.asFile.parentFile })",
                            "Directory of OpenAPI descriptions, derived from rootDocument (which is required and has "
                                    + "no default of its own - configure it separately); used only for up-to-date "
                                    + "input tracking."),
                    DslPropertySpec.scalar("trackContractHistory", "false",
                            "Whether to persist a cross-build history of each endpoint's contract lifecycle stages."),
                    DslPropertySpec.scalar("contractHistoryFile",
                            "file('" + DoppelgangerApiDetectorExtension.DEFAULT_CONTRACT_HISTORY_FILE_NAME + "')",
                            "File the contract progress history is read from and written back to."),
                    DslPropertySpec.scalar("updateContractHistory", "trackContractHistory",
                            "Whether the contract history file is written back to disk after being updated; tracks "
                                    + "trackContractHistory live rather than a fixed value."),
                    DslPropertySpec.scalar("excludePaths", "[]",
                            "Exclusion rule strings, e.g. '/actuator/health' or 'GET /actuator/**'."),
                    DslPropertySpec.scalar("excludeWellKnown", "[]",
                            "Names of bundled well-known exclusion sets to apply, e.g. 'spring-boot-actuator'."),
                    DslPropertySpec.scalar("pathResolverHelperMethods", "[]",
                            "Static helper-method conventions ('ClassName.methodName') recognised when resolving "
                                    + "request paths."),
                    DslPropertySpec.scalar("includeResponseCoverage", "false",
                            "Whether scanContracts computes per-response-code test coverage."),
                    DslPropertySpec.scalar("ignore5xx", "false",
                            "Whether 5xx response codes are excluded from the response coverage breakdown."),
                    DslPropertySpec.scalar("scanContractsReportFileName",
                            "'" + DoppelgangerApiDetectorExtension.DEFAULT_SCAN_CONTRACTS_REPORT_FILE_NAME + "'",
                            "Name of the scanContracts task's generated AsciiDoc report file, written to the same "
                                    + "reportDir."),
                    DslPropertySpec.scalar("trackResponseCoverageHistory", "false",
                            "Whether to persist a cross-build response-code coverage history."),
                    DslPropertySpec.scalar("responseCoverageHistoryFile",
                            "file('" + DoppelgangerApiDetectorExtension.DEFAULT_RESPONSE_COVERAGE_HISTORY_FILE_NAME + "')",
                            "File the response coverage history is read from and written back to."),
                    DslPropertySpec.scalar("updateResponseCoverageHistory", "trackResponseCoverageHistory",
                            "Whether the response coverage history file is written back to disk; tracks "
                                    + "trackResponseCoverageHistory live rather than a fixed value.")
            ));

    private DoppelgangerApiDetectorDslSchema() {
    }
}

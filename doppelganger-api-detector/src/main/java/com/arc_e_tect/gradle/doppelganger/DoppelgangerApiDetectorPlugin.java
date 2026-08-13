package com.arc_e_tect.gradle.doppelganger;

import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;

import java.util.Locale;

/**
 * Gradle plugin that registers the {@code detectDoppelgangerApis} task and wires the
 * {@code doppelgangerApiDetector} DSL extension into the project.
 *
 * <h2>Usage</h2>
 * <pre>
 * plugins {
 *     id 'com.arc-e-tect.doppelganger-api-detector'
 * }
 *
 * doppelgangerApiDetector {
 *     rootDocument = file('src/main/resources/openapi/openapi.yaml')
 * }
 * </pre>
 *
 * <h2>Defaults</h2>
 * <ul>
 *   <li>Controller directories: {@code src/main/java}</li>
 *   <li>Test directories: {@code src/testContract/java}</li>
 *   <li>OpenAPI description directory: the root document's own parent directory</li>
 *   <li>Contracts directory: {@code src/testContract/resources/contracts}</li>
 *   <li>Contract verification sources: Spring RestDocs, OpenAPI request validator, and Spring
 *       Cloud Contract, all enabled</li>
 *   <li>Fail on doppelganger APIs: {@code false}</li>
 *   <li>Report directory: {@code build/reports/doppelganger-api-detector}</li>
 *   <li>Report file name: {@code doppelganger-apis.adoc}</li>
 *   <li>Track contract history: {@code false}</li>
 *   <li>Contract history file: {@code doppelganger-api-detector-contract-history.ndjson} (project directory)</li>
 *   <li>Update contract history: same as track contract history</li>
 * </ul>
 *
 * <p>The task is <strong>not</strong> wired into {@code check} or {@code build} automatically -
 * teams generating their OpenAPI documentation from code, or migrating their tests onto one of the
 * supported verification mechanisms gradually, would otherwise see every build fail immediately.
 * Opt in explicitly once the task is safe to run as part of your build:</p>
 * <pre>
 * tasks.named('check') {
 *     dependsOn 'detectDoppelgangerApis'
 * }
 * </pre>
 */
public class DoppelgangerApiDetectorPlugin implements Plugin<Project> {

    /** Name of the Gradle task registered by this plugin. */
    public static final String TASK_NAME = "detectDoppelgangerApis";

    /** Creates a new plugin instance. Instantiated by Gradle infrastructure. */
    public DoppelgangerApiDetectorPlugin() {}

    @Override
    public void apply(Project project) {
        DoppelgangerApiDetectorExtension ext = project.getExtensions()
                .create(DoppelgangerApiDetectorExtension.NAME, DoppelgangerApiDetectorExtension.class);

        ext.getFailOnDoppelganger().convention(false);
        ext.getUseRestDocs().convention(true);
        ext.getUseOpenApiRequestValidator().convention(false);
        ext.getUseSpringCloudContract().convention(false);
        ext.getReportDir().convention(
                project.getLayout().getBuildDirectory().dir("reports/doppelganger-api-detector"));
        ext.getReportFileName().convention(DoppelgangerApiDetectorExtension.DEFAULT_REPORT_FILE_NAME);
        ext.getSystemUnderTestVersion().convention(
                project.provider(() -> String.valueOf(project.getVersion())));
        ext.getOpenApiDir().convention(ext.getRootDocument().flatMap(rootDocument ->
                project.getLayout().dir(project.provider(() -> rootDocument.getAsFile().getParentFile()))));
        ext.getContractsDir().convention(project.getLayout().getProjectDirectory()
                .dir(DoppelgangerApiDetectorExtension.DEFAULT_CONTRACTS_DIR));

        ext.getTrackContractHistory().convention(false);
        ext.getContractHistoryFile().convention(project.getLayout().getProjectDirectory()
                .file(DoppelgangerApiDetectorExtension.DEFAULT_CONTRACT_HISTORY_FILE_NAME));
        // updateContractHistory defaults to trackContractHistory's own value, tracking it live
        // rather than snapshotting it at this point.
        ext.getUpdateContractHistory().convention(ext.getTrackContractHistory());

        // The -PdoppelgangerApiDetector.updateContractHistory=<true|false> project property, when
        // set, overrides updateContractHistory for every project in the build - regardless of what
        // any project's own extension configures - typically used to advance the committed history
        // only from the branch(es) whose CI pipeline should, without touching the build script.
        Provider<Boolean> updateContractHistoryCliOverride = project.getProviders()
                .gradleProperty(DoppelgangerApiDetectorExtension.UPDATE_CONTRACT_HISTORY_OVERRIDE_PROPERTY)
                .map(DoppelgangerApiDetectorPlugin::parseUpdateContractHistory);

        TaskProvider<DetectDoppelgangerApisTask> taskProvider =
                project.getTasks().register(TASK_NAME, DetectDoppelgangerApisTask.class, task -> {
                    task.getControllerDirs().from(ext.getControllerDirs());
                    task.getTestDirs().from(ext.getTestDirs());
                    task.getRootDocument().set(ext.getRootDocument());
                    task.getOpenApiDir().set(ext.getOpenApiDir());
                    task.getContractsDir().set(ext.getContractsDir());
                    task.getUseRestDocs().set(ext.getUseRestDocs());
                    task.getUseOpenApiRequestValidator().set(ext.getUseOpenApiRequestValidator());
                    task.getUseSpringCloudContract().set(ext.getUseSpringCloudContract());
                    task.getFailOnDoppelganger().set(ext.getFailOnDoppelganger());
                    task.getReportDir().set(ext.getReportDir());
                    task.getReportFileName().set(ext.getReportFileName());
                    task.getSystemUnderTestVersion().set(ext.getSystemUnderTestVersion());
                    task.getTrackContractHistory().set(ext.getTrackContractHistory());
                    task.getContractHistoryFile().set(ext.getContractHistoryFile());
                    task.getUpdateContractHistory().set(
                            updateContractHistoryCliOverride.orElse(ext.getUpdateContractHistory()));
                });

        // Default controllerDirs/testDirs only when the user has not configured them themselves;
        // deferred to afterEvaluate so the check happens once the build script has had a chance to
        // configure the extension.
        project.afterEvaluate(p -> {
            if (ext.getControllerDirs().isEmpty()) {
                taskProvider.configure(task -> task.getControllerDirs()
                        .from(p.file(DoppelgangerApiDetectorExtension.DEFAULT_CONTROLLER_DIR)));
            }
            if (ext.getTestDirs().isEmpty()) {
                taskProvider.configure(task -> task.getTestDirs()
                        .from(p.file(DoppelgangerApiDetectorExtension.DEFAULT_TEST_DIR)));
            }
        });
    }

    /**
     * Parses the {@code -PdoppelgangerApiDetector.updateContractHistory=<value>} project property's
     * value, accepting {@code true}/{@code false} case-insensitively.
     */
    private static boolean parseUpdateContractHistory(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if ("true".equals(normalized)) {
            return true;
        }
        if ("false".equals(normalized)) {
            return false;
        }
        throw new GradleException(
                "doppelgangerApiDetector: invalid value '" + value + "' for -P"
                + DoppelgangerApiDetectorExtension.UPDATE_CONTRACT_HISTORY_OVERRIDE_PROPERTY
                + "; expected 'true' or 'false'");
    }
}

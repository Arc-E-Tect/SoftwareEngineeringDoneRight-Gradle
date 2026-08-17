package com.arc_e_tect.gradle.mirage;

import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;

import java.util.Locale;

/**
 * Gradle plugin that registers the {@code detectMirageApis} task and wires the
 * {@code mirageApiDetector} DSL extension into the project.
 *
 * <h2>Usage</h2>
 * <pre>
 * plugins {
 *     id 'com.arc-e-tect.mirage-api-detector'
 * }
 *
 * mirageApiDetector {
 *     rootDocument = file('src/main/resources/openapi/openapi.yaml')
 * }
 * </pre>
 *
 * <h2>Defaults</h2>
 * <ul>
 *   <li>Controller directories: {@code src/main/java}</li>
 *   <li>OpenAPI description directory: the root document's own parent directory</li>
 *   <li>Additionally scan WireMock stubs: {@code false}</li>
 *   <li>WireMock stub directories: {@code src/test/resources/mappings} (used only when scanning
 *       mocks)</li>
 *   <li>Fail on mirage APIs: {@code false}</li>
 *   <li>Report directory: {@code build/reports/mirage-api-detector}</li>
 *   <li>Report file name: {@code mirage-apis.adoc}</li>
 *   <li>Track contract history: {@code false}</li>
 *   <li>Contract history file: {@code mirage-api-detector-contract-history.ndjson} (project directory)</li>
 *   <li>Update contract history: same as track contract history</li>
 * </ul>
 *
 * <p>The task is <strong>not</strong> wired into {@code check} or {@code build} automatically -
 * teams that write their OpenAPI documentation ahead of the implementation would otherwise see
 * every build fail on endpoints that simply have not been built yet. Opt in explicitly once the
 * task is safe to run as part of your build:</p>
 * <pre>
 * tasks.named('check') {
 *     dependsOn 'detectMirageApis'
 * }
 * </pre>
 */
public class MirageApiDetectorPlugin implements Plugin<Project> {

    /** Name of the Gradle task registered by this plugin. */
    public static final String TASK_NAME = "detectMirageApis";

    /** Name of the contract history migration task registered by this plugin. */
    public static final String MIGRATE_CONTRACT_HISTORY_TASK_NAME = "migrateContractHistory";

    /** Creates a new plugin instance. Instantiated by Gradle infrastructure. */
    public MirageApiDetectorPlugin() {}

    @Override
    public void apply(Project project) {
        MirageApiDetectorExtension ext = project.getExtensions()
                .create(MirageApiDetectorExtension.NAME, MirageApiDetectorExtension.class);

        ext.getFailOnMirage().convention(false);
        ext.getScanMocks().convention(false);
        ext.getReportDir().convention(
                project.getLayout().getBuildDirectory().dir("reports/mirage-api-detector"));
        ext.getReportFileName().convention(MirageApiDetectorExtension.DEFAULT_REPORT_FILE_NAME);
        ext.getSystemUnderTestVersion().convention(
                project.provider(() -> String.valueOf(project.getVersion())));
        ext.getOpenApiDir().convention(ext.getRootDocument().flatMap(rootDocument ->
                project.getLayout().dir(project.provider(() -> rootDocument.getAsFile().getParentFile()))));

        ext.getTrackContractHistory().convention(false);
        ext.getContractHistoryFile().convention(project.getLayout().getProjectDirectory()
                .file(MirageApiDetectorExtension.DEFAULT_CONTRACT_HISTORY_FILE_NAME));
        // updateContractHistory defaults to trackContractHistory's own value, tracking it live
        // rather than snapshotting it at this point.
        ext.getUpdateContractHistory().convention(ext.getTrackContractHistory());

        // The -PmirageApiDetector.updateContractHistory=<true|false> project property, when set,
        // overrides updateContractHistory for every project in the build - regardless of what any
        // project's own extension configures - typically used to advance the committed history
        // only from the branch(es) whose CI pipeline should, without touching the build script.
        Provider<Boolean> updateContractHistoryCliOverride = project.getProviders()
                .gradleProperty(MirageApiDetectorExtension.UPDATE_CONTRACT_HISTORY_OVERRIDE_PROPERTY)
                .map(MirageApiDetectorPlugin::parseUpdateContractHistory);

        TaskProvider<DetectMirageApisTask> taskProvider =
                project.getTasks().register(TASK_NAME, DetectMirageApisTask.class, task -> {
                    task.getControllerDirs().from(ext.getControllerDirs());
                    task.getScanMocks().set(ext.getScanMocks());
                    task.getStubDirs().from(ext.getStubDirs());
                    task.getBasePath().set(ext.getBasePath());
                    task.getRootDocument().set(ext.getRootDocument());
                    task.getOpenApiDir().set(ext.getOpenApiDir());
                    task.getFailOnMirage().set(ext.getFailOnMirage());
                    task.getReportDir().set(ext.getReportDir());
                    task.getReportFileName().set(ext.getReportFileName());
                    task.getSystemUnderTestVersion().set(ext.getSystemUnderTestVersion());
                    task.getTrackContractHistory().set(ext.getTrackContractHistory());
                    task.getContractHistoryFile().set(ext.getContractHistoryFile());
                    task.getUpdateContractHistory().set(
                            updateContractHistoryCliOverride.orElse(ext.getUpdateContractHistory()));
                });

        // controllerDirs defaults unconditionally - it's always scanned, regardless of scanMocks -
        // while stubDirs only defaults when stub scanning is actually active; deferred to
        // afterEvaluate so both checks happen once the build script has had a chance to configure
        // the extension, and only when the user has not configured the directory themselves.
        project.afterEvaluate(p -> {
            if (ext.getControllerDirs().isEmpty()) {
                taskProvider.configure(task ->
                        task.getControllerDirs().from(p.file(MirageApiDetectorExtension.DEFAULT_CONTROLLER_DIR)));
            }
            if (ext.getScanMocks().get() && ext.getStubDirs().isEmpty()) {
                taskProvider.configure(task ->
                        task.getStubDirs().from(p.file(MirageApiDetectorExtension.DEFAULT_STUB_DIR)));
            }
        });

        TaskProvider<MigrateContractHistoryTask> migrateTaskProvider = project.getTasks().register(
                MIGRATE_CONTRACT_HISTORY_TASK_NAME, MigrateContractHistoryTask.class, task -> {
                    task.getControllerDirs().from(ext.getControllerDirs());
                    task.getStubDirs().from(ext.getStubDirs());
                    task.getContractHistoryFile().set(ext.getContractHistoryFile());
                });

        // Unlike detectMirageApis, migration always needs both controller and stub directories
        // regardless of scanMocks, since it must independently check each legacy record against
        // both kinds of evidence - so both defaults apply unconditionally here.
        project.afterEvaluate(p -> {
            if (ext.getControllerDirs().isEmpty()) {
                migrateTaskProvider.configure(task ->
                        task.getControllerDirs().from(p.file(MirageApiDetectorExtension.DEFAULT_CONTROLLER_DIR)));
            }
            if (ext.getStubDirs().isEmpty()) {
                migrateTaskProvider.configure(task ->
                        task.getStubDirs().from(p.file(MirageApiDetectorExtension.DEFAULT_STUB_DIR)));
            }
        });
    }

    /**
     * Parses the {@code -PmirageApiDetector.updateContractHistory=<value>} project property's
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
                "mirageApiDetector: invalid value '" + value + "' for -P"
                + MirageApiDetectorExtension.UPDATE_CONTRACT_HISTORY_OVERRIDE_PROPERTY
                + "; expected 'true' or 'false'");
    }
}

package com.arc_e_tect.gradle.mirage;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;

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
 *   <li>Scan mocks instead of controllers: {@code false}</li>
 *   <li>WireMock stub directories: {@code src/test/resources/mappings} (used only when scanning
 *       mocks)</li>
 *   <li>Fail on mirage APIs: {@code false}</li>
 *   <li>Report directory: {@code build/reports/mirage-api-detector}</li>
 *   <li>Report file name: {@code mirage-apis.adoc}</li>
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

        TaskProvider<DetectMirageApisTask> taskProvider =
                project.getTasks().register(TASK_NAME, DetectMirageApisTask.class, task -> {
                    task.getControllerDirs().from(ext.getControllerDirs());
                    task.getScanMocks().set(ext.getScanMocks());
                    task.getStubDirs().from(ext.getStubDirs());
                    task.getRootDocument().set(ext.getRootDocument());
                    task.getOpenApiDir().set(ext.getOpenApiDir());
                    task.getFailOnMirage().set(ext.getFailOnMirage());
                    task.getReportDir().set(ext.getReportDir());
                    task.getReportFileName().set(ext.getReportFileName());
                    task.getSystemUnderTestVersion().set(ext.getSystemUnderTestVersion());
                });

        // Default controllerDirs/stubDirs only when the corresponding scanning mode is active and
        // the user has not configured them themselves; deferred to afterEvaluate so the check
        // happens once the build script has had a chance to configure the extension.
        project.afterEvaluate(p -> {
            if (!ext.getScanMocks().get() && ext.getControllerDirs().isEmpty()) {
                taskProvider.configure(task ->
                        task.getControllerDirs().from(p.file(MirageApiDetectorExtension.DEFAULT_CONTROLLER_DIR)));
            }
            if (ext.getScanMocks().get() && ext.getStubDirs().isEmpty()) {
                taskProvider.configure(task ->
                        task.getStubDirs().from(p.file(MirageApiDetectorExtension.DEFAULT_STUB_DIR)));
            }
        });
    }
}

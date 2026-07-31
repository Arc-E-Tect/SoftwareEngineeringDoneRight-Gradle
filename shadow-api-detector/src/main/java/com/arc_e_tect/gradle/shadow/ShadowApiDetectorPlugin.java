package com.arc_e_tect.gradle.shadow;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;

/**
 * Gradle plugin that registers the {@code detectShadowApis} task and wires the
 * {@code shadowApiDetector} DSL extension into the project.
 *
 * <h2>Usage</h2>
 * <pre>
 * plugins {
 *     id 'com.arc-e-tect.shadow-api-detector'
 * }
 *
 * shadowApiDetector {
 *     rootDocument = file('src/main/resources/openapi/openapi.yaml')
 * }
 * </pre>
 *
 * <h2>Defaults</h2>
 * <ul>
 *   <li>Controller directories: {@code src/main/java}</li>
 *   <li>OpenAPI description directory: the root document's own parent directory</li>
 *   <li>Fail on shadow APIs: {@code false}</li>
 *   <li>Report directory: {@code build/reports/shadow-api-detector}</li>
 *   <li>Report file name: {@code shadow-apis.adoc}</li>
 * </ul>
 *
 * <p>The task is added as a dependency of {@code check}, so running {@code ./gradlew check}
 * always regenerates the report.</p>
 */
public class ShadowApiDetectorPlugin implements Plugin<Project> {

    /** Name of the Gradle task registered by this plugin. */
    public static final String TASK_NAME = "detectShadowApis";

    /** Creates a new plugin instance. Instantiated by Gradle infrastructure. */
    public ShadowApiDetectorPlugin() {}

    @Override
    public void apply(Project project) {
        ShadowApiDetectorExtension ext = project.getExtensions()
                .create(ShadowApiDetectorExtension.NAME, ShadowApiDetectorExtension.class);

        ext.getFailOnShadow().convention(false);
        ext.getReportDir().convention(
                project.getLayout().getBuildDirectory().dir("reports/shadow-api-detector"));
        ext.getReportFileName().convention(ShadowApiDetectorExtension.DEFAULT_REPORT_FILE_NAME);
        ext.getSystemUnderTestVersion().convention(
                project.provider(() -> String.valueOf(project.getVersion())));
        ext.getOpenApiDir().convention(ext.getRootDocument().flatMap(rootDocument ->
                project.getLayout().dir(project.provider(() -> rootDocument.getAsFile().getParentFile()))));

        TaskProvider<DetectShadowApisTask> taskProvider =
                project.getTasks().register(TASK_NAME, DetectShadowApisTask.class, task -> {
                    task.getControllerDirs().from(ext.getControllerDirs());
                    task.getRootDocument().set(ext.getRootDocument());
                    task.getOpenApiDir().set(ext.getOpenApiDir());
                    task.getFailOnShadow().set(ext.getFailOnShadow());
                    task.getReportDir().set(ext.getReportDir());
                    task.getReportFileName().set(ext.getReportFileName());
                    task.getSystemUnderTestVersion().set(ext.getSystemUnderTestVersion());
                });

        // Default to src/main/java only when the user has not configured any controller
        // directories themselves; deferred to afterEvaluate so the check happens once the build
        // script has had a chance to configure the extension.
        project.afterEvaluate(p -> {
            if (ext.getControllerDirs().isEmpty()) {
                taskProvider.configure(task ->
                        task.getControllerDirs().from(p.file(ShadowApiDetectorExtension.DEFAULT_CONTROLLER_DIR)));
            }
        });

        project.getPluginManager().withPlugin("java", ignored ->
                project.getTasks().named("check").configure(check -> check.dependsOn(taskProvider)));
    }
}

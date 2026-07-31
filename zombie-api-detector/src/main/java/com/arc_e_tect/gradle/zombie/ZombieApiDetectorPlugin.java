package com.arc_e_tect.gradle.zombie;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;

/**
 * Gradle plugin that registers the {@code detectZombieApis} task and wires the
 * {@code zombieApiDetector} DSL extension into the project.
 *
 * <h2>Usage</h2>
 * <pre>
 * plugins {
 *     id 'com.arc-e-tect.zombie-api-detector'
 * }
 *
 * zombieApiDetector {
 *     rootDocument = file('src/main/resources/openapi/openapi.yaml')
 * }
 * </pre>
 *
 * <h2>Defaults</h2>
 * <ul>
 *   <li>Controller directories: {@code src/main/java}</li>
 *   <li>OpenAPI description directory: the root document's own parent directory</li>
 *   <li>Fail on zombie APIs: {@code false}</li>
 *   <li>Report directory: {@code build/reports/zombie-api-detector}</li>
 *   <li>Report file name: {@code zombie-apis.adoc}</li>
 * </ul>
 *
 * <p>The task is added as a dependency of {@code check}, so running {@code ./gradlew check}
 * always regenerates the report.</p>
 */
public class ZombieApiDetectorPlugin implements Plugin<Project> {

    /** Name of the Gradle task registered by this plugin. */
    public static final String TASK_NAME = "detectZombieApis";

    /** Creates a new plugin instance. Instantiated by Gradle infrastructure. */
    public ZombieApiDetectorPlugin() {}

    @Override
    public void apply(Project project) {
        ZombieApiDetectorExtension ext = project.getExtensions()
                .create(ZombieApiDetectorExtension.NAME, ZombieApiDetectorExtension.class);

        ext.getFailOnZombie().convention(false);
        ext.getReportDir().convention(
                project.getLayout().getBuildDirectory().dir("reports/zombie-api-detector"));
        ext.getReportFileName().convention(ZombieApiDetectorExtension.DEFAULT_REPORT_FILE_NAME);
        ext.getOpenApiDir().convention(ext.getRootDocument().flatMap(rootDocument ->
                project.getLayout().dir(project.provider(() -> rootDocument.getAsFile().getParentFile()))));

        TaskProvider<DetectZombieApisTask> taskProvider =
                project.getTasks().register(TASK_NAME, DetectZombieApisTask.class, task -> {
                    task.getControllerDirs().from(ext.getControllerDirs());
                    task.getRootDocument().set(ext.getRootDocument());
                    task.getOpenApiDir().set(ext.getOpenApiDir());
                    task.getFailOnZombie().set(ext.getFailOnZombie());
                    task.getReportDir().set(ext.getReportDir());
                    task.getReportFileName().set(ext.getReportFileName());
                });

        // Default to src/main/java only when the user has not configured any controller
        // directories themselves; deferred to afterEvaluate so the check happens once the build
        // script has had a chance to configure the extension.
        project.afterEvaluate(p -> {
            if (ext.getControllerDirs().isEmpty()) {
                taskProvider.configure(task ->
                        task.getControllerDirs().from(p.file(ZombieApiDetectorExtension.DEFAULT_CONTROLLER_DIR)));
            }
        });

        project.getPluginManager().withPlugin("java", ignored ->
                project.getTasks().named("check").configure(check -> check.dependsOn(taskProvider)));
    }
}

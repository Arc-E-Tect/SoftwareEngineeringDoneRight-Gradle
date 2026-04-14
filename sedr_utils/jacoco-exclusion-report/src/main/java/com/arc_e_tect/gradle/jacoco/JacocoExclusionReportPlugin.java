package com.arc_e_tect.gradle.jacoco;

import com.arc_e_tect.book.sedr.jacoco.marker.ExcludeFromJacocoGeneratedCodeCoverage;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.TaskProvider;

/**
 * Gradle plugin that registers the {@code jacocoExclusionReport} task and
 * wires it into the standard verification lifecycle.
 *
 * <h2>Usage</h2>
 * <pre>
 * // settings.gradle (composite build)
 * includeBuild '../sedr_utils/jacoco-exclusion-report'
 *
 * // build.gradle
 * plugins {
 *     id 'com.arc-e-tect.jacoco-exclusion-report'
 * }
 * </pre>
 *
 * <h2>Lifecycle</h2>
 * <ul>
 *   <li>The task is added as a dependency of {@code check}.</li>
 *   <li>If {@code jacocoTestCoverageVerification} is present the report runs
 *       before it, so coverage numbers and the exclusion audit are always
 *       produced together.</li>
 * </ul>
 *
 * <h2>Defaults</h2>
 * <ul>
 *   <li>Annotation: {@code ExcludeFromJacocoGeneratedCodeCoverage}</li>
 *   <li>Sources: {@code sourceSets.main.java.srcDirs} (when the Java plugin is applied)</li>
 *   <li>Output: {@code build/reports/jacoco-exclusions/}</li>
 * </ul>
 */
public class JacocoExclusionReportPlugin implements Plugin<Project> {

    /** Name of the Gradle task registered by this plugin. */
    public static final String TASK_NAME = "jacocoExclusionReport";

    /** Creates a new plugin instance. Instantiated by Gradle infrastructure. */
    public JacocoExclusionReportPlugin() {}

    @Override
    @ExcludeFromJacocoGeneratedCodeCoverage(justification = "Gradle plugin wiring — requires a full Gradle test kit to exercise")
    public void apply(Project project) {
        JacocoExclusionReportExtension ext = project.getExtensions()
                .create(JacocoExclusionReportExtension.NAME,
                        JacocoExclusionReportExtension.class);

        // Sensible defaults
        ext.getAnnotationName().convention(JacocoExclusionReportExtension.DEFAULT_ANNOTATION);
        ext.getReportDir().convention(
                project.getLayout().getBuildDirectory().dir("reports/jacoco-exclusions"));

        // Register the task
        TaskProvider<JacocoExclusionReportTask> taskProvider =
                project.getTasks().register(TASK_NAME, JacocoExclusionReportTask.class, task -> {
                    task.getAnnotationName().set(ext.getAnnotationName());
                    task.getReportDir().set(ext.getReportDir());
                    // sourceFiles lazily resolved so consumer can override ext.sourceDirs
                    task.getSourceFiles().setFrom(ext.getSourceDirs());
                });

        // Wire source dirs from the Java plugin's main source set when available.
        project.getPlugins().withId("java", ignored ->
            project.afterEvaluate(p -> {
                if (ext.getSourceDirs().isEmpty()) {
                    JavaPluginExtension java = p.getExtensions()
                            .findByType(JavaPluginExtension.class);
                    if (java != null) {
                        // Use the SourceDirectorySet (a FileTree) so iterating
                        // the collection yields individual .java files, not
                        // just directory entries.
                        ext.getSourceDirs().setFrom(
                                java.getSourceSets().getByName("main").getJava());
                    }
                }
                // sourceFiles on the task mirrors ext.sourceDirs
                taskProvider.configure(t -> t.getSourceFiles().setFrom(ext.getSourceDirs()));
            })
        );

        // Hook into check
        project.getTasks().named("check").configure(
                check -> check.dependsOn(taskProvider));

        // Run before jacocoTestCoverageVerification when present
        project.getTasks().configureEach(task -> {
            if ("jacocoTestCoverageVerification".equals(task.getName())) {
                task.dependsOn(taskProvider);
            }
        });
    }
}

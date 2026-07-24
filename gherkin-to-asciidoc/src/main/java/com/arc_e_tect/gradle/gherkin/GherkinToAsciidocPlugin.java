package com.arc_e_tect.gradle.gherkin;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * Gradle plugin that registers the {@code generateFeatureDocs} task and wires
 * the {@code gherkinToAsciidoc} DSL extension into the project.
 *
 * <h2>Usage</h2>
 * <pre>
 * plugins {
 *     id 'com.arc-e-tect.gherkin-to-asciidoc'
 * }
 *
 * gherkinToAsciidoc {
 *     sourceDirs.from(layout.projectDirectory.dir('src/test/resources/features'))
 * }
 * </pre>
 *
 * <h2>Defaults</h2>
 * <ul>
 *   <li>Source directory: {@code src/test/resources/features}</li>
 *   <li>Include sub-directories: {@code false}</li>
 *   <li>Output directory: {@code build/generated-docs}</li>
 *   <li>Output file name: {@code features.adoc}</li>
 * </ul>
 */
public class GherkinToAsciidocPlugin implements Plugin<Project> {

    /** Name of the Gradle task registered by this plugin. */
    public static final String TASK_NAME = "generateFeatureDocs";

    /** Creates a new plugin instance. Instantiated by Gradle infrastructure. */
    public GherkinToAsciidocPlugin() {}

    @Override
    public void apply(Project project) {
        GherkinToAsciidocExtension ext = project.getExtensions()
                .create(GherkinToAsciidocExtension.NAME, GherkinToAsciidocExtension.class);

        ext.getTrackProgress().convention(false);
        // Enabling trackProgress implies recursive scanning and grouping by feature,
        // unless includeSubDirs/groupByFeature have been set explicitly.
        ext.getIncludeSubDirs().convention(ext.getTrackProgress());
        ext.getGroupByFeature().convention(ext.getTrackProgress());
        ext.getOutputDir().convention(
                project.getLayout().getBuildDirectory().dir("generated-docs"));
        ext.getOutputFileName().convention(GherkinToAsciidocExtension.DEFAULT_OUTPUT_FILE_NAME);
        ext.getSnippetDir().convention(
                project.getLayout().getBuildDirectory().dir(GherkinToAsciidocExtension.DEFAULT_SNIPPET_DIR));

        project.getTasks().register(TASK_NAME, GenerateFeatureDocsTask.class, task -> {
            task.getSourceDirs().from(ext.getSourceDirs());
            task.getSourceFile().set(ext.getSourceFile());
            task.getIncludeSubDirs().set(ext.getIncludeSubDirs());
            task.getOutputDir().set(ext.getOutputDir());
            task.getOutputFileName().set(ext.getOutputFileName());
            task.getTrackProgress().set(ext.getTrackProgress());
            task.getGlueCodeDirs().from(ext.getGlueCodeDirs());
            task.getGroupByFeature().set(ext.getGroupByFeature());
            task.getSnippetDir().set(ext.getSnippetDir());
            task.getTemplate().set(ext.getTemplate());
            task.getProjectDirectory().set(project.getLayout().getProjectDirectory());
        });
    }
}

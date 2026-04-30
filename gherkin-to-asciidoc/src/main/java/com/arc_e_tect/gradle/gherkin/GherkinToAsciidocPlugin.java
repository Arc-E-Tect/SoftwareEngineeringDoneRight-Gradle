package com.arc_e_tect.gradle.gherkin;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class GherkinToAsciidocPlugin implements Plugin<Project> {

    public static final String TASK_NAME = "generateFeatureDocs";

    public GherkinToAsciidocPlugin() {}

    @Override
    public void apply(Project project) {
        GherkinToAsciidocExtension ext = project.getExtensions()
                .create(GherkinToAsciidocExtension.NAME, GherkinToAsciidocExtension.class);

        ext.getIncludeSubDirs().convention(false);
        ext.getOutputDir().convention(
                project.getLayout().getBuildDirectory().dir("generated-docs"));
        ext.getOutputFileName().convention(GherkinToAsciidocExtension.DEFAULT_OUTPUT_FILE_NAME);

        project.getTasks().register(TASK_NAME, GenerateFeatureDocsTask.class, task -> {
            task.getSourceDir().set(ext.getSourceDir());
            task.getSourceFile().set(ext.getSourceFile());
            task.getIncludeSubDirs().set(ext.getIncludeSubDirs());
            task.getOutputDir().set(ext.getOutputDir());
            task.getOutputFileName().set(ext.getOutputFileName());
            task.getProjectDirectory().set(project.getLayout().getProjectDirectory());
        });
    }
}

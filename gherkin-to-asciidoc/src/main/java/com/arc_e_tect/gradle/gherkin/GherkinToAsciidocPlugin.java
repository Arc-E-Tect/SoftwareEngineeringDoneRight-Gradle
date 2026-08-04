package com.arc_e_tect.gradle.gherkin;

import com.arc_e_tect.gradle.gherkin.indexing.IndexingMode;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.Provider;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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
 *   <li>Include sub-directories: {@code true}</li>
 *   <li>Group scenarios by feature: {@code true}</li>
 *   <li>Output directory: {@code build/generated-docs}</li>
 *   <li>Output file name: {@code features.adoc}</li>
 *   <li>Indexing: {@code off}</li>
 * </ul>
 *
 * <h2>Multi-project builds</h2>
 * <p>When applied to more than one project in the same build (typically via
 * {@code allprojects { apply plugin: ... } }), every project gets its own
 * {@code generateFeatureDocs} task and its own generated report. Every DSL property defaults to
 * the value configured on the root project's own {@code gherkinToAsciidoc} extension; a
 * sub-project that configures a property itself overrides that inherited default for that
 * property only - except for {@code outputDir} and {@code snippetDir} (see below).</p>
 * <p>{@code sourceDirs}, {@code sourceFile}, and {@code glueCodeDirs} cascade too, but not
 * verbatim: when a sub-project configures neither its own {@code sourceDirs} nor
 * {@code sourceFile}, any of the root project's directories/file that live inside the root
 * project's own directory are re-resolved against the sub-project's directory instead - so a
 * standard layout (e.g. every module keeping its features under
 * {@code src/test/resources/features}) only needs to be declared once, at the root, following
 * "convention over configuration". A root-configured path that points outside the root
 * project's own directory (e.g. a directory shared by every module) is used as-is, unchanged,
 * by every sub-project that inherits it.</p>
 * <p>{@code outputDir} and {@code snippetDir} are the one exception that never inherits from the
 * root project: each project always defaults to a location under its own build directory,
 * regardless of what the root project configures, so that every project's report lands in its
 * own build output rather than colliding with another project's. Set these explicitly on a
 * specific project to relocate that project's report.</p>
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

        GherkinToAsciidocExtension rootExt = rootExtension(project);

        if (rootExt != null) {
            ext.getTrackProgress().convention(rootExt.getTrackProgress());
            ext.getOutputFileName().convention(rootExt.getOutputFileName());
            ext.getTemplate().convention(rootExt.getTemplate());
            ext.getSystemUnderTestVersion().convention(rootExt.getSystemUnderTestVersion());
            ext.getIndexing().convention(rootExt.getIndexing());
        } else {
            ext.getTrackProgress().convention(false);
            ext.getOutputFileName().convention(GherkinToAsciidocExtension.DEFAULT_OUTPUT_FILE_NAME);
            ext.getSystemUnderTestVersion().convention(
                    project.provider(() -> String.valueOf(project.getVersion())));
            ext.getIndexing().convention(IndexingMode.OFF);
        }

        // outputDir/snippetDir intentionally always default to this project's own build directory,
        // never to the root project's, so that every project's report lands in its own build output
        // by default instead of colliding with another project's when neither configures them.
        ext.getOutputDir().convention(
                project.getLayout().getBuildDirectory().dir("generated-docs"));
        ext.getSnippetDir().convention(
                project.getLayout().getBuildDirectory().dir(GherkinToAsciidocExtension.DEFAULT_SNIPPET_DIR));

        // Enabling trackProgress implies recursive scanning and grouping by feature, unless
        // includeSubDirs/groupByFeature have been set explicitly - either directly on this
        // project, or (absent a local trackProgress override) inherited from the root project.
        // Both default to true when neither this project nor the root project configures them.
        Provider<Boolean> inheritedIncludeSubDirs =
                rootExt != null ? rootExt.getIncludeSubDirs() : project.provider(() -> true);
        Provider<Boolean> inheritedGroupByFeature =
                rootExt != null ? rootExt.getGroupByFeature() : project.provider(() -> true);
        ext.getIncludeSubDirs().convention(ext.getTrackProgress()
                .flatMap(trackProgress -> trackProgress ? project.provider(() -> true) : inheritedIncludeSubDirs));
        ext.getGroupByFeature().convention(ext.getTrackProgress()
                .flatMap(trackProgress -> trackProgress ? project.provider(() -> true) : inheritedGroupByFeature));

        Project rootProject = project.getRootProject();

        project.getTasks().register(TASK_NAME, GenerateFeatureDocsTask.class, task -> {
            wireSourceLocation(project, rootProject, ext, rootExt, task);
            task.getIncludeSubDirs().set(ext.getIncludeSubDirs());
            task.getOutputDir().set(ext.getOutputDir());
            task.getOutputFileName().set(ext.getOutputFileName());
            task.getTrackProgress().set(ext.getTrackProgress());
            wireGlueCodeDirs(project, rootProject, ext, rootExt, task);
            task.getGroupByFeature().set(ext.getGroupByFeature());
            task.getSnippetDir().set(ext.getSnippetDir());
            task.getTemplate().set(ext.getTemplate());
            task.getSystemUnderTestVersion().set(ext.getSystemUnderTestVersion());
            task.getIndexing().set(ext.getIndexing());
            task.getProjectDirectory().set(project.getLayout().getProjectDirectory());
        });
    }

    /**
     * Wires the task's {@code sourceDirs}/{@code sourceFile} from this project's own extension, or -
     * when neither is configured locally and a root extension exists - from the root project's own
     * {@code sourceDirs}/{@code sourceFile}, re-resolved against this project's directory.
     */
    private static void wireSourceLocation(
            Project project, Project rootProject, GherkinToAsciidocExtension ext,
            GherkinToAsciidocExtension rootExt, GenerateFeatureDocsTask task) {
        boolean configuredLocally = !ext.getSourceDirs().isEmpty() || ext.getSourceFile().isPresent();
        if (configuredLocally || rootExt == null) {
            task.getSourceDirs().from(ext.getSourceDirs());
            task.getSourceFile().set(ext.getSourceFile());
            return;
        }

        if (rootExt.getSourceFile().isPresent()) {
            task.getSourceFile().set(reapply(project, rootProject, rootExt.getSourceFile().get().getAsFile()));
        } else if (!rootExt.getSourceDirs().isEmpty()) {
            task.getSourceDirs().from(reapplyAll(project, rootProject, rootExt.getSourceDirs()));
        }
        // else: leave both empty; the task falls back to its own hardcoded default directory.
    }

    /**
     * Wires the task's {@code glueCodeDirs} from this project's own extension, plus - when empty
     * locally and a root extension configures its own - the root project's {@code glueCodeDirs},
     * re-resolved against this project's directory.
     */
    private static void wireGlueCodeDirs(
            Project project, Project rootProject, GherkinToAsciidocExtension ext,
            GherkinToAsciidocExtension rootExt, GenerateFeatureDocsTask task) {
        task.getGlueCodeDirs().from(ext.getGlueCodeDirs());
        if (rootExt != null && ext.getGlueCodeDirs().isEmpty() && !rootExt.getGlueCodeDirs().isEmpty()) {
            task.getGlueCodeDirs().from(reapplyAll(project, rootProject, rootExt.getGlueCodeDirs()));
        }
    }

    /**
     * Re-resolves {@code file} against {@code targetProject}'s directory when it lives inside
     * {@code sourceProject}'s own directory (the "standard layout" case); returns it unchanged
     * otherwise (a path deliberately shared across every project).
     */
    private static File reapply(Project targetProject, Project sourceProject, File file) {
        Path sourceProjectDir = sourceProject.getProjectDir().toPath();
        Path resolved = file.toPath();
        if (resolved.startsWith(sourceProjectDir)) {
            Path relative = sourceProjectDir.relativize(resolved);
            return targetProject.getProjectDir().toPath().resolve(relative).toFile();
        }
        return file;
    }

    private static List<File> reapplyAll(Project targetProject, Project sourceProject, FileCollection files) {
        List<File> result = new ArrayList<>();
        for (File file : files) {
            result.add(reapply(targetProject, sourceProject, file));
        }
        return result;
    }

    /**
     * Returns the {@code gherkinToAsciidoc} extension registered on {@code project}'s root project,
     * or {@code null} when {@code project} is itself the root project, or when the root project does
     * not have this plugin applied.
     *
     * @param project the project the plugin is currently being applied to
     * @return the root project's extension to inherit defaults from, or {@code null}
     */
    private static GherkinToAsciidocExtension rootExtension(Project project) {
        Project rootProject = project.getRootProject();
        if (rootProject.equals(project)) {
            return null;
        }
        return rootProject.getExtensions().findByType(GherkinToAsciidocExtension.class);
    }
}

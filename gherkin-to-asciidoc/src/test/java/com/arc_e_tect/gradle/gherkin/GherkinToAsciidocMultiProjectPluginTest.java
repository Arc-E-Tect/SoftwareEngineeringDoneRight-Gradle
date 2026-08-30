package com.arc_e_tect.gradle.gherkin;

import com.arc_e_tect.gradle.gherkin.indexing.IndexingMode;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GherkinToAsciidocPlugin (multi-project)")
class GherkinToAsciidocMultiProjectPluginTest {

    @TempDir
    Path tempDir;

    @TempDir
    Path outsideProjectsDir;

    @Test
    @DisplayName("registers a separate generateFeatureDocs task per project")
    void registersTaskPerProject() {
        Project root = rootProject();
        Project sub = subProject(root, "sub");

        assertThat(root.getTasks().findByName(GherkinToAsciidocPlugin.TASK_NAME)).isNotNull();
        assertThat(sub.getTasks().findByName(GherkinToAsciidocPlugin.TASK_NAME)).isNotNull();
    }

    @Test
    @DisplayName("sub-project without its own configuration inherits outputFileName from the root project")
    void subProjectInheritsOutputFileNameFromRoot() {
        Project root = rootProject();
        extension(root).getOutputFileName().set("root-features.adoc");
        Project sub = subProject(root, "sub");

        assertThat(extension(sub).getOutputFileName().get()).isEqualTo("root-features.adoc");
    }

    @Test
    @DisplayName("sub-project's own outputFileName takes precedence over the root project's")
    void subProjectOutputFileNameOverridesRoot() {
        Project root = rootProject();
        extension(root).getOutputFileName().set("root-features.adoc");
        Project sub = subProject(root, "sub");
        extension(sub).getOutputFileName().set("sub-features.adoc");

        assertThat(extension(sub).getOutputFileName().get()).isEqualTo("sub-features.adoc");
    }

    @Test
    @DisplayName("sub-project inherits trackProgress from the root project")
    void subProjectInheritsTrackProgressFromRoot() {
        Project root = rootProject();
        extension(root).getTrackProgress().set(true);
        Project sub = subProject(root, "sub");

        assertThat(extension(sub).getTrackProgress().get()).isTrue();
    }

    @Test
    @DisplayName("sub-project's own trackProgress = false overrides the root project's true")
    void subProjectTrackProgressOverridesRoot() {
        Project root = rootProject();
        extension(root).getTrackProgress().set(true);
        Project sub = subProject(root, "sub");
        extension(sub).getTrackProgress().set(false);

        assertThat(extension(sub).getTrackProgress().get()).isFalse();
    }

    @Test
    @DisplayName("sub-project's own trackProgress = true implies includeSubDirs/groupByFeature "
            + "even when the root project's trackProgress is false")
    void subProjectTrackProgressImpliesIncludeSubDirsAndGroupByFeature() {
        Project root = rootProject();
        Project sub = subProject(root, "sub");
        extension(sub).getTrackProgress().set(true);

        assertThat(extension(sub).getIncludeSubDirs().get()).isTrue();
        assertThat(extension(sub).getGroupByFeature().get()).isTrue();
    }

    @Test
    @DisplayName("sub-project inherits includeSubDirs from the root project when neither project's "
            + "trackProgress is enabled")
    void subProjectInheritsIncludeSubDirsFromRoot() {
        Project root = rootProject();
        extension(root).getIncludeSubDirs().set(true);
        Project sub = subProject(root, "sub");

        assertThat(extension(sub).getIncludeSubDirs().get()).isTrue();
    }

    @Test
    @DisplayName("sub-project inherits systemUnderTestVersion from the root project, not its own version")
    void subProjectInheritsSystemUnderTestVersionFromRoot() {
        Project root = rootProject();
        root.setVersion("9.9.9");
        extension(root).getSystemUnderTestVersion().set("v2.0.0");
        Project sub = subProject(root, "sub");
        sub.setVersion("0.0.1");

        assertThat(extension(sub).getSystemUnderTestVersion().get()).isEqualTo("v2.0.0");
    }

    @Test
    @DisplayName("sub-project inherits the root project's own version as systemUnderTestVersion "
            + "when neither project overrides it explicitly")
    void subProjectInheritsRootsOwnVersionWhenNeitherOverridesExplicitly() {
        Project root = rootProject();
        root.setVersion("9.9.9");
        Project sub = subProject(root, "sub");
        sub.setVersion("0.0.1");

        assertThat(extension(sub).getSystemUnderTestVersion().get()).isEqualTo("9.9.9");
    }

    @Test
    @DisplayName("sub-project's own outputDir always defaults to its own build directory, "
            + "even when the root project configures a custom outputDir")
    void subProjectOutputDirNeverInheritsFromRoot() {
        Project root = rootProject();
        File rootCustomDir = new File(tempDir.toFile(), "shared-docs");
        extension(root).getOutputDir().set(rootCustomDir);
        Project sub = subProject(root, "sub");

        File subOwnDefault = new File(sub.getLayout().getBuildDirectory().get().getAsFile(), "generated-docs");
        assertThat(extension(sub).getOutputDir().get().getAsFile())
                .isEqualTo(subOwnDefault)
                .isNotEqualTo(rootCustomDir);
    }

    @Test
    @DisplayName("root project without a gherkinToAsciidoc extension leaves sub-project defaults untouched")
    void rootWithoutExtensionLeavesSubProjectDefaultsUntouched() {
        Project root = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build();
        Project sub = subProject(root, "sub");

        assertThat(extension(sub).getTrackProgress().get()).isFalse();
        assertThat(extension(sub).getOutputFileName().get()).isEqualTo("features.adoc");
    }

    @Test
    @DisplayName("sub-project inherits the root project's sourceDirs, re-resolved against its own directory")
    void subProjectInheritsSourceDirsFromRootReResolvedToOwnDirectory() {
        Project root = rootProject();
        extension(root).getSourceDirs().from(new File(root.getProjectDir(), "src/test/resources/gherkin"));
        Project sub = subProject(root, "sub");

        File expected = new File(sub.getProjectDir(), "src/test/resources/gherkin");
        assertThat(task(sub).getSourceDirs().getFiles()).containsExactly(expected);
    }

    @Test
    @DisplayName("sub-project's own sourceDirs overrides the root project's cascaded sourceDirs entirely")
    void subProjectOwnSourceDirsOverridesRootCascade() {
        Project root = rootProject();
        extension(root).getSourceDirs().from(new File(root.getProjectDir(), "src/test/resources/gherkin"));
        Project sub = subProject(root, "sub");
        File ownDir = new File(sub.getProjectDir(), "src/test/resources/own-features");
        extension(sub).getSourceDirs().from(ownDir);

        assertThat(task(sub).getSourceDirs().getFiles()).containsExactly(ownDir);
    }

    @Test
    @DisplayName("sub-project's own sourceFile prevents the root project's sourceDirs from cascading")
    void subProjectOwnSourceFilePreventsSourceDirsCascade() {
        Project root = rootProject();
        extension(root).getSourceDirs().from(new File(root.getProjectDir(), "src/test/resources/gherkin"));
        Project sub = subProject(root, "sub");
        File ownFile = new File(sub.getProjectDir(), "src/test/resources/single.feature");
        extension(sub).getSourceFile().set(ownFile);

        assertThat(task(sub).getSourceFile().get().getAsFile()).isEqualTo(ownFile);
        assertThat(task(sub).getSourceDirs().isEmpty()).isTrue();
    }

    @Test
    @DisplayName("sub-project inherits the root project's sourceFile, re-resolved against its own directory")
    void subProjectInheritsSourceFileFromRootReResolvedToOwnDirectory() {
        Project root = rootProject();
        extension(root).getSourceFile().set(new File(root.getProjectDir(), "src/test/resources/single.feature"));
        Project sub = subProject(root, "sub");

        File expected = new File(sub.getProjectDir(), "src/test/resources/single.feature");
        assertThat(task(sub).getSourceFile().get().getAsFile()).isEqualTo(expected);
        assertThat(task(sub).getSourceDirs().isEmpty()).isTrue();
    }

    @Test
    @DisplayName("a root sourceDirs entry outside the root project's own directory is shared as-is, unchanged")
    void rootSourceDirOutsideRootProjectDirectoryIsSharedAsIs() {
        Project root = rootProject();
        // outsideProjectsDir is a separate @TempDir, guaranteed not to be nested inside root's own
        // project directory (tempDir) - unlike a subdirectory of tempDir would be.
        File sharedDir = new File(outsideProjectsDir.toFile(), "shared-features-outside-any-project");
        extension(root).getSourceDirs().from(sharedDir);
        Project sub = subProject(root, "sub");

        assertThat(task(sub).getSourceDirs().getFiles()).containsExactly(sharedDir);
    }

    @Test
    @DisplayName("sub-project inherits the root project's glueCodeDirs, re-resolved against its own directory")
    void subProjectInheritsGlueCodeDirsFromRootReResolvedToOwnDirectory() {
        Project root = rootProject();
        extension(root).getGlueCodeDirs().from(new File(root.getProjectDir(), "src/test/java/steps"));
        Project sub = subProject(root, "sub");

        File expected = new File(sub.getProjectDir(), "src/test/java/steps");
        assertThat(task(sub).getGlueCodeDirs().getFiles()).containsExactly(expected);
    }

    @Test
    @DisplayName("sub-project's own glueCodeDirs overrides the root project's cascaded glueCodeDirs entirely")
    void subProjectOwnGlueCodeDirsOverridesRootCascade() {
        Project root = rootProject();
        extension(root).getGlueCodeDirs().from(new File(root.getProjectDir(), "src/test/java/steps"));
        Project sub = subProject(root, "sub");
        File ownDir = new File(sub.getProjectDir(), "src/test/java/own-steps");
        extension(sub).getGlueCodeDirs().from(ownDir);

        assertThat(task(sub).getGlueCodeDirs().getFiles()).containsExactly(ownDir);
    }

    @Test
    @DisplayName("sub-project without its own configuration inherits indexing from the root project")
    void subProjectInheritsIndexingFromRoot() {
        Project root = rootProject();
        extension(root).getIndexing().set(IndexingMode.ALL);
        Project sub = subProject(root, "sub");

        assertThat(extension(sub).getIndexing().get()).isEqualTo(IndexingMode.ALL);
    }

    @Test
    @DisplayName("sub-project's own indexing takes precedence over the root project's")
    void subProjectIndexingOverridesRoot() {
        Project root = rootProject();
        extension(root).getIndexing().set(IndexingMode.ALL);
        Project sub = subProject(root, "sub");
        extension(sub).getIndexing().set(IndexingMode.OFF);

        assertThat(extension(sub).getIndexing().get()).isEqualTo(IndexingMode.OFF);
    }

    @Test
    @DisplayName("sub-project without its own configuration inherits forceRewrite from the root project")
    void subProjectInheritsForceRewriteFromRoot() {
        Project root = rootProject();
        extension(root).getForceRewrite().set(true);
        Project sub = subProject(root, "sub");

        assertThat(extension(sub).getForceRewrite().get()).isTrue();
    }

    @Test
    @DisplayName("sub-project's own forceRewrite takes precedence over the root project's")
    void subProjectForceRewriteOverridesRoot() {
        Project root = rootProject();
        extension(root).getForceRewrite().set(true);
        Project sub = subProject(root, "sub");
        extension(sub).getForceRewrite().set(false);

        assertThat(extension(sub).getForceRewrite().get()).isFalse();
    }

    @Test
    @DisplayName("sub-project without its own configuration inherits consolidatedIndex from the root project")
    void subProjectInheritsConsolidatedIndexFromRoot() {
        Project root = rootProject();
        extension(root).getConsolidatedIndex().set(true);
        Project sub = subProject(root, "sub");

        assertThat(extension(sub).getConsolidatedIndex().get()).isTrue();
    }

    @Test
    @DisplayName("sub-project's own consolidatedIndex takes precedence over the root project's")
    void subProjectConsolidatedIndexOverridesRoot() {
        Project root = rootProject();
        extension(root).getConsolidatedIndex().set(true);
        Project sub = subProject(root, "sub");
        extension(sub).getConsolidatedIndex().set(false);

        assertThat(extension(sub).getConsolidatedIndex().get()).isFalse();
    }

    @Test
    @DisplayName("consolidatedIndex defaults to false at the root project")
    void consolidatedIndexDefaultsToFalse() {
        Project root = rootProject();

        assertThat(extension(root).getConsolidatedIndex().get()).isFalse();
    }

    @Test
    @DisplayName("every project's own task is given every project's own directory, root and every "
            + "sub-project alike, so it can scope indexing to whichever one each feature file lives under")
    void taskProjectDirectoriesIncludesEveryProjectInTheBuild() {
        Project root = rootProject();
        Project sub = subProject(root, "sub");

        assertThat(task(root).getProjectDirectories().get())
                .containsExactlyInAnyOrder(root.getProjectDir(), sub.getProjectDir());
        assertThat(task(sub).getProjectDirectories().get())
                .containsExactlyInAnyOrder(root.getProjectDir(), sub.getProjectDir());
    }

    @Test
    @DisplayName("sub-project without its own configuration inherits trackProgressHistory from the root project")
    void subProjectInheritsTrackProgressHistoryFromRoot() {
        Project root = rootProject();
        extension(root).getTrackProgressHistory().set(true);
        Project sub = subProject(root, "sub");

        assertThat(extension(sub).getTrackProgressHistory().get()).isTrue();
    }

    @Test
    @DisplayName("sub-project's own trackProgressHistory takes precedence over the root project's")
    void subProjectTrackProgressHistoryOverridesRoot() {
        Project root = rootProject();
        extension(root).getTrackProgressHistory().set(true);
        Project sub = subProject(root, "sub");
        extension(sub).getTrackProgressHistory().set(false);

        assertThat(extension(sub).getTrackProgressHistory().get()).isFalse();
    }

    @Test
    @DisplayName("sub-project's own progressHistoryFile always defaults to its own project directory, "
            + "even when the root project configures a custom progressHistoryFile")
    void subProjectProgressHistoryFileNeverInheritsFromRoot() {
        Project root = rootProject();
        File rootCustomFile = new File(tempDir.toFile(), "shared-history.ndjson");
        extension(root).getProgressHistoryFile().set(rootCustomFile);
        Project sub = subProject(root, "sub");

        File subOwnDefault = new File(sub.getProjectDir(), "gherkin-progress-history.ndjson");
        assertThat(extension(sub).getProgressHistoryFile().get().getAsFile())
                .isEqualTo(subOwnDefault)
                .isNotEqualTo(rootCustomFile);
    }

    @Test
    @DisplayName("sub-project's own updateProgressHistory follows its own trackProgressHistory, "
            + "not the root project's")
    void subProjectUpdateProgressHistoryFollowsItsOwnTrackProgressHistory() {
        Project root = rootProject();
        extension(root).getTrackProgressHistory().set(true);
        Project sub = subProject(root, "sub");
        extension(sub).getTrackProgressHistory().set(false);

        assertThat(extension(sub).getUpdateProgressHistory().get()).isFalse();
    }

    // --- helpers ---

    private Project rootProject() {
        Project root = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build();
        root.getPluginManager().apply("com.arc-e-tect.gherkin-to-asciidoc");
        return root;
    }

    private Project subProject(Project root, String name) {
        File subDir = new File(tempDir.toFile(), name);
        subDir.mkdirs();
        Project sub = ProjectBuilder.builder()
                .withParent(root)
                .withProjectDir(subDir)
                .withName(name)
                .build();
        sub.getPluginManager().apply("com.arc-e-tect.gherkin-to-asciidoc");
        return sub;
    }

    private GherkinToAsciidocExtension extension(Project project) {
        return project.getExtensions().getByType(GherkinToAsciidocExtension.class);
    }

    private GenerateFeatureDocsTask task(Project project) {
        return project.getTasks()
                .named(GherkinToAsciidocPlugin.TASK_NAME, GenerateFeatureDocsTask.class)
                .get();
    }
}

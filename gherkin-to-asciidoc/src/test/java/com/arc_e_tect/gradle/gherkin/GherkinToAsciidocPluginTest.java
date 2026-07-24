package com.arc_e_tect.gradle.gherkin;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("GherkinToAsciidocPlugin")
class GherkinToAsciidocPluginTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("registers the generateFeatureDocs task when applied")
    void registersGenerateFeatureDocsTask() {
        Project project = projectWithPlugin();

        assertThat(project.getTasks().findByName(GherkinToAsciidocPlugin.TASK_NAME)).isNotNull();
    }

    @Test
    @DisplayName("extension default: includeSubDirs is false")
    void extensionDefaultIncludeSubDirsIsFalse() {
        Project project = projectWithPlugin();
        GherkinToAsciidocExtension ext = extension(project);

        assertThat(ext.getIncludeSubDirs().get()).isFalse();
    }

    @Test
    @DisplayName("extension default: trackProgress is false")
    void extensionDefaultTrackProgressIsFalse() {
        Project project = projectWithPlugin();
        GherkinToAsciidocExtension ext = extension(project);

        assertThat(ext.getTrackProgress().get()).isFalse();
    }

    @Test
    @DisplayName("extension: includeSubDirs defaults to true when trackProgress is enabled")
    void includeSubDirsDefaultsToTrueWhenTrackProgressEnabled() {
        Project project = projectWithPlugin();
        GherkinToAsciidocExtension ext = extension(project);

        ext.getTrackProgress().set(true);

        assertThat(ext.getIncludeSubDirs().get()).isTrue();
    }

    @Test
    @DisplayName("extension default: outputFileName is features.adoc")
    void extensionDefaultOutputFileNameIsFeaturesAdoc() {
        Project project = projectWithPlugin();
        GherkinToAsciidocExtension ext = extension(project);

        assertThat(ext.getOutputFileName().get()).isEqualTo("features.adoc");
    }

    @Test
    @DisplayName("extension default: outputDir is build/generated-docs")
    void extensionDefaultOutputDirIsGeneratedDocs() {
        Project project = projectWithPlugin();
        GherkinToAsciidocExtension ext = extension(project);

        assertThat(ext.getOutputDir().get().getAsFile().getPath())
                .endsWith("build" + File.separator + "generated-docs");
    }

    @Test
    @DisplayName("generates features.adoc from a flat source directory")
    void generatesAsciidocFromFlatDirectory() throws IOException {
        Project project = projectWithPlugin();
        File featuresDir = new File(tempDir.toFile(), "features");
        featuresDir.mkdirs();
        writeFeatureFile(featuresDir, "signup.feature",
                "Feature: Sign Up\n\n  Scenario: New user signs up\n    Given the sign-up page\n");

        File outputDir = new File(tempDir.toFile(), "output");

        GenerateFeatureDocsTask task = task(project);
        task.getSourceDir().set(featuresDir);
        task.getOutputDir().set(outputDir);
        task.getProjectDirectory().set(project.getLayout().getProjectDirectory());
        task.generate();

        File outputFile = new File(outputDir, "features.adoc");
        assertThat(outputFile).exists();
        List<String> lines = Files.readAllLines(outputFile.toPath(), StandardCharsets.UTF_8);
        assertThat(lines).contains("* Scenario: New user signs up");
    }

    @Test
    @DisplayName("generates features.adoc from a single feature file")
    void generatesAsciidocFromSingleFile() throws IOException {
        Project project = projectWithPlugin();
        File singleFile = tempDir.resolve("single.feature").toFile();
        Files.writeString(singleFile.toPath(),
                "Feature: Single\n\n  Scenario: Only scenario\n    Given something\n");

        File outputDir = new File(tempDir.toFile(), "output");

        GenerateFeatureDocsTask task = task(project);
        task.getSourceFile().set(singleFile);
        task.getOutputDir().set(outputDir);
        task.getProjectDirectory().set(project.getLayout().getProjectDirectory());
        task.generate();

        File outputFile = new File(outputDir, "features.adoc");
        assertThat(outputFile).exists();
        assertThat(Files.readString(outputFile.toPath())).contains("* Scenario: Only scenario");
    }

    @Test
    @DisplayName("generates features.adoc including subdirectories when includeSubDirs is true")
    void generatesAsciidocFromRecursiveDirectory() throws IOException {
        Project project = projectWithPlugin();
        File rootDir = new File(tempDir.toFile(), "features");
        File subDir = new File(rootDir, "sub");
        subDir.mkdirs();
        writeFeatureFile(rootDir, "root.feature",
                "Feature: Root\n\n  Scenario: Root scenario\n    Given root\n");
        writeFeatureFile(subDir, "sub.feature",
                "Feature: Sub\n\n  Scenario: Sub scenario\n    Given sub\n");

        File outputDir = new File(tempDir.toFile(), "output");

        GenerateFeatureDocsTask task = task(project);
        task.getSourceDir().set(rootDir);
        task.getIncludeSubDirs().set(true);
        task.getOutputDir().set(outputDir);
        task.getProjectDirectory().set(project.getLayout().getProjectDirectory());
        task.generate();

        String content = Files.readString(new File(outputDir, "features.adoc").toPath());
        assertThat(content)
                .contains("* Scenario: Root scenario")
                .contains("* Scenario: Sub scenario");
    }

    @Test
    @DisplayName("throws GradleException when both sourceDir and sourceFile are configured")
    void throwsWhenBothSourceDirAndSourceFileAreSet() throws IOException {
        Project project = projectWithPlugin();
        File dir = tempDir.toFile();
        File file = tempDir.resolve("single.feature").toFile();
        Files.writeString(file.toPath(), "Feature: F\n  Scenario: S\n    Given g\n");

        GenerateFeatureDocsTask task = task(project);
        task.getSourceDir().set(dir);
        task.getSourceFile().set(file);
        task.getOutputDir().set(new File(tempDir.toFile(), "output"));
        task.getProjectDirectory().set(project.getLayout().getProjectDirectory());

        assertThatThrownBy(task::generate)
                .isInstanceOf(org.gradle.api.GradleException.class)
                .hasMessageContaining("sourceDir and sourceFile are mutually exclusive");
    }

    @Test
    @DisplayName("throws GradleException when sourceFile and includeSubDirs are both configured")
    void throwsWhenSourceFileAndIncludeSubDirsAreSet() throws IOException {
        Project project = projectWithPlugin();
        File file = tempDir.resolve("single.feature").toFile();
        Files.writeString(file.toPath(), "Feature: F\n  Scenario: S\n    Given g\n");

        GenerateFeatureDocsTask task = task(project);
        task.getSourceFile().set(file);
        task.getIncludeSubDirs().set(true);
        task.getOutputDir().set(new File(tempDir.toFile(), "output"));
        task.getProjectDirectory().set(project.getLayout().getProjectDirectory());

        assertThatThrownBy(task::generate)
                .isInstanceOf(org.gradle.api.GradleException.class)
                .hasMessageContaining("includeSubDirs cannot be used when sourceFile is configured");
    }

    @Test
    @DisplayName("throws GradleException when trackProgress is enabled without sourceDir")
    void throwsWhenTrackProgressEnabledWithoutSourceDir() throws IOException {
        Project project = projectWithPlugin();
        File file = tempDir.resolve("single.feature").toFile();
        Files.writeString(file.toPath(), "Feature: F\n  Scenario: S\n    Given g\n");
        File glueCodeDir = new File(tempDir.toFile(), "steps");
        glueCodeDir.mkdirs();

        GenerateFeatureDocsTask task = task(project);
        task.getSourceFile().set(file);
        task.getTrackProgress().set(true);
        task.getGlueCodeDir().set(glueCodeDir);
        task.getOutputDir().set(new File(tempDir.toFile(), "output"));
        task.getProjectDirectory().set(project.getLayout().getProjectDirectory());

        assertThatThrownBy(task::generate)
                .isInstanceOf(org.gradle.api.GradleException.class)
                .hasMessageContaining("trackProgress can only be enabled when sourceDir is configured");
    }

    @Test
    @DisplayName("throws GradleException when trackProgress is enabled without glueCodeDir")
    void throwsWhenTrackProgressEnabledWithoutGlueCodeDir() throws IOException {
        Project project = projectWithPlugin();
        File featuresDir = new File(tempDir.toFile(), "features");
        featuresDir.mkdirs();
        writeFeatureFile(featuresDir, "sample.feature", "Feature: F\n  Scenario: S\n    Given g\n");

        GenerateFeatureDocsTask task = task(project);
        task.getSourceDir().set(featuresDir);
        task.getTrackProgress().set(true);
        task.getOutputDir().set(new File(tempDir.toFile(), "output"));
        task.getProjectDirectory().set(project.getLayout().getProjectDirectory());

        assertThatThrownBy(task::generate)
                .isInstanceOf(org.gradle.api.GradleException.class)
                .hasMessageContaining("trackProgress requires glueCodeDir to be configured");
    }

    @Test
    @DisplayName("trackProgress implies recursive scanning even if includeSubDirs is explicitly false")
    void trackProgressImpliesRecursiveScanningEvenWhenIncludeSubDirsExplicitlyFalse() throws IOException {
        Project project = projectWithPlugin();
        File rootDir = new File(tempDir.toFile(), "features");
        File subDir = new File(rootDir, "sub");
        subDir.mkdirs();
        writeFeatureFile(rootDir, "root.feature", "Feature: Root\n\n  Scenario: Root scenario\n    Given root\n");
        writeFeatureFile(subDir, "sub.feature", "Feature: Sub\n\n  Scenario: Sub scenario\n    Given sub\n");
        File glueCodeDir = new File(tempDir.toFile(), "steps");
        glueCodeDir.mkdirs();

        GenerateFeatureDocsTask task = task(project);
        task.getSourceDir().set(rootDir);
        task.getIncludeSubDirs().set(false);
        task.getTrackProgress().set(true);
        task.getGlueCodeDir().set(glueCodeDir);
        File outputDir = new File(tempDir.toFile(), "output");
        task.getOutputDir().set(outputDir);
        task.getProjectDirectory().set(project.getLayout().getProjectDirectory());
        task.generate();

        String content = Files.readString(new File(outputDir, "features.adoc").toPath());
        assertThat(content)
                .contains("Root scenario")
                .contains("Sub scenario");
    }

    @Test
    @DisplayName("generates a progress report classifying scenarios as listed, defined, and implemented")
    void generatesProgressReport() throws IOException {
        Project project = projectWithPlugin();
        File featuresDir = new File(tempDir.toFile(), "features");
        featuresDir.mkdirs();
        writeFeatureFile(featuresDir, "sample.feature", """
                Feature: Sample

                  Scenario: Only a title

                  Scenario: Has steps but no glue
                    Given an unimplemented step

                  Scenario: Fully wired up
                    Given an implemented step
                """);

        File glueCodeDir = new File(tempDir.toFile(), "steps");
        glueCodeDir.mkdirs();
        Files.writeString(new File(glueCodeDir, "Steps.java").toPath(), """
                public class Steps {
                    @Given("an implemented step")
                    public void implemented() {}
                }
                """);

        File outputDir = new File(tempDir.toFile(), "output");

        GenerateFeatureDocsTask task = task(project);
        task.getSourceDir().set(featuresDir);
        task.getTrackProgress().set(true);
        task.getGlueCodeDir().set(glueCodeDir);
        task.getOutputDir().set(outputDir);
        task.getProjectDirectory().set(project.getLayout().getProjectDirectory());
        task.generate();

        String content = Files.readString(new File(outputDir, "features.adoc").toPath());
        assertThat(content)
                .contains("Progress Summary")
                .contains("| Listed", "| 1", "| 33.3%")
                .contains("| Defined", "| 1")
                .contains("| Implemented", "| 1")
                .contains("== Listed", "* Scenario: Only a title")
                .contains("== Defined", "* Scenario: Has steps but no glue")
                .contains("== Implemented", "* Scenario: Fully wired up");
    }

    @Test
    @DisplayName("output file starts with = Feature Scenarios header")
    void outputFileContainsHeader() throws IOException {
        Project project = projectWithPlugin();
        File featuresDir = new File(tempDir.toFile(), "features");
        featuresDir.mkdirs();
        writeFeatureFile(featuresDir, "sample.feature",
                "Feature: Sample\n\n  Scenario: A scenario\n    Given something\n");

        File outputDir = new File(tempDir.toFile(), "output");

        GenerateFeatureDocsTask task = task(project);
        task.getSourceDir().set(featuresDir);
        task.getOutputDir().set(outputDir);
        task.getProjectDirectory().set(project.getLayout().getProjectDirectory());
        task.generate();

        String content = Files.readString(new File(outputDir, "features.adoc").toPath());
        assertThat(content).startsWith("= Feature Scenarios");
    }

    // --- helpers ---

    private Project projectWithPlugin() {
        Project project = ProjectBuilder.builder()
                .withProjectDir(tempDir.toFile())
                .build();
        project.getPluginManager().apply("com.arc-e-tect.gherkin-to-asciidoc");
        return project;
    }

    private GherkinToAsciidocExtension extension(Project project) {
        return project.getExtensions().getByType(GherkinToAsciidocExtension.class);
    }

    private GenerateFeatureDocsTask task(Project project) {
        return project.getTasks()
                .named(GherkinToAsciidocPlugin.TASK_NAME, GenerateFeatureDocsTask.class)
                .get();
    }

    private void writeFeatureFile(File dir, String name, String content) throws IOException {
        Files.writeString(new File(dir, name).toPath(), content, StandardCharsets.UTF_8);
    }
}

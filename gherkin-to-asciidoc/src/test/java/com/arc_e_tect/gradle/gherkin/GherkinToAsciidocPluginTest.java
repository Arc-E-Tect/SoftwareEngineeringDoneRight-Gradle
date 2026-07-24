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
    @DisplayName("extension default: sourceDirs is empty")
    void extensionDefaultSourceDirsIsEmpty() {
        Project project = projectWithPlugin();
        GherkinToAsciidocExtension ext = extension(project);

        assertThat(ext.getSourceDirs().isEmpty()).isTrue();
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
    @DisplayName("extension default: groupByFeature is false")
    void extensionDefaultGroupByFeatureIsFalse() {
        Project project = projectWithPlugin();
        GherkinToAsciidocExtension ext = extension(project);

        assertThat(ext.getGroupByFeature().get()).isFalse();
    }

    @Test
    @DisplayName("extension: groupByFeature defaults to true when trackProgress is enabled")
    void groupByFeatureDefaultsToTrueWhenTrackProgressEnabled() {
        Project project = projectWithPlugin();
        GherkinToAsciidocExtension ext = extension(project);

        ext.getTrackProgress().set(true);

        assertThat(ext.getGroupByFeature().get()).isTrue();
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
        task.getSourceDirs().from(featuresDir);
        task.getOutputDir().set(outputDir);
        task.getProjectDirectory().set(project.getLayout().getProjectDirectory());
        task.generate();

        File outputFile = new File(outputDir, "features.adoc");
        assertThat(outputFile).exists();
        List<String> lines = Files.readAllLines(outputFile.toPath(), StandardCharsets.UTF_8);
        assertThat(lines).contains("* Scenario: New user signs up");
    }

    @Test
    @DisplayName("generates features.adoc from multiple source directories")
    void generatesAsciidocFromMultipleSourceDirectories() throws IOException {
        Project project = projectWithPlugin();
        File firstDir = new File(tempDir.toFile(), "features-a");
        File secondDir = new File(tempDir.toFile(), "features-b");
        firstDir.mkdirs();
        secondDir.mkdirs();
        writeFeatureFile(firstDir, "signup.feature",
                "Feature: Sign Up\n\n  Scenario: New user signs up\n    Given the sign-up page\n");
        writeFeatureFile(secondDir, "login.feature",
                "Feature: Login\n\n  Scenario: User logs in\n    Given the login page\n");

        File outputDir = new File(tempDir.toFile(), "output");

        GenerateFeatureDocsTask task = task(project);
        task.getSourceDirs().from(firstDir, secondDir);
        task.getOutputDir().set(outputDir);
        task.getProjectDirectory().set(project.getLayout().getProjectDirectory());
        task.generate();

        String content = Files.readString(new File(outputDir, "features.adoc").toPath());
        assertThat(content)
                .contains("* Scenario: New user signs up")
                .contains("* Scenario: User logs in")
                .doesNotContain("== Sign Up")
                .doesNotContain("== Login");
    }

    @Test
    @DisplayName("groups scenarios by feature when groupByFeature is enabled")
    void generatesGroupedAsciidocWhenGroupByFeatureEnabled() throws IOException {
        Project project = projectWithPlugin();
        File firstDir = new File(tempDir.toFile(), "features-a");
        File secondDir = new File(tempDir.toFile(), "features-b");
        firstDir.mkdirs();
        secondDir.mkdirs();
        writeFeatureFile(firstDir, "signup.feature",
                "Feature: Sign Up\n\n  Scenario: New user signs up\n    Given the sign-up page\n");
        writeFeatureFile(secondDir, "login.feature",
                "Feature: Login\n\n  Scenario: User logs in\n    Given the login page\n");

        File outputDir = new File(tempDir.toFile(), "output");

        GenerateFeatureDocsTask task = task(project);
        task.getSourceDirs().from(firstDir, secondDir);
        task.getGroupByFeature().set(true);
        task.getOutputDir().set(outputDir);
        task.getProjectDirectory().set(project.getLayout().getProjectDirectory());
        task.generate();

        String content = Files.readString(new File(outputDir, "features.adoc").toPath());
        assertThat(content)
                .contains("== Sign Up", "* Scenario: New user signs up")
                .contains("== Login", "* Scenario: User logs in");
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
        task.getSourceDirs().from(rootDir);
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
    @DisplayName("throws GradleException when both sourceDirs and sourceFile are configured")
    void throwsWhenBothSourceDirsAndSourceFileAreSet() throws IOException {
        Project project = projectWithPlugin();
        File dir = tempDir.toFile();
        File file = tempDir.resolve("single.feature").toFile();
        Files.writeString(file.toPath(), "Feature: F\n  Scenario: S\n    Given g\n");

        GenerateFeatureDocsTask task = task(project);
        task.getSourceDirs().from(dir);
        task.getSourceFile().set(file);
        task.getOutputDir().set(new File(tempDir.toFile(), "output"));
        task.getProjectDirectory().set(project.getLayout().getProjectDirectory());

        assertThatThrownBy(task::generate)
                .isInstanceOf(org.gradle.api.GradleException.class)
                .hasMessageContaining("sourceDirs and sourceFile are mutually exclusive");
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
    @DisplayName("throws GradleException when trackProgress is enabled without sourceDirs")
    void throwsWhenTrackProgressEnabledWithoutSourceDirs() throws IOException {
        Project project = projectWithPlugin();
        File file = tempDir.resolve("single.feature").toFile();
        Files.writeString(file.toPath(), "Feature: F\n  Scenario: S\n    Given g\n");
        File glueCodeDir = new File(tempDir.toFile(), "steps");
        glueCodeDir.mkdirs();

        GenerateFeatureDocsTask task = task(project);
        task.getSourceFile().set(file);
        task.getTrackProgress().set(true);
        task.getGlueCodeDirs().from(glueCodeDir);
        task.getOutputDir().set(new File(tempDir.toFile(), "output"));
        task.getProjectDirectory().set(project.getLayout().getProjectDirectory());

        assertThatThrownBy(task::generate)
                .isInstanceOf(org.gradle.api.GradleException.class)
                .hasMessageContaining("trackProgress can only be enabled when sourceDirs is configured");
    }

    @Test
    @DisplayName("throws GradleException when trackProgress is enabled without glueCodeDirs")
    void throwsWhenTrackProgressEnabledWithoutGlueCodeDirs() throws IOException {
        Project project = projectWithPlugin();
        File featuresDir = new File(tempDir.toFile(), "features");
        featuresDir.mkdirs();
        writeFeatureFile(featuresDir, "sample.feature", "Feature: F\n  Scenario: S\n    Given g\n");

        GenerateFeatureDocsTask task = task(project);
        task.getSourceDirs().from(featuresDir);
        task.getTrackProgress().set(true);
        task.getOutputDir().set(new File(tempDir.toFile(), "output"));
        task.getProjectDirectory().set(project.getLayout().getProjectDirectory());

        assertThatThrownBy(task::generate)
                .isInstanceOf(org.gradle.api.GradleException.class)
                .hasMessageContaining("trackProgress requires glueCodeDirs to be configured");
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
        task.getSourceDirs().from(rootDir);
        task.getIncludeSubDirs().set(false);
        task.getTrackProgress().set(true);
        task.getGlueCodeDirs().from(glueCodeDir);
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
        task.getSourceDirs().from(featuresDir);
        task.getTrackProgress().set(true);
        task.getGlueCodeDirs().from(glueCodeDir);
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
    @DisplayName("aggregates feature files and glue code from multiple directories each")
    void aggregatesFeaturesAndGlueCodeFromMultipleDirectories() throws IOException {
        Project project = projectWithPlugin();
        File authFeatures = new File(tempDir.toFile(), "features-auth");
        File billingFeatures = new File(tempDir.toFile(), "features-billing");
        authFeatures.mkdirs();
        billingFeatures.mkdirs();
        writeFeatureFile(authFeatures, "login.feature",
                "Feature: Login\n\n  Scenario: User logs in\n    Given the login page is open\n");
        writeFeatureFile(billingFeatures, "invoice.feature",
                "Feature: Invoice\n\n  Scenario: User pays an invoice\n    Given an outstanding invoice\n");

        File authSteps = new File(tempDir.toFile(), "steps-auth");
        File billingSteps = new File(tempDir.toFile(), "steps-billing");
        authSteps.mkdirs();
        billingSteps.mkdirs();
        Files.writeString(new File(authSteps, "LoginSteps.java").toPath(), """
                public class LoginSteps {
                    @Given("the login page is open")
                    public void loginPageOpen() {}
                }
                """);
        Files.writeString(new File(billingSteps, "InvoiceSteps.java").toPath(), """
                public class InvoiceSteps {
                    @Given("an outstanding invoice")
                    public void outstandingInvoice() {}
                }
                """);

        File outputDir = new File(tempDir.toFile(), "output");

        GenerateFeatureDocsTask task = task(project);
        task.getSourceDirs().from(authFeatures, billingFeatures);
        task.getTrackProgress().set(true);
        task.getGlueCodeDirs().from(authSteps, billingSteps);
        task.getOutputDir().set(outputDir);
        task.getProjectDirectory().set(project.getLayout().getProjectDirectory());
        task.generate();

        String content = Files.readString(new File(outputDir, "features.adoc").toPath());
        assertThat(content)
                .contains("== Implemented", "* Scenario: User logs in", "* Scenario: User pays an invoice")
                .contains("== Defined" + System.lineSeparator() + System.lineSeparator()
                        + "Scenarios with steps written, but at least one step has no matching glue code yet."
                        + System.lineSeparator() + System.lineSeparator() + "_None._");
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
        task.getSourceDirs().from(featuresDir);
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

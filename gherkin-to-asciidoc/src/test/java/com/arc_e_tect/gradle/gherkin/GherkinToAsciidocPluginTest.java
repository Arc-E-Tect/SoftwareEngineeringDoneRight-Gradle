package com.arc_e_tect.gradle.gherkin;

import com.arc_e_tect.gradle.gherkin.console.RecordingLogger;
import com.arc_e_tect.gradle.gherkin.indexing.IndexingMode;
import com.arc_e_tect.gradle.gherkin.progress.ProgressHistoryStore;
import com.arc_e_tect.gradle.gherkin.progress.ScenarioFingerprint;
import com.arc_e_tect.gradle.gherkin.progress.ScenarioProgressRecord;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
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
    @DisplayName("extension default: includeSubDirs is true")
    void extensionDefaultIncludeSubDirsIsTrue() {
        Project project = projectWithPlugin();
        GherkinToAsciidocExtension ext = extension(project);

        assertThat(ext.getIncludeSubDirs().get()).isTrue();
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
    @DisplayName("extension default: groupByFeature is true")
    void extensionDefaultGroupByFeatureIsTrue() {
        Project project = projectWithPlugin();
        GherkinToAsciidocExtension ext = extension(project);

        assertThat(ext.getGroupByFeature().get()).isTrue();
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
    @DisplayName("extension default: snippetDir is build/generated-docs/features/snippets")
    void extensionDefaultSnippetDirIsGeneratedDocsFeaturesSnippets() {
        Project project = projectWithPlugin();
        GherkinToAsciidocExtension ext = extension(project);

        assertThat(ext.getSnippetDir().get().getAsFile().getPath())
                .endsWith(String.join(File.separator, "build", "generated-docs", "features", "snippets"));
    }

    @Test
    @DisplayName("extension default: template is not set")
    void extensionDefaultTemplateIsNotSet() {
        Project project = projectWithPlugin();
        GherkinToAsciidocExtension ext = extension(project);

        assertThat(ext.getTemplate().isPresent()).isFalse();
    }

    @Test
    @DisplayName("extension default: systemUnderTestVersion is the project's version")
    void extensionDefaultSystemUnderTestVersionIsProjectVersion() {
        Project project = projectWithPlugin();
        project.setVersion("2.5.0");
        GherkinToAsciidocExtension ext = extension(project);

        assertThat(ext.getSystemUnderTestVersion().get()).isEqualTo("2.5.0");
    }

    @Test
    @DisplayName("extension: systemUnderTestVersion can be overridden")
    void extensionSystemUnderTestVersionCanBeOverridden() {
        Project project = projectWithPlugin();
        project.setVersion("2.5.0");
        GherkinToAsciidocExtension ext = extension(project);

        ext.getSystemUnderTestVersion().set("v1.0.0");

        assertThat(ext.getSystemUnderTestVersion().get()).isEqualTo("v1.0.0");
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
    @DisplayName("extension default: indexing is off")
    void extensionDefaultIndexingIsOff() {
        Project project = projectWithPlugin();
        GherkinToAsciidocExtension ext = extension(project);

        assertThat(ext.getIndexing().get()).isEqualTo(IndexingMode.OFF);
    }

    @Test
    @DisplayName("extension default: forceRewrite is false")
    void extensionDefaultForceRewriteIsFalse() {
        Project project = projectWithPlugin();
        GherkinToAsciidocExtension ext = extension(project);

        assertThat(ext.getForceRewrite().get()).isFalse();
    }

    @Test
    @DisplayName("extension default: trackProgressHistory is false")
    void extensionDefaultTrackProgressHistoryIsFalse() {
        Project project = projectWithPlugin();
        GherkinToAsciidocExtension ext = extension(project);

        assertThat(ext.getTrackProgressHistory().get()).isFalse();
    }

    @Test
    @DisplayName("extension default: progressHistoryFile is gherkin-progress-history.ndjson in the project directory")
    void extensionDefaultProgressHistoryFileIsInProjectDirectory() {
        Project project = projectWithPlugin();
        GherkinToAsciidocExtension ext = extension(project);

        assertThat(ext.getProgressHistoryFile().get().getAsFile())
                .isEqualTo(new File(project.getProjectDir(), "gherkin-progress-history.ndjson"));
    }

    @Test
    @DisplayName("extension default: updateProgressHistory follows trackProgressHistory's own value")
    void extensionDefaultUpdateProgressHistoryFollowsTrackProgressHistory() {
        Project project = projectWithPlugin();
        GherkinToAsciidocExtension ext = extension(project);

        ext.getTrackProgressHistory().set(true);

        assertThat(ext.getUpdateProgressHistory().get()).isTrue();
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
        task.getGroupByFeature().set(false);
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
        task.getIncludeSubDirs().set(false);
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
    @DisplayName("processes a directory's own feature files before descending into its sub-directories, "
            + "even when a sub-directory would sort first alphabetically by path")
    void processesOwnDirectoryFilesBeforeSubDirectories() throws IOException {
        Project project = projectWithPlugin();
        File rootDir = new File(tempDir.toFile(), "features");
        // "sub" sorts before "z.feature" if compared as plain path strings, but the own-directory
        // file must still be processed first: only descending into sub-directories afterwards.
        File subDir = new File(rootDir, "sub");
        subDir.mkdirs();
        writeFeatureFile(rootDir, "z.feature",
                "Feature: Z Feature\n\n  Scenario: Z scenario\n    Given z\n");
        writeFeatureFile(subDir, "a.feature",
                "Feature: A Feature\n\n  Scenario: A scenario\n    Given a\n");

        File outputDir = new File(tempDir.toFile(), "output");

        GenerateFeatureDocsTask task = task(project);
        task.getSourceDirs().from(rootDir);
        task.getOutputDir().set(outputDir);
        task.getProjectDirectory().set(project.getLayout().getProjectDirectory());
        task.generate();

        List<String> lines = Files.readAllLines(new File(outputDir, "features.adoc").toPath());
        assertThat(lines).containsSubsequence("== Z Feature", "== A Feature");
    }

    @Test
    @DisplayName("indexing numbers a directory's own feature files before its sub-directories' files")
    void indexingNumbersOwnDirectoryFilesBeforeSubDirectories() throws IOException {
        Project project = projectWithPlugin();
        File rootDir = new File(tempDir.toFile(), "features");
        File subDir = new File(rootDir, "sub");
        subDir.mkdirs();
        writeFeatureFile(rootDir, "z.feature",
                "Feature: Z Feature\n\n  Scenario: Z scenario\n    Given z\n");
        writeFeatureFile(subDir, "a.feature",
                "Feature: A Feature\n\n  Scenario: A scenario\n    Given a\n");

        File outputDir = new File(tempDir.toFile(), "output");

        GenerateFeatureDocsTask task = task(project);
        task.getSourceDirs().from(rootDir);
        task.getIndexing().set(IndexingMode.FEATURE);
        task.getOutputDir().set(outputDir);
        task.getProjectDirectory().set(project.getLayout().getProjectDirectory());
        task.generate();

        assertThat(Files.readString(rootDir.toPath().resolve("z.feature")))
                .contains("Feature: 1 - Z Feature");
        assertThat(Files.readString(subDir.toPath().resolve("a.feature")))
                .contains("Feature: 2 - A Feature");
    }

    @Test
    @DisplayName("multiple sourceDirs are processed alphabetically by path, regardless of configuration order")
    void multipleSourceDirsProcessedAlphabeticallyByPath() throws IOException {
        Project project = projectWithPlugin();
        File featuresAuth = new File(tempDir.toFile(), "features-auth");
        File featuresBilling = new File(tempDir.toFile(), "features-billing");
        featuresAuth.mkdirs();
        featuresBilling.mkdirs();
        writeFeatureFile(featuresAuth, "authentication.feature",
                "Feature: User authentication\n\n  Scenario: User logs in\n    Given a user\n");
        writeFeatureFile(featuresBilling, "invoice.feature",
                "Feature: Invoice payment\n\n  Scenario: User pays an invoice\n    Given an invoice\n");

        File outputDir = new File(tempDir.toFile(), "output");

        GenerateFeatureDocsTask task = task(project);
        // Configured out of alphabetical order: billing before auth.
        task.getSourceDirs().from(featuresBilling, featuresAuth);
        task.getIndexing().set(IndexingMode.FEATURE);
        task.getOutputDir().set(outputDir);
        task.getProjectDirectory().set(project.getLayout().getProjectDirectory());
        task.generate();

        assertThat(Files.readString(featuresAuth.toPath().resolve("authentication.feature")))
                .contains("Feature: 1 - User authentication");
        assertThat(Files.readString(featuresBilling.toPath().resolve("invoice.feature")))
                .contains("Feature: 2 - Invoice payment");
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
        task.getIncludeSubDirs().set(false);
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
    @DisplayName("writes listed/defined/implemented snippet files when trackProgress is enabled")
    void writesSnippetFilesWhenTrackProgressEnabled() throws IOException {
        Project project = projectWithPlugin();
        File featuresDir = new File(tempDir.toFile(), "features");
        featuresDir.mkdirs();
        writeFeatureFile(featuresDir, "sample.feature", """
                Feature: Sample

                  Scenario: Only a title

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
        File snippetDir = new File(tempDir.toFile(), "snippets");

        GenerateFeatureDocsTask task = task(project);
        task.getSourceDirs().from(featuresDir);
        task.getTrackProgress().set(true);
        task.getGlueCodeDirs().from(glueCodeDir);
        task.getGroupByFeature().set(true);
        task.getSnippetDir().set(snippetDir);
        task.getOutputDir().set(outputDir);
        task.getProjectDirectory().set(project.getLayout().getProjectDirectory());
        task.generate();

        File featureDir = new File(snippetDir, "sample");
        File listedFile = new File(featureDir, "listed.adoc");
        File implementedFile = new File(featureDir, "implemented.adoc");
        assertThat(listedFile).exists();
        assertThat(implementedFile).exists();
        assertThat(Files.readString(listedFile.toPath())).contains("* Scenario: Only a title");
        assertThat(Files.readString(implementedFile.toPath())).contains("* Scenario: Fully wired up");
    }

    @Test
    @DisplayName("renders the report from a template referencing the generated snippets")
    void generatesReportFromTemplateEndToEnd() throws IOException {
        Project project = projectWithPlugin();
        File featuresDir = new File(tempDir.toFile(), "features");
        featuresDir.mkdirs();
        writeFeatureFile(featuresDir, "sample.feature",
                "Feature: Sample\n\n  Scenario: Fully wired up\n    Given an implemented step\n");

        File glueCodeDir = new File(tempDir.toFile(), "steps");
        glueCodeDir.mkdirs();
        Files.writeString(new File(glueCodeDir, "Steps.java").toPath(), """
                public class Steps {
                    @Given("an implemented step")
                    public void implemented() {}
                }
                """);

        File templateFile = new File(tempDir.toFile(), "report.mustache");
        Files.writeString(templateFile.toPath(),
                "= Custom Report\n{{#sections}}{{{status}}}\n{{/sections}}");

        File outputDir = new File(tempDir.toFile(), "output");

        GenerateFeatureDocsTask task = task(project);
        task.getSourceDirs().from(featuresDir);
        task.getTrackProgress().set(true);
        task.getGlueCodeDirs().from(glueCodeDir);
        task.getGroupByFeature().set(true);
        task.getSnippetDir().set(new File(tempDir.toFile(), "snippets"));
        task.getTemplate().set(templateFile);
        task.getOutputDir().set(outputDir);
        task.getProjectDirectory().set(project.getLayout().getProjectDirectory());
        task.generate();

        String content = Files.readString(new File(outputDir, "features.adoc").toPath());
        assertThat(content).startsWith("= Custom Report");
        assertThat(content).doesNotContain("Progress Summary");
    }

    @Test
    @DisplayName("includes the project's version as the System Under Test version by default")
    void includesProjectVersionAsSystemUnderTestVersionByDefault() throws IOException {
        Project project = projectWithPlugin();
        project.setVersion("3.1.4");
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
        assertThat(content).contains("System Under Test version: 3.1.4");
    }

    @Test
    @DisplayName("systemUnderTestVersion overrides the project's own version in the generated document")
    void systemUnderTestVersionOverridesProjectVersion() throws IOException {
        Project project = projectWithPlugin();
        project.setVersion("3.1.4");
        File featuresDir = new File(tempDir.toFile(), "features");
        featuresDir.mkdirs();
        writeFeatureFile(featuresDir, "sample.feature",
                "Feature: Sample\n\n  Scenario: A scenario\n    Given something\n");

        File outputDir = new File(tempDir.toFile(), "output");

        GenerateFeatureDocsTask task = task(project);
        task.getSourceDirs().from(featuresDir);
        task.getSystemUnderTestVersion().set("v1.0.0");
        task.getOutputDir().set(outputDir);
        task.getProjectDirectory().set(project.getLayout().getProjectDirectory());
        task.generate();

        String content = Files.readString(new File(outputDir, "features.adoc").toPath());
        assertThat(content)
                .contains("System Under Test version: v1.0.0")
                .doesNotContain("3.1.4");
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

    @Test
    @DisplayName("output file declares a table of contents right after the title")
    void outputFileDeclaresTableOfContents() throws IOException {
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

        List<String> lines = Files.readAllLines(new File(outputDir, "features.adoc").toPath());
        assertThat(lines).containsSubsequence(
                "= Feature Scenarios", ":toc:", ":toclevels: 2", "");
    }

    @Test
    @DisplayName("throws GradleException when indexing is enabled but includeSubDirs is false")
    void throwsWhenIndexingEnabledWithoutIncludeSubDirs() throws IOException {
        Project project = projectWithPlugin();
        File featuresDir = new File(tempDir.toFile(), "features");
        featuresDir.mkdirs();
        writeFeatureFile(featuresDir, "sample.feature", "Feature: Sample\n\n  Scenario: A scenario\n    Given g\n");

        GenerateFeatureDocsTask task = task(project);
        task.getSourceDirs().from(featuresDir);
        task.getIncludeSubDirs().set(false);
        task.getIndexing().set(IndexingMode.SCENARIO);
        task.getOutputDir().set(new File(tempDir.toFile(), "output"));
        task.getProjectDirectory().set(project.getLayout().getProjectDirectory());

        assertThatThrownBy(task::generate)
                .isInstanceOf(org.gradle.api.GradleException.class)
                .hasMessageContaining("indexing can only be used when includeSubDirs is true");
    }

    @Test
    @DisplayName("throws GradleException when indexing is FEATURE and groupByFeature is false")
    void throwsWhenIndexingFeatureAndGroupByFeatureFalse() throws IOException {
        Project project = projectWithPlugin();
        File featuresDir = new File(tempDir.toFile(), "features");
        featuresDir.mkdirs();
        writeFeatureFile(featuresDir, "sample.feature", "Feature: Sample\n\n  Scenario: A scenario\n    Given g\n");

        GenerateFeatureDocsTask task = task(project);
        task.getSourceDirs().from(featuresDir);
        task.getGroupByFeature().set(false);
        task.getIndexing().set(IndexingMode.FEATURE);
        task.getOutputDir().set(new File(tempDir.toFile(), "output"));
        task.getProjectDirectory().set(project.getLayout().getProjectDirectory());

        assertThatThrownBy(task::generate)
                .isInstanceOf(org.gradle.api.GradleException.class)
                .hasMessageContaining("when groupByFeature is false, indexing can only be 'off', 'ci', or 'scenario'");
    }

    @Test
    @DisplayName("throws GradleException when indexing is ALL and groupByFeature is false")
    void throwsWhenIndexingAllAndGroupByFeatureFalse() throws IOException {
        Project project = projectWithPlugin();
        File featuresDir = new File(tempDir.toFile(), "features");
        featuresDir.mkdirs();
        writeFeatureFile(featuresDir, "sample.feature", "Feature: Sample\n\n  Scenario: A scenario\n    Given g\n");

        GenerateFeatureDocsTask task = task(project);
        task.getSourceDirs().from(featuresDir);
        task.getGroupByFeature().set(false);
        task.getIndexing().set(IndexingMode.ALL);
        task.getOutputDir().set(new File(tempDir.toFile(), "output"));
        task.getProjectDirectory().set(project.getLayout().getProjectDirectory());

        assertThatThrownBy(task::generate)
                .isInstanceOf(org.gradle.api.GradleException.class)
                .hasMessageContaining("when groupByFeature is false, indexing can only be 'off', 'ci', or 'scenario'");
    }

    @Test
    @DisplayName("indexing SCENARIO is allowed when groupByFeature is false")
    void indexingScenarioAllowedWithGroupByFeatureFalse() throws IOException {
        Project project = projectWithPlugin();
        File featuresDir = new File(tempDir.toFile(), "features");
        featuresDir.mkdirs();
        writeFeatureFile(featuresDir, "sample.feature", "Feature: Sample\n\n  Scenario: A scenario\n    Given g\n");

        GenerateFeatureDocsTask task = task(project);
        task.getSourceDirs().from(featuresDir);
        task.getGroupByFeature().set(false);
        task.getIndexing().set(IndexingMode.SCENARIO);
        task.getOutputDir().set(new File(tempDir.toFile(), "output"));
        task.getProjectDirectory().set(project.getLayout().getProjectDirectory());
        task.generate();

        assertThat(Files.readString(featuresDir.toPath().resolve("sample.feature")))
                .contains("Scenario: 1 - A scenario");
    }

    @Test
    @DisplayName("indexing FEATURE numbers features alphabetically by file name across source directories "
            + "and is reflected in the generated report")
    void indexingFeatureNumbersFeaturesAndUpdatesReport() throws IOException {
        Project project = projectWithPlugin();
        File authDir = new File(tempDir.toFile(), "features-auth");
        File billingDir = new File(tempDir.toFile(), "features-billing");
        authDir.mkdirs();
        billingDir.mkdirs();
        writeFeatureFile(authDir, "authentication.feature",
                "Feature: User authentication\n\n  Scenario: User logs in\n    Given a user\n");
        writeFeatureFile(billingDir, "invoice.feature",
                "Feature: Invoice payment\n\n  Scenario: User pays an invoice\n    Given an invoice\n");

        File outputDir = new File(tempDir.toFile(), "output");

        GenerateFeatureDocsTask task = task(project);
        task.getSourceDirs().from(authDir, billingDir);
        task.getIndexing().set(IndexingMode.FEATURE);
        task.getOutputDir().set(outputDir);
        task.getProjectDirectory().set(project.getLayout().getProjectDirectory());
        task.generate();

        assertThat(Files.readString(authDir.toPath().resolve("authentication.feature")))
                .contains("Feature: 1 - User authentication");
        assertThat(Files.readString(billingDir.toPath().resolve("invoice.feature")))
                .contains("Feature: 2 - Invoice payment");

        String content = Files.readString(new File(outputDir, "features.adoc").toPath());
        assertThat(content)
                .contains("== 1 - User authentication")
                .contains("== 2 - Invoice payment");
    }

    @Test
    @DisplayName("indexing ALL numbers scenarios per feature and is reflected in the generated report")
    void indexingAllNumbersScenariosPerFeatureAndUpdatesReport() throws IOException {
        Project project = projectWithPlugin();
        File authDir = new File(tempDir.toFile(), "features-auth");
        authDir.mkdirs();
        writeFeatureFile(authDir, "authentication.feature", """
                Feature: User authentication

                  Scenario: User logs in
                    Given a user

                  Scenario: User resets password
                    Given a user
                """);

        File outputDir = new File(tempDir.toFile(), "output");

        GenerateFeatureDocsTask task = task(project);
        task.getSourceDirs().from(authDir);
        task.getIndexing().set(IndexingMode.ALL);
        task.getOutputDir().set(outputDir);
        task.getProjectDirectory().set(project.getLayout().getProjectDirectory());
        task.generate();

        String content = Files.readString(new File(outputDir, "features.adoc").toPath());
        assertThat(content)
                .contains("== 1 - User authentication")
                .contains("* Scenario: 1.1 - User logs in")
                .contains("* Scenario: 1.2 - User resets password");
    }

    @Test
    @DisplayName("changing indexing from ALL to OFF on a subsequent run removes the numbering")
    void changingIndexingToOffRemovesNumberingOnNextRun() throws IOException {
        Project project = projectWithPlugin();
        File featuresDir = new File(tempDir.toFile(), "features");
        featuresDir.mkdirs();
        writeFeatureFile(featuresDir, "sample.feature",
                "Feature: Sample\n\n  Scenario: A scenario\n    Given g\n");
        File outputDir = new File(tempDir.toFile(), "output");

        GenerateFeatureDocsTask firstRun = task(project);
        firstRun.getSourceDirs().from(featuresDir);
        firstRun.getIndexing().set(IndexingMode.ALL);
        firstRun.getOutputDir().set(outputDir);
        firstRun.getProjectDirectory().set(project.getLayout().getProjectDirectory());
        firstRun.generate();
        assertThat(Files.readString(featuresDir.toPath().resolve("sample.feature")))
                .contains("Feature: 1 - Sample")
                .contains("Scenario: 1.1 - A scenario");

        GenerateFeatureDocsTask secondRun = task(project);
        secondRun.getSourceDirs().from(featuresDir);
        secondRun.getIndexing().set(IndexingMode.OFF);
        secondRun.getOutputDir().set(outputDir);
        secondRun.getProjectDirectory().set(project.getLayout().getProjectDirectory());
        secondRun.generate();

        assertThat(Files.readString(featuresDir.toPath().resolve("sample.feature")))
                .contains("Feature: Sample")
                .contains("Scenario: A scenario")
                .doesNotContain("1 -")
                .doesNotContain("1.1 -");
    }

    @Test
    @DisplayName("indexing CI leaves feature files completely untouched, even numbering left over from a previous run")
    void indexingCiLeavesFilesCompletelyUntouched() throws IOException {
        Project project = projectWithPlugin();
        File featuresDir = new File(tempDir.toFile(), "features");
        featuresDir.mkdirs();
        // Simulates numbering left over from an earlier ALL-mode run.
        writeFeatureFile(featuresDir, "sample.feature",
                "Feature: 1 - Sample\n\n  Scenario: 1.1 - A scenario\n    Given g\n");
        String before = Files.readString(featuresDir.toPath().resolve("sample.feature"));
        File outputDir = new File(tempDir.toFile(), "output");

        GenerateFeatureDocsTask task = task(project);
        task.getSourceDirs().from(featuresDir);
        task.getIndexing().set(IndexingMode.CI);
        task.getOutputDir().set(outputDir);
        task.getProjectDirectory().set(project.getLayout().getProjectDirectory());
        task.generate();

        // Unlike OFF, CI does not even strip prior numbering - the file is byte-for-byte unchanged.
        assertThat(Files.readString(featuresDir.toPath().resolve("sample.feature"))).isEqualTo(before);
        String content = Files.readString(new File(outputDir, "features.adoc").toPath());
        assertThat(content).contains("* Scenario: 1.1 - A scenario");
    }

    @Test
    @DisplayName("indexing CI is allowed even when includeSubDirs and groupByFeature are both false")
    void indexingCiAllowedRegardlessOfIncludeSubDirsAndGroupByFeature() throws IOException {
        Project project = projectWithPlugin();
        File featuresDir = new File(tempDir.toFile(), "features");
        featuresDir.mkdirs();
        writeFeatureFile(featuresDir, "sample.feature", "Feature: Sample\n\n  Scenario: A scenario\n    Given g\n");

        GenerateFeatureDocsTask task = task(project);
        task.getSourceDirs().from(featuresDir);
        task.getIncludeSubDirs().set(false);
        task.getGroupByFeature().set(false);
        task.getIndexing().set(IndexingMode.CI);
        task.getOutputDir().set(new File(tempDir.toFile(), "output"));
        task.getProjectDirectory().set(project.getLayout().getProjectDirectory());

        assertThatCode(task::generate).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("the -PgherkinToAsciidoc.indexing project property overrides indexing regardless of the configured value")
    void cliPropertyOverridesConfiguredIndexing() throws IOException {
        Files.writeString(tempDir.resolve("gradle.properties"), "gherkinToAsciidoc.indexing=ci\n");
        Project project = projectWithPlugin();
        File featuresDir = new File(tempDir.toFile(), "features");
        featuresDir.mkdirs();
        writeFeatureFile(featuresDir, "sample.feature",
                "Feature: 1 - Sample\n\n  Scenario: 1.1 - A scenario\n    Given g\n");
        String before = Files.readString(featuresDir.toPath().resolve("sample.feature"));

        GenerateFeatureDocsTask task = task(project);
        task.getSourceDirs().from(featuresDir);
        // Configured to ALL, but the CLI override must win.
        extension(project).getIndexing().set(IndexingMode.ALL);
        task.getOutputDir().set(new File(tempDir.toFile(), "output"));
        task.getProjectDirectory().set(project.getLayout().getProjectDirectory());
        task.generate();

        assertThat(task.getIndexing().get()).isEqualTo(IndexingMode.CI);
        assertThat(Files.readString(featuresDir.toPath().resolve("sample.feature"))).isEqualTo(before);
    }

    @Test
    @DisplayName("an invalid -PgherkinToAsciidoc.indexing value throws a descriptive GradleException")
    void cliPropertyInvalidValueThrowsDescriptiveError() throws IOException {
        Files.writeString(tempDir.resolve("gradle.properties"), "gherkinToAsciidoc.indexing=bogus\n");
        Project project = projectWithPlugin();
        File featuresDir = new File(tempDir.toFile(), "features");
        featuresDir.mkdirs();
        writeFeatureFile(featuresDir, "sample.feature", "Feature: Sample\n\n  Scenario: A scenario\n    Given g\n");

        GenerateFeatureDocsTask task = task(project);
        task.getSourceDirs().from(featuresDir);
        task.getOutputDir().set(new File(tempDir.toFile(), "output"));
        task.getProjectDirectory().set(project.getLayout().getProjectDirectory());

        // Gradle wraps the exception thrown while lazily resolving the "indexing" property's
        // value (the -P override is parsed lazily via a Provider) in a PropertyQueryException;
        // the GradleException with the actual descriptive message is its root cause.
        assertThatThrownBy(task::generate)
                .hasRootCauseInstanceOf(org.gradle.api.GradleException.class)
                .hasRootCauseMessage("gherkinToAsciidoc: invalid value 'bogus' for -PgherkinToAsciidoc.indexing; "
                        + "expected one of: off, feature, scenario, all, ci");
    }

    @Test
    @DisplayName("forceRewrite default (false): a new alphabetically-earlier feature file added on a later "
            + "run does not renumber an already-numbered file")
    void forceRewriteDefaultDoesNotRenumberOnLaterRun() throws IOException {
        Project project = projectWithPlugin();
        File featuresDir = new File(tempDir.toFile(), "features");
        featuresDir.mkdirs();
        writeFeatureFile(featuresDir, "z.feature",
                "Feature: Z Feature\n\n  Scenario: Z scenario\n    Given z\n");
        File outputDir = new File(tempDir.toFile(), "output");

        GenerateFeatureDocsTask firstRun = task(project);
        firstRun.getSourceDirs().from(featuresDir);
        firstRun.getIndexing().set(IndexingMode.FEATURE);
        firstRun.getOutputDir().set(outputDir);
        firstRun.getProjectDirectory().set(project.getLayout().getProjectDirectory());
        firstRun.generate();
        assertThat(Files.readString(featuresDir.toPath().resolve("z.feature")))
                .contains("Feature: 1 - Z Feature");

        // A new file that sorts alphabetically before z.feature is added on a later run.
        writeFeatureFile(featuresDir, "a.feature",
                "Feature: A Feature\n\n  Scenario: A scenario\n    Given a\n");

        GenerateFeatureDocsTask secondRun = task(project);
        secondRun.getSourceDirs().from(featuresDir);
        secondRun.getIndexing().set(IndexingMode.FEATURE);
        secondRun.getOutputDir().set(outputDir);
        secondRun.getProjectDirectory().set(project.getLayout().getProjectDirectory());
        secondRun.generate();

        assertThat(Files.readString(featuresDir.toPath().resolve("z.feature")))
                .contains("Feature: 1 - Z Feature");
        assertThat(Files.readString(featuresDir.toPath().resolve("a.feature")))
                .contains("Feature: 2 - A Feature");
    }

    @Test
    @DisplayName("forceRewrite true: a new alphabetically-earlier feature file added on a later run "
            + "renumbers the already-numbered file to fit alphabetical order")
    void forceRewriteTrueRenumbersOnLaterRun() throws IOException {
        Project project = projectWithPlugin();
        File featuresDir = new File(tempDir.toFile(), "features");
        featuresDir.mkdirs();
        writeFeatureFile(featuresDir, "z.feature",
                "Feature: Z Feature\n\n  Scenario: Z scenario\n    Given z\n");
        File outputDir = new File(tempDir.toFile(), "output");

        GenerateFeatureDocsTask firstRun = task(project);
        firstRun.getSourceDirs().from(featuresDir);
        firstRun.getIndexing().set(IndexingMode.FEATURE);
        firstRun.getForceRewrite().set(true);
        firstRun.getOutputDir().set(outputDir);
        firstRun.getProjectDirectory().set(project.getLayout().getProjectDirectory());
        firstRun.generate();

        writeFeatureFile(featuresDir, "a.feature",
                "Feature: A Feature\n\n  Scenario: A scenario\n    Given a\n");

        GenerateFeatureDocsTask secondRun = task(project);
        secondRun.getSourceDirs().from(featuresDir);
        secondRun.getIndexing().set(IndexingMode.FEATURE);
        secondRun.getForceRewrite().set(true);
        secondRun.getOutputDir().set(outputDir);
        secondRun.getProjectDirectory().set(project.getLayout().getProjectDirectory());
        secondRun.generate();

        assertThat(Files.readString(featuresDir.toPath().resolve("a.feature")))
                .contains("Feature: 1 - A Feature");
        assertThat(Files.readString(featuresDir.toPath().resolve("z.feature")))
                .contains("Feature: 2 - Z Feature");
    }

    @Test
    @DisplayName("the -PgherkinToAsciidoc.forceRewrite project property overrides forceRewrite regardless "
            + "of the configured value")
    void cliPropertyOverridesConfiguredForceRewrite() throws IOException {
        Files.writeString(tempDir.resolve("gradle.properties"), "gherkinToAsciidoc.forceRewrite=true\n");
        Project project = projectWithPlugin();
        File featuresDir = new File(tempDir.toFile(), "features");
        featuresDir.mkdirs();
        writeFeatureFile(featuresDir, "z.feature",
                "Feature: 1 - Z Feature\n\n  Scenario: Z scenario\n    Given z\n");
        writeFeatureFile(featuresDir, "a.feature",
                "Feature: A Feature\n\n  Scenario: A scenario\n    Given a\n");

        GenerateFeatureDocsTask task = task(project);
        task.getSourceDirs().from(featuresDir);
        task.getIndexing().set(IndexingMode.FEATURE);
        // Configured to false, but the CLI override must win.
        extension(project).getForceRewrite().set(false);
        task.getOutputDir().set(new File(tempDir.toFile(), "output"));
        task.getProjectDirectory().set(project.getLayout().getProjectDirectory());
        task.generate();

        assertThat(task.getForceRewrite().get()).isTrue();
        assertThat(Files.readString(featuresDir.toPath().resolve("a.feature")))
                .contains("Feature: 1 - A Feature");
        assertThat(Files.readString(featuresDir.toPath().resolve("z.feature")))
                .contains("Feature: 2 - Z Feature");
    }

    @Test
    @DisplayName("an invalid -PgherkinToAsciidoc.forceRewrite value throws a descriptive GradleException")
    void cliPropertyInvalidForceRewriteValueThrowsDescriptiveError() throws IOException {
        Files.writeString(tempDir.resolve("gradle.properties"), "gherkinToAsciidoc.forceRewrite=maybe\n");
        Project project = projectWithPlugin();
        File featuresDir = new File(tempDir.toFile(), "features");
        featuresDir.mkdirs();
        writeFeatureFile(featuresDir, "sample.feature", "Feature: Sample\n\n  Scenario: A scenario\n    Given g\n");

        GenerateFeatureDocsTask task = task(project);
        task.getSourceDirs().from(featuresDir);
        task.getOutputDir().set(new File(tempDir.toFile(), "output"));
        task.getProjectDirectory().set(project.getLayout().getProjectDirectory());

        assertThatThrownBy(task::generate)
                .hasRootCauseInstanceOf(org.gradle.api.GradleException.class)
                .hasRootCauseMessage("gherkinToAsciidoc: invalid value 'maybe' for "
                        + "-PgherkinToAsciidoc.forceRewrite; expected 'true' or 'false'");
    }

    @Test
    @DisplayName("throws GradleException when trackProgressHistory is enabled without trackProgress")
    void throwsWhenTrackProgressHistoryEnabledWithoutTrackProgress() throws IOException {
        Project project = projectWithPlugin();
        File featuresDir = new File(tempDir.toFile(), "features");
        featuresDir.mkdirs();
        writeFeatureFile(featuresDir, "sample.feature", "Feature: F\n  Scenario: S\n    Given g\n");

        GenerateFeatureDocsTask task = task(project);
        task.getSourceDirs().from(featuresDir);
        task.getTrackProgressHistory().set(true);
        task.getOutputDir().set(new File(tempDir.toFile(), "output"));
        task.getProjectDirectory().set(project.getLayout().getProjectDirectory());

        assertThatThrownBy(task::generate)
                .isInstanceOf(org.gradle.api.GradleException.class)
                .hasMessageContaining("trackProgressHistory requires trackProgress to be enabled");
    }

    @Test
    @DisplayName("writes the progress history file when trackProgressHistory and updateProgressHistory are both true")
    void writesProgressHistoryFileWhenBothPropertiesAreTrue() throws IOException {
        Project project = projectWithPlugin();
        File featuresDir = new File(tempDir.toFile(), "features");
        featuresDir.mkdirs();
        writeFeatureFile(featuresDir, "sample.feature", "Feature: Sample\n\n  Scenario: A scenario\n");
        File glueCodeDir = new File(tempDir.toFile(), "steps");
        glueCodeDir.mkdirs();
        File historyFile = new File(tempDir.toFile(), "history.ndjson");

        GenerateFeatureDocsTask task = task(project);
        task.getSourceDirs().from(featuresDir);
        task.getTrackProgress().set(true);
        task.getGlueCodeDirs().from(glueCodeDir);
        task.getTrackProgressHistory().set(true);
        task.getUpdateProgressHistory().set(true);
        task.getProgressHistoryFile().set(historyFile);
        task.getOutputDir().set(new File(tempDir.toFile(), "output"));
        task.getProjectDirectory().set(project.getLayout().getProjectDirectory());
        task.generate();

        assertThat(historyFile).exists();
    }

    @Test
    @DisplayName("does not write the progress history file when updateProgressHistory is false, "
            + "even though trackProgressHistory is true")
    void doesNotWriteProgressHistoryFileWhenUpdateProgressHistoryIsFalse() throws IOException {
        Project project = projectWithPlugin();
        File featuresDir = new File(tempDir.toFile(), "features");
        featuresDir.mkdirs();
        writeFeatureFile(featuresDir, "sample.feature", "Feature: Sample\n\n  Scenario: A scenario\n");
        File glueCodeDir = new File(tempDir.toFile(), "steps");
        glueCodeDir.mkdirs();
        File historyFile = new File(tempDir.toFile(), "history.ndjson");

        GenerateFeatureDocsTask task = task(project);
        task.getSourceDirs().from(featuresDir);
        task.getTrackProgress().set(true);
        task.getGlueCodeDirs().from(glueCodeDir);
        task.getTrackProgressHistory().set(true);
        task.getUpdateProgressHistory().set(false);
        task.getProgressHistoryFile().set(historyFile);
        task.getOutputDir().set(new File(tempDir.toFile(), "output"));
        task.getProjectDirectory().set(project.getLayout().getProjectDirectory());
        task.generate();

        assertThat(historyFile).doesNotExist();
    }

    @Test
    @DisplayName("progressHistoryFilePath reflects the configured progressHistoryFile's absolute path")
    void progressHistoryFilePathReflectsConfiguredFile() {
        Project project = projectWithPlugin();
        File historyFile = new File(tempDir.toFile(), "history.ndjson");
        GenerateFeatureDocsTask task = task(project);
        task.getProgressHistoryFile().set(historyFile);

        assertThat(task.getProgressHistoryFilePath()).isEqualTo(historyFile.getAbsolutePath());
    }

    @Test
    @DisplayName("getProgressHistoryFilePath is annotated with @Input so a changed path invalidates up-to-date state")
    void progressHistoryFilePathIsAnnotatedAsInput() throws NoSuchMethodException {
        var method = GenerateFeatureDocsTask.class.getMethod("getProgressHistoryFilePath");

        assertThat(method.isAnnotationPresent(org.gradle.api.tasks.Input.class)).isTrue();
    }

    @Test
    @DisplayName("still renders a Progress Over Time section from the in-memory history "
            + "even when updateProgressHistory is false")
    void rendersProgressOverTimeSectionEvenWhenNotPersisted() throws IOException {
        Project project = projectWithPlugin();
        File featuresDir = new File(tempDir.toFile(), "features");
        featuresDir.mkdirs();
        writeFeatureFile(featuresDir, "sample.feature", "Feature: Sample\n\n  Scenario: A scenario\n");
        File glueCodeDir = new File(tempDir.toFile(), "steps");
        glueCodeDir.mkdirs();
        File outputDir = new File(tempDir.toFile(), "output");

        GenerateFeatureDocsTask task = task(project);
        task.getSourceDirs().from(featuresDir);
        task.getTrackProgress().set(true);
        task.getGlueCodeDirs().from(glueCodeDir);
        task.getTrackProgressHistory().set(true);
        task.getUpdateProgressHistory().set(false);
        task.getProgressHistoryFile().set(new File(tempDir.toFile(), "history.ndjson"));
        task.getOutputDir().set(outputDir);
        task.getProjectDirectory().set(project.getLayout().getProjectDirectory());
        task.generate();

        String content = Files.readString(new File(outputDir, "features.adoc").toPath());
        assertThat(content).contains("Progress Over Time");
    }

    @Test
    @DisplayName("the -PgherkinToAsciidoc.updateProgressHistory project property overrides updateProgressHistory "
            + "regardless of the configured value")
    void cliPropertyOverridesConfiguredUpdateProgressHistory() throws IOException {
        Files.writeString(tempDir.resolve("gradle.properties"), "gherkinToAsciidoc.updateProgressHistory=true\n");
        Project project = projectWithPlugin();
        File featuresDir = new File(tempDir.toFile(), "features");
        featuresDir.mkdirs();
        writeFeatureFile(featuresDir, "sample.feature", "Feature: Sample\n\n  Scenario: A scenario\n");
        File glueCodeDir = new File(tempDir.toFile(), "steps");
        glueCodeDir.mkdirs();
        File historyFile = new File(tempDir.toFile(), "history.ndjson");

        GenerateFeatureDocsTask task = task(project);
        task.getSourceDirs().from(featuresDir);
        task.getTrackProgress().set(true);
        task.getGlueCodeDirs().from(glueCodeDir);
        task.getTrackProgressHistory().set(true);
        // Configured to false, but the CLI override must win.
        extension(project).getUpdateProgressHistory().set(false);
        task.getProgressHistoryFile().set(historyFile);
        task.getOutputDir().set(new File(tempDir.toFile(), "output"));
        task.getProjectDirectory().set(project.getLayout().getProjectDirectory());
        task.generate();

        assertThat(historyFile).exists();
    }

    @Test
    @DisplayName("an invalid -PgherkinToAsciidoc.updateProgressHistory value throws a descriptive GradleException")
    void cliPropertyInvalidUpdateProgressHistoryValueThrowsDescriptiveError() throws IOException {
        Files.writeString(tempDir.resolve("gradle.properties"), "gherkinToAsciidoc.updateProgressHistory=maybe\n");
        Project project = projectWithPlugin();
        File featuresDir = new File(tempDir.toFile(), "features");
        featuresDir.mkdirs();
        writeFeatureFile(featuresDir, "sample.feature", "Feature: Sample\n\n  Scenario: A scenario\n");
        File glueCodeDir = new File(tempDir.toFile(), "steps");
        glueCodeDir.mkdirs();

        GenerateFeatureDocsTask task = task(project);
        task.getSourceDirs().from(featuresDir);
        task.getTrackProgress().set(true);
        task.getGlueCodeDirs().from(glueCodeDir);
        task.getTrackProgressHistory().set(true);
        task.getProgressHistoryFile().set(new File(tempDir.toFile(), "history.ndjson"));
        task.getOutputDir().set(new File(tempDir.toFile(), "output"));
        task.getProjectDirectory().set(project.getLayout().getProjectDirectory());

        assertThatThrownBy(task::generate)
                .hasRootCauseInstanceOf(org.gradle.api.GradleException.class)
                .hasRootCauseMessage("gherkinToAsciidoc: invalid value 'maybe' for "
                        + "-PgherkinToAsciidoc.updateProgressHistory; expected 'true' or 'false'");
    }

    @Test
    @DisplayName("persists scenario progress history across runs, preserving it when a scenario moves "
            + "between features - the gherkinToAsciidoc trackProgressHistory worked example")
    void persistsProgressHistoryAcrossRunsAndScenarioMoves() throws IOException {
        Project project = projectWithPlugin();
        File featuresDir = new File(tempDir.toFile(), "features");
        featuresDir.mkdirs();
        writeFeatureFile(featuresDir, "auth.feature", """
                Feature: User authentication

                  Scenario: User logs in

                  Scenario: User resets password
                """);

        File glueCodeDir = new File(tempDir.toFile(), "steps");
        glueCodeDir.mkdirs();
        File outputDir = new File(tempDir.toFile(), "output");
        File historyFile = new File(tempDir.toFile(), "history.ndjson");
        ProgressHistoryStore store = new ProgressHistoryStore();
        ScenarioFingerprint fingerprinter = new ScenarioFingerprint();
        String loginFingerprint = fingerprinter.fingerprint("Scenario: User logs in");
        String resetFingerprint = fingerprinter.fingerprint("Scenario: User resets password");

        // Run 1: neither scenario has steps yet - both are listed.
        runTrackProgressHistory(project, featuresDir, glueCodeDir, historyFile, outputDir);
        Map<String, ScenarioProgressRecord> afterRun1 = store.load(historyFile);
        ScenarioProgressRecord loginAfterRun1 = afterRun1.get(loginFingerprint);
        ScenarioProgressRecord resetAfterRun1 = afterRun1.get(resetFingerprint);
        assertThat(loginAfterRun1.listedAt()).isNotNull();
        assertThat(loginAfterRun1.definedAt()).isNull();
        assertThat(loginAfterRun1.implementedAt()).isNull();

        // Run 2: "User logs in" gains steps, fully covered by newly added glue code;
        // "User resets password" is untouched and stays listed.
        writeFeatureFile(featuresDir, "auth.feature", """
                Feature: User authentication

                  Scenario: User logs in
                    Given a registered user
                    When they submit valid credentials
                    Then they are signed in

                  Scenario: User resets password
                """);
        Files.writeString(new File(glueCodeDir, "LoginSteps.java").toPath(), """
                public class LoginSteps {
                    @Given("a registered user")
                    public void aRegisteredUser() {}
                    @When("they submit valid credentials")
                    public void theySubmitValidCredentials() {}
                    @Then("they are signed in")
                    public void theyAreSignedIn() {}
                }
                """);
        runTrackProgressHistory(project, featuresDir, glueCodeDir, historyFile, outputDir);
        Map<String, ScenarioProgressRecord> afterRun2 = store.load(historyFile);
        ScenarioProgressRecord loginAfterRun2 = afterRun2.get(loginFingerprint);
        // The scenario jumped straight from listed to implemented - defined was never observed.
        assertThat(loginAfterRun2.listedAt()).isEqualTo(loginAfterRun1.listedAt());
        assertThat(loginAfterRun2.definedAt()).isNull();
        assertThat(loginAfterRun2.implementedAt()).isNotNull();
        ScenarioProgressRecord resetAfterRun2 = afterRun2.get(resetFingerprint);
        assertThat(resetAfterRun2.listedAt()).isEqualTo(resetAfterRun1.listedAt());

        // Run 3: "User logs in" is moved, unchanged, into a new Feature: Sign-in.
        writeFeatureFile(featuresDir, "auth.feature",
                "Feature: User authentication\n\n  Scenario: User resets password\n");
        writeFeatureFile(featuresDir, "sign-in.feature", """
                Feature: Sign-in

                  Scenario: User logs in
                    Given a registered user
                    When they submit valid credentials
                    Then they are signed in
                """);
        runTrackProgressHistory(project, featuresDir, glueCodeDir, historyFile, outputDir);
        Map<String, ScenarioProgressRecord> afterRun3 = store.load(historyFile);
        ScenarioProgressRecord loginAfterRun3 = afterRun3.get(loginFingerprint);
        assertThat(loginAfterRun3.featureTitle()).isEqualTo("Sign-in");
        assertThat(loginAfterRun3.listedAt()).isEqualTo(loginAfterRun1.listedAt());
        assertThat(loginAfterRun3.implementedAt()).isEqualTo(loginAfterRun2.implementedAt());
    }

    private void runTrackProgressHistory(
            Project project, File featuresDir, File glueCodeDir, File historyFile, File outputDir) {
        GenerateFeatureDocsTask task = task(project);
        task.getSourceDirs().from(featuresDir);
        task.getTrackProgress().set(true);
        task.getGlueCodeDirs().from(glueCodeDir);
        task.getTrackProgressHistory().set(true);
        task.getUpdateProgressHistory().set(true);
        task.getProgressHistoryFile().set(historyFile);
        task.getOutputDir().set(outputDir);
        task.getProjectDirectory().set(project.getLayout().getProjectDirectory());
        task.generate();
    }

    @Test
    @DisplayName("emits at least one progress line while parsing more than one feature file")
    void emitsProgressLineWhileParsingMultipleFeatureFiles() throws IOException {
        Project project = projectWithPlugin();
        File featuresDir = new File(tempDir.toFile(), "features");
        featuresDir.mkdirs();
        writeFeatureFile(featuresDir, "signup.feature",
                "Feature: Sign Up\n\n  Scenario: New user signs up\n    Given the sign-up page\n");
        writeFeatureFile(featuresDir, "login.feature",
                "Feature: Login\n\n  Scenario: User logs in\n    Given the login page\n");
        RecordingLogger recordingLogger = new RecordingLogger();
        // Created directly rather than via the registered generateFeatureDocs task, so every
        // property the plugin would otherwise wire from the extension must be set explicitly here.
        LoggerCapturingGenerateFeatureDocsTask task = project.getTasks()
                .create("generateFeatureDocsWithRecordingLogger", LoggerCapturingGenerateFeatureDocsTask.class);
        task.recordingLogger = recordingLogger;
        task.getSourceDirs().from(featuresDir);
        task.getIncludeSubDirs().set(true);
        task.getOutputDir().set(new File(tempDir.toFile(), "output"));
        task.getOutputFileName().set("features.adoc");
        task.getTrackProgress().set(false);
        task.getGroupByFeature().set(true);
        task.getSystemUnderTestVersion().set("1.0.0");
        task.getIndexing().set(IndexingMode.OFF);
        task.getForceRewrite().set(false);
        task.getTrackProgressHistory().set(false);
        task.getUpdateProgressHistory().set(false);
        task.getProjectDirectory().set(project.getLayout().getProjectDirectory());

        task.generate();

        assertThat(recordingLogger.lifecycleMessages())
                .anyMatch(message -> message.contains("Parsing feature files"));
    }

    /**
     * Test-only subclass that substitutes a {@link RecordingLogger} for the framework-provided
     * task logger, since {@link GenerateFeatureDocsTask#getLogger()} cannot otherwise be observed
     * from a {@link ProjectBuilder}-based test.
     */
    abstract static class LoggerCapturingGenerateFeatureDocsTask extends GenerateFeatureDocsTask {

        RecordingLogger recordingLogger;

        @Inject
        public LoggerCapturingGenerateFeatureDocsTask() {}

        @Override
        public Logger getLogger() {
            return recordingLogger;
        }
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
